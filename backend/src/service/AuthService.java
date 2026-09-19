package service;

import dao.DoctorDAO;
import dao.PatientDAO;
import dao.UserDAO;
import db.DatabaseConnection;
import model.Doctor;
import model.Patient;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();

    // In-memory token store: token -> SessionData
    private static final Map<String, Map<String, Object>> activeSessions = new ConcurrentHashMap<>();

    public Map<String, Object> login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            throw new SecurityException("Invalid email or password.");
        }

        if (!PasswordUtil.verifyPassword(password.trim(), user.getPassword())) {
            throw new SecurityException("Invalid email or password.");
        }

        // Build authenticated session payload
        Map<String, Object> session = new HashMap<>();
        session.put("userId", user.getUserId());
        session.put("name", user.getName());
        session.put("email", user.getEmail());
        session.put("role", user.getRole());
        session.put("phone", user.getPhone());

        if ("PATIENT".equalsIgnoreCase(user.getRole())) {
            Patient p = patientDAO.findByUserId(user.getUserId());
            if (p != null) {
                session.put("patientId", p.getPatientId());
                session.put("dateOfBirth", p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");
                session.put("gender", p.getGender());
                session.put("address", p.getAddress());
            }
        } else if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
            Doctor d = doctorDAO.findByUserId(user.getUserId());
            if (d != null) {
                session.put("doctorId", d.getDoctorId());
                session.put("departmentId", d.getDepartmentId());
                session.put("departmentName", d.getDepartmentName());
                session.put("specialization", d.getSpecialization());
                session.put("availability", d.getAvailability());
            }
        }

        String token = "TOKEN-" + UUID.randomUUID().toString();
        session.put("token", token);
        activeSessions.put(token, session);

        return session;
    }

    public Map<String, Object> registerPatient(String name, String email, String password, String phone,
                                               String dobStr, String gender, String address) throws Exception {
        // Validation
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (phone == null || !phone.matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
        }
        if (dobStr == null || dobStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (gender == null || (!gender.equalsIgnoreCase("MALE") && !gender.equalsIgnoreCase("FEMALE") && !gender.equalsIgnoreCase("OTHER"))) {
            throw new IllegalArgumentException("Please select a valid gender (MALE, FEMALE, OTHER).");
        }

        // Duplicate checks
        if (userDAO.emailExists(email, 0)) {
            throw new IllegalStateException("An account with this email already exists.");
        }
        if (userDAO.phoneExists(phone, 0)) {
            throw new IllegalStateException("An account with this phone number already exists.");
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        Date dob = Date.valueOf(dobStr.trim());

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            User user = new User(0, name.trim(), email.trim().toLowerCase(), hashedPassword, "PATIENT", phone.trim(), null);
            int userId = userDAO.create(user, conn);
            if (userId <= 0) {
                throw new SQLException("Failed to create user record.");
            }

            Patient patient = new Patient(0, userId, dob, gender.toUpperCase(), address != null ? address.trim() : "");
            int patientId = patientDAO.create(patient, conn);
            if (patientId <= 0) {
                throw new SQLException("Failed to create patient profile.");
            }

            conn.commit();

            // Auto-login after successful registration
            return login(email, password);
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

    public static Map<String, Object> getSession(String token) {
        if (token == null) return null;
        return activeSessions.get(token.trim());
    }

    public static void invalidateSession(String token) {
        if (token != null) {
            activeSessions.remove(token.trim());
        }
    }
}
