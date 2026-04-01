package com.example.mail_client.app;

import com.example.mail_client.controller.Client_Controller;
import com.example.mail_client.model.Client_Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Client_App extends Application
{
    private static final String PATH = "/com/example/mail_client/client_gui.fxml";

    @Override
    public void start (Stage stage) throws IOException
    {
        // Client Model
        Client_Model model = new Client_Model ();

        // GUI and Controller
        FXMLLoader fxmlLoader = new FXMLLoader (Client_App.class.getResource (PATH));
        Scene scene = new Scene (fxmlLoader.load (), 900, 600);
        scene.getStylesheets ().add (
                Objects.requireNonNull (getClass ().getResource ("/com/example/mail_client/styles.css"))
                        .toExternalForm ());
        ((Client_Controller) fxmlLoader.getController ()).set_model (model);

        // Display GUI
        stage.setTitle ("Mail Client");
        stage.setScene (scene);
        stage.show ();
    }
}
