import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class UDPServerFrame extends JFrame {
    private UDPServerCombinedPanel combinedPanel;

    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        setupLookAndFeel();
        EventQueue.invokeLater(() -> {
            try {
                UDPServerFrame frame = new UDPServerFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public UDPServerFrame() {
        setTitle("UDP Server Manager - Chat & File Transfer");
        setBounds(100, 100, 1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(950, 600));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // Create combined panel
        combinedPanel = new UDPServerCombinedPanel();
        contentPane.add(combinedPanel, BorderLayout.CENTER);

        // Bottom panel: Back button
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
}

// ==================== UDP SERVER COMBINED PANEL ====================
class UDPServerCombinedPanel extends JPanel {
    private JTextField txtPort;

    private JTextField txtSelectedFile;
    private JTextField inputMessage;
    private JTextArea chattingHistory;
    private JTextArea transferHistory;
    private JTextArea availableFilesArea;
    private JLabel lblStatus;
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnSend;
    private JButton btnSendFile;
    private JProgressBar progressBar;

    private volatile boolean chatServerRunning = false;
    private volatile boolean fileServerRunning = false;
    private DatagramSocket chatSocket;
    private final ConcurrentHashMap<String, ClientInfo> chatClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> fileClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileReceiveState> fileReceiveStates = new ConcurrentHashMap<>();

    private class ClientInfo {
        InetAddress address;
        int port;
        String username;
        String room; // Room identifier for grouping clients

        ClientInfo(InetAddress address, int port, String username, String room) {
            this.address = address;
            this.port = port;
            this.username = username;
            this.room = room != null && !room.isEmpty() ? room : "default";
        }
    }

    private class FileReceiveState {
        FileOutputStream fos;
        long totalReceived = 0;
        String filePath;

        FileReceiveState(String fileName, long fileSize, String serverPath) throws IOException {
            File targetFile;
            if (serverPath != null && !serverPath.isEmpty()) {
                targetFile = new File(serverPath, "received_" + fileName);
            } else {
                targetFile = new File("received_" + fileName);
            }

            this.filePath = targetFile.getAbsolutePath();
            this.fos = new FileOutputStream(targetFile);
        }

        void writeData(byte[] data, int offset, int len) throws IOException {
            if (fos != null) {
                fos.write(data, offset, len);
                totalReceived += len;
            }
        }

        void close() {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getFilePath() {
            return filePath;
        }
    }

    public UDPServerCombinedPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // Top panel: Base port
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(Color.WHITE);

        JLabel lblPort = new JLabel("Base Port:");
        lblPort.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblPort);

        txtPort = new JTextField("", 6);
        txtPort.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtPort);

        btnStart = new JButton(">> Start");
        btnStart.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnStart.addActionListener(e -> startAllServers());
        topPanel.add(btnStart);

        btnStop = new JButton("[X] Stop");
        btnStop.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopAllServers());
        topPanel.add(btnStop);

        lblStatus = new JLabel("Status: Stopped");
        lblStatus.setFont(new Font("Times New Roman", Font.BOLD, 12));
        lblStatus.setForeground(Color.BLACK);
        topPanel.add(lblStatus);

        add(topPanel, BorderLayout.NORTH);

        // Center: Split pane with chat and file transfer
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        // Chat section
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                "Chat (Port: base, shared with file transfer)",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Times New Roman", Font.BOLD, 14),
                Color.BLACK));

        chattingHistory = new JTextArea();
        chattingHistory.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        chattingHistory.setEditable(false);
        chattingHistory.setLineWrap(true);
        chattingHistory.setWrapStyleWord(true);
        chattingHistory.setBackground(Color.WHITE);
        chattingHistory.setForeground(Color.BLACK);

        JScrollPane chatScrollPane = new JScrollPane(chattingHistory);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Chat input
        // Create emoji bar
        JPanel emojiBar = createEmojiBar();

        JPanel chatInputPanel = new JPanel(new BorderLayout(10, 10));
        chatInputPanel.setBackground(Color.WHITE);

        inputMessage = new JTextField();
        inputMessage.setFont(new Font("Dialog", Font.PLAIN, 14));
        inputMessage.setBackground(Color.WHITE);
        inputMessage.setForeground(Color.BLACK);
        inputMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSend.doClick();
                }
            }
        });

        btnSend = new JButton("> Send");
        btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSend.setPreferredSize(new Dimension(100, 30));
        btnSend.addActionListener(e -> sendBroadcast());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(btnSend);

        chatInputPanel.add(inputMessage, BorderLayout.CENTER);
        chatInputPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel chatBottomPanel = new JPanel(new BorderLayout(10, 10));
        chatBottomPanel.setBackground(Color.WHITE);
        chatBottomPanel.add(emojiBar, BorderLayout.NORTH);
        chatBottomPanel.add(chatInputPanel, BorderLayout.CENTER);
        chatPanel.add(chatBottomPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(chatPanel);

        // File transfer section
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBackground(Color.WHITE);
        filePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                "File Transfer (Port: base, shared)",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Times New Roman", Font.BOLD, 14),
                Color.BLACK));

        // Split panel for transfer history and available files
        JSplitPane fileSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        fileSplitPane.setDividerLocation(150);
        fileSplitPane.setResizeWeight(0.5);
        fileSplitPane.setContinuousLayout(true);

        transferHistory = new JTextArea();
        transferHistory.setEditable(false);
        transferHistory.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        transferHistory.setBackground(Color.WHITE);
        transferHistory.setForeground(Color.BLACK);
        fileSplitPane.setTopComponent(new JScrollPane(transferHistory));

        availableFilesArea = new JTextArea();
        availableFilesArea.setEditable(false);
        availableFilesArea.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        availableFilesArea.setBackground(Color.WHITE);
        availableFilesArea.setForeground(Color.BLACK);
        fileSplitPane.setBottomComponent(new JScrollPane(availableFilesArea));

        filePanel.add(fileSplitPane, BorderLayout.CENTER);

        // File controls
        JPanel fileControlPanel = new JPanel(new BorderLayout(10, 10));
        fileControlPanel.setBackground(Color.WHITE);

        // File selection panel
        JPanel fileSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        fileSelectionPanel.setBackground(Color.WHITE);

        JLabel lblSelect = new JLabel("Selected File:");
        lblSelect.setFont(new Font("Times New Roman", Font.BOLD, 12));
        fileSelectionPanel.add(lblSelect);

        txtSelectedFile = new JTextField();
        txtSelectedFile.setPreferredSize(new Dimension(300, 25));
        txtSelectedFile.setEditable(false);
        txtSelectedFile.setBackground(Color.WHITE);
        fileSelectionPanel.add(txtSelectedFile);

        JButton btnSelectFile = new JButton("[ ] Select");
        btnSelectFile.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnSelectFile.setPreferredSize(new Dimension(120, 30));
        btnSelectFile.addActionListener(e -> selectSingleFile());
        fileSelectionPanel.add(btnSelectFile);

        btnSendFile = new JButton(">> Send All");
        btnSendFile.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        btnSendFile.setPreferredSize(new Dimension(130, 30));
        btnSendFile.addActionListener(e -> sendFileToAllClients());
        fileSelectionPanel.add(btnSendFile);

        fileControlPanel.add(fileSelectionPanel, BorderLayout.NORTH);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressPanel.setBackground(Color.WHITE);

        JLabel lblProgress = new JLabel("Progress:");
        lblProgress.setFont(new Font("Times New Roman", Font.BOLD, 12));
        lblProgress.setForeground(Color.BLACK);
        progressPanel.add(lblProgress);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(250, 25));
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar);

        fileControlPanel.add(progressPanel, BorderLayout.CENTER);
        filePanel.add(fileControlPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(filePanel);

        // Create tabbed pane for different features
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Chat & File Transfer
        tabbedPane.addTab("Chat & File Transfer", splitPane);

        // Tab 2: Tic-Tac-Toe Game (Server monitoring)
        JPanel gameMonitorPanel = new JPanel(new BorderLayout(10, 10));
        gameMonitorPanel.setBackground(Color.WHITE);
        gameMonitorPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel gameInfoLabel = new JLabel("Game Status: Ready to accept game connections");
        gameInfoLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
        gameMonitorPanel.add(gameInfoLabel, BorderLayout.NORTH);

        JTextArea gameStatusArea = new JTextArea();
        gameStatusArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        gameStatusArea.setEditable(false);
        gameStatusArea.setLineWrap(true);
        gameStatusArea.setWrapStyleWord(true);
        gameStatusArea.setText("Game server features:\n\n" +
                "- Supports board sizes from 3x3 to 21x21\n" +
                "- Real-time game synchronization via UDP\n" +
                "- Automatic player pairing\n" +
                "- Move validation and game state tracking\n\n" +
                "Status: Game server active\n" +
                "Waiting for players...");

        JScrollPane gameScrollPane = new JScrollPane(gameStatusArea);
        gameScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        gameMonitorPanel.add(gameScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Game Status", gameMonitorPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== START/STOP ALL SERVERS ====================
    private void startAllServers() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Port must be in range 1-65535!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be an integer!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Start combined server on single port
        if (!chatServerRunning) {
            try {
                chatSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
                chatSocket.setSoTimeout(0); // No timeout - persistent connection
                chatServerRunning = true;
                fileServerRunning = true;

                String serverIP = InetAddress.getLocalHost().getHostAddress();
                appendChat("[UDP] ⚠ Server listening on " + serverIP + ":" + port + " (Connectionless)");
                appendChat("[Game] Server ready to accept game connections");

                Thread receiveThread = new Thread(() -> combinedReceiveLoop(), "udp-server-receive");
                receiveThread.start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error starting server: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        lblStatus.setText("Status: UDP SERVER RUNNING ▼ (Connectionless, Unreliable)");
        appendChat("[UDP SERVER] Listening on port " + port + " (Connectionless)");
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void stopAllServers() {
        chatServerRunning = false;
        fileServerRunning = false;

        int connectedClients = chatClients.size();
        if (connectedClients > 0) {
            appendChat("[UDP] Disconnecting " + connectedClients + " client(s)...");
        }

        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
            }
        } catch (Exception ignored) {
        }

        chatClients.clear();
        fileClients.clear();

        // Close any active file transfers
        for (FileReceiveState state : fileReceiveStates.values()) {
            try {
                state.close();
            } catch (Exception ignored) {
            }
        }
        fileReceiveStates.clear();

        lblStatus.setText("Status: UDP SERVER STOPPED");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        appendChat("[UDP] Server stopped. (" + connectedClients + " clients disconnected)");
        appendHistory("[UDP] All resources released.");
    }

    // ==================== COMBINED RECEIVE LOOP (Single Port) ====================

    private void combinedReceiveLoop() {
        try {
            byte[] buffer = new byte[65535]; // UDP max packet size
            while (chatServerRunning && fileServerRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    chatSocket.receive(packet);
                    byte[] dataBytes = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, dataBytes, 0, packet.getLength());
                    String message = new String(dataBytes, "UTF-8");

                    String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    ClientInfo info = chatClients.get(clientKey);

                    // Route by protocol prefix
                    if (message.startsWith("CMD|LOGIN")) {
                        // LOGIN - Client sends username and room after authentication at client side
                        // Format: CMD|LOGIN|username|room
                        String[] authParts = message.split("\\|");
                        if (authParts.length >= 3) {
                            String username = authParts[2].trim();
                            String room = authParts.length >= 4 ? authParts[3].trim() : "default";
                            if (!username.isEmpty()) {
                                info = new ClientInfo(packet.getAddress(), packet.getPort(), username, room);
                                chatClients.put(clientKey, info);
                                appendChat("[UDP] " + username + " connected to room '" + info.room + "' from "
                                        + clientKey);

                                String response = "CMD|AUTH_OK|WELCOME";
                                byte[] responseData = response.getBytes("UTF-8");
                                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                                        packet.getAddress(), packet.getPort());
                                chatSocket.send(responsePacket);
                            }
                        }
                    } else if (message.startsWith("CMD|CREATE_ROOM")) {
                        String[] parts = message.split("\\|");
                        if (parts.length >= 3) {
                            String roomName = parts[2].trim();
                            if (info != null) {
                                info.room = roomName;
                                appendChat("[Room] " + info.username + " created room '" + roomName + "'");
                            }
                        }
                    } else if (message.startsWith("CMD|JOIN_ROOM")) {
                        String[] parts = message.split("\\|");
                        if (parts.length >= 3) {
                            String roomName = parts[2].trim();
                            if (info != null) {
                                info.room = roomName;
                                appendChat("[Room] " + info.username + " joined room '" + roomName + "'");
                            }
                        }
                    } else if (message.startsWith("CMD|LEAVE_ROOM")) {
                        if (info != null) {
                            appendChat("[Room] " + info.username + " left room '" + info.room + "'");
                            info.room = "default";
                        }
                    } else if (message.startsWith("CMD|REGISTER")) {
                        // REGISTER
                        String[] authParts = message.split("\\|");
                        if (authParts.length >= 4) {
                            String username = authParts[2];
                            String password = authParts[3];

                            boolean success = UDPUserManager.register(username, password);

                            if (success) {
                                appendChat("[UDP] " + username + " registered from " + clientKey);
                                String response = "CMD|REG_OK";
                                byte[] responseData = response.getBytes("UTF-8");
                                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                                        packet.getAddress(), packet.getPort());
                                chatSocket.send(responsePacket);
                            } else {
                                appendChat("[UDP] Register failed for " + username + " from " + clientKey);
                                String response = "CMD|REG_FAIL";
                                byte[] responseData = response.getBytes("UTF-8");
                                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                                        packet.getAddress(), packet.getPort());
                                chatSocket.send(responsePacket);
                            }
                        }
                    } else if (message.startsWith("FILEINFO|")) {
                        // FILE TRANSFER HEADER - Initialize file receive state
                        String[] fileParts = message.split("\\|", 3);
                        if (fileParts.length >= 3) {
                            String fileName = fileParts[1];
                            long fileSize = Long.parseLong(fileParts[2]);

                            String serverPath = "";
                            try {
                                FileReceiveState state = new FileReceiveState(fileName, fileSize, serverPath);
                                fileReceiveStates.put(clientKey, state);
                                appendHistory(
                                        "[UDP FILE] From " + clientKey + ": " + fileName + " (" + fileSize + " bytes)");
                            } catch (IOException ex) {
                                appendHistory("[UDP ERROR] Failed to create file: " + ex.getMessage());
                            }
                        }
                    } else if (message.startsWith("END|")) {
                        // FILE TRANSFER END - Close file and cleanup
                        FileReceiveState state = fileReceiveStates.remove(clientKey);
                        if (state != null) {
                            state.close();
                            appendHistory("[UDP FILE] From " + clientKey + ": Transfer complete (" + state.totalReceived
                                    + " bytes received)");
                            appendHistory("[UDP FILE] Saved to: " + state.getFilePath());
                        }
                    } else if (packet.getLength() > 0 && packet.getData()[0] != 0) {
                        // DATA PACKET - Could be file data or chat message
                        String messageStr = new String(dataBytes, "UTF-8");

                        // Check if this is file data (starts with packet number bytes) or chat message
                        boolean couldBeFileData = packet.getLength() >= 4;
                        FileReceiveState state = fileReceiveStates.get(clientKey);

                        if (state != null && couldBeFileData) {
                            // Try to write as file data - skip first 4 bytes (packet number)
                            try {
                                state.writeData(dataBytes, 4, dataBytes.length - 4);
                            } catch (IOException ex) {
                                appendHistory("[UDP ERROR] Error writing file data: " + ex.getMessage());
                            }
                        } else if (info != null && !messageStr.isEmpty() && !messageStr.startsWith("FILEINFO")
                                && !messageStr.startsWith("END")) {
                            // REGULAR CHAT MESSAGE - Only broadcast to clients in same room
                            appendChat("[" + info.room + "][" + info.username + "]: " + messageStr);
                            chatBroadcast("[" + info.username + "]: " + messageStr, clientKey, info.room);
                        }
                    }
                } catch (java.io.UnsupportedEncodingException uee) {
                    if (chatServerRunning || fileServerRunning) {
                        appendChat("⚠️ [UDP] UTF-8 encoding not supported");
                    }
                } catch (java.net.SocketException e) {
                    if (chatServerRunning || fileServerRunning) {
                        appendChat("⚠️ [UDP] Network error: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            if (chatServerRunning || fileServerRunning) {
                appendChat("[UDP ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    }

    // ==================== FILE SERVER METHODS ====================
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
                inputMessage.setText(inputMessage.getText() + emoji);
                inputMessage.requestFocus();
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

    private void sendBroadcast() {
        String str = inputMessage.getText().trim();
        if (str.isEmpty()) {
            appendChat("[System] Empty message not sent");
            return;
        }

        if (chatClients.isEmpty()) {
            appendChat("[System] No clients connected. Message not broadcasted.");
            inputMessage.setText("");
            return;
        }

        appendChat("[Server] Broadcasting to all rooms: " + str);
        String broadcastMsg = "[Server] " + str;
        chatBroadcast(broadcastMsg, null); // Broadcast to all rooms
        inputMessage.setText("");
    }

    private void chatBroadcast(String message, String excludeKey, String room) {
        try {
            byte[] data = message.getBytes("UTF-8");

            // Filter clients by room and send only to clients in the same room
            for (ClientInfo client : chatClients.values()) {
                // Only send to clients in the same room
                if (room != null && !client.room.equals(room)) {
                    continue;
                }
                // Exclude sender by comparing full IP:port, not just IP
                if (excludeKey != null) {
                    String clientKey = client.address.getHostAddress() + ":" + client.port;
                    if (clientKey.equals(excludeKey)) {
                        continue;
                    }
                }
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length, client.address, client.port);
                    chatSocket.send(packet);
                } catch (Exception ex) {
                    appendChat("[UDP WARNING] Failed to send to " + client.username + ": " + ex.getMessage());
                }
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            appendChat("[UDP WARNING] UTF-8 encoding not supported for broadcast");
        }
    }

    // Overload for backward compatibility (broadcast to all rooms)
    private void chatBroadcast(String message, String excludeKey) {
        chatBroadcast(message, excludeKey, null);
    }

    private void appendChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chattingHistory.append(line + "\n");
            chattingHistory.setCaretPosition(chattingHistory.getDocument().getLength());
        });
    }

    private void selectSingleFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            txtSelectedFile.setText(selectedFile.getAbsolutePath());
            appendHistory("Selected file: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
        }
    }

    private void sendFileToAllClients() {
        String filePath = txtSelectedFile.getText().trim();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (chatClients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No clients connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            appendHistory("Sending file: " + file.getName() + " to " + chatClients.size() + " client(s)...");

            for (ClientInfo client : chatClients.values()) {
                new Thread(() -> {
                    try {
                        // Check if socket is still open
                        if (chatSocket == null || chatSocket.isClosed()) {
                            appendHistory("Error: Server socket closed");
                            return;
                        }

                        // Send file info via UDP message with UTF-8 encoding
                        String fileInfo = "FILEINFO|" + file.getName() + "|" + file.length();
                        byte[] infoBuffer = fileInfo.getBytes("UTF-8");
                        DatagramPacket infoPacket = new DatagramPacket(infoBuffer, infoBuffer.length, client.address,
                                client.port);
                        chatSocket.send(infoPacket);

                        // Send file data in chunks
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[512];
                            int seqNum = 0;
                            long totalSent = 0;
                            int read;

                            while ((read = fis.read(buffer)) > 0) {
                                // Prepend sequence number (4 bytes) to data
                                byte[] dataPacket = new byte[read + 4];
                                dataPacket[0] = (byte) ((seqNum >> 24) & 0xFF);
                                dataPacket[1] = (byte) ((seqNum >> 16) & 0xFF);
                                dataPacket[2] = (byte) ((seqNum >> 8) & 0xFF);
                                dataPacket[3] = (byte) (seqNum & 0xFF);
                                System.arraycopy(buffer, 0, dataPacket, 4, read);

                                DatagramPacket packet = new DatagramPacket(dataPacket, dataPacket.length,
                                        client.address, client.port);
                                chatSocket.send(packet);

                                totalSent += read;
                                seqNum++;
                                int progress = (int) ((totalSent * 100) / file.length());
                                updateProgress(progress);
                            }

                            // Send END marker with UTF-8 encoding
                            String endMarker = "END|" + file.getName();
                            byte[] endBuffer = endMarker.getBytes("UTF-8");
                            DatagramPacket endPacket = new DatagramPacket(endBuffer, endBuffer.length, client.address,
                                    client.port);
                            chatSocket.send(endPacket);

                            appendHistory("File sent to client: " + client.username + " (" + totalSent + " bytes)");
                        }
                    } catch (java.io.UnsupportedEncodingException uee) {
                        appendHistory("Error: UTF-8 encoding not supported for file transfer");
                    } catch (IOException ex) {
                        appendHistory("Error sending to client " + client.username + ": " + ex.getMessage());
                    }
                }).start();
            }
        }).start();
    }

    private void appendHistory(String line) {
        SwingUtilities.invokeLater(() -> {
            transferHistory.append(line + "\n");
            transferHistory.setCaretPosition(transferHistory.getDocument().getLength());
        });
    }

    private void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
    }
}
