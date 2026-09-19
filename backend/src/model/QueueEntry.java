package model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Represents a live queue tracking entry in the Outpatient Department.
 */
public class QueueEntry {
    private int queueId;
    private int appointmentId;
    private int doctorId;
    private int patientId;
    private String queueNumber;
    private int queuePosition;
    private Date queueDate;
    private String status; // WAITING, CALLED, IN_CONSULTATION, COMPLETED, SKIPPED, CANCELLED
    private Timestamp calledAt;
    private Timestamp consultationStartedAt;
    private Timestamp completedAt;

    // Joined metadata for UI display
    private String patientName;
    private String doctorName;
    private String departmentName;
    private Time appointmentTime;
    private int patientsAhead;

    public QueueEntry() {}

    public QueueEntry(int queueId, int appointmentId, int doctorId, int patientId,
                      String queueNumber, int queuePosition, Date queueDate, String status,
                      Timestamp calledAt, Timestamp consultationStartedAt, Timestamp completedAt) {
        this.queueId = queueId;
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.queueNumber = queueNumber;
        this.queuePosition = queuePosition;
        this.queueDate = queueDate;
        this.status = status;
        this.calledAt = calledAt;
        this.consultationStartedAt = consultationStartedAt;
        this.completedAt = completedAt;
    }

    public int getQueueId() { return queueId; }
    public void setQueueId(int queueId) { this.queueId = queueId; }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getQueueNumber() { return queueNumber; }
    public void setQueueNumber(String queueNumber) { this.queueNumber = queueNumber; }

    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }

    public Date getQueueDate() { return queueDate; }
    public void setQueueDate(Date queueDate) { this.queueDate = queueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCalledAt() { return calledAt; }
    public void setCalledAt(Timestamp calledAt) { this.calledAt = calledAt; }

    public Timestamp getConsultationStartedAt() { return consultationStartedAt; }
    public void setConsultationStartedAt(Timestamp consultationStartedAt) { this.consultationStartedAt = consultationStartedAt; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Time getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(Time appointmentTime) { this.appointmentTime = appointmentTime; }

    public int getPatientsAhead() { return patientsAhead; }
    public void setPatientsAhead(int patientsAhead) { this.patientsAhead = patientsAhead; }
}
