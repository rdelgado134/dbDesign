package com.db440;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainPageController {

    @FXML
    private Button myItemsButton;

    @FXML
    private Button itemBrowser;

    @FXML 
    private Button projectSQLQueries;

    @FXML
    private Label welcomeLabel;

    @FXML 
    public void goToMyItems(ActionEvent event) throws IOException { 
        App.setRoot("my_items");
    }

    @FXML 
    public void goToItemBrowser(ActionEvent event) throws IOException { 
        App.setRoot("item_browser");
    }

    @FXML 
    public void goToProjectSQLQueries(ActionEvent event) throws IOException { 
        App.setRoot("project_sql_queries");
    }

    @FXML
    private void initialize() {
        welcomeLabel.setText("Welcome, " + Session.getUsername() + "!");
    }

    @FXML
    public void logout(ActionEvent event) throws IOException {
        Session.logout();
        App.setRoot("login_form");
    }

}
