package com.example.mail_client.model;

import com.example.shared.data.Email;
import com.example.shared.data.Package;
import com.example.shared.data.TYPE;
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
    /**
     * Fetches the inbox for the specified user.
     * If last_id is provided, the server only returns emails newer than that ID.
     */
    public List <Email> get_inbox (User user, String last_id)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot fetch inbox: User is not logged in.");

        // Pass the last_id into the message slot of the Package
        Package response = network_manager.send_package (new Package (REQUEST_INBOX, user, null, null, last_id));

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

    /**
     * Sends a request to the server to delete a specific email.
     *
     * @param user  the user requesting the deletion
     * @param email the email to be deleted
     * @throws IllegalStateException if the user is not logged in or the server is offline
     */
    public void delete_email (User user, Email email)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot delete email: User is not logged in.");

        Package response = network_manager.send_package (new Package (DELETE_EMAIL, user, email, null, null));

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }
    }

    public void process_outgoing_email (TYPE action_type, User sender, List <User> receivers, String subject,
                                        String text, Email original_email)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot send email: User is not logged in.");

        Email new_email = new Email (UUID.randomUUID ().toString (), sender, receivers, subject, text,
                                     LocalDateTime.now ());

        // If it's a reply or forward, package the original email into the list slot
        List <Email> context_list = null;
        if ((action_type == ANSWER || action_type == FORWARD) && original_email != null)
        {
            context_list = List.of (original_email);
        }

        Package pkg = new Package (action_type, sender, new_email, context_list, null);
        Package response = network_manager.send_package (pkg);

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }
    }
}
