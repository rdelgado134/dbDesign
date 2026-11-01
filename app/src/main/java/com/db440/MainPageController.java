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
        App.setRoot("myItems");
    }

    @FXML 
    public void goToItemBrowser(ActionEvent event) throws IOException { 
        App.setRoot("itemBrowser");
    }

    @FXML 
    public void goToProjectSQLQueries(ActionEvent event) throws IOException { 
        App.setRoot("projectSQLQueries");
    }

    @FXML
    public void logout(ActionEvent event) throws IOException {
        App.setRoot("login_form");
    }

    public void setUser(String username) {
        welcomeLabel.setText("Welcome, " + username + "!");
    }

}
