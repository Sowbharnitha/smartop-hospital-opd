package handler;

import com.sun.net.httpserver.HttpExchange;
import model.QueueEntry;
import service.QueueService;

import java.util.List;
import java.util.Map;

public class QueueHandler extends BaseHandler {

    private final QueueService queueService = new QueueService();

    @Override
    protected void execute(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String[] parts = path.split("/");

        // URL structures:
        // GET /api/queue?doctorId=1&date=2026-09-19
        // GET /api/queue/patient/{patientId}
        // GET /api/queue/{id}
        // POST /api/queue/doctor/{doctorId}/call-next
        // PUT /api/queue/{id}/call
        // PUT /api/queue/{id}/start
        // PUT /api/queue/{id}/complete
        // PUT /api/queue/{id}/skip

        if ("GET".equalsIgnoreCase(method)) {
            if (parts.length > 4 && "patient".equalsIgnoreCase(parts[3])) {
                int patientId = Integer.parseInt(parts[4]);
                QueueEntry entry = queueService.getPatientTodayQueue(patientId);
                sendJson(exchange, 200, entry);
            } else if (parts.length > 3 && !parts[3].isEmpty() && !"doctor".equalsIgnoreCase(parts[3])) {
                int queueId = Integer.parseInt(parts[3]);
                QueueEntry entry = queueService.getQueueEntryById(queueId);
                if (entry == null) sendError(exchange, 404, "Queue entry not found.");
                else sendJson(exchange, 200, entry);
            } else {
                Map<String, String> query = parseQueryParams(exchange);
                if (query.containsKey("doctorId")) {
                    int doctorId = Integer.parseInt(query.get("doctorId"));
                    String dateStr = query.get("date");
                    List<QueueEntry> queue = queueService.getQueueForDoctor(doctorId, dateStr);
                    sendJson(exchange, 200, queue);
                } else {
                    sendError(exchange, 400, "Query parameter 'doctorId' is required for doctor queue.");
                }
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            if (parts.length > 4 && "doctor".equalsIgnoreCase(parts[3]) && "call-next".equalsIgnoreCase(parts[5])) {
                int doctorId = Integer.parseInt(parts[4]);
                QueueEntry called = queueService.callNextPatient(doctorId);
                sendJson(exchange, 200, called);
            } else {
                sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } else if ("PUT".equalsIgnoreCase(method)) {
            if (parts.length >= 5) {
                int queueId = Integer.parseInt(parts[3]);
                String action = parts[4].toLowerCase();
                QueueEntry updated;
                switch (action) {
                    case "call":
                        updated = queueService.startConsultation(queueId); // or queueService.callNextPatient
                        sendJson(exchange, 200, updated);
                        break;
                    case "start":
                        updated = queueService.startConsultation(queueId);
                        sendJson(exchange, 200, updated);
                        break;
                    case "complete":
                        updated = queueService.completeConsultation(queueId);
                        sendJson(exchange, 200, updated);
                        break;
                    case "skip":
                        updated = queueService.skipPatient(queueId);
                        sendJson(exchange, 200, updated);
                        break;
                    case "cancel":
                        updated = queueService.cancelQueue(queueId);
                        sendJson(exchange, 200, updated);
                        break;
                    default:
                        sendError(exchange, 400, "Unknown queue action: " + action);
                }
            } else {
                sendError(exchange, 400, "Queue ID and action required in URL.");
            }
        } else {
            sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
