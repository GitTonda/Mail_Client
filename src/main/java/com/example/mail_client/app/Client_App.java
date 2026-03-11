package com.example.mail_client.app;

import com.example.mail_client.model.Client_Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Client_App extends Application
{
    @Override
    public void start (Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader (Client_App.class.getResource ("hello-view.fxml"));
        Scene scene = new Scene (fxmlLoader.load (), 320, 240);

        Client_Model client_model = new Client_Model ();

        stage.setTitle ("Hello!");
        stage.setScene (scene);
        // stage.show (); // TODO uncomment this for GUI
    }
}
