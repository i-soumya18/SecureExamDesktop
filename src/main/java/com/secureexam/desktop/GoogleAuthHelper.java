package com.secureexam.desktop;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import okhttp3.*;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleAuthHelper {
    private static final Logger LOGGER = Logger.getLogger(GoogleAuthHelper.class.getName());
    private static final String CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID"; // Replace with your actual client ID
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final OkHttpClient client = new OkHttpClient();
    private static final int REDIRECT_PORT = 8888;
    
    private static String codeVerifier;
    private static String authCode;
    private static ServerSocket serverSocket;
    
    /**
     * Starts the Google OAuth flow and returns the ID token
     * @return CompletableFuture that will resolve to the ID token
     */
    public static CompletableFuture<String> getGoogleIdToken() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Generate PKCE code verifier and challenge
            codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            
            // Start local server to receive the redirect
            startLocalServer(future);
            
            // Build the authorization URL
            String authUrl = AUTH_ENDPOINT + "?" +
                    "client_id=" + CLIENT_ID +
                    "&redirect_uri=" + REDIRECT_URI +
                    "&response_type=code" +
                    "&scope=email%20profile" +
                    "&code_challenge=" + codeChallenge +
                    "&code_challenge_method=S256";
            
            // Open the default browser with the auth URL
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
                LOGGER.info("Opened browser for Google authentication");
            } else {
                // Fallback to showing the URL if browser can't be opened
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Google Login");
                    alert.setHeaderText("Please open this URL in your browser");
                    alert.setContentText(authUrl);
                    alert.showAndWait();
                });
                LOGGER.warning("Could not open browser, showing URL to user");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting Google OAuth flow", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Starts a local server to receive the OAuth redirect
     */
    private static void startLocalServer(CompletableFuture<String> future) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(REDIRECT_PORT);
                LOGGER.info("Local server started on port " + REDIRECT_PORT);
                
                // Wait for the redirect from Google
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Received connection from Google redirect");
                
                // Parse the request to extract the authorization code
                Scanner scanner = new Scanner(clientSocket.getInputStream());
                String request = scanner.nextLine();
                String response = "HTTP/1.1 200 OK\r\n\r\n" +
                        "<html><body><h1>Authentication Successful</h1>" +
                        "<p>You can close this window and return to the application.</p></body></html>";
                
                clientSocket.getOutputStream().write(response.getBytes());
                clientSocket.close();
                
                // Extract the authorization code from the request
                if (request.contains("code=")) {
                    authCode = request.substring(request.indexOf("code=") + 5);
                    if (authCode.contains(" ")) {
                        authCode = authCode.substring(0, authCode.indexOf(" "));
                    }
                    if (authCode.contains("&")) {
                        authCode = authCode.substring(0, authCode.indexOf("&"));
                    }
                    
                    LOGGER.info("Received authorization code");
                    
                    // Exchange the code for tokens
                    exchangeCodeForTokens(authCode, future);
                } else {
                    LOGGER.severe("No authorization code found in redirect");
                    future.completeExceptionally(new RuntimeException("No authorization code received"));
                }
                
                serverSocket.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in local server", e);
                future.completeExceptionally(e);
            }
        }).start();
    }
    
    /**
     * Exchanges the authorization code for tokens
     */
    private static void exchangeCodeForTokens(String code, CompletableFuture<String> future) {
        try {
            JSONObject json = new JSONObject()
                    .put("code", code)
                    .put("client_id", CLIENT_ID)
                    .put("redirect_uri", REDIRECT_URI)
                    .put("code_verifier", codeVerifier)
                    .put("grant_type", "authorization_code");
            
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(TOKEN_ENDPOINT)
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to exchange code for tokens", e);
                    future.completeExceptionally(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = responseBody != null ? responseBody.string() : "Unknown error";
                            LOGGER.severe("Token exchange failed: " + errorBody);
                            future.completeExceptionally(new IOException("Token exchange failed: " + errorBody));
                            return;
                        }
                        
                        String responseData = responseBody.string();
                        JSONObject result = new JSONObject(responseData);
                        String idToken = result.getString("id_token");
                        LOGGER.info("Successfully obtained ID token");
                        future.complete(idToken);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error processing token response", e);
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exchanging code for tokens", e);
            future.completeExceptionally(e);
        }
    }
    
    /**
     * Generates a random code verifier for PKCE
     */
    private static String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }
    
    /**
     * Generates a code challenge from the code verifier using SHA-256
     */
    private static String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}