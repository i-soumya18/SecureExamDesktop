package com.secureexam.desktop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import javafx.application.Platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirebaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(FirebaseInitializer.class.getName());
    private static boolean initialized = false;
    private static Firestore db;  // Static Firestore instance
    private static FirebaseAuth auth;  // Static FirebaseAuth instance

    public static void initialize() {
        if (!initialized) {
            try {
                // Load the service account file from src/main/resources/firebase/
                FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebase/assistant-65908-firebase-adminsdk-w999m-181efe1e50.json");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://assistant-65908.firebaseio.com") // Your Firebase Realtime Database URL
                        .build();
                FirebaseApp.initializeApp(options);

                // Initialize Firestore and FirebaseAuth
                db = FirestoreClient.getFirestore();
                auth = FirebaseAuth.getInstance();

                initialized = true;
                LOGGER.info("Firebase initialized successfully");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize Firebase", e);
                Platform.runLater(() -> showFatalError("Firebase initialization failed: " + e.getMessage()));
                throw new RuntimeException("Firebase initialization failed", e);
            }
        }
    }

    // Getter for Firestore instance
    public static Firestore getFirestore() {
        if (!initialized) {
            initialize();
        }
        return db;
    }

    // Getter for FirebaseAuth instance
    public static FirebaseAuth getAuth() {
        if (!initialized) {
            initialize();
        }
        return auth;
    }

    // Method to show fatal error on JavaFX thread
    private static void showFatalError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Fatal Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}