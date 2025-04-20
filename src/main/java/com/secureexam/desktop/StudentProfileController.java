package com.secureexam.desktop;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the student profile view.
 */
public class StudentProfileController {
    private static final Logger LOGGER = Logger.getLogger(StudentProfileController.class.getName());
    
    // UI Components - Profile Header
    @FXML private ImageView profilePictureView;
    @FXML private Text studentNameText;
    @FXML private Text studentEmailText;
    @FXML private Text studentIdText;
    @FXML private Button changeProfilePictureButton;
    @FXML private Button editProfileButton;
    
    // UI Components - Personal Information
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private TextArea bioTextArea;
    
    // UI Components - Academic Information
    @FXML private TextField regNumberField;
    @FXML private TextField streamField;
    @FXML private TextField branchField;
    @FXML private TextField courseField;
    @FXML private TextField classField;
    @FXML private TextField sectionField;
    
    // UI Components - Contact Information
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField stateField;
    @FXML private TextField countryField;
    @FXML private TextField postalCodeField;
    
    // UI Components - Security
    @FXML private Button changePasswordButton;
    @FXML private Button enable2FAButton;
    @FXML private TableView<LoginHistoryEntry> loginHistoryTable;
    @FXML private TableColumn<LoginHistoryEntry, String> dateColumn;
    @FXML private TableColumn<LoginHistoryEntry, String> deviceColumn;
    @FXML private TableColumn<LoginHistoryEntry, String> locationColumn;
    
    // UI Components - Action Buttons
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private Button backButton;
    
    // Data
    private Student student;
    private String uid;
    private boolean editMode = false;
    private Firestore db;
    private Map<String, String> originalUserAttributes;
    
    /**
     * Initialize the controller.
     */
    @FXML
    private void initialize() {
        try {
            db = FirestoreClient.getFirestore();
            
            // Initialize gender combo box
            genderComboBox.setItems(FXCollections.observableArrayList("Male", "Female", "Other", "Prefer not to say"));
            
            // Set fields to read-only initially
            setFieldsEditable(false);
            
            // Load student data if UID is available
            String idToken = LoginController.getIdToken();
            if (idToken != null && !idToken.isEmpty()) {
                try {
                    uid = FirebaseAuth.getInstance().verifyIdToken(idToken).getUid();
                    loadStudentData();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to verify ID token", e);
                    showAlert(Alert.AlertType.ERROR, "Authentication Error", "Failed to verify your identity. Please log in again.");
                }
            } else {
                LOGGER.warning("No ID token available; cannot load student data");
                showAlert(Alert.AlertType.WARNING, "Authentication Required", "Please log in to view your profile.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing profile view", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load profile: " + e.getMessage());
        }
    }
    
    /**
     * Set the user attributes from the login controller.
     */
    public void setUserAttributes(Map<String, String> attributes) {
        this.originalUserAttributes = attributes;
        
        // If student data hasn't been loaded yet, use these attributes to display basic info
        if (student == null) {
            student = new Student();
            student.setEmail(attributes.get("email"));
            student.setPhone(attributes.get("phone"));
            student.setRegNumber(attributes.get("reg_number"));
            student.setStream(attributes.get("stream"));
            student.setBranch(attributes.get("branch"));
            student.setCourse(attributes.get("course"));
            student.setClassName(attributes.get("class"));
            student.setSection(attributes.get("section"));
            
            updateUIFromStudent();
        }
    }
    
    /**
     * Load student data from Firestore.
     */
    private void loadStudentData() {
        try {
            DocumentReference docRef = db.collection("users").document(uid);
            DocumentSnapshot doc = docRef.get().get();
            
            if (doc.exists()) {
                Map<String, Object> data = doc.getData();
                student = Student.fromMap(uid, data);
                updateUIFromStudent();
                LOGGER.info("Loaded student data for UID: " + uid);
            } else {
                LOGGER.warning("No student data found for UID: " + uid);
                showAlert(Alert.AlertType.WARNING, "Profile Not Found", "Your profile information was not found. Please update your profile.");
                
                // Create a new student object with basic info
                student = new Student();
                student.setUid(uid);
                if (originalUserAttributes != null) {
                    student.setEmail(originalUserAttributes.get("email"));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading student data", e);
            showAlert(Alert.AlertType.ERROR, "Data Load Error", "Failed to load your profile data: " + e.getMessage());
        }
    }
    
    /**
     * Update the UI with student data.
     */
    private void updateUIFromStudent() {
        Platform.runLater(() -> {
            // Update profile header
            studentNameText.setText(student.getFullName());
            studentEmailText.setText(student.getEmail());
            studentIdText.setText("ID: " + (student.getRegNumber() != null ? student.getRegNumber() : "Not Set"));
            
            // Update profile picture if available
            if (student.getProfilePictureUrl() != null && !student.getProfilePictureUrl().isEmpty()) {
                try {
                    profilePictureView.setImage(new Image(student.getProfilePictureUrl()));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load profile picture", e);
                    // Keep default image
                }
            }
            
            // Update personal information fields
            firstNameField.setText(student.getFirstName());
            lastNameField.setText(student.getLastName());
            emailField.setText(student.getEmail());
            phoneField.setText(student.getPhone());
            
            // Set date of birth if available
            if (student.getDateOfBirth() != null && !student.getDateOfBirth().isEmpty()) {
                try {
                    LocalDate dob = LocalDate.parse(student.getDateOfBirth(), DateTimeFormatter.ISO_DATE);
                    dobPicker.setValue(dob);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse date of birth", e);
                }
            }
            
            // Set gender if available
            if (student.getGender() != null) {
                genderComboBox.setValue(student.getGender());
            }
            
            bioTextArea.setText(student.getBio());
            
            // Update academic information fields
            regNumberField.setText(student.getRegNumber());
            streamField.setText(student.getStream());
            branchField.setText(student.getBranch());
            courseField.setText(student.getCourse());
            classField.setText(student.getClassName());
            sectionField.setText(student.getSection());
            
            // Update contact information fields
            addressField.setText(student.getAddress());
            cityField.setText(student.getCity());
            stateField.setText(student.getState());
            countryField.setText(student.getCountry());
            postalCodeField.setText(student.getPostalCode());
        });
    }
    
    /**
     * Update the student object with data from the UI.
     */
    private void updateStudentFromUI() {
        student.setFirstName(firstNameField.getText());
        student.setLastName(lastNameField.getText());
        student.setPhone(phoneField.getText());
        
        // Get date of birth from date picker
        if (dobPicker.getValue() != null) {
            student.setDateOfBirth(dobPicker.getValue().format(DateTimeFormatter.ISO_DATE));
        }
        
        student.setGender(genderComboBox.getValue());
        student.setBio(bioTextArea.getText());
        
        // Academic information
        student.setRegNumber(regNumberField.getText());
        student.setStream(streamField.getText());
        student.setBranch(branchField.getText());
        student.setCourse(courseField.getText());
        student.setClassName(classField.getText());
        student.setSection(sectionField.getText());
        
        // Contact information
        student.setAddress(addressField.getText());
        student.setCity(cityField.getText());
        student.setState(stateField.getText());
        student.setCountry(countryField.getText());
        student.setPostalCode(postalCodeField.getText());
    }
    
    /**
     * Set all fields editable or read-only.
     */
    private void setFieldsEditable(boolean editable) {
        // Personal information
        firstNameField.setEditable(editable);
        lastNameField.setEditable(editable);
        phoneField.setEditable(editable);
        dobPicker.setDisable(!editable);
        genderComboBox.setDisable(!editable);
        bioTextArea.setEditable(editable);
        
        // Academic information
        regNumberField.setEditable(editable);
        streamField.setEditable(editable);
        branchField.setEditable(editable);
        courseField.setEditable(editable);
        classField.setEditable(editable);
        sectionField.setEditable(editable);
        
        // Contact information
        addressField.setEditable(editable);
        cityField.setEditable(editable);
        stateField.setEditable(editable);
        countryField.setEditable(editable);
        postalCodeField.setEditable(editable);
        
        // Action buttons
        saveButton.setVisible(editable);
        cancelButton.setVisible(editable);
        changeProfilePictureButton.setVisible(editable);
        
        // Email is always read-only
        emailField.setEditable(false);
    }
    
    /**
     * Save student data to Firestore.
     */
    private void saveStudentData() {
        try {
            if (uid == null || uid.isEmpty()) {
                throw new IllegalStateException("User ID is not available");
            }
            
            updateStudentFromUI();
            Map<String, Object> data = student.toMap();
            
            db.collection("users").document(uid).set(data).get();
            LOGGER.info("Saved student data for UID: " + uid);
            
            showAlert(Alert.AlertType.INFORMATION, "Profile Saved", "Your profile has been updated successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving student data", e);
            showAlert(Alert.AlertType.ERROR, "Save Error", "Failed to save your profile: " + e.getMessage());
        }
    }
    
    /**
     * Handle the "Edit Profile" button click.
     */
    @FXML
    private void handleEditProfile(ActionEvent event) {
        editMode = true;
        setFieldsEditable(true);
        editProfileButton.setVisible(false);
    }
    
    /**
     * Handle the "Save Changes" button click.
     */
    @FXML
    private void handleSave(ActionEvent event) {
        saveStudentData();
        editMode = false;
        setFieldsEditable(false);
        editProfileButton.setVisible(true);
        updateUIFromStudent(); // Refresh UI with saved data
    }
    
    /**
     * Handle the "Cancel" button click.
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        // Revert changes by reloading student data
        loadStudentData();
        editMode = false;
        setFieldsEditable(false);
        editProfileButton.setVisible(true);
    }
    
    /**
     * Handle the "Change Profile Picture" button click.
     */
    @FXML
    private void handleChangeProfilePicture(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(profilePictureView.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // In a real application, you would upload this file to a storage service
                // and get a URL to store in the student profile
                Image image = new Image(selectedFile.toURI().toString());
                profilePictureView.setImage(image);
                
                // For this implementation, we'll just store the local file path
                // In a real app, this would be a URL to the uploaded image
                student.setProfilePictureUrl(selectedFile.toURI().toString());
                
                LOGGER.info("Changed profile picture: " + selectedFile.getPath());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading profile picture", e);
                showAlert(Alert.AlertType.ERROR, "Image Error", "Failed to load the selected image: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle the "Back to Dashboard" button click.
     */
    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        if (editMode) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText(null);
            alert.setContentText("You have unsaved changes. Do you want to save before going back?");
            
            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == saveButton) {
                saveStudentData();
                navigateToDashboard();
            } else if (result.get() == discardButton) {
                navigateToDashboard();
            }
            // If cancel, do nothing
        } else {
            navigateToDashboard();
        }
    }
    
    /**
     * Navigate back to the student dashboard.
     */
    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student_dashboard.fxml"));
            Parent root = loader.load();
            
            StudentDashboardController controller = loader.getController();
            
            // Pass user attributes to the dashboard controller
            if (originalUserAttributes != null) {
                controller.setUserAttributes(originalUserAttributes);
            } else {
                // Create a map from the student object
                Map<String, String> attributes = new HashMap<>();
                attributes.put("email", student.getEmail());
                attributes.put("phone", student.getPhone());
                attributes.put("reg_number", student.getRegNumber());
                attributes.put("stream", student.getStream());
                attributes.put("branch", student.getBranch());
                attributes.put("course", student.getCourse());
                attributes.put("class", student.getClassName());
                attributes.put("section", student.getSection());
                controller.setUserAttributes(attributes);
            }
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            LOGGER.info("Navigated back to student dashboard");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error navigating to dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Handle the "Change Password" button click.
     */
    @FXML
    private void handleChangePassword(ActionEvent event) {
        // This would typically open a dialog to change password
        // For now, just show a placeholder alert
        showAlert(Alert.AlertType.INFORMATION, "Change Password", "Password change functionality will be implemented in a future update.");
    }
    
    /**
     * Handle the "Enable Two-Factor Authentication" button click.
     */
    @FXML
    private void handleEnable2FA(ActionEvent event) {
        // This would typically open a dialog to set up 2FA
        // For now, just show a placeholder alert
        showAlert(Alert.AlertType.INFORMATION, "Two-Factor Authentication", "2FA functionality will be implemented in a future update.");
    }
    
    /**
     * Show an alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Class to represent a login history entry for the table view.
     */
    public static class LoginHistoryEntry {
        private String date;
        private String device;
        private String location;
        
        public LoginHistoryEntry(String date, String device, String location) {
            this.date = date;
            this.device = device;
            this.location = location;
        }
        
        public String getDate() {
            return date;
        }
        
        public String getDevice() {
            return device;
        }
        
        public String getLocation() {
            return location;
        }
    }
}