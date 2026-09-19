package model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Represents an Outpatient Department Appointment.
 */
public class Appointment {
    private int appointmentId;
    private int patientId;
    private int doctorId;
    private int departmentId;
    private Date appointmentDate;
    private Time appointmentTime;
    private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    private Timestamp createdAt;

    // Joined metadata
    private String patientName;
    private String doctorName;
    private String departmentName;
    private String queueNumber;
    private String queueStatus;

    public Appointment() {}

    public Appointment(int appointmentId, int patientId, int doctorId, int departmentId,
                       Date appointmentDate, Time appointmentTime, String status, Timestamp createdAt) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.departmentId = departmentId;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public Date getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(Date appointmentDate) { this.appointmentDate = appointmentDate; }

    public Time getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(Time appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getQueueNumber() { return queueNumber; }
    public void setQueueNumber(String queueNumber) { this.queueNumber = queueNumber; }

    public String getQueueStatus() { return queueStatus; }
    public void setQueueStatus(String queueStatus) { this.queueStatus = queueStatus; }
}
