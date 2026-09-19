package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Department;
import service.DepartmentService;
import util.JsonUtil;

import java.util.List;
import java.util.Map;

public class DepartmentHandler extends BaseHandler {

    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // /api/departments or /api/departments/{id}
        String[] parts = path.split("/");

        if ("GET".equalsIgnoreCase(method)) {
            if (parts.length > 3 && !parts[3].isEmpty()) {
                int id = Integer.parseInt(parts[3]);
                Department dept = departmentService.getDepartmentById(id);
                if (dept == null) sendError(exchange, 404, "Department not found.");
                else sendJson(exchange, 200, dept);
            } else {
                Map<String, String> query = parseQueryParams(exchange);
                boolean all = "true".equalsIgnoreCase(query.get("all"));
                List<Department> list = all ? departmentService.getAllDepartments() : departmentService.getActiveDepartments();
                sendJson(exchange, 200, list);
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String name = JsonUtil.getString(req, "departmentName");
            String desc = JsonUtil.getString(req, "description");
            Department created = departmentService.createDepartment(name, desc);
            sendJson(exchange, 201, created);
        } else if ("PUT".equalsIgnoreCase(method)) {
            if (parts.length < 4) {
                sendError(exchange, 400, "Department ID is required in URL.");
                return;
            }
            int id = Integer.parseInt(parts[3]);
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String name = JsonUtil.getString(req, "departmentName");
            String desc = JsonUtil.getString(req, "description");
            String status = JsonUtil.getString(req, "status", "ACTIVE");
            boolean ok = departmentService.updateDepartment(id, name, desc, status);
            sendJson(exchange, 200, Map.of("updated", ok));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            if (parts.length < 4) {
                sendError(exchange, 400, "Department ID is required in URL.");
                return;
            }
            int id = Integer.parseInt(parts[3]);
            boolean ok = departmentService.deleteDepartment(id);
            sendJson(exchange, 200, Map.of("deleted", ok));
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
