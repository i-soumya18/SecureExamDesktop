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
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String API_KEY = "AIzaSyBaO5yvrwKXVHiCU2_UFOI1jiql7Kou7So"; // Replace with your Firebase Web API Key
    private static final OkHttpClient client = new OkHttpClient();
    private static String idToken;
    private static String refreshToken;
    private static ScheduledExecutorService tokenRefreshScheduler;
    private static Firestore db;
    private static FirebaseAuth auth;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button loginButton;
    @FXML private Button googleLoginButton;
    @FXML private Button mfaButton;
    @FXML private CheckBox rememberMeCheckbox;

    // Static initialization for Firebase
    static {
        try {
            FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase/assistant-65908-firebase-adminsdk-w999m-181efe1e50.json");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://assistant-65908.firebaseio.com") // Update with your project ID
                    .build();
            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
            auth = FirebaseAuth.getInstance();
            LOGGER.info("Firebase initialized successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Firebase", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    @FXML
    public void initialize() {
        errorLabel.setText("");
        loadingIndicator.setVisible(false);
        googleLoginButton.setDisable(false);
        mfaButton.setDisable(true); // Enabled after email/password login
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
        googleLoginButton.setDisable(true);

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
                        String uid = result.getString("localId");
                        if (rememberMeCheckbox.isSelected()) {
                            LOGGER.info("Remember me selected; storing refreshToken (placeholder)");
                            // TODO: Encrypt and store refreshToken locally
                        }
                        fetchUserData(uid, email);
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        Platform.runLater(() -> showError("Login failed: " + errorMessage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Login request failed", e);
                Platform.runLater(() -> showError("Login failed: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                    googleLoginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        loadingIndicator.setVisible(true);
        loginButton.setDisable(true);
        googleLoginButton.setDisable(true);

        new Thread(() -> {
            try {
                // Placeholder for Google login (requires OAuth flow in a real app)
                String googleToken = "mock-google-token"; // Replace with actual Google token
                JSONObject json = new JSONObject()
                        .put("postBody", "id_token=" + googleToken + "&providerId=google.com")
                        .put("requestUri", "http://localhost")
                        .put("returnSecureToken", true);
                RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=" + API_KEY)
                        .post(body)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    JSONObject result = new JSONObject(responseBody);
                    if (response.isSuccessful()) {
                        idToken = result.getString("idToken");
                        refreshToken = result.getString("refreshToken");
                        String uid = result.getString("localId");
                        String email = result.getString("email");
                        fetchUserData(uid, email);
                    } else {
                        String errorMessage = result.getJSONObject("error").getString("message");
                        Platform.runLater(() -> showError("Google login failed: " + errorMessage));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Google login failed", e);
                Platform.runLater(() -> showError("Google login failed: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                    googleLoginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleMFA(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter your email for MFA.");
            return;
        }

        loadingIndicator.setVisible(true);
        mfaButton.setDisable(true);

        new Thread(() -> {
            try {
                auth.generateEmailVerificationLink(email, null);
                Platform.runLater(() -> showError("MFA email sent! Check your inbox to verify."));
            } catch (FirebaseAuthException e) {
                LOGGER.log(Level.SEVERE, "MFA email failed", e);
                Platform.runLater(() -> showError("MFA failed: " + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    mfaButton.setDisable(false);
                });
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
                        String uid = result.getString("localId");
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("role", "student");
                        userData.put("email", email);
                        db.collection("users").document(uid).set(userData);
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
            } finally {
                Platform.runLater(() -> {
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
            } finally {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private void fetchUserData(String uid, String email) {
        try {
            DocumentSnapshot doc = db.collection("users").document(uid).get().get();
            Map<String, String> attributes = new HashMap<>();
            attributes.put("email", email);
            String role;
            if (doc.exists()) {
                Map<String, Object> data = doc.getData();
                role = (String) data.getOrDefault("role", "student");
                attributes.put("phone", (String) data.get("phone"));
                attributes.put("reg_number", (String) data.get("reg_number"));
                attributes.put("stream", (String) data.get("stream"));
                attributes.put("branch", (String) data.get("branch"));
                attributes.put("course", (String) data.get("course"));
                attributes.put("class", (String) data.get("class"));
                attributes.put("section", (String) data.get("section"));
            } else {
                Map<String, Object> userData = new HashMap<>();
                userData.put("role", "student");
                userData.put("email", email);
                db.collection("users").document(uid).set(userData);
                role = "student";
            }
            LOGGER.info("User role: " + role + ", attributes: " + attributes);
            Platform.runLater(() -> navigateBasedOnRole(role, attributes));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch user data", e);
            Platform.runLater(() -> showError("Error fetching user data: " + e.getMessage()));
        }
    }

    private void navigateBasedOnRole(String role, Map<String, String> attributes) {
        try {
            FXMLLoader loader;
            Parent root;
            switch (role) {
                case "admin":
                    loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
                    root = loader.load();
                    break;
                case "examiner":
                    loader = new FXMLLoader(getClass().getResource("/fxml/instructor_dashboard.fxml"));
                    root = loader.load();
                    InstructorDashboardController instructorController = loader.getController();
                    instructorController.setUserAttributes(attributes);
                    break;
                case "student":
                default:
                    loader = new FXMLLoader(getClass().getResource("/fxml/student_dashboard.fxml"));
                    root = loader.load();
                    StudentDashboardController studentController = loader.getController();
                    studentController.setUserAttributes(attributes);
                    break;
            }
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

            if (tokenRefreshScheduler == null || tokenRefreshScheduler.isShutdown()) {
                tokenRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
                tokenRefreshScheduler.scheduleAtFixedRate(() -> refreshToken(stage, () -> LOGGER.info("Token refreshed successfully")),
                        50, 50, TimeUnit.MINUTES);
            }
            LOGGER.info("Navigated to " + role + " dashboard successfully");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard", e);
            showError("Error loading dashboard: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        loadingIndicator.setVisible(false);
        loginButton.setDisable(false);
        googleLoginButton.setDisable(false);
        mfaButton.setDisable(true);
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

    public static void shutdown() {
        if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler.shutdownNow();
            LOGGER.info("LoginController shutdown complete");
        }
    }
}