import java.awt.*;
import javax.swing.*;

/**
 * XO Game Frame for TCP and UDP
 */
public class XOGameFrame extends JFrame {
    private XOGamePanel gamePanel;

    public XOGameFrame(String username, GameMessageListener chatPanel) {
        setTitle("XO - " + username);
        setBounds(200, 150, 800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        gamePanel = new XOGamePanel(username, chatPanel);
        contentPane.add(gamePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnClose = new JButton("Close Game");
        btnClose.setFont(new Font("Times New Roman", Font.BOLD, 12));
        btnClose.setPreferredSize(new Dimension(120, 30));
        btnClose.addActionListener(e -> this.dispose());

        bottomPanel.add(btnClose);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    public XOGamePanel getGamePanel() {
        return gamePanel;
    }
}
