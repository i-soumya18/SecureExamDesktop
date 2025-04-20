package com.secureexam.desktop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Question class to verify question model functionality.
 */
public class QuestionTest {
    
    private String examId;
    private String text;
    private String[] options;
    private String correctAnswer;
    
    @BeforeEach
    public void setUp() {
        examId = "test-exam-123";
        text = "What is the capital of France?";
        options = new String[]{"London", "Paris", "Berlin", "Madrid"};
        correctAnswer = "Paris";
    }
    
    @Test
    public void testQuestionCreation() {
        Question question = new Question(examId, text, options, correctAnswer);
        
        assertEquals(examId, question.getExamId(), "Exam ID should match");
        assertEquals(text, question.getText(), "Question text should match");
        assertArrayEquals(options, question.getOptions(), "Options should match");
        assertEquals(correctAnswer, question.getCorrectAnswer(), "Correct answer should match");
        
        System.out.println("[DEBUG_LOG] Question created successfully");
    }
    
    @Test
    public void testInvalidOptions() {
        String[] invalidOptions = new String[]{"London", "Paris", "Berlin"}; // Only 3 options
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Question(examId, text, invalidOptions, correctAnswer);
        });
        
        assertTrue(exception.getMessage().contains("MCQ must have exactly 4 options"), 
                "Exception message should mention the requirement for 4 options");
        
        System.out.println("[DEBUG_LOG] Invalid options test passed");
    }
    
    @Test
    public void testInvalidCorrectAnswer() {
        String invalidCorrectAnswer = "Rome"; // Not in options
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Question(examId, text, options, invalidCorrectAnswer);
        });
        
        assertTrue(exception.getMessage().contains("Correct answer must be one of the options"), 
                "Exception message should mention that correct answer must be in options");
        
        System.out.println("[DEBUG_LOG] Invalid correct answer test passed");
    }
    
    @Test
    public void testSetOptions() {
        Question question = new Question(examId, text, options, correctAnswer);
        
        String[] newOptions = new String[]{"Rome", "Paris", "Athens", "Madrid"};
        question.setOptions(newOptions);
        
        assertArrayEquals(newOptions, question.getOptions(), "Options should be updated");
        assertEquals(correctAnswer, question.getCorrectAnswer(), "Correct answer should remain the same");
        
        System.out.println("[DEBUG_LOG] Set options test passed");
    }
    
    @Test
    public void testSetOptionsWithInvalidCorrectAnswer() {
        Question question = new Question(examId, text, options, correctAnswer);
        
        String[] newOptions = new String[]{"Rome", "London", "Athens", "Madrid"}; // Paris is not in options
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            question.setOptions(newOptions);
        });
        
        assertTrue(exception.getMessage().contains("New options must include the correct answer"), 
                "Exception message should mention that new options must include correct answer");
        
        System.out.println("[DEBUG_LOG] Set options with invalid correct answer test passed");
    }
}