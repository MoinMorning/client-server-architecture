import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static List<ClientHandler> players = new ArrayList<>(); // List to store player ClientHandlers
    private static Map<String, ClientHandler> clientHandlers = new HashMap<>();
    private static List<String> deck = new ArrayList<>();
    private static Map<String, String> playerHands = new HashMap<>();
    private static Queue<String> playerTurnOrder = new LinkedList<>();
    private static Map<String, Integer> playerScores = new HashMap<>();
    private static Map<String, Boolean> playerProtectionStatus = new HashMap<>();
    public static boolean gameStarted;
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6789);
        System.out.println("Server is running and waiting for a client connection...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            players.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String username;
        private PrintWriter out;
        private int score;

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
                gameStarted = false;
                clientHandlers.put(this.username, this);
                out.println("Welcome, " + this.username);
                broadcastMessage(this.username + " joined the room");
                broadcastMessage("If you want to start the game, type /start!!!");

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if ("bye".equals(clientMessage)) {
                        handleClientDisconnection();
                        break;
                    } else if (clientMessage.startsWith("/pm")) {
                        handlePrivateMessage(clientMessage);
                    } else if (clientMessage.startsWith("/start")) {
                        if(!gameStarted){
                        startGame();
                        gameStarted = true;
                    } else { 
                        out.println("The game can't be started twice!!!!");
                    }
                    } else if (clientMessage.startsWith("/play")) {
                        playCard(clientMessage);
                    } else if (clientMessage.startsWith("/show")) {
                        showHands();
                    } else if (clientMessage.startsWith("/scores")) {
                        displayScores();
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
            String[] ranks = {"Guard", "Priest", "Baron", "King", "Prince", "Handmaid", "Countess", "Princess"};
            for (String rank : ranks) {
                deck.add(rank);
            }
            return deck;
        }
        private void displayScores() {
            StringBuilder scoreMessage = new StringBuilder("Current Scores:\n");
            for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
                scoreMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            out.println(scoreMessage.toString());
        }
        
    
        public static void shuffleDeck(List<String> deck) {
            Collections.shuffle(deck);
        }

        private static String getCurrentPlayer() {
            if (!playerTurnOrder.isEmpty()) {
                return playerTurnOrder.peek(); // Peek at the front of the queue
            } else {
                throw new IllegalStateException("No current player. The turn order queue is empty.");
            }
        }
        private void startGame() {
           initializeGame();
        }
        public static void initializeGame() {
            
            // Create and shuffle the deck
            deck = createDeck();
            shuffleDeck(deck);
    
            // Deal cards to players
            // Deal one card to each player
        for (ClientHandler player : players) {
            String card = deck.remove(0); // Draw the top card
            playerHands.put(player.getUsername(), card); // Assign the card to the player's hand
        }
            // Inform players that the game has started
            broadcastMessage("Welcome to LoveLetter! Good Luck");
    
            // Initialize player protection status (assuming all players start unprotected)
            for (String player : clientHandlers.keySet()) {
                playerProtectionStatus.put(player, false);
            }
    
            // Initialize player scores
            for (String player : clientHandlers.keySet()) {
                playerScores.put(player, 0);
            }
    
            // Determine the first player's turn (you can choose randomly or based on some criteria)
            playerTurnOrder.addAll(clientHandlers.keySet()); // Initialize turn order
            Collections.shuffle(new ArrayList<>(playerTurnOrder)); // Randomize the order
    
            // Begin the first turn
            String firstPlayer = getCurrentPlayer();
            broadcastMessage("First turn: " + firstPlayer);
        }        
       

        private void playCard(String clientMessage) {
            while (true) {                
            // Parse the clientMessage to extract the card name (e.g., "/play Guard")
            String[] parts = clientMessage.split(" ");
            if (parts.length < 2) {
                out.println("Invalid command. Use /play <card>");
                break;
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
            if (!playerHand.startsWith(cardName)) {
                out.println("You don't have that card in your hand!");
                return;
            }
        
            // Mark the card as played
            playerHands.put(this.username, playerHand.replaceFirst(cardName, cardName + ":played"));
        
            // Broadcast the played card to all clients
            broadcastMessage(this.username + " played " + cardName);
        
            // Display the updated player hand
            showHands();
        
        
            // Apply the card effect based on the card name
            switch (cardName) {
                case "Guard":
                    // Guess another player's card; if correct, they're out.
                    handleGuardGuess(clientMessage);
                    break;
                case "Priest":
                    // Look at another player's hand.
                    handlePriestInspect(clientMessage);
                    break;
                case "Baron":
                    // Compare hands with another player; lower hand is out.
                    handleBaronEffect(clientMessage);
                    break;
                case "Handmaid":
                    // Player is protected until their next turn.
                    handleHandmaidEffect(clientMessage);
                    break;
                case "Prince":
                    // Choose a player (including yourself) to discard their hand and draw a new card.
                    handlePrinceEffect(clientMessage);
                    break;
                case "King":
                    // Trade hands with another player.
                    handleKingEffect(clientMessage);
                    break;
                case "Countess":
                    // Must discard if caught with King or Prince.
                    handleCountessEffect(clientMessage);
                    break;
                case "Princess":
                    // Discard and lose the round.
                    handlePrincessEffect(clientMessage);
                    break;
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

            if (playerHands.get(this.username).isEmpty()) {
                // Update player score (eliminated players get 1 point)
                int currentScore = playerScores.get(this.username);
                playerScores.put(this.username, currentScore + 1);
                // Broadcast elimination message
                broadcastMessage(this.username + " has been eliminated!");
            }
            
            // Check if there's only one player left
            if (playerScores.size() == clientHandlers.size() - 1) {
                // Determine the winner
                String winner = "";
                for (String player : clientHandlers.keySet()) {
                    if (!playerHands.get(player).isEmpty()) {
                        winner = player;
                        break;
                    }
                }
                broadcastMessage("Round winner: " + winner);
                break;
            }
        }
    }
        private void discardHand(String playerName) {
        // Remove all cards from the player's hand
        playerHands.remove(playerName);
        // Inform the player and other players about the discard
        broadcastMessage(playerName + " has discarded their hand.");
     }
        private void drawCard(String playerName, String card) {
        // Add the drawn card to the player's hand
        playerHands.put(playerName, card);
        // Inform the player and other players about the drawn card
        broadcastMessage(playerName + " has drawn a new card: " + card);
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
                    out.println("User " + targetUsername + " not found. Please specify a valid target.");
                    return;
                }
            
                // Get the target player's hand
                String targetHand = playerHands.get(targetUsername);
            
                // Compare the guessed card with the actual card in the target player's hand
                if (targetHand.equals(guessedCard)) {
                    // Guessed correctly; eliminate the target player
                    out.println("Correct guess! " + targetUsername + " is eliminated.");
                    // Remove the target player from the game (update turn order, etc.)
                    // TODO: Implement game logic for elimination
                } else {
                    // Incorrect guess; inform the current player
                    out.println("Incorrect guess! " + targetUsername + " still in the game.");
                }
            }
        
        
            private void handleKingEffect(String targetPlayer) {
                // Validate if the target player exists
                if (!playerHands.containsKey(targetPlayer)) {
                    out.println("Invalid target player.");
                    return;
                }
            
                // Swap the cards in the hands of the current player and the target player
                String currentPlayerHand = playerHands.get(this.username);
                String targetPlayerHand = playerHands.get(targetPlayer);
                playerHands.put(this.username, targetPlayerHand);
                playerHands.put(targetPlayer, currentPlayerHand);
            
                // Inform all players about the trade
                broadcastMessage(this.username + " traded hands with " + targetPlayer);
            
                // Proceed to the next turn
                playerTurnOrder.add(playerTurnOrder.poll());
                String nextPlayer = getCurrentPlayer();
                broadcastMessage("Next turn: " + nextPlayer);
            }
            
        
            public void playPrinceEffect(String clientMessage) {
                // Parse the clientMessage to extract the target player's username
                String[] parts = clientMessage.split(" ");
                if (parts.length < 3) {
                    sendMessage("Invalid command. Use /play Prince <targetUsername>");
                    return;
                }
                String targetUsername = parts[2];
            
                // Validate if the target player exists
                ClientHandler targetPlayer = clientHandlers.get(targetUsername);
                if (targetPlayer == null) {
                    sendMessage("User " + targetUsername + " not found");
                    return;
                }
            
                // Get the target player's hand
                String targetPlayerHand = playerHands.get(targetUsername);
            
                // Check if the target player's hand is empty
                if (targetPlayerHand.isEmpty()) {
                    sendMessage(targetUsername + "'s hand is already empty.");
                    return;
                }
            
                // Discard the target player's hand
                discardHand(targetUsername);
            
                // Draw a new card for the target player
              //  drawCard(targetUsername);
            
                // Inform all players about the action
                broadcastMessage(this.username + " played the Prince card on " + targetUsername + ", discarding their hand and drawing a new card.");
            }
            
        
        
        
        private void handlePriestInspect(String clientMessage) {
            // Parse the clientMessage to extract the target player's username
            String[] parts = clientMessage.split(" ");
            if (parts.length < 2) {
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
                out.println("User " + targetUsername + " not found. Please specify a valid target.");
                return;
            }
        
            // Compare hands and determine the winner
            String currentPlayerHand = playerHands.get(this.username);
            String targetPlayerHand = playerHands.get(targetUsername);
        
            if (currentPlayerHand.equals(targetPlayerHand)) {
                out.println("It's a tie! Both players have the same hand.");
            } else if (currentPlayerHand.compareTo(targetPlayerHand) > 0) {
                out.println("You win! " + targetUsername + " is eliminated.");
                discardHand(targetUsername);
            } else {
                out.println(targetUsername + " wins! You are eliminated.");
                discardHand(this.username);
            }
        }
        
        private void handleHandmaidEffect(String clientMessage) {
            // Player is protected until their next turn.
            // Add code here to mark the current player as protected.
            // You can use a map to store the protection status of each player.
            // When a player plays the Handmaid card, set their protection status to true.
            // When their next turn starts, set their protection status back to false.
        
            // Mark the current player as protected until their next turn
            playerProtectionStatus.put(this.username, true);
            out.println("You are protected until your next turn.");
            playerTurnOrder.add(playerTurnOrder.poll());
            String nextPlayer = getCurrentPlayer();
            broadcastMessage("Next turn: " + nextPlayer);

            // Inform the current player about being protected
            sendMessage("You are protected until your next turn.");
        }
        
        
        private void handlePrinceEffect(String clientMessage) {
            // Choose a player (including yourself) to discard their hand and draw a new card.
            // Parse the clientMessage to extract the target player's username
            String[] parts = clientMessage.split(" ");
            if (parts.length < 3) {
                out.println("Invalid command. Use /play Prince <targetUsername>");
                return;
            }
            String targetUsername = parts[2];
        
            // Validate if the target player exists
            ClientHandler targetPlayer = clientHandlers.get(targetUsername);
            if (targetPlayer == null) {
                out.println("User " + targetUsername + " not found");
                return;
            }
        
            // Discard the target player's hand (remove all their cards)
            discardHand(targetUsername);

        }
        
        private void handleCountessEffect(String clientMessage) {
            // Must discard if caught with King or Prince.
            // In Love Letter, if a player has a Countess card along with a King or Prince, they must discard the Countess.
            // However, since there's no direct interaction between cards in this implementation, you may not need to do anything here.
        }
        
        private void handlePrincessEffect(String clientMessage) {
            // Discard and lose the round.
            // In Love Letter, if a player plays the Princess card, they are immediately eliminated from the round.
            // You should remove the player from the game (if needed) and notify all players about their elimination.
            // You can also update their score accordingly.
            out.println("You played the Princess card and are eliminated from the round!");
            handleClientDisconnection(); // Handle the elimination of the current player
            broadcastMessage(this.username + " has discarded the Princess card and is eliminated!");
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
                case "Handmaid":
                    return 4;
                case "Prince":
                    return 5;
                case "King":
                    return 6;
                case "Countess":
                    return 7;
                case "Princess":
                    return 8;
                // Add other card ranks as needed
                default:
                    return 0; // Unknown card (or use a different default value)
            }
        }
        private void skipTurn() {
            // Skip the turn of the current player and proceed to the next player
            String currentPlayer = playerTurnOrder.poll();
            playerTurnOrder.add(currentPlayer);
            broadcastMessage("Turn skipped for player: " + currentPlayer);
            broadcastMessage("Next turn: " + playerTurnOrder.peek());
        }

        private void showHands() {
            StringBuilder handMessage = new StringBuilder("Your current hand:\n");
            String currentPlayerHand = playerHands.get(this.username);
            String[] cards = currentPlayerHand.split(":"); // Assuming cards are separated by ":"
            int numCards = cards.length;
        
            for (int i = 0; i < numCards; i++) {
                if (i < numCards) {
                    handMessage.append("- ").append(cards[i]).append("\n");
                } else {
                    handMessage.append("- [Empty]\n");
                }
            }
        
            out.println(handMessage.toString());
        }
        
        
        public static void broadcastMessage(String message) {
            for (ClientHandler clientHandler : clientHandlers.values()) {
                clientHandler.sendMessage(message);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
        
        public String getUsername() {
            return username;
        }
    }
}
