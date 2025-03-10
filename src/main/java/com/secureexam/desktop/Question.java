package com.secureexam.desktop;

import java.util.Arrays;

public class Question {
    private String examId;
    private String text;
    private String[] options;
    private String correctAnswer;

    public Question(String examId, String text, String[] options, String correctAnswer) {
        if (examId == null || text == null || options == null || correctAnswer == null) {
            throw new IllegalArgumentException("All question fields must be non-null");
        }
        if (options.length < 2) {
            throw new IllegalArgumentException("Question must have at least 2 options");
        }
        if (!Arrays.asList(options).contains(correctAnswer)) {
            throw new IllegalArgumentException("Correct answer must be one of the options");
        }
        this.examId = examId;
        this.text = text;
        this.options = options.clone(); // Defensive copy to prevent external modification
        this.correctAnswer = correctAnswer;
    }

    public String getExamId() { return examId; }
    public String getText() { return text; }
    public String[] getOptions() { return options.clone(); } // Defensive copy
    public String getCorrectAnswer() { return correctAnswer; }

    public void setOptions(String[] options) {
        if (options == null || options.length < 2) {
            throw new IllegalArgumentException("Options array must be non-null and have at least 2 elements");
        }
        if (!Arrays.asList(options).contains(correctAnswer)) {
            throw new IllegalArgumentException("New options must include the correct answer");
        }
        this.options = options.clone(); // Defensive copy
    }
}