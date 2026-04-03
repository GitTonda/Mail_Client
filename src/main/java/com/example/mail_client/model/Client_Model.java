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

    public Client_Model ()
    {
        network_manager = new Network_Manager ("127.0.0.1", 8181);
        logged_in = false;
    }

    public String login (User user)
    {
        Package response = network_manager.send_package (new Package (LOGIN, user, null, null, null));
        if (response == null) return "OFFLINE";
        logged_in = "SUCCESS".equals (response.message ());
        return logged_in ? "SUCCESS" : "REJECTED";
    }

    public List <Email> get_inbox (User user, String last_id)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot fetch inbox: User is not logged in.");
        Package response = network_manager.send_package (new Package (REQUEST_INBOX, user, null, null, last_id));

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }

        return response.email_list ();
    }

    public void process_outgoing_email (TYPE action_type, User sender, List <User> receivers, String subject,
                                        String text, Email original_email)
    {
        if (! logged_in) throw new IllegalStateException ("Cannot send email: User is not logged in.");
        Email new_email = new Email (UUID.randomUUID ().toString (), sender, receivers, subject, text,
                                     LocalDateTime.now ());

        List <Email> context_list = null;
        if ((action_type == ANSWER || action_type == FORWARD) && original_email != null)
            context_list = List.of (original_email);

        Package pkg = new Package (action_type, sender, new_email, context_list, null);
        Package response = network_manager.send_package (pkg);

        if (response == null)
        {
            logged_in = false;
            throw new IllegalStateException ("SERVER_OFFLINE");
        }
    }

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

    public String register (User user)
    {
        Package request = new Package (TYPE.REGISTER, user, null, null, null);
        try
        {
            Package response = network_manager.send_package (request);
            if (response != null) return response.message ();
            return "OFFLINE";
        }
        catch (Exception e)
        {
            return "OFFLINE";
        }
    }

    public Network_Manager get_network_manager ()
    {
        return network_manager;
    }
}
