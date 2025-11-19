import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ChatClientGUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JTextField ipField, portField;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private String username;

    private Thread listenerThread;

    public ChatClientGUI() {
        setTitle("My Chat Application");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initConnectionPanel();
        initChatPanel();

        setVisible(true);
    }

    private void initConnectionPanel() {
        JPanel connectPanel = new JPanel(new GridBagLayout());
        connectPanel.setBorder(BorderFactory.createTitledBorder("Server Connection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ipField = new JTextField("127.0.0.1");
        portField = new JTextField("5000");
        connectButton = new JButton("Connect");

        gbc.gridx = 0; gbc.gridy = 0;
        connectPanel.add(new JLabel("Server IP:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        connectPanel.add(ipField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        connectPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        connectPanel.add(portField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        connectPanel.add(connectButton, gbc);

        add(connectPanel, BorderLayout.NORTH);

        connectButton.addActionListener(e -> connectToServer());
    }

    private void initChatPanel() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void connectToServer() {
        String serverIP = ipField.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(serverIP, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            chatArea.append("[Connected to server " + serverIP + ":" + port + "]\n");

            // FIXED — Login is handled locally ONLY (not through server)
            if (!performLogin()) {
                JOptionPane.showMessageDialog(this, "Login failed. Disconnecting.");
                closeConnections();
                return;
            }

            username = JOptionPane.showInputDialog(this, "Enter your username:", "User", JOptionPane.PLAIN_MESSAGE);
            if (username == null || username.trim().isEmpty()) username = "Anonymous";

            // IMPORTANT FIX: Send ONLY username as first message
            output.writeUTF(username);
            output.flush();

            startMessageListener();

            sendButton.setEnabled(true);
            connectButton.setEnabled(false);
            ipField.setEnabled(false);
            portField.setEnabled(false);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ExceptionHandler.handle("GUI connect error", e);
        }
    }

    private boolean performLogin() {
        String[] options = {"Login", "Register"};
        int choice = JOptionPane.showOptionDialog(
                this, "Do you have an account?", "Login System",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        String email = JOptionPane.showInputDialog(this, "Enter your email:");
        if (email == null) return false;

        String password = JOptionPane.showInputDialog(this, "Enter your password:");
        if (password == null) return false;

        boolean success;
        if (choice == JOptionPane.YES_OPTION) {
            success = LoginSystem.login(email, password);
        } else {
            success = LoginSystem.register(email, password);
        }

        if (!success) {
            JOptionPane.showMessageDialog(this, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return success;
    }

    private void startMessageListener() {
        listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String msg = input.readUTF();
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("[Server disconnected]\n"));
                sendButton.setEnabled(false);
                ExceptionHandler.handle("GUI listener", e);
            } finally {
                closeConnections();
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;

        try {
            output.writeUTF(msg);
            output.flush();
            inputField.setText("");

            if (msg.equalsIgnoreCase("/exit")) {
                chatArea.append("[You left the chat]\n");
                closeConnections();
                sendButton.setEnabled(false);
            }
        } catch (IOException e) {
            chatArea.append("[Failed to send message]\n");
            ExceptionHandler.handle("GUI send message", e);
        }
    }

    private void closeConnections() {
        try { if (input != null) input.close(); } catch (IOException ignored) {}
        try { if (output != null) output.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(false);
            connectButton.setEnabled(true);
            ipField.setEnabled(true);
            portField.setEnabled(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
