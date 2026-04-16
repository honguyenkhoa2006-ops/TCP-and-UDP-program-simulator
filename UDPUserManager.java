import java.io.*;

public class UDPUserManager {

    private static final String DATA_FILE = "dataUDP.txt";

    public static boolean register(String username, String password) {
        try {
            File file = new File(DATA_FILE);
            String filePath = file.getAbsolutePath();

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
                return true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean authenticate(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
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
                        return true;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
