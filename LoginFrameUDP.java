import java.awt.EventQueue;
import java.io.*;
import javax.swing.*;
import java.util.HashMap;


public class LoginFrameUDP extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private static String user = " ";
    private static String pass = " ";
    private static HashMap<String, String> userCredentials = new HashMap<>();
    
    static {
        loadUserCredentialsFromFile("dataUDP.txt");
    }
    
    public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				LoginFrameUDP frame = new LoginFrameUDP();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

    public LoginFrameUDP() {
        setTitle("Login (UDP)");
        setSize(350, 250);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(30, 10, 80, 25);
        add(lblUser);

        txtUser = new JTextField();
        txtUser.setBounds(120, 10, 120, 25);
        add(txtUser);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(30, 40, 80, 25);
        add(lblPass);

        txtPass = new JPasswordField();
        txtPass.setBounds(120, 40, 120, 25);
        add(txtPass);

        JCheckBox chkShowPassword = new JCheckBox("Show");
        chkShowPassword.setBounds(250, 40, 60, 25);
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPass.setEchoChar((char) 0);
            } else {
                txtPass.setEchoChar('*');
            }
        });
        add(chkShowPassword);

        JLabel lblog = new JLabel("Register");
        lblog.setBounds(30, 70, 80, 25);
        add(lblog);
        
        JButton btnRegister = new JButton("Register");
        btnRegister.setBounds(120, 70, 120, 25);
        add(btnRegister);

        JButton btnChangePassword = new JButton("Change Password");
        btnChangePassword.setBounds(250, 70, 80, 25);
        add(btnChangePassword);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(30, 110, 80, 30);
        add(btnLogin);

        JButton btnBack = new JButton("Back");
        btnBack.setBounds(120, 110, 80, 30);
        add(btnBack);

        
        btnLogin.addActionListener(e ->{
            user = txtUser.getText().trim();
            pass = new String(txtPass.getPassword()).trim();
            if(getUsernameFromFile(user, pass)) {
                JOptionPane.showMessageDialog(this, "Login success!");

                UDPClientFrame.setupLookAndFeel();
                UDPClientFrame manager = new UDPClientFrame(user);
                manager.setVisible(true);

                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản!");
            }
        });
        btnBack.addActionListener(e -> {
            MainMenu.setupLookAndFeel();
            new MainMenu().setVisible(true);
            this.dispose();
        });
        btnRegister.addActionListener(e -> {
            new RegisterFrameUDP().setVisible(true);
            this.dispose();
        });
        
        btnChangePassword.addActionListener(e -> showChangePasswordDialog());
        
        setLocationRelativeTo(null);
    }
    
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Change Password", true);
        dialog.setSize(300, 180);
        dialog.setLayout(null);
        dialog.setLocationRelativeTo(this);
        
        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(20, 10, 80, 25);
        dialog.add(lblUser);
        
        JTextField txtUsername = new JTextField();
        txtUsername.setBounds(110, 10, 150, 25);
        dialog.add(txtUsername);
        
        JLabel lblOldPass = new JLabel("Old Password:");
        lblOldPass.setBounds(20, 40, 80, 25);
        dialog.add(lblOldPass);
        
        JPasswordField txtOldPass = new JPasswordField();
        txtOldPass.setBounds(110, 40, 150, 25);
        dialog.add(txtOldPass);
        
        JLabel lblNewPass = new JLabel("New Password:");
        lblNewPass.setBounds(20, 70, 80, 25);
        dialog.add(lblNewPass);
        
        JPasswordField txtNewPass = new JPasswordField();
        txtNewPass.setBounds(110, 70, 150, 25);
        dialog.add(txtNewPass);
        
        JButton btnChange = new JButton("Change");
        btnChange.setBounds(60, 110, 80, 30);
        btnChange.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String oldPass = new String(txtOldPass.getPassword());
            String newPass = new String(txtNewPass.getPassword());
            
            if (username.isEmpty() || oldPass.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (changePasswordInFile(username, oldPass, newPass)) {
                JOptionPane.showMessageDialog(dialog, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Invalid username or old password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        dialog.add(btnChange);
        
        JButton btnClose = new JButton("Close");
        btnClose.setBounds(160, 110, 80, 30);
        btnClose.addActionListener(e -> dialog.dispose());
        dialog.add(btnClose);
        
        dialog.setVisible(true);
    }
    
    private boolean changePasswordInFile(String username, String oldPass, String newPass) {
        // Check if user exists and old password is correct using HashMap
        if (userCredentials.containsKey(username)) {
            String storedPass = userCredentials.get(username);
            if (storedPass.equals(oldPass)) {
                // Update HashMap
                userCredentials.put(username, newPass);
                // Write updated credentials back to file
                writeUserCredentialsToFile("dataUDP.txt");
                return true;
            }
        }
        return false;
    }
    private boolean getUsernameFromFile(String user, String pass) {
        if (user.isEmpty() || pass.isEmpty()) {
            return false; 
        }
        // Check credentials using HashMap
        return userCredentials.containsKey(user) && userCredentials.get(user).equals(pass);
    }
    
    private static void loadUserCredentialsFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    userCredentials.put(username, password);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void writeUserCredentialsToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String username : userCredentials.keySet()) {
                writer.write(username + "," + userCredentials.get(username));
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }    
}

