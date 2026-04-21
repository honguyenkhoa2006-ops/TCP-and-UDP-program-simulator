import java.io.*;
import javax.swing.*;

public class RegisterFrame extends JFrame {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JPasswordField txtConfirmPass;

    public RegisterFrame() {
        setTitle("Register - TCP");
        setSize(350, 300);
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

        JLabel lblConfirmPass = new JLabel("Confirm:");
        lblConfirmPass.setBounds(30, 70, 80, 25);
        add(lblConfirmPass);

        txtConfirmPass = new JPasswordField();
        txtConfirmPass.setBounds(120, 70, 120, 25);
        add(txtConfirmPass);

        JButton btnRegister = new JButton("+ Register");
        btnRegister.setBounds(60, 110, 100, 30);
        add(btnRegister);

        JButton btnBack = new JButton("< Back");
        btnBack.setBounds(170, 110, 100, 30);
        add(btnBack);

        btnRegister.addActionListener(e -> {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());
            String confirmPass = new String(txtConfirmPass.getPassword());

            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (registerUserToFile(user, pass)) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                new LoginFrame().setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "User already exists!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });

        setLocationRelativeTo(null);
    }

    private boolean registerUserToFile(String user, String pass) {
        try {
            // Ensure files are properly set up before registration
            FileSetupUtility.verifyAndSetupFiles();
            
            File file = new File("data.txt");
            
            // Try to find file in jar directory if not in current directory
            if (!file.exists()) {
                try {
                    String jarPath = RegisterFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                    file = new File(new File(jarPath).getParent(), "data.txt");
                } catch (Exception e) {
                    file = new File("data.txt");
                }
            }
            
            String filePath = file.getAbsolutePath();

            // Create file if it doesn't exist
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            // Check if user already exists
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 1 && parts[0].trim().equals(user)) {
                        return false; // User already exists
                    }
                }
            }

            // Add new user
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                // Ensure file ends with newline before appending
                if (file.length() > 0) {
                    RandomAccessFile raf = new RandomAccessFile(filePath, "r");
                    if (raf.length() > 0) {
                        raf.seek(raf.length() - 1);
                        byte lastByte = raf.readByte();
                        raf.close();
                        if (lastByte != '\n') {
                            writer.append("\n");
                        }
                    } else {
                        raf.close();
                    }
                }
                writer.append(user).append(",").append(pass).append("\n");
                System.out.println("User registered to " + filePath);
                return true;
            }
        } catch (IOException ex) {
            System.err.println("Error registering user: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
}
