package dao;

import db.DatabaseConnection;
import model.QueueEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueDAO {

    private static final String BASE_SELECT =
        "SELECT q.queue_id, q.appointment_id, q.doctor_id, q.patient_id, " +
        "q.queue_number, q.queue_position, q.queue_date, q.status, " +
        "q.called_at, q.consultation_started_at, q.completed_at, " +
        "up.name AS patient_name, ud.name AS doctor_name, dep.department_name, " +
        "a.appointment_time " +
        "FROM queue q " +
        "JOIN appointments a ON q.appointment_id = a.appointment_id " +
        "JOIN patients p ON q.patient_id = p.patient_id " +
        "JOIN users up ON p.user_id = up.user_id " +
        "JOIN doctors d ON q.doctor_id = d.doctor_id " +
        "JOIN users ud ON d.user_id = ud.user_id " +
        "JOIN departments dep ON d.department_id = dep.department_id ";

    public int create(QueueEntry entry, Connection conn) throws SQLException {
        String sql = "INSERT INTO queue (appointment_id, doctor_id, patient_id, queue_number, queue_position, queue_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, entry.getAppointmentId());
            stmt.setInt(2, entry.getDoctorId());
            stmt.setInt(3, entry.getPatientId());
            stmt.setString(4, entry.getQueueNumber());
            stmt.setInt(5, entry.getQueuePosition());
            stmt.setDate(6, entry.getQueueDate());
            stmt.setString(7, entry.getStatus() != null ? entry.getStatus() : "WAITING");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setQueueId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public int getNextQueueSequence(int doctorId, Date date, Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(queue_position), 0) + 1 FROM queue WHERE doctor_id = ? AND queue_date = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 1;
    }

    public QueueEntry findById(int queueId) throws SQLException {
        String sql = BASE_SELECT + "WHERE q.queue_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, queueId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<QueueEntry> getQueueForDoctor(int doctorId, Date date) throws SQLException {
        List<QueueEntry> list = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE q.doctor_id = ? AND q.queue_date = ? ORDER BY q.queue_position ASC";
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

    public QueueEntry getPatientTodayQueue(int patientId, Date date) throws SQLException {
        String sql = BASE_SELECT + "WHERE q.patient_id = ? AND q.queue_date = ? ORDER BY q.queue_id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    QueueEntry entry = mapRow(rs);
                    int ahead = countPatientsAhead(entry.getDoctorId(), entry.getQueueDate(), entry.getQueuePosition());
                    entry.setPatientsAhead(ahead);
                    return entry;
                }
            }
        }
        return null;
    }

    public int countPatientsAhead(int doctorId, Date date, int currentPosition) throws SQLException {
        // Count WAITING or CALLED or IN_CONSULTATION before this position
        String sql = "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND queue_date = ? " +
                     "AND queue_position < ? AND status IN ('WAITING', 'CALLED', 'IN_CONSULTATION')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            stmt.setInt(3, currentPosition);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public QueueEntry getNextWaitingPatient(int doctorId, Date date) throws SQLException {
        String sql = BASE_SELECT + "WHERE q.doctor_id = ? AND q.queue_date = ? AND q.status = 'WAITING' ORDER BY q.queue_position ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public QueueEntry getCurrentActivePatient(int doctorId, Date date) throws SQLException {
        String sql = BASE_SELECT + "WHERE q.doctor_id = ? AND q.queue_date = ? AND q.status IN ('IN_CONSULTATION', 'CALLED') ORDER BY q.queue_position ASC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean updateStatus(int queueId, String status) throws SQLException {
        String sql;
        if ("CALLED".equalsIgnoreCase(status)) {
            sql = "UPDATE queue SET status = 'CALLED', called_at = NOW() WHERE queue_id = ?";
        } else if ("IN_CONSULTATION".equalsIgnoreCase(status)) {
            sql = "UPDATE queue SET status = 'IN_CONSULTATION', consultation_started_at = NOW() WHERE queue_id = ?";
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            sql = "UPDATE queue SET status = 'COMPLETED', completed_at = NOW() WHERE queue_id = ?";
        } else if ("SKIPPED".equalsIgnoreCase(status)) {
            sql = "UPDATE queue SET status = 'SKIPPED' WHERE queue_id = ?";
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            sql = "UPDATE queue SET status = 'CANCELLED' WHERE queue_id = ?";
        } else {
            sql = "UPDATE queue SET status = ? WHERE queue_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setInt(2, queueId);
                return stmt.executeUpdate() > 0;
            }
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, queueId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countWaitingForDoctorToday(int doctorId, Date date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND queue_date = ? AND status = 'WAITING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countCompletedForDoctorToday(int doctorId, Date date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM queue WHERE doctor_id = ? AND queue_date = ? AND status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            stmt.setDate(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countWaitingTotalToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM queue WHERE queue_date = CURDATE() AND status = 'WAITING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countCompletedTotalToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM queue WHERE queue_date = CURDATE() AND status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public List<Map<String, Object>> getActiveQueuesSummary() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT d.doctor_id, ud.name AS doctor_name, dep.department_name, " +
            "COUNT(CASE WHEN q.status = 'WAITING' THEN 1 END) AS waiting_count, " +
            "COUNT(CASE WHEN q.status = 'COMPLETED' THEN 1 END) AS completed_count, " +
            "MAX(CASE WHEN q.status IN ('IN_CONSULTATION', 'CALLED') THEN q.queue_number END) AS current_queue_number, " +
            "MAX(CASE WHEN q.status IN ('IN_CONSULTATION', 'CALLED') THEN up.name END) AS current_patient_name " +
            "FROM doctors d " +
            "JOIN users ud ON d.user_id = ud.user_id " +
            "JOIN departments dep ON d.department_id = dep.department_id " +
            "LEFT JOIN queue q ON d.doctor_id = q.doctor_id AND q.queue_date = CURDATE() " +
            "LEFT JOIN patients p ON q.patient_id = p.patient_id " +
            "LEFT JOIN users up ON p.user_id = up.user_id " +
            "WHERE d.status = 'ACTIVE' " +
            "GROUP BY d.doctor_id, ud.name, dep.department_name " +
            "ORDER BY dep.department_name, ud.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("doctorId", rs.getInt("doctor_id"));
                map.put("doctorName", rs.getString("doctor_name"));
                map.put("departmentName", rs.getString("department_name"));
                map.put("waitingCount", rs.getInt("waiting_count"));
                map.put("completedCount", rs.getInt("completed_count"));
                map.put("currentQueueNumber", rs.getString("current_queue_number") != null ? rs.getString("current_queue_number") : "None");
                map.put("currentPatientName", rs.getString("current_patient_name") != null ? rs.getString("current_patient_name") : "None");
                map.put("status", rs.getInt("waiting_count") > 0 || rs.getString("current_queue_number") != null ? "ACTIVE" : "IDLE");
                list.add(map);
            }
        }
        return list;
    }

    private QueueEntry mapRow(ResultSet rs) throws SQLException {
        QueueEntry q = new QueueEntry(
            rs.getInt("queue_id"),
            rs.getInt("appointment_id"),
            rs.getInt("doctor_id"),
            rs.getInt("patient_id"),
            rs.getString("queue_number"),
            rs.getInt("queue_position"),
            rs.getDate("queue_date"),
            rs.getString("status"),
            rs.getTimestamp("called_at"),
            rs.getTimestamp("consultation_started_at"),
            rs.getTimestamp("completed_at")
        );
        q.setPatientName(rs.getString("patient_name"));
        q.setDoctorName(rs.getString("doctor_name"));
        q.setDepartmentName(rs.getString("department_name"));
        q.setAppointmentTime(rs.getTime("appointment_time"));
        return q;
    }
}
