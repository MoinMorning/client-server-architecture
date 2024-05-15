package src;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static List<ClientHandler> clients = new ArrayList<>(); // List to store connected clients

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6789);
        System.out.println("Server is running and waiting for a client connection...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if ("bye".equals(clientMessage)) {
                        handleClientDisconnection();
                        break;
                    } else {
                        broadcastMessage(clientMessage);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Client disconnected");
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    System.out.println("Error when closing the socket for client: " + ex.getMessage());
                }
            }
        }

        private void handleClientDisconnection() {
            // Remove the disconnected client from the list
            clients.remove(this);
            // Close the client socket
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Error when closing the socket for client: " + ex.getMessage());
            }
        }

        public void broadcastMessage(String message) {
            for (ClientHandler clientHandler : clients) {
                clientHandler.sendMessage(message);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
