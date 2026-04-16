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

        // Top panel: Title
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("Protocol Communication Project");
        lblTitle.setFont(new Font("Times New Roman", Font.BOLD, 24));
        lblTitle.setForeground(new Color(5, 150, 215));
        titlePanel.add(lblTitle);
        contentPane.add(titlePanel, BorderLayout.NORTH);

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
            Color.BLACK
        ));

        JLabel lblMember1 = new JLabel("Hồ Nguyên Khoa          MSSV: 52400279");
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
            Color.BLACK
        ));

        JLabel lblSelect = new JLabel("Protocol");
        lblSelect.setFont(new Font("Times New Roman", Font.BOLD, 13));

        String[] options = { "-- Select --", "TCP Client", "TCP Server", "UDP Client", "UDP Server" };
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        comboBox.setPreferredSize(new Dimension(200, 35));

        JButton btnSelect = new JButton("Open");
        btnSelect.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnSelect.setPreferredSize(new Dimension(100, 35));
        btnSelect.setFocusPainted(false);
        btnSelect.addActionListener(e -> {
            String selected = (String) comboBox.getSelectedItem();
            if (selected.equals("TCP Client")) {
                try {
                    new LoginFrame().setVisible(true);
                    this.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening TCP Client: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (selected.equals("TCP Server")) {
                try {
                    TCPServerFrame.setupLookAndFeel();
                    new TCPServerFrame().setVisible(true);
                    this.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening TCP Server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (selected.equals("UDP Client")) {
                try {
                    new LoginFrameUDP().setVisible(true);
                    this.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening UDP Client: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (selected.equals("UDP Server")) {
                try {
                    UDPServerFrame.setupLookAndFeel();
                    new UDPServerFrame().setVisible(true);
                    this.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error opening UDP Server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an option!", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        comboPanel.setBackground(Color.WHITE);
        comboPanel.add(lblSelect);
        comboPanel.add(comboBox);
        comboPanel.add(btnSelect);

        selectionPanel.add(comboPanel, BorderLayout.CENTER);
        centerPanel.add(selectionPanel, BorderLayout.CENTER);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel: Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnExit = new JButton("Exit");
        btnExit.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnExit.setPreferredSize(new Dimension(100, 30));
        btnExit.addActionListener(e -> System.exit(0));

        bottomPanel.add(btnExit);

        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }
}

