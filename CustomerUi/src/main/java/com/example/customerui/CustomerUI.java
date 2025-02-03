package com.example.customerui;


import com.example.shared.utils.RabbitMQUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application class for the Mulligan Parking System's Customer UI.
 *
 * This class handles the initialization of the application's UI and transitions between
 * the Login UI and the Customer UI. It also ensures RabbitMQ queues are initialized before
 * the application starts.
 */
public class CustomerUI extends Application {

    /**
     * Entry point for JavaFX applications. This method is called after the `main` method
     * and is responsible for setting up the primary stage and its initial scene.
     *
     * @param primaryStage the primary stage for this application.
     * @throws Exception if there is an issue loading the FXML files or setting up the scene.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the login UI FXML file.
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/fxml/LoginUI.fxml"));
        Scene loginScene = new Scene(loginLoader.load());
        // Add the stylesheet to the scene
        loginScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setWidth(400);  // Narrow width
        primaryStage.setHeight(600); // Taller height
        primaryStage.setScene(loginScene); // Set the login scene on the primary stage.
        primaryStage.setResizable(true); // Allow resizing if needed
        primaryStage.setTitle("Mulligan Parking System - Login"); // Set the title of the primary stage.

        // Get the LoginController from the FXML loader.
        LoginController loginController = loginLoader.getController();

        // Define the behavior for successful login.
        loginController.setOnLoginSuccess((customerId, customerName) -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerUI.fxml"));
                Scene scene = new Scene(loader.load());
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                primaryStage.setScene(scene);
                primaryStage.setTitle("Mulligan Parking System");
                primaryStage.setWidth(1200);  // Narrow width
                primaryStage.setHeight(800); // Taller height
                primaryStage.setX(150);
                primaryStage.setY(10);
                primaryStage.show();
                // Get the CustomerController from the FXML loader.
                CustomerController customerController = loader.getController();

                // Pass the logged-in customer's details to the CustomerController.
                customerController.setCustomerIdAndVehicleNumber(new String[]{customerId, customerName});

                // Switch to the Customer UI scene and update the stage title.
                primaryStage.setScene(scene);
                primaryStage.setTitle("Mulligan Parking System - Welcome, " + customerName);
            } catch (Exception e) {
                // Print stack trace if an error occurs during scene transition.
                e.printStackTrace();
            }
        });

        // Display the primary stage.
        primaryStage.show();
    }

    /**
     * The main method for launching the JavaFX application.
     *
     * This method initializes RabbitMQ queues for message handling before starting
     * the JavaFX application.
     *
     * @param args the command-line arguments passed to the application.
     */
    public static void main(String[] args) {
        // Initialize RabbitMQ queues to ensure messaging infrastructure is ready.
        RabbitMQUtil rabbitMQUtil = new RabbitMQUtil();
        rabbitMQUtil.initializeQueues();

        // Launch the JavaFX application.
        Application.launch(args);
    }
}
