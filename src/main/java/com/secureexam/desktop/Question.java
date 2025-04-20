package com.secureexam.desktop;

import java.util.Arrays;

public class Question {
    private String examId;
    private String text;
    private String[] options; // Always 4 options for MCQ
    private String correctAnswer;

    public Question(String examId, String text, String[] options, String correctAnswer) {
        if (examId == null || text == null || options == null || correctAnswer == null) {
            throw new IllegalArgumentException("All question fields must be non-null");
        }
        if (options.length != 4) {
            throw new IllegalArgumentException("MCQ must have exactly 4 options");
        }
        if (!Arrays.asList(options).contains(correctAnswer)) {
            throw new IllegalArgumentException("Correct answer must be one of the options");
        }
        this.examId = examId;
        this.text = text;
        this.options = options.clone();
        this.correctAnswer = correctAnswer;
    }

    public String getExamId() { return examId; }
    public String getText() { return text; }
    public String[] getOptions() { return options.clone(); }
    public String getCorrectAnswer() { return correctAnswer; }

    public void setOptions(String[] options) {
        if (options == null || options.length != 4) {
            throw new IllegalArgumentException("MCQ options must be non-null and exactly 4");
        }
        if (!Arrays.asList(options).contains(correctAnswer)) {
            throw new IllegalArgumentException("New options must include the correct answer");
        }
        this.options = options.clone();
    }
}