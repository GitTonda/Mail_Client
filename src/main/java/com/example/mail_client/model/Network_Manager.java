package com.example.mail_client.model;

import com.example.shared.data.Json_Mapper;
import com.example.shared.data.Package;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Network_Manager
{
    private final String SERVER_IP;
    private final int SERVER_PORT;

    public Network_Manager (String server_address, int port)
    {
        SERVER_IP = server_address;
        SERVER_PORT = port;
    }

    public Package send_package (Package pkg)
    {
        try (Socket socket = new Socket (SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter (socket.getOutputStream (), true);
             BufferedReader in = new BufferedReader (new InputStreamReader (socket.getInputStream ())))
        {
            String json_request = Json_Mapper.get ().writeValueAsString (pkg);
            out.println (json_request);

            String json_response = in.readLine ();
            if (json_response != null) return Json_Mapper.get ().readValue (json_response, Package.class);
        }
        catch (Exception e)
        {
            System.err.println ("Network error: " + e.getMessage ());
        }
        return null;
    }

    public String get_IP ()
    {
        return SERVER_IP;
    }

    public int get_PORT ()
    {
        return SERVER_PORT;
    }
}
