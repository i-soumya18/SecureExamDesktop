package com.secureexam.desktop;

import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class InstructorDashboardController {
    private static final Logger LOGGER = Logger.getLogger(InstructorDashboardController.class.getName());
    private Firestore db;

    // Exam Creation Fields
    @FXML private TextField examName, examCode, examStream, examBranch, examCourse, examClass, examSection;
    @FXML private TextField questionExamId, questionText, correctAnswer;
    @FXML private TextArea optionsTextArea; // Replaced individual option fields with a TextArea for flexible options
    @FXML private TextField csvExamIdField;

    // UI Elements for Added Features
    @FXML private TabPane dashboardTabs;
    @FXML private TableView<StudentResult> resultTable;
    @FXML private PieChart performanceChart;
    @FXML private ListView<String> activeStudentsList;
    @FXML private TextField evaluationExamId, passingScoreField;
    @FXML private Button pauseExamButton, resumeExamButton, endExamButton, evaluateButton, exportResultsButton;

    @FXML private Button createExam, addQuestion, logoutButton, generateQuestionCSVButton, uploadQuestionCSVButton;
    @FXML private Label feedbackLabel;

    private Map<String, String> userAttributes;
    private ObservableList<StudentResult> studentResults = FXCollections.observableArrayList();
    private ObservableList<String> activeStudents = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        db = FirestoreClient.getFirestore();
        feedbackLabel.setText("");
        setupResultTable();
        // Defer loading until userAttributes is set
    }

    public void setUserAttributes(Map<String, String> attributes) {
        this.userAttributes = attributes;
        if (this.userAttributes != null) {
            loadAnalytics();
            loadResults();
            monitorActiveStudents();
            LOGGER.info("User attributes set and data loaded for instructor: " + userAttributes.get("email"));
        } else {
            LOGGER.warning("User attributes were null when setUserAttributes was called");
            showFeedback("User data not available. Please re-login.", true);
        }
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
            examData.put("status", "active");

            db.collection("exams").document(examId).set(examData).get();
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
        String optionsInput = optionsTextArea.getText().trim();
        String correct = correctAnswer.getText().trim();

        if (examId.isEmpty() || text.isEmpty() || optionsInput.isEmpty() || correct.isEmpty()) {
            showFeedback("All question fields are required.", true);
            return;
        }

        List<String> options = Arrays.stream(optionsInput.split("\n"))
                .map(String::trim)
                .filter(opt -> !opt.isEmpty())
                .collect(Collectors.toList());

        if (options.size() < 2) {
            showFeedback("At least two options are required.", true);
            return;
        }

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

            examRef.update("questions", questions).get();
            LOGGER.info("Added question to examId " + examId + " with " + options.size() + " options");
            logAudit("add_question", examId);
            showFeedback("Question added to exam " + examId + " successfully.", false);
            clearQuestionFields();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add question", e);
            showFeedback("Failed to add question: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleGenerateQuestionCSV(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Question CSV Template");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("question_template.csv");
            File file = fileChooser.showSaveDialog(generateQuestionCSVButton.getScene().getWindow());

            if (file == null) return;

            String examId = csvExamIdField.getText().trim();
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                String[] headers = {"examId", "questionText", "options", "correctAnswer"};
                writer.writeNext(headers);
                if (!examId.isEmpty()) {
                    String[] sampleRow = {examId, "Sample question", "Option1\nOption2\nOption3", "Option1"};
                    writer.writeNext(sampleRow);
                }
            }
            showFeedback("Question CSV template generated successfully at " + file.getAbsolutePath(), false);
            LOGGER.info("Generated question CSV template at: " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate question CSV", e);
            showFeedback("Failed to generate question CSV: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleUploadQuestionCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Question CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(uploadQuestionCSVButton.getScene().getWindow());

        if (file == null) return;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext();
            if (headers == null || !Arrays.asList(headers).containsAll(Arrays.asList("examId", "questionText", "options", "correctAnswer"))) {
                showFeedback("Invalid CSV format: 'examId', 'questionText', 'options', 'correctAnswer' columns required.", true);
                return;
            }

            String[] line;
            int addedCount = 0;
            int lineNumber = 1;
            List<String> errors = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                lineNumber++;
                Map<String, String> questionData = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    questionData.put(headers[i], line[i].trim());
                }

                String examId = questionData.get("examId");
                String text = questionData.get("questionText");
                String optionsStr = questionData.get("options");
                String correct = questionData.get("correctAnswer");

                if (examId == null || examId.isEmpty() || text == null || text.isEmpty() || optionsStr == null || optionsStr.isEmpty() || correct == null || correct.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Missing required fields.");
                    continue;
                }

                List<String> options = Arrays.stream(optionsStr.split("\n"))
                        .map(String::trim)
                        .filter(opt -> !opt.isEmpty())
                        .collect(Collectors.toList());

                if (options.size() < 2) {
                    errors.add("Line " + lineNumber + ": At least two options are required.");
                    continue;
                }

                if (!options.contains(correct)) {
                    errors.add("Line " + lineNumber + ": Correct answer must match one of the options.");
                    continue;
                }

                try {
                    DocumentReference examRef = db.collection("exams").document(examId);
                    DocumentSnapshot examDoc = examRef.get().get();
                    if (!examDoc.exists()) {
                        errors.add("Line " + lineNumber + ": Exam ID " + examId + " does not exist.");
                        continue;
                    }

                    Map<String, Object> question = new HashMap<>();
                    question.put("text", text);
                    question.put("options", options);
                    question.put("correctAnswer", correct);

                    List<Map<String, Object>> questions = (List<Map<String, Object>>) examDoc.get("questions");
                    if (questions == null) questions = new ArrayList<>();
                    questions.add(question);

                    examRef.update("questions", questions).get();
                    logAudit("add_question_bulk", examId);
                    addedCount++;
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": Failed to add question - " + e.getMessage());
                }
            }

            StringBuilder feedback = new StringBuilder("Added " + addedCount + " questions successfully.");
            if (!errors.isEmpty()) {
                feedback.append("\nErrors:\n").append(String.join("\n", errors));
                showFeedback(feedback.toString(), true);
            } else {
                showFeedback(feedback.toString(), false);
            }
            LOGGER.info("Uploaded " + addedCount + " questions from CSV: " + file.getAbsolutePath());
        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Failed to upload question CSV", e);
            showFeedback("Failed to upload question CSV: " + e.getMessage(), true);
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
                    stage.setResizable(true);
                    stage.setOnCloseRequest(null);
                    LOGGER.info("Instructor logged out successfully");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during logout", e);
                    showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to logout: " + e.getMessage());
                }
            }
        });
    }

    // Analytical Tool
    private void loadAnalytics() {
        if (userAttributes == null || userAttributes.get("email") == null) {
            LOGGER.warning("Cannot load analytics: userAttributes is null or email missing");
            showFeedback("User data not available for analytics.", true);
            return;
        }
        try {
            List<QueryDocumentSnapshot> exams = db.collection("exams")
                    .whereEqualTo("createdBy", userAttributes.get("email"))
                    .get().get().getDocuments();
            int totalExams = exams.size();
            int completed = 0, inProgress = 0;

            for (QueryDocumentSnapshot exam : exams) {
                String status = exam.getString("status");
                if ("completed".equals(status)) completed++;
                else if ("active".equals(status)) inProgress++;
            }

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Completed", completed),
                    new PieChart.Data("In Progress", inProgress),
                    new PieChart.Data("Not Started", totalExams - completed - inProgress)
            );
            Platform.runLater(() -> performanceChart.setData(pieChartData));
            LOGGER.info("Loaded analytics for instructor: " + userAttributes.get("email"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load analytics", e);
            showFeedback("Failed to load analytics: " + e.getMessage(), true);
        }
    }

    // Result Display
    private void setupResultTable() {
        TableColumn<StudentResult, String> studentIdCol = new TableColumn<>("Student ID");
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<StudentResult, String> examIdCol = new TableColumn<>("Exam ID");
        examIdCol.setCellValueFactory(new PropertyValueFactory<>("examId"));

        TableColumn<StudentResult, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));

        TableColumn<StudentResult, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));

        resultTable.getColumns().addAll(studentIdCol, examIdCol, scoreCol, gradeCol);
        resultTable.setItems(studentResults);
    }

    private void loadResults() {
        if (userAttributes == null || userAttributes.get("email") == null) {
            LOGGER.warning("Cannot load results: userAttributes is null or email missing");
            showFeedback("User data not available for results.", true);
            return;
        }
        try {
            List<QueryDocumentSnapshot> submissions = db.collection("submissions")
                    .whereEqualTo("testSeries", userAttributes.get("email"))
                    .get().get().getDocuments();
            studentResults.clear();
            for (QueryDocumentSnapshot submission : submissions) {
                String studentId = submission.getString("studentId");
                String examId = submission.getString("examId");
                Long score = submission.getLong("score");
                Long maxScore = submission.getLong("maxScore");
                String grade = submission.getString("grade") != null ? submission.getString("grade") : "Not Evaluated";
                studentResults.add(new StudentResult(
                        studentId,
                        examId,
                        score + "/" + maxScore,
                        grade
                ));
            }
            LOGGER.info("Loaded " + studentResults.size() + " results for instructor");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load results", e);
            showFeedback("Failed to load results: " + e.getMessage(), true);
        }
    }

    // Monitoring of Students
    private void monitorActiveStudents() {
        if (userAttributes == null || userAttributes.get("email") == null) {
            LOGGER.warning("Cannot monitor students: userAttributes is null or email missing");
            showFeedback("User data not available for monitoring.", true);
            return;
        }
        try {
            db.collection("exam_sessions")
                    .whereEqualTo("instructorEmail", userAttributes.get("email"))
                    .whereEqualTo("status", "active")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            LOGGER.log(Level.SEVERE, "Failed to monitor students", e);
                            showFeedback("Monitoring failed: " + e.getMessage(), true);
                            return;
                        }
                        activeStudents.clear();
                        if (snapshots != null) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                activeStudents.add(doc.getString("studentId") + " - " + doc.getString("examId"));
                            }
                        }
                        Platform.runLater(() -> activeStudentsList.setItems(activeStudents));
                        LOGGER.info("Monitoring " + activeStudents.size() + " active students");
                    });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize student monitoring", e);
            showFeedback("Monitoring setup failed: " + e.getMessage(), true);
        }
    }

    // Marks & Evaluation Methods
    @FXML
    private void handleEvaluateExam(ActionEvent event) {
        String examId = evaluationExamId.getText().trim();
        String passingScoreStr = passingScoreField.getText().trim();

        if (examId.isEmpty() || passingScoreStr.isEmpty()) {
            showFeedback("Exam ID and Passing Score are required.", true);
            return;
        }

        try {
            int passingScore = Integer.parseInt(passingScoreStr);
            List<QueryDocumentSnapshot> submissions = db.collection("submissions")
                    .whereEqualTo("examId", examId)
                    .whereEqualTo("testSeries", userAttributes.get("email"))
                    .get().get().getDocuments();

            for (QueryDocumentSnapshot submission : submissions) {
                long score = submission.getLong("score");
                String grade = (score >= passingScore) ? "Pass" : "Fail";
                submission.getReference().update("grade", grade).get();
            }
            loadResults();
            logAudit("evaluate_exam", examId);
            showFeedback("Evaluated " + submissions.size() + " results for exam " + examId, false);
            LOGGER.info("Evaluated exam " + examId + " with passing score " + passingScore);
        } catch (NumberFormatException e) {
            showFeedback("Passing score must be a number.", true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to evaluate exam", e);
            showFeedback("Evaluation failed: " + e.getMessage(), true);
        }
    }

    // Real-Time Exam Control
    @FXML
    private void handlePauseExam(ActionEvent event) {
        String examId = evaluationExamId.getText().trim();
        if (examId.isEmpty()) {
            showFeedback("Enter Exam ID to pause.", true);
            return;
        }
        updateExamStatus(examId, "paused", "Paused exam " + examId);
    }

    @FXML
    private void handleResumeExam(ActionEvent event) {
        String examId = evaluationExamId.getText().trim();
        if (examId.isEmpty()) {
            showFeedback("Enter Exam ID to resume.", true);
            return;
        }
        updateExamStatus(examId, "active", "Resumed exam " + examId);
    }

    @FXML
    private void handleEndExam(ActionEvent event) {
        String examId = evaluationExamId.getText().trim();
        if (examId.isEmpty()) {
            showFeedback("Enter Exam ID to end.", true);
            return;
        }
        updateExamStatus(examId, "completed", "Ended exam " + examId);
    }

    private void updateExamStatus(String examId, String status, String successMessage) {
        try {
            DocumentReference examRef = db.collection("exams").document(examId);
            DocumentSnapshot examDoc = examRef.get().get();
            if (!examDoc.exists()) {
                showFeedback("Exam ID " + examId + " does not exist.", true);
                return;
            }
            examRef.update("status", status).get();
            logAudit("control_exam_" + status, examId);
            showFeedback(successMessage, false);
            LOGGER.info(successMessage);
            loadAnalytics(); // Update analytics after status change
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update exam status", e);
            showFeedback("Failed to " + status + " exam: " + e.getMessage(), true);
        }
    }

    // Export Results to CSV
    @FXML
    private void handleExportResults(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("exam_results.csv");
        File file = fileChooser.showSaveDialog(logoutButton.getScene().getWindow());

        if (file == null) return;

        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            String[] headers = {"Student ID", "Exam ID", "Score", "Grade"};
            writer.writeNext(headers);
            for (StudentResult result : studentResults) {
                writer.writeNext(new String[]{
                        result.getStudentId(), result.getExamId(), result.getScore(), result.getGrade()
                });
            }
            showFeedback("Results exported to " + file.getAbsolutePath(), false);
            LOGGER.info("Exported results to CSV: " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to export results", e);
            showFeedback("Failed to export results: " + e.getMessage(), true);
        }
    }

    // Helper Methods
    private void logAudit(String action, String target) {
        try {
            String idToken = LoginController.getIdToken();
            String uid = (idToken != null && !idToken.isEmpty()) ?
                    FirebaseAuth.getInstance().verifyIdToken(idToken).getUid() : "unknown";
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("uid", uid);
            auditData.put("action", action);
            auditData.put("target", target);
            auditData.put("timestamp", System.currentTimeMillis());
            db.collection("audit_logs").document().set(auditData).get();
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
        optionsTextArea.clear();
        correctAnswer.clear();
    }

    // StudentResult Class
    public static class StudentResult {
        private final SimpleStringProperty studentId;
        private final SimpleStringProperty examId;
        private final SimpleStringProperty score;
        private final SimpleStringProperty grade;

        public StudentResult(String studentId, String examId, String score, String grade) {
            this.studentId = new SimpleStringProperty(studentId);
            this.examId = new SimpleStringProperty(examId);
            this.score = new SimpleStringProperty(score);
            this.grade = new SimpleStringProperty(grade);
        }

        public String getStudentId() { return studentId.get(); }
        public String getExamId() { return examId.get(); }
        public String getScore() { return score.get(); }
        public String getGrade() { return grade.get(); }
    }
}