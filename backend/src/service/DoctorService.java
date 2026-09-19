package service;

import dao.DoctorDAO;
import dao.QueueDAO;
import dao.UserDAO;
import db.DatabaseConnection;
import model.Doctor;
import model.QueueEntry;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorService {

    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final UserDAO userDAO = new UserDAO();
    private final QueueDAO queueDAO = new QueueDAO();

    public List<Doctor> getAllDoctors() throws SQLException {
        return doctorDAO.findAll();
    }

    public List<Doctor> getDoctorsByDepartment(int deptId) throws SQLException {
        return doctorDAO.findByDepartment(deptId);
    }

    public Doctor getDoctorById(int doctorId) throws SQLException {
        return doctorDAO.findById(doctorId);
    }

    public Doctor getDoctorByUserId(int userId) throws SQLException {
        return doctorDAO.findByUserId(userId);
    }

    public Doctor createDoctor(String name, String email, String password, String phone,
                               int departmentId, String specialization, String availability) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Doctor name is required.");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new IllegalArgumentException("Valid email is required.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (phone == null || !phone.matches("^[0-9]{10}$")) throw new IllegalArgumentException("Phone number must be 10 digits.");
        if (specialization == null || specialization.trim().isEmpty()) throw new IllegalArgumentException("Specialization is required.");

        if (userDAO.emailExists(email, 0)) throw new IllegalStateException("Email already registered.");
        if (userDAO.phoneExists(phone, 0)) throw new IllegalStateException("Phone already registered.");

        String hashedPassword = PasswordUtil.hashPassword(password);
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User user = new User(0, name.trim(), email.trim().toLowerCase(), hashedPassword, "DOCTOR", phone.trim(), null);
            int userId = userDAO.create(user, conn);
            if (userId <= 0) throw new SQLException("Failed to create doctor user record.");

            Doctor doc = new Doctor(0, userId, departmentId, specialization.trim(),
                availability != null ? availability.trim() : "09:00 AM - 01:00 PM", "ACTIVE");
            int doctorId = doctorDAO.create(doc, conn);
            if (doctorId <= 0) throw new SQLException("Failed to create doctor record.");

            conn.commit();
            return doctorDAO.findById(doctorId);
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

    public boolean updateDoctor(int doctorId, String name, String phone, int departmentId,
                                String specialization, String availability, String status) throws Exception {
        Doctor doc = doctorDAO.findById(doctorId);
        if (doc == null) throw new IllegalArgumentException("Doctor not found.");

        if (name != null || phone != null) {
            User user = userDAO.findById(doc.getUserId());
            if (user != null) {
                if (name != null) user.setName(name.trim());
                if (phone != null) user.setPhone(phone.trim());
                userDAO.update(user);
            }
        }

        if (departmentId > 0) doc.setDepartmentId(departmentId);
        if (specialization != null) doc.setSpecialization(specialization.trim());
        if (availability != null) doc.setAvailability(availability.trim());
        if (status != null) doc.setStatus(status.trim().toUpperCase());

        return doctorDAO.update(doc);
    }

    public boolean deleteDoctor(int doctorId) throws SQLException {
        return doctorDAO.delete(doctorId);
    }

    public Map<String, Object> getDoctorDashboard(int doctorId) throws SQLException {
        Doctor doc = doctorDAO.findById(doctorId);
        if (doc == null) throw new IllegalArgumentException("Doctor not found with ID: " + doctorId);

        Date today = Date.valueOf(LocalDate.now());
        List<QueueEntry> queue = queueDAO.getQueueForDoctor(doctorId, today);
        QueueEntry currentPatient = queueDAO.getCurrentActivePatient(doctorId, today);

        int totalAppointments = queue.size();
        int waitingCount = 0;
        int completedCount = 0;
        for (QueueEntry q : queue) {
            if ("WAITING".equalsIgnoreCase(q.getStatus())) waitingCount++;
            else if ("COMPLETED".equalsIgnoreCase(q.getStatus())) completedCount++;
        }

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("doctor", doc);
        dashboard.put("today", today.toString());
        dashboard.put("totalAppointments", totalAppointments);
        dashboard.put("waitingPatients", waitingCount);
        dashboard.put("completedConsultations", completedCount);
        dashboard.put("currentPatient", currentPatient);
        dashboard.put("queue", queue);

        return dashboard;
    }
}
