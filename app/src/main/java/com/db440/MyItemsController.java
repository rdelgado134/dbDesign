package com.db440;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public class MyItemsController {

    private final JdbcDao jdbcDao = new JdbcDao();

    @FXML
    private ListView<Item> itemList;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Button updatePriceButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button addCategoriesButton;

    @FXML
    private Button removeCategoryButton;

    @FXML
    private void initialize() {
        ItemListings.setUp(itemList, descriptionLabel);
        itemList.setPlaceholder(new Label("You haven't posted any items yet. Click \"Add Item\" to post one."));

        BooleanBinding nothingSelected = itemList.getSelectionModel().selectedItemProperty().isNull();
        updatePriceButton.disableProperty().bind(nothingSelected);
        deleteButton.disableProperty().bind(nothingSelected);
        addCategoriesButton.disableProperty().bind(nothingSelected);
        removeCategoryButton.disableProperty().bind(nothingSelected);

        refresh();
    }

    @FXML
    public void addItem(ActionEvent event) {
        TextField titleField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);
        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 19.99");
        TextField categoriesField = new TextField();
        categoriesField.setPromptText("e.g. electronics, gaming");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Title:"), titleField);
        form.addRow(1, new Label("Description:"), descriptionArea);
        form.addRow(2, new Label("Price:"), priceField);
        form.addRow(3, new Label("Categories:"), categoriesField);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Item");
        dialog.initOwner(owner());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Save before the dialog closes, so a typo doesn't throw away everything the user entered.
        Button postButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        postButton.setText("Post");
        postButton.addEventFilter(ActionEvent.ACTION, click -> {
            Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
            try {
                jdbcDao.insertItem(Session.getUsername(),
                    ItemInput.title(titleField.getText()),
                    ItemInput.description(descriptionArea.getText()),
                    ItemInput.price(priceField.getText()),
                    ItemInput.categories(categoriesField.getText()));
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, dialogWindow, "Form Error!", e.getMessage());
                click.consume();
            } catch (SQLException e) {
                JdbcDao.printSQLException(e);
                showDatabaseError(dialogWindow);
                click.consume();
            }
        });

        if (dialog.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            refresh();
        }
    }

    @FXML
    public void updatePrice(ActionEvent event) {
        Item item = itemList.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(item.getPrice().toPlainString());
        dialog.setTitle("Update Price");
        dialog.setHeaderText(null);
        dialog.setContentText("New price for \"" + item.getTitle() + "\":");
        dialog.initOwner(owner());
        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            return;
        }

        try {
            BigDecimal price = ItemInput.price(input.get());
            if (!jdbcDao.updateItemPrice(item.getItemId(), Session.getUsername(), price)) {
                showItemGone();
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, owner(), "Form Error!", e.getMessage());
            return;
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError(owner());
            return;
        }
        refresh();
    }

    @FXML
    public void deleteItem(ActionEvent event) {
        Item item = itemList.getSelectionModel().getSelectedItem();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + item.getTitle() + "\"? Its reviews will be deleted too.");
        confirm.setTitle("Delete Item");
        confirm.setHeaderText(null);
        confirm.initOwner(owner());
        if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            if (!jdbcDao.deleteItem(item.getItemId(), Session.getUsername())) {
                showItemGone();
            }
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError(owner());
            return;
        }
        refresh();
    }

    @FXML
    public void addCategories(ActionEvent event) {
        Item item = itemList.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Categories");
        dialog.setHeaderText(null);
        dialog.setContentText("Categories to add to \"" + item.getTitle() + "\" (comma separated):");
        dialog.getEditor().setPromptText("e.g. electronics, gaming");
        dialog.initOwner(owner());
        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            return;
        }

        try {
            List<String> newCategories = new ArrayList<>(ItemInput.categories(input.get()));
            newCategories.removeAll(item.getCategories());
            if (newCategories.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, owner(), "Nothing to Add",
                    "\"" + item.getTitle() + "\" already has those categories.");
                return;
            }
            if (!jdbcDao.addCategories(item.getItemId(), Session.getUsername(), newCategories)) {
                showItemGone();
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, owner(), "Form Error!", e.getMessage());
            return;
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError(owner());
            return;
        }
        refresh();
    }

    @FXML
    public void removeCategory(ActionEvent event) {
        Item item = itemList.getSelectionModel().getSelectedItem();
        List<String> categories = item.getCategories();
        if (categories.size() <= 1) {
            showAlert(Alert.AlertType.ERROR, owner(), "Cannot Remove Category",
                "Every item needs at least one category. Add another category to \""
                    + item.getTitle() + "\" before removing this one.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(categories.get(0), categories);
        dialog.setTitle("Remove Category");
        dialog.setHeaderText(null);
        dialog.setContentText("Category to remove from \"" + item.getTitle() + "\":");
        dialog.initOwner(owner());
        Optional<String> category = dialog.showAndWait();
        if (category.isEmpty()) {
            return;
        }

        try {
            if (!jdbcDao.removeCategory(item.getItemId(), Session.getUsername(), category.get())) {
                showAlert(Alert.AlertType.ERROR, owner(), "Cannot Remove Category",
                    "The category could not be removed. The item may have changed; the list has been refreshed.");
            }
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError(owner());
            return;
        }
        refresh();
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        App.setRoot("main_page");
    }

    private void refresh() {
        try {
            ItemListings.show(itemList, jdbcDao.findItemsBySeller(Session.getUsername()));
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError(owner());
        }
    }

    private void showItemGone() {
        showAlert(Alert.AlertType.ERROR, owner(), "Item Not Found",
            "That item no longer exists or isn't yours. The list has been refreshed.");
    }

    // The list isn't attached to a window yet while initialize() runs.
    private Window owner() {
        Scene scene = itemList.getScene();
        return scene == null ? null : scene.getWindow();
    }

    private static void showDatabaseError(Window owner) {
        showAlert(Alert.AlertType.ERROR, owner, "Database Error!",
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
