package com.secureexam.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("SecureExam Desktop");
            primaryStage.show();
            LOGGER.info("SecureExam Desktop app started successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to launch application", e);
            throw new RuntimeException("Application launch failed", e);
        }
    }

    @Override
    public void stop() {
        LoginController.shutdown();
        LOGGER.info("Application shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}