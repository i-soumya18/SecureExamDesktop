package com.secureexam.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for the ConfigLoader class to verify configuration loading functionality.
 */
public class ConfigLoaderTest {
    
    private static final String TEST_CONFIG_PATH = "src/test/resources/test_settings.json";
    private static final String ORIGINAL_CONFIG_PATH = "src/main/resources/settings.json";
    private static final String BACKUP_CONFIG_PATH = "src/main/resources/settings.json.bak";
    
    private boolean originalConfigExists;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create test resources directory if it doesn't exist
        Files.createDirectories(Paths.get("src/test/resources"));
        
        // Check if original config exists and back it up if it does
        File originalConfig = new File(ORIGINAL_CONFIG_PATH);
        originalConfigExists = originalConfig.exists();
        if (originalConfigExists) {
            Files.copy(Paths.get(ORIGINAL_CONFIG_PATH), Paths.get(BACKUP_CONFIG_PATH));
        }
        
        // Create test config
        JSONObject testConfig = new JSONObject()
            .put("institute", new JSONObject()
                .put("name", "Test Institute"))
            .put("firebase", new JSONObject()
                .put("projectId", "test-project")
                .put("serviceAccount", "test-service-account.json"))
            .put("examRules", new JSONObject()
                .put("defaultTimeLimit", 45));
        
        try (FileWriter writer = new FileWriter(TEST_CONFIG_PATH)) {
            writer.write(testConfig.toString(2));
        }
        
        // Copy test config to original location
        Files.copy(Paths.get(TEST_CONFIG_PATH), Paths.get(ORIGINAL_CONFIG_PATH), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        System.out.println("[DEBUG_LOG] Test setup complete with test config");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        // Delete test config
        Files.deleteIfExists(Paths.get(TEST_CONFIG_PATH));
        
        // Restore original config if it existed
        if (originalConfigExists) {
            Files.copy(Paths.get(BACKUP_CONFIG_PATH), Paths.get(ORIGINAL_CONFIG_PATH), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(Paths.get(BACKUP_CONFIG_PATH));
        } else {
            Files.deleteIfExists(Paths.get(ORIGINAL_CONFIG_PATH));
        }
        
        System.out.println("[DEBUG_LOG] Test cleanup complete");
    }
    
    @Test
    public void testGetInstituteName() {
        String instituteName = ConfigLoader.getInstituteName();
        assertEquals("Test Institute", instituteName, "Institute name should match test config");
        System.out.println("[DEBUG_LOG] Institute name test passed");
    }
    
    @Test
    public void testGetFirebaseConfig() {
        JSONObject firebaseConfig = ConfigLoader.getFirebaseConfig();
        assertNotNull(firebaseConfig, "Firebase config should not be null");
        assertEquals("test-project", firebaseConfig.getString("projectId"), "Project ID should match test config");
        assertEquals("test-service-account.json", firebaseConfig.getString("serviceAccount"), "Service account should match test config");
        System.out.println("[DEBUG_LOG] Firebase config test passed");
    }
    
    @Test
    public void testGetDefaultTimeLimit() {
        int timeLimit = ConfigLoader.getDefaultTimeLimit();
        assertEquals(45, timeLimit, "Default time limit should match test config");
        System.out.println("[DEBUG_LOG] Default time limit test passed");
    }
}