package handler;

import com.sun.net.httpserver.HttpExchange;
import service.AdminService;

import java.util.List;
import java.util.Map;

public class AdminHandler extends BaseHandler {

    private final AdminService adminService = new AdminService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        if (path.endsWith("/statistics")) {
            Map<String, Object> stats = adminService.getStatistics();
            sendJson(exchange, 200, stats);
        } else if (path.endsWith("/queues")) {
            List<Map<String, Object>> queues = adminService.getActiveQueues();
            sendJson(exchange, 200, queues);
        } else {
            sendError(exchange, 404, "Admin endpoint not found: " + path);
        }
    }
}
