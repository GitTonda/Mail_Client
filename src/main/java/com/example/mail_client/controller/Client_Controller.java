package com.example.mail_client.controller;

import com.example.mail_client.model.Client_Model;
import com.example.shared.data.Email;
import com.example.shared.data.TYPE;
import com.example.shared.data.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.shared.data.TYPE.*;

public class Client_Controller
{
    // =============== //
    // FXML INJECTIONS //
    // =============== //

    @FXML
    private SplitPane main_app_pane;
    @FXML
    private VBox login_pane;

    // --- Login Pane ---
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

    // --- Inbox Pane (Left) ---
    @FXML
    private Button button_refresh;
    @FXML
    private ListView <Email> email_list_view;

    // --- Header / Toolbar ---
    @FXML
    private Button button_write_new;
    @FXML
    private Label label_connection_status;

    // --- Read Pane (Right) ---
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
    @FXML
    private Button button_reply_all;

    // --- Compose Pane (Right) ---
    @FXML
    private VBox compose_mail_pane;
    @FXML
    private TextField text_compose_to;
    @FXML
    private TextField text_compose_subject;
    @FXML
    private TextArea text_compose_body;

    // =============== //
    // STATE VARIABLES //
    // =============== //

    private Client_Model model;
    private User current_user;
    private Email current_selected_email;
    private TYPE compose_mode = SEND_EMAIL;

    private volatile boolean keep_polling = false;
    private Thread polling_thread;

    // ====================== //
    // INITIALIZATION & SETUP //
    // ====================== //

    public void set_model (Client_Model model)
    {
        this.model = model;
    }

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

    // ============== //
    // AUTHENTICATION //
    // ============== //

    @FXML
    public void on_login_clicked ()
    {
        String username = text_username.getText ().trim ();
        String password = text_password.getText ();

        if (username.isBlank () || password.isBlank ())
        {
            update_login_error ("Please enter both email and password.", Color.RED);
            return;
        }
        if (UI_Utils.is_invalid_email (username))
        {
            update_login_error ("Invalid email format! Must be name@domain.com", Color.RED);
            return;
        }

        update_login_error ("Connecting to server... Please wait.", Color.BLUE);
        button_login.setDisable (true);
        progress_login.setVisible (true);

        User attempt_user = new User (username, password);

        new Thread (() -> {
            boolean connected = false;
            while (! connected)
            {
                String status = model.login (attempt_user);

                if ("SUCCESS".equals (status))
                {
                    connected = true;
                    Platform.runLater (() -> {
                        progress_login.setVisible (false);
                        button_login.setDisable (false);
                        this.current_user = attempt_user;
                        transition_to_main_app ();
                    });
                }
                else if ("REJECTED".equals (status))
                {
                    Platform.runLater (() -> {
                        progress_login.setVisible (false);
                        button_login.setDisable (false);
                        update_login_error ("Login failed: Invalid user.", Color.RED);
                    });
                    break;
                }
                else if ("OFFLINE".equals (status))
                {
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

    private void transition_to_main_app ()
    {
        FadeTransition fadeOut = new FadeTransition (Duration.millis (750), login_pane);
        fadeOut.setFromValue (1.0);
        fadeOut.setToValue (0.0);
        fadeOut.setOnFinished (_ ->
                               {
                                   login_pane.setVisible (false);
                                   main_app_pane.setOpacity (0.0);
                                   main_app_pane.setVisible (true);

                                   FadeTransition fadeIn = new FadeTransition (Duration.millis (750), main_app_pane);
                                   fadeIn.setFromValue (0.0);
                                   fadeIn.setToValue (1.0);
                                   fadeIn.play ();

                                   on_refresh_clicked ();
                                   start_polling ();
                               });
        fadeOut.play ();
    }

    private void update_login_error (String msg, Color color)
    {
        label_login_error.setText (msg);
        label_login_error.setTextFill (color);
    }

    // ================ //
    // BACKGROUND TASKS //
    // ================ //

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
                        String last_id = null;
                        int current_size = email_list_view.getItems ().size ();
                        if (current_size > 0) last_id = email_list_view.getItems ().get (current_size - 1).id ();

                        List <Email> new_emails = model.get_inbox (current_user, last_id);
                        Platform.runLater (() -> {
                            if (new_emails != null && ! new_emails.isEmpty ())
                            {
                                if (current_size > 0 && new_emails.getFirst ().id ()
                                        .equals (email_list_view.getItems ().getFirst ().id ()))
                                    email_list_view.getItems ().setAll (new_emails);
                                else
                                {
                                    email_list_view.getItems ().addAll (new_emails);
                                    Toolkit.getDefaultToolkit ().beep ();

                                    button_refresh.setText ("★ New Mail!");
                                    button_refresh.setStyle (
                                            "-fx-background-color: #48bb78; -fx-text-fill: white; -fx-border-color: #48bb78;");

                                    new Thread (() -> {
                                        try
                                        {
                                            Thread.sleep (3000);
                                        }
                                        catch (InterruptedException _)
                                        {
                                        }
                                        Platform.runLater (() -> {
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
                    if ("SERVER_OFFLINE".equals (e.getMessage ())) Platform.runLater (this :: handle_server_disconnect);
                }
            }
        });
        polling_thread.setDaemon (true);
        polling_thread.start ();
    }

    private void handle_server_disconnect ()
    {
        keep_polling = false;
        if (polling_thread != null) polling_thread.interrupt ();

        UI_Utils.show_alert (Alert.AlertType.ERROR, "Connection Lost",
                             "The server went offline. Trying to reconnect...");

        String saved_username = current_user != null ? current_user.username () : "";
        current_user = null;
        email_list_view.getItems ().clear ();

        main_app_pane.setVisible (false);
        login_pane.setVisible (true);
        login_pane.setOpacity (1.0);
        main_app_pane.setOpacity (1.0);

        button_login.setDisable (true);
        update_login_error ("Server offline. Reconnecting automatically...", Color.web ("#e53e3e"));

        Thread reconnect_thread = create_reconnect_thread (saved_username);
        reconnect_thread.start ();
    }

    private Thread create_reconnect_thread (String saved_username)
    {
        Thread reconnect_thread = new Thread (() -> {
            boolean reconnected = false;
            while (! reconnected)
            {
                try
                {
                    Thread.sleep (3000);
                    try (Socket _ = new Socket (model.get_network_manager ().get_IP (),
                                                model.get_network_manager ().get_PORT ()))
                    {
                        reconnected = true;
                    }
                }
                catch (Exception _)
                {
                }
            }

            Platform.runLater (() -> {
                update_login_error ("Connection restored! Logging in...", Color.web ("#38a169"));
                text_username.setText (saved_username);
                button_login.setDisable (false);
                if (! saved_username.isEmpty ()) on_login_clicked ();
            });
        });
        reconnect_thread.setDaemon (true);
        return reconnect_thread;
    }

    // ============= //
    // INBOX ACTIONS //
    // ============= //

    @FXML
    public void on_refresh_clicked ()
    {
        if (current_user == null) return;

        try
        {
            email_list_view.getItems ().setAll (model.get_inbox (current_user, null));
            set_connected_ui ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ())) handle_server_disconnect ();
            else UI_Utils.show_alert (Alert.AlertType.ERROR, "Error", e.getMessage ());
        }
    }

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

    @FXML
    public void on_delete_clicked ()
    {
        if (current_selected_email == null || current_user == null) return;

        try
        {
            model.delete_email (current_user, current_selected_email);

            current_selected_email = null;
            label_read_subject.setText ("Select an email to read");
            label_read_sender.setText ("From: ");
            label_read_date.setText ("Date: ");
            text_read_body.clear ();

            on_refresh_clicked ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ())) handle_server_disconnect ();
            else UI_Utils.show_alert (Alert.AlertType.ERROR, "Failed to Delete", e.getMessage ());
        }
    }

    // ======================= //
    // COMPOSE / REPLY ACTIONS //
    // ======================= //

    @FXML
    public void on_write_new_clicked ()
    {
        compose_mode = SEND_EMAIL;
        setup_compose_pane ("", true, "", "");
    }

    @FXML
    public void on_reply_clicked ()
    {
        if (current_selected_email == null) return;
        compose_mode = ANSWER;
        setup_compose_pane (current_selected_email.sender ().username (), false,
                            "Re: " + current_selected_email.subject (), "Write your reply here...");
    }

    @FXML
    public void on_reply_all_clicked ()
    {
        if (current_selected_email == null) return;
        compose_mode = ANSWER;

        String sender = current_selected_email.sender ().username ();
        String other_receivers = current_selected_email.receivers ().stream ()
                .map (User :: username)
                .filter (u -> ! u.equals (current_user.username ()))
                .collect (Collectors.joining (", "));

        String all_targets = sender + (other_receivers.isEmpty () ? "" : ", " + other_receivers);
        setup_compose_pane (all_targets, false, "Re: " + current_selected_email.subject (),
                            "Write your reply to everyone here...");
    }

    @FXML
    public void on_forward_clicked ()
    {
        if (current_selected_email == null) return;
        compose_mode = FORWARD;
        setup_compose_pane ("", true, "Fwd: " + current_selected_email.subject (),
                            "Add a message to your forwarded email...");
    }

    @FXML
    public void on_cancel_compose_clicked ()
    {
        compose_mail_pane.setVisible (false);
        read_mail_pane.setVisible (true);
    }

    @FXML
    public void on_send_clicked ()
    {
        String to_text = text_compose_to.getText ();
        String subject = text_compose_subject.getText ();
        String body = text_compose_body.getText ();

        if (to_text.isBlank () || subject.isBlank ())
        {
            UI_Utils.show_alert (Alert.AlertType.WARNING, "Missing Info", "Please provide a recipient and a subject.");
            return;
        }

        for (String address : to_text.split (","))
        {
            if (UI_Utils.is_invalid_email (address.trim ()))
            {
                UI_Utils.show_alert (Alert.AlertType.WARNING, "Invalid Address",
                                     "The address '" + address.trim () + "' is not formatted correctly.");
                return;
            }
        }

        List <User> receivers = Arrays.stream (to_text.split (","))
                .map (String :: trim)
                .distinct ()
                .map (e -> new User (e, ""))
                .toList ();

        try
        {
            model.process_outgoing_email (compose_mode, current_user, receivers, subject, body, current_selected_email);
            UI_Utils.show_alert (Alert.AlertType.INFORMATION, "Success", "Email sent successfully!");
            on_cancel_compose_clicked ();
        }
        catch (IllegalStateException e)
        {
            if ("SERVER_OFFLINE".equals (e.getMessage ())) handle_server_disconnect ();
            else UI_Utils.show_alert (Alert.AlertType.ERROR, "Failed to Send", e.getMessage ());
        }
    }

    private void setup_compose_pane (String to, boolean editable_to, String subject, String body_prompt)
    {
        text_compose_to.setText (to);
        text_compose_to.setEditable (editable_to);
        text_compose_subject.setText (subject);
        text_compose_body.clear ();
        text_compose_body.setPromptText (body_prompt);

        read_mail_pane.setVisible (false);
        compose_mail_pane.setVisible (true);
    }

    // ========== //
    // UI HELPERS //
    // ========== //

    private void set_connected_ui ()
    {
        label_connection_status.setText ("● ONLINE");
        label_connection_status.setStyle ("-fx-font-weight: bold; -fx-text-fill: #38a169;");
    }
}