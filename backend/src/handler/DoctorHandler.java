package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Doctor;
import service.DoctorService;
import util.JsonUtil;

import java.util.List;
import java.util.Map;

public class DoctorHandler extends BaseHandler {

    private final DoctorService doctorService = new DoctorService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Path patterns:
        // /api/doctors
        // /api/doctors/{id}
        // /api/doctors/{id}/dashboard
        // /api/doctors/department/{deptId}
        String[] parts = path.split("/");

        if ("GET".equalsIgnoreCase(method)) {
            if (parts.length > 4 && "department".equalsIgnoreCase(parts[3])) {
                int deptId = Integer.parseInt(parts[4]);
                List<Doctor> docs = doctorService.getDoctorsByDepartment(deptId);
                sendJson(exchange, 200, docs);
            } else if (parts.length > 4 && "dashboard".equalsIgnoreCase(parts[4])) {
                int docId = Integer.parseInt(parts[3]);
                Map<String, Object> dash = doctorService.getDoctorDashboard(docId);
                sendJson(exchange, 200, dash);
            } else if (parts.length > 3 && !parts[3].isEmpty()) {
                int docId = Integer.parseInt(parts[3]);
                Doctor doc = doctorService.getDoctorById(docId);
                if (doc == null) sendError(exchange, 404, "Doctor not found.");
                else sendJson(exchange, 200, doc);
            } else {
                Map<String, String> query = parseQueryParams(exchange);
                if (query.containsKey("departmentId")) {
                    int deptId = Integer.parseInt(query.get("departmentId"));
                    sendJson(exchange, 200, doctorService.getDoctorsByDepartment(deptId));
                } else {
                    sendJson(exchange, 200, doctorService.getAllDoctors());
                }
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);

            String name = JsonUtil.getString(req, "name");
            String email = JsonUtil.getString(req, "email");
            String password = JsonUtil.getString(req, "password");
            String phone = JsonUtil.getString(req, "phone");
            int departmentId = JsonUtil.getInt(req, "departmentId");
            String specialization = JsonUtil.getString(req, "specialization");
            String availability = JsonUtil.getString(req, "availability", "09:00 AM - 01:00 PM");

            Doctor doc = doctorService.createDoctor(name, email, password, phone, departmentId, specialization, availability);
            sendJson(exchange, 201, doc);
        } else if ("PUT".equalsIgnoreCase(method)) {
            if (parts.length < 4) {
                sendError(exchange, 400, "Doctor ID is required in URL.");
                return;
            }
            int docId = Integer.parseInt(parts[3]);
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);

            String name = req.containsKey("name") ? (String) req.get("name") : null;
            String phone = req.containsKey("phone") ? (String) req.get("phone") : null;
            int departmentId = JsonUtil.getInt(req, "departmentId", 0);
            String specialization = req.containsKey("specialization") ? (String) req.get("specialization") : null;
            String availability = req.containsKey("availability") ? (String) req.get("availability") : null;
            String status = req.containsKey("status") ? (String) req.get("status") : null;

            boolean ok = doctorService.updateDoctor(docId, name, phone, departmentId, specialization, availability, status);
            sendJson(exchange, 200, Map.of("updated", ok));
        } else if ("DELETE".equalsIgnoreCase(method)) {
            if (parts.length < 4) {
                sendError(exchange, 400, "Doctor ID is required in URL.");
                return;
            }
            int docId = Integer.parseInt(parts[3]);
            boolean ok = doctorService.deleteDoctor(docId);
            sendJson(exchange, 200, Map.of("deleted", ok));
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
