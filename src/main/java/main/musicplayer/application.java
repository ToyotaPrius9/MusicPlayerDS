package main.musicplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class application extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(application.class.getResource("window.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 830, 400);

        stage.setTitle("Musik");
        // Set the CSS file
        String cssPath = getClass().getResource("/main/musicplayer/style.css").toExternalForm();
        Application.setUserAgentStylesheet(cssPath);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }



}