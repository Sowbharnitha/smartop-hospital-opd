package dao;

import db.DatabaseConnection;
import model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    private static final String BASE_SELECT =
        "SELECT p.patient_id, p.user_id, p.date_of_birth, p.gender, p.address, " +
        "u.name, u.email, u.phone " +
        "FROM patients p " +
        "JOIN users u ON p.user_id = u.user_id ";

    public int create(Patient patient, Connection conn) throws SQLException {
        String sql = "INSERT INTO patients (user_id, date_of_birth, gender, address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, patient.getUserId());
            stmt.setDate(2, patient.getDateOfBirth());
            stmt.setString(3, patient.getGender());
            stmt.setString(4, patient.getAddress());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    patient.setPatientId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public Patient findById(int patientId) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Patient findByUserId(int userId) throws SQLException {
        String sql = BASE_SELECT + "WHERE p.user_id = ?";
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

    public List<Patient> findAll() throws SQLException {
        List<Patient> list = new ArrayList<>();
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

    public int countPatients() throws SQLException {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient(
            rs.getInt("patient_id"),
            rs.getInt("user_id"),
            rs.getDate("date_of_birth"),
            rs.getString("gender"),
            rs.getString("address")
        );
        p.setName(rs.getString("name"));
        p.setEmail(rs.getString("email"));
        p.setPhone(rs.getString("phone"));
        return p;
    }
}
