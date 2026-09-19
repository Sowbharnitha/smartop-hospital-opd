package service;

import dao.AppointmentDAO;
import dao.DepartmentDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import dao.QueueDAO;
import db.DatabaseConnection;
import model.Appointment;
import model.Department;
import model.Doctor;
import model.Patient;
import model.QueueEntry;
import util.QueueNumberGenerator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentService {

    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final QueueDAO queueDAO = new QueueDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public Map<String, Object> bookAppointment(int patientId, int doctorId, int departmentId,
                                              String dateStr, String timeStr) throws Exception {
        // 1. Validate inputs
        Patient patient = patientDAO.findById(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Invalid patient ID: " + patientId);
        }

        Doctor doctor = doctorDAO.findById(doctorId);
        if (doctor == null || !"ACTIVE".equalsIgnoreCase(doctor.getStatus())) {
            throw new IllegalArgumentException("Selected doctor is not available or inactive.");
        }

        Department dept = departmentDAO.findById(departmentId);
        if (dept == null) {
            throw new IllegalArgumentException("Invalid department ID: " + departmentId);
        }

        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment date is required.");
        }
        Date appointmentDate = Date.valueOf(dateStr.trim());
        LocalDate requestedLocalDate = appointmentDate.toLocalDate();
        if (requestedLocalDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Appointment date cannot be in the past.");
        }

        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = "10:00:00";
        }
        if (timeStr.length() == 5) {
            timeStr = timeStr + ":00";
        }
        Time appointmentTime = Time.valueOf(timeStr);

        // 2. Prevent duplicate active appointments for same patient + doctor on same day
        if (appointmentDAO.hasExistingActiveAppointment(patientId, doctorId, appointmentDate)) {
            throw new IllegalStateException("You already have an active appointment scheduled with Dr. " +
                doctor.getName() + " on " + appointmentDate);
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 3. Generate Queue Number & Position
            int nextSequence = queueDAO.getNextQueueSequence(doctorId, appointmentDate, conn);
            String doctorPrefix = QueueNumberGenerator.getDoctorPrefix(doctor.getName());
            String queueNumber = QueueNumberGenerator.formatQueueNumber(doctorPrefix, nextSequence);

            // 4. Create Appointment record
            Appointment appt = new Appointment(0, patientId, doctorId, departmentId,
                appointmentDate, appointmentTime, "SCHEDULED", null);
            int appointmentId = appointmentDAO.create(appt, conn);
            if (appointmentId <= 0) {
                throw new SQLException("Failed to save appointment record.");
            }

            // 5. Create Queue record
            QueueEntry queueEntry = new QueueEntry(0, appointmentId, doctorId, patientId,
                queueNumber, nextSequence, appointmentDate, "WAITING", null, null, null);
            int queueId = queueDAO.create(queueEntry, conn);
            if (queueId <= 0) {
                throw new SQLException("Failed to generate queue record.");
            }

            conn.commit();

            // Calculate patients ahead
            int patientsAhead = queueDAO.countPatientsAhead(doctorId, appointmentDate, nextSequence);

            Map<String, Object> result = new HashMap<>();
            result.put("appointmentId", appointmentId);
            result.put("queueId", queueId);
            result.put("queueNumber", queueNumber);
            result.put("queuePosition", nextSequence);
            result.put("patientsAhead", patientsAhead);
            result.put("appointmentDate", appointmentDate.toString());
            result.put("appointmentTime", timeStr);
            result.put("doctorName", doctor.getName());
            result.put("departmentName", dept.getDepartmentName());
            result.put("status", "SCHEDULED");
            result.put("queueStatus", "WAITING");

            return result;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<Appointment> getPatientAppointments(int patientId) throws SQLException {
        return appointmentDAO.findByPatientId(patientId);
    }

    public List<Appointment> getDoctorAppointments(int doctorId, String dateStr) throws SQLException {
        Date date = dateStr != null && !dateStr.isEmpty() ? Date.valueOf(dateStr) : Date.valueOf(LocalDate.now());
        return appointmentDAO.findByDoctorIdAndDate(doctorId, date);
    }

    public Appointment getAppointmentById(int appointmentId) throws SQLException {
        return appointmentDAO.findById(appointmentId);
    }
}
