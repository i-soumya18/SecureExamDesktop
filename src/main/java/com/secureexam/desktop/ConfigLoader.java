package com.secureexam.desktop;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigLoader {
    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static JSONObject config;

    static {
        try {
            String content = Files.readString(Path.of("src/main/resources/settings.json"));
            config = new JSONObject(content);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Config file not found; using defaults", e);
            config = new JSONObject();
        }
    }

    public static String getInstituteName() {
        return config.optJSONObject("institute", new JSONObject()).optString("name", "SecureExam");
    }

    public static JSONObject getFirebaseConfig() {
        return config.optJSONObject("firebase", new JSONObject()
            .put("projectId", "assistant-65908")
            .put("serviceAccount", "assistant-65908-firebase-adminsdk-w999m-181efe1e50.json"));
    }

    public static int getDefaultTimeLimit() {
        return config.optJSONObject("examRules", new JSONObject()).optInt("defaultTimeLimit", 30);
    }
}