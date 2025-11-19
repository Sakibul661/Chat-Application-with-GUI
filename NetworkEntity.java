import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class NetworkEntity {
    protected DataInputStream input;
    protected DataOutputStream output;
    protected boolean connected = true;
    protected final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void sendMessage(String message) {
        try {
            if (output != null && connected) {
                output.writeUTF(message);
                output.flush();
            }
        } catch (IOException e) {
            handleError("Error sending message", e);
        }
    }

    protected String getCurrentTime() {
        return dtf.format(LocalDateTime.now());
    }

    // Abstract methods to be implemented by subclasses
    public abstract void handleMessage(String message) throws IOException;
    public abstract void disconnect();
    protected abstract void handleError(String message, Exception e);

    public boolean isConnected() {
        return connected;
    }
}
