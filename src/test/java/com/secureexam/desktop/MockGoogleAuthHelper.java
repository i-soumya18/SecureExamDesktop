package com.secureexam.desktop;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Mock implementation of GoogleAuthHelper for testing purposes.
 * This class provides a simplified version of the authentication flow
 * without requiring JavaFX web components.
 */
public class MockGoogleAuthHelper {
    private static final Logger LOGGER = Logger.getLogger(MockGoogleAuthHelper.class.getName());
    
    /**
     * Returns a CompletableFuture that completes with a mock ID token
     * @return CompletableFuture with a mock ID token
     */
    public static CompletableFuture<String> getGoogleIdToken() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Simulate a successful authentication with a mock token
        String mockIdToken = "mock_id_token_for_testing_purposes";
        LOGGER.info("Returning mock Google ID token for testing");
        future.complete(mockIdToken);
        
        return future;
    }
}