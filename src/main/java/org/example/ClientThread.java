package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

public class ClientThread extends Thread {
    Socket client;
    Server server;
    PrintWriter writer;
    String clientName;

    public ClientThread(Socket client, Server server) {
        this.client = client;
        this.server = server;
    }

    public String getClientName() {
        return clientName;
    }

    public void run() {
        try {
            InputStream input = client.getInputStream();
            OutputStream output = client.getOutputStream();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input)
            );
            writer = new PrintWriter(output, true);

            String rawMessage;

            while ((rawMessage = reader.readLine()) != null) {
                Message message = new ObjectMapper()
                        .readValue(rawMessage, Message.class);

                switch (message.type) {
                    case Broadcast -> {
                        if (message.content.equals("/online")) {
                            sendOnlineUsers();
                        } else {
                            message.content = clientName + ": " + message.content;
                            server.broadcast(message);
                        }
                    }
                    case Login -> {
                        login(message.content);
                        server.broadcast(new Message(MessageType.Broadcast, clientName + " has joined the chat"));
                    }
                    case Private -> handlePrivateMessage(message);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                server.broadcast(new Message(MessageType.Broadcast, clientName + " has left the chat"));
                server.removeClient(this);
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(Message message) throws JsonProcessingException {
        String rawMessage = new ObjectMapper().writeValueAsString(message);
        writer.println(rawMessage);
    }

    public void login(String name) throws JsonProcessingException {
        clientName = name;
        Message message = new Message(MessageType.Broadcast, "Welcome, " + name);
        send(message);
    }

    private void sendOnlineUsers() throws JsonProcessingException {
        StringBuilder usersList = new StringBuilder("Online users: ");
        for (ClientThread client : server.getClients()) {
            usersList.append(client.getClientName()).append(", ");
        }
        if (usersList.length() > 14) {
            usersList.setLength(usersList.length() - 2);
        }
        send(new Message(MessageType.Broadcast, usersList.toString()));
    }

    private void handlePrivateMessage(Message message) throws JsonProcessingException {
        ClientThread recipient = server.getClientByName(message.recipient);
        if (recipient != null) {
            recipient.send(new Message(MessageType.Private, clientName + " (private): " + message.content));
        } else {
            send(new Message(MessageType.Broadcast, "User " + message.recipient + " is not online."));
        }
    }
}
