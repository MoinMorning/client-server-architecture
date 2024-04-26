import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, ClientHandler> clientHandlers = new HashMap<>();
    private static List<String> deck = new ArrayList<>();
    private static Map<String, String> playerHands = new HashMap<>();
    private static Queue<String> playerTurnOrder = new LinkedList<>();
  
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6789);
        System.out.println("Server is running and waiting for a client connection...");
        initializeDeck();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
           // System.out.println("Message from client " + clientHandler.username);
            new Thread(clientHandler).start();

        }
       
    }

     static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private  String username;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        public String getClientname(){
            return username;
        }

        @Override
public void run() {
    try {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);

        while (true) {
            this.username = in.readLine();
            if (clientHandlers.containsKey(this.username)) {
                out.println("Username is already taken. Please choose another one.");
            } else {
                System.out.println("Client " + this.username + " has connected!!!"); 
                 // Print the client's username on the server's console
                break;
            }
        }

        clientHandlers.put(this.username, this);
        out.println("Welcome " + this.username);
        broadcastMessage(this.username + " joined the room");

        String clientMessage;
        while ((clientMessage = in.readLine()) != null) {
            if ("bye".equals(clientMessage)) {

                //TODO: Client has disconnected in Server (show in Server) 
                
                break;
            }
            if (clientMessage.startsWith("/pm")) {
                String[] parts = clientMessage.split(" ", 3);
                if (parts.length < 3) {
                    out.println("Invalid private message format. Use /pm <username> <message>");
                } else {
                    String recipientUsername = parts[1];
                    String message = parts[2];
                    ClientHandler recipient = clientHandlers.get(recipientUsername);
                    if (recipient != null) {
                        recipient.sendMessage(this.username + " (private): " + message);
                    } else {
                        out.println("User " + recipientUsername + " not found");
                    }
                }
            }  else if (clientMessage.startsWith("/start")) {
            //TODO: Game implementieren.

            // Start a new game
        } else if (clientMessage.startsWith("/play")) {
            // Play a card
        } else {
                broadcastMessage(this.username + ": " + clientMessage);
            }       
         }

    } catch (IOException ex) {
        System.out.println("Client " + this.username + " disconnected");
    } finally {
        clientHandlers.remove(this.username);
        broadcastMessage(this.username + " left the room");
        try {
            clientSocket.close();
        } catch (IOException ex) {
            System.out.println("Error when closing the socket for client!!: " + ex.getMessage());
        }
    }
}

        private void broadcastMessage(String message) {
            for (ClientHandler clientHandler : clientHandlers.values()) {
                clientHandler.sendMessage(message);
            }
        }

        private void sendMessage(String message) {
            this.out.println(message);
        }
        private void startGame() {
            // Distribute cards to players (draw 1 card for each player)
            // Inform players that the game has started
            // Start the first turn (choose a random player to begin)
            // ...
        }
        private void playCard(String cardName) {
            // Validate if it's the player's turn
            // Validate if the player has the specified card in their hand
            // Apply the card effect (e.g., compare hands, force discard, etc.)
            // Update playerHands map accordingly
            // Inform players about the played card and its effect
            // Proceed to the next turn
            // ...
        }
        private static void initializeDeck() {
            // Add card names to the deck
            // Example: deck.add("Guard");
            // ...
            deck.add("Guard");
            deck.add("Priest");
            deck.add("Baron");
            deck.add("Prince");

            // Shuffle the deck
            Collections.shuffle(deck);
        }

    }
}