package com.secureexam.desktop;

/**
 * Manual test class for NetworkManager to verify network control functionality
 * across different platforms.
 */
public class NetworkManagerTest {
    
    public static void main(String[] args) {
        System.out.println("Starting NetworkManager tests...");
        
        // Store initial network state
        boolean initialNetworkState = NetworkManager.isOnline();
        System.out.println("Initial network state: " + (initialNetworkState ? "Online" : "Offline"));
        
        try {
            // Test 1: Check if isOnline works
            testIsOnline();
            
            // Test 2: Test disabling internet (if initially online)
            if (initialNetworkState) {
                testDisableInternet();
            } else {
                System.out.println("Skipping disable test as network is already offline");
            }
            
            // Test 3: Test enabling internet (after disabling)
            testEnableInternet();
            
            // Test 4: Test offline mode
            testOfflineMode();
            
        } finally {
            // Restore initial network state
            if (initialNetworkState && !NetworkManager.isOnline()) {
                System.out.println("Restoring network state to online...");
                NetworkManager.enableInternet();
            }
            System.out.println("Final network state: " + (NetworkManager.isOnline() ? "Online" : "Offline"));
        }
        
        System.out.println("NetworkManager tests completed.");
    }
    
    private static void testIsOnline() {
        System.out.println("\n--- Testing isOnline method ---");
        boolean online = NetworkManager.isOnline();
        System.out.println("Network is " + (online ? "online" : "offline"));
        System.out.println("isOnline test completed");
    }
    
    private static void testDisableInternet() {
        System.out.println("\n--- Testing internet disable functionality ---");
        boolean result = NetworkManager.disableInternet();
        
        if (result) {
            boolean offline = !NetworkManager.isOnline();
            System.out.println("Disable result: " + (offline ? "SUCCESS" : "FAILED") + 
                " - Network is now " + (offline ? "offline" : "still online"));
        } else {
            System.out.println("Failed to disable internet (may need admin privileges)");
        }
    }
    
    private static void testEnableInternet() {
        System.out.println("\n--- Testing internet enable functionality ---");
        
        // First make sure internet is disabled
        NetworkManager.disableInternet();
        
        boolean result = NetworkManager.enableInternet();
        
        if (result) {
            boolean online = NetworkManager.isOnline();
            System.out.println("Enable result: " + (online ? "SUCCESS" : "FAILED") + 
                " - Network is now " + (online ? "online" : "still offline"));
        } else {
            System.out.println("Failed to enable internet (may need admin privileges)");
        }
    }
    
    private static void testOfflineMode() {
        System.out.println("\n--- Testing offline mode ---");
        
        // Disable internet to simulate offline mode
        boolean disableResult = NetworkManager.disableInternet();
        
        if (!disableResult) {
            System.out.println("Skipping offline mode test as disable operation failed");
            return;
        }
        
        try {
            // Verify we're offline
            boolean offline = !NetworkManager.isOnline();
            System.out.println("Offline mode: " + (offline ? "SUCCESS" : "FAILED") + 
                " - Network is " + (offline ? "offline" : "still online"));
            
            // Here you would test application functionality in offline mode
            System.out.println("Testing LocalCache in offline mode...");
            
            // Example: Check if we can retrieve cached data
            if (LocalCache.isCached("test-exam-id")) {
                System.out.println("Successfully retrieved cached data in offline mode");
            } else {
                System.out.println("No cached data found for testing");
            }
            
        } finally {
            // Re-enable internet
            NetworkManager.enableInternet();
        }
    }
}