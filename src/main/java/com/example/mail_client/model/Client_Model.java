package com.example.mail_client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client_Model
{
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Localhost
    private static final int PORT = 8181;

    public Client_Model ()
    {
        System.out.println("Attempting to connect to server...");

        try (
                Socket socket = new Socket (SERVER_ADDRESS, PORT);
                PrintWriter out = new PrintWriter (socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader (socket.getInputStream()))
        ) {
            Scanner input = new Scanner (System.in);
            while (true)
            {
                String messageToSend = input.nextLine();
                System.out.println("Sending: " + messageToSend);

                out.println(messageToSend);
                String serverResponse = in.readLine();
                System.out.println("Received: " + serverResponse);
            }
        } catch (IOException e) {
            System.err.println("Could not connect to the server: " + e.getMessage());
        }

        System.out.println("Connection closed.");
    }
}
