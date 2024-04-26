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
        ClientHandler.initializeDeck();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            new Thread(clientHandler).start();
            ClientHandler.startGame();
        }
    }

    

   static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private static String username;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        public static String getClientname(){
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
    
    private static void startGame() {
        // Distribute cards to players (draw 1 card for each player)
        for (String player : clientHandlers.keySet()) {
            String card = deck.remove(0); // Draw the top card
            playerHands.put(player, card);
        }

        // Inform players that the game has started
     //   broadcastMessage("Game has started! Good luck!");

        // Determine the first player's turn (you can choose randomly or based on some criteria)
        playerTurnOrder.addAll(clientHandlers.keySet()); // Initialize turn order
        Collections.shuffle(new ArrayList<>(playerTurnOrder)); // Randomize the order

        // Begin the first turn
        String firstPlayer = getCurrentPlayer();
    //    broadcastMessage("First turn: " + firstPlayer);
    }

    private static void playCard(String cardName) {
        String currentPlayer = getCurrentPlayer();

        // Validate if it's the player's turn
        if (!currentPlayer.equals(username)) {
     //       out.println("It's not your turn!");
            return;
        }

        // Validate if the player has the specified card in their hand
        String playerHand = playerHands.get(getClientname());
        if (!playerHand.equals(cardName)) {
       //     out.println("You don't have that card in your hand!");
            return;
        }

        // Apply the card effect (example: Guard)
        switch (cardName) {
            case "Guard":
                // Example: Guess another player's card; if correct, they're out.
                // Implement the guessing logic here
                // ...
                break;
            case "Priest":
                // Example: Look at another player's hand.
                // Implement the hand inspection logic here
                // ...
                break;
            // Add other card effects (Baron, Handmaid, etc.) as needed
            default:
           //     out.println("Invalid card name.");
                return;
        }

        // Update playerHands map (remove the played card from the player's hand)

        // Inform all players about the played card and its effect
        System.out.println("Player " + username + " played " + cardName);

        // Proceed to the next turn
        // Update the playerTurnOrder queue (rotate the order)
        playerTurnOrder.add(playerTurnOrder.poll());
        String nextPlayer = getCurrentPlayer();
        System.out.println("Next turn: " + nextPlayer);
    }

    private static void initializeDeck() {
        // Add card names to the deck
        // Example: deck.add("Guard");
        deck.add("Guard");
        deck.add("Priest");
        deck.add("Baron");
        deck.add("Prince");

        // Shuffle the deck
        Collections.shuffle(deck);
    }

    private static String getCurrentPlayer() {
        // Ensure the playerTurnOrder queue is not empty
        if (playerTurnOrder.isEmpty()) {
            // Handle this case (e.g., start a new round or end the game)
            // ...
        }

        // Get the current player from the front of the queue
        return playerTurnOrder.peek();
    }
}
}