import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UserManager {

    private static final String DATA_FILE = "data.txt";

    public static boolean authenticate(String user, String pass) {
        System.out.println("\n=== AUTHENTICATE START ===");
        System.out.println("User: '" + user + "', Pass: '" + pass + "'");
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println("Data file path: " + new File(DATA_FILE).getAbsolutePath());
        
        // List files in current directory
        System.out.println("Files in working directory:");
        File[] files = new File(System.getProperty("user.dir")).listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("  - " + f.getName() + (f.isDirectory() ? " (DIR)" : ""));
            }
        }
        
        try {
            File file = new File(DATA_FILE);
            System.out.println("File exists: " + file.exists());
            
            if (!file.exists()) {
                System.out.println("File does not exist, authentication fails");
                return false;
            }
            
            // Read and print file contents
            System.out.println("File contents:");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    System.out.println("  Line " + lineNum + ": '" + line + "'");
                }
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
                    System.out.println("User or pass is empty");
                    return false;
                }
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    System.out.println("  Checking line " + lineNum + ": '" + line + "'");
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String username = parts[0].trim();
                        String password = parts[1].trim();
                        System.out.println("    Parsed - username: '" + username + "', password: '" + password + "'");
                        
                        boolean userMatch = user.equals(username);
                        boolean passMatch = pass.equals(password);
                        System.out.println("    User match: " + userMatch + ", Pass match: " + passMatch);
                        
                        if (userMatch && passMatch) {
                            System.out.println("=== AUTHENTICATE SUCCESS ===\n");
                            return true;
                        }
                    } else {
                        System.out.println("    Invalid format, parts: " + parts.length);
                    }
                }
            }
            System.out.println("=== AUTHENTICATE FAILED (no match) ===\n");
        } catch (IOException ex) {
            System.err.println("Authenticate error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean register(String user, String pass) {
        System.out.println("\n=== REGISTER START ===");
        System.out.println("User: '" + user + "', Pass: '" + pass + "'");
        System.out.println("Working dir: " + System.getProperty("user.dir"));
        System.out.println("Data file path: " + new File(DATA_FILE).getAbsolutePath());
        
        // List files in current directory
        System.out.println("Files in working directory:");
        File[] files = new File(System.getProperty("user.dir")).listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println("  - " + f.getName() + (f.isDirectory() ? " (DIR)" : ""));
            }
        }
        
        try {
            if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
                System.out.println("User or pass is empty");
                return false;
            }

            File file = new File(DATA_FILE);
            
            // Create file if not exists
            if (!file.exists()) {
                System.out.println("File does not exist, creating...");
                boolean created = file.createNewFile();
                System.out.println("File created: " + created);
            } else {
                System.out.println("File already exists");
            }

            // Check if user already exists
            System.out.println("Checking if user already exists...");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    System.out.println("  Checking line " + lineNum + ": '" + line + "'");
                    String[] parts = line.split(",");
                    if (parts.length >= 1) {
                        String existingUser = parts[0].trim();
                        System.out.println("    Existing user: '" + existingUser + "'");
                        if (existingUser.equals(user)) {
                            System.out.println("=== REGISTER FAILED (user exists) ===\n");
                            return false;
                        }
                    }
                }
            }

            // Add new user
            System.out.println("Adding new user...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                String newLine = user + "," + pass;
                System.out.println("Writing line: '" + newLine + "'");
                writer.append(newLine);
                writer.append("\n");
                writer.flush();
                System.out.println("Flushed successfully");
                
                // Verify write by re-reading file
                System.out.println("Verifying file contents after write:");
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        System.out.println("  Line " + lineNum + ": '" + line + "'");
                    }
                }
                
                System.out.println("=== REGISTER SUCCESS ===\n");
                return true;
            }
        } catch (IOException ex) {
            System.err.println("Register error: " + ex.getMessage());
            ex.printStackTrace();
            System.out.println("=== REGISTER FAILED (exception) ===\n");
            return false;
        }
    }
}
