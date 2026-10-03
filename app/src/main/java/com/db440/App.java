package com.db440;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        scene = new Scene(loadFxml("login_form").load(), 800, 500);
        stage.setTitle("COMP 440");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Swaps the window's contents to /fxml/{name}.fxml and returns its controller,
     * so the caller can pass data to the new screen.
     */
    public static <T> T setRoot(String name) throws IOException {
        FXMLLoader loader = loadFxml(name);
        Parent root = loader.load();
        scene.setRoot(root);
        return loader.getController();
    }

    private static FXMLLoader loadFxml(String name) {
        return new FXMLLoader(App.class.getResource("/fxml/" + name + ".fxml"));
    }

}
