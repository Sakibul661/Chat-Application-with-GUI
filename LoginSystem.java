import java.io.*;
import java.util.regex.Pattern;

public class LoginSystem {
    private static final String USER_FILE = "users.txt";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    public static synchronized boolean register(String email, String password) {
        if (email == null || password == null) {
            System.out.println("Email or password cannot be null!");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            System.out.println("Invalid email format!");
            return false;
        }

        if (userExists(email)) {
            System.out.println("User already exists!");
            return false;
        }

        try (FileWriter fw = new FileWriter(USER_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(email + "," + password);
            System.out.println("Registration successful!");
            ServerLogger.log("New user registered: " + email);
            return true;
        } catch (IOException e) {
            ExceptionHandler.handle("Registering user failed", e);
            return false;
        }
    }

    public static synchronized boolean login(String email, String password) {
        if (email == null || password == null) return false;

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            System.out.println("Invalid email format!");
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2 && parts[0].equals(email) && parts[1].equals(password)) {
                    System.out.println("Login successful!");
                    ServerLogger.log("User logged in: " + email);
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            // file not yet created - no users registered
        } catch (IOException e) {
            ExceptionHandler.handle("Login check failed", e);
        }

        System.out.println("Invalid credentials!");
        return false;
    }

    private static synchronized boolean userExists(String email) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(email + ",")) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }
}
