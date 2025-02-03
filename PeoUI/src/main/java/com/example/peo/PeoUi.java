package com.example.peo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application for the Parking Enforcement Officer (PEO) interface of the Mulligan Parking System.
 * This class is responsible for initializing the PEO user interface and starting the JavaFX application.
 * Usage: Run this class to launch the PEO interface.
 * Example: {@code java PeoUi}
 * The interface is defined in the {@code peoView.fxml} file located in the specified resources directory.
 */
public class PeoUi extends Application {

    /**
     * Starts the JavaFX application and sets up the PEO UI stage.
     * @param primaryStage The primary stage for this application.
     * @throws Exception If there is an error loading the FXML file or initializing the UI.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/peoView.fxml"));

        // Create a Scene object from the loaded FXML
        Scene scene = new Scene(loader.load());

        // Add the CSS file to the Scene
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set the Scene to the Stage
        primaryStage.setScene(scene);

        // Set the window title
        primaryStage.setTitle("Mulligan Parking System - Parking Enforcement Officer UI");
        primaryStage.setWidth(500);  // Narrow width
        primaryStage.setHeight(500); // Taller height
        // Show the primary stage
        primaryStage.show();
    }
    /**
     * The main entry point for the application.
     * Launches the JavaFX application.
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
