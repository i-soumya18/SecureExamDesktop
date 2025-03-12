package com.secureexam.desktop;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudentDashboardController {
    private static final Logger LOGGER = Logger.getLogger(StudentDashboardController.class.getName());

    @FXML private ListView<String> examListView;
    @FXML private Button startExamButton;
    @FXML private Button notificationsButton;
    @FXML private MenuButton userMenuButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Label examCountLabel;
    @FXML private Button refreshButton;
    @FXML private StackPane emptyResultsPlaceholder;
    @FXML private VBox resultCard1;
    @FXML private VBox resultCard2;
    @FXML private TextField examCodeField;

    private ScheduledExecutorService tokenRefreshScheduler;
    private Firestore db;
    private Map<String, String> userAttributes;
    private Map<String, String> examIdMap;

    @FXML
    private void initialize() {
        try {
            db = FirestoreClient.getFirestore();
            examIdMap = new HashMap<>();

            examListView.setItems(FXCollections.observableArrayList());
            examListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                startExamButton.setDisable(newVal == null || newVal.equals("No exams available"));
            });

            sortComboBox.setItems(FXCollections.observableArrayList("Date", "Name", "Difficulty"));
            filterComboBox.setItems(FXCollections.observableArrayList("All", "Upcoming", "Completed"));

            if (userAttributes != null && userAttributes.containsKey("email")) {
                userMenuButton.setText(userAttributes.get("email"));
            } else {
                userMenuButton.setText("John Doe");
            }

            loadExams();

            tokenRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
            tokenRefreshScheduler.scheduleAtFixedRate(() -> {
                Stage stage = (Stage) examListView.getScene().getWindow();
                LoginController.refreshToken(stage, () -> LOGGER.info("Token refreshed successfully"));
            }, 50, 50, TimeUnit.MINUTES);

            Stage stage = (Stage) examListView.getScene().getWindow();
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setOnCloseRequest(e -> {
                LOGGER.warning("Attempted window close blocked");
                e.consume();
            });

            updateResultsVisibility();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load dashboard: " + e.getMessage());
        }
    }

    public void setUserAttributes(Map<String, String> attributes) {
        this.userAttributes = attributes;
    }

    private void loadExams() {
        try {
            if (userAttributes == null) {
                LOGGER.warning("User attributes not set; showing empty exam list");
                examListView.setItems(FXCollections.observableArrayList("No exams available"));
                examCountLabel.setText("0 exams");
                return;
            }

            String stream = userAttributes.get("stream");
            String branch = userAttributes.get("branch");
            String course = userAttributes.get("course");
            String className = userAttributes.get("class");
            String section = userAttributes.get("section");

            List<QueryDocumentSnapshot> exams = db.collection("exams")
                    .whereEqualTo("stream", stream)
                    .whereEqualTo("branch", branch)
                    .whereEqualTo("course", course)
                    .whereEqualTo("class", className)
                    .whereEqualTo("section", section)
                    .get()
                    .get()
                    .getDocuments();

            ObservableList<String> testSeries = FXCollections.observableArrayList();
            examIdMap.clear();
            if (exams.isEmpty()) {
                LOGGER.warning("No exams available for user attributes: " + userAttributes);
                testSeries.add("No exams available");
            } else {
                for (DocumentSnapshot exam : exams) {
                    String examName = exam.getString("name");
                    testSeries.add(examName);
                    examIdMap.put(examName, exam.getString("examId"));
                }
            }
            examListView.setItems(testSeries);
            examCountLabel.setText(testSeries.size() + " exams");
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error loading exams from Firestore", e);
            showAlert(Alert.AlertType.ERROR, "Exam Load Error", "Failed to load exams: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartExam(ActionEvent event) {
        String selectedTestSeries = examListView.getSelectionModel().getSelectedItem();
        if (selectedTestSeries == null || selectedTestSeries.equals("No exams available")) {
            LOGGER.warning("No valid test series selected");
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a valid exam to start.");
            return;
        }

        String enteredCode = examCodeField.getText().trim();
        if (enteredCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Exam Code Error", "Please enter the exam code.");
            return;
        }

        try {
            DocumentSnapshot examDoc = db.collection("exams")
                    .whereEqualTo("name", selectedTestSeries)
                    .whereEqualTo("stream", userAttributes.get("stream"))
                    .whereEqualTo("branch", userAttributes.get("branch"))
                    .whereEqualTo("course", userAttributes.get("course"))
                    .whereEqualTo("class", userAttributes.get("class"))
                    .whereEqualTo("section", userAttributes.get("section"))
                    .get()
                    .get()
                    .getDocuments()
                    .get(0);

            String correctCode = examDoc.getString("code");
            if (!enteredCode.equals(correctCode)) {
                logFailedAttempt(selectedTestSeries, enteredCode);
                showAlert(Alert.AlertType.ERROR, "Exam Code Error", "Invalid exam code. Please try again.");
                return;
            }

            String examId = examDoc.getString("examId");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exam.fxml"));
            Parent root = loader.load();
            ExamController examController = loader.getController();
            examController.setExamDetails(selectedTestSeries, examId, correctCode);
            Stage stage = (Stage) startExamButton.getScene().getWindow();
            Scene newScene = new Scene(root);
            stage.setScene(newScene);
            LOGGER.info("Started exam: " + selectedTestSeries + " with examId: " + examId);
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error starting exam", e);
            showAlert(Alert.AlertType.ERROR, "Exam Error", "Failed to start exam: " + e.getMessage());
        }
    }

    private void logFailedAttempt(String examName, String enteredCode) {
        try {
            String idToken = LoginController.getIdToken();
            String uid = (idToken != null && !idToken.isEmpty()) ?
                    FirebaseAuth.getInstance().verifyIdToken(idToken).getUid() : "unknown";
            db.collection("audit_logs").document().set(
                    new AuditLog(uid, "exam_code_validation_failed", examName, enteredCode, System.currentTimeMillis())
            );
            LOGGER.info("Logged failed exam code attempt for exam: " + examName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to log audit event", e);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    LoginController.signOut();
                    if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
                        tokenRefreshScheduler.shutdownNow();
                        LOGGER.info("Token refresh scheduler shut down");
                    }
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                    Stage stage = (Stage) userMenuButton.getScene().getWindow();
                    stage.setFullScreen(false);
                    Scene newScene = new Scene(root, 800, 600);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setOnFinished(e -> stage.setScene(newScene));
                    fadeIn.play();
                    LOGGER.info("User logged out successfully");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during logout", e);
                    showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to logout: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        LOGGER.info("Notifications button clicked");
        showAlert(Alert.AlertType.INFORMATION, "Notifications", "No new notifications available.");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        LOGGER.info("Profile menu item clicked");
        showAlert(Alert.AlertType.INFORMATION, "Profile", "Profile view under development.");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        LOGGER.info("Settings menu item clicked");
        showAlert(Alert.AlertType.INFORMATION, "Settings", "Settings panel under development.");
    }

    @FXML
    private void handleMyExams(ActionEvent event) {
        LOGGER.info("My Exams sidebar button clicked");
        refreshExamList();
        showAlert(Alert.AlertType.INFORMATION, "My Exams", "Displaying your available exams.");
    }

    @FXML
    private void handleResults(ActionEvent event) {
        LOGGER.info("Results sidebar button clicked");
        updateResultsVisibility();
        showAlert(Alert.AlertType.INFORMATION, "Results", "Recent results displayed below.");
    }

    @FXML
    private void handleCalendar(ActionEvent event) {
        LOGGER.info("Calendar sidebar button clicked");
        showAlert(Alert.AlertType.INFORMATION, "Calendar", "Exam calendar under development.");
    }

    @FXML
    private void handleHelp(ActionEvent event) {
        LOGGER.info("Help sidebar button clicked");
        showAlert(Alert.AlertType.INFORMATION, "Help", "Contact support at support@secureexam.com.");
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        LOGGER.info("Refresh button clicked");
        refreshExamList();
    }

    @FXML
    private void handleViewAllResults(ActionEvent event) {
        LOGGER.info("View All Results hyperlink clicked");
        showAlert(Alert.AlertType.INFORMATION, "Results", "Full results view under development.");
    }

    @FXML
    private void handleViewResult(ActionEvent event) {
        LOGGER.info("View Result button clicked");
        showAlert(Alert.AlertType.INFORMATION, "Result Details", "Result details under development.");
    }

    private void refreshExamList() {
        loadExams();
        showAlert(Alert.AlertType.INFORMATION, "Refresh", "Exam list refreshed successfully.");
    }

    private void updateResultsVisibility() {
        boolean hasResults = false; // Placeholder logic
        emptyResultsPlaceholder.setVisible(!hasResults);
        resultCard1.setVisible(hasResults);
        resultCard2.setVisible(hasResults);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void shutdown() {
        if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler.shutdownNow();
            LOGGER.info("StudentDashboardController shutdown complete");
        }
    }
}

class AuditLog {
    private String userId;
    private String action;
    private String examName;
    private String enteredCode;
    private long timestamp;

    public AuditLog(String userId, String action, String examName, String enteredCode, long timestamp) {
        this.userId = userId;
        this.action = action;
        this.examName = examName;
        this.enteredCode = enteredCode;
        this.timestamp = timestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public String getEnteredCode() { return enteredCode; }
    public void setEnteredCode(String enteredCode) { this.enteredCode = enteredCode; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}