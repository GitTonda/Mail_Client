package com.example.mail_client;

import com.example.mail_client.app.Client_App;
import javafx.application.Application;

public class Launcher
{
    /**
     * Main entry point for the application.
     *
     * @param args command line arguments
     */
    public static void main (String[] args)
    {
        Application.launch (Client_App.class, args);
    }
}
