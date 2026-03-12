package com.example.mail_client.model;

import com.example.shared.data.Email;
import com.example.shared.data.Package;
import com.example.shared.data.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.shared.data.TYPE.*;

public class Client_Model
{
    private final Network_Manager network_manager;
    private boolean logged_in;

    /**
     * Constructor for Client_Model.
     */
    public Client_Model ()
    {
        network_manager = new Network_Manager ("127.0.0.1", 8181);
        logged_in = false;
    }

    /**
     * Attempts to log in with the given user credentials.
     *
     * @param user the user credentials
     * @return "SUCCESS", "OFFLINE", or "REJECTED" based on the response
     */
    public String login (User user)
    {
        Package response = network_manager.send_package (new Package (LOGIN, user, null, null, null));
        if (response == null) return "OFFLINE";
        logged_in = "SUCCESS".equals (response.message ());
        return logged_in ? "SUCCESS" : "REJECTED";
    }

    /**
     * Fetches the inbox for the specified user.
     *
     * @param user the user whose inbox is to be fetched
     * @return a list of emails in the user's inbox
     * @throws IllegalStateException if the user is not logged in
     */
    public List <Email> get_inbox (User user)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot fetch inbox: User is not logged in.");

        Package response = network_manager.send_package (new Package (REQUEST_INBOX, user, null, null, null));

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }

        return response.email_list ();
    }

    /**
     * Sends an email with the specified details.
     *
     * @param sender    the sender of the email
     * @param receivers the list of recipients
     * @param subject   the subject of the email
     * @param text      the content of the email
     * @throws IllegalStateException if the user is not logged in
     */
    public void send_email (User sender, List <User> receivers, String subject, String text)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot send email: User is not logged in.");

        Email email = new Email (UUID.randomUUID ().toString (), sender, receivers, subject, text,
                                 LocalDateTime.now ());
        Package response = network_manager.send_package (new Package (SEND_EMAIL, sender, email, null, null));

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }
    }
}
