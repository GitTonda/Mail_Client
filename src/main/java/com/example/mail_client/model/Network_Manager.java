package com.example.mail_client.model;

import com.example.shared.data.Package;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Network_Manager
{
    private final String SERVER_ADDRESS;
    private final int PORT;

    /**
     * Constructor for Network_Manager.
     *
     * @param server_address the server address
     * @param port           the server port
     */
    public Network_Manager (String server_address, int port)
    {
        SERVER_ADDRESS = server_address;
        PORT = port;
    }

    /**
     * Sends a package to the server and returns the response package.
     *
     * @param pkg the package to send
     * @return the response package from the server, or null if an error occurs
     */
    public Package send_package (Package pkg)
    {
        try (Socket socket = new Socket (SERVER_ADDRESS, PORT);
             ObjectOutputStream out = new ObjectOutputStream (socket.getOutputStream ());
             ObjectInputStream in = new ObjectInputStream (socket.getInputStream ()))
        {
            out.writeObject (pkg);
            out.flush ();
            Object response = in.readObject ();
            return response instanceof Package ? (Package) response : null;
        }
        catch (IOException | ClassNotFoundException e)
        {
            // Logging or error handling could go here
            return null;
        }
    }
}
