package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    List<ClientThread> clients = new ArrayList<>();

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void listen() throws IOException {
        System.out.println("Server started on port " + serverSocket.getLocalPort());
        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientThread clientThread = new ClientThread(clientSocket, this);
            clients.add(clientThread);
            clientThread.start();
        }
    }

    public void broadcast(Message message) throws JsonProcessingException {
        for (ClientThread client : clients) {
            client.send(message);
        }
    }

    public void removeClient(ClientThread clientThread) {
        clients.remove(clientThread);
    }

    public List<ClientThread> getClients() {
        return clients;
    }

    public ClientThread getClientByName(String name) {
        for (ClientThread client : clients) {
            if (client.getClientName().equals(name)) {
                return client;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            int port = 12345;
            Server server = new Server(port);
            server.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
