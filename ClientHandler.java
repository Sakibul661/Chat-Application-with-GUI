import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends NetworkEntity implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        initializeStreams();
    }

    private void initializeStreams() {
        try {
            this.input = new DataInputStream(socket.getInputStream());
            this.output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            handleError("Error setting up client streams", e);
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            username = input.readUTF();
            server.log(username + " joined from " + socket.getRemoteSocketAddress());
            server.broadcast("[" + getCurrentTime() + "] " + username + " joined the chat.", this);
            sendMessage("[Server] Welcome, " + username + "! Type /exit to leave.");

            while (connected) {
                String msg = input.readUTF();
                handleMessage(msg);
            }
        } catch (IOException e) {
            handleError("Connection lost with " + username, e);
        } finally {
            disconnect();
        }
    }

    @Override
    public void handleMessage(String message) throws IOException {
        if (message.equalsIgnoreCase("/exit")) {
            connected = false;
            return;
        }
        String formatted = "[" + getCurrentTime() + "] " + username + ": " + message;
        server.log(formatted);
        server.broadcast(formatted, this);
    }

    @Override
    public void disconnect() {
        if (!connected) {
            // already disconnected flag maybe set; still proceed with removal
        }
        connected = false;
        server.removeClient(this);
        server.broadcast("[" + getCurrentTime() + "] " + username + " left the chat.", this);
        closeResources();
    }

    @Override
    protected void handleError(String message, Exception e) {
        server.log(message + ": " + e.getMessage());
        ExceptionHandler.handle(message, e); // integrated exception handler
    }

    private void closeResources() {
        try {
            if (input != null) input.close();
        } catch (IOException ignored) {}
        try {
            if (output != null) output.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() {
        return username;
    }
}
