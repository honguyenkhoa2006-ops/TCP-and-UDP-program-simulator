import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TCPServerFrame extends JFrame {
    private TCPServerCombinedPanel combinedPanel;

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
                TCPServerFrame frame = new TCPServerFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        });
    }

    public TCPServerFrame() {
        setTitle("TCP Server Manager - Chat & File Transfer");
        setBounds(100, 100, 1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(850, 500));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // Create combined panel
        combinedPanel = new TCPServerCombinedPanel();
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

// ==================== TCP SERVER COMBINED PANEL ====================
class TCPServerCombinedPanel extends JPanel {
    private final JTextField txtPort;

    private final JTextField txtSelectedFile;
    private final JTextField inputMessage;
    private final JTextArea chattingHistory;
    private final JTextArea transferHistory;
    private final JTextArea availableFilesArea;
    private final JLabel lblStatus;
    private final JButton btnStart;
    private final JButton btnStop;
    private final JButton btnSend;
    private final JButton btnSendFile;
    private final JProgressBar progressBar;

    private volatile boolean serverRunning = false;
    private ServerSocket combinedSocket;
    private final ConcurrentHashMap<Integer, ClientCombinedHandler> chatClients = new ConcurrentHashMap<>();
    private final AtomicInteger clientIdSeq = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public TCPServerCombinedPanel() {
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
            "Chat (Port: base)",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 14),
            Color.BLACK
        ));

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
            Color.BLACK
        ));

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

        // File selection main panel
        JPanel fileSelectPanel = new JPanel(new BorderLayout(0, 8));
        fileSelectPanel.setBackground(Color.WHITE);

        // File selection panel - first row (Selected file info)
        JPanel fileNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        fileNamePanel.setBackground(Color.WHITE);

        JLabel lblFileSelect = new JLabel("Selected:");
        lblFileSelect.setFont(new Font("Times New Roman", Font.BOLD, 12));
        fileNamePanel.add(lblFileSelect);

        txtSelectedFile = new JTextField(25);
        txtSelectedFile.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        txtSelectedFile.setEditable(false);
        txtSelectedFile.setBackground(Color.WHITE);
        fileNamePanel.add(txtSelectedFile);

        JButton btnSelectFile = new JButton("[ ] Select");
        btnSelectFile.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnSelectFile.setPreferredSize(new Dimension(120, 30));
        btnSelectFile.addActionListener(e -> selectSingleFile());
        fileNamePanel.add(btnSelectFile);

        fileSelectPanel.add(fileNamePanel, BorderLayout.NORTH);

        // File selection panel - second row (Send button)
        JPanel sendButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sendButtonPanel.setBackground(Color.WHITE);

        btnSendFile = new JButton(">> Send All");
        btnSendFile.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSendFile.setPreferredSize(new Dimension(130, 30));
        btnSendFile.addActionListener(e -> sendFileToAllClients());
        sendButtonPanel.add(btnSendFile);

        fileSelectPanel.add(sendButtonPanel, BorderLayout.SOUTH);

        fileControlPanel.add(fileSelectPanel, BorderLayout.NORTH);

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
        add(splitPane, BorderLayout.CENTER);
    }

    // ==================== START/STOP ALL SERVERS ====================
    private void startAllServers() {
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

        // Start combined server on single port
        if (!serverRunning) {
            try {
                combinedSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
                serverRunning = true;
                String serverIP = InetAddress.getLocalHost().getHostAddress();
                appendChat("[TCP] ✓ Server listening on " + serverIP + ":" + port);
                appendHistory("File & Chat on " + serverIP + ":" + port);
                Thread acceptThread = new Thread(() -> combinedAcceptLoop(), "tcp-combined-accept");
                acceptThread.start();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error starting server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        lblStatus.setText("Status: TCP SERVER RUNNING ✓ (Reliable, Ordered)");
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
    }

    private void stopAllServers() {
        serverRunning = false;

        try {
            if (combinedSocket != null && !combinedSocket.isClosed()) {
                combinedSocket.close();
            }
        } catch (Exception ignored) {}

        lblStatus.setText("Status: TCP SERVER STOPPED");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        appendChat("Server stopped (chat & file).");
        appendHistory("Server stopped.");
    }

    private void combinedAcceptLoop() {
        while (serverRunning) {
            try {
                Socket clientSocket = combinedSocket.accept();
                int clientId = clientIdSeq.incrementAndGet();
                ClientCombinedHandler handler = new ClientCombinedHandler(clientId, clientSocket);
                Thread t = new Thread(handler, "tcp-client-" + clientId);
                t.start();
            } catch (IOException ex) {
                if (serverRunning) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }

    private void sendBroadcast() {
        String str = inputMessage.getText().trim();
        if (str.isEmpty()) return;

        appendChat("Me (Server) say: " + str);
        chatBroadcast("[Server] " + str, -1);
        inputMessage.setText("");
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

    private void chatBroadcast(String message, int excludeClientId) {
        for (ClientCombinedHandler ch : chatClients.values()) {
            if (ch.clientId == excludeClientId) continue;
            ch.send(message);
        }
    }

    private void removeChatClient(int clientId) {
        ClientCombinedHandler ch = chatClients.remove(clientId);
        if (ch != null) {
            try {
                ch.close();
            } catch (Exception ignored) {}
            String name = ch.username != null ? ch.username : "Client#" + clientId;
            appendChat(name + " disconnected.");
            chatBroadcast("[System] " + name + " left.", clientId);
        }
    }

    private void appendChat(String line) {
        SwingUtilities.invokeLater(() -> {
            chattingHistory.append(line + "\n");
            chattingHistory.setCaretPosition(chattingHistory.getDocument().getLength());
        });
    }



    private void selectSingleFile() {
        String folderPath = "";
        JFileChooser fileChooser = new JFileChooser();
        if (!folderPath.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(folderPath));
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
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

        File file = new File(filePath);
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
            
            for (ClientCombinedHandler client : chatClients.values()) {
                new Thread(() -> {
                    try {
                        // Check if client is still connected
                        if (!client.running) {
                            appendHistory("Client #" + client.clientId + " disconnected before file transfer");
                            return;
                        }

                        synchronized (client.dos) {
                            if (client.socket.isClosed()) {
                                appendHistory("Client #" + client.clientId + " socket closed");
                                return;
                            }
                            client.dos.writeUTF("FILE|SEND_FROM_SERVER|" + file.getName() + "|" + file.length());
                            client.dos.flush();
                        }

                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        long totalSent = 0;
                        int read;

                        try {
                            while ((read = fis.read(buffer)) > 0) {
                                // Check if client is still running before sending each chunk
                                if (!client.running || client.socket.isClosed()) {
                                    appendHistory("Client #" + client.clientId + " disconnected during file transfer");
                                    break;
                                }

                                synchronized (client.dos) {
                                    client.dos.write(buffer, 0, read);
                                    totalSent += read;
                                }
                                int progress = (int) ((totalSent * 100) / file.length());
                                updateProgress(progress);
                            }

                            synchronized (client.dos) {
                                if (!client.socket.isClosed()) {
                                    client.dos.flush();
                                }
                            }
                        } finally {
                            fis.close();
                        }

                        if (totalSent == file.length()) {
                            appendHistory("File sent to client #" + client.clientId + ": " + file.getName());
                        }
                    } catch (IOException ex) {
                        appendHistory("Error sending to client #" + client.clientId + ": " + ex.getMessage());
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

    // ==================== COMBINED CLIENT HANDLER ====================
    private class ClientCombinedHandler implements Runnable {
        private final int clientId;
        private final Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private volatile boolean running = true;
        public String username;
        private String currentRoom = "";

        ClientCombinedHandler(int clientId, Socket socket) throws IOException {
            this.clientId = clientId;
            this.socket = socket;
            this.dis = new DataInputStream(socket.getInputStream());
            this.dos = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                username = "User_" + clientId;
                chatClients.put(clientId, this);
                
                // Wait for login command from client (first message)
                String firstMessage = dis.readUTF();
                if (firstMessage != null && firstMessage.startsWith("CMD|LOGIN|")) {
                    String[] parts = firstMessage.split("\\|");
                    if (parts.length >= 3) {
                        username = parts[2].trim();
                    }
                }
                
                appendChat(username + " connected.");
                chatBroadcast("[System] " + username + " joined the chat.", clientId);

                // Handle chat and file messages
                while (running) {
                    try {
                        String message = dis.readUTF();
                        if (message == null || message.isEmpty()) break;
                        
                        // Handle room commands
                        if (message.startsWith("CMD|")) {
                            handleRoomCommand(message);
                            continue;
                        }
                        
                        // Handle FILE commands
                        if (message.startsWith("FILE|")) {
                            processFileCommand(message);
                            continue;
                        }
                        
                        // Handle GAME messages - broadcast to all clients
                        if (message.startsWith("GAME|")) {
                            appendChat("[Game] " + username + ": " + message);
                            chatBroadcast(message, clientId);
                            continue;
                        }
                        
                        // Regular chat message
                        if (!currentRoom.isEmpty()) {
                            // Send to room members only
                            sendToRoom(currentRoom, message);
                        } else {
                            // Broadcast to all
                            appendChat(message);
                            chatBroadcast(message, clientId);
                        }
                        
                    } catch (EOFException e) {
                        appendChat("[System] Client #" + clientId + " disconnected.");
                        break;
                    } catch (IOException e) {
                        appendChat("[System] Client #" + clientId + " connection lost.");
                        break;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Handler error: " + ex.getMessage());
                ex.printStackTrace();
                appendChat("[System] Client #" + clientId + " error: " + ex.getMessage());
                running = false;
            } finally {
                running = false;
                try {
                    if (dis != null) dis.close();
                    if (dos != null) dos.close();
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (Exception ignored) {}
                removeChatClient(clientId);
                
                // Check if any clients remain
                if (chatClients.isEmpty()) {
                    appendChat("[System] No clients connected. Server ready for new connections.");
                }
            }
        }

        private void handleRoomCommand(String command) {
            // Format: CMD|CREATE_ROOM|roomname or CMD|JOIN_ROOM|roomname or CMD|LEAVE_ROOM|roomname
            String[] parts = command.split("\\|", 3);
            if (parts.length < 3) return;
            
            String action = parts[1];
            String roomName = parts[2];
            
            switch (action) {
                case "CREATE_ROOM":
                    createRoom(roomName);
                    break;
                case "JOIN_ROOM":
                    joinRoom(roomName);
                    break;
                case "LEAVE_ROOM":
                    leaveRoom(roomName);
                    break;
            }
        }
        
        private void createRoom(String roomName) {
            if (rooms.containsKey(roomName)) {
                appendChat("[Room] Room '" + roomName + "' already exists. Client #" + clientId + " was told.");
                return;
            }
            
            Room room = new Room(roomName);
            room.addMember(clientId, username);
            rooms.put(roomName, room);
            currentRoom = roomName;
            appendChat("[Room] " + username + " (Client #" + clientId + ") created room: " + roomName);
        }
        
        private void joinRoom(String roomName) {
            Room room = rooms.get(roomName);
            if (room == null) {
                appendChat("[Room] Room '" + roomName + "' does not exist. Client #" + clientId + " cannot join.");
                return;
            }
            
            // Leave current room first if in one
            if (!currentRoom.isEmpty() && !currentRoom.equals(roomName)) {
                leaveRoom(currentRoom);
            }
            
            room.addMember(clientId, username);
            currentRoom = roomName;
            appendChat("[Room] " + username + " (Client #" + clientId + ") joined room: " + roomName);
            
            // Notify room members
            broadcastToRoom(roomName, "[System] " + username + " joined the room.", clientId);
        }
        
        private void leaveRoom(String roomName) {
            Room room = rooms.get(roomName);
            if (room == null) return;
            
            room.removeMember(clientId);
            
            if (room.getMemberCount() == 0) {
                rooms.remove(roomName);
                appendChat("[Room] Room '" + roomName + "' deleted (empty).");
            } else {
                broadcastToRoom(roomName, "[System] " + username + " left the room.", clientId);
                appendChat("[Room] " + username + " (Client #" + clientId + ") left room: " + roomName);
            }
            
            currentRoom = "";
        }
        
        private void sendToRoom(String roomName, String message) {
            Room room = rooms.get(roomName);
            if (room == null) return;
            
            appendChat("[Room: " + roomName + "] " + message);
            broadcastToRoom(roomName, message, clientId);
        }
        
        private void broadcastToRoom(String roomName, String message, int excludeClientId) {
            Room room = rooms.get(roomName);
            if (room == null) return;
            
            for (Integer memberId : room.getMembers().keySet()) {
                if (memberId == excludeClientId) continue;
                
                ClientCombinedHandler handler = chatClients.get(memberId);
                if (handler != null && handler.running) {
                    try {
                        synchronized (handler.dos) {
                            handler.dos.writeUTF(message);
                            handler.dos.flush();
                        }
                    } catch (Exception e) {
                        System.out.println("Error sending to room member: " + e.getMessage());
                    }
                }
            }
        }

        private void processFileCommand(String command) throws IOException {
            if (command == null || !command.startsWith("FILE|")) {
                return;
            }

            String[] parts = command.split("\\|");
            if (parts.length < 2) return;

            String action = parts[1]; // SEND or GET

            if (action.equals("SEND")) {
                // FILE|SEND|filename|filesize
                if (parts.length >= 4) {
                    String filename = parts[2];
                    long fileSize = Long.parseLong(parts[3]);
                    receiveFile(filename, fileSize);
                }
            } else if (action.equals("GET")) {
                // FILE|GET|filename
                if (parts.length >= 3) {
                    String filename = parts[2];
                    sendFile(filename);
                }
            }
        }

        private void receiveFile(String filename, long fileSize) throws IOException {
            if (fileSize <= 0) return;
            
            String serverRootPath = "";
            File file = new File(filename);
            if (!file.isAbsolute() && !serverRootPath.isEmpty()) {
                file = new File(serverRootPath, filename);
            }
            
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int read;

            try {
                while (totalRead < fileSize && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) > 0) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                    int progress = (int) ((totalRead * 100) / fileSize);
                    updateProgress(progress);
                }
                fos.flush();
                String filePath = file.getAbsolutePath();
                appendHistory("Received file: " + file.getName() + " (Size: " + fileSize + " bytes)");
                appendHistory("Saved to: " + filePath);
                
                synchronized (dos) {
                    dos.writeUTF("FILE|RECEIVED");
                    dos.flush();
                }
                
                // Broadcast file to room members or all clients
                broadcastFileToRecipients(file, filename, fileSize);
                
            } catch (IOException ex) {
                appendHistory("Error receiving file: " + ex.getMessage());
                throw ex;
            } finally {
                try { fos.close(); } catch (Exception ignored) {}
            }
        }
        
        private void broadcastFileToRecipients(File file, String filename, long fileSize) {
            new Thread(() -> {
                try {
                    java.util.List<Integer> recipientIds = new java.util.ArrayList<>();
                    
                    if (!currentRoom.isEmpty()) {
                        // Send to all clients in the same room (except sender)
                        Room room = rooms.get(currentRoom);
                        if (room != null) {
                            for (Integer memberId : room.getMembers().keySet()) {
                                if (memberId != clientId) {
                                    recipientIds.add(memberId);
                                }
                            }
                        }
                        appendChat("[Room: " + currentRoom + "] " + username + " shared file: " + filename);
                    } else {
                        // Send to all other connected clients (not in room)
                        for (Integer otherClientId : chatClients.keySet()) {
                            if (otherClientId != clientId) {
                                recipientIds.add(otherClientId);
                            }
                        }
                        appendChat(username + " shared file: " + filename);
                    }
                    
                    // Send file to each recipient
                    for (Integer recipientId : recipientIds) {
                        ClientCombinedHandler recipient = chatClients.get(recipientId);
                        if (recipient != null && recipient.running) {
                            try {
                                synchronized (recipient.dos) {
                                    recipient.dos.writeUTF("FILE|SEND_FROM_SERVER|" + filename + "|" + fileSize + "|" + username);
                                    recipient.dos.flush();
                                }
                                
                                // Send file content
                                FileInputStream fis = new FileInputStream(file);
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                
                                try {
                                    while ((bytesRead = fis.read(buffer)) > 0) {
                                        synchronized (recipient.dos) {
                                            recipient.dos.write(buffer, 0, bytesRead);
                                        }
                                    }
                                    synchronized (recipient.dos) {
                                        recipient.dos.flush();
                                    }
                                } finally {
                                    fis.close();
                                }
                                
                                appendHistory("File broadcasted to client #" + recipientId + ": " + filename);
                            } catch (Exception e) {
                                appendHistory("Error sending file to client #" + recipientId + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    appendHistory("Error in file broadcast: " + e.getMessage());
                }
            }).start();
        }

        private void sendFile(String filename) throws IOException {
            String serverRootPath = "";
            File file = new File(serverRootPath.isEmpty() ? filename : serverRootPath + File.separator + filename);
            
            if (!file.exists()) {
                synchronized (dos) {
                    dos.writeUTF("FILE|NOT_FOUND");
                    dos.flush();
                }
                appendHistory("Client requested non-existent file: " + filename);
                return;
            }

            synchronized (dos) {
                dos.writeUTF("FILE|FOUND");
                dos.writeLong(file.length());
                dos.flush();
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            long totalSent = 0;
            int read;

            try {
                while ((read = fis.read(buffer)) > 0) {
                    synchronized (dos) {
                        dos.write(buffer, 0, read);
                        totalSent += read;
                    }
                    int progress = (int) ((totalSent * 100) / file.length());
                    updateProgress(progress);
                }
                synchronized (dos) {
                    dos.flush();
                }
                appendHistory("Sent file: " + file.getName() + " (" + file.length() + " bytes)");
            } catch (IOException ex) {
                appendHistory("Error sending file: " + ex.getMessage());
                throw ex;
            } finally {
                try { fis.close(); } catch (Exception ignored) {}
            }
        }

        private void updateProgress(int progress) {
            SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
        }

        void send(String message) {
            try {
                dos.writeUTF(message);
                dos.flush();
            } catch (IOException ex) {
                running = false;
                removeChatClient(clientId);
            }
        }

        void close() throws IOException {
            running = false;
            try {
                if (dis != null) dis.close();
            } catch (Exception ignored) {}
            try {
                if (dos != null) dos.close();
            } catch (Exception ignored) {}
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }
}

