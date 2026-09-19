package service;

import dao.AppointmentDAO;
import dao.QueueDAO;
import model.QueueEntry;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class QueueService {

    private final QueueDAO queueDAO = new QueueDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    public List<QueueEntry> getQueueForDoctor(int doctorId, String dateStr) throws SQLException {
        Date date = dateStr != null && !dateStr.trim().isEmpty() ? Date.valueOf(dateStr.trim()) : Date.valueOf(LocalDate.now());
        return queueDAO.getQueueForDoctor(doctorId, date);
    }

    public QueueEntry getPatientTodayQueue(int patientId) throws SQLException {
        Date today = Date.valueOf(LocalDate.now());
        return queueDAO.getPatientTodayQueue(patientId, today);
    }

    public QueueEntry getQueueEntryById(int queueId) throws SQLException {
        return queueDAO.findById(queueId);
    }

    public QueueEntry callNextPatient(int doctorId) throws Exception {
        Date today = Date.valueOf(LocalDate.now());
        QueueEntry nextWaiting = queueDAO.getNextWaitingPatient(doctorId, today);
        if (nextWaiting == null) {
            throw new IllegalStateException("No waiting patients in the queue for today.");
        }

        boolean ok = queueDAO.updateStatus(nextWaiting.getQueueId(), "CALLED");
        if (!ok) {
            throw new SQLException("Failed to update queue status to CALLED.");
        }
        return queueDAO.findById(nextWaiting.getQueueId());
    }

    public QueueEntry startConsultation(int queueId) throws Exception {
        QueueEntry entry = queueDAO.findById(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("Queue record not found.");
        }
        if (!"CALLED".equalsIgnoreCase(entry.getStatus()) && !"WAITING".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalStateException("Cannot start consultation for patient with status: " + entry.getStatus());
        }

        boolean ok = queueDAO.updateStatus(queueId, "IN_CONSULTATION");
        if (!ok) throw new SQLException("Failed to transition queue status to IN_CONSULTATION.");

        appointmentDAO.updateStatus(entry.getAppointmentId(), "IN_PROGRESS");
        return queueDAO.findById(queueId);
    }

    public QueueEntry completeConsultation(int queueId) throws Exception {
        QueueEntry entry = queueDAO.findById(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("Queue record not found.");
        }

        boolean ok = queueDAO.updateStatus(queueId, "COMPLETED");
        if (!ok) throw new SQLException("Failed to complete consultation.");

        appointmentDAO.updateStatus(entry.getAppointmentId(), "COMPLETED");
        return queueDAO.findById(queueId);
    }

    public QueueEntry skipPatient(int queueId) throws Exception {
        QueueEntry entry = queueDAO.findById(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("Queue record not found.");
        }

        boolean ok = queueDAO.updateStatus(queueId, "SKIPPED");
        if (!ok) throw new SQLException("Failed to mark patient as SKIPPED.");

        return queueDAO.findById(queueId);
    }

    public QueueEntry cancelQueue(int queueId) throws Exception {
        QueueEntry entry = queueDAO.findById(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("Queue record not found.");
        }

        boolean ok = queueDAO.updateStatus(queueId, "CANCELLED");
        if (!ok) throw new SQLException("Failed to cancel queue entry.");

        appointmentDAO.updateStatus(entry.getAppointmentId(), "CANCELLED");
        return queueDAO.findById(queueId);
    }
}
