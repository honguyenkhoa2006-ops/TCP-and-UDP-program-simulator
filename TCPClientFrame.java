import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class TCPClientFrame extends JFrame {
    private TCPChatPanel chatPanel;
    private TCPFileTransferPanel fileTransferPanel;

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
                TCPClientFrame frame = new TCPClientFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public TCPClientFrame() {
        this("");
    }

    public TCPClientFrame(String username) {
        setTitle("TCP Manager - Chat & File Transfer (Single Port)");
        setBounds(100, 100, 1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(900, 600));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(Color.WHITE);

        JLabel lblServer = new JLabel("Server (IP/Hostname):");
        lblServer.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblServer);

        JTextField txtServer = new JTextField(8);
        txtServer.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtServer);

        JLabel lblPort = new JLabel("Port:");
        lblPort.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblPort);

        JTextField txtPort = new JTextField(8);
        txtPort.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtPort);

        JButton btnStart = new JButton("Start");
        btnStart.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(btnStart);

        JButton btnStop = new JButton("Stop");
        btnStop.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnStop.setEnabled(false);
        topPanel.add(btnStop);

        JLabel lblStatus = new JLabel("Status: Disconnected");
        lblStatus.setFont(new Font("Times New Roman", Font.BOLD, 11));
        topPanel.add(lblStatus);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);

        chatPanel = new TCPChatPanel(username, txtServer, txtPort, lblStatus, btnStart, btnStop);
        fileTransferPanel = new TCPFileTransferPanel(txtServer, txtPort, chatPanel);

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

        JButton btnBack = new JButton("Back");
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

// ==================== TCP CHAT PANEL ====================
class TCPChatPanel extends JPanel implements Runnable {
    private JTextField txtServer;
    private JTextField txtPort;
    private JTextArea chatArea;
    private JTextField msgInput;
    private JButton btnSend;
    private JLabel lblStatus;
    private JButton btnStart;
    private JButton btnStop;
    
    // Room functionality
    private JTextField txtRoomName;
    private JButton btnCreateRoom;
    private JButton btnJoinRoom;
    private JButton btnLeaveRoom;
    private JLabel lblCurrentRoom;
    private String currentRoom = "";

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private volatile boolean connected = false;
    private String username = "";

    public TCPChatPanel(String username, JTextField txtServer, JTextField txtPort, JLabel lblStatus, JButton btnStart, JButton btnStop) {
        this.txtServer = txtServer;
        this.txtPort = txtPort;
        this.lblStatus = lblStatus;
        this.btnStart = btnStart;
        this.btnStop = btnStop;
        this.username = username.isEmpty() ? "User" : username;

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

        btnSend = new JButton("Send");
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
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        roomPanel.setBackground(Color.WHITE);
        roomPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            "Room",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Times New Roman", Font.BOLD, 11),
            Color.BLACK
        ));
        
        JLabel lblRoom = new JLabel("Room Name:");
        lblRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        roomPanel.add(lblRoom);
        
        txtRoomName = new JTextField(10);
        txtRoomName.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        roomPanel.add(txtRoomName);
        
        btnCreateRoom = new JButton("Create Room");
        btnCreateRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnCreateRoom.addActionListener(e -> createRoom());
        roomPanel.add(btnCreateRoom);
        
        btnJoinRoom = new JButton("Join Room");
        btnJoinRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnJoinRoom.addActionListener(e -> joinRoom());
        roomPanel.add(btnJoinRoom);
        
        btnLeaveRoom = new JButton("Leave Room");
        btnLeaveRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnLeaveRoom.addActionListener(e -> leaveRoom());
        roomPanel.add(btnLeaveRoom);
        
        lblCurrentRoom = new JLabel("Current Room: None");
        lblCurrentRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        lblCurrentRoom.setForeground(new Color(0, 100, 0));
        roomPanel.add(lblCurrentRoom);
        
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
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Move connection to background thread to prevent UI freezing
        new Thread(() -> {
            try {
                socket = new Socket(server, port);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

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
        JButton emojiPickerBtn = new JButton("▼");
        emojiPickerBtn.setFont(new Font("Arial", Font.BOLD, 14));
        emojiPickerBtn.setPreferredSize(new Dimension(40, 35));
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
            "☺", "❤", "✓", "✔","✉", "✍", "☑", "☒"
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
                    } else {
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
            appendChat("[System] File received: " + receivedFile.getName() + " (" + fileSize + " bytes)");
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
    private JButton btnSend;
    
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

        // Buttons
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filePanel.setBackground(Color.WHITE);

        btnSend = new JButton("Send");
        btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> sendFile());
        filePanel.add(btnSend);

        bottomPanel.add(filePanel, BorderLayout.NORTH);

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

        bottomPanel.add(progressPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        btnSend.setEnabled(connected);
    }



    private void sendFile() {
        if (!connected || !chatPanel.isConnected()) {
            JOptionPane.showMessageDialog(this, "Not connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File file = fileChooser.getSelectedFile();
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
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
                    out.writeUTF("FILE|SEND|" + file.getName() + "|" + file.length());
                    out.flush();
                }

                // Send file content
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                long totalSent = 0;
                int read;

                try {
                    while ((read = fis.read(buffer)) > 0) {
                        synchronized (out) {
                            out.write(buffer, 0, read);
                            totalSent += read;
                        }
                        int progress = (int) ((totalSent * 100) / file.length());
                        updateProgress(progress);
                    }
                    synchronized (out) {
                        out.flush();
                    }
                } finally {
                    fis.close();
                }

                appendHistory("File sent: " + file.getName() + " (" + file.length() + " bytes)");
                updateProgress(0);
            } catch (IOException ex) {
                appendHistory("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void receiveFile() {
        if (!connected || !chatPanel.isConnected()) {
            JOptionPane.showMessageDialog(this, "Not connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String filePath = JOptionPane.showInputDialog(this, "Enter filename to request:", "");
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                DataOutputStream out = chatPanel.getDataOutputStream();
                DataInputStream in = chatPanel.getDataInputStream();

                if (out == null || in == null) {
                    appendHistory("Error: Connection not properly initialized");
                    return;
                }

                // Request file
                synchronized (out) {
                    out.writeUTF("FILE|GET|" + filePath);
                    out.flush();
                }

                // Read response header
                String response;
                long fileSize = 0;
                synchronized (in) {
                    response = in.readUTF();
                    if (response != null && response.startsWith("FILE|FOUND")) {
                        fileSize = in.readLong();
                    }
                }

                // Check response
                if (response == null || response.startsWith("FILE|NOT_FOUND")) {
                    appendHistory("File not found on server!");
                    return;
                }

                if (!response.startsWith("FILE|FOUND")) {
                    appendHistory("Invalid response: " + response);
                    return;
                }

                // Receive file
                File receivedFile = new File("received_" + new File(filePath).getName());
                FileOutputStream fos = new FileOutputStream(receivedFile);

                byte[] buffer = new byte[4096];
                long totalReceived = 0;
                int read;

                try {
                    while (totalReceived < fileSize && (read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalReceived))) > 0) {
                        fos.write(buffer, 0, read);
                        totalReceived += read;
                        int progress = (int) ((totalReceived * 100) / fileSize);
                        updateProgress(progress);
                    }
                } finally {
                    fos.close();
                }
                
                appendHistory("File received: " + receivedFile.getName() + " (" + fileSize + " bytes)");
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

