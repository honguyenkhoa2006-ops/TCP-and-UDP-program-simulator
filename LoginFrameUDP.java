import java.awt.EventQueue;
import java.io.*;
import javax.swing.*;
import java.util.HashMap;


public class LoginFrameUDP extends JFrame {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private static HashMap<String, String> userCredentials = new HashMap<>();
    
    static {
        try {
            loadUserCredentialsFromFile("dataUDP.txt");
            System.out.println("[SUCCESS] HashMap loaded with " + userCredentials.size() + " users");
            for (String user : userCredentials.keySet()) {
                System.out.println("  - User: " + user + " | Pass: " + userCredentials.get(user));
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] Failed to load credentials: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				// Verify and setup data files at startup
				try {
					FileSetupUtility.verifyAndSetupFiles();
				} catch (Exception setupEx) {
					System.err.println("[WARN] FileSetupUtility failed: " + setupEx.getMessage());
				}
				
				LoginFrameUDP frame = new LoginFrameUDP();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

    public LoginFrameUDP() {
        // Reload HashMap every time LoginFrameUDP is created
        userCredentials.clear();
        loadUserCredentialsFromFile("dataUDP.txt");
        
        setTitle("Login (UDP)");
        setSize(350, 200);
        setLayout(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel lblUser = new JLabel("Username:");
        lblUser.setBounds(30, 10, 80, 25);
        add(lblUser);

        txtUser = new placeholding("Enter username", 15);
        txtUser.setBounds(120, 10, 120, 25);
        txtUser.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
        add(txtUser);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setBounds(30, 40, 80, 25);
        add(lblPass);

        txtPass = new JPasswordField();
        txtPass.setBounds(120, 40, 120, 25);
        txtPass.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
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
        btnChangePassword.setBounds(30, 100, 290, 25);
        add(btnChangePassword);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBounds(250, 70, 70, 25);
        add(btnLogin);

        JButton btnBack = new JButton("< Back");
        btnBack.setBounds(250, 10, 80, 25);
        add(btnBack);

        
        btnLogin.addActionListener(e ->{
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            
            System.out.println("\n" + repeat("=", 50));
        System.out.println("[LOGIN] ATTEMPT");
        System.out.println(repeat("=", 50));
        System.out.println("[INPUT] user='" + user + "' | pass='" + pass + "'");
            if (user.isEmpty() || pass.isEmpty()) {
                System.out.println("Empty fields!");
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if(checkCredentials(user, pass)) {
                System.out.println("[SUCCESS] Login SUCCESS!");
                JOptionPane.showMessageDialog(this, "Login success!");

                UDPClientFrame.setupLookAndFeel();
                UDPClientFrame manager = new UDPClientFrame(user);
                manager.setVisible(true);

                this.dispose();
            } else {
                System.out.println("Login FAILED - Invalid credentials!");
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
            }
            System.out.println(repeat("=", 50) + "\n");
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
        
        JButton btnChange = new JButton("[OK] Confirm");
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
        
        JButton btnClose = new JButton("[X] Close");
        btnClose.setBounds(160, 110, 80, 30);
        btnClose.addActionListener(e -> dialog.dispose());
        dialog.add(btnClose);
        
        dialog.setVisible(true);
    }
    
    private boolean checkCredentials(String user, String pass) {
        System.out.println("[CRED] Checking credentials:");
        System.out.println("   Input user: '" + user + "'");
        System.out.println("   Input pass: '" + pass + "'");
        System.out.println("   HashMap size: " + userCredentials.size());
        
        // Filter out placeholder text
        if (user.equals("Enter username") || user.isEmpty()) {
            System.out.println("Empty/Placeholder username!");
            return false;
        }
        
        if (pass.isEmpty()) {
            System.out.println("Empty password!");
            return false; 
        }
        
        // List all users in HashMap
        System.out.println("   Available users in HashMap:");
        for (String key : userCredentials.keySet()) {
            System.out.println("     - '" + key + "' : '" + userCredentials.get(key) + "'");
        }
        
        // Check credentials using HashMap
        boolean exists = userCredentials.containsKey(user);
        System.out.println("   User exists: " + exists);
        
        if (exists) {
            String storedPass = userCredentials.get(user);
            boolean matches = storedPass.equals(pass);
            System.out.println("   Stored pass: '" + storedPass + "'");
            System.out.println("   Password match: " + matches);
            return matches;
        } else {
            System.out.println("[ERROR] User '" + user + "' not found");
            return false;
        }
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
                System.out.println("[OK] Password changed for user: " + username);
                return true;
            }
        }
        return false;
    }
    
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    private static void loadUserCredentialsFromFile(String filename) {
        try {
            // Get the actual file path - first check current directory
            File file = new File(filename);
            System.out.println("[LOAD] Loading " + filename + "...");
            System.out.println("   Current working dir: " + System.getProperty("user.dir"));
            
            if (!file.exists()) {
                System.out.println("   Not found in current dir, checking JAR location...");
                try {
                    String jarPath = LoginFrameUDP.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                    file = new File(new File(jarPath).getParent(), filename);
                } catch (Exception e) {
                    file = new File(filename);
                }
            }
            
            if (!file.exists()) {
                System.err.println("[ERROR] " + filename + " not found at: " + file.getAbsolutePath());
                return;
            }
            
            System.out.println("[OK] Found: " + file.getAbsolutePath());
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String username = parts[0].trim();
                        String password = parts[1].trim();
                        userCredentials.put(username, password);
                        System.out.println("   [OK] User: " + username + " | Pass: " + password);
                        lineCount++;
                    } else {
                        System.out.println("   [WARN] Skipped invalid line: " + line);
                    }
                }
                System.out.println("[OK] Total loaded: " + lineCount + " users\n");
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private static void writeUserCredentialsToFile(String filename) {
        try {
            // Get the actual file path - use the same logic as loading
            File file = new File(filename);
            
            if (!file.exists()) {
                try {
                    String jarPath = LoginFrameUDP.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                    file = new File(new File(jarPath).getParent(), filename);
                } catch (Exception e) {
                    file = new File(filename);
                }
            }
            
            // Create file if it doesn't exist
            if (!file.exists()) {
                file.createNewFile();
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String username : userCredentials.keySet()) {
                    String password = userCredentials.get(username);
                    writer.write(username + "," + password);
                    writer.newLine();
                }
            }
            System.out.println("[OK] Credentials saved to " + file.getAbsolutePath() + " (" + userCredentials.size() + " users)");
        } catch (IOException ex) {
            System.err.println("[ERROR] Error writing credentials: " + ex.getMessage());
            ex.printStackTrace();
        }
    }    
}

