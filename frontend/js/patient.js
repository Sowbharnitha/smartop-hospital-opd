/**
 * SmartOP Patient Operations Module
 * Handles Dashboard, Dynamic Booking Flow, and Live Queue Tracking.
 */

let patientPollTimer = null;

// ==========================================
// 1. Patient Dashboard Logic
// ==========================================
async function initPatientDashboard() {
    const session = checkSession('PATIENT');
    if (!session) return;
    populateUserDisplay(session);

    const patientId = session.patientId;
    if (!patientId) {
        showToast("Patient profile error. Please re-login.", "error");
        return;
    }

    // Load initial data
    await loadPatientTodayQueue(patientId);
    await loadPatientAppointmentHistory(patientId);

    // Start 5-second polling for live queue updates
    if (patientPollTimer) clearInterval(patientPollTimer);
    patientPollTimer = setInterval(() => {
        loadPatientTodayQueue(patientId, true);
    }, 5000);
}

async function loadPatientTodayQueue(patientId, isPolling = false) {
    try {
        const queueEntry = await apiRequest(`/api/queue/patient/${patientId}`);
        renderTodayQueueBanner(queueEntry);
    } catch (err) {
        if (!isPolling) {
            console.error("Error loading today's queue:", err);
        }
    }
}

function renderTodayQueueBanner(entry) {
    const bannerEl = document.getElementById('patient-active-queue-banner');
    const noApptEl = document.getElementById('patient-no-appt-banner');
    if (!bannerEl) return;

    if (!entry) {
        bannerEl.style.display = 'none';
        if (noApptEl) noApptEl.style.display = 'block';
        return;
    }

    if (noApptEl) noApptEl.style.display = 'none';
    bannerEl.style.display = 'grid';

    // Populate banner details
    const doctorEl = document.getElementById('ticket-doctor');
    const deptEl = document.getElementById('ticket-department');
    const timeEl = document.getElementById('ticket-time');
    const qnumEl = document.getElementById('ticket-queue-number');
    const qposEl = document.getElementById('ticket-queue-position');
    const aheadEl = document.getElementById('ticket-patients-ahead');
    const statusEl = document.getElementById('ticket-status');

    if (doctorEl) doctorEl.textContent = entry.doctorName || '--';
    if (deptEl) deptEl.textContent = entry.departmentName || '--';
    if (timeEl) timeEl.textContent = entry.appointmentTime ? entry.appointmentTime.substring(0, 5) : '--';
    if (qnumEl) qnumEl.textContent = entry.queueNumber || '--';
    if (qposEl) qposEl.textContent = entry.queuePosition || '1';
    if (aheadEl) aheadEl.textContent = entry.patientsAhead !== undefined ? entry.patientsAhead : '0';

    if (statusEl) {
        const status = entry.status || 'WAITING';
        statusEl.className = `badge badge-${status.toLowerCase()}`;
        statusEl.textContent = status.replace('_', ' ');

        // Visual alert if patient is CALLED
        if (status === 'CALLED') {
            bannerEl.style.boxShadow = '0 0 25px rgba(239, 68, 68, 0.6)';
        } else {
            bannerEl.style.boxShadow = '0 12px 30px rgba(15, 118, 110, 0.25)';
        }
    }
}

async function loadPatientAppointmentHistory(patientId) {
    const tableBody = document.getElementById('patient-history-tbody');
    if (!tableBody) return;

    try {
        const appointments = await apiRequest(`/api/appointments/patient/${patientId}`);
        if (!appointments || appointments.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted); padding: 2rem;">No appointments found.</td></tr>`;
            return;
        }

        tableBody.innerHTML = appointments.map(appt => `
            <tr>
                <td><strong>#${appt.appointmentId}</strong></td>
                <td>${appt.appointmentDate}</td>
                <td>${appt.appointmentTime ? appt.appointmentTime.substring(0, 5) : ''}</td>
                <td>${appt.doctorName}</td>
                <td>${appt.departmentName}</td>
                <td><span class="badge badge-${(appt.queueStatus || appt.status || 'SCHEDULED').toLowerCase()}">${(appt.queueNumber ? appt.queueNumber + ' • ' : '')}${(appt.queueStatus || appt.status)}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        console.error("Error loading appointments:", err);
    }
}

// ==========================================
// 2. Book Appointment Flow
// ==========================================
async function initBookingPage() {
    const session = checkSession('PATIENT');
    if (!session) return;
    populateUserDisplay(session);

    // Restrict date picker to today or future
    const dateInput = document.getElementById('appt-date');
    if (dateInput) {
        const todayStr = new Date().toISOString().split('T')[0];
        dateInput.min = todayStr;
        dateInput.value = todayStr;
    }

    await loadDepartmentOptions();

    const deptSelect = document.getElementById('dept-select');
    if (deptSelect) {
        deptSelect.addEventListener('change', async (e) => {
            const deptId = e.target.value;
            await loadDoctorOptions(deptId);
        });
    }

    const bookingForm = document.getElementById('booking-form');
    if (bookingForm) {
        bookingForm.addEventListener('submit', handleBookingSubmit);
    }
}

async function loadDepartmentOptions() {
    const deptSelect = document.getElementById('dept-select');
    if (!deptSelect) return;

    try {
        const departments = await apiRequest('/api/departments');
        deptSelect.innerHTML = '<option value="">-- Choose Department --</option>' +
            departments.map(d => `<option value="${d.departmentId}">${d.departmentName}</option>`).join('');
    } catch (err) {
        showToast("Error loading departments: " + err.message, "error");
    }
}

async function loadDoctorOptions(deptId) {
    const docSelect = document.getElementById('doctor-select');
    if (!docSelect) return;

    if (!deptId) {
        docSelect.innerHTML = '<option value="">-- Select department first --</option>';
        docSelect.disabled = true;
        return;
    }

    try {
        docSelect.disabled = true;
        docSelect.innerHTML = '<option value="">Loading doctors...</option>';

        const doctors = await apiRequest(`/api/doctors/department/${deptId}`);
        if (!doctors || doctors.length === 0) {
            docSelect.innerHTML = '<option value="">No doctors available in this department</option>';
            return;
        }

        docSelect.innerHTML = '<option value="">-- Choose Doctor --</option>' +
            doctors.map(d => `<option value="${d.doctorId}">${d.name} (${d.specialization})</option>`).join('');
        docSelect.disabled = false;
    } catch (err) {
        docSelect.innerHTML = '<option value="">Failed to load doctors</option>';
        showToast("Error loading doctors: " + err.message, "error");
    }
}

async function handleBookingSubmit(e) {
    e.preventDefault();
    const session = getStoredSession();
    if (!session || !session.patientId) {
        showToast("Session expired. Please log in again.", "error");
        return;
    }

    const deptId = document.getElementById('dept-select').value;
    const doctorId = document.getElementById('doctor-select').value;
    const date = document.getElementById('appt-date').value;
    const time = document.getElementById('appt-time').value;

    if (!deptId || !doctorId || !date || !time) {
        showToast("Please fill in all booking fields.", "error");
        return;
    }

    const submitBtn = document.getElementById('btn-book-submit');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Generating Queue Ticket...';
    }

    try {
        const payload = {
            patientId: session.patientId,
            doctorId: parseInt(doctorId),
            departmentId: parseInt(deptId),
            appointmentDate: date,
            appointmentTime: time
        };

        const result = await apiRequest('/api/appointments', {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        // Show Success Modal with Generated Queue Number
        showBookingSuccessModal(result);
    } catch (err) {
        showToast(err.message, "error");
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Confirm & Generate Queue Number';
        }
    }
}

function showBookingSuccessModal(data) {
    const modal = document.getElementById('booking-success-modal');
    if (!modal) {
        alert(`Appointment Booked! Your Queue Number is: ${data.queueNumber}`);
        window.location.href = '/patient/dashboard.html';
        return;
    }

    document.getElementById('modal-queue-number').textContent = data.queueNumber;
    document.getElementById('modal-doctor-name').textContent = data.doctorName;
    document.getElementById('modal-dept-name').textContent = data.departmentName;
    document.getElementById('modal-appt-date').textContent = data.appointmentDate;
    document.getElementById('modal-appt-time').textContent = data.appointmentTime.substring(0, 5);
    document.getElementById('modal-ahead-count').textContent = data.patientsAhead;

    modal.classList.add('active');
}
