package handler;

import com.sun.net.httpserver.HttpExchange;
import service.AuthService;
import util.JsonUtil;

import java.util.Map;

public class AuthHandler extends BaseHandler {

    private final AuthService authService = new AuthService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
            handleLogin(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/register")) {
            handleRegister(exchange);
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/logout")) {
            handleLogout(exchange);
        } else if ("GET".equalsIgnoreCase(method) && path.endsWith("/me")) {
            handleMe(exchange);
        } else {
            sendError(exchange, 404, "Authentication endpoint not found: " + path);
        }
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String email = JsonUtil.getString(req, "email");
        String password = JsonUtil.getString(req, "password");

        Map<String, Object> session = authService.login(email, password);
        sendJson(exchange, 200, session);
    }

    private void handleRegister(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);

        String name = JsonUtil.getString(req, "name");
        String email = JsonUtil.getString(req, "email");
        String password = JsonUtil.getString(req, "password");
        String phone = JsonUtil.getString(req, "phone");
        String dob = JsonUtil.getString(req, "dateOfBirth");
        String gender = JsonUtil.getString(req, "gender");
        String address = JsonUtil.getString(req, "address");

        Map<String, Object> session = authService.registerPatient(name, email, password, phone, dob, gender, address);
        sendJson(exchange, 201, session);
    }

    private void handleLogout(HttpExchange exchange) throws Exception {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            AuthService.invalidateSession(token);
        }
        sendJson(exchange, 200, Map.of("message", "Logged out successfully."));
    }

    private void handleMe(HttpExchange exchange) throws Exception {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(exchange, 401, "No authentication token provided.");
            return;
        }
        String token = authHeader.substring(7);
        Map<String, Object> session = AuthService.getSession(token);
        if (session == null) {
            sendError(exchange, 401, "Invalid or expired session.");
            return;
        }
        sendJson(exchange, 200, session);
    }
}
