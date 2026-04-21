import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class TCPClientFrame extends JFrame {
    private TCPChatPanel chatPanel;
    private TCPFileTransferPanel fileTransferPanel;
    private XOGamePanel gamePanel;
    private XOGameFrame gameFrame;
    private String username;

    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        setupLookAndFeel();
        EventQueue.invokeLater(() -> {
            try {
                TCPClientFrame frame = new TCPClientFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        });
    }

    public TCPClientFrame() {
        this("");
    }

    public TCPClientFrame(String username) {
        this.username = username;
        setTitle("TCP Client Frame - Chat & File Transfer (Single Port)");
        setBounds(100, 100, 1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(850, 500));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(Color.WHITE);

        JLabel lblServer = new JLabel("Server (IP/Hostname):");
        lblServer.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblServer);

        JTextField txtServer = new JTextField(6);
        txtServer.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtServer);

        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblPort);

        JTextField txtPort = new JTextField(6);
        txtPort.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtPort);

        JButton btnStart = new JButton(">> Start");
        btnStart.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(btnStart);

        JButton btnStop = new JButton("[X] Stop");
        btnStop.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnStop.setEnabled(false);
        topPanel.add(btnStop);

        JButton btnXOGame = new JButton("XO Game");
        btnXOGame.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnXOGame.addActionListener(e -> openXOGame());
        topPanel.add(btnXOGame);

        JLabel lblStatus = new JLabel("Status: Disconnected");
        lblStatus.setFont(new Font("Times New Roman", Font.BOLD, 11));
        topPanel.add(lblStatus);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        chatPanel = new TCPChatPanel(username, txtServer, txtPort, lblStatus, btnStart, btnStop, this);
        fileTransferPanel = new TCPFileTransferPanel(txtServer, txtPort, chatPanel);
        gamePanel = null;
        gameFrame = null;

        splitPane.setLeftComponent(chatPanel);
        splitPane.setRightComponent(fileTransferPanel);
        contentPane.add(splitPane, BorderLayout.CENTER);

        // Buttons
        btnStart.addActionListener(e -> {
            chatPanel.startConnection();
            fileTransferPanel.setConnected(true);
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        });

        btnStop.addActionListener(e -> {
            chatPanel.disconnect();
            fileTransferPanel.setConnected(false);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });

        // Bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnBack = new JButton("< Back");
        btnBack.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnBack.setPreferredSize(new Dimension(100, 30));
        btnBack.addActionListener(e -> {
            try {
                MainMenu.setupLookAndFeel();
                new MainMenu().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomPanel.add(btnBack);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void openXOGame() {
        if (gameFrame != null && gameFrame.isDisplayable()) {
            gameFrame.toFront();
            return;
        }
        
        if (!chatPanel.isConnected()) {
            JOptionPane.showMessageDialog(this, "Connect to server first!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            gameFrame = new XOGameFrame(username, chatPanel);
            gamePanel = gameFrame.getGamePanel();
            chatPanel.sendGameMessage("GAME|JOIN|" + gamePanel.getGameId() + "|" + username);
            gameFrame.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error opening game: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public XOGamePanel getGamePanel() {
        return gamePanel;
    }
}

// ==================== TCP CHAT PANEL ====================
class TCPChatPanel extends JPanel implements Runnable, GameMessageListener {
    private final JTextField txtServer;
    private final JTextField txtPort;
    private final JTextArea chatArea;
    private final JTextField msgInput;
    private final JButton btnSend;
    private final JLabel lblStatus;
    private final JButton btnStart;
    private final JButton btnStop;
    
    // Room functionality
    private final JTextField txtRoomName;
    private final JButton btnCreateRoom;
    private final JButton btnJoinRoom;
    private final JButton btnLeaveRoom;
    private final JLabel lblCurrentRoom;
    private String currentRoom = "";

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private volatile boolean connected = false;
    private String username = "";
    
    // Reference to parent frame to access gamePanel
    private TCPClientFrame parentFrame;

    public TCPChatPanel(String username, JTextField txtServer, JTextField txtPort, JLabel lblStatus, JButton btnStart, JButton btnStop, TCPClientFrame parentFrame) {
        this.txtServer = txtServer;
        this.txtPort = txtPort;
        this.lblStatus = lblStatus;
        this.btnStart = btnStart;
        this.btnStop = btnStop;
        this.username = username.isEmpty() ? "User" : username;
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "Chat (Port: base)",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 14),
            Color.BLACK
        ));
        setBackground(Color.WHITE);

        chatArea = new JTextArea();
        chatArea.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(Color.WHITE);
        chatArea.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);

        // Create emoji bar
        JPanel emojiBar = createEmojiBar();
        
        // Create message input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(Color.WHITE);
        
        msgInput = new JTextField();
        msgInput.setFont(new Font("Dialog", Font.PLAIN, 14));
        msgInput.setBackground(Color.WHITE);
        msgInput.setForeground(Color.BLACK);
        msgInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && connected) {
                    sendMessage();
                }
            }
        });

        btnSend = new JButton("> Send");
        btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> sendMessage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(btnSend);

        inputPanel.add(msgInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        bottomPanel.add(emojiBar, BorderLayout.NORTH);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        
        // Room functionality panel
        JPanel roomPanel = new JPanel(new BorderLayout(10, 5));
        roomPanel.setBackground(Color.WHITE);
        roomPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            "Room",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 11),
            Color.BLACK
        ));
        
        // Room controls panel - left column
        JPanel roomLeftPanel = new JPanel(new BorderLayout(0, 8));
        roomLeftPanel.setBackground(Color.WHITE);
        
        // Room Name input
        JPanel roomNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        roomNamePanel.setBackground(Color.WHITE);
        JLabel lblRoom = new JLabel("Room Name:");
        lblRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        roomNamePanel.add(lblRoom);
        txtRoomName = new JTextField(15);
        txtRoomName.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        roomNamePanel.add(txtRoomName);
        
        roomLeftPanel.add(roomNamePanel, BorderLayout.NORTH);
        
        // Current Room display
        JPanel currentRoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        currentRoomPanel.setBackground(Color.WHITE);
        lblCurrentRoom = new JLabel("Current Room: None");
        lblCurrentRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        lblCurrentRoom.setForeground(new Color(0, 100, 0));
        currentRoomPanel.add(lblCurrentRoom);
        
        roomLeftPanel.add(currentRoomPanel, BorderLayout.SOUTH);
        
        // Room controls panel - right column
        JPanel roomRightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        roomRightPanel.setBackground(Color.WHITE);
        
        btnCreateRoom = new JButton("+ Create");
        btnCreateRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnCreateRoom.addActionListener(e -> createRoom());
        roomRightPanel.add(btnCreateRoom);
        
        btnJoinRoom = new JButton("< Join");
        btnJoinRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnJoinRoom.addActionListener(e -> joinRoom());
        roomRightPanel.add(btnJoinRoom);
        
        btnLeaveRoom = new JButton("- Leave");
        btnLeaveRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnLeaveRoom.addActionListener(e -> leaveRoom());
        roomRightPanel.add(btnLeaveRoom);
        
        // Main room controls - combine left and right
        JPanel roomControlsPanel = new JPanel(new BorderLayout(15, 5));
        roomControlsPanel.setBackground(Color.WHITE);
        roomControlsPanel.add(roomLeftPanel, BorderLayout.WEST);
        roomControlsPanel.add(roomRightPanel, BorderLayout.CENTER);
        
        roomPanel.add(roomControlsPanel, BorderLayout.CENTER);
        
        bottomPanel.add(roomPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public boolean isConnected() {
        return connected;
    }

    public DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    public void startConnection() {
        if (connected) {
            appendChat("Connected.");
            return;
        }

        String server = txtServer.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Port must be in range 1-65535!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be an integer!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Move connection to background thread to prevent UI freezing
        new Thread(() -> {
            try {
                socket = new Socket(server, port);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                // Send username to server as first message
                dataOutputStream.writeUTF("CMD|LOGIN|" + username);
                dataOutputStream.flush();

                // Simple connection without authentication
                connected = true;
                SwingUtilities.invokeLater(() -> {
                    btnSend.setEnabled(true);
                    appendChat("[TCP] ✓ Connected to " + server + ":" + port);
                    lblStatus.setText("Status: TCP Connected");
                });
                new Thread(this, "chat-receiver").start();

            } catch (Exception e) {
                appendChat("Connection failed: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Status: TCP CONNECTION FAILED ✗");
                    JOptionPane.showMessageDialog(TCPChatPanel.this, "Connection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
                disconnect();
            }
        }, "connection-thread").start();
    }

    private void sendMessage() {
        if (!connected || socket == null || socket.isClosed()) {
            appendChat("Not connected.");
            return;
        }

        String str = msgInput.getText().trim();
        if (str.isEmpty()) return;

        new Thread(() -> {
            try {
                synchronized (dataOutputStream) {
                    // Simple message format: username: message
                    dataOutputStream.writeUTF(username + ": " + str);
                    dataOutputStream.flush();
                }

                appendChat(username + ": " + str);
                SwingUtilities.invokeLater(() -> msgInput.setText(""));

                if (str.equalsIgnoreCase("exit")) {
                    disconnect();
                }
            } catch (Exception e) {
                appendChat("Send failed: " + e.getMessage());
                disconnect();
            }
        }).start();
    }

    private void createRoom() {
        if (!connected) {
            appendChat("Not connected to server.");
            return;
        }
        String roomName = txtRoomName.getText().trim();
        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a room name!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            synchronized (dataOutputStream) {
                dataOutputStream.writeUTF("CMD|CREATE_ROOM|" + roomName);
                dataOutputStream.flush();
            }
            currentRoom = roomName;
            updateCurrentRoom();
            appendChat("[Room] Created room: " + roomName);
        } catch (Exception e) {
            appendChat("Error creating room: " + e.getMessage());
        }
    }
    
    private void joinRoom() {
        if (!connected) {
            appendChat("Not connected to server.");
            return;
        }
        String roomName = txtRoomName.getText().trim();
        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a room name!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            synchronized (dataOutputStream) {
                dataOutputStream.writeUTF("CMD|JOIN_ROOM|" + roomName);
                dataOutputStream.flush();
            }
            currentRoom = roomName;
            updateCurrentRoom();
            appendChat("[Room] Joined room: " + roomName);
        } catch (Exception e) {
            appendChat("Error joining room: " + e.getMessage());
        }
    }
    
    private void leaveRoom() {
        if (!connected || currentRoom.isEmpty()) {
            appendChat("Not in any room.");
            return;
        }
        
        try {
            synchronized (dataOutputStream) {
                dataOutputStream.writeUTF("CMD|LEAVE_ROOM|" + currentRoom);
                dataOutputStream.flush();
            }
            appendChat("[Room] Left room: " + currentRoom);
            currentRoom = "";
            updateCurrentRoom();
        } catch (Exception e) {
            appendChat("Error leaving room: " + e.getMessage());
        }
    }
    
    private void updateCurrentRoom() {
        SwingUtilities.invokeLater(() -> {
            if (currentRoom.isEmpty()) {
                lblCurrentRoom.setText("Current Room: None");
                lblCurrentRoom.setForeground(Color.BLACK);
            } else {
                lblCurrentRoom.setText("Current Room: " + currentRoom);
                lblCurrentRoom.setForeground(new Color(0, 100, 0));
            }
        });
    }

    private JPanel createEmojiBar() {
        JPanel emojiBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        emojiBar.setBackground(Color.WHITE);
        emojiBar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY));
        
        // Main emoji picker button
        JButton emojiPickerBtn = new JButton("Emoji");
        emojiPickerBtn.setFont(new Font("Times New Roman", Font.BOLD, 11));
        emojiPickerBtn.setPreferredSize(new Dimension(80, 25));
        emojiPickerBtn.setBackground(Color.WHITE);
        emojiPickerBtn.setForeground(Color.BLACK);
        emojiPickerBtn.setFocusPainted(false);
        emojiPickerBtn.setToolTipText("Click to open emoji picker");
        emojiPickerBtn.addActionListener(e -> showEmojiPicker(emojiPickerBtn));
        
        emojiBar.add(emojiPickerBtn);
        return emojiBar;
    }
    
    private void showEmojiPicker(JButton target) {
        JPopupMenu emojiMenu = new JPopupMenu();
        JPanel pickerPanel = new JPanel(new GridLayout(4, 6, 5, 5)); // 4 rows x 6 cols
        pickerPanel.setBackground(Color.WHITE);
        
        // Comprehensive emoji list - using symbols for Java 8 compatibility
        String[] emojis = {
            "☺", "❤", "✓", "✔", "✉", "✍", "☑", "☒", "👍", "😊", "💬", "⭐"
        };
        
        Font emojiFont = getEmojiFont(16);
        
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(emojiFont);
            btn.setMargin(new Insets(5, 5, 5, 5));
            btn.setBackground(Color.WHITE);
            btn.setForeground(Color.BLACK);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            btn.addActionListener(e -> {
                msgInput.setText(msgInput.getText() + emoji);
                msgInput.requestFocus();
                emojiMenu.setVisible(false);
            });
            pickerPanel.add(btn);
        }
        
        emojiMenu.add(pickerPanel);
        emojiMenu.show(target, 0, target.getHeight());
    }
    
    // Helper method to find emoji-friendly font
    private Font getEmojiFont(int size) {
        try {
            // Try emoji fonts in order of preference (optimized for Windows)
            String[] emojiFontNames = {
                "Segoe UI Emoji",
                "Segoe UI Symbol",
                "Segoe UI",
                "Noto Color Emoji",
                "Arial Unicode MS",
                "Arial",
                "Dialog"
            };
            
            for (String fontName : emojiFontNames) {
                Font testFont = new Font(fontName, Font.BOLD, size);
                // For non-Dialog fonts, return the first available
                if (!testFont.getName().equalsIgnoreCase("Dialog")) {
                    return testFont;
                }
                // For Dialog, return it only if requested
                if (fontName.equals("Dialog")) {
                    return testFont;
                }
            }
        } catch (Exception e) {
            // Fallback on error
        }
        
        // Final fallback with bold style
        return new Font("Segoe UI Emoji", Font.BOLD, size);
    }

    @Override
    public void run() {
        try {
            while (connected && !socket.isClosed()) {
                try {
                    String str = dataInputStream.readUTF();
                    if (str == null) break;
                    
                    // Handle FILE messages
                    if (str.startsWith("FILE|")) {
                        handleFileCommand(str);
                    }
                    // Handle GAME messages
                    else if (str.startsWith("GAME|")) {
                        handleGameMessage(str);
                    }
                    else {
                        appendChat(str);
                    }
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (Exception ex) {
            if (connected) appendChat("Disconnected.");
        } finally {
            disconnect();
        }
    }

    private void handleFileCommand(String command) {
        String[] parts = command.split("\\|");
        if (parts.length < 2) return;

        String action = parts[1];

        if (action.equals("SEND_FROM_SERVER")) {
            // FILE|SEND_FROM_SERVER|filename|filesize|sender_username
            if (parts.length >= 4) {
                try {
                    String filename = parts[2];
                    long fileSize = Long.parseLong(parts[3]);
                    String senderUsername = parts.length >= 5 ? parts[4] : "Unknown";
                    appendChat("[System] " + senderUsername + " shared file: " + filename);
                    receiveFileFromServer(filename, fileSize);
                } catch (Exception e) {
                    appendChat("[System] File transfer error: " + e.getMessage());
                }
            }
        }
    }

    private void handleGameMessage(String command) {
        String[] parts = command.split("\\|");
        if (parts.length < 2) return;

        String action = parts[1];
        String gameId = parts.length > 2 ? parts[2] : "";

        // Only process if we have an active game panel
        XOGamePanel gamePanel = parentFrame.getGamePanel();
        if (gamePanel == null) {
            // Game messages received but no game panel active - just log them
            appendChat("[Game] " + command);
            return;
        }

        try {
            switch (action) {
                case "JOIN":
                    // GAME|JOIN|gameId|playerName
                    if (parts.length >= 4) {
                        String playerName = parts[3];
                        gamePanel.handleOpponentJoin(gameId, playerName);
                        appendChat("[Game] " + playerName + " joined match " + gameId);
                    }
                    break;

                case "INIT":
                    // GAME|INIT|gameId|boardSize|playerName
                    if (parts.length >= 5) {
                        int boardSize = Integer.parseInt(parts[3]);
                        String playerName = parts[4];
                        gamePanel.acceptGameInvitation(gameId, boardSize, playerName);
                        appendChat("[Game] Match " + gameId + " started with " + playerName);
                    }
                    break;

                case "MOVE":
                    // GAME|MOVE|gameId|row|col|symbol
                    if (parts.length >= 6) {
                        int row = Integer.parseInt(parts[3]);
                        int col = Integer.parseInt(parts[4]);
                        String symbol = parts[5];
                        gamePanel.handleOpponentMove(row, col, symbol);
                    }
                    break;

                case "LEAVE":
                    // GAME|LEAVE|gameId
                    gamePanel.handleOpponentLeave(gameId);
                    appendChat("[Game] Opponent left match " + gameId);
                    break;

                case "REPLAY":
                    // GAME|REPLAY|gameId
                    gamePanel.handleOpponentReplay(gameId);
                    appendChat("[Game] Opponent wants rematch");
                    break;

                default:
                    appendChat("[Game] Unknown game command: " + action);
            }
        } catch (Exception e) {
            appendChat("[Game Error] Failed to process game message: " + e.getMessage());
        }
    }

    private void receiveFileFromServer(String filename, long fileSize) throws IOException {
        if (fileSize <= 0) return;

        File receivedFile = new File("received_" + filename);
        FileOutputStream fos = new FileOutputStream(receivedFile);

        byte[] buffer = new byte[4096];
        long totalReceived = 0;
        int read;

        try {
            while (totalReceived < fileSize && (read = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalReceived))) > 0) {
                fos.write(buffer, 0, read);
                totalReceived += read;
            }
            fos.flush();
            String filePath = receivedFile.getAbsolutePath();
            appendChat("[System] File received: " + receivedFile.getName() + " (" + fileSize + " bytes)");
            appendChat("[System] Saved to: " + filePath);
        } catch (IOException ex) {
            appendChat("[System] Error receiving file: " + ex.getMessage());
            throw ex;
        } finally {
            try { fos.close(); } catch (Exception ignored) {}
        }
    }

    private void appendChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(line + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public void sendGameMessage(String message) {
        if (connected && dataOutputStream != null && socket != null && !socket.isClosed()) {
            try {
                synchronized (dataOutputStream) {
                    dataOutputStream.writeUTF(message);
                    dataOutputStream.flush();
                }
            } catch (Exception e) {
                appendChat("[Error] Failed to send game data: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        connected = false;
        lblStatus.setText("Status: TCP DISCONNECTED");
        btnSend.setEnabled(false);
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });
        try { if (dataInputStream != null) dataInputStream.close(); } catch (Exception ignored) {}
        try { if (dataOutputStream != null) dataOutputStream.close(); } catch (Exception ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
        appendChat("[System] Disconnected");
    }
}

// ==================== TCP FILE TRANSFER PANEL ====================
class TCPFileTransferPanel extends JPanel {

    private JTextArea transferHistory;
    private JProgressBar progressBar;
    private JButton btnBrowse;
    private JButton btnSend;
    private JTextField txtSelectedFile;
    private File selectedFile = null;
    
    private TCPChatPanel chatPanel;
    private volatile boolean connected = false;

    public TCPFileTransferPanel(JTextField txtServer, JTextField txtPort, TCPChatPanel chatPanel) {
        this.chatPanel = chatPanel;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            "File Transfer (Same Port)",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 14),
            Color.BLACK
        ));
        setBackground(Color.WHITE);

        // Transfer history
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBackground(Color.WHITE);

        transferHistory = new JTextArea();
        transferHistory.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        transferHistory.setEditable(false);
        transferHistory.setLineWrap(true);
        transferHistory.setWrapStyleWord(true);
        transferHistory.setBackground(Color.WHITE);
        transferHistory.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(transferHistory);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);

        // File selection panel
        JPanel fileSelectionPanel = new JPanel(new BorderLayout(10, 5));
        fileSelectionPanel.setBackground(Color.WHITE);
        
        JLabel lblSelectFile = new JLabel("Selected File:");
        lblSelectFile.setFont(new Font("Times New Roman", Font.BOLD, 11));
        
        JPanel fileInputPanel = new JPanel(new BorderLayout(5, 0));
        fileInputPanel.setBackground(Color.WHITE);
        
        txtSelectedFile = new JTextField();
        txtSelectedFile.setEditable(false);
        txtSelectedFile.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        txtSelectedFile.setBackground(Color.WHITE);
        fileInputPanel.add(txtSelectedFile, BorderLayout.CENTER);
        
        btnBrowse = new JButton("+ Browse");
        btnBrowse.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnBrowse.setPreferredSize(new Dimension(100, 25));
        btnBrowse.addActionListener(e -> browseFile());
        fileInputPanel.add(btnBrowse, BorderLayout.EAST);
        
        fileSelectionPanel.add(lblSelectFile, BorderLayout.WEST);
        fileSelectionPanel.add(fileInputPanel, BorderLayout.CENTER);
        
        bottomPanel.add(fileSelectionPanel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.setBackground(Color.WHITE);

        btnSend = new JButton("Send");
        btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> sendFile());
        buttonPanel.add(btnSend);

        JPanel buttonsWrapper = new JPanel(new BorderLayout(10, 0));
        buttonsWrapper.setBackground(Color.WHITE);
        buttonsWrapper.add(buttonPanel, BorderLayout.WEST);
        
        bottomPanel.add(buttonsWrapper, BorderLayout.CENTER);

        // Progress bar
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressPanel.setBackground(Color.WHITE);
        JLabel lblProgress = new JLabel("Progress:");
        lblProgress.setFont(new Font("Times New Roman", Font.BOLD, 12));
        progressPanel.add(lblProgress);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(250, 25));
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar);

        bottomPanel.add(progressPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        btnBrowse.setEnabled(connected);
        btnSend.setEnabled(connected && selectedFile != null);
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                txtSelectedFile.setText(selectedFile.getAbsolutePath());
                btnSend.setEnabled(connected && selectedFile != null);
                appendHistory("Selected: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
            } else {
                JOptionPane.showMessageDialog(this, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
                selectedFile = null;
                txtSelectedFile.setText("");
                btnSend.setEnabled(false);
            }
        }
    }

    private void sendFile() {
        if (!connected || !chatPanel.isConnected()) {
            JOptionPane.showMessageDialog(this, "Not connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select a file first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                DataOutputStream out = chatPanel.getDataOutputStream();
                if (out == null) {
                    appendHistory("Error: Connection not properly initialized");
                    return;
                }

                // Send file command
                synchronized (out) {
                    out.writeUTF("FILE|SEND|" + selectedFile.getName() + "|" + selectedFile.length());
                    out.flush();
                }

                // Send file content
                FileInputStream fis = new FileInputStream(selectedFile);
                byte[] buffer = new byte[4096];
                long totalSent = 0;
                int read;

                try {
                    while ((read = fis.read(buffer)) > 0) {
                        synchronized (out) {
                            out.write(buffer, 0, read);
                            totalSent += read;
                        }
                        int progress = (int) ((totalSent * 100) / selectedFile.length());
                        updateProgress(progress);
                    }
                    synchronized (out) {
                        out.flush();
                    }
                } finally {
                    fis.close();
                }

                appendHistory("File sent: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
                updateProgress(0);
            } catch (IOException ex) {
                appendHistory("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
    }

    private void appendHistory(String line) {
        SwingUtilities.invokeLater(() -> {
            transferHistory.append(line + "\n");
            transferHistory.setCaretPosition(transferHistory.getDocument().getLength());
        });
    }
}

