package com.secureexam.desktop;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class StudentDashboardController {

    @FXML
    private ListView<String> examListView;

    @FXML
    private Button startExamButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button notificationsButton;

    @FXML
    private MenuButton userMenuButton;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private Label examCountLabel;

    @FXML
    private Button refreshButton;

    @FXML
    private StackPane emptyResultsPlaceholder;

    @FXML
    private VBox resultCard1;

    @FXML
    private VBox resultCard2;

    @FXML
    private void initialize() {
        ObservableList<String> exams = FXCollections.observableArrayList(
                "Math Exam 101", "Science Exam 102", "History Exam 103"
        );
        examListView.setItems(exams);
        examCountLabel.setText(exams.size() + " exams");

        examListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            startExamButton.setDisable(newVal == null);
        });

        sortComboBox.setItems(FXCollections.observableArrayList("Date", "Name", "Difficulty"));
        filterComboBox.setItems(FXCollections.observableArrayList("All", "Upcoming", "Completed"));
    }

    @FXML
    private void handleStartExam(ActionEvent event) {
        String selectedExam = examListView.getSelectionModel().getSelectedItem();
        if (selectedExam != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exam.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) startExamButton.getScene().getWindow();
                Scene newScene = new Scene(root, 800, 600);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setOnFinished(e -> {
                    stage.setScene(newScene);
                    stage.setFullScreen(true);
                });
                fadeIn.play();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                LoginController.signOut(); // Clear the session
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                    Stage stage = (Stage) logoutButton.getScene().getWindow();
                    Scene newScene = new Scene(root, 800, 600);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setOnFinished(e -> stage.setScene(newScene));
                    fadeIn.play();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void handleNotifications(ActionEvent event) {
        System.out.println("Notifications clicked");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        System.out.println("Profile clicked");
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        System.out.println("Settings clicked");
    }

    @FXML
    private void handleMyExams(ActionEvent event) {
        System.out.println("My Exams clicked");
    }

    @FXML
    private void handleResults(ActionEvent event) {
        System.out.println("Results clicked");
    }

    @FXML
    private void handleCalendar(ActionEvent event) {
        System.out.println("Calendar clicked");
    }

    @FXML
    private void handleHelp(ActionEvent event) {
        System.out.println("Help clicked");
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        System.out.println("Refresh clicked");
    }

    @FXML
    private void handleViewAllResults(ActionEvent event) {
        System.out.println("View All Results clicked");
    }

    @FXML
    private void handleViewResult(ActionEvent event) {
        System.out.println("View Result clicked");
    }
}