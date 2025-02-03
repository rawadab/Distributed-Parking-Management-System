package com.example.customerui;

import com.example.queries.ParkingService;
import com.example.recommender.ConsensusProtocol;
import com.example.recommender.ElectionManager;
import com.example.recommender.RaftNode;
import com.example.recommender.RecommendationResponse;
import com.example.shared.models.ParkingEvent;
import com.example.shared.utils.RabbitMQUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.*;

/**
 * Controller class for the Customer UI in the Mulligan Parking System.
 * This class manages user interactions related to parking events, including:
 * - Starting and stopping parking sessions.
 * - Displaying active and completed parking events.
 * - Requesting parking recommendations using RabbitMQ and consensus-based decision-making.
 */
public class CustomerController {

    @FXML
    private TextField parkingSpaceField;

    @FXML
    private TableView<ParkingEvent> eventsTable;

    @FXML
    private TableColumn<ParkingEvent, Integer> spaceIdColumn;

    @FXML
    private TableColumn<ParkingEvent, String> startTimeColumn;

    @FXML
    private TableColumn<ParkingEvent, String> endTimeColumn;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<ParkingEvent> eventsTable1;

    @FXML
    private TableColumn<ParkingEvent, Integer> spaceIdColumn1;

    @FXML
    private TableColumn<ParkingEvent, String> startTimeColumn1;

    private final ParkingService parkingService = new ParkingService();
    private int customerID;
    private int vehicleId;
    private String customerName;


    /**
     * Initializes the controller by setting up table columns for parking events.
     */
    @FXML
    public void initialize() {
        spaceIdColumn.setCellValueFactory(new PropertyValueFactory<>("spaceId"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        spaceIdColumn1.setCellValueFactory(new PropertyValueFactory<>("spaceId"));
        startTimeColumn1.setCellValueFactory(new PropertyValueFactory<>("startTime"));
    }


    /**
     * Sets the customer ID and retrieves the associated vehicle number.
     *
     * @param args Contains customer ID and customer name.
     */
    public void setCustomerIdAndVehicleNumber(String[] args) {
        try {
            this.customerID = Integer.parseInt(args[0]);
            this.customerName = args[1];
            this.vehicleId = parkingService.getVehicleNumber(this.customerID);
            System.out.println("Logged in successfully as: " + customerName);
        } catch (NumberFormatException ex) {
            System.err.println("Invalid customer ID format.");
        } catch (Exception e) {
            System.err.println("Error fetching vehicle number: " + e.getMessage());
        }
    }

    /**
     * Initiates a parking session for the customer's vehicle.
     */
    @FXML
    public void startParking() {
        String parkingSpaceId = parkingSpaceField.getText();

        if (parkingSpaceId == null || parkingSpaceId.isEmpty()) {
            showMessage("Please enter Parking Space ID.");
            return;
        }

        boolean result = parkingService.startParking(vehicleId, parkingSpaceId);

        if (result) {
            showMessage("Parking started successfully.");
        } else {
            showMessage("Failed to start parking. Please check the details and try again.");
        }
    }

    /**
     * Stops an active parking session for the customer's vehicle.
     */
    @FXML
    public void stopParking() {
        boolean result = parkingService.stopParking(vehicleId);

        if (result) {
            showMessage("Parking stopped successfully.");
        } else {
            showMessage("No active parking event found for the provided Vehicle ID.");
        }
    }

    /**
     * Displays the customer's active and completed parking events.
     *
     * @return true if parking events are found, false otherwise.
     */
    @FXML
    public boolean showParkingEvents() {
        try {
            List<ParkingEvent> completedEvents = parkingService.getParkingcompletedEvents(vehicleId);
            List<ParkingEvent> activeEvents = parkingService.getParkingactiveEvents(vehicleId);

            ObservableList<ParkingEvent> completedObservable = FXCollections.observableArrayList(completedEvents);
            eventsTable.setItems(completedObservable);

            ObservableList<ParkingEvent> activeObservable = FXCollections.observableArrayList(activeEvents);
            eventsTable1.setItems(activeObservable);

            double totalAmount = completedEvents.stream()
                    .mapToDouble(ParkingEvent::getTotalCost)
                    .sum();
            totalAmountLabel.setText(String.format("Total Amount: $%.2f", totalAmount));

            if (completedEvents.isEmpty() && activeEvents.isEmpty()) {
                showMessage("No parking events found for the provided Vehicle ID.");
                return false;
            } else {
                showMessage("Parking events loaded successfully.");
                return true;
            }
        } catch (Exception e) {
            showMessage("Error loading parking events: " + e.getMessage());
            return false;
        }
    }

    /**
     * Requests a parking recommendation using a consensus-based approach among available RabbitMQ nodes.
     */
    @FXML
    public void recommendParking() {
        String spaceStr = parkingSpaceField.getText();
        if (spaceStr == null || spaceStr.isEmpty()) {
            showMessage("Please enter a parking space ID.");
            return;
        }

        int requestedSpaceID;
        try {
            requestedSpaceID = Integer.parseInt(spaceStr);
        } catch (NumberFormatException e) {
            showMessage("Invalid parking space ID.");
            return;
        }

        // רשימת URI של ה-Nodes ב-RabbitMQ
        List<String> nodeUris = Arrays.asList(
                "amqp://100.85.154.51",
                "amqp://100.76.110.20",
                "amqp://100.78.144.87"
        );
        String nodeDbUrl  = "jdbc:mysql://%s:3306/muligansystem";


        // ElectionManager (אם תרצה להשתמש בו למנהיג בלבד)
        ElectionManager electionManager = new ElectionManager(nodeUris);
        String leaderNode = electionManager.getLeader();

        // יצירת RabbitMQUtil + RaftNode לכל Node
        List<RabbitMQUtil> rabbitMQs = new ArrayList<>();
        List<RaftNode> raftNodes = new ArrayList<>();
        for (String uri : nodeUris) {
            RabbitMQUtil rmq = new RabbitMQUtil(uri);
            if(rmq.getConnectedServer() == null)
                continue;
            rabbitMQs.add(rmq);

            // הגדרות DB (אפשר שונות לכל Node)
            String dbUrl = String.format(nodeDbUrl, uri.replace("amqp://", ""));
            String dbUser = "root";
            String dbPass = "root";

            RaftNode node = new RaftNode(uri, rmq, dbUrl, dbUser, dbPass);
            raftNodes.add(node);
        }

        // יצירת אובייקט פרוטוקול הקונצנזוס
        ConsensusProtocol protocol = new ConsensusProtocol(raftNodes, rabbitMQs);

        // שליחה "בקשה" לכל צומת לחשב את ההמלצה ולפרסם ל-recommendationResponsesQueue
        protocol.sendRecommendationRequest(requestedSpaceID);

        // איסוף התגובות
        List<RecommendationResponse> responses = protocol.collectResponses();
        for (RabbitMQUtil rabbitMQUtil : rabbitMQs) {
            if(rabbitMQUtil.getConnectedServer()!=null)
            {
                rabbitMQUtil.purgeQueue("recommendationResponsesQueue");

            }
        }

        // חישוב Majority Vote
        String finalRec = protocol.getConsensusDecision(responses);

        // הצגת תוצאה
        if (finalRec != null) {
            showMessage("Parking Recommendation: \n" + finalRec);
        } else {
            showMessage("No consensus reached");
        }


        // סגירה
        for (RabbitMQUtil rmq : rabbitMQs) {
            rmq.closeConnection();
        }
    }

    /**
     * Displays a message in the UI.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        Platform.runLater(() -> {
            messageLabel.setText(message);
            System.out.println(message);
        });
    }

    private void showPopup(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
