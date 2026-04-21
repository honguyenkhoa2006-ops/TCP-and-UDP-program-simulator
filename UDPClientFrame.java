import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.swing.*;

public class UDPClientFrame extends JFrame {
    private UDPChatPanel chatPanel;
    private UDPFileTransferPanel fileTransferPanel;
    private JButton btnVideoCall;

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
                UDPClientFrame frame = new UDPClientFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public UDPClientFrame() {
        this("");
    }

    public UDPClientFrame(String username) {

        setTitle("UDP Manager - Chat & File Transfer");
        setBounds(100, 100, 1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(950, 600));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // Top panel
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

        JLabel lblStatus = new JLabel("Status: Disconnected");
        lblStatus.setFont(new Font("Times New Roman", Font.BOLD, 11));
        topPanel.add(lblStatus);

        contentPane.add(topPanel, BorderLayout.NORTH);

        // Tabbed pane for different features
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Chat & File Transfer
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);

        chatPanel = new UDPChatPanel(username, txtServer, txtPort, lblStatus, btnStart, btnStop);

        fileTransferPanel = new UDPFileTransferPanel(txtServer, txtPort, chatPanel, this);

        splitPane.setLeftComponent(chatPanel);
        splitPane.setRightComponent(fileTransferPanel);
        tabbedPane.addTab("Chat & File Transfer", splitPane);

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Buttons
        btnStart.addActionListener(e -> {
            chatPanel.startConnection();
            fileTransferPanel.setConnected(true);
            // Note: Socket will be set to gamePanel after connection is established
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnVideoCall.setEnabled(true); // Enable video call button when connected
        });

        btnStop.addActionListener(e -> {
            chatPanel.disconnect1();
            fileTransferPanel.setConnected(false);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            btnVideoCall.setEnabled(false); // Disable video call button when disconnected
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

    // ==================== UDP CHAT PANEL ====================
    static class UDPChatPanel extends JPanel implements Runnable, GameMessageListener {
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

        private DatagramSocket socket;
        private InetAddress serverAddress;
        private int serverPort;
        private volatile boolean connected = false;
        private String username = "";

        // File transfer state
        private volatile boolean receivingFile = false;
        private String currentFileName;
        private long currentFileSize;
        private java.util.Map<Integer, byte[]> filePackets = new java.util.TreeMap<>();

        public UDPChatPanel(String username, JTextField txtServer, JTextField txtPort, JLabel lblStatus,
                JButton btnStart, JButton btnStop) {
            this.txtServer = txtServer;
            this.txtPort = txtPort;
            this.lblStatus = lblStatus;
            this.btnStart = btnStart;
            this.btnStop = btnStop;
            this.username = username == null || username.isEmpty() ? "User" : username;

            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    "Chat (Port: same)",
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP,
                    new Font("Times New Roman", Font.BOLD, 14),
                    Color.BLACK));
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
                    Color.BLACK));

            JPanel roomLeftPanel = new JPanel(new BorderLayout(0, 8));
            roomLeftPanel.setBackground(Color.WHITE);

            JPanel roomNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            roomNamePanel.setBackground(Color.WHITE);
            JLabel lblRoom = new JLabel("Room Name:");
            lblRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
            roomNamePanel.add(lblRoom);
            txtRoomName = new JTextField(15);
            txtRoomName.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            roomNamePanel.add(txtRoomName);

            roomLeftPanel.add(roomNamePanel, BorderLayout.NORTH);

            JPanel currentRoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            currentRoomPanel.setBackground(Color.WHITE);
            lblCurrentRoom = new JLabel("Current Room: None");
            lblCurrentRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
            lblCurrentRoom.setForeground(new Color(0, 100, 0));
            currentRoomPanel.add(lblCurrentRoom);

            roomLeftPanel.add(currentRoomPanel, BorderLayout.SOUTH);

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

        public String getUsername() {
            return username;
        }

        public DatagramSocket getSocket() {
            return socket;
        }

        public InetAddress getServerAddress() {
            return serverAddress;
        }

        public int getServerPort() {
            return serverPort;
        }

        public String getServerAddressString() {
            return txtServer.getText().trim();
        }

        public void startConnection() {
            if (connected) {
                appendChat("UDP socket ready.");
                return;
            }

            String server = txtServer.getText().trim();
            if (server.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter server address!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int port;
            try {
                port = Integer.parseInt(txtPort.getText().trim());

                // Validate port ranges
                if (port < 1 || port > 65535) {
                    JOptionPane.showMessageDialog(this, "Server port must be in range 1-65535!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Port must be an integer!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Move initialization to background thread to prevent UI freezing
            new Thread(() -> {
                try {
                    // Let OS choose an available local port (UDP connectionless, no need to bind to
                    // specific port)
                    socket = new DatagramSocket();
                    socket.setSoTimeout(5000); // 5 second timeout for receiving
                    serverAddress = InetAddress.getByName(server);
                    serverPort = port;

                    String authMsg = "CMD|LOGIN|" + username;
                    byte[] data = authMsg.getBytes("UTF-8");
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                    socket.send(packet);

                    connected = true;
                    int boundPort = socket.getLocalPort();
                    SwingUtilities.invokeLater(() -> {
                        // Disable input fields after successful connection
                        txtServer.setEnabled(false);
                        txtPort.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(true);
                        btnSend.setEnabled(true);

                        lblStatus.setText("Status: UDP READY ▼ (Connectionless, Unreliable)");
                        appendChat("[UDP] ✓ Connected - Listening on port " + boundPort);
                        appendChat("[UDP] Target: " + server + ":" + port);
                    });

                    new Thread(this, "udp-receiver").start();

                } catch (Exception e) {
                    appendChat("Initialization failed: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Status: UDP FAILED ✗");
                        JOptionPane.showMessageDialog(UDPChatPanel.this, "Initialization failed: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }, "udp-init-thread").start();
        }

        private void sendMessage() {
            if (!connected) {
                appendChat("[Error] Failed to connect");
                return;
            }

            if (socket == null || socket.isClosed()) {
                appendChat("[Error] Socket has been closed.");
                connected = false;
                disconnect1();
                return;
            }

            String str = msgInput.getText().trim();
            if (str.isEmpty())
                return;

            try {
                String message = username + ": " + str;
                byte[] data = message.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);

                appendChat(message);
                msgInput.setText("");
            } catch (java.net.SocketException se) {
                appendChat("[Error] Failed to send message: Socket error");
                connected = false;
                disconnect1();
            } catch (java.io.UnsupportedEncodingException uee) {
                appendChat("[Error] Encoding error: UTF-8 not supported");
            } catch (Exception e) {
                appendChat("[Error] Failed to send message: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[65507];
                while (connected && socket != null && !socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        // Kiểm tra lại socket trước khi receive
                        if (socket == null || socket.isClosed()) {
                            break;
                        }

                        socket.receive(packet);
                        byte[] data = packet.getData();
                        int length = packet.getLength();

                        // Try to convert to string first to check for protocol markers
                        String message = null;
                        try {
                            message = new String(data, 0, length, "UTF-8");
                        } catch (Exception e) {
                            // If conversion fails and we're receiving a file, treat as binary file data
                            if (receivingFile) {
                                // Store the binary data as file content
                                int seqNum = filePackets.size();
                                byte[] fileData = new byte[length];
                                System.arraycopy(data, 0, fileData, 0, length);
                                filePackets.put(seqNum, fileData);
                                continue;
                            } else {
                                // Skip if we can't convert and not receiving file
                                continue;
                            }
                        }

                        if (message != null && !message.isEmpty()) {
                            // Handle FILE|SEND_FROM_SERVER message - file from server/other clients
                            if (message.startsWith("FILE|SEND_FROM_SERVER|")) {
                                String[] parts = message.split("\\|", 5);
                                if (parts.length >= 4) {
                                    currentFileName = parts[2];
                                    currentFileSize = Long.parseLong(parts[3]);
                                    String senderUsername = parts.length >= 5 ? parts[4] : "Server";
                                    receivingFile = true;
                                    filePackets.clear();
                                    appendChat("[System] " + senderUsername + " shared file: " + currentFileName + " (" + currentFileSize
                                            + " bytes)");
                                }
                            }
                            // Handle FILE|SEND message - start of file transfer from client
                            else if (message.startsWith("FILE|SEND|")) {
                                String[] parts = message.split("\\|");
                                if (parts.length >= 4) {
                                    currentFileName = parts[2];
                                    currentFileSize = Long.parseLong(parts[3]);
                                    receivingFile = true;
                                    filePackets.clear();
                                    appendChat("[System] Receiving file: " + currentFileName + " (" + currentFileSize
                                            + " bytes)");
                                }
                            }
                            // Handle FILE|END message - end of file transfer
                            else if (message.startsWith("FILE|END")) {
                                if (receivingFile) {
                                    writeReceivedFile();
                                    receivingFile = false;
                                    filePackets.clear();
                                }
                            }
                            // Regular chat message (only if not receiving file or message doesn't match file protocol)
                            else if (!receivingFile) {
                                appendChat(message);
                            } else {
                                // During file transfer, treat unrecognized text as file data
                                byte[] fileData = message.getBytes("UTF-8");
                                int seqNum = filePackets.size();
                                filePackets.put(seqNum, fileData);
                            }
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // Timeout - just continue listening
                        if (connected && !socket.isClosed()) {
                            // Silently ignore timeout, keep listening
                        }
                    } catch (Exception e) {
                        // Other error, continue
                        if (connected && !socket.isClosed()) {
                            // Only log if still connected
                        }
                    }
                }
            } catch (Exception ex) {
                if (connected)
                    appendChat("Disconnected.");
            } finally {
                disconnect1();
            }
        }

        private void writeReceivedFile() {
            try {
                File receivedFile = new File("received_" + currentFileName);
                try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                    for (byte[] packetData : filePackets.values()) {
                        fos.write(packetData);
                    }
                    fos.flush();
                    String filePath = receivedFile.getAbsolutePath();
                    appendChat(
                            "[System] File received: " + receivedFile.getName() + " (" + currentFileSize + " bytes)");
                    appendChat("[System] Saved to: " + filePath);
                }
            } catch (IOException ex) {
                appendChat("[System] Error writing file: " + ex.getMessage());
            }
        }

        private void appendChat(String line) {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(line + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            });
        }

        public void disconnect1() {
            connected = false;
            receivingFile = false;
            filePackets.clear();

            lblStatus.setText("Status: UDP STOPPED");
            btnSend.setEnabled(false);
            msgInput.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                // Re-enable input fields
                txtServer.setEnabled(true);
                txtPort.setEnabled(true);
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                msgInput.setEnabled(true);
            });

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }

            appendChat("[UDP] ✗ Disconnected.");
        }

        private void createRoom() {
            if (!connected) {
                appendChat("Not connected to server.");
                return;
            }
            String roomName = txtRoomName.getText().trim();
            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a room name!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String msg = "CMD|CREATE_ROOM|" + roomName;
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);

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
                JOptionPane.showMessageDialog(this, "Please enter a room name!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String msg = "CMD|JOIN_ROOM|" + roomName;
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);

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
                String msg = "CMD|LEAVE_ROOM|" + currentRoom;
                byte[] data = msg.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);

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

        public void sendGameMessage(String message) {
            if (connected && socket != null && !socket.isClosed()) {
                try {
                    byte[] data = message.getBytes("UTF-8");
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                    socket.send(packet);
                } catch (Exception e) {
                    appendChat("[Error] Failed to send game data: " + e.getMessage());
                }
            }
        }

        public void disconnect() {
            connected = false;
            receivingFile = false;
            filePackets.clear();

            lblStatus.setText("Status: UDP STOPPED");
            btnSend.setEnabled(false);
            msgInput.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                // Re-enable input fields
                txtServer.setEnabled(true);
                txtPort.setEnabled(true);
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                msgInput.setEnabled(true);
            });

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }

            appendChat("[UDP] ✗ Disconnected.");
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

            // Emoji list
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
    }

    // ==================== UDP FILE TRANSFER PANEL ====================
    static class UDPFileTransferPanel extends JPanel {

        private JTextArea transferHistory;
        private JProgressBar progressBar;
        private JButton btnBrowse;
        private JButton btnSend;
        private JTextField txtSelectedFile;
        private File selectedFile = null;

        private UDPChatPanel chatPanel;
        private volatile boolean connected = false;

        public UDPFileTransferPanel(JTextField txtServer, JTextField txtPort, UDPChatPanel chatPanel,
                UDPClientFrame udpClientFrame) {
            this.chatPanel = chatPanel;
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    "File Transfer (Same Port)",
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP,
                    new Font("Times New Roman", Font.BOLD, 14),
                    Color.BLACK));
            setBackground(Color.WHITE);

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

            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            filePanel.setBackground(Color.WHITE);

            btnSend = new JButton("> Send");
            btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
            btnSend.setEnabled(false);
            btnSend.addActionListener(e -> sendFile());
            filePanel.add(btnSend);

            bottomPanel.add(filePanel, BorderLayout.CENTER);

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
                    appendHistory("📄 Selected: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this, "File not found!", "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    selectedFile = null;
                    txtSelectedFile.setText("");
                    btnSend.setEnabled(false);
                }
            }
        }

        private void sendFile() {
            if (!connected || !chatPanel.isConnected()) {
                appendHistory("Not connected!");
                return;
            }

            if (selectedFile == null || !selectedFile.exists()) {
                appendHistory("Please select a file first!");
                return;
            }

            appendHistory(
                    "Starting UDP file transfer: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");

            new Thread(() -> {
                try {
                    DatagramSocket socket = chatPanel.getSocket();
                    InetAddress serverAddr = chatPanel.getServerAddress();
                    int serverPort = chatPanel.getServerPort();

                    if (socket == null || socket.isClosed()) {
                        appendHistory("Error: Socket not available");
                        return;
                    }

                    synchronized (socket) {
                        // Send file command (similar to TCP: FILE|SEND|filename|size)
                        String fileCommand = "FILE|SEND|" + selectedFile.getName() + "|" + selectedFile.length();
                        byte[] commandData = fileCommand.getBytes("UTF-8");
                        DatagramPacket commandPacket = new DatagramPacket(commandData, commandData.length, serverAddr,
                                serverPort);
                        socket.send(commandPacket);

                        // Send file content with larger buffer (4096 bytes like TCP)
                        FileInputStream fis = new FileInputStream(selectedFile);
                        byte[] buffer = new byte[4096];
                        int read;
                        long totalSent = 0;

                        try {
                            while ((read = fis.read(buffer)) > 0) {
                                DatagramPacket packet = new DatagramPacket(buffer, read, serverAddr, serverPort);
                                socket.send(packet);
                                totalSent += read;
                                int progress = (int) ((totalSent * 100) / selectedFile.length());
                                updateProgress(progress);
                            }
                        } finally {
                            fis.close();
                        }

                        // Small delay to ensure last packet is received before end marker
                        Thread.sleep(100);
                        
                        // Send end marker
                        String endMsg = "FILE|END";
                        byte[] endData = endMsg.getBytes("UTF-8");
                        DatagramPacket endPacket = new DatagramPacket(endData, endData.length, serverAddr, serverPort);
                        socket.send(endPacket);
                    }

                    appendHistory("File sent successfully!");
                    updateProgress(0);
                } catch (Exception ex) {
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
}
