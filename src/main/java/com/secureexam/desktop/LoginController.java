package com.secureexam.desktop;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Button loginButton;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
        } else {
            loadingIndicator.setVisible(true);
            loginButton.setDisable(true);

            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Simulate network delay
                    javafx.application.Platform.runLater(() -> {
                        try {
                            errorLabel.setText("");
                            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_dashboard.fxml"));
                            Stage stage = (Stage) loginButton.getScene().getWindow();
                            Scene newScene = new Scene(root, 800, 600);
                            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                            fadeIn.setFromValue(0.0);
                            fadeIn.setToValue(1.0);
                            fadeIn.setOnFinished(e -> stage.setScene(newScene));
                            fadeIn.play();
                        } catch (IOException e) {
                            errorLabel.setText("Error loading dashboard: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // Placeholder for forgot password logic
        errorLabel.setText("Forgot password feature not implemented yet.");
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        // Placeholder for sign-up logic
        errorLabel.setText("Sign-up feature not implemented yet.");
    }
}