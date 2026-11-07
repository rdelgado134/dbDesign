package com.db440;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

/** Shared setup for the item listings on the My Items and Item Browser pages. */
final class ItemListings {

    private static final String NO_SELECTION_TEXT = "Click a listing to see its description.";

    private ItemListings() {
    }

    /** Shows each item as one listing line, and its description in descriptionLabel when clicked. */
    static void setUp(ListView<Item> list, Label descriptionLabel) {
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.listingText());
            }
        });

        descriptionLabel.setWrapText(true);
        descriptionLabel.setText(NO_SELECTION_TEXT);
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, item) -> {
            if (item == null) {
                descriptionLabel.setText(NO_SELECTION_TEXT);
            } else {
                descriptionLabel.setText(item.getTitle() + ": "
                    + (item.hasDescription() ? item.getDescription() : "(no description)"));
            }
        });
    }

    /** Replaces the listings, keeping the same item selected if it is still there. */
    static void show(ListView<Item> list, List<Item> items) {
        Item selected = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(items);
        if (selected != null) {
            items.stream()
                .filter(item -> item.getItemId() == selected.getItemId())
                .findFirst()
                .ifPresent(item -> list.getSelectionModel().select(item));
        }
    }

}
