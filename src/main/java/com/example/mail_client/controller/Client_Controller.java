package com.example.mail_client.controller;

import com.example.mail_client.model.Client_Model;
import com.example.shared.data.Email;
import com.example.shared.data.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Client_Controller
{
    // --- LAYOUT PANES ---
    @FXML
    private SplitPane main_app_pane;
    @FXML
    private VBox login_pane;

    // --- LOGIN SCREEN ---
    @FXML
    private TextField text_username;
    @FXML
    private PasswordField text_password;
    @FXML
    private Label label_login_error;
    @FXML
    private Button button_login;
    @FXML
    private ProgressIndicator progress_login;

    // --- LEFT SIDE ---
    @FXML
    private Button button_refresh;
    @FXML
    private ListView <Email> email_list_view;

    // --- RIGHT SIDE (TOOLBAR) ---
    @FXML
    private Button button_write_new;

    // --- RIGHT SIDE (READ PANE) ---
    @FXML
    private VBox read_mail_pane;
    @FXML
    private Label label_read_subject;
    @FXML
    private Label label_read_sender;
    @FXML
    private Label label_read_date;
    @FXML
    private TextArea text_read_body;

    // --- RIGHT SIDE (COMPOSE PANE) ---
    @FXML
    private VBox compose_mail_pane;
    @FXML
    private TextField text_compose_to;
    @FXML
    private TextField text_compose_subject;
    @FXML
    private TextArea text_compose_body;

    private Client_Model model;
    private User current_user;

    /**
     * Sets the data model for the controller.
     *
     * @param model the data model to be used
     */
    public void set_model (Client_Model model)
    {
        this.model = model;
    }

    /**
     * Initializes the controller, sets up the list view cell factory and selection listener.
     */
    @FXML
    public void initialize ()
    {
        email_list_view.setCellFactory (_ -> new ListCell <> ()
        {
            @Override
            protected void updateItem (Email email, boolean empty)
            {
                super.updateItem (email, empty);
                setText (empty || email == null ? null : email.sender ().username () + " - " + email.subject ());
            }
        });

        email_list_view.getSelectionModel ().selectedItemProperty ()
                .addListener ((_, _, newValue) ->
                              {
                                  if (newValue != null) display_email (newValue);
                              });
    }

    /**
     * Handles the login button click, starts a background thread to attempt login.
     */
    @FXML
    public void on_login_clicked ()
    {
        String username = text_username.getText ().trim ();
        String password = text_password.getText ();

        if (username.isBlank () || password.isBlank ())
        {
            label_login_error.setText ("Please enter both email and password.");
            label_login_error.setTextFill (javafx.scene.paint.Color.RED);
            return;
        }

        // 1. START LOADING STATE
        label_login_error.setText ("Connecting to server... Please wait.");
        label_login_error.setTextFill (javafx.scene.paint.Color.BLUE);
        button_login.setDisable (true); // Stop user from clicking again
        progress_login.setVisible (true); // Show the spinning circle

        User attempt_user = new User (username, password);

        // 2. FIRE OFF BACKGROUND THREAD
        new Thread (() -> {
            boolean connected = false;

            while (! connected)
            {
                String status = model.login (attempt_user);

                if ("SUCCESS".equals (status))
                {
                    connected = true;

                    javafx.application.Platform.runLater (() -> {
                        // Reset UI elements
                        progress_login.setVisible (false);
                        button_login.setDisable (false);

                        // Enter the app
                        this.current_user = attempt_user;
                        login_pane.setVisible (false);
                        main_app_pane.setVisible (true);
                        on_refresh_clicked ();
                    });

                }
                else if ("REJECTED".equals (status))
                {
                    javafx.application.Platform.runLater (() -> {
                        // Reset UI and show error
                        progress_login.setVisible (false);
                        button_login.setDisable (false);
                        label_login_error.setText ("Login failed: Invalid user.");
                        label_login_error.setTextFill (javafx.scene.paint.Color.RED);
                    });
                    break;

                }
                else if ("OFFLINE".equals (status))
                {
                    // Server is offline. We keep the spinner spinning and loop again.
                    try
                    {
                        Thread.sleep (2000);
                    }
                    catch (InterruptedException e)
                    {
                        break;
                    }
                }
            }
        }).start ();
    }

    private void update_login_error (String msg, javafx.scene.paint.Color color)
    {
        label_login_error.setText (msg);
        label_login_error.setTextFill (color);
    }

    /**
     * Refreshes the inbox by fetching emails from the model.
     */
    @FXML
    public void on_refresh_clicked ()
    {
        if (current_user == null) return;

        try
        {
            email_list_view.getItems ().setAll (model.get_inbox (current_user));
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ())) handle_server_disconnect ();
            else show_alert (Alert.AlertType.ERROR, "Error", e.getMessage ());
        }
    }

    /**
     * Displays the content of the selected email in the read pane.
     *
     * @param email the email to display
     */
    private void display_email (Email email)
    {
        compose_mail_pane.setVisible (false);
        read_mail_pane.setVisible (true);

        label_read_subject.setText (email.subject ());
        label_read_sender.setText ("From: " + email.sender ().username ());
        label_read_date.setText ("Date: " + email.date ().format (DateTimeFormatter.ofPattern ("yyyy-MM-dd HH:mm")));
        text_read_body.setText (email.text ());
    }

    /**
     * Switches to the compose mail pane and clears existing input fields.
     */
    @FXML
    public void on_write_new_clicked ()
    {
        text_compose_to.clear ();
        text_compose_subject.clear ();
        text_compose_body.clear ();
        read_mail_pane.setVisible (false);
        compose_mail_pane.setVisible (true);
    }

    /**
     * Cancels the mail composition and returns to the read pane.
     */
    @FXML
    public void on_cancel_compose_clicked ()
    {
        compose_mail_pane.setVisible (false);
        read_mail_pane.setVisible (true);
    }

    /**
     * Sends the composed email using the data model.
     */
    @FXML
    public void on_send_clicked ()
    {
        String to_text = text_compose_to.getText ();
        String subject = text_compose_subject.getText ();
        String body = text_compose_body.getText ();

        if (to_text.isBlank () || subject.isBlank ())
        {
            show_alert (Alert.AlertType.WARNING, "Missing Info", "Please provide a recipient and a subject.");
            return;
        }

        List <User> receivers = Arrays.stream (to_text.split (","))
                .map (String :: trim)
                .map (e -> new User (e, ""))
                .collect (Collectors.toList ());

        try
        {
            model.send_email (current_user, receivers, subject, body);
            show_alert (Alert.AlertType.INFORMATION, "Success", "Email sent successfully!");
            on_cancel_compose_clicked ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ())) handle_server_disconnect ();
            else show_alert (Alert.AlertType.ERROR, "Failed to Send", e.getMessage ());
        }
    }

    /**
     * Displays a JavaFX alert dialog with the specified type, title, and message.
     *
     * @param type    the alert type
     * @param title   the title of the alert
     * @param message the content text of the alert
     */
    private void show_alert (Alert.AlertType type, String title, String message)
    {
        Alert alert = new Alert (type);
        alert.setTitle (title);
        alert.setHeaderText (null);
        alert.setContentText (message);
        alert.showAndWait ();
    }

    /**
     * Handles the UI updates when the server connection is unexpectedly lost.
     */
    private void handle_server_disconnect ()
    {
        show_alert (Alert.AlertType.ERROR, "Connection Lost", "The server went offline. You have been disconnected.");
        current_user = null;
        email_list_view.getItems ().clear ();

        main_app_pane.setVisible (false);
        login_pane.setVisible (true);

        text_password.clear ();
        progress_login.setVisible (false);
        button_login.setDisable (false);
        update_login_error ("", javafx.scene.paint.Color.BLACK);
    }
}