package com.example.peo;


import com.example.queries.PEOService;
import com.example.shared.models.Citation;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller class for managing Parking Enforcement Officer (PEO) actions.
 * Provides functionality for checking parking validity and issuing citations for violations.
 * This class interacts with the {@link PEOService} to handle the business logic
 * and updates the UI with results or error messages.
 * @version 6.0
 */
public class PEOController {

    /** Text field for entering the vehicle number */
    @FXML
    private TextField vehicleNumberField;

    /** Text field for entering the parking space ID */
    @FXML
    private TextField parkingSpaceField;

    /** Text field for entering the citation cost */
    @FXML
    private TextField citationCostField;

    /** Service class for handling PEO operations */
    private final PEOService peoService = new PEOService();

    /**
     * Checks whether the given vehicle is legally parked.
     * If not legally parked, enables citation issuance by pre-filling the citation cost field.
     */
    @FXML
    public void checkParking() {
        String vehicleNumber = vehicleNumberField.getText();
        String parkingSpaceId = parkingSpaceField.getText();

        if (vehicleNumber == null || vehicleNumber.isEmpty() || parkingSpaceId == null || parkingSpaceId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter both Vehicle Number and Parking Space ID.");
            return;
        }

        // Check if the vehicle is valid
        if (!peoService.isVehicleValid(vehicleNumber)) {
            showAlert(Alert.AlertType.ERROR, "Invalid Vehicle", "The vehicle number does not exist in the database.");
            return;
        }

        // Validate parking space
        boolean isParkingSpaceValid = peoService.isParkingSpaceValid(parkingSpaceId);
        if (!isParkingSpaceValid) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "The Parking Space ID does not exist in the database.");
            return;
        }


        // Check parking legality
        boolean isLegallyParked = peoService.checkIfLegallyParked(vehicleNumber, parkingSpaceId);

        if (isLegallyParked) {
            showAlert(Alert.AlertType.INFORMATION, "Parking Status", "Parking Ok: The vehicle is legally parked.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Parking Status", "Parking Not Ok: The vehicle is not legally parked.");
            citationCostField.setDisable(false);
            citationCostField.setPromptText("Enter Citation Cost (NIS)");
            vehicleNumberField.setDisable(true);
            parkingSpaceField.setDisable(true);
            citationCostField.requestFocus();
        }
    }

    /**
     * Issues a citation for a parking violation.
     * Validates the input fields and creates a {@link Citation} object to be issued via {@link PEOService}.
     */
    @FXML
    public void issueCitation() {
        String vehicleNumber = vehicleNumberField.getText();
        String parkingSpaceId = parkingSpaceField.getText();
        String costInput = citationCostField.getText();

        if (vehicleNumber == null || vehicleNumber.isEmpty() || parkingSpaceId == null || parkingSpaceId.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter Vehicle Number, Parking Space ID, and Citation Cost.");
            return;
        }

        if (costInput == null || costInput.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter Citation Cost.");
            return;
        }

        // Check if the citation cost is a valid number
        if (!isValidCitationCost(costInput)) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid citation cost. Please enter a valid positive number.");
            return;
        }

        double citationCost;
        try {
            citationCost = Double.parseDouble(costInput);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Invalid citation cost. Please enter a numeric value.");
            return;
        }

        // Fetch ZoneID for the given SpaceID
        String zoneId = peoService.getZoneIdBySpaceId(parkingSpaceId);
        if (zoneId == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to retrieve ZoneID for the Parking Space ID.");
            return;
        }

        // Create a Citation object
        Citation citation = new Citation(
                peoService.generateUniqueCitationId(),
                vehicleNumber,
                parkingSpaceId,
                zoneId,
                citationCost,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // Issue the citation
        if (peoService.issueCitation(citation)) {
            showAlert(Alert.AlertType.INFORMATION, "Citation Issued", "Citation issued successfully.");
            resetFields();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to issue citation. Please try again.");
        }
    }

    /**
     * Validates if the citation cost is a positive number.
     *
     * @param costInput The input cost as a string.
     * @return {@code true} if the cost is valid; {@code false} otherwise.
     */
    private Boolean isValidCitationCost(String costInput) {
        try {
            double cost = Double.parseDouble(costInput);
            return cost > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Resets the input fields to their default states after a citation is issued.
     */
    private void resetFields() {
        vehicleNumberField.clear();
        parkingSpaceField.clear();
        citationCostField.clear();
        citationCostField.setDisable(true);
        vehicleNumberField.setDisable(false);
        parkingSpaceField.setDisable(false);
    }

    /**
     * Displays an alert dialog with the specified type, title, and message.
     *
     * @param alertType the type of alert (e.g., ERROR, INFORMATION, WARNING)
     * @param title the title of the alert
     * @param message the message to display in the alert
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
