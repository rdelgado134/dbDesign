package com.db440;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    public void login(ActionEvent event) {

        Window owner = loginButton.getScene().getWindow();

        if (usernameField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Please enter your username");
            return;
        }
        if (passwordField.getText().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Please enter your password");
            return;
        }

        String username = usernameField.getText();
        char[] password = passwordField.getText().toCharArray();

        Optional<String> storedHash;
        try {
            storedHash = new JdbcDao().findPasswordHash(username);
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showAlert(Alert.AlertType.ERROR, owner, "Database Error!",
                "Could not connect to the database. Please try again later.");
            return;
        }

        // Same message whether the username or the password is wrong,
        // so the form doesn't reveal which usernames are registered.
        if (storedHash.isEmpty() || !PasswordHasher.verify(password, storedHash.get())) {
            passwordField.clear();
            showAlert(Alert.AlertType.ERROR, owner, "Login Failed!",
                "Invalid username or password");
            return;
        }

        try {
            WelcomeController welcome = App.setRoot("welcome");
            welcome.setUser(username);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, owner, "Error!",
                "Could not open the welcome page.");
        }
    }

    @FXML
    public void goToRegistration(ActionEvent event) throws IOException {
        App.setRoot("registration_form");
    }

    private static void showAlert(Alert.AlertType alertType, Window owner, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.show();
    }

}
