package com.example.mo;


import com.example.shared.utils.RabbitMQUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application for the Municipality Officer (MO) interface of the Mulligan Parking System.
 * This class is responsible for initializing the MO user interface and starting the JavaFX application.
 * It also initializes RabbitMQ queues for handling message communication within the system.
 * Usage: Run this class to launch the MO interface.
 * Example: {@code java MoUi}
 */
public class MoUi extends Application {

    /**
     * Starts the JavaFX application and sets up the MO UI stage.
     * @param primaryStage The primary stage for this application.
     * @throws Exception If there is an error loading the FXML file or initializing the UI.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MoUi.fxml"));

        // Create a scene and load the CSS file
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Mulligan Parking System - Municipality Officer UI");
        primaryStage.setWidth(800);  // Narrow width
        primaryStage.setHeight(800); // Taller height
        primaryStage.show();
    }
    /**
     * The main entry point for the application.
     * Initializes RabbitMQ queues and launches the JavaFX application.
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        // Initialize RabbitMQ queues
        RabbitMQUtil rabbitMQUtil = new RabbitMQUtil();
        rabbitMQUtil.initializeQueues();
        // Launch the JavaFX application
        Application.launch(args);
    }
}
