package com.example.mail_client.controller;

import com.example.mail_client.model.Client_Model;
import com.example.shared.data.Email;
import com.example.shared.data.TYPE;
import com.example.shared.data.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.shared.data.TYPE.*;

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
    private Email current_selected_email;
    private TYPE compose_mode = SEND_EMAIL;

    private volatile boolean keep_polling = false;
    private Thread polling_thread;

    /**
     * Sets the data model for the controller.
     *
     * @param model the data model to be used
     */
    public void set_model (Client_Model model)
    {
        this.model = model;
    }

    private void start_polling ()
    {
        keep_polling = true;

        polling_thread = new Thread (() -> {
            while (keep_polling)
            {
                try
                {
                    Thread.sleep (5000);

                    if (current_user != null)
                    {

                        // 1. Find the ID of the latest email we have
                        String last_id = null;
                        int current_size = email_list_view.getItems ().size ();
                        if (current_size > 0)
                        {
                            // Assuming the newest emails are at the bottom of the list
                            last_id = email_list_view.getItems ().get (current_size - 1).id ();
                        }

                        // 2. Ask the server for ONLY the new emails
                        List <Email> new_emails = model.get_inbox (current_user, last_id);

                        javafx.application.Platform.runLater (() -> {
                            // 3. If the server actually sent us new stuff...
                            if (new_emails != null && ! new_emails.isEmpty ())
                            {

                                // Check if the server reset and sent us the whole list
                                if (current_size > 0 && new_emails.get (0).id ()
                                        .equals (email_list_view.getItems ().get (0).id ()))
                                {
                                    email_list_view.getItems ().setAll (new_emails);
                                }
                                else
                                {
                                    // Otherwise, just append the new emails!
                                    email_list_view.getItems ().addAll (new_emails);

                                    // Trigger notifications
                                    java.awt.Toolkit.getDefaultToolkit ().beep ();
                                    button_refresh.setText ("✨ New Mail!");
                                    button_refresh.setStyle (
                                            "-fx-background-color: #48bb78; -fx-text-fill: white; -fx-border-color: #48bb78;");

                                    new Thread (() -> {
                                        try
                                        {
                                            Thread.sleep (3000);
                                        }
                                        catch (InterruptedException ignored)
                                        {
                                        }
                                        javafx.application.Platform.runLater (() -> {
                                            button_refresh.setText ("Refresh");
                                            button_refresh.setStyle ("");
                                        });
                                    }).start ();
                                }
                            }
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
                catch (IllegalStateException e)
                {
                    keep_polling = false;
                    if ("SERVER_OFFLINE".equals (e.getMessage ()))
                    {
                        javafx.application.Platform.runLater (this :: handle_server_disconnect);
                    }
                }
            }
        });

        polling_thread.setDaemon (true);
        polling_thread.start ();
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
                        this.current_user = attempt_user;

                        // --- START ANIMATION ---

                        // 1. Fade OUT the login screen (duration: 500ms)
                        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition (
                                javafx.util.Duration.millis (500), login_pane);
                        fadeOut.setFromValue (1.0);
                        fadeOut.setToValue (0.0);

                        // 2. When fade out finishes, swap panes and fade IN the main app
                        fadeOut.setOnFinished (event -> {
                            login_pane.setVisible (false);

                            // Prep main app to be invisible before showing it
                            main_app_pane.setOpacity (0.0);
                            main_app_pane.setVisible (true);

                            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition (
                                    javafx.util.Duration.millis (500), main_app_pane);
                            fadeIn.setFromValue (0.0);
                            fadeIn.setToValue (1.0);
                            fadeIn.play ();

                            // Fetch the emails while the fade-in is happening!
                            on_refresh_clicked ();
                            start_polling ();
                        });

                        // 3. Play the fade out!
                        fadeOut.play ();
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
            // Pass 'null' to force a full refresh of the inbox
            email_list_view.getItems ().setAll (model.get_inbox (current_user, null));
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ()))
            {
                handle_server_disconnect ();
            }
            else
            {
                show_alert (Alert.AlertType.ERROR, "Error", e.getMessage ());
            }
        }
    }

    /**
     * Displays the content of the selected email in the read pane.
     *
     * @param email the email to display
     */
    private void display_email (Email email)
    {
        this.current_selected_email = email;
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
        compose_mode = SEND_EMAIL;
        text_compose_to.clear ();
        text_compose_to.setEditable (true);
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
            model.process_outgoing_email (compose_mode, current_user, receivers, subject, body, current_selected_email);

            show_alert (Alert.AlertType.INFORMATION, "Success", "Email sent successfully!");
            on_cancel_compose_clicked ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ()))
            {
                handle_server_disconnect ();
            }
            else
            {
                show_alert (Alert.AlertType.ERROR, "Failed to Send", e.getMessage ());
            }
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
        // Stop the background thread
        keep_polling = false;
        if (polling_thread != null) polling_thread.interrupt ();

        show_alert (Alert.AlertType.ERROR, "Connection Lost", "The server went offline. You have been disconnected.");

        // Clear user data
        current_user = null;
        email_list_view.getItems ().clear ();

        // Swap UI back to login screen
        main_app_pane.setVisible (false);
        login_pane.setVisible (true);

        // --- THE FIX: Reset the opacity that the animation changed! ---
        login_pane.setOpacity (1.0);
        main_app_pane.setOpacity (1.0);

        // Reset login screen state
        text_password.clear ();
        progress_login.setVisible (false);
        button_login.setDisable (false);
        label_login_error.setText ("");
    }

    @FXML
    public void on_reply_clicked ()
    {
        if (current_selected_email == null) return;
        compose_mode = ANSWER;

        text_compose_to.setText (current_selected_email.sender ().username ());
        text_compose_to.setEditable (false);
        text_compose_subject.setText ("Re: " + current_selected_email.subject ());

        text_compose_body.clear ();
        text_compose_body.setPromptText ("Write your reply here...");

        read_mail_pane.setVisible (false);
        compose_mail_pane.setVisible (true);
    }

    @FXML
    public void on_forward_clicked ()
    {
        if (current_selected_email == null) return;
        compose_mode = FORWARD;

        text_compose_to.clear ();
        text_compose_to.setEditable (true);
        text_compose_subject.setText ("Fwd: " + current_selected_email.subject ());

        text_compose_body.clear ();
        text_compose_body.setPromptText ("Add a message to your forwarded email...");

        read_mail_pane.setVisible (false);
        compose_mail_pane.setVisible (true);
    }

    @FXML
    public void on_delete_clicked ()
    {
        if (current_selected_email == null || current_user == null) return;

        try
        {
            model.delete_email (current_user, current_selected_email);

            // Clear the screen and refresh the list
            current_selected_email = null;
            label_read_subject.setText ("Select an email to read");
            label_read_sender.setText ("From: ");
            label_read_date.setText ("Date: ");
            text_read_body.clear ();

            on_refresh_clicked ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ()))
            {
                handle_server_disconnect ();
            }
            else
            {
                show_alert (Alert.AlertType.ERROR, "Failed to Delete", e.getMessage ());
            }
        }
    }
}