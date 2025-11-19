import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final String LOG_FILE = "server_history.txt";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static synchronized void log(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timeStamp = "[" + dtf.format(LocalDateTime.now()) + "] ";
            pw.println(timeStamp + message);
        } catch (IOException e) {
            System.err.println("[Logger Error] Could not write to log file: " + e.getMessage());
        }
    }
}
