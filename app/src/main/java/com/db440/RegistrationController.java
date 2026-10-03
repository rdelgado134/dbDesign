package com.db440;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public class RegistrationController {

    // Must match the column sizes in schema.sql.
    private static final int MAX_USERNAME = 50;
    private static final int MAX_NAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MIN_PHONE_DIGITS = 7;
    private static final int MAX_PHONE_DIGITS = 15;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private Button submitButton;

    @FXML
    public void register(ActionEvent event) {

        Window owner = submitButton.getScene().getWindow();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String rawPhone = phoneField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || rawPhone.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Please fill in every field");
            return;
        }

        if (username.length() > MAX_USERNAME || firstName.length() > MAX_NAME
                || lastName.length() > MAX_NAME || email.length() > MAX_EMAIL) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Username and names are limited to " + MAX_NAME + " characters and email to "
                    + MAX_EMAIL);
            return;
        }

        // Store phones as digits only so "(555) 123-4567" and "555-123-4567"
        // collide on the unique constraint instead of creating two accounts.
        if (!rawPhone.matches("[0-9()+\\-.\\s]+")) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Phone may only contain digits, spaces and ( ) - . +");
            return;
        }
        String phone = rawPhone.replaceAll("\\D", "");
        if (phone.length() < MIN_PHONE_DIGITS || phone.length() > MAX_PHONE_DIGITS) {
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Phone must have between " + MIN_PHONE_DIGITS + " and " + MAX_PHONE_DIGITS + " digits");
            return;
        }

        if (!password.equals(confirmPassword)) {
            passwordField.clear();
            confirmPasswordField.clear();
            showAlert(Alert.AlertType.ERROR, owner, "Form Error!",
                "Passwords do not match");
            return;
        }

        JdbcDao jdbcDao = new JdbcDao();
        try {
            List<String> taken = new ArrayList<>();
            if (jdbcDao.usernameExists(username)) {
                taken.add("Username \"" + username + "\" is already taken");
            }
            if (jdbcDao.emailExists(email)) {
                taken.add("Email \"" + email + "\" is already registered");
            }
            if (jdbcDao.phoneExists(phone)) {
                taken.add("Phone \"" + rawPhone + "\" is already registered");
            }
            if (!taken.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, owner, "Sign Up Failed!",
                    String.join("\n", taken));
                return;
            }

            jdbcDao.insertUser(username, password, firstName, lastName, email, phone);
        } catch (SQLIntegrityConstraintViolationException e) {
            // Someone else registered the same username/email/phone after our checks ran.
            showAlert(Alert.AlertType.ERROR, owner, "Sign Up Failed!",
                "That username, email or phone was just registered. Please try another.");
            return;
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showAlert(Alert.AlertType.ERROR, owner, "Database Error!",
                "Could not connect to the database. Please try again later.");
            return;
        }

        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Sign Up Successful!");
        success.setHeaderText(null);
        success.setContentText("Welcome " + firstName + "! You can now log in.");
        success.initOwner(owner);
        success.showAndWait();

        try {
            App.setRoot("login_form");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) throws IOException {
        App.setRoot("login_form");
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
