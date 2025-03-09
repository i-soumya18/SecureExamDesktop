package com.secureexam.desktop;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private AuthService authService;

    @Autowired
    private QuestionRepository questionRepository;

    @GetMapping("/test")
    public String testEndpoint() {
        return "Backend is running!";
    }

    @GetMapping("/register")
    public String registerUser(@RequestParam String email, @RequestParam String password) {
        try {
            UserRecord user = authService.registerUser(email, password);
            return "User registered: " + user.getUid();
        } catch (FirebaseAuthException e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/add-question")
    public String addQuestion() {
        String[] options = {"Paris", "Florida", "Python", "MongoDB"};
        Question question = new Question("exam123", "What is the capital of France?", options, "Paris");
        questionRepository.save(question);
        return "Question added: " + question.getId();
    }
}