package com.secureexam.desktop;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminController {
    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());
    private Firestore db;

    @FXML private TextField teacherEmail;
    @FXML private TextField studentEmail, studentReg, studentStream, studentBranch, studentCourse, studentClass, studentSection;
    @FXML private TextField csvStreamField, csvBranchField, csvClassField, csvSectionField;
    @FXML private TextField updateEmailField, updateRoleField;
    @FXML private TextField deleteEmailField;
    @FXML private Button assignExaminer, addStudent, logoutButton, generateStudentCSVButton, uploadStudentCSVButton;
    @FXML private Button updateUserButton, deleteUserButton;
    @FXML private Label feedbackLabel;

    private Map<String, String> userAttributes;

    @FXML
    private void initialize() {
        db = FirestoreClient.getFirestore();
        feedbackLabel.setText("");
        // No full-screen or close restrictions here
    }

    public void setUserAttributes(Map<String, String> attributes) {
        this.userAttributes = attributes;
    }

    // CREATE: Assign Examiner
    @FXML
    private void handleAssignExaminer(ActionEvent event) {
        String email = teacherEmail.getText().trim();
        if (email.isEmpty()) {
            showFeedback("Please enter a teacher email.", true);
            return;
        }

        try {
            DocumentSnapshot userDoc = db.collection("users").document(email).get().get();
            String inviteCode = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("email", email);
            inviteData.put("role", "examiner");
            inviteData.put("used", false);
            db.collection("invites").document(inviteCode).set(inviteData).get();

            if (userDoc.exists()) {
                db.collection("users").document(email).update("role", "examiner").get();
                showFeedback("Examiner role assigned to " + email + ". Invite code: " + inviteCode, false);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("role", "examiner");
                data.put("email", email);
                db.collection("users").document(email).set(data).get();
                showFeedback("New examiner " + email + " added. Invite code: " + inviteCode, false);
            }

            LOGGER.info("Assigned examiner role to " + email);
            logAudit("assign_examiner", email);
            teacherEmail.clear();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to assign examiner role", e);
            showFeedback("Failed to assign examiner role: " + e.getMessage(), true);
        }
    }

    // CREATE: Add Student
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
            DocumentSnapshot userDoc = db.collection("users").document(email).get().get();
            if (userDoc.exists()) {
                showFeedback("User " + email + " already exists.", true);
                return;
            }

            String inviteCode = UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("email", email);
            inviteData.put("role", "student");
            inviteData.put("used", false);
            db.collection("invites").document(inviteCode).set(inviteData).get();

            Map<String, Object> data = new HashMap<>();
            data.put("role", "student");
            data.put("email", email);
            data.put("reg_number", regNumber);
            data.put("stream", stream);
            data.put("branch", branch.isEmpty() ? null : branch);
            data.put("course", course.isEmpty() ? null : course);
            data.put("class", className.isEmpty() ? null : className);
            data.put("section", section.isEmpty() ? null : section);

            db.collection("users").document(email).set(data).get();
            LOGGER.info("Added student " + email + " with reg_number " + regNumber);
            logAudit("add_student", email);
            showFeedback("Student " + email + " added successfully. Invite code: " + inviteCode, false);
            clearStudentFields();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to add student", e);
            showFeedback("Failed to add student: " + e.getMessage(), true);
        }
    }

    // READ: List Users (for future UI expansion, stubbed here)
    private List<Map<String, Object>> listUsers() {
        try {
            List<QueryDocumentSnapshot> docs = db.collection("users").get().get().getDocuments();
            List<Map<String, Object>> users = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                users.add(doc.getData());
            }
            LOGGER.info("Retrieved " + users.size() + " users");
            return users;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list users", e);
            showFeedback("Failed to list users: " + e.getMessage(), true);
            return new ArrayList<>();
        }
    }

    // UPDATE: Update User Role
    @FXML
    private void handleUpdateUser(ActionEvent event) {
        String email = updateEmailField.getText().trim();
        String newRole = updateRoleField.getText().trim();

        if (email.isEmpty() || newRole.isEmpty()) {
            showFeedback("Email and new role are required.", true);
            return;
        }

        try {
            DocumentSnapshot userDoc = db.collection("users").document(email).get().get();
            if (!userDoc.exists()) {
                showFeedback("User " + email + " does not exist.", true);
                return;
            }

            db.collection("users").document(email).update("role", newRole).get();
            LOGGER.info("Updated role for " + email + " to " + newRole);
            logAudit("update_user_role", email + " to " + newRole);
            showFeedback("Updated role for " + email + " to " + newRole, false);
            updateEmailField.clear();
            updateRoleField.clear();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update user role", e);
            showFeedback("Failed to update user role: " + e.getMessage(), true);
        }
    }

    // DELETE: Delete User
    @FXML
    private void handleDeleteUser(ActionEvent event) {
        String email = deleteEmailField.getText().trim();

        if (email.isEmpty()) {
            showFeedback("Email is required.", true);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete " + email + "?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    DocumentSnapshot userDoc = db.collection("users").document(email).get().get();
                    if (!userDoc.exists()) {
                        showFeedback("User " + email + " does not exist.", true);
                        return;
                    }

                    UserRecord user = FirebaseAuth.getInstance().getUserByEmail(email);
                    db.collection("users").document(email).delete().get();
                    FirebaseAuth.getInstance().deleteUser(user.getUid());
                    LOGGER.info("Deleted user " + email);
                    logAudit("delete_user", email);
                    showFeedback("Deleted user " + email + " successfully.", false);
                    deleteEmailField.clear();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to delete user", e);
                    showFeedback("Failed to delete user: " + e.getMessage(), true);
                }
            }
        });
    }

    // Generate Student CSV Template
    @FXML
    private void handleGenerateStudentCSV(ActionEvent event) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Student CSV Template");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("student_template.csv");
            File file = fileChooser.showSaveDialog(generateStudentCSVButton.getScene().getWindow());

            if (file == null) return;

            String stream = csvStreamField.getText().trim();
            String branch = csvBranchField.getText().trim();
            String className = csvClassField.getText().trim();
            String section = csvSectionField.getText().trim();

            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                String[] headers = {"email", "reg_number", "stream", "branch", "course", "class", "section"};
                writer.writeNext(headers);
                if (!stream.isEmpty() && !branch.isEmpty() && !className.isEmpty() && !section.isEmpty()) {
                    String[] sampleRow = {"", "", stream, branch, "", className, section};
                    writer.writeNext(sampleRow);
                }
            }
            showFeedback("Student CSV template generated successfully at " + file.getAbsolutePath(), false);
            LOGGER.info("Generated student CSV template at: " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate student CSV", e);
            showFeedback("Failed to generate student CSV: " + e.getMessage(), true);
        }
    }

    // Bulk CREATE: Upload Student CSV
    @FXML
    private void handleUploadStudentCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Student CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(uploadStudentCSVButton.getScene().getWindow());

        if (file == null) return;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext();
            if (headers == null || !Arrays.asList(headers).containsAll(Arrays.asList("email", "reg_number", "stream"))) {
                showFeedback("Invalid CSV format: 'email', 'reg_number', and 'stream' columns are required.", true);
                return;
            }

            String[] line;
            int addedCount = 0;
            int lineNumber = 1;
            List<String> errors = new ArrayList<>();

            while ((line = reader.readNext()) != null) {
                lineNumber++;
                Map<String, String> studentData = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    studentData.put(headers[i], line[i].trim());
                }

                String email = studentData.get("email");
                String regNumber = studentData.get("reg_number");
                String stream = studentData.get("stream");

                if (email == null || email.isEmpty() || regNumber == null || regNumber.isEmpty() || stream == null || stream.isEmpty()) {
                    errors.add("Line " + lineNumber + ": Missing required fields (email, reg_number, stream).");
                    continue;
                }

                try {
                    DocumentSnapshot userDoc = db.collection("users").document(email).get().get();
                    if (userDoc.exists()) {
                        errors.add("Line " + lineNumber + ": User " + email + " already exists.");
                        continue;
                    }

                    String inviteCode = UUID.randomUUID().toString().substring(0, 8);
                    Map<String, Object> inviteData = new HashMap<>();
                    inviteData.put("email", email);
                    inviteData.put("role", "student");
                    inviteData.put("used", false);
                    db.collection("invites").document(inviteCode).set(inviteData).get();

                    Map<String, Object> data = new HashMap<>();
                    data.put("role", "student");
                    data.put("email", email);
                    data.put("reg_number", regNumber);
                    data.put("stream", stream);
                    data.put("branch", studentData.getOrDefault("branch", ""));
                    data.put("course", studentData.getOrDefault("course", ""));
                    data.put("class", studentData.getOrDefault("class", ""));
                    data.put("section", studentData.getOrDefault("section", ""));

                    db.collection("users").document(email).set(data).get();
                    logAudit("add_student_bulk", email);
                    addedCount++;
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": Failed to add " + email + " - " + e.getMessage());
                }
            }

            StringBuilder feedback = new StringBuilder("Added " + addedCount + " students successfully.");
            if (!errors.isEmpty()) {
                feedback.append("\nErrors:\n").append(String.join("\n", errors));
                showFeedback(feedback.toString(), true);
            } else {
                showFeedback(feedback.toString(), false);
            }
            LOGGER.info("Uploaded " + addedCount + " students from CSV: " + file.getAbsolutePath() + " with " + errors.size() + " errors");
        } catch (IOException | CsvValidationException e) {
            LOGGER.log(Level.SEVERE, "Failed to upload student CSV", e);
            showFeedback("Failed to upload student CSV: " + e.getMessage(), true);
        }
    }

    // Logout
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
                    LOGGER.info("Admin logged out successfully");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during logout", e);
                    showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to logout: " + e.getMessage());
                }
            }
        });
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
            db.collection("audit_logs").document().set(auditData);
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

    // Getters and Setters for Testing
    public Button comprendreExaminer() {
        return assignExaminer;
    }

    public void setAssignExaminer(Button assignExaminer) {
        this.assignExaminer = assignExaminer;
    }

    public Button getAddStudent() {
        return addStudent;
    }

    public void setAddStudent(Button addStudent) {
        this.addStudent = addStudent;
    }
}