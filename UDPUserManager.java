import java.io.*;

public class UDPUserManager {

    private static final String DATA_FILE = "dataUDP.txt";

    private static File getDataFile() {
        File file = new File(DATA_FILE);
        
        // If file doesn't exist in current directory, try jar directory
        if (!file.exists()) {
            try {
                String jarPath = UDPUserManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                file = new File(new File(jarPath).getParent(), DATA_FILE);
            } catch (Exception e) {
                // Fall back to current directory
                file = new File(DATA_FILE);
            }
        }
        
        return file;
    }

    public static boolean register(String username, String password) {
        try {
            File file = getDataFile();
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
                    if (parts.length >= 1 && parts[0].trim().equals(username)) {
                        return false; // User already exists
                    }
                }
            }

            // Add new user
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                writer.append(username).append(",").append(password).append("\n");
                System.out.println("User registered to " + filePath);
                return true;
            }
        } catch (IOException ex) {
            System.err.println("Error registering user: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean authenticate(String username, String password) {
        try {
            File file = getDataFile();
            
            // Create file if it doesn't exist
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                return false; // No users registered yet
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
                String line;
                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    return false;
                }
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String storedUser = parts[0].trim();
                        String storedPass = parts[1].trim();
                        if (username.equals(storedUser) && password.equals(storedPass)) {
                            System.out.println("User authenticated: " + username);
                            return true;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Error authenticating user: " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }
}
