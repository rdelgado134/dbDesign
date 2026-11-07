package com.db440;

import java.io.IOException;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Window;

public class ItemBrowserController {

    private final JdbcDao jdbcDao = new JdbcDao();

    @FXML
    private TextField categorySearchField;

    @FXML
    private Label resultsLabel;

    @FXML
    private ListView<Item> itemList;

    @FXML
    private Label descriptionLabel;

    @FXML
    private void initialize() {
        ItemListings.setUp(itemList, descriptionLabel);
        showAll(null);
    }

    @FXML
    public void search(ActionEvent event) {
        String category;
        try {
            category = ItemInput.category(categorySearchField.getText());
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, owner(), "Search Error!", e.getMessage());
            return;
        }

        try {
            ItemListings.show(itemList, jdbcDao.findItemsByCategory(category));
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError();
            return;
        }
        categorySearchField.setText(category);
        resultsLabel.setText("Items in category \"" + category + "\"");
        itemList.setPlaceholder(new Label("No items in category \"" + category + "\"."));
    }

    @FXML
    public void showAll(ActionEvent event) {
        try {
            ItemListings.show(itemList, jdbcDao.findAllItems());
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError();
            return;
        }
        categorySearchField.clear();
        resultsLabel.setText("All items");
        itemList.setPlaceholder(new Label("No items have been posted yet."));
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        App.setRoot("main_page");
    }

    // The list isn't attached to a window yet while initialize() runs.
    private Window owner() {
        Scene scene = itemList.getScene();
        return scene == null ? null : scene.getWindow();
    }

    private void showDatabaseError() {
        showAlert(Alert.AlertType.ERROR, owner(), "Database Error!",
            "Could not reach the database. Check the terminal for details.");
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
