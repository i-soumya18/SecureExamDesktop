package com.secureexam.desktop;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminController {
    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());
    private Firestore db;

    @FXML private TextField teacherEmail;
    @FXML private TextField studentEmail, studentReg, studentStream, studentBranch, studentCourse, studentClass, studentSection;
    @FXML private Button assignExaminer, addStudent, logoutButton;
    @FXML private Label feedbackLabel;

    @FXML
    private void initialize() {
        db = FirestoreClient.getFirestore();
        feedbackLabel.setText("");
    }

    @FXML
    private void handleAssignExaminer(ActionEvent event) {
        String email = teacherEmail.getText().trim();
        if (email.isEmpty()) {
            showFeedback("Please enter a teacher email or phone.", true);
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("role", "examiner");
            data.put("email", email);
            db.collection("users").document(email).set(data);
            LOGGER.info("Assigned examiner role to " + email);
            logAudit("assign_examiner", email);
            showFeedback("Examiner role assigned to " + email + " successfully.", false);
            teacherEmail.clear();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to assign examiner role", e);
            showFeedback("Failed to assign examiner role: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleAddStudent(ActionEvent event) {
        String email = studentEmail.getText().trim();
        String regNumber = studentReg.getText().trim();
        String stream = studentStream.getText().trim();
        String branch = studentBranch.getText().trim();
        String course = studentCourse.getText().trim();
        String className = studentClass.getText().trim();
        String section = studentSection.getText().trim();

        if (email.isEmpty() || regNumber.isEmpty() || stream.isEmpty()) {
            showFeedback("Email, Registration Number, and Stream are required.", true);
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("role", "student");
            data.put("email", email);
            data.put("reg_number", regNumber);
            data.put("stream", stream);
            data.put("branch", branch.isEmpty() ? null : branch);
            data.put("course", course.isEmpty() ? null : course);
            data.put("class", className.isEmpty() ? null : className);
            data.put("section", section.isEmpty() ? null : section);

            db.collection("users").document(email).set(data);
            LOGGER.info("Added student " + email + " with reg_number " + regNumber);
            logAudit("add_student", email);
            showFeedback("Student " + email + " added successfully.", false);
            clearStudentFields();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add student", e);
            showFeedback("Failed to add student: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    LoginController.signOut();
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                    Stage stage = (Stage) logoutButton.getScene().getWindow();
                    stage.setFullScreen(false);
                    stage.setScene(new Scene(root, 800, 600));
                    LOGGER.info("Admin logged out successfully");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during logout", e);
                    showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to logout: " + e.getMessage());
                }
            }
        });
    }

    private void logAudit(String action, String target) {
        try {
            String idToken = LoginController.getIdToken();
            String uid = (idToken != null && !idToken.isEmpty()) ?
                    FirebaseAuth.getInstance().verifyIdToken(idToken).getUid() : "unknown";
            db.collection("audit_logs").document().set(
                    new AuditLog(uid, action, target, null, System.currentTimeMillis())
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to log audit event", e);
        }
    }

    private void showFeedback(String message, boolean isError) {
        Platform.runLater(() -> {
            feedbackLabel.setText(message);
            feedbackLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        });
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

    private void clearStudentFields() {
        studentEmail.clear();
        studentReg.clear();
        studentStream.clear();
        studentBranch.clear();
        studentCourse.clear();
        studentClass.clear();
        studentSection.clear();
    }
}