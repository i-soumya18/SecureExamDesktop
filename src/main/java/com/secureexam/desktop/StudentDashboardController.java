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
import java.util.ArrayList;
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
            examListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                startExamButton.setDisable(newVal == null || newVal.equals("No exams available")));

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
            stage.setResizable(true);
            stage.setOnCloseRequest(null);
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
                updateExamList(FXCollections.observableArrayList("No exams available"), 0);
                return;
            }

            String stream = userAttributes.get("stream");
            String branch = userAttributes.get("branch");
            String course = userAttributes.get("course");
            String className = userAttributes.get("class");
            String section = userAttributes.get("section");

            List<QueryDocumentSnapshot> exams;
            if (NetworkManager.isOnline()) {
                exams = db.collection("exams")
                    .whereEqualTo("stream", stream)
                    .whereEqualTo("branch", branch)
                    .whereEqualTo("course", course)
                    .whereEqualTo("class", className)
                    .whereEqualTo("section", section)
                    .get()
                    .get()
                    .getDocuments();
            } else {
                LOGGER.info("Offline mode: Loading exams from cache");
                exams = new ArrayList<>(); // Stub; extend LocalCache if needed
            }

            ObservableList<String> testSeries = FXCollections.observableArrayList();
            examIdMap.clear();
            if (exams.isEmpty()) {
                LOGGER.warning("No exams available for user attributes: " + userAttributes);
                testSeries.add("No exams available");
            } else {
                for (DocumentSnapshot exam : exams) {
                    String examName = exam.getString("name");
                    if (examName != null) {
                        testSeries.add(examName);
                        examIdMap.put(examName, exam.getString("examId"));
                    }
                }
            }
            updateExamList(testSeries, testSeries.size());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error loading exams from Firestore", e);
            showAlert(Alert.AlertType.ERROR, "Exam Load Error", "Failed to load exams: " + e.getMessage());
        }
    }

    private void updateExamList(ObservableList<String> testSeries, int count) {
        Platform.runLater(() -> {
            examListView.setItems(testSeries);
            examCountLabel.setText(count + " exams");
        });
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
            List<QueryDocumentSnapshot> examDocs = db.collection("exams")
                .whereEqualTo("name", selectedTestSeries)
                .whereEqualTo("stream", userAttributes.get("stream"))
                .whereEqualTo("branch", userAttributes.get("branch"))
                .whereEqualTo("course", userAttributes.get("course"))
                .whereEqualTo("class", userAttributes.get("class"))
                .whereEqualTo("section", userAttributes.get("section"))
                .get()
                .get()
                .getDocuments();

            if (examDocs.isEmpty()) {
                throw new IllegalStateException("Exam not found for: " + selectedTestSeries);
            }

            DocumentSnapshot examDoc = examDocs.get(0);
            String correctCode = examDoc.getString("code");
            if (!enteredCode.equals(correctCode)) {
                logFailedAttempt(selectedTestSeries, enteredCode);
                showAlert(Alert.AlertType.ERROR, "Exam Code Error", "Invalid exam code. Please try again.");
                return;
            }

            String examId = examDoc.getString("examId");
            NetworkManager.disableInternet(); // Cut off internet
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exam.fxml"));
            Parent root = loader.load();
            ExamController examController = loader.getController();
            examController.setExamDetails(selectedTestSeries, examId, correctCode, userAttributes.get("email"));
            Stage stage = (Stage) startExamButton.getScene().getWindow();
            Scene newScene = new Scene(root);
            stage.setScene(newScene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setOnCloseRequest(e -> e.consume());
            LOGGER.info("Started exam: " + selectedTestSeries + " with examId: " + examId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading exam FXML", e);
            showAlert(Alert.AlertType.ERROR, "Exam Error", "Failed to start exam: " + e.getMessage());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error querying exam data", e);
            showAlert(Alert.AlertType.ERROR, "Exam Error", "Failed to start exam: " + e.getMessage());
        }
    }

    private void logFailedAttempt(String examName, String enteredCode) {
        try {
            String idToken = LoginController.getIdToken();
            String uid = (idToken != null && !idToken.isEmpty()) ?
                FirebaseAuth.getInstance().verifyIdToken(idToken).getUid() : "unknown";
            db.collection("audit_logs").document().set(
                new HashMap<String, Object>() {{
                    put("uid", uid);
                    put("event", "exam_code_validation_failed");
                    put("examName", examName);
                    put("enteredCode", enteredCode);
                    put("timestamp", System.currentTimeMillis());
                }}
            ).get();
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
                    fadeIn.setOnFinished(e -> {
                        stage.setScene(newScene);
                        stage.setResizable(true);
                        stage.setOnCloseRequest(null);
                    });
                    fadeIn.play();
                    LOGGER.info("User logged out successfully");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during logout", e);
                    showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to logout: " + e.getMessage());
                }
            }
        });
    }

    @FXML private void handleNotifications(ActionEvent event) { showFeatureAlert("Notifications"); }
    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student_profile.fxml"));
            Parent root = loader.load();

            StudentProfileController controller = loader.getController();
            controller.setUserAttributes(userAttributes);

            Stage stage = (Stage) userMenuButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            LOGGER.info("Navigated to student profile view");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading profile view", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open profile: " + e.getMessage());
        }
    }
    @FXML private void handleSettings(ActionEvent event) { showFeatureAlert("Settings"); }
    @FXML private void handleMyExams(ActionEvent event) { refreshExamList(); showAlert(Alert.AlertType.INFORMATION, "My Exams", "Displaying your available exams."); }
    @FXML private void handleResults(ActionEvent event) { updateResultsVisibility(); showAlert(Alert.AlertType.INFORMATION, "Results", "Recent results displayed below."); }
    @FXML private void handleCalendar(ActionEvent event) { showFeatureAlert("Calendar"); }
    @FXML private void handleHelp(ActionEvent event) { showAlert(Alert.AlertType.INFORMATION, "Help", "Contact support at support@secureexam.com."); }
    @FXML private void handleRefresh(ActionEvent event) { refreshExamList(); }
    @FXML private void handleViewAllResults(ActionEvent event) { showFeatureAlert("Results"); }
    @FXML private void handleViewResult(ActionEvent event) { showFeatureAlert("Result Details"); }

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

    private void showFeatureAlert(String feature) {
        LOGGER.info(feature + " feature clicked");
        showAlert(Alert.AlertType.INFORMATION, feature, feature + " view under development.");
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
