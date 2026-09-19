package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * Loads configuration properties for SmartOP server and database.
 * Supports cloud deployment environment variables (Railway, Render, Heroku, etc.)
 * with automatic fallback to local config.properties and local defaults.
 */
public class DatabaseConfig {

    private static final Properties properties = new Properties();

    // Parsed credentials from standard cloud DATABASE_URL if present
    private static String parsedDbUrl = null;
    private static String parsedDbUser = null;
    private static String parsedDbPassword = null;

    static {
        loadProperties();
        parseCloudEnvironment();
    }

    private static void loadProperties() {
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
            System.out.println("[Config] No local properties file found. Using environment variables or defaults.");
        }
    }

    /**
     * Inspects environment variables commonly provided by cloud platforms
     * (e.g. DATABASE_URL, MYSQL_URL, DB_URL, MYSQLHOST, etc.)
     */
    private static void parseCloudEnvironment() {
        // 1. Check for combined URL string (DATABASE_URL, DB_URL, MYSQL_URL, etc.)
        String rawUrl = getEnv("DATABASE_URL", "DB_URL", "MYSQL_URL", "MYSQL_PUBLIC_URL");

        if (rawUrl != null && !rawUrl.trim().isEmpty()) {
            rawUrl = rawUrl.trim();
            if (rawUrl.startsWith("jdbc:mysql://")) {
                parsedDbUrl = rawUrl;
            } else if (rawUrl.startsWith("mysql://")) {
                try {
                    // Parse mysql://username:password@hostname:port/database
                    URI uri = new URI(rawUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 3306;
                    String path = uri.getPath();
                    String dbName = (path != null && path.length() > 1) ? path.substring(1) : "smartop_db";

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        parsedDbUser = parts[0];
                        parsedDbPassword = parts[1];
                    }

                    parsedDbUrl = String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                        host, port, dbName
                    );
                    System.out.println("[Config] Parsed cloud DATABASE_URL targeting host: " + host + ":" + port + "/" + dbName);
                } catch (Exception e) {
                    System.err.println("[Config] Could not parse raw mysql URL (" + e.getMessage() + "), using as-is.");
                    parsedDbUrl = rawUrl.replace("mysql://", "jdbc:mysql://");
                }
            }
        }

        // 2. Check for individual host/port/database environment variables (e.g. Railway)
        if (parsedDbUrl == null) {
            String host = getEnv("DB_HOST", "MYSQLHOST", "DATABASE_HOST");
            if (host != null && !host.trim().isEmpty()) {
                String port = getEnv("DB_PORT", "MYSQLPORT", "DATABASE_PORT");
                if (port == null || port.trim().isEmpty()) port = "3306";
                String dbName = getEnv("DB_NAME", "MYSQLDATABASE", "DATABASE_NAME");
                if (dbName == null || dbName.trim().isEmpty()) dbName = "smartop_db";

                parsedDbUrl = String.format(
                    "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                    host.trim(), port.trim(), dbName.trim()
                );
                System.out.println("[Config] Assembled cloud JDBC URL from individual env vars: " + host + ":" + port + "/" + dbName);
            }
        }
    }

    public static int getServerPort() {
        // Cloud containers (Render, Railway, Heroku) supply PORT environment variable
        String envPort = getEnv("PORT", "SERVER_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        String port = properties.getProperty("server.port", "8080");
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 8080;
        }
    }

    public static String getDbUrl() {
        if (parsedDbUrl != null) {
            return parsedDbUrl;
        }
        return properties.getProperty("db.url",
            "jdbc:mysql://localhost:3306/smartop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
    }

    public static String getDbUsername() {
        String envUser = getEnv("DB_USER", "DB_USERNAME", "MYSQLUSER", "DATABASE_USERNAME");
        if (envUser != null && !envUser.trim().isEmpty()) {
            return envUser.trim();
        }
        if (parsedDbUser != null) {
            return parsedDbUser;
        }
        return properties.getProperty("db.username", "root");
    }

    public static String getDbPassword() {
        String envPass = getEnv("DB_PASSWORD", "DB_PASS", "MYSQLPASSWORD", "DATABASE_PASSWORD");
        if (envPass != null) {
            return envPass;
        }
        if (parsedDbPassword != null) {
            return parsedDbPassword;
        }
        return properties.getProperty("db.password", "root@123");
    }

    private static String getEnv(String... keys) {
        for (String k : keys) {
            String val = System.getenv(k);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return null;
    }
}
