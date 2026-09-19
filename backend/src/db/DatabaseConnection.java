package db;

import config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centralized JDBC Database Connection Manager.
 * Demonstrates singleton connection acquisition pattern with
 * standard MySQL JDBC driver initialization.
 */
public class DatabaseConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DatabaseConnection] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] ERROR: MySQL Connector/J driver not found in classpath!");
            System.err.println("[DatabaseConnection] Please place mysql-connector-j.jar in backend/lib/ folder.");
        }
    }

    /**
     * Obtains a new active connection to the MySQL database.
     * Callers must close the returned connection using try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getDbUrl();
        String user = DatabaseConfig.getDbUsername();
        String pass = DatabaseConfig.getDbPassword();
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Validates database connectivity on startup.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Connectivity check failed: " + e.getMessage());
            return false;
        }
    }
}
