package server;

import com.sun.net.httpserver.HttpServer;
import config.DatabaseConfig;
import db.DatabaseConnection;
import handler.*;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main HTTP Server for SmartOP Outpatient Department Queue Management System.
 * Built strictly with Core Java (JDK HttpServer) without external web frameworks.
 */
public class SmartOPServer {

    private static HttpServer server;

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("   SmartOP: Hospital OPD Queue Management System");
        System.out.println("=======================================================");

        // 1. Verify Database Connectivity
        System.out.println("[SmartOP] Checking MySQL connection to: " + DatabaseConfig.getDbUrl());
        boolean dbOk = DatabaseConnection.testConnection();
        if (dbOk) {
            System.out.println("[SmartOP] Database connection verified successfully!");
        } else {
            System.err.println("[SmartOP] WARNING: Could not establish connection to MySQL database!");
            System.err.println("[SmartOP] Please verify MySQL service is running and credentials in config.properties are correct.");
        }

        // 2. Initialize HTTP Server
        int port = DatabaseConfig.getServerPort();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register API Handlers
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/departments", new DepartmentHandler());
            server.createContext("/api/doctors", new DoctorHandler());
            server.createContext("/api/appointments", new AppointmentHandler());
            server.createContext("/api/queue", new QueueHandler());
            server.createContext("/api/admin", new AdminHandler());

            // Register Static File Handler for Frontend UI
            server.createContext("/", new StaticFileHandler());

            // Assign multi-threaded executor
            server.setExecutor(Executors.newFixedThreadPool(25));
            server.start();

            System.out.println("[SmartOP] Server started successfully on port " + port);
            System.out.println("[SmartOP] Web Application: http://localhost:" + port + "/");
            System.out.println("[SmartOP] REST API root:   http://localhost:" + port + "/api/");
            System.out.println("=======================================================");

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SmartOP] Shutting down server...");
                if (server != null) {
                    server.stop(1);
                }
                System.out.println("[SmartOP] Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("[SmartOP] Fatal error while starting HTTP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
