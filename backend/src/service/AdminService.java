package service;

import dao.AppointmentDAO;
import dao.DepartmentDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import dao.QueueDAO;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {

    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();
    private final QueueDAO queueDAO = new QueueDAO();

    public Map<String, Object> getStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPatients", patientDAO.countPatients());
        stats.put("totalDoctors", doctorDAO.countDoctors());
        stats.put("totalDepartments", departmentDAO.countDepartments());
        stats.put("todayAppointments", appointmentDAO.countAppointmentsToday());
        stats.put("waitingPatients", queueDAO.countWaitingTotalToday());
        stats.put("completedConsultations", queueDAO.countCompletedTotalToday());

        List<Map<String, Object>> activeQueues = queueDAO.getActiveQueuesSummary();
        long activeCount = activeQueues.stream().filter(q -> "ACTIVE".equals(q.get("status"))).count();
        stats.put("activeQueues", activeCount);

        return stats;
    }

    public List<Map<String, Object>> getActiveQueues() throws SQLException {
        return queueDAO.getActiveQueuesSummary();
    }
}
