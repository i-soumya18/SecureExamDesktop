package com.secureexam.desktop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LocalCache {
    private static final Logger LOGGER = Logger.getLogger(LocalCache.class.getName());
    private static final String DB_URL = "jdbc:h2:./secureexam_cache";
    private static Connection conn;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long CACHE_EXPIRY_DAYS = 30; // Default expiry time in days

    static {
        try {
            conn = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize local cache", e);
        }
    }

    // Ensure resources are released when the application exits
    public static void shutdown() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                LOGGER.info("LocalCache connection closed");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    private static void initializeDatabase() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create questions table with timestamp
            stmt.execute("CREATE TABLE IF NOT EXISTS questions (" +
                "examId VARCHAR(255), " +
                "text VARCHAR(1024), " +
                "options VARCHAR(2048), " +
                "correctAnswer VARCHAR(255), " +
                "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (examId, text))");

            // Create submissions table with timestamp
            stmt.execute("CREATE TABLE IF NOT EXISTS submissions (" +
                "examId VARCHAR(255), " +
                "studentId VARCHAR(255), " +
                "answers VARCHAR(2048), " + // JSON string of answers
                "submitted BOOLEAN, " +
                "created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (examId, studentId))");

            // Add timestamp column to existing tables if they don't have it
            try {
                stmt.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS created TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS created TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException e) {
                // This might fail if columns already exist or if H2 version doesn't support IF NOT EXISTS in ALTER TABLE
                LOGGER.log(Level.INFO, "Timestamp columns might already exist", e);
            }
        }
    }

    public static void saveQuestions(String examId, List<Question> questions) {
        try {
            // First delete existing questions for this exam to avoid duplicates
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM questions WHERE examId = ?")) {
                deleteStmt.setString(1, examId);
                deleteStmt.executeUpdate();
            }

            // Then insert new questions
            try (PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO questions (examId, text, options, correctAnswer) VALUES (?, ?, ?, ?)")) {
                for (Question q : questions) {
                    insertStmt.setString(1, examId);
                    insertStmt.setString(2, q.getText());
                    insertStmt.setString(3, String.join(",", q.getOptions()));
                    insertStmt.setString(4, q.getCorrectAnswer());
                    insertStmt.executeUpdate();
                }
            }
            LOGGER.info("Cached " + questions.size() + " questions for examId: " + examId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save questions to cache", e);
        }
    }

    public static List<Question> getQuestions(String examId) {
        List<Question> questions = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT text, options, correctAnswer FROM questions WHERE examId = ?")) {
            stmt.setString(1, examId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String text = rs.getString("text");
                String[] options = rs.getString("options").split(",");
                String correctAnswer = rs.getString("correctAnswer");
                questions.add(new Question(examId, text, options, correctAnswer));
            }
            LOGGER.info("Retrieved " + questions.size() + " questions from cache for examId: " + examId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve questions from cache", e);
        }
        return questions;
    }

    public static boolean isCached(String examId) {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT COUNT(*) FROM questions WHERE examId = ?")) {
            stmt.setString(1, examId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking cache for examId: " + examId, e);
            return false;
        }
    }

    public static void saveSubmission(String examId, String studentId, Map<Integer, String> answers) {
        try {
            // First check if submission exists
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM submissions WHERE examId = ? AND studentId = ?")) {
                checkStmt.setString(1, examId);
                checkStmt.setString(2, studentId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }
            }

            // Then either update or insert
            if (exists) {
                try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE submissions SET answers = ?, submitted = ? WHERE examId = ? AND studentId = ?")) {
                    updateStmt.setString(1, new JSONObject(answers).toString());
                    updateStmt.setBoolean(2, false);
                    updateStmt.setString(3, examId);
                    updateStmt.setString(4, studentId);
                    updateStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO submissions (examId, studentId, answers, submitted) VALUES (?, ?, ?, ?)")) {
                    insertStmt.setString(1, examId);
                    insertStmt.setString(2, studentId);
                    insertStmt.setString(3, new JSONObject(answers).toString());
                    insertStmt.setBoolean(4, false);
                    insertStmt.executeUpdate();
                }
            }
            LOGGER.info("Saved submission for examId: " + examId + ", studentId: " + studentId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save submission", e);
        }
    }

    public static Map<Integer, String> getSubmission(String examId, String studentId) {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT answers FROM submissions WHERE examId = ? AND studentId = ?")) {
            stmt.setString(1, examId);
            stmt.setString(2, studentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new JSONObject(rs.getString("answers")).toMap().entrySet().stream()
                    .collect(Collectors.toMap(
                        e -> Integer.parseInt(e.getKey()),
                        e -> (String) e.getValue()
                    ));
            }
            return new HashMap<>();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve submission", e);
            return new HashMap<>();
        }
    }

    public static void markSubmissionAsSynced(String examId, String studentId) {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE submissions SET submitted = true WHERE examId = ? AND studentId = ?")) {
            stmt.setString(1, examId);
            stmt.setString(2, studentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark submission as synced", e);
        }
    }

    public static List<Map<String, Object>> getPendingSubmissions() {
        List<Map<String, Object>> pending = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT examId, studentId, answers FROM submissions WHERE submitted = false");
            while (rs.next()) {
                Map<String, Object> submission = new HashMap<>();
                submission.put("examId", rs.getString("examId"));
                submission.put("studentId", rs.getString("studentId"));
                submission.put("answers", new JSONObject(rs.getString("answers")).toMap());
                pending.add(submission);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve pending submissions", e);
        }
        return pending;
    }

    /**
     * Cleans up expired cache entries based on the configured expiry time
     */
    public static void cleanupExpiredCache() {
        try {
            // Calculate the cutoff date
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(CACHE_EXPIRY_DAYS);
            Timestamp cutoffTimestamp = Timestamp.valueOf(cutoffDate);

            // Delete expired questions
            try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM questions WHERE created < ?")) {
                stmt.setTimestamp(1, cutoffTimestamp);
                int deletedQuestions = stmt.executeUpdate();
                LOGGER.info("Cleaned up " + deletedQuestions + " expired question entries");
            }

            // Delete expired submissions that have been successfully submitted
            try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM submissions WHERE created < ? AND submitted = true")) {
                stmt.setTimestamp(1, cutoffTimestamp);
                int deletedSubmissions = stmt.executeUpdate();
                LOGGER.info("Cleaned up " + deletedSubmissions + " expired submission entries");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up expired cache", e);
        }
    }

    /**
     * Performs a complete cleanup of the cache for a specific exam
     * @param examId The ID of the exam to clean up
     */
    public static void cleanupExamCache(String examId) {
        try {
            // Delete questions for the exam
            try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM questions WHERE examId = ?")) {
                stmt.setString(1, examId);
                int deletedQuestions = stmt.executeUpdate();
                LOGGER.info("Cleaned up " + deletedQuestions + " question entries for examId: " + examId);
            }

            // Delete submissions for the exam that have been successfully submitted
            try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM submissions WHERE examId = ? AND submitted = true")) {
                stmt.setString(1, examId);
                int deletedSubmissions = stmt.executeUpdate();
                LOGGER.info("Cleaned up " + deletedSubmissions + " submission entries for examId: " + examId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up exam cache for examId: " + examId, e);
        }
    }

    /**
     * Performs a complete cleanup of all cache data
     */
    public static void cleanupAllCache() {
        try (Statement stmt = conn.createStatement()) {
            // Delete all questions
            int deletedQuestions = stmt.executeUpdate("DELETE FROM questions");
            LOGGER.info("Cleaned up " + deletedQuestions + " question entries");

            // Delete all submissions that have been successfully submitted
            int deletedSubmissions = stmt.executeUpdate("DELETE FROM submissions WHERE submitted = true");
            LOGGER.info("Cleaned up " + deletedSubmissions + " submission entries");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up all cache", e);
        }
    }
}
