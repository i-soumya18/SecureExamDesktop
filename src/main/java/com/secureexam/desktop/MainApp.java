package com.secureexam.desktop;
//https://grok.com/share/bGVnYWN5_2175083d-503e-45d3-ab3d-64e17afdc25e
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        /*if (!Files.exists(Path.of("src/main/resources/settings.json"))) {
            Parent setupRoot = FXMLLoader.load(getClass().getResource("/fxml/setup_wizard.fxml"));
            Stage setupStage = new Stage();
            setupStage.setScene(new Scene(setupRoot));
            setupStage.setTitle("Setup Wizard");
            setupStage.showAndWait();
        }*/

        FirebaseInitializer.initialize();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        primaryStage.setTitle(ConfigLoader.getInstituteName());
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}