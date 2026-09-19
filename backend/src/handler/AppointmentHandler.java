package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Appointment;
import service.AppointmentService;
import util.JsonUtil;

import java.util.List;
import java.util.Map;

public class AppointmentHandler extends BaseHandler {

    private final AppointmentService appointmentService = new AppointmentService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String[] parts = path.split("/");

        if ("POST".equalsIgnoreCase(method)) {
            String body = readRequestBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);

            int patientId = JsonUtil.getInt(req, "patientId");
            int doctorId = JsonUtil.getInt(req, "doctorId");
            int departmentId = JsonUtil.getInt(req, "departmentId");
            String dateStr = JsonUtil.getString(req, "appointmentDate");
            String timeStr = JsonUtil.getString(req, "appointmentTime", "10:00:00");

            Map<String, Object> result = appointmentService.bookAppointment(patientId, doctorId, departmentId, dateStr, timeStr);
            sendJson(exchange, 201, result);
        } else if ("GET".equalsIgnoreCase(method)) {
            if (parts.length > 4 && "patient".equalsIgnoreCase(parts[3])) {
                int patientId = Integer.parseInt(parts[4]);
                List<Appointment> list = appointmentService.getPatientAppointments(patientId);
                sendJson(exchange, 200, list);
            } else if (parts.length > 4 && "doctor".equalsIgnoreCase(parts[3])) {
                int doctorId = Integer.parseInt(parts[4]);
                Map<String, String> query = parseQueryParams(exchange);
                String dateStr = query.get("date");
                List<Appointment> list = appointmentService.getDoctorAppointments(doctorId, dateStr);
                sendJson(exchange, 200, list);
            } else if (parts.length > 3 && !parts[3].isEmpty()) {
                int id = Integer.parseInt(parts[3]);
                Appointment appt = appointmentService.getAppointmentById(id);
                if (appt == null) sendError(exchange, 404, "Appointment not found.");
                else sendJson(exchange, 200, appt);
            } else {
                sendError(exchange, 400, "Please specify patient ID or doctor ID to list appointments.");
            }
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
