package com.secureexam.desktop;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExamController {
    private static final Logger LOGGER = Logger.getLogger(ExamController.class.getName());
    private static final int MOUSE_EDGE_BUFFER = 10; // Pixels from edge to trap mouse
    private static final int MAX_FOCUS_LOSSES = 3;   // Max focus losses before auto-submit
    private static final int EXAM_DURATION_SECONDS = 30 * 60; // 30 minutes

    @FXML private Label timerLabel;
    @FXML private Label questionLabel;
    @FXML private Label questionNumberLabel;
    @FXML private RadioButton option1, option2, option3, option4;
    @FXML private ToggleGroup optionsGroup;
    @FXML private Button previousButton, nextButton, submitButton, flagButton, quitButton;
    @FXML private ProgressBar progressBar;

    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Timeline timer;
    private int timeRemaining = EXAM_DURATION_SECONDS;
    private List<String> userAnswers;
    private List<Boolean> flaggedQuestions;
    private String testSeries;
    private int focusLossCount = 0;
    private boolean isExamActive = false;

    public void setTestSeries(String testSeries) {
        this.testSeries = testSeries;
        initializeQuestions();
    }

    @FXML
    private void initialize() {
        // Ensure ToggleGroup is set up if FXML injection fails
        if (optionsGroup == null) {
            LOGGER.severe("optionsGroup injection failed; creating fallback");
            optionsGroup = new ToggleGroup();
            option1.setToggleGroup(optionsGroup);
            option2.setToggleGroup(optionsGroup);
            option3.setToggleGroup(optionsGroup);
            option4.setToggleGroup(optionsGroup);
        }
        if (testSeries != null) {
            initializeQuestions();
            setupLockdown();
        } else {
            LOGGER.warning("Test series not set during initialization");
        }
    }

    private void initializeQuestions() {
        try {
            questions = TestManager.getQuestionsForTestSeries(testSeries);
            if (questions == null || questions.isEmpty()) {
                LOGGER.severe("No questions loaded for test series: " + testSeries);
                showAlert(Alert.AlertType.ERROR, "Exam Error", "No questions available. Exiting exam.");
                Platform.exit();
                return;
            }
            userAnswers = new ArrayList<>(questions.size());
            flaggedQuestions = new ArrayList<>(questions.size());
            for (int i = 0; i < questions.size(); i++) {
                userAnswers.add(null);
                flaggedQuestions.add(false);
            }
            startTimer();
            loadQuestion(currentQuestionIndex);
            updateProgressBar();
            isExamActive = true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize questions", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Unable to start exam: " + e.getMessage());
            Platform.exit();
        }
    }

    private void setupLockdown() {
        Stage stage = (Stage) timerLabel.getScene().getWindow();
        Scene scene = stage.getScene();

        // Enforce fullscreen and prevent exit
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnCloseRequest(event -> {
            LOGGER.warning("Attempted window close blocked");
            event.consume();
        });

        // Block all keyboard shortcuts
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            LOGGER.info("Blocked key press: " + event.getCode());
            event.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, KeyEvent::consume);
        scene.addEventFilter(KeyEvent.KEY_TYPED, KeyEvent::consume);

        // Trap mouse near screen edges
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            double x = event.getScreenX();
            double y = event.getScreenY();
            if (x < screenBounds.getMinX() + MOUSE_EDGE_BUFFER || x > screenBounds.getMaxX() - MOUSE_EDGE_BUFFER ||
                y < screenBounds.getMinY() + MOUSE_EDGE_BUFFER || y > screenBounds.getMaxY() - MOUSE_EDGE_BUFFER) {
                LOGGER.warning("Mouse near edge at (" + x + ", " + y + "); refocusing");
                Platform.runLater(() -> stage.getScene().getRoot().requestFocus());
            }
        });

        // Monitor focus loss
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && isExamActive) {
                focusLossCount++;
                LOGGER.warning("Focus lost; count: " + focusLossCount);
                Platform.runLater(() -> {
                    stage.requestFocus();
                    if (focusLossCount >= MAX_FOCUS_LOSSES) {
                        LOGGER.severe("Max focus losses reached; auto-submitting");
                        handleSubmit(null);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Focus Warning", "Stay in the exam window. Attempt " + focusLossCount + " of " + MAX_FOCUS_LOSSES + ".");
                    }
                });
            }
        });
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeRemaining--;
            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;
            timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
            if (timeRemaining <= 0) {
                timer.stop();
                LOGGER.info("Time expired; auto-submitting");
                handleSubmit(null);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void loadQuestion(int index) {
        if (index < 0 || index >= questions.size()) {
            LOGGER.warning("Invalid question index: " + index);
            return;
        }
        Question question = questions.get(index);
        questionNumberLabel.setText(String.format("Question %d of %d", index + 1, questions.size()));
        questionLabel.setText(question.getText());
        String[] options = question.getOptions();
        option1.setText(options[0]);
        option2.setText(options[1]);
        option3.setText(options[2]);
        option4.setText(options[3]);

        String selectedAnswer = userAnswers.get(index);
        if (selectedAnswer != null) {
            option1.setSelected(options[0].equals(selectedAnswer));
            option2.setSelected(options[1].equals(selectedAnswer));
            option3.setSelected(options[2].equals(selectedAnswer));
            option4.setSelected(options[3].equals(selectedAnswer));
        } else {
            optionsGroup.getToggles().forEach(toggle -> ((RadioButton) toggle).setSelected(false));
        }

        previousButton.setDisable(index == 0);
        nextButton.setDisable(index == questions.size() - 1);
        submitButton.setVisible(index == questions.size() - 1);
        flagButton.setText(flaggedQuestions.get(index) ? "Unflag" : "Flag");
    }

    private void updateProgressBar() {
        double progress = (double) (currentQuestionIndex + 1) / questions.size();
        progressBar.setProgress(progress);
    }

    @FXML
    private void handlePrevious(ActionEvent event) {
        saveAnswer();
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            loadQuestion(currentQuestionIndex);
            LOGGER.info("Navigated to previous question: " + (currentQuestionIndex + 1));
        }
    }

    @FXML
    private void handleNext(ActionEvent event) {
        saveAnswer();
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            loadQuestion(currentQuestionIndex);
            LOGGER.info("Navigated to next question: " + (currentQuestionIndex + 1));
        }
    }

    @FXML
    private void handleFlag(ActionEvent event) {
        boolean isFlagged = flaggedQuestions.get(currentQuestionIndex);
        flaggedQuestions.set(currentQuestionIndex, !isFlagged);
        flagButton.setText(!isFlagged ? "Unflag" : "Flag");
        LOGGER.info("Question " + (currentQuestionIndex + 1) + " " + (!isFlagged ? "flagged" : "unflagged"));
        showAlert(Alert.AlertType.INFORMATION, "Flag Status", "Question " + (currentQuestionIndex + 1) + " " + (!isFlagged ? "flagged" : "unflagged") + ".");
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        saveAnswer();
        if (event != null) { // Null event means auto-submit (time out or focus loss)
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to submit your exam?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (!result.isPresent() || result.get() != ButtonType.YES) return;
        }
        endExam("submitted", calculateScore());
    }

    @FXML
    private void handleQuit(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to quit? Your progress will be lost.", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            endExam("quit", 0);
        }
    }

    private void saveAnswer() {
        RadioButton selected = (RadioButton) optionsGroup.getSelectedToggle();
        userAnswers.set(currentQuestionIndex, selected != null ? selected.getText() : null);
    }

    private int calculateScore() {
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getCorrectAnswer().equals(userAnswers.get(i))) {
                score++;
            }
        }
        return score;
    }

    private void endExam(String reason, int score) {
        if (timer != null) {
            timer.stop();
        }
        isExamActive = false;
        LOGGER.info("Exam " + reason + "; score: " + score);
        Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
        resultAlert.setTitle("Exam " + reason.substring(0, 1).toUpperCase() + reason.substring(1));
        resultAlert.setHeaderText("Exam Completed");
        resultAlert.setContentText("You scored " + score + " out of " + questions.size() + ".\nFocus losses: " + focusLossCount);
        resultAlert.showAndWait();

        releaseLockdown();
        returnToDashboard();
    }

    private void releaseLockdown() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.setFullScreen(false);
        stage.setOnCloseRequest(null); // Re-enable window close
        LOGGER.info("Lockdown released");
    }

    private void returnToDashboard() {
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
            LOGGER.log(Level.SEVERE, "Failed to return to dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to return to dashboard: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}