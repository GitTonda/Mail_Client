package com.example.mail_client.app;

import com.example.mail_client.controller.Client_Controller;
import com.example.mail_client.model.Client_Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Client_App extends Application
{
    private static final String PATH = "/com/example/mail_client/client_gui.fxml";
    private Client_Model model;

    /**
     * Starts the JavaFX application.
     *
     * @param stage the primary stage for this application
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start (Stage stage) throws IOException
    {
        model = new Client_Model ();
        FXMLLoader fxmlLoader = new FXMLLoader (Client_App.class.getResource (PATH));
        Scene scene = new Scene (fxmlLoader.load (), 900, 600);
        ((Client_Controller) fxmlLoader.getController ()).set_model (model);
        stage.setTitle ("Mail Client");
        stage.setScene (scene);
        stage.show ();
    }
}
