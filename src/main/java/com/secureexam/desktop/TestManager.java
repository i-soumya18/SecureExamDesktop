package com.secureexam.desktop;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages test series and questions for the SecureExam Desktop app.
 */
public class TestManager {
    private static final Logger LOGGER = Logger.getLogger(TestManager.class.getName());
    private static final List<Question> DEFAULT_QUESTIONS = initializeDefaultQuestions();

    private static List<Question> initializeDefaultQuestions() {
        List<Question> defaults = new ArrayList<>();
        try {
            defaults.add(new Question("default123", "What is the capital of France?",
                    new String[]{"Paris", "Florida", "Python", "MongoDB"}, "Paris"));
            defaults.add(new Question("default123", "What is 2 + 2?",
                    new String[]{"3", "4", "5", "6"}, "4"));
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize default questions", e);
        }
        return Collections.unmodifiableList(defaults);
    }

    public static List<Question> getQuestionsForTestSeries(String testSeries) {
        if (testSeries == null || testSeries.trim().isEmpty()) {
            LOGGER.warning("Test series is null or empty; returning default questions");
            return new ArrayList<>(DEFAULT_QUESTIONS);
        }

        String trimmedSeries = testSeries.trim();
        List<Question> questions = new ArrayList<>();
        try {
            switch (trimmedSeries) {
                case "Math Test Series":
                    questions.add(new Question("math123", "What is 2 + 2?",
                            new String[]{"1", "2", "3", "4"}, "4"));
                    questions.add(new Question("math123", "Solve: 5 × 3",
                            new String[]{"10", "15", "20", "25"}, "15"));
                    questions.add(new Question("math123", "What is the square root of 16?",
                            new String[]{"2", "4", "6", "8"}, "4"));
                    questions.add(new Question("math123", "What is 10 - 7?",
                            new String[]{"1", "2", "3", "4"}, "3"));
                    questions.add(new Question("math123", "What is 12 ÷ 4?",
                            new String[]{"2", "3", "4", "6"}, "3"));
                    break;

                case "Science Test Series":
                    questions.add(new Question("sci123", "What is H2O?",
                            new String[]{"Salt", "Sugar", "Water", "Oil"}, "Water"));
                    questions.add(new Question("sci123", "Which planet is closest to the Sun?",
                            new String[]{"Earth", "Mars", "Mercury", "Venus"}, "Mercury"));
                    questions.add(new Question("sci123", "What gas do plants absorb?",
                            new String[]{"Oxygen", "Carbon Dioxide", "Nitrogen", "Helium"}, "Carbon Dioxide"));
                    questions.add(new Question("sci123", "What is the boiling point of water?",
                            new String[]{"50°C", "75°C", "100°C", "125°C"}, "100°C"));
                    questions.add(new Question("sci123", "What is the primary source of Earth's energy?",
                            new String[]{"Moon", "Sun", "Wind", "Water"}, "Sun"));
                    break;

                case "History Test Series":
                    questions.add(new Question("hist123", "Who discovered America?",
                            new String[]{"Columbus", "Vasco da Gama", "Magellan", "Cook"}, "Columbus"));
                    questions.add(new Question("hist123", "In which year did World War II end?",
                            new String[]{"1940", "1945", "1950", "1955"}, "1945"));
                    questions.add(new Question("hist123", "What was the ancient name of Iraq?",
                            new String[]{"Persia", "Mesopotamia", "Egypt", "Rome"}, "Mesopotamia"));
                    questions.add(new Question("hist123", "Who was the first President of the USA?",
                            new String[]{"Lincoln", "Washington", "Jefferson", "Adams"}, "Washington"));
                    questions.add(new Question("hist123", "What event started World War I?",
                            new String[]{"Pearl Harbor", "Assassination of Archduke Franz Ferdinand", "Invasion of Poland", "Treaty of Versailles"}, "Assassination of Archduke Franz Ferdinand"));
                    break;

                default:
                    LOGGER.warning("Unknown test series: " + trimmedSeries + "; returning default questions");
                    return new ArrayList<>(DEFAULT_QUESTIONS);
            }

            if (questions.isEmpty()) {
                LOGGER.warning("No questions found for test series: " + trimmedSeries + "; returning default questions");
                return new ArrayList<>(DEFAULT_QUESTIONS);
            }

            Collections.shuffle(questions);
            for (Question q : questions) {
                try {
                    List<String> opts = new ArrayList<>(Arrays.asList(q.getOptions()));
                    Collections.shuffle(opts);
                    q.setOptions(opts.toArray(new String[0]));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.SEVERE, "Failed to shuffle options for question: " + q.getText(), e);
                }
            }
            LOGGER.info("Loaded " + questions.size() + " questions for " + trimmedSeries);
            return questions;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading questions for test series: " + trimmedSeries, e);
            return new ArrayList<>(DEFAULT_QUESTIONS);
        }
    }

    public static ObservableList<String> getTestSeries() {
        try {
            ObservableList<String> testSeries = FXCollections.observableArrayList(
                    "Math Test Series", "Science Test Series", "History Test Series");
            FXCollections.sort(testSeries, String::compareToIgnoreCase);
            LOGGER.info("Retrieved " + testSeries.size() + " test series");
            return testSeries;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving test series", e);
            return FXCollections.observableArrayList("Default Test Series");
        }
    }
}