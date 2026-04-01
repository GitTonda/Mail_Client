package com.example.mail_client.controller;

import javafx.scene.control.Alert;

public class UI_Utils
{
    public static boolean is_invalid_email (String email)
    {
        String email_regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email == null || ! email.matches (email_regex);
    }

    public static void show_alert (Alert.AlertType type, String title, String message)
    {
        javafx.application.Platform.runLater (() -> {
            Alert alert = new Alert (type);
            alert.setTitle (title);
            alert.setHeaderText (null);
            alert.setContentText (message);
            alert.showAndWait ();
        });
    }
}