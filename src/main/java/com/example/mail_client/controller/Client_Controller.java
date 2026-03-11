package com.example.mail_client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Client_Controller
{
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick ()
    {
        welcomeText.setText ("Welcome to JavaFX Application!");
    }
}
