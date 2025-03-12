package com.secureexam.desktop;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstructorDashboardController {
    private static final Logger LOGGER = Logger.getLogger(InstructorDashboardController.class.getName());
    private Firestore db;

    @FXML private TextField examName, examCode, examStream, examBranch, examCourse, examClass, examSection;
    @FXML private TextField questionExamId, questionText, option1, option2, option3, option4, correctAnswer;
    @FXML private Button createExam, addQuestion, logoutButton;
    @FXML private Label feedbackLabel;

    private Map<String, String> userAttributes;

    @FXML
    private void initialize() {
        db = FirestoreClient.getFirestore();
        feedbackLabel.setText("");
    }

    public void setUserAttributes(Map<String, String> attributes) {
        this.userAttributes = attributes;
    }

    @FXML
    private void handleCreateExam(ActionEvent event) {
        String name = examName.getText().trim();
        String code = examCode.getText().trim();
        String stream = examStream.getText().trim();
        String branch = examBranch.getText().trim();
        String course = examCourse.getText().trim();
        String className = examClass.getText().trim();
        String section = examSection.getText().trim();

        if (name.isEmpty() || code.isEmpty() || stream.isEmpty()) {
            showFeedback("Exam Name, Code, and Stream are required.", true);
            return;
        }

        try {
            String examId = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> examData = new HashMap<>();
            examData.put("examId", examId);
            examData.put("name", name);
            examData.put("code", code);
            examData.put("stream", stream);
            examData.put("branch", branch.isEmpty() ? null : branch);
            examData.put("course", course.isEmpty() ? null : course);
            examData.put("class", className.isEmpty() ? null : className);
            examData.put("section", section.isEmpty() ? null : section);
            examData.put("questions", new ArrayList<>());
            examData.put("createdBy", userAttributes != null ? userAttributes.get("email") : "unknown");
            examData.put("createdAt", System.currentTimeMillis());

            db.collection("exams").document(examId).set(examData);
            LOGGER.info("Created exam " + name + " with examId " + examId);
            logAudit("create_exam", name);
            showFeedback("Exam " + name + " created successfully. Exam ID: " + examId, false);
            clearExamFields();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create exam", e);
            showFeedback("Failed to create exam: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleAddQuestion(ActionEvent event) {
        String examId = questionExamId.getText().trim();
        String text = questionText.getText().trim();
        String opt1 = option1.getText().trim();
        String opt2 = option2.getText().trim();
        String opt3 = option3.getText().trim();
        String opt4 = option4.getText().trim();
        String correct = correctAnswer.getText().trim();

        if (examId.isEmpty() || text.isEmpty() || opt1.isEmpty() || opt2.isEmpty() || opt3.isEmpty() || opt4.isEmpty() || correct.isEmpty()) {
            showFeedback("All question fields are required.", true);
            return;
        }

        List<String> options = Arrays.asList(opt1, opt2, opt3, opt4);
        if (!options.contains(correct)) {
            showFeedback("Correct answer must be one of the options.", true);
            return;
        }

        try {
            DocumentReference examRef = db.collection("exams").document(examId);
            DocumentSnapshot examDoc = examRef.get().get();
            if (!examDoc.exists()) {
                showFeedback("Exam ID " + examId + " does not exist.", true);
                return;
            }

            Map<String, Object> questionData = new HashMap<>();
            questionData.put("text", text);
            questionData.put("options", options);
            questionData.put("correctAnswer", correct);

            List<Map<String, Object>> questions = (List<Map<String, Object>>) examDoc.get("questions");
            if (questions == null) questions = new ArrayList<>();
            questions.add(questionData);

            examRef.update("questions", questions);
            LOGGER.info("Added question to examId " + examId);
            logAudit("add_question", examId);
            showFeedback("Question added to exam " + examId + " successfully.", false);
            clearQuestionFields();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add question", e);
            showFeedback("Failed to add question: " + e.getMessage(), true);
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
                    LOGGER.info("Instructor logged out successfully");
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

    private void clearExamFields() {
        examName.clear();
        examCode.clear();
        examStream.clear();
        examBranch.clear();
        examCourse.clear();
        examClass.clear();
        examSection.clear();
    }

    private void clearQuestionFields() {
        questionExamId.clear();
        questionText.clear();
        option1.clear();
        option2.clear();
        option3.clear();
        option4.clear();
        correctAnswer.clear();
    }
}