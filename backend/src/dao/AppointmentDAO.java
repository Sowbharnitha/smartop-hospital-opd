package dao;

import db.DatabaseConnection;
import model.Appointment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    private static final String BASE_SELECT =
        "SELECT a.appointment_id, a.patient_id, a.doctor_id, a.department_id, " +
        "a.appointment_date, a.appointment_time, a.status, a.created_at, " +
        "up.name AS patient_name, ud.name AS doctor_name, dep.department_name, " +
        "q.queue_number, q.status AS queue_status " +
        "FROM appointments a " +
        "JOIN patients p ON a.patient_id = p.patient_id " +
        "JOIN users up ON p.user_id = up.user_id " +
        "JOIN doctors d ON a.doctor_id = d.doctor_id " +
        "JOIN users ud ON d.user_id = ud.user_id " +
        "JOIN departments dep ON a.department_id = dep.department_id " +
        "LEFT JOIN queue q ON a.appointment_id = q.appointment_id ";

    public int create(Appointment appointment, Connection conn) throws SQLException {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, department_id, appointment_date, appointment_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, appointment.getPatientId());
            stmt.setInt(2, appointment.getDoctorId());
            stmt.setInt(3, appointment.getDepartmentId());
            stmt.setDate(4, appointment.getAppointmentDate());
            stmt.setTime(5, appointment.getAppointmentTime());
            stmt.setString(6, appointment.getStatus() != null ? appointment.getStatus() : "SCHEDULED");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    appointment.setAppointmentId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public Appointment findById(int appointmentId) throws SQLException {
        String sql = BASE_SELECT + "WHERE a.appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, appointmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Appointment> findByPatientId(int patientId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE a.patient_id = ? ORDER BY a.appointment_date DESC, a.appointment_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Appointment> findByDoctorIdAndDate(int doctorId, Date date) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE a.doctor_id = ? AND a.appointment_date = ? ORDER BY a.appointment_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int appointmentId, String status) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, appointmentId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean hasExistingActiveAppointment(int patientId, int doctorId, Date date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND doctor_id = ? AND appointment_date = ? AND status != 'CANCELLED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            stmt.setInt(2, doctorId);
            stmt.setDate(3, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public int countAppointmentsToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE appointment_date = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment(
            rs.getInt("appointment_id"),
            rs.getInt("patient_id"),
            rs.getInt("doctor_id"),
            rs.getInt("department_id"),
            rs.getDate("appointment_date"),
            rs.getTime("appointment_time"),
            rs.getString("status"),
            rs.getTimestamp("created_at")
        );
        a.setPatientName(rs.getString("patient_name"));
        a.setDoctorName(rs.getString("doctor_name"));
        a.setDepartmentName(rs.getString("department_name"));
        a.setQueueNumber(rs.getString("queue_number"));
        a.setQueueStatus(rs.getString("queue_status"));
        return a;
    }
}
