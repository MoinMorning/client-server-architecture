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

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            new Thread(clientHandler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String username;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Handle username selection
                while (true) {
                    this.username = in.readLine();
                    if (clientHandlers.containsKey(this.username)) {
                        out.println("Username is already taken. Please choose another one.");
                    } else {
                        System.out.println("Client " + this.username + " has connected!");
                        break;
                    }
                }

                clientHandlers.put(this.username, this);
                out.println("Welcome, " + this.username);
                broadcastMessage(this.username + " joined the room");

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if ("bye".equals(clientMessage)) {
                        handleClientDisconnection();
                        break;
                    } else if (clientMessage.startsWith("/pm")) {
                        handlePrivateMessage(clientMessage);
                    } else if (clientMessage.startsWith("/start")) {
                        startGame();
                    } else if (clientMessage.startsWith("/play")) {
                        playCard(clientMessage);
                    } else if(clientMessage.startsWith("/score")){
                        //TODO: Score anzeige
                    }else{
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
                    System.out.println("Error when closing the socket for client: " + ex.getMessage());
                }
            }
        }

        private void handleClientDisconnection() {
            // Remove the disconnected client from the game (if needed)
            clientHandlers.remove(this.username);
        
            // Notify other players about the disconnection
            broadcastMessage(this.username + " left the room");
        
            // Close the client socket
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Error when closing the socket for client: " + ex.getMessage());
            }
        }
        

        private void handlePrivateMessage(String clientMessage) {
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
            }
        }
        public static List<String> createDeck() {
            List<String> deck = new ArrayList<>();
            String[] ranks = {"Guard", "Priest", "Baron", /* TODO:.. add other cards ... */};
    
            for (String rank : ranks) {
                deck.add(rank);
            }
            return deck;
        }
    
        public static void shuffleDeck(List<String> deck) {
            Collections.shuffle(deck);
        }
        private String getCurrentPlayer() {
            if (!playerTurnOrder.isEmpty()) {
                return playerTurnOrder.peek(); // Peek at the front of the queue
            } else {
                //TODO: handle exception
                // Handle the case when the turn order queue is empty (e.g., game over)
                return null; // Or throw an exception, return a default player, etc.
            }
        }
        
        private void startGame() {
            // Initialize game logic (deck, hands, turn order)
            // ...
            List<String> deck = createDeck();
            shuffleDeck(deck);

        // Draw 1 card for each player and assign it to their hand
        for (String player : clientHandlers.keySet()) {
        String card = deck.remove(0); // Draw the top card
        playerHands.put(player, card);
        }

        // Inform players that the game has started
        broadcastMessage("Game has started! Good luck!");

        // Determine the first player's turn (you can choose randomly or based on some criteria)
        playerTurnOrder.addAll(clientHandlers.keySet()); // Initialize turn order
        Collections.shuffle(new ArrayList<>(playerTurnOrder)); // Randomize the order

        // Begin the first turn
        String firstPlayer = getCurrentPlayer();
        broadcastMessage("First turn: " + firstPlayer);
        }

        private void playCard(String clientMessage) {
            // Parse the clientMessage to extract the card name (e.g., "/play Guard")
            String[] parts = clientMessage.split(" ");
            if (parts.length < 2) {
                out.println("Invalid command. Use /play <card>");
                return;
            }
            String cardName = parts[1];
        
            // Validate if it's the player's turn
            String currentPlayer = getCurrentPlayer();
            if (!this.username.equals(currentPlayer)) {
                out.println("It's not your turn!");
                return;
            }
        
            // Validate if the player has the specified card in their hand
            String playerHand = playerHands.get(this.username);
            if (!playerHand.equals(cardName)) {
                out.println("You don't have that card in your hand!");
                return;
            }
        
            // Apply the card effect based on the card name
            switch (cardName) {
                case "Guard":
                    // Example: Guess another player's card; if correct, they're out.

                    handleGuardGuess(cardName);
                break;
                case "Priest":
                    // Example: Look at another player's hand.
                    handlePriestInspect(cardName);
                    break;
                case "Baron":
                    // Example: Look at another player's hand.
                handleBaronEffect(cardName);
                break;
                case "King":
                // Example: Look at another player's hand.
            handleKingEffect(cardName);
            break;
                // TODO: Add other card effects (Baron, Handmaid, etc.) as needed
                default:
                    out.println("Invalid card name.");
                    return;
            }
        
            // Update playerHands map (remove the played card from the player's hand)
            playerHands.remove(this.username);
        
            // Inform all players about the played card and its effect
            broadcastMessage(this.username + " played " + cardName);
        
            // Proceed to the next turn
            playerTurnOrder.add(playerTurnOrder.poll());
            String nextPlayer = getCurrentPlayer();
            broadcastMessage("Next turn: " + nextPlayer);
        }
        private void handleGuardGuess(String clientMessage) {
            // Parse the clientMessage to extract the target player's username and guessed card
            String[] parts = clientMessage.split(" ");
            if (parts.length < 4) {
                out.println("Invalid command. Use /play Guard <targetUsername> <guessedCard>");
                return;
            }
            String targetUsername = parts[2];
            String guessedCard = parts[3].toUpperCase(); // Convert to uppercase for consistency
        
            // Validate if the target player exists
            ClientHandler targetPlayer = clientHandlers.get(targetUsername);
            if (targetPlayer == null) {
                out.println("User " + targetUsername + " not found");
                return;
            }
        
            // Get the target player's hand
            String targetHand = playerHands.get(targetUsername);
        
            // Compare the guessed card with the actual card in the target player's hand
            if (targetHand.equals(guessedCard)) {
                // Guessed correctly; eliminate the target player
                out.println("Correct guess! " + targetUsername + " is eliminated.");
                // Remove the target player from the game (update turn order, etc.)
                // TODO:
            } else {
                // Incorrect guess; inform the current player
                out.println("Incorrect guess! " + targetUsername + " still in the game.");
            }
        }
        private void handleKingEffect(String clientMessage){
            //TODO: more card
        }
        
        
        private void handlePriestInspect(String clientMessage) {
            // Parse the clientMessage to extract the target player's username
            String[] parts = clientMessage.split(" ");
            if (parts.length < 3) {
                out.println("Invalid command. Use /play Priest <targetUsername>");
                return;
            }
            String targetUsername = parts[2];
        
            // Validate if the target player exists
            ClientHandler targetPlayer = clientHandlers.get(targetUsername);
            if (targetPlayer == null) {
                out.println("User " + targetUsername + " not found");
                return;
            }
        
            // Get the target player's hand (assuming you have a playerHands map)
            String targetHand = playerHands.get(targetUsername);
        
            // Send the hand information to the current player
            out.println(targetUsername + "'s hand: " + targetHand);
        }
        // TODO: Show hand
        // TODO: Score anzeigen
        
        
        private void handleBaronEffect(String clientMessage) {
            // Parse the clientMessage to extract the target player's username
            String[] parts = clientMessage.split(" ");
            if (parts.length < 3) {
                out.println("Invalid command. Use /play Baron <targetUsername>");
                return;
            }
            String targetUsername = parts[2];
        
            // Validate if the target player exists
            ClientHandler targetPlayer = clientHandlers.get(targetUsername);
            if (targetPlayer == null) {
                out.println("User " + targetUsername + " not found");
                return;
            }
        
            // Get the current player's hand and the target player's hand
            String currentPlayerHand = playerHands.get(this.username);
            String targetPlayerHand = playerHands.get(targetUsername);
        
            // Compare the ranks of the cards
            int currentPlayerRank = getCardRank(currentPlayerHand);
            int targetPlayerRank = getCardRank(targetPlayerHand);
        
            if (currentPlayerRank > targetPlayerRank) {
                // Current player wins; eliminate the target player
                out.println("You win! " + targetUsername + " is eliminated.");
                // Remove the target player from the game (update turn order, etc.)
                // ...
            } else if (currentPlayerRank < targetPlayerRank) {
                // Target player wins; current player is eliminated
                out.println(targetUsername + " wins! You are eliminated.");
                // Remove the current player from the game
                // ...
            } else {
                // It's a tie; no one is eliminated
                out.println("It's a tie! No one is eliminated.");
            }
        }
        
        private int getCardRank(String cardName) {
            // Assign numeric ranks to different cards (customize as needed)
            switch (cardName) {
                case "Guard":
                    return 1;
                case "Priest":
                    return 2;
                case "Baron":
                    return 3;
                // Add other card ranks
                default:
                    return 0; // Unknown card (or use a different default value)
            }
        }
        
        
        public static void broadcastMessage(String message) {
            for (ClientHandler clientHandler : clientHandlers.values()) {
                clientHandler.sendMessage(message);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
