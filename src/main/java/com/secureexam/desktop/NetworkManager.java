package com.secureexam.desktop;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NetworkManager handles internet access control for the application.
 * It uses a combination of Java's proxy settings and firewall-like blocking
 * to restrict internet access during exams.
 */
public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static boolean internetDisabled = false;
    private static final String[] TEST_URLS = {
        "https://www.google.com",
        "https://www.cloudflare.com",
        "https://www.microsoft.com"
    };
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    
    // List of allowed domains during exams (e.g., Firebase domains for submission)
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
        "firestore.googleapis.com",
        "firebase-settings.crashlytics.com",
        "firebase.googleapis.com",
        "firebaseio.com"
    );
    
    // Store original proxy settings to restore them later
    private static String originalProxyHost;
    private static String originalProxyPort;
    private static String originalProxyExclusions;
    private static String originalHttpsProxyHost;
    private static String originalHttpsProxyPort;
    private static ProxySelector originalProxySelector;

    /**
     * Disables internet access by setting up a blocking proxy
     * @return true if successful, false otherwise
     */
    public static boolean disableInternet() {
        if (internetDisabled) return true;

        try {
            // Save original proxy settings
            saveOriginalProxySettings();
            
            // Set up our custom proxy selector
            ProxySelector.setDefault(new BlockingProxySelector());
            
            // Set system properties for applications that don't use ProxySelector
            System.setProperty("http.proxyHost", "localhost");
            System.setProperty("http.proxyPort", "0");
            System.setProperty("https.proxyHost", "localhost");
            System.setProperty("https.proxyPort", "0");
            
            // Mark internet as disabled
            internetDisabled = true;
            LOGGER.info("Internet access restricted successfully");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error disabling internet", e);
            // Try to restore original settings
            enableInternet();
            return false;
        }
    }

    /**
     * Enables internet access by restoring original proxy settings
     * @return true if successful, false otherwise
     */
    public static boolean enableInternet() {
        if (!internetDisabled) return true;

        try {
            // Restore original proxy settings
            restoreOriginalProxySettings();
            
            // Mark internet as enabled
            internetDisabled = false;
            LOGGER.info("Internet access restored successfully");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error enabling internet", e);
            return false;
        }
    }

    /**
     * Checks if the system is online by attempting to connect to multiple test URLs
     * @return true if online, false otherwise
     */
    public static boolean isOnline() {
        if (internetDisabled) {
            return false;
        }

        for (String testUrl : TEST_URLS) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 400) {
                    return true;
                }
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to connect to " + testUrl, e);
                // Continue to next URL
            }
        }

        // If all HTTP checks fail, try ping as a fallback
        try {
            String pingCommand = System.getProperty("os.name").toLowerCase().contains("win") 
                ? "ping -n 1 8.8.8.8" 
                : "ping -c 1 8.8.8.8";
            Process process = Runtime.getRuntime().exec(pingCommand);
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.INFO, "Ping check failed", e);
            return false;
        }
    }
    
    /**
     * Saves the original proxy settings to restore them later
     */
    private static void saveOriginalProxySettings() {
        originalProxyHost = System.getProperty("http.proxyHost");
        originalProxyPort = System.getProperty("http.proxyPort");
        originalProxyExclusions = System.getProperty("http.nonProxyHosts");
        originalHttpsProxyHost = System.getProperty("https.proxyHost");
        originalHttpsProxyPort = System.getProperty("https.proxyPort");
        originalProxySelector = ProxySelector.getDefault();
    }
    
    /**
     * Restores the original proxy settings
     */
    private static void restoreOriginalProxySettings() {
        // Restore system properties
        setOrClearProperty("http.proxyHost", originalProxyHost);
        setOrClearProperty("http.proxyPort", originalProxyPort);
        setOrClearProperty("http.nonProxyHosts", originalProxyExclusions);
        setOrClearProperty("https.proxyHost", originalHttpsProxyHost);
        setOrClearProperty("https.proxyPort", originalHttpsProxyPort);
        
        // Restore proxy selector
        if (originalProxySelector != null) {
            ProxySelector.setDefault(originalProxySelector);
        } else {
            ProxySelector.setDefault(ProxySelector.getDefault());
        }
    }
    
    /**
     * Helper method to set or clear a system property
     */
    private static void setOrClearProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
    }
    
    /**
     * Custom ProxySelector that blocks all connections except to allowed domains
     */
    private static class BlockingProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            String host = uri.getHost().toLowerCase();
            
            // Allow connections to Firebase and other essential services
            for (String allowedDomain : ALLOWED_DOMAINS) {
                if (host.endsWith(allowedDomain)) {
                    LOGGER.fine("Allowing connection to " + host);
                    return Arrays.asList(Proxy.NO_PROXY);
                }
            }
            
            // Block all other connections
            LOGGER.fine("Blocking connection to " + host);
            return Arrays.asList(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 0)));
        }
        
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            LOGGER.log(Level.FINE, "Connection failed to " + uri, ioe);
        }
    }
}
