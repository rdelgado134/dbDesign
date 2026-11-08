package com.db440;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
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
        clearResults();

        queryTitleLabel.setText("Query 1: Display the most expensive item(s) in each category.\nIf there is more than one item with the highest price, show them all."); // Sets the Title of the query to be executed in the center section of the page

        inputContainer.getChildren().clear(); // Clears any inputs that may have been inserted in a previous query selection

        // This query does not have any users inputs, it is simply executed.
        // The above clear cleans of any inputs, and then no new ones are provided.
    }

    @FXML
    public void selectQuery2() {
        currentQueryID = 2;
        clearResults();

        queryTitleLabel.setText("Query 2: Display all users who posted at least 2 different items on the same day.\nOne item must be from category 1, and the other item from category 2.");

        inputContainer.getChildren().clear();

        Label label1 = new Label("Category 1: ");
        categoryField1 = new TextField();
        categoryField1.setPromptText("e.g. electronics");
        Label label2 = new Label("Category 2: ");
        categoryField2 = new TextField();
        categoryField2.setPromptText("e.g. books");

        inputContainer.getChildren().addAll(label1, categoryField1, label2, categoryField2); // Collects and sends the above units to the page's' input fields section
    }

    @FXML
    public void selectQuery3() {
        currentQueryID = 3;
        clearResults();

        queryTitleLabel.setText("Query 3: Display all items posted by a specified user that satisfy the following conditions:\n1) The item has received at least one review\n2) Every review for that item is either Excellent or Good");

        inputContainer.getChildren().clear();

        Label label = new Label("Username: ");
        usernameField = new TextField();
        usernameField.setPromptText("e.g. alice");

        inputContainer.getChildren().addAll(label, usernameField);
    }

    @FXML
    public void selectQuery4() {
        currentQueryID = 4;
        clearResults();

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
        clearResults();

        queryTitleLabel.setText("Query 5: Display all users who have submitted one or more reviews,\nwhere every review they have written is rated “Poor”.");

        inputContainer.getChildren().clear();
    }

    @FXML
    public void selectQuery6() {
        currentQueryID = 6;
        clearResults();

        queryTitleLabel.setText("Query 6: Display all users whose posted items have never received a “Poor” review.\nItems with no reviews also count as never receiving a Poor review.");

        inputContainer.getChildren().clear();
    }

    public QueryResult executeQuery1() throws SQLException {
        return dao.executeQuery1();
    }

    public QueryResult executeQuery2() throws SQLException {
        String category1 = ItemInput.category(categoryField1.getText());
        String category2 = ItemInput.category(categoryField2.getText());
        categoryField1.setText(category1);
        categoryField2.setText(category2);

        return dao.executeQuery2(category1, category2);
    }

    public QueryResult executeQuery3() throws SQLException {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("Please enter a username");
        }

        return dao.executeQuery3(username);
    }

    public QueryResult executeQuery4() throws SQLException {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Please pick a date");
        }

        return dao.executeQuery4(date);
    }

    public QueryResult executeQuery5() throws SQLException {
        return dao.executeQuery5();
    }

    public QueryResult executeQuery6() throws SQLException {
        return dao.executeQuery6();
    }

    @FXML 
    private void handleRunQuery(){
        QueryResult result;
        try {
            switch (currentQueryID) {
                case 1: result = executeQuery1(); break;
                case 2: result = executeQuery2(); break;
                case 3: result = executeQuery3(); break;
                case 4: result = executeQuery4(); break;
                case 5: result = executeQuery5(); break;
                case 6: result = executeQuery6(); break;
                default:
                    showAlert(Alert.AlertType.INFORMATION, "No Query Selected", "Please select a query first.");
                    return;
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Input Error!", e.getMessage());
            return;
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showAlert(Alert.AlertType.ERROR, "Database Error!",
                "Could not run the query. Check the terminal for details.");
            return;
        }
        showResult(result);
    }

    // Builds one table column per result column, then fills in the rows.
    private void showResult(QueryResult result) {
        // Rebuild the columns with auto-sizing off, so every column starts the same
        // width; JavaFX 20+ would otherwise give the first new column all the space.
        resultsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        resultsTable.getColumns().clear();

        List<String> columns = result.getColumns();
        double columnWidth = Math.max(100, (resultsTable.getWidth() - 20) / columns.size());
        for (int i = 0; i < columns.size(); i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columns.get(i));
            column.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(columnIndex)));
            column.setPrefWidth(columnWidth);
            resultsTable.getColumns().add(column);
        }

        ObservableList<ObservableList<String>> rows = FXCollections.observableArrayList();
        for (List<String> row : result.getRows()) {
            rows.add(FXCollections.observableArrayList(row));
        }
        resultsTable.setItems(rows);
        resultsTable.setPlaceholder(new Label("No rows match this query."));

        // Turn auto-sizing back on so the equal-width columns stretch to fill the table.
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void clearResults() {
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        resultsTable.setPlaceholder(new Label("Click \"Run Query\" to see the results."));
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (resultsTable.getScene() != null) {
            alert.initOwner(resultsTable.getScene().getWindow());
        }
        alert.show();
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        App.setRoot("main_page");

        inputContainer.getChildren().clear();
    }

}