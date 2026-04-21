import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MainMenu extends JFrame {

    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                // Verify and setup data files at startup
                FileSetupUtility.verifyAndSetupFiles();

                setupLookAndFeel();
                MainMenu frame = new MainMenu();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MainMenu() {
        setTitle("Protocol Communication System");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPane.setBackground(Color.WHITE);
        setContentPane(contentPane);

        // Center panel: Team info
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 10, 15));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                "Team Members",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Times New Roman", Font.BOLD, 14),
                Color.BLACK));

        JLabel lblMember1 = new JLabel("Hồ Nguyên Khoa                 MSSV: 52400279");
        lblMember1.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lblMember1.setForeground(Color.BLACK);

        JLabel lblMember2 = new JLabel("Nguyễn Trần Thanh Tâm          MSSV: 52400315");
        lblMember2.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        lblMember2.setForeground(Color.BLACK);

        infoPanel.add(lblMember1);
        infoPanel.add(lblMember2);

        centerPanel.add(infoPanel, BorderLayout.NORTH);

        JPanel selectionPanel = new JPanel(new BorderLayout(10, 10));
        selectionPanel.setBackground(Color.WHITE);
        selectionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                "Select Mode",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Times New Roman", Font.BOLD, 14),
                Color.BLACK));

        JLabel lblSelect = new JLabel("Protocol");
        lblSelect.setFont(new Font("Times New Roman", Font.BOLD, 13));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        // TCP Client Button
        JButton btnTcpClient = new JButton("TCP client");
        btnTcpClient.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnTcpClient.setPreferredSize(new Dimension(120, 50));
        btnTcpClient.setFocusPainted(false);
        btnTcpClient.addActionListener(e -> {
            try {
                new LoginFrame().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error opening TCP Client: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(btnTcpClient);

        // TCP Server Button
        JButton btnTcpServer = new JButton("TCP server");
        btnTcpServer.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnTcpServer.setPreferredSize(new Dimension(120, 50));
        btnTcpServer.setFocusPainted(false);
        btnTcpServer.addActionListener(e -> {
            try {
                TCPServerFrame.setupLookAndFeel();
                new TCPServerFrame().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error opening TCP Server: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(btnTcpServer);

        // UDP Client Button
        JButton btnUdpClient = new JButton("UDP client");
        btnUdpClient.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnUdpClient.setPreferredSize(new Dimension(120, 50));
        btnUdpClient.setFocusPainted(false);
        btnUdpClient.addActionListener(e -> {
            try {
                new LoginFrameUDP().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error opening UDP Client: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(btnUdpClient);

        // UDP Server Button
        JButton btnUdpServer = new JButton("UDP server");
        btnUdpServer.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnUdpServer.setPreferredSize(new Dimension(120, 50));
        btnUdpServer.setFocusPainted(false);
        btnUdpServer.addActionListener(e -> {
            try {
                UDPServerFrame.setupLookAndFeel();
                new UDPServerFrame().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error opening UDP Server: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttonPanel.add(btnUdpServer);

        selectionPanel.add(buttonPanel, BorderLayout.CENTER);
        centerPanel.add(selectionPanel, BorderLayout.CENTER);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel: Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnExit = new JButton("[X] Exit");
        btnExit.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnExit.setPreferredSize(new Dimension(100, 30));
        btnExit.addActionListener(e -> System.exit(0));

        bottomPanel.add(btnExit);

        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }
}
