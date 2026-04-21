import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * Simple XO Network Game - TCP Only
 * Direct implementation based on provided XOGameTCP pattern
 */
public class XOGameTCP extends JFrame {
    private int size = 3;
    private JButton[][] buttons;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean myTurn = false;
    private String mySymbol = "";
    private String opponentSymbol = "";

    // UI Components
    private JPanel lobbyPanel, gamePanel;
    private JTextField txtIP, txtSize;

    public XOGameTCP() {
        initLobby();
    }

    private void initLobby() {
        setTitle("XO Network Game - TCP");
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        lobbyPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        lobbyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtSize = new JTextField("3");
        txtIP = new JTextField("localhost");
        JButton btnHost = new JButton("Tạo Match (Host)");
        JButton btnJoin = new JButton("Tham gia Match (Join)");

        lobbyPanel.add(new JLabel("Kích thước bàn cờ (3-21):"));
        lobbyPanel.add(txtSize);
        lobbyPanel.add(new JLabel("Nhập IP đối thủ (để Join):"));
        lobbyPanel.add(txtIP);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        btnPanel.add(btnHost);
        btnPanel.add(btnJoin);
        lobbyPanel.add(btnPanel);

        add(lobbyPanel, "Lobby");

        btnHost.addActionListener(e -> startServer());
        btnJoin.addActionListener(e -> startClient());

        setVisible(true);
    }

    private void startServer() {
        size = Integer.parseInt(txtSize.getText());
        if (size < 3 || size > 21)
            size = 3;

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(9999)) {
                System.out.println("Đang đợi đối thủ...");
                socket = server.accept();
                setupNetwork(true);
                // Gửi size cho Client
                out.println("INIT:" + size);
                SwingUtilities.invokeLater(() -> createBoard());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startClient() {
        new Thread(() -> {
            try {
                socket = new Socket(txtIP.getText(), 9999);
                setupNetwork(false);
                // Đợi nhận size từ Host
                String msg = in.readLine();
                if (msg.startsWith("INIT:")) {
                    size = Integer.parseInt(msg.split(":")[1]);
                }
                SwingUtilities.invokeLater(() -> createBoard());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupNetwork(boolean isHost) throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        myTurn = isHost;
        mySymbol = isHost ? "X" : "O";
        opponentSymbol = isHost ? "O" : "X";
        startListening();
    }

    private void createBoard() {
        lobbyPanel.setVisible(false);
        gamePanel = new JPanel(new GridLayout(size, size));
        buttons = new JButton[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 12));
                int r = i, c = j;
                buttons[i][j].addActionListener(e -> makeMove(r, c));
                gamePanel.add(buttons[i][j]);
            }
        }

        add(gamePanel, "Game");
        ((CardLayout) getContentPane().getLayout()).show(getContentPane(), "Game");
        setSize(size * 40 + 50, size * 40 + 80);
        setTitle("Bạn là: " + mySymbol + (myTurn ? " (Đến lượt)" : " (Chờ...)"));
    }

    private void makeMove(int r, int c) {
        if (myTurn && buttons[r][c].getText().equals("")) {
            buttons[r][c].setText(mySymbol);
            out.println("MOVE:" + r + ":" + c);
            myTurn = false;
            setTitle("Đợi đối thủ...");
            checkGameOver(r, c, mySymbol);
        }
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readLine();
                    if (msg == null)
                        break;
                    if (msg.startsWith("MOVE:")) {
                        String[] p = msg.split(":");
                        int r = Integer.parseInt(p[1]);
                        int c = Integer.parseInt(p[2]);
                        SwingUtilities.invokeLater(() -> {
                            buttons[r][c].setText(opponentSymbol);
                            myTurn = true;
                            setTitle("Đến lượt bạn (" + mySymbol + ")");
                            checkGameOver(r, c, opponentSymbol);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkGameOver(int r, int c, String s) {
        if (isWin(r, c, s)) {
            JOptionPane.showMessageDialog(this, "Người chơi " + s + " thắng!");
            System.exit(0);
        }
    }

    private boolean isWin(int r, int c, String s) {
        int[][] dirs = { { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1, -1 } };
        int target = (size == 3) ? 3 : 5;
        for (int[] d : dirs) {
            int count = 1 + countDir(r, c, d[0], d[1], s) + countDir(r, c, -d[0], -d[1], s);
            if (count >= target)
                return true;
        }
        return false;
    }

    private int countDir(int r, int c, int dr, int dc, String s) {
        int count = 0;
        int nr = r + dr, nc = c + dc;
        while (nr >= 0 && nr < size && nc >= 0 && nc < size && buttons[nr][nc].getText().equals(s)) {
            count++;
            nr += dr;
            nc += dc;
        }
        return count;
    }

    public static void main(String[] args) {
        new XOGameTCP();
    }
}
