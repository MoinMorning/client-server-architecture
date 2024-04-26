
import java.io.*;
import java.net.*;
public class Client {
    public static void main(String[] args) {
        try {
            Socket clientSocket = new Socket("localhost", 6789);
            System.out.println("Connected to the server!");

            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter your username: ");
            String username = in.readLine();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(username);

            new Thread(() -> {
                try {
                    BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    while (true) {
                        String serverMessage = serverIn.readLine();
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Error when reading from server: " + e.getMessage());
                }
            }).start();
    
            while (true) {
                String message = in.readLine();
                out.println(message);
                if ("bye".equals(message)) {
                    clientSocket.close();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error when connecting to server or sending/receiving messages: " + e.getMessage());
        }
    }
}
