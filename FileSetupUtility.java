import java.io.*;

/**
 * Utility class to verify and set up data files for the application.
 * Run this before starting the main application to ensure files are configured correctly.
 */
public class FileSetupUtility {

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("   FILE SETUP UTILITY");
        System.out.println("========================================\n");
        
        verifyAndSetupFiles();
        
        System.out.println("\n========================================");
        System.out.println("   SETUP COMPLETE");
        System.out.println("========================================\n");
    }

    public static void verifyAndSetupFiles() {
        verifyFile("data.txt", "TCP User Credentials", getDefaultTCPData());
        verifyFile("dataUDP.txt", "UDP User Credentials", getDefaultUDPData());
    }

    private static void verifyFile(String filename, String description, String defaultContent) {
        System.out.println("[" + filename + "] - " + description);
        System.out.println("-----------------------------------------");
        
        // Find file location (current dir or JAR dir)
        File file = new File(filename);
        
        // If not found in current directory, try JAR directory
        if (!file.exists()) {
            try {
                String jarPath = FileSetupUtility.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI().getPath();
                file = new File(new File(jarPath).getParent(), filename);
                System.out.println("🔍 Tìm kiếm trong JAR directory: " + file.getAbsolutePath());
            } catch (Exception e) {
                file = new File(filename);
            }
        }
        
        try {
            if (!file.exists()) {
                System.out.println("❌ File không tồn tại: " + file.getAbsolutePath());
                System.out.println("✓ Đang tạo file mới với dữ liệu mặc định...");
                
                // Create parent directories if needed
                file.getParentFile().mkdirs();
                
                // Create file with default content
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(defaultContent);
                }
                
                System.out.println("✓ Tạo thành công: " + file.getAbsolutePath());
                System.out.println("✓ Nội dung mặc định:");
                System.out.println("   " + defaultContent.replace("\n", "\n   "));
            } else {
                System.out.println("✓ File tồn tại: " + file.getAbsolutePath());
                System.out.println("✓ Kích thước: " + file.length() + " bytes");
                
                // Verify content format
                int lineCount = 0;
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            lineCount++;
                            if (!line.contains(",")) {
                                System.out.println("⚠️  Cảnh báo: Dòng không hợp lệ: '" + line + "'");
                                System.out.println("   Format đúng: username,password");
                            } else {
                                String[] parts = line.split(",");
                                System.out.println("   ✓ Người dùng: " + parts[0].trim());
                            }
                        }
                    }
                }
                System.out.println("✓ Tổng tài khoản: " + lineCount);
            }
        } catch (IOException ex) {
            System.out.println("❌ Lỗi khi xử lý file: " + ex.getMessage());
            ex.printStackTrace();
        }
        
        System.out.println();
    }

    private static String getDefaultTCPData() {
        return "khoa,123\n" +
               "tam,123\n" +
               "soone,123\n" +
               "tamtt,123\n";
    }

    private static String getDefaultUDPData() {
        return "user1,pass123\n" +
               "user2,pass456\n";
    }

    /**
     * Hàm để tìm file tự động trong nhiều vị trí
     */
    public static File findDataFile(String filename) {
        // 1. Kiểm tra thư mục hiện tại
        File file = new File(filename);
        if (file.exists()) {
            return file;
        }

        // 2. Kiểm tra thư mục jar
        try {
            String jarPath = FileSetupUtility.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            file = new File(new File(jarPath).getParent(), filename);
            if (file.exists()) {
                return file;
            }
        } catch (Exception e) {
            // Ignored
        }

        // 3. Kiểm tra thư mục src (nếu chạy từ IDE)
        file = new File("src/" + filename);
        if (file.exists()) {
            return file;
        }

        // 4. Quay lại mặc định
        return new File(filename);
    }

    /**
     * In ra tất cả các vị trí mà ứng dụng sẽ tìm kiếm file
     */
    public static void printSearchLocations() {
        System.out.println("\nVị trí tìm kiếm file:");
        System.out.println("1. Thư mục hiện tại: " + new File(".").getAbsolutePath());
        
        try {
            String jarPath = FileSetupUtility.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            System.out.println("2. Thư mục JAR: " + new File(jarPath).getParent());
        } catch (Exception e) {
            System.out.println("2. Thư mục JAR: (không thể xác định)");
        }
        
        System.out.println("3. Thư mục src: " + new File("src").getAbsolutePath());
    }
}
