package com.secureexam.desktop;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestManager {
    private static final Logger LOGGER = Logger.getLogger(TestManager.class.getName());
    private static final Firestore db = FirestoreClient.getFirestore();

    public static List<Question> getQuestionsForTestSeries(String examId) {
        if (examId == null || examId.trim().isEmpty()) {
            LOGGER.warning("Exam ID is null or empty; returning empty list");
            return new ArrayList<>();
        }

        try {
            if (!NetworkManager.isOnline() && LocalCache.isCached(examId)) {
                LOGGER.info("Offline mode: Loading questions from cache for examId: " + examId);
                return LocalCache.getQuestions(examId);
            }

            DocumentSnapshot examDoc = db.collection("exams").document(examId).get().get();
            if (!examDoc.exists()) {
                LOGGER.warning("No exam found for examId: " + examId);
                return LocalCache.getQuestions(examId); // Fallback to cache
            }

            List<Map<String, Object>> questionData = (List<Map<String, Object>>) examDoc.get("questions");
            if (questionData == null || questionData.isEmpty()) {
                LOGGER.warning("No questions found for examId: " + examId);
                return LocalCache.getQuestions(examId); // Fallback to cache
            }

            List<Question> questions = new ArrayList<>();
            for (Map<String, Object> q : questionData) {
                List<String> options = (List<String>) q.get("options");
                if (options.size() != 4) { // Enforce MCQ with exactly 4 options
                    LOGGER.warning("Invalid MCQ format for question: " + q.get("text"));
                    continue;
                }
                Question question = new Question(
                    examId,
                    (String) q.get("text"),
                    options.toArray(new String[0]),
                    (String) q.get("correctAnswer")
                );
                questions.add(question);
            }

            Collections.shuffle(questions);
            for (Question q : questions) {
                List<String> opts = new ArrayList<>(Arrays.asList(q.getOptions()));
                Collections.shuffle(opts);
                q.setOptions(opts.toArray(new String[0]));
            }

            LocalCache.saveQuestions(examId, questions);
            LOGGER.info("Loaded and cached " + questions.size() + " MCQ questions for examId: " + examId);
            return questions;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading questions for examId: " + examId, e);
            return LocalCache.getQuestions(examId); // Fallback to cache
        }
    }
}