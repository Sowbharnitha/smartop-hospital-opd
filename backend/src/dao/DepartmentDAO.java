package dao;

import db.DatabaseConnection;
import model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public List<Department> findAll() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name, description, status FROM departments ORDER BY department_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Department> findActive() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name, description, status FROM departments WHERE status = 'ACTIVE' ORDER BY department_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Department findById(int id) throws SQLException {
        String sql = "SELECT department_id, department_name, description, status FROM departments WHERE department_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Department findByName(String name) throws SQLException {
        String sql = "SELECT department_id, department_name, description, status FROM departments WHERE LOWER(department_name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(Department dept) throws SQLException {
        String sql = "INSERT INTO departments (department_name, description, status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dept.getDepartmentName());
            stmt.setString(2, dept.getDescription());
            stmt.setString(3, dept.getStatus() != null ? dept.getStatus() : "ACTIVE");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    dept.setDepartmentId(rs.getInt(1));
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public boolean update(Department dept) throws SQLException {
        String sql = "UPDATE departments SET department_name = ?, description = ?, status = ? WHERE department_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dept.getDepartmentName());
            stmt.setString(2, dept.getDescription());
            stmt.setString(3, dept.getStatus());
            stmt.setInt(4, dept.getDepartmentId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        // Soft-delete or toggle status to INACTIVE to preserve historic integrity
        String sql = "UPDATE departments SET status = 'INACTIVE' WHERE department_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countDepartments() throws SQLException {
        String sql = "SELECT COUNT(*) FROM departments WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
            rs.getInt("department_id"),
            rs.getString("department_name"),
            rs.getString("description"),
            rs.getString("status")
        );
    }
}
