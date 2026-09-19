package service;

import dao.DepartmentDAO;
import model.Department;

import java.sql.SQLException;
import java.util.List;

public class DepartmentService {

    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public List<Department> getAllDepartments() throws SQLException {
        return departmentDAO.findAll();
    }

    public List<Department> getActiveDepartments() throws SQLException {
        return departmentDAO.findActive();
    }

    public Department getDepartmentById(int id) throws SQLException {
        return departmentDAO.findById(id);
    }

    public Department createDepartment(String name, String description) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be empty.");
        }
        if (departmentDAO.findByName(name.trim()) != null) {
            throw new IllegalStateException("A department with this name already exists.");
        }
        Department dept = new Department(0, name.trim(), description != null ? description.trim() : "", "ACTIVE");
        int id = departmentDAO.create(dept);
        if (id <= 0) {
            throw new SQLException("Failed to create department.");
        }
        return dept;
    }

    public boolean updateDepartment(int id, String name, String description, String status) throws Exception {
        Department existing = departmentDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Department not found.");
        }
        if (name != null && !name.trim().isEmpty()) {
            Department withSameName = departmentDAO.findByName(name.trim());
            if (withSameName != null && withSameName.getDepartmentId() != id) {
                throw new IllegalStateException("Another department already has this name.");
            }
            existing.setDepartmentName(name.trim());
        }
        if (description != null) existing.setDescription(description.trim());
        if (status != null) existing.setStatus(status.trim().toUpperCase());
        return departmentDAO.update(existing);
    }

    public boolean deleteDepartment(int id) throws SQLException {
        return departmentDAO.delete(id);
    }
}
