package model;

import java.sql.Date;

/**
 * Represents a registered hospital Patient.
 */
public class Patient {
    private int patientId;
    private int userId;
    private Date dateOfBirth;
    private String gender; // MALE, FEMALE, OTHER
    private String address;

    // Joined user fields
    private String name;
    private String email;
    private String phone;

    public Patient() {}

    public Patient(int patientId, int userId, Date dateOfBirth, String gender, String address) {
        this.patientId = patientId;
        this.userId = userId;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = address;
    }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Date getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Date dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
