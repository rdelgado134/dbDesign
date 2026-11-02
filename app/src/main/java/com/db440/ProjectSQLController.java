package com.db440;

import java.io.IOException;
import java.time.LocalDate;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class ProjectSQLController {

    private JdbcDao dao = new JdbcDao();

    private int currentQueryID = 0;

    @FXML 
    private Label queryTitleLabel;

    @FXML 
    private TextField categoryField1, categoryField2, usernameField;

    @FXML 
    private DatePicker datePicker;

    @FXML 
    private HBox inputContainer;

    @FXML
    private Button query1, query2, query3, query4, query5, query6;

    @FXML
    private Button backButton;

    @FXML 
    private TableView<ObservableList<String>> resultsTable;

    @FXML
    public void selectQuery1() {
        currentQueryID = 1;

        queryTitleLabel.setText("Query 1: Display the most expensive item(s) in each category.\nIf there is more than one item with the highest price, show them all."); // Sets the Title of the query to be executed in the center section of the page

        inputContainer.getChildren().clear(); // Clears any inputs that may have been inserted in a previous query selection

        // This query does not have any users inputs, it is simply executed.
        // The above clear cleans of any inputs, and then no new ones are provided.
    }

    @FXML
    public void selectQuery2() {
        currentQueryID = 2;

        queryTitleLabel.setText("Query 2: Display all users who posted at least 2 different items on the same day.\nOne item must be from category 1, and the other item from category 2.");

        inputContainer.getChildren().clear();

        Label label1 = new Label("Category 1: ");
        categoryField1 = new TextField();
        categoryField1.setPromptText("e.g. Electronics");
        Label label2 = new Label("Category 2: ");
        categoryField2 = new TextField();
        categoryField2.setPromptText("e.g. Books");

        inputContainer.getChildren().addAll(label1, categoryField1, label2, categoryField2); // Collects and sends the above units to the page's' input fields section
    }

    @FXML
    public void selectQuery3() {
        currentQueryID = 3;

        queryTitleLabel.setText("Query 3: Display all items posted by a specified user that satisfies the following conditions:\n1) Items has received at least one review\n2)Every review for that item is either Excellent or Good");

        inputContainer.getChildren().clear();

        Label label = new Label("Username: ");
        usernameField = new TextField();
        usernameField.setPromptText("e.g. JohnDoe64");

        inputContainer.getChildren().addAll(label, usernameField);
    }

    @FXML
    public void selectQuery4() {
        currentQueryID = 4;

        queryTitleLabel.setText("Query 4: Given a date entered by the user, display the user(s) who posted the largest number of items on that date.\nDisplay all users if there is a tie.");

        inputContainer.getChildren().clear();

        Label label = new Label("Date: ");
        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now()); // Verify this works. Intent is to default to today's date.

        inputContainer.getChildren().addAll(label, datePicker);
    }

    @FXML
    public void selectQuery5() {
        currentQueryID = 5;

        queryTitleLabel.setText("Query 5: Display all users who have submitted at least one review.\nFor each of these users, display every review they've written with a “poor” rating.");

        inputContainer.getChildren().clear();
    }

    @FXML
    public void selectQuery6() {
        currentQueryID = 6;

        queryTitleLabel.setText("Query 6: Display all users that have never received a “poor” review on any of their items.\nThis inludes items with no reviews.");

        inputContainer.getChildren().clear();
    }

    public void executeQuery1() {
        dao.executeQuery1(); //TODO: define this method in JdbcDao
    }

    public void executeQuery2() {
        String category1 = categoryField1.getText();
        String category2 = categoryField2.getText();

        dao.executeQuery2(category1, category2); //TODO: define this method in JdbcDao
    }

    public void executeQuery3() {
        String username = usernameField.getText();

        dao.executeQuery3(username); //TODO: define this method in JdbcDao
    }

    public void executeQuery4() {
        LocalDate date = datePicker.getValue();

        dao.executeQuery4(date); //TODO: define this method in JdbcDao
    }

    public void executeQuery5() {
        dao.executeQuery5(); //TODO: define this method in JdbcDao
    }

    public void executeQuery6() {
        dao.executeQuery6(); //TODO: define this method in JdbcDao
    }

    @FXML 
    private void handleRunQuery(){
        switch (currentQueryID) {
            case 1: executeQuery1(); break;
            case 2: executeQuery2(); break;
            case 3: executeQuery3(); break;
            case 4: executeQuery4(); break;
            case 5: executeQuery5(); break;
            case 6: executeQuery6(); break;
            default:
                System.out.println("Please select a query first.");
                break;
        }
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        App.setRoot("main_page");

        inputContainer.getChildren().clear();
    }

}