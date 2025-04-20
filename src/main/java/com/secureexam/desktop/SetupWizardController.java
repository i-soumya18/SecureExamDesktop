package com.secureexam.desktop;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;

public class SetupWizardController {

    @FXML private TabPane stepTabs;
    @FXML private ProgressBar progressBar;
    @FXML private TextField serviceAccountField;
    @FXML private TextField projectIdField;
    @FXML private TextField mongoConnectionString;
    @FXML private TextField mongoDatabase;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;
    @FXML private Label generalFeedback;

    private int currentStep = 0;
    private static final int TOTAL_STEPS = 5;

    @FXML
    private void handleBack() {
        if (currentStep > 0) {
            currentStep--;
            stepTabs.getSelectionModel().select(currentStep);
            updateButtons();
            updateProgress();
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep < TOTAL_STEPS - 1) {
            if (validateStep(currentStep)) {
                currentStep++;
                stepTabs.getSelectionModel().select(currentStep);
                updateButtons();
                updateProgress();
            }
        }
    }

    @FXML
    private void handleFinish() {
        if (validateStep(currentStep)) {
            saveConfiguration();
            // Logic to close wizard and launch main app
            stepTabs.getScene().getWindow().hide();
        }
    }

    @FXML
    private void handleCancel() {
        stepTabs.getScene().getWindow().hide();
    }

    @FXML
    private void handleBrowseServiceAccount() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(stepTabs.getScene().getWindow());
        if (file != null) {
            serviceAccountField.setText(file.getAbsolutePath());
        }
    }

    private void updateButtons() {
        backButton.setDisable(currentStep == 0);
        nextButton.setDisable(currentStep == TOTAL_STEPS - 1);
        finishButton.setDisable(currentStep != TOTAL_STEPS - 1);
    }

    private void updateProgress() {
        progressBar.setProgress((double) (currentStep + 1) / TOTAL_STEPS);
    }

    private boolean validateStep(int step) {
        switch (step) {
            case 1: // Firebase
                if (serviceAccountField.getText().isEmpty() || projectIdField.getText().isEmpty()) {
                    generalFeedback.setText("Please provide both service account file and project ID.");
                    return false;
                }
                break;
            case 2: // MongoDB
                if (mongoConnectionString.getText().isEmpty() || mongoDatabase.getText().isEmpty()) {
                    generalFeedback.setText("Please provide MongoDB connection string and database name.");
                    return false;
                }
                break;
            case 3: // Preferences
                if (themeComboBox.getValue() == null || languageComboBox.getValue() == null) {
                    generalFeedback.setText("Please select a theme and language.");
                    return false;
                }
                break;
        }
        generalFeedback.setText("");
        return true;
    }

    private void saveConfiguration() {
        // Logic to save settings (e.g., to a config file or database)
        // Example: Save Firebase and MongoDB settings to ConfigLoader or a properties file
    }

    @FXML
    private void initialize() {
        updateButtons();
        updateProgress();
    }
}