package com.secureexam.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the LocalCache class to verify caching functionality.
 */
public class LocalCacheTest {
    
    private String examId;
    private String studentId;
    private List<Question> questions;
    private Map<Integer, String> answers;
    
    @BeforeEach
    public void setUp() {
        examId = "test-exam-" + System.currentTimeMillis(); // Unique exam ID for each test
        studentId = "test-student-123";
        
        // Create test questions
        questions = new ArrayList<>();
        questions.add(new Question(examId, "Question 1", new String[]{"A", "B", "C", "D"}, "A"));
        questions.add(new Question(examId, "Question 2", new String[]{"A", "B", "C", "D"}, "B"));
        questions.add(new Question(examId, "Question 3", new String[]{"A", "B", "C", "D"}, "C"));
        
        // Create test answers
        answers = new HashMap<>();
        answers.put(0, "A");
        answers.put(1, "B");
        answers.put(2, "D"); // Intentionally wrong answer
        
        System.out.println("[DEBUG_LOG] Test setup complete with examId: " + examId);
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up the cache after each test
        LocalCache.cleanupExamCache(examId);
        System.out.println("[DEBUG_LOG] Test cleanup complete for examId: " + examId);
    }
    
    @Test
    public void testQuestionCaching() {
        // Save questions to cache
        LocalCache.saveQuestions(examId, questions);
        System.out.println("[DEBUG_LOG] Saved " + questions.size() + " questions to cache");
        
        // Verify cache status
        assertTrue(LocalCache.isCached(examId), "Exam should be cached after saving questions");
        
        // Retrieve questions from cache
        List<Question> cachedQuestions = LocalCache.getQuestions(examId);
        
        // Verify retrieved questions
        assertNotNull(cachedQuestions, "Retrieved questions should not be null");
        assertEquals(questions.size(), cachedQuestions.size(), "Should retrieve the same number of questions");
        
        // Verify question content
        for (int i = 0; i < questions.size(); i++) {
            assertEquals(questions.get(i).getText(), cachedQuestions.get(i).getText(), 
                    "Question text should match for question " + i);
            assertEquals(questions.get(i).getCorrectAnswer(), cachedQuestions.get(i).getCorrectAnswer(), 
                    "Correct answer should match for question " + i);
        }
        
        System.out.println("[DEBUG_LOG] Question caching test passed");
    }
    
    @Test
    public void testSubmissionCaching() {
        // Save submission to cache
        LocalCache.saveSubmission(examId, studentId, answers);
        System.out.println("[DEBUG_LOG] Saved submission to cache");
        
        // Retrieve submission from cache
        Map<Integer, String> cachedAnswers = LocalCache.getSubmission(examId, studentId);
        
        // Verify retrieved answers
        assertNotNull(cachedAnswers, "Retrieved answers should not be null");
        assertEquals(answers.size(), cachedAnswers.size(), "Should retrieve the same number of answers");
        
        // Verify answer content
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            assertEquals(entry.getValue(), cachedAnswers.get(entry.getKey()), 
                    "Answer should match for question " + entry.getKey());
        }
        
        System.out.println("[DEBUG_LOG] Submission caching test passed");
    }
    
    @Test
    public void testMarkSubmissionAsSynced() {
        // Save submission to cache
        LocalCache.saveSubmission(examId, studentId, answers);
        System.out.println("[DEBUG_LOG] Saved submission to cache");
        
        // Mark submission as synced
        LocalCache.markSubmissionAsSynced(examId, studentId);
        System.out.println("[DEBUG_LOG] Marked submission as synced");
        
        // Verify submission is not in pending submissions
        List<Map<String, Object>> pendingSubmissions = LocalCache.getPendingSubmissions();
        boolean found = false;
        for (Map<String, Object> submission : pendingSubmissions) {
            if (examId.equals(submission.get("examId")) && studentId.equals(submission.get("studentId"))) {
                found = true;
                break;
            }
        }
        
        assertFalse(found, "Synced submission should not be in pending submissions");
        System.out.println("[DEBUG_LOG] Mark submission as synced test passed");
    }
    
    @Test
    public void testCleanupExamCache() {
        // Save questions and submission to cache
        LocalCache.saveQuestions(examId, questions);
        LocalCache.saveSubmission(examId, studentId, answers);
        System.out.println("[DEBUG_LOG] Saved questions and submission to cache");
        
        // Verify cache status
        assertTrue(LocalCache.isCached(examId), "Exam should be cached after saving questions");
        
        // Clean up exam cache
        LocalCache.cleanupExamCache(examId);
        System.out.println("[DEBUG_LOG] Cleaned up exam cache");
        
        // Verify cache status after cleanup
        assertFalse(LocalCache.isCached(examId), "Exam should not be cached after cleanup");
        
        // Verify submission is removed (if it was marked as synced)
        LocalCache.markSubmissionAsSynced(examId, studentId);
        LocalCache.cleanupExamCache(examId);
        
        Map<Integer, String> cachedAnswers = LocalCache.getSubmission(examId, studentId);
        assertTrue(cachedAnswers.isEmpty(), "Submission should be removed after cleanup if marked as synced");
        
        System.out.println("[DEBUG_LOG] Cleanup exam cache test passed");
    }
}