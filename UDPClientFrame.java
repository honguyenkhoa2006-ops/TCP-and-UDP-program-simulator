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
        setTitle("UDP Manager - Chat & File Transfer (Single Port)");
        setBounds(100, 100, 1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(900, 600));

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // Top panel
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

        JLabel lblLocalPort = new JLabel("Local Port:");
        lblLocalPort.setFont(new Font("Times New Roman", Font.BOLD, 12));
        topPanel.add(lblLocalPort);

        JTextField txtLocalPort = new JTextField(8);
        txtLocalPort.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        topPanel.add(txtLocalPort);

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

        chatPanel = new UDPChatPanel(username, txtServer, txtPort, txtLocalPort, lblStatus, btnStart, btnStop);
        fileTransferPanel = new UDPFileTransferPanel(txtServer, txtPort, txtLocalPort, chatPanel);

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

// ==================== UDP CHAT PANEL ====================
class UDPChatPanel extends JPanel implements Runnable {
    private JTextField txtServer;
    private JTextField txtPort;
    private JTextField txtLocalPort;
    private JTextArea chatArea;
    private JTextField msgInput;
    private JButton btnSend;
    private JLabel lblStatus;
    private JButton btnStart;
    private JButton btnStop;

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
    
    public UDPChatPanel(String username, JTextField txtServer, JTextField txtPort, JTextField txtLocalPort, JLabel lblStatus, JButton btnStart, JButton btnStop) {
        this.txtServer = txtServer;
        this.txtPort = txtPort;
        this.txtLocalPort = txtLocalPort;
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

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public boolean isConnected() {
        return connected;
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

    public void startConnection() {
        if (connected) {
            appendChat("UDP socket ready.");
            return;
        }

        String server = txtServer.getText().trim();
        if (server.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập server address!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int port, localPort;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
            localPort = Integer.parseInt(txtLocalPort.getText().trim());
            
            // Validate port ranges
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Server port phải nằm trong khoảng 1-65535!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (localPort < 1 || localPort > 65535) {
                JOptionPane.showMessageDialog(this, "Local port phải nằm trong khoảng 1-65535!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port phải là số nguyên!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Move initialization to background thread to prevent UI freezing
        new Thread(() -> {
            try {
                // Try to create socket with specified local port
                socket = new DatagramSocket(localPort);
                socket.setSoTimeout(5000); // 5 second timeout for receiving
                serverAddress = InetAddress.getByName(server);
                serverPort = port;

                String authMsg = "CMD|LOGIN|" + username + "|";
                byte[] data = authMsg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);

                connected = true;
                SwingUtilities.invokeLater(() -> {
                    // Disable input fields after successful connection
                    txtServer.setEnabled(false);
                    txtPort.setEnabled(false);
                    txtLocalPort.setEnabled(false);
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    btnSend.setEnabled(true);
                    
                    lblStatus.setText("Status: UDP READY ▼ (Connectionless, Unreliable)");
                    appendChat("[UDP] ✓ Kết nối thành công - Listening on port " + localPort);
                    appendChat("[UDP] Target: " + server + ":" + port);
                });

                new Thread(this, "udp-receiver").start();

            } catch (java.net.BindException be) {
                appendChat("Local port " + localPort + " đang được sử dụng!");
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Status: UDP FAILED ✗ (Port đang dùng)");
                    JOptionPane.showMessageDialog(UDPChatPanel.this, "Local port " + localPort + " đang được sử dụng!\nVui lòng chọn port khác.", "Error", JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception e) {
                appendChat("Khởi tạo thất bại: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Status: UDP FAILED ✗");
                    JOptionPane.showMessageDialog(UDPChatPanel.this, "Khởi tạo thất bại: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "udp-init-thread").start();
    }

    private void sendMessage() {
        if (!connected) {
            appendChat("[Lỗi] Chưa kết nối đến server.");
            return;
        }
        
        if (socket == null || socket.isClosed()) {
            appendChat("[Lỗi] Socket đã bị đóng.");
            connected = false;
            disconnect();
            return;
        }

        String str = msgInput.getText().trim();
        if (str.isEmpty()) return;

        try {
            String message = username + ": " + str;
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);

            appendChat(message);
            msgInput.setText("");
        } catch (java.net.SocketException se) {
            appendChat("[Lỗi] Gửi tin nhắn thất bại: Socket error");
            connected = false;
            disconnect();
        } catch (Exception e) {
            appendChat("[Lỗi] Gửi tin nhắn thất bại: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
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
                    
                    // Handle file data packets (if we're receiving a file and packet has 4+ bytes)
                    if (receivingFile && length > 4) {
                        // Check if this looks like a file data packet (first 4 bytes could be sequence number)
                        // Or if it looks like a text message starting with known markers
                        String[] textStartMarkers = {"FILEINFO|", "END|"};
                        boolean isTextMessage = false;
                        
                        try {
                            String testMessage = new String(data, 0, Math.min(20, length));
                            for (String marker : textStartMarkers) {
                                if (testMessage.startsWith(marker)) {
                                    isTextMessage = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            // If we can't convert to string safely, it's probably binary data
                            isTextMessage = false;
                        }
                        
                        if (!isTextMessage && length > 4) {
                            // Treat as binary file data packet
                            int seqNum = ((data[0] & 0xFF) << 24) | 
                                        ((data[1] & 0xFF) << 16) |
                                        ((data[2] & 0xFF) << 8) |
                                        (data[3] & 0xFF);
                            int dataLen = length - 4;
                            byte[] fileData = new byte[dataLen];
                            System.arraycopy(data, 4, fileData, 0, dataLen);
                            filePackets.put(seqNum, fileData);
                            continue;
                        }
                    }
                    
                    // Try to convert to string for text messages
                    String message = null;
                    try {
                        message = new String(data, 0, length);
                    } catch (Exception e) {
                        // If conversion fails, skip this packet
                        continue;
                    }
                    
                    if (message != null && !message.isEmpty()) {
                        // Handle FILEINFO message - start of file transfer
                        if (message.startsWith("FILEINFO|")) {
                            String[] parts = message.split("\\|");
                            if (parts.length >= 3) {
                                currentFileName = parts[1];
                                currentFileSize = Long.parseLong(parts[2]);
                                receivingFile = true;
                                filePackets.clear();
                                appendChat("[System] Receiving file: " + currentFileName + " (" + currentFileSize + " bytes)");
                            }
                        }
                        // Handle END message - end of file transfer
                        else if (message.startsWith("END|")) {
                            if (receivingFile) {
                                writeReceivedFile();
                                receivingFile = false;
                                filePackets.clear();
                            }
                        }
                        // Regular chat message
                        else if (!receivingFile) {
                            appendChat(message);
                        }
                    }
                } catch (Exception e) {
                    // Timeout or other error, continue
                    if (connected && !socket.isClosed()) {
                        // Only log if still connected
                    }
                }
            }
        } catch (Exception ex) {
            if (connected) appendChat("Disconnected.");
        } finally {
            disconnect();
        }
    }

    private void writeReceivedFile() {
        try {
            File receivedFile = new File("received_" + currentFileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            
            try {
                for (byte[] packetData : filePackets.values()) {
                    fos.write(packetData);
                }
                fos.flush();
                appendChat("[System] File received: " + receivedFile.getName() + " (" + currentFileSize + " bytes)");
            } finally {
                fos.close();
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
            txtLocalPort.setEnabled(true);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            msgInput.setEnabled(true);
        });
        
        try { 
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
        
        appendChat("[UDP] ✗ Ngắt kết nối từ server");
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
}

// ==================== UDP FILE TRANSFER PANEL ====================
class UDPFileTransferPanel extends JPanel {

    private JTextArea transferHistory;
    private JProgressBar progressBar;
    private JButton btnSend;
    private JButton btnReceive;

    private UDPChatPanel chatPanel;
    private volatile boolean connected = false;

    public UDPFileTransferPanel(JTextField txtServer, JTextField txtPort, JTextField txtLocalPort, UDPChatPanel chatPanel) {
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

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filePanel.setBackground(Color.WHITE);

        btnSend = new JButton("Send");
        btnSend.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> sendFile());
        filePanel.add(btnSend);

        btnReceive = new JButton("Receive");
        btnReceive.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnReceive.setEnabled(false);
        btnReceive.addActionListener(e -> appendHistory("UDP file receive: Waiting for incoming file..."));
        filePanel.add(btnReceive);

        bottomPanel.add(filePanel, BorderLayout.NORTH);

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
        btnReceive.setEnabled(connected);
    }



    private void sendFile() {
        if (!connected || !chatPanel.isConnected()) {
            appendHistory("Not connected!");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File file = fileChooser.getSelectedFile();
        if (!file.exists()) {
            appendHistory("File not found: " + file.getAbsolutePath());
            return;
        }

        appendHistory("Starting UDP file transfer: " + file.getName() + " (" + file.length() + " bytes)");
        
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
                    // Send file info
                    String fileInfo = "FILEINFO|" + file.getName() + "|" + file.length();
                    byte[] infoData = fileInfo.getBytes();
                    DatagramPacket infoPacket = new DatagramPacket(infoData, infoData.length, serverAddr, serverPort);
                    socket.send(infoPacket);

                    // Send file data
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[512];
                    int packetNum = 0;
                    int read;
                    long totalSent = 0;

                    try {
                        while ((read = fis.read(buffer)) > 0) {
                            byte[] packetData = new byte[read + 4];
                            packetData[0] = (byte) ((packetNum >> 24) & 0xFF);
                            packetData[1] = (byte) ((packetNum >> 16) & 0xFF);
                            packetData[2] = (byte) ((packetNum >> 8) & 0xFF);
                            packetData[3] = (byte) (packetNum & 0xFF);
                            System.arraycopy(buffer, 0, packetData, 4, read);

                            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddr, serverPort);
                            socket.send(packet);
                            
                            totalSent += read;
                            if (packetNum % 10 == 0) {
                                int progress = (int) ((totalSent * 100) / file.length());
                                updateProgress(progress);
                            }
                            packetNum++;
                        }
                    } finally {
                        fis.close();
                    }

                    // Send end marker
                    String endMsg = "END|";
                    byte[] endData = endMsg.getBytes();
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

