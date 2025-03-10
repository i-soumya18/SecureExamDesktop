package com.secureexam.desktop;

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

    private ScheduledExecutorService tokenRefreshScheduler;

    @FXML
    private void initialize() {
        try {
            // Set up exam list
            ObservableList<String> testSeries = FXCollections.observableArrayList(TestManager.getTestSeries());
            if (testSeries == null || testSeries.isEmpty()) {
                LOGGER.warning("No test series available; showing empty list");
                testSeries = FXCollections.observableArrayList("No exams available");
            }
            examListView.setItems(testSeries);
            examCountLabel.setText(testSeries.size() + " exams");

            // Enable/disable Start Exam button based on selection
            examListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                startExamButton.setDisable(newVal == null || newVal.equals("No exams available"));
            });

            // Populate sort and filter options
            sortComboBox.setItems(FXCollections.observableArrayList("Date", "Name", "Difficulty"));
            filterComboBox.setItems(FXCollections.observableArrayList("All", "Upcoming", "Completed"));

            // Set user name (placeholder; fetch from Firebase in future)
            userMenuButton.setText("John Doe");

            // Token refresh scheduler
            tokenRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
            tokenRefreshScheduler.scheduleAtFixedRate(() -> {
                Stage stage = (Stage) examListView.getScene().getWindow();
                LoginController.refreshToken(stage, () -> LOGGER.info("Token refreshed successfully"));
            }, 50, 50, TimeUnit.MINUTES);

            // Maintain fullscreen lockdown
            Stage stage = (Stage) examListView.getScene().getWindow();
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setOnCloseRequest(e -> {
                LOGGER.warning("Attempted window close blocked");
                e.consume();
            });

            // Update results visibility (placeholder)
            updateResultsVisibility();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load dashboard: " + e.getMessage());
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exam.fxml"));
            Parent root = loader.load();
            ExamController examController = loader.getController();
            examController.setTestSeries(selectedTestSeries);
            Stage stage = (Stage) startExamButton.getScene().getWindow();
            Scene newScene = new Scene(root);
            stage.setScene(newScene);
            // Fullscreen already enforced; ExamController will maintain lockdown
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading exam interface", e);
            showAlert(Alert.AlertType.ERROR, "Exam Error", "Failed to start exam: " + e.getMessage());
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
                    stage.setFullScreen(false); // Release fullscreen
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
        // Placeholder: Refresh exam list (future: filter to user's exams)
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
        try {
            ObservableList<String> testSeries = FXCollections.observableArrayList(TestManager.getTestSeries());
            if (testSeries == null || testSeries.isEmpty()) {
                LOGGER.warning("No test series available on refresh");
                testSeries = FXCollections.observableArrayList("No exams available");
            }
            examListView.setItems(testSeries);
            examCountLabel.setText(testSeries.size() + " exams");
            updateResultsVisibility();
            showAlert(Alert.AlertType.INFORMATION, "Refresh", "Exam list refreshed successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing exam list", e);
            showAlert(Alert.AlertType.ERROR, "Refresh Error", "Failed to refresh exams: " + e.getMessage());
        }
    }

    private void updateResultsVisibility() {
        // Placeholder logic; replace with backend data post-MVP
        boolean hasResults = false; // Simulate no results for now
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

    // Cleanup method for app shutdown (optional, call from MainApp if exists)
    public void shutdown() {
        if (tokenRefreshScheduler != null && !tokenRefreshScheduler.isShutdown()) {
            tokenRefreshScheduler.shutdownNow();
            LOGGER.info("StudentDashboardController shutdown complete");
        }
    }
}