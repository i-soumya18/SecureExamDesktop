package com.secureexam.desktop;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.io.InputStream;

public class FirebaseInitializer {
    private static boolean initialized = false;

    public static void initialize() {
        if (!initialized) {
            try (InputStream serviceAccount = FirebaseInitializer.class.getResourceAsStream("/assistant-65908-firebase-adminsdk-w999m-181efe1e50.json")) {
                if (serviceAccount == null) {
                    throw new IOException("Service account file not found in resources");
                }
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://your-project-id.firebaseio.com") // Replace with your Firebase project URL
                        .build();
                FirebaseApp.initializeApp(options);
                initialized = true;
                System.out.println("Firebase initialized successfully");
            } catch (IOException e) {
                System.err.println("Failed to initialize Firebase: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}