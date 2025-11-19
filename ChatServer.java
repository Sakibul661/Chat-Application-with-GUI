import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Server started on port " + port);
            acceptClientConnections(serverSocket);
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
            ExceptionHandler.handle("Server start error", e);
        }
    }

    private void acceptClientConnections(ServerSocket serverSocket) {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                handleNewClient(socket);
            } catch (IOException e) {
                log("Error accepting client connection: " + e.getMessage());
                ExceptionHandler.handle("Client connection accept error", e);
            }
        }
    }

    private void handleNewClient(Socket socket) {
        ClientHandler handler = new ClientHandler(socket, this);
        clients.add(handler);
        new Thread(handler).start();
        log("New client connected: " + socket.getRemoteSocketAddress());
    }

    public void broadcast(String message, ClientHandler excludeClient) {
        synchronized (clients) {
            for (ClientHandler client : new HashSet<>(clients)) {
                if (client != excludeClient && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        log("Client disconnected: " + client.getUsername());
    }

    public void log(String msg) {
        String formatted = "[Server " + dtf.format(LocalDateTime.now()) + "] " + msg;
        System.out.println(formatted);
        ServerLogger.log(msg); // integrated logger
    }

    public static void main(String[] args) {
        int port = getPortFromArgs(args);
        ChatServer server = new ChatServer(port);
        server.start();
    }

    private static int getPortFromArgs(String[] args) {
        int port = 5000;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, using default 5000.");
            }
        }
        return port;
    }
}
