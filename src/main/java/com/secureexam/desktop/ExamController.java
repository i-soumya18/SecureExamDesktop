package com.secureexam.desktop;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExamController {

    @FXML
    private Label timerLabel;

    @FXML
    private Label questionLabel;

    @FXML
    private Label questionNumberLabel;

    @FXML
    private RadioButton option1, option2, option3, option4;

    @FXML
    private ToggleGroup optionsGroup;

    @FXML
    private Button previousButton, nextButton, submitButton, flagButton;

    @FXML
    private ProgressBar progressBar;

    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Timeline timer;
    private int timeRemaining = 30 * 60; // 30 minutes in seconds

    @FXML
    private void initialize() {
        questions = new ArrayList<>();
        questions.add(new Question("exam123", "What is the capital of France?", new String[]{"Paris", "Florida", "Python", "MongoDB"}, "Paris"));
        questions.add(new Question("exam123", "What is 2 + 2?", new String[]{"3", "4", "5", "6"}, "4"));

        startTimer();
        loadQuestion(currentQuestionIndex);
        updateProgressBar();
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeRemaining--;
            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;
            timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
            if (timeRemaining <= 0) {
                timer.stop();
                handleSubmit(null);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void loadQuestion(int index) {
        Question question = questions.get(index);
        questionNumberLabel.setText(String.format("Question %d of %d", index + 1, questions.size()));
        questionLabel.setText(question.getText());
        option1.setText(question.getOptions()[0]);
        option2.setText(question.getOptions()[1]);
        option3.setText(question.getOptions()[2]);
        option4.setText(question.getOptions()[3]);
        previousButton.setDisable(index == 0);
        nextButton.setDisable(index == questions.size() - 1);
        updateProgressBar();
    }

    private void updateProgressBar() {
        double progress = (double) (currentQuestionIndex + 1) / questions.size();
        progressBar.setProgress(progress);
    }

    @FXML
    private void handlePrevious(ActionEvent event) {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            loadQuestion(currentQuestionIndex);
        }
    }

    @FXML
    private void handleNext(ActionEvent event) {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            loadQuestion(currentQuestionIndex);
        }
    }

    @FXML
    private void handleFlag(ActionEvent event) {
        // Placeholder for flagging logic
        System.out.println("Question " + (currentQuestionIndex + 1) + " flagged");
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        if (event != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to submit your exam?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (!result.isPresent() || result.get() != ButtonType.YES) {
                return;
            }
        }

        timer.stop();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/student_dashboard.fxml"));
            Stage stage = (Stage) submitButton.getScene().getWindow();
            Scene newScene = new Scene(root, 800, 600);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setOnFinished(e -> stage.setScene(newScene));
            fadeIn.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}