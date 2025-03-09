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
import okhttp3.*;
import org.json.JSONObject;

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

    private static final String API_KEY = "AIzaSyBaO5yvrwKXVHiCU2_UFOI1jiql7Kou7So"; // Replace with your Firebase Web API Key
    private static final OkHttpClient client = new OkHttpClient();
    private static String idToken; // Store ID token for session management

    @FXML
    public void initialize() {
        // No need for Firebase Admin SDK initialization here; we're using REST API
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject json = new JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .put("returnSecureToken", true);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        idToken = result.getString("idToken");
                        String refreshToken = result.getString("refreshToken"); // Store for token refresh if needed
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
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Login failed: " + errorMessage);
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        });
                    }
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Login failed: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password to sign up.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject json = new JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .put("returnSecureToken", true);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Sign-up successful! Please log in.");
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        });
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Sign-up failed: " + errorMessage);
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        });
                    }
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Sign-up failed: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email to reset password.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                JSONObject json = new JSONObject()
                        .put("requestType", "PASSWORD_RESET")
                        .put("email", email);
                RequestBody body = RequestBody.create(json.toString(), JSON);
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Password reset email sent! Check your inbox.");
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        });
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        javafx.application.Platform.runLater(() -> {
                            errorLabel.setText("Reset failed: " + errorMessage);
                            loadingIndicator.setVisible(false);
                            loginButton.setDisable(false);
                        });
                    }
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    errorLabel.setText("Reset failed: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    // Helper method to get the current ID token (for session management)
    public static String getIdToken() {
        return idToken;
    }

    // Helper method to clear the session (for signout)
    public static void signOut() {
        idToken = null; // Invalidate token locally
        // Note: Firebase REST API doesn't have a direct sign-out endpoint; token is invalidated locally
    }
}