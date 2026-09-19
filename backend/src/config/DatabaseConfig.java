package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration properties for SmartOP server and database.
 */
public class DatabaseConfig {

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        // Look in common locations: current dir, backend/, or parent
        String[] possiblePaths = {
            "config.properties",
            "backend/config.properties",
            "../config.properties"
        };

        boolean loaded = false;
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try (InputStream in = new FileInputStream(file)) {
                    properties.load(in);
                    loaded = true;
                    System.out.println("[Config] Loaded properties from: " + file.getAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.err.println("[Config] Error reading " + path + ": " + e.getMessage());
                }
            }
        }

        if (!loaded) {
            System.out.println("[Config] Using default configuration values.");
        }
    }

    public static int getServerPort() {
        String port = properties.getProperty("server.port", "8080");
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    public static String getDbUrl() {
        return properties.getProperty("db.url",
            "jdbc:mysql://localhost:3306/smartop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
    }

    public static String getDbUsername() {
        return properties.getProperty("db.username", "root");
    }

    public static String getDbPassword() {
        return properties.getProperty("db.password", "root@123");
    }
}
