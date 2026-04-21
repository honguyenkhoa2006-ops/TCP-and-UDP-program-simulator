import java.awt.*;
import java.awt.event.*;
import java.util.UUID;
import javax.swing.*;

/**
 * XO Game Panel for TCP and UDP
 * Handles XO game UI and communication (simplified version)
 */
public class XOGamePanel extends JPanel {
    private XOGame game;
    private JLabel lblStatus;
    private JButton btnCreateMatch;
    private JButton btnJoinGame;
    private JButton btnCopyId;
    private JButton btnReplayGame;
    private JButton btnLeaveMatch;
    private JTextArea gameLog;
    private JLabel lblGameId;
    private JTextField txtGameId;
    private JSpinner spinBoardSize;

    private int cellSize = 40;
    private int boardMarginX = 20;
    private int boardMarginY = 20;

    private String playerName = "";
    private String playerSymbol = "X";
    private boolean isMyTurn = false;

    private boolean gameInProgress = false;
    private String gameId;
    private GameMessageListener chatPanel;

    public XOGamePanel(String playerName, GameMessageListener chatPanel) {
        this.playerName = playerName;
        this.chatPanel = chatPanel;
        this.gameId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBackground(Color.WHITE);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        statusPanel.setBackground(Color.WHITE);

        JLabel lblPlayerInfo = new JLabel("Player: " + playerName);
        lblPlayerInfo.setFont(new Font("Times New Roman", Font.BOLD, 12));
        statusPanel.add(lblPlayerInfo);

        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        idPanel.setBackground(Color.WHITE);
        lblGameId = new JLabel("Match ID: " + gameId);
        lblGameId.setFont(new Font("Times New Roman", Font.BOLD, 11));
        lblGameId.setForeground(new Color(0, 100, 200));
        idPanel.add(lblGameId);

        btnCopyId = new JButton("Copy");
        btnCopyId.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        btnCopyId.setPreferredSize(new Dimension(65, 20));
        btnCopyId.addActionListener(e -> copyGameIdToClipboard());
        idPanel.add(btnCopyId);
        statusPanel.add(idPanel);

        lblStatus = new JLabel("Ready to create or join match");
        lblStatus.setFont(new Font("Times New Roman", Font.BOLD, 12));
        lblStatus.setForeground(Color.BLUE);
        statusPanel.add(lblStatus);
        topPanel.add(statusPanel, BorderLayout.WEST);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        controlPanel.setBackground(Color.WHITE);

        btnCreateMatch = new JButton("+ Create Match");
        btnCreateMatch.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnCreateMatch.addActionListener(e -> showCreateMatchDialog());
        controlPanel.add(btnCreateMatch);

        btnReplayGame = new JButton("Replay");
        btnReplayGame.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnReplayGame.setEnabled(false);
        btnReplayGame.addActionListener(e -> replayGame());
        controlPanel.add(btnReplayGame);

        btnLeaveMatch = new JButton("Leave");
        btnLeaveMatch.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnLeaveMatch.setEnabled(false);
        btnLeaveMatch.addActionListener(e -> leaveMatch());
        controlPanel.add(btnLeaveMatch);
        topPanel.add(controlPanel, BorderLayout.EAST);

        // Join game panel
        JPanel joinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        joinPanel.setBackground(new Color(240, 240, 240));
        joinPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY));

        JLabel lblJoin = new JLabel("Join Match ID:");
        lblJoin.setFont(new Font("Times New Roman", Font.BOLD, 11));
        joinPanel.add(lblJoin);

        txtGameId = new JTextField(10);
        txtGameId.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        txtGameId.setPreferredSize(new Dimension(110, 25));
        joinPanel.add(txtGameId);

        btnJoinGame = new JButton("Join");
        btnJoinGame.setFont(new Font("Times New Roman", Font.BOLD, 11));
        btnJoinGame.setPreferredSize(new Dimension(80, 25));
        btnJoinGame.addActionListener(e -> joinExistingGame());
        joinPanel.add(btnJoinGame);

        JPanel topFullPanel = new JPanel(new BorderLayout());
        topFullPanel.add(topPanel, BorderLayout.NORTH);
        topFullPanel.add(joinPanel, BorderLayout.SOUTH);
        add(topFullPanel, BorderLayout.NORTH);

        // Center: board + log
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerLocation(500);
        centerSplit.setResizeWeight(0.7);
        centerSplit.setContinuousLayout(true);

        JPanel boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGameBoard(g);
            }
        };
        boardPanel.setBackground(Color.WHITE);
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameInProgress && isMyTurn && game != null && !game.isGameOver()) {
                    handleBoardClick(e.getX(), e.getY());
                }
            }
        });
        centerSplit.setLeftComponent(boardPanel);

        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBackground(Color.WHITE);
        logPanel.setBorder(BorderFactory.createTitledBorder("Game Log"));

        gameLog = new JTextArea();
        gameLog.setFont(new Font("Courier New", Font.PLAIN, 11));
        gameLog.setEditable(false);
        gameLog.setLineWrap(true);
        gameLog.setWrapStyleWord(true);

        JScrollPane logScroll = new JScrollPane(gameLog);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(logScroll, BorderLayout.CENTER);
        centerSplit.setRightComponent(logPanel);

        add(centerSplit, BorderLayout.CENTER);

        initializeGame(15); // Default 15x15
        addLogMessage("Ready to create or join a match.");
    }

    private void showCreateMatchDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Create Match");
        dialog.setModal(true);
        dialog.setSize(400, 220);
        dialog.setLayout(null);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));

        JLabel lblTitle = new JLabel("Create New Match");
        lblTitle.setFont(new Font("Times New Roman", Font.BOLD, 14));
        lblTitle.setBounds(20, 15, 360, 25);
        dialog.add(lblTitle);

        JLabel lblBoardSize = new JLabel("Board Size (3-21):");
        lblBoardSize.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        lblBoardSize.setBounds(20, 55, 150, 25);
        dialog.add(lblBoardSize);

        spinBoardSize = new JSpinner(new SpinnerNumberModel(15, 3, 21, 1));
        spinBoardSize.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        spinBoardSize.setBounds(175, 55, 60, 25);
        dialog.add(spinBoardSize);

        JLabel lblInfo = new JLabel("Win condition: 5 in a row (3-4 board: full row)");
        lblInfo.setFont(new Font("Times New Roman", Font.ITALIC, 10));
        lblInfo.setForeground(Color.GRAY);
        lblInfo.setBounds(20, 85, 360, 20);
        dialog.add(lblInfo);

        JButton btnCreate = new JButton("Create Match");
        btnCreate.setBounds(80, 155, 130, 30);
        btnCreate.addActionListener(e -> {
            int boardSize = (Integer) spinBoardSize.getValue();
            createNewMatch(boardSize);
            dialog.dispose();
        });
        dialog.add(btnCreate);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBounds(225, 155, 100, 30);
        btnCancel.addActionListener(e -> dialog.dispose());
        dialog.add(btnCancel);

        dialog.setVisible(true);
    }

    private void createNewMatch(int boardSize) {
        if (gameInProgress) return;

        gameId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        lblGameId.setText("Match ID: " + gameId);

        gameInProgress = true;
        isMyTurn = true;
        playerSymbol = "X";
        initializeGame(boardSize);
        updateGameStatus();

        addLogMessage("=== MATCH CREATED ===");
        addLogMessage("Match ID: " + gameId + " | Board: " + boardSize + "x" + boardSize);
        addLogMessage("Share your Match ID. Waiting for opponent to join...");

        btnCreateMatch.setEnabled(false);
        btnJoinGame.setEnabled(false);
        txtGameId.setEnabled(false);
        btnLeaveMatch.setEnabled(true);

        repaint();
    }

    private void joinExistingGame() {
        String inputId = txtGameId.getText().trim().toUpperCase();
        if (inputId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Match ID!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        gameId = inputId;
        lblGameId.setText("Match ID: " + gameId + " (joining...)");

        playerSymbol = "O";
        isMyTurn = false;

        addLogMessage("Sending join request for Match ID: " + gameId + "...");

        btnJoinGame.setEnabled(false);
        btnCreateMatch.setEnabled(false);
        txtGameId.setEnabled(false);
        btnLeaveMatch.setEnabled(true);

        sendGameJoinMessage();
        updateGameStatus();
    }

    private void replayGame() {
        if (gameInProgress && game != null && game.isGameOver()) {
            game.reset();
            isMyTurn = (playerSymbol.equals("X"));
            updateGameStatus();
            addLogMessage("=== REMATCH STARTED ===");
            btnReplayGame.setEnabled(false);

            if (chatPanel != null) {
                chatPanel.sendGameMessage("GAME|REPLAY|" + gameId);
            }
            repaint();
        }
    }

    private void leaveMatch() {
        if (chatPanel != null) {
            chatPanel.sendGameMessage("GAME|LEAVE|" + gameId);
        }
        SwingUtilities.invokeLater(this::resetMatchState);
    }

    private void resetMatchState() {
        gameInProgress = false;
        if (game != null) game.reset();
        playerSymbol = "X";
        isMyTurn = false;
        gameId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        lblGameId.setText("Match ID: " + gameId);

        updateGameStatus();
        addLogMessage("Left match. Ready to create or join.");

        btnCreateMatch.setEnabled(true);
        btnJoinGame.setEnabled(true);
        txtGameId.setEnabled(true);
        txtGameId.setText("");
        btnReplayGame.setEnabled(false);
        btnLeaveMatch.setEnabled(false);
        repaint();
    }

    private void initializeGame(int boardSize) {
        game = new XOGame(boardSize);
        cellSize = Math.max(10, Math.min(450 / boardSize, 40));
        repaint();
    }

    private void handleBoardClick(int x, int y) {
        if (game == null) return;
        int row = (y - boardMarginY) / cellSize;
        int col = (x - boardMarginX) / cellSize;

        if (row < 0 || row >= game.getBoardSize() || col < 0 || col >= game.getBoardSize())
            return;

        if (game.makeMove(row, col)) {
            isMyTurn = false;
            addLogMessage("You played: " + playerSymbol + " at (" + row + ", " + col + ")");
            sendGameMove(row, col);

            updateGameStatus();
            repaint();
            checkGameOver();
        }
    }

    public void handleOpponentMove(int row, int col, String symbol) {
        if (!gameInProgress || game == null) return;

        SwingUtilities.invokeLater(() -> {
            if (game.isGameOver()) return;
            String[][] board = game.getBoard();
            if (row < 0 || row >= game.getBoardSize() || col < 0 || col >= game.getBoardSize()) return;
            if (!board[row][col].equals("")) return;

            if (game.getCurrentPlayer().equals(symbol) && game.makeMove(row, col)) {
                isMyTurn = true;
                addLogMessage("Opponent played: " + symbol + " at (" + row + ", " + col + ")");
                updateGameStatus();
                repaint();
                checkGameOver();
            }
        });
    }

    private void checkGameOver() {
        if (game == null || !game.isGameOver()) return;
        addLogMessage("");
        if (game.getWinner().equals("")) {
            addLogMessage("GAME OVER: It's a Draw!");
        } else {
            boolean iWon = game.getWinner().equals(playerSymbol);
            addLogMessage("GAME OVER: " + (iWon ? "You WIN! (" : "You LOSE! (") + game.getWinner() + " wins)");
        }
        btnReplayGame.setEnabled(true);
    }

    private void sendGameMove(int row, int col) {
        if (chatPanel != null) {
            chatPanel.sendGameMessage("GAME|MOVE|" + gameId + "|" + row + "|" + col + "|" + playerSymbol);
        }
    }

    private void sendGameInitMessage() {
        if (chatPanel != null && game != null) {
            chatPanel.sendGameMessage(
                    "GAME|INIT|" + gameId + "|" + game.getBoardSize() + "|" + playerName);
        }
    }

    private void sendGameJoinMessage() {
        if (chatPanel != null) {
            chatPanel.sendGameMessage("GAME|JOIN|" + gameId + "|" + playerName);
        }
    }

    public void acceptGameInvitation(String receivedGameId, int boardSize, String opponentName) {
        if (!this.gameId.equals(receivedGameId)) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            initializeGame(boardSize);
            playerSymbol = "O";
            gameInProgress = true;
            isMyTurn = false;

            lblGameId.setText("Match ID: " + gameId + " (JOINED)");
            addLogMessage("=== MATCH STARTED ===");
            addLogMessage("Opponent: " + opponentName + " | Board: " + boardSize + "x" + boardSize);
            addLogMessage("You are O. Waiting for opponent's first move...");

            btnCreateMatch.setEnabled(false);
            btnJoinGame.setEnabled(false);
            txtGameId.setEnabled(false);
            btnLeaveMatch.setEnabled(true);

            updateGameStatus();
            repaint();
        });
    }

    public void handleOpponentJoin(String receivedGameId, String opponentName) {
        if (!gameInProgress || !this.gameId.equals(receivedGameId)) return;

        sendGameInitMessage();

        SwingUtilities.invokeLater(() -> {
            addLogMessage("=== " + opponentName + " JOINED ===");
            addLogMessage("You are X. Your turn first!");
            isMyTurn = true;
            updateGameStatus();
        });
    }

    public void handleOpponentLeave(String receivedGameId) {
        if (!this.gameId.equals(receivedGameId)) return;
        SwingUtilities.invokeLater(() -> {
            addLogMessage("Opponent left the match.");
            JOptionPane.showMessageDialog(this, "Opponent left the match.", "Match Ended",
                    JOptionPane.INFORMATION_MESSAGE);
            resetMatchState();
        });
    }

    public void handleOpponentReplay(String receivedGameId) {
        if (!this.gameId.equals(receivedGameId) || game == null) return;
        SwingUtilities.invokeLater(() -> {
            if (game.isGameOver()) {
                game.reset();
                isMyTurn = playerSymbol.equals("O");
                updateGameStatus();
                addLogMessage("Opponent requested rematch. Your turn: " + (isMyTurn ? "YES" : "NO"));
                btnReplayGame.setEnabled(false);
                repaint();
            }
        });
    }

    private void updateGameStatus() {
        if (!gameInProgress) {
            lblStatus.setText("Ready to create or join");
            lblStatus.setForeground(Color.DARK_GRAY);
        } else if (game != null && game.isGameOver()) {
            lblStatus.setText(game.getWinner().equals("") ? "Draw!" : "Player " + game.getWinner() + " Wins!");
            lblStatus.setForeground(new Color(200, 0, 0));
        } else if (isMyTurn) {
            lblStatus.setText("Your turn (" + playerSymbol + ")");
            lblStatus.setForeground(new Color(0, 150, 0));
        } else {
            lblStatus.setText("Opponent's turn (" + (playerSymbol.equals("X") ? "O" : "X") + ")");
            lblStatus.setForeground(new Color(0, 0, 200));
        }
    }

    private void addLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
            String time = sdf.format(new java.util.Date());
            gameLog.append("[" + time + "] " + message + "\n");
            gameLog.setCaretPosition(gameLog.getDocument().getLength());
        });
    }

    private void drawGameBoard(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (game == null) return;
        int boardSize = game.getBoardSize();
        int totalWidth  = boardSize * cellSize;
        int totalHeight = boardSize * cellSize;

        // Draw board background
        g2d.setColor(new Color(255, 250, 230));
        g2d.fillRect(boardMarginX, boardMarginY, totalWidth, totalHeight);

        // Draw grid lines
        g2d.setColor(new Color(100, 80, 60));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i <= boardSize; i++) {
            g2d.drawLine(boardMarginX, boardMarginY + i * cellSize,
                         boardMarginX + totalWidth, boardMarginY + i * cellSize);
            g2d.drawLine(boardMarginX + i * cellSize, boardMarginY,
                         boardMarginX + i * cellSize, boardMarginY + totalHeight);
        }

        // Draw pieces
        String[][] board = game.getBoard();
        int fontSize = Math.max(8, cellSize - 8);
        g2d.setFont(new Font("Times New Roman", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                String piece = board[i][j];
                if (!piece.equals("")) {
                    int cx = boardMarginX + j * cellSize + cellSize / 2;
                    int cy = boardMarginY + i * cellSize + cellSize / 2;
                    int tw = fm.stringWidth(piece);
                    int th = fm.getAscent();

                    g2d.setColor(piece.equals("X") ? new Color(30, 80, 200) : new Color(200, 30, 30));
                    g2d.drawString(piece, cx - tw / 2, cy + th / 2 - 2);
                }
            }
        }

        // Highlight whose turn (border glow when it's my turn)
        if (gameInProgress && isMyTurn && (game == null || !game.isGameOver())) {
            g2d.setColor(new Color(0, 200, 0, 80));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect(boardMarginX, boardMarginY, totalWidth, totalHeight);
        }
    }

    private void copyGameIdToClipboard() {
        try {
            java.awt.datatransfer.StringSelection selection =
                    new java.awt.datatransfer.StringSelection(gameId);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            btnCopyId.setText("Copied!");
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    SwingUtilities.invokeLater(() -> btnCopyId.setText("Copy"));
                } catch (Exception ex) { /* ignored */ }
            }).start();
        } catch (Exception e) { /* ignored */ }
    }

    public String getGameId() {
        return gameId;
    }
}
