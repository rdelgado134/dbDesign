package com.db440;

import java.io.IOException;
import java.sql.SQLException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Window;

/**
 * Shows one item's reviews and lets the logged-in user add a review.
 * Opened from the Item Browser, which passes the item in with setItem().
 *
 * The form is disabled for the item's seller and for users who already reviewed it,
 * but the database enforces those rules (and the 2-per-day limit) on its own, so this
 * page only shows whatever message the database sends back.
 */
public class ItemReviewsController {

    private static final int MAX_COMMENT = 500;

    private final JdbcDao jdbcDao = new JdbcDao();

    private Item item;

    @FXML
    private Label itemLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private ListView<Review> reviewList;

    @FXML
    private Label formStatusLabel;

    @FXML
    private ComboBox<String> ratingBox;

    @FXML
    private TextArea commentArea;

    @FXML
    private Button submitButton;

    @FXML
    private void initialize() {
        ratingBox.getItems().setAll(Review.RATINGS);
        descriptionLabel.setWrapText(true);
        reviewList.setPlaceholder(new Label("No reviews yet."));
        reviewList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Review review, boolean empty) {
                super.updateItem(review, empty);
                setText(empty || review == null ? null : review.listingText());
                setWrapText(true);
                setPrefWidth(0); // let long comments wrap instead of scrolling sideways
            }
        });
    }

    /** Called by the Item Browser right after this page is loaded. */
    public void setItem(Item item) {
        this.item = item;
        itemLabel.setText(item.listingText());
        descriptionLabel.setText(item.hasDescription() ? item.getDescription() : "(no description)");
        refresh();
    }

    @FXML
    public void submitReview(ActionEvent event) {
        String rating = ratingBox.getValue();
        String comment = commentArea.getText().trim();
        if (rating == null) {
            showAlert(Alert.AlertType.ERROR, owner(), "Form Error!", "Please choose a rating");
            return;
        }
        if (comment.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, owner(), "Form Error!", "Please enter a short comment");
            return;
        }
        if (comment.length() > MAX_COMMENT) {
            showAlert(Alert.AlertType.ERROR, owner(), "Form Error!",
                "Comments are limited to " + MAX_COMMENT + " characters");
            return;
        }

        try {
            jdbcDao.insertReview(Session.getUsername(), item.getItemId(), rating, comment);
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            String ruleMessage = JdbcDao.ruleViolationMessage(e);
            if (ruleMessage != null) {
                showAlert(Alert.AlertType.ERROR, owner(), "Review Not Allowed", ruleMessage);
            } else {
                showDatabaseError();
            }
            return;
        }

        ratingBox.setValue(null);
        commentArea.clear();
        showAlert(Alert.AlertType.INFORMATION, owner(), "Review Submitted",
            "Thanks! Your review of \"" + item.getTitle() + "\" was posted.");
        refresh();
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        App.setRoot("item_browser");
    }

    private void refresh() {
        try {
            reviewList.getItems().setAll(jdbcDao.findReviewsForItem(item.getItemId()));
            updateForm();
        } catch (SQLException e) {
            JdbcDao.printSQLException(e);
            showDatabaseError();
        }
    }

    // Explains up front why a user can't review, instead of letting them type a comment first.
    private void updateForm() throws SQLException {
        String user = Session.getUsername();
        String reason = null;
        if (item.getSeller().equals(user)) {
            reason = "This is your item, so you can't review it.";
        } else if (jdbcDao.hasReviewed(user, item.getItemId())) {
            reason = "You have already reviewed this item. Reviews can't be changed after they are submitted.";
        }
        boolean canReview = reason == null;
        formStatusLabel.setText(canReview ? "Write a review (limit: 2 reviews per day):" : reason);
        ratingBox.setDisable(!canReview);
        commentArea.setDisable(!canReview);
        submitButton.setDisable(!canReview);
    }

    // The list isn't attached to a window yet while initialize() runs.
    private Window owner() {
        Scene scene = reviewList.getScene();
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
