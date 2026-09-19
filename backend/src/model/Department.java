package model;

/**
 * Represents a hospital medical department.
 */
public class Department {
    private int departmentId;
    private String departmentName;
    private String description;
    private String status; // ACTIVE, INACTIVE

    public Department() {}

    public Department(int departmentId, String departmentName, String description, String status) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.description = description;
        this.status = status;
    }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
