package com.secureexam.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the NetworkManager class to verify network control functionality
 * across different platforms.
 */
public class NetworkManagerTest {
    
    private boolean initialNetworkState;
    
    @BeforeEach
    public void setUp() {
        // Store the initial network state
        initialNetworkState = NetworkManager.isOnline();
        System.out.println("[DEBUG_LOG] Initial network state: " + (initialNetworkState ? "Online" : "Offline"));
    }
    
    @AfterEach
    public void tearDown() {
        // Restore the initial network state
        if (initialNetworkState) {
            NetworkManager.enableInternet();
        }
        System.out.println("[DEBUG_LOG] Network state restored to: " + (NetworkManager.isOnline() ? "Online" : "Offline"));
    }
    
    @Test
    public void testIsOnline() {
        // This test simply checks if the isOnline method works without exceptions
        boolean online = NetworkManager.isOnline();
        System.out.println("[DEBUG_LOG] Network is " + (online ? "online" : "offline"));
        // We can't assert a specific value since it depends on the actual network state
    }
    
    @Test
    public void testDisableInternet() {
        // Only run this test if we're initially online
        if (!initialNetworkState) {
            System.out.println("[DEBUG_LOG] Skipping disable test as network is already offline");
            return;
        }
        
        System.out.println("[DEBUG_LOG] Testing internet disable functionality");
        boolean result = NetworkManager.disableInternet();
        
        // Check if the operation was successful (may fail if not running with admin privileges)
        if (result) {
            // Verify network is now offline
            assertFalse(NetworkManager.isOnline(), "Network should be offline after disabling");
            System.out.println("[DEBUG_LOG] Successfully disabled internet");
        } else {
            System.out.println("[DEBUG_LOG] Failed to disable internet (may need admin privileges)");
        }
    }
    
    @Test
    public void testEnableInternet() {
        // First disable the internet
        boolean disableResult = NetworkManager.disableInternet();
        
        // Only continue if disable was successful
        if (!disableResult) {
            System.out.println("[DEBUG_LOG] Skipping enable test as disable operation failed");
            return;
        }
        
        System.out.println("[DEBUG_LOG] Testing internet enable functionality");
        boolean result = NetworkManager.enableInternet();
        
        // Check if the operation was successful
        if (result) {
            // Verify network is now online (if we had internet initially)
            if (initialNetworkState) {
                assertTrue(NetworkManager.isOnline(), "Network should be online after enabling");
            }
            System.out.println("[DEBUG_LOG] Successfully enabled internet");
        } else {
            System.out.println("[DEBUG_LOG] Failed to enable internet (may need admin privileges)");
        }
    }
    
    @Test
    public void testOfflineMode() {
        // Disable internet to simulate offline mode
        boolean disableResult = NetworkManager.disableInternet();
        
        if (!disableResult) {
            System.out.println("[DEBUG_LOG] Skipping offline mode test as disable operation failed");
            return;
        }
        
        try {
            // Verify we're offline
            assertFalse(NetworkManager.isOnline(), "Should be in offline mode");
            System.out.println("[DEBUG_LOG] Successfully verified offline mode");
            
            // Here you would test application functionality in offline mode
            // For example, verify that the application can still load cached data
            
        } finally {
            // Re-enable internet
            NetworkManager.enableInternet();
        }
    }
}