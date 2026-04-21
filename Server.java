import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class Server extends JFrame {
    private JTextField txtPort;
    private JTextArea textArea;
    private JButton btnStart, btnStop;
    private DatagramSocket socket;
    private volatile boolean running = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            new Server().setVisible(true);
        });
    }

    public Server() {
        setTitle("UDP Server UPPERCASE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // --- TOP PANEL ---
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Cổng:");
        label.setFont(new Font("Tahoma", Font.BOLD, 10));
        label.setForeground(new Color(0, 0, 255));
        pnlTop.add(label);

        txtPort = new JTextField("Nhập cổng", 15);
        txtPort.setFont(new Font("Tahoma", Font.ITALIC, 10));
        txtPort.setForeground(Color.GRAY);
        
        txtPort.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtPort.getText().equals("Nhập cổng")) {
                    txtPort.setText("");
                    txtPort.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtPort.getText().trim().isEmpty()) {
                    txtPort.setForeground(Color.GRAY);
                    txtPort.setText("Nhập cổng");
                }
            }
        });
        pnlTop.add(txtPort);

        btnStart = new JButton("Start Server");
        btnStart.setFont(new Font("Tahoma", Font.BOLD, 10));
        btnStart.setForeground(new Color(0, 255, 0));
        btnStart.setBackground(new Color(0, 128, 0));
        btnStop = new JButton("Stop Server");
        btnStop.setFont(new Font("Tahoma", Font.BOLD, 10));
        btnStop.setForeground(new Color(255, 255, 255));
        btnStop.setBackground(new Color(255, 0, 0));
        btnStop.setEnabled(false);
        pnlTop.add(btnStart);
        pnlTop.add(btnStop);

        contentPane.add(pnlTop, BorderLayout.NORTH);

        // --- CENTER PANEL ---
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Server Logs", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 255)));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // --- SỰ KIỆN ---
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());

        // MẸO QUAN TRỌNG: Chuyển tiêu điểm ra khỏi ô Port khi vừa mở app
        SwingUtilities.invokeLater(() -> textArea.requestFocusInWindow());
    }

    private void startServer() {
        String portStr = txtPort.getText();
        if (portStr.equals("Nhập cổng") || portStr.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập port!");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Port must be in range 1-65535!");
                return;
            }
            socket = new DatagramSocket(port);
            running = true;
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            txtPort.setEditable(false);
            textArea.append("[System] Server đang chạy ở cổng " + port + "\n");

            new Thread(() -> {
                byte[] buffer = new byte[65507];
                while (running) {
                    try {
                        DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                        socket.receive(rp);
                        String msg = new String(rp.getData(), 0, rp.getLength(), "UTF-8");
                        textArea.append("Nhận: " + msg + "\n");

                        byte[] sd = msg.toUpperCase().getBytes("UTF-8");
                        socket.send(new DatagramPacket(sd, sd.length, rp.getAddress(), rp.getPort()));
                    } catch (Exception ex) {
                        if (running) textArea.append("[Info] Server dừng.\n");
                    }
                }
            }).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void stopServer() {
        running = false;
        if (socket != null) socket.close();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        txtPort.setEditable(true);
        // Trả lại placeholder nếu ô trống
        if(txtPort.getText().isEmpty()){
            txtPort.setForeground(Color.GRAY);
            txtPort.setText("Nhập cổng");
        }
    }
}