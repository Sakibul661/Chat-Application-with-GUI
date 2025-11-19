import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private final String serverIP;
    private final int serverPort;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private final Scanner scanner = new Scanner(System.in);

    public ChatClient(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void start() {
        try {
            initializeConnection();
            if (!performLogin()) return; // new login system integration
            String username = getUsername();
            sendUsername(username);
            startMessageListener();
            handleUserInput();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            ExceptionHandler.handle("Client start error", e);
        } finally {
            closeConnections();
            System.out.println("Disconnected from chat.");
        }
    }

    private boolean performLogin() {
        System.out.print("Do you have an account? (yes/no): ");
        String choice = scanner.nextLine().trim().toLowerCase();

        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (choice.equals("no")) {
            return LoginSystem.register(email, password);
        } else {
            return LoginSystem.login(email, password);
        }
    }

    private void initializeConnection() throws IOException {
        socket = new Socket(serverIP, serverPort);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
        System.out.println("Connected to server: " + serverIP + ":" + serverPort);
    }

    private String getUsername() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        return username.isEmpty() ? "Anonymous" : username;
    }

    private void sendUsername(String username) throws IOException {
        output.writeUTF(username);
        output.flush();
    }

    private void startMessageListener() {
        Thread listener = new Thread(this::listenForMessages);
        listener.setDaemon(true);
        listener.start();
    }

    private void handleUserInput() {
        try {
            while (true) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("/exit")) {
                    output.writeUTF("/exit");
                    break;
                }
                output.writeUTF(message);
                output.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            ExceptionHandler.handle("Sending message", e);
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                String message = input.readUTF();
                System.out.println(message);
            }
        } catch (IOException e) {
            System.out.println("[Server disconnected]");
            ExceptionHandler.handle("Listening for messages", e);
        }
    }

    private void closeConnections() {
        try { if (input != null) input.close(); } catch (IOException ignored) {}
        try { if (output != null) output.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        scanner.close();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ChatClient <server-ip> <server-port>");
            return;
        }
        String ip = args[0];
        int port = getPortFromArgs(args);
        ChatClient client = new ChatClient(ip, port);
        client.start();
    }

    private static int getPortFromArgs(String[] args) {
        try {
            return Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port. Using default 5000.");
            return 5000;
        }
    }
}
