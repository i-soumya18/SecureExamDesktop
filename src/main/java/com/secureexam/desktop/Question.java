package com.secureexam.desktop;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "questions")
public class Question {

    @Id
    private String id;
    private String examId;
    private String text;
    private String[] options;
    private String correctAnswer;

    // Getters, setters, and constructors
    public Question() {}

    public Question(String examId, String text, String[] options, String correctAnswer) {
        this.examId = examId;
        this.text = text;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
}