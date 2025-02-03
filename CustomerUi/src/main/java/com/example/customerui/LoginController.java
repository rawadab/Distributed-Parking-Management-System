package com.example.customerui;


import com.example.queries.Server;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for managing the Login UI in the Mulligan Parking System.
 * This class handles user inputs, communicates with the server for authentication,
 * and manages the transition to the customer interface upon successful login.
 *
 * **Features:**
 * - Validates user input (Customer ID and Password).
 * - Authenticates user credentials with the server.
 * - Displays error messages for invalid input or failed authentication.
 * - Provides a callback mechanism to handle post-login actions.
 *
 * **FXML Components:**
 * - `usernameField`: Input field for the Customer ID.
 * - `passwordField`: Input field for the password.
 * - `loginButton`: Button for triggering the login process.
 * - `errorLabel`: Label for displaying error messages.
 *
 * **Dependencies:**
 * - Uses `Server` class for authentication.
 * - Implements a callback mechanism to handle successful login events.
 */
public class LoginController {

    @FXML
    private TextField usernameField; // Input field for the Customer ID.

    @FXML
    private PasswordField passwordField; // Input field for the password.

    @FXML
    private Button loginButton; // Button for triggering the login process.

    @FXML
    private Label errorLabel; // Label for displaying error messages.

    private Server server = new Server(); // Instance of the Server class for authentication.

    private OnLoginSuccess onLoginSuccess; // Callback interface for handling successful login events.

    /**
     * Interface for defining the login success callback.
     */
    public interface OnLoginSuccess {
        /**
         * Method to handle actions after a successful login.
         *
         * @param customerId   The authenticated customer's ID.
         * @param customerName The authenticated customer's name.
         */
        void handle(String customerId, String customerName);
    }

    /**
     * Sets the login success callback.
     *
     * @param onLoginSuccess The callback to handle successful login events.
     */
    public void setOnLoginSuccess(OnLoginSuccess onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess; // Save the callback for later use.
    }

    /**
     * Initializes the controller and sets the login button's action event.
     */
    public void initialize() {
        loginButton.setOnAction(event -> handleLogin()); // Attach the login handler to the button.
    }

    /**
     * Handles the login process when the login button is clicked.
     * - Validates user input.
     * - Authenticates user credentials with the server.
     * - Invokes the success callback if authentication succeeds.
     * - Displays an error message if authentication fails.
     */
    @FXML
    private void handleLogin() {
        String customerId = usernameField.getText(); // Retrieve the Customer ID input.
        String password = passwordField.getText(); // Retrieve the Password input.

        // Validate that both fields are not empty.
        if (customerId.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Customer ID and Password cannot be empty."); // Display error message.
            return;
        }

        // Authenticate the user's credentials using the server.
        String customerName = server.authenticateCustomer(customerId, password);

        if (customerName != null) {
            // If authentication is successful, invoke the success callback.
            if (onLoginSuccess != null) {
                onLoginSuccess.handle(customerId, customerName); // Pass the customer ID and name to the callback.
            }
        } else {
            // If authentication fails, display an error message.
            errorLabel.setText("Invalid Customer ID or Password.");
        }
    }
}
