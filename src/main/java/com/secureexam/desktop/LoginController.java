package com.secureexam.desktop;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.util.Duration;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String API_KEY = "AIza*************VHiCU2_UFOI1jiql7Kou7So"; // Firebase Web API Key
    private static final OkHttpClient client = new OkHttpClient();
    private static String idToken;
    private static String refreshToken;
    private static ScheduledExecutorService tokenRefreshScheduler;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheckbox;

    @FXML
    public void initialize() {
        // Clear any previous error messages
        errorLabel.setText("");
        loadingIndicator.setVisible(false);

        // Start token refresh scheduler if not already running
        if (tokenRefreshScheduler == null || tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
            tokenRefreshScheduler.scheduleAtFixedRate(() -> {
                Stage stage = (Stage) loginButton.getScene().getWindow();
                refreshToken(stage, () -> LOGGER.info("Token refreshed successfully"));
            }, 50, 50, TimeUnit.MINUTES); // Refresh every 50 minutes
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .put("returnSecureToken", true);
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        idToken = result.getString("idToken");
                        refreshToken = result.getString("refreshToken");
                        if (rememberMeCheckbox.isSelected()) {
                            LOGGER.info("Remember me selected; storing credentials (placeholder)");
                            // Future: Store encrypted refreshToken locally
                        }
                        Platform.runLater(this::navigateToDashboard);
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        Platform.runLater(() -> showError("Login failed: " + errorMessage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Login request failed", e);
                Platform.runLater(() -> showError("Login failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password to sign up.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject()
                        .put("email", email)
                        .put("password", password)
                        .put("returnSecureToken", true);
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> {
                            showError("Sign-up successful! Please log in.");
                            emailField.clear();
                            passwordField.clear();
                        });
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        Platform.runLater(() -> showError("Sign-up failed: " + errorMessage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Sign-up request failed", e);
                Platform.runLater(() -> showError("Sign-up failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter your email to reset password.");
            return;
        }

        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject()
                        .put("requestType", "PASSWORD_RESET")
                        .put("email", email);
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        Platform.runLater(() -> showError("Password reset email sent! Check your inbox."));
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        Platform.runLater(() -> showError("Reset failed: " + errorMessage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Password reset request failed", e);
                Platform.runLater(() -> showError("Reset failed: " + e.getMessage()));
            }
        }).start();
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_dashboard.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene newScene = new Scene(root);
            stage.setScene(newScene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setOnCloseRequest(e -> {
                LOGGER.warning("Attempted window close blocked");
                e.consume();
            });
            LOGGER.info("Navigated to dashboard successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard", e);
            showError("Error loading dashboard: " + e.getMessage());
        } finally {
            loadingIndicator.setVisible(false);
            loginButton.setDisable(false);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        loadingIndicator.setVisible(false);
        loginButton.setDisable(false);
    }

    public static String getIdToken() {
        return idToken;
    }

    public static void signOut() {
        if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler.shutdownNow();
            LOGGER.info("Token refresh scheduler shut down");
        }
        idToken = null;
        refreshToken = null;
        LOGGER.info("User signed out");
    }

    public static void refreshToken(Stage stage, Runnable onSuccess) {
        if (refreshToken == null) {
            LOGGER.warning("No refresh token available; navigating to login");
            navigateToLogin(stage);
            return;
        }

        new Thread(() -> {
            try {
                JSONObject json = new JSONObject()
                        .put("grant_type", "refresh_token")
                        .put("refresh_token", refreshToken);
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://securetoken.googleapis.com/v1/token?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        idToken = result.getString("id_token");
                        refreshToken = result.getString("refresh_token");
                        LOGGER.info("Token refreshed successfully");
                        Platform.runLater(onSuccess::run);
                    } else {
                        LOGGER.warning("Token refresh failed: " + result.getJSONObject("error").getString("message"));
                        Platform.runLater(() -> navigateToLogin(stage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Token refresh failed", e);
                Platform.runLater(() -> navigateToLogin(stage));
            }
        }).start();
    }

    private static void navigateToLogin(Stage stage) {
        try {
            Parent root = FXMLLoader.load(LoginController.class.getResource("/fxml/login.fxml"));
            Scene newScene = new Scene(root, 800, 600);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(e -> stage.setScene(newScene));
            fadeIn.play();
            LOGGER.info("Navigated back to login due to token refresh failure");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to login", e);
        }
    }

    // Cleanup method for app shutdown (optional, call from MainApp if exists)
    public static void shutdown() {
        if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler.shutdownNow();
            LOGGER.info("LoginController shutdown complete");
        }
    }
}
