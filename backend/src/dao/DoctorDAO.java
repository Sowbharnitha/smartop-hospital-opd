package dao;

import db.DatabaseConnection;
import model.Doctor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDAO {

    private static final String BASE_SELECT =
        "SELECT d.doctor_id, d.user_id, d.department_id, d.specialization, d.availability, d.status, " +
        "u.name, u.email, u.phone, dep.department_name " +
        "FROM doctors d " +
        "JOIN users u ON d.user_id = u.user_id " +
        "JOIN departments dep ON d.department_id = dep.department_id ";

    public List<Doctor> findAll() throws SQLException {
        List<Doctor> list = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY u.name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Doctor> findByDepartment(int departmentId) throws SQLException {
        List<Doctor> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE d.department_id = ? AND d.status = 'ACTIVE' ORDER BY u.name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, departmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Doctor findById(int doctorId) throws SQLException {
        String sql = BASE_SELECT + "WHERE d.doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Doctor findByUserId(int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE d.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(Doctor doctor, Connection conn) throws SQLException {
        String sql = "INSERT INTO doctors (user_id, department_id, specialization, availability, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, doctor.getUserId());
            stmt.setInt(2, doctor.getDepartmentId());
            stmt.setString(3, doctor.getSpecialization());
            stmt.setString(4, doctor.getAvailability() != null ? doctor.getAvailability() : "09:00 AM - 01:00 PM");
            stmt.setString(5, doctor.getStatus() != null ? doctor.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    doctor.setDoctorId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Doctor doctor) throws SQLException {
        String sql = "UPDATE doctors SET department_id = ?, specialization = ?, availability = ?, status = ? WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctor.getDepartmentId());
            stmt.setString(2, doctor.getSpecialization());
            stmt.setString(3, doctor.getAvailability());
            stmt.setString(4, doctor.getStatus());
            stmt.setInt(5, doctor.getDoctorId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int doctorId) throws SQLException {
        String sql = "UPDATE doctors SET status = 'INACTIVE' WHERE doctor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countDoctors() throws SQLException {
        String sql = "SELECT COUNT(*) FROM doctors WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor doc = new Doctor(
            rs.getInt("doctor_id"),
            rs.getInt("user_id"),
            rs.getInt("department_id"),
            rs.getString("specialization"),
            rs.getString("availability"),
            rs.getString("status")
        );
        doc.setName(rs.getString("name"));
        doc.setEmail(rs.getString("email"));
        doc.setPhone(rs.getString("phone"));
        doc.setDepartmentName(rs.getString("department_name"));
        return doc;
    }
}
