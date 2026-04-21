import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class Client extends JFrame {

	private JPanel contentPane;
	private JTextField txtHost;
	private JTextField txtPort;
	private JTextField txtInput;
	private JTextArea areaResult;
	private JButton btnSend;

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				Client frame = new Client();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public Client() {
		setTitle("Client Nguyễn Trần Thanh Tâm - 52400315");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(600, 450);
		setLocationRelativeTo(null);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(10, 10));

		JPanel pnlConfig = new JPanel();
		FlowLayout flowLayout = (FlowLayout) pnlConfig.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(pnlConfig, BorderLayout.NORTH);
		
		JLabel label = new JLabel("Host:");
		label.setFont(new Font("Tahoma", Font.BOLD, 10));
		label.setForeground(new Color(0, 0, 255));
		pnlConfig.add(label);
		txtHost = new PlaceholderTextField("localhost", 15);
		pnlConfig.add(txtHost);
		
		JLabel label_1 = new JLabel("Port:");
		label_1.setFont(new Font("Tahoma", Font.BOLD, 10));
		label_1.setForeground(new Color(0, 0, 255));
		pnlConfig.add(label_1);
		txtPort = new PlaceholderTextField("2020", 8);
		pnlConfig.add(txtPort);

		areaResult = new JTextArea();
		areaResult.setEditable(false);
		areaResult.setFont(new Font("Monospaced", Font.PLAIN, 14));
		
		JScrollPane scrollPane = new JScrollPane(areaResult);
		scrollPane.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "K\u1EBFt qu\u1EA3 tr\u1EA3 v\u1EC1", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 255)));
		contentPane.add(scrollPane, BorderLayout.CENTER);

		JPanel pnlBottom = new JPanel();
		contentPane.add(pnlBottom, BorderLayout.SOUTH);
		pnlBottom.setLayout(new BorderLayout(5, 0));
		
		txtInput = new PlaceholderTextField("nhập văn bản tại đây...", 30);
		pnlBottom.add(txtInput, BorderLayout.CENTER);
		
		btnSend = new JButton("Gửi (Enter)");
		btnSend.setForeground(new Color(128, 255, 255));
		btnSend.setBackground(new Color(0, 128, 255));
		pnlBottom.add(btnSend, BorderLayout.EAST);

		btnSend.addActionListener(e -> sendRequest());
		txtInput.addActionListener(e -> sendRequest());
		this.getRootPane().setDefaultButton(btnSend);
		SwingUtilities.invokeLater(() -> areaResult.requestFocusInWindow());
	}

	private void sendRequest() {
	    try {
	        String host = txtHost.getText().trim();
	        String portStr = txtPort.getText().trim();
	        String message = txtInput.getText().trim();

	        if (host.isEmpty() || host.equals("localhost")) {
	            host = "127.0.0.1";
	        }

	        if (portStr.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Bạn chưa nhập Port!");
	            return;
	        }
	        
	        if (message.isEmpty()) {
	            return;
	        }

	        int port = Integer.parseInt(portStr);
	        
	        try (DatagramSocket clientSocket = new DatagramSocket()) {
	        	InetAddress IPAddress = InetAddress.getLoopbackAddress();
	            clientSocket.setSoTimeout(3000); // Chờ server trong 3 giây

	            // Gửi dữ liệu
	            byte[] sendData = message.getBytes("UTF-8");
	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
	            clientSocket.send(sendPacket);

	            // Nhận dữ liệu
	            byte[] receiveData = new byte[65507];
	            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	            clientSocket.receive(receivePacket);

	            String result = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
	            areaResult.append("SEND: " + message + "\n");
	            areaResult.append("RECV: " + result + "\n\n");
	            areaResult.setCaretPosition(areaResult.getDocument().getLength());
	            txtInput.setText(""); 
	            txtInput.requestFocus();
	            
	        } catch (SocketTimeoutException e) {
	            areaResult.append("[Lỗi] Server không phản hồi (Timeout).\n");
	        } catch (Exception ex) {
	            areaResult.append("[Lỗi Kết Nối] " + ex.getMessage() + "\n");
	        }

	    } catch (NumberFormatException nfe) {
	        JOptionPane.showMessageDialog(this, "Cổng (Port) phải là số nguyên!");
	    } catch (Exception ex) {
	        JOptionPane.showMessageDialog(this, "Lỗi không xác định: " + ex.getMessage());
	    }
	}

	class PlaceholderTextField extends JTextField {
		public PlaceholderTextField(String placeholder, int columns) {
			super(placeholder, columns);
			setForeground(Color.GRAY);

			this.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					if (getText().equals(placeholder)) {
						setText("");
						setForeground(Color.BLACK);
					}
				}
				@Override
				public void focusLost(FocusEvent e) {
					if (getText().trim().isEmpty()) {
						setForeground(Color.GRAY);
						setText(placeholder);
					}
				}
			});
		}
	}
}