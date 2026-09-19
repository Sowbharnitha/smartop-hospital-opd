package model;

/**
 * Represents a hospital Doctor, encapsulating professional specialization,
 * department details, and availability hours.
 */
public class Doctor {
    private int doctorId;
    private int userId;
    private int departmentId;
    private String specialization;
    private String availability;
    private String status; // ACTIVE, INACTIVE

    // Joined fields from User & Department tables
    private String name;
    private String email;
    private String phone;
    private String departmentName;

    public Doctor() {}

    public Doctor(int doctorId, int userId, int departmentId, String specialization, String availability, String status) {
        this.doctorId = doctorId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.specialization = specialization;
        this.availability = availability;
        this.status = status;
    }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
}
