package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Serves static HTML, CSS, JavaScript, and asset files from the frontend directory.
 */
public class StaticFileHandler implements HttpHandler {

    private final String frontendRoot;

    public StaticFileHandler() {
        // Resolve frontend directory path
        String[] candidates = { "frontend", "../frontend", "smartop/frontend" };
        String found = "frontend";
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory()) {
                found = c;
                break;
            }
        }
        this.frontendRoot = found;
        System.out.println("[StaticFileHandler] Serving frontend from: " + new File(frontendRoot).getAbsolutePath());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Normalize root path to index.html
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent directory traversal attacks
        if (path.contains("..")) {
            send404(exchange, "Invalid path.");
            return;
        }

        File targetFile = new File(frontendRoot, path.startsWith("/") ? path.substring(1) : path);

        // If directory requested, check for index.html inside
        if (targetFile.isDirectory()) {
            targetFile = new File(targetFile, "index.html");
        }

        if (!targetFile.exists() || !targetFile.isFile()) {
            send404(exchange, "File not found: " + path);
            return;
        }

        String mimeType = getMimeType(targetFile.getName());
        byte[] bytes = Files.readAllBytes(targetFile.toPath());

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void send404(HttpExchange exchange, String message) throws IOException {
        byte[] bytes = ("<h1>404 Not Found</h1><p>" + message + "</p>").getBytes();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
