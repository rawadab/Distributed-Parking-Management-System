package com.example.mo;


import com.example.queries.MunicipalityOfficerService;
import com.example.shared.models.Citation;
import com.example.shared.models.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.ArrayList;

/**
 * Controller class for managing municipality officer interactions.
 * Provides functionality to generate and display transaction and citation reports.
 * This controller interacts with the {@link MunicipalityOfficerService} to fetch
 * data and populate UI components such as tables for transactions and citations.
 */
public class MunicipalityOfficerController {

    /** Table for displaying transactions */
    @FXML
    private TableView<Transaction> transactionsTable;

    /** Table column for vehicle numbers in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionVehicleNumberColumn;

    /** Table column for space IDs in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionSpaceIdColumn;

    /** Table column for zone names in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionZoneColumn;

    /** Table column for start times in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionStartTimeColumn;

    /** Table column for stop times in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionStopTimeColumn;

    /** Table column for total amounts in transactions */
    @FXML
    private TableColumn<Transaction, String> TransactionAmountColumn;

    /** Table for displaying citations */
    @FXML
    private TableView<Citation> citationsTable;

    /** Table column for vehicle numbers in citations */
    @FXML
    private TableColumn<Citation, String> citationsVehicleNumberColumn;

    /** Table column for space IDs in citations */
    @FXML
    private TableColumn<Citation, String> citationsSpaceIdColumn;

    /** Table column for parking zones in citations */
    @FXML
    private TableColumn<Citation, String> citationsZoneColumn;

    /** Table column for inspection times in citations */
    @FXML
    private TableColumn<Citation, String> citationsInspectionTimes;

    /** Table column for total amounts in citations */
    @FXML
    private TableColumn<Citation, String> citationsAmountColumn;

    /** Service class for handling municipality officer operations */
    private final MunicipalityOfficerService municipalityOfficerService = new MunicipalityOfficerService();

    /**
     * Initializes the MunicipalityOfficerController.
     * Sets up table columns for displaying transaction and citation data.
     */
    @FXML
    public void initialize() {
        // Initialize Transaction Table
        TransactionVehicleNumberColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        TransactionSpaceIdColumn.setCellValueFactory(new PropertyValueFactory<>("spaceId"));
        TransactionZoneColumn.setCellValueFactory(new PropertyValueFactory<>("zoneName"));
        TransactionStartTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        TransactionStopTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        TransactionAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

        // Initialize Citation Table
        citationsVehicleNumberColumn.setCellValueFactory(new PropertyValueFactory<>("VehicleID"));
        citationsSpaceIdColumn.setCellValueFactory(new PropertyValueFactory<>("SpaceID"));
        citationsZoneColumn.setCellValueFactory(new PropertyValueFactory<>("ParkingZone"));
        citationsInspectionTimes.setCellValueFactory(new PropertyValueFactory<>("inspectionTimes"));
        citationsAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
    }

    /**
     * Generates and displays a report of all transactions.
     * Fetches data from the {@link MunicipalityOfficerService} and populates the transaction table.
     */
    public void generateTransactionReport() {
        // Get the transactions as an ArrayList from the service
        ArrayList<Transaction> transactions = municipalityOfficerService.generateTransactionReport();
        // Convert the ArrayList to an ObservableList
        ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList(transactions);
        // Set the ObservableList to the TableView
        transactionsTable.setItems(observableTransactions);
    }

    /**
     * Generates and displays a report of all citations.
     * Fetches data from the {@link MunicipalityOfficerService} and populates the citation table.
     */
    public void generateCitationReport() {
        // Get the citations as an ArrayList from the service
        ArrayList<Citation> citations = municipalityOfficerService.generateCitationReport();
        // Convert the ArrayList to an ObservableList
        ObservableList<Citation> observableCitation = FXCollections.observableArrayList(citations);
        // Set the ObservableList to the TableView
        citationsTable.setItems(observableCitation);
    }
}
