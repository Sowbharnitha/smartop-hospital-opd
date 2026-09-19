/**
 * SmartOP Doctor Operations Module
 * Handles Doctor Queue Management, Calling Patients, Consultations, and Live Polling.
 */

let doctorPollTimer = null;

async function initDoctorDashboard() {
    const session = checkSession('DOCTOR');
    if (!session) return;
    populateUserDisplay(session);

    const doctorId = session.doctorId;
    if (!doctorId) {
        showToast("Doctor profile not found. Please re-login.", "error");
        return;
    }

    // Set today's date in header
    const dateEl = document.getElementById('doctor-today-date');
    if (dateEl) {
        dateEl.textContent = new Date().toLocaleDateString('en-US', {
            weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
        });
    }

    await loadDoctorDashboard(doctorId);

    // Setup 5s Polling for incoming patient updates
    if (doctorPollTimer) clearInterval(doctorPollTimer);
    doctorPollTimer = setInterval(() => {
        loadDoctorDashboard(doctorId, true);
    }, 5000);

    // Attach Action Listeners
    const btnCallNext = document.getElementById('btn-call-next');
    if (btnCallNext) {
        btnCallNext.addEventListener('click', () => callNextPatient(doctorId));
    }
}

async function loadDoctorDashboard(doctorId, isPolling = false) {
    try {
        const data = await apiRequest(`/api/doctors/${doctorId}/dashboard`);
        if (!data) return;

        // Update KPI counters
        updateKpi('doc-stat-total', data.totalAppointments || 0);
        updateKpi('doc-stat-waiting', data.waitingPatients || 0);
        updateKpi('doc-stat-completed', data.completedConsultations || 0);

        // Render Current Active Patient
        renderCurrentPatientStation(data.currentPatient);

        // Render Queue Table
        renderDoctorQueueTable(data.queue || [], doctorId);
    } catch (err) {
        if (!isPolling) {
            console.error("Error loading doctor dashboard:", err);
            showToast("Failed to refresh queue: " + err.message, "error");
        }
    }
}

function updateKpi(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function renderCurrentPatientStation(currentPatient) {
    const cardEl = document.getElementById('current-patient-station');
    if (!cardEl) return;

    if (!currentPatient) {
        cardEl.innerHTML = `
            <div style="text-align: center; padding: 2.5rem 1rem; color: var(--text-muted);">
                <div style="font-size: 2.5rem; margin-bottom: 0.5rem; opacity: 0.6;">🩺</div>
                <h3 style="color: var(--primary-900); font-weight: 700; margin-bottom: 0.25rem;">No Active Consultation</h3>
                <p style="font-size: 0.9rem;">Click "Call Next Patient" to bring the next waiting patient into the consultation room.</p>
            </div>
        `;
        return;
    }

    const isCalled = currentPatient.status === 'CALLED';
    const isInConsult = currentPatient.status === 'IN_CONSULTATION';

    cardEl.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1.5rem;">
            <div>
                <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem;">
                    <span class="badge badge-${currentPatient.status.toLowerCase()}">${currentPatient.status.replace('_', ' ')}</span>
                    <span style="font-size: 0.85rem; color: var(--text-muted);">Token No.</span>
                </div>
                <h2 style="font-size: 2.2rem; font-weight: 900; color: var(--primary-900); line-height: 1;">
                    ${currentPatient.queueNumber}
                </h2>
                <h3 style="font-size: 1.25rem; font-weight: 700; color: var(--teal-800); margin-top: 0.5rem;">
                    ${currentPatient.patientName}
                </h3>
                <p style="font-size: 0.9rem; color: var(--text-muted);">
                    Scheduled Time: <strong>${currentPatient.appointmentTime ? currentPatient.appointmentTime.substring(0, 5) : 'N/A'}</strong>
                </p>
            </div>

            <div style="display: flex; gap: 0.75rem; flex-wrap: wrap; align-items: center;">
                ${isCalled ? `
                    <button class="btn btn-success" onclick="startConsultation(${currentPatient.queueId})">
                        ▶ Start Consultation
                    </button>
                    <button class="btn btn-outline" onclick="skipPatient(${currentPatient.queueId})">
                        Skip Patient
                    </button>
                ` : ''}

                ${isInConsult ? `
                    <button class="btn btn-primary" onclick="completeConsultation(${currentPatient.queueId})">
                        ✓ Complete Consultation
                    </button>
                ` : ''}
            </div>
        </div>
    `;
}

function renderDoctorQueueTable(queueList, doctorId) {
    const tbody = document.getElementById('doctor-queue-tbody');
    if (!tbody) return;

    if (!queueList || queueList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 2rem; color: var(--text-muted);">No patients in today's OPD queue.</td></tr>`;
        return;
    }

    tbody.innerHTML = queueList.map((entry, idx) => {
        const isWaiting = entry.status === 'WAITING';
        const isCalled = entry.status === 'CALLED';
        const isInConsult = entry.status === 'IN_CONSULTATION';

        let actionHtml = '--';
        if (isWaiting) {
            actionHtml = `
                <button class="btn btn-sm btn-outline" onclick="callSpecificPatient(${entry.queueId})">Call</button>
                <button class="btn btn-sm btn-outline" style="color: var(--text-muted);" onclick="skipPatient(${entry.queueId})">Skip</button>
            `;
        } else if (isCalled) {
            actionHtml = `
                <button class="btn btn-sm btn-success" onclick="startConsultation(${entry.queueId})">Start</button>
                <button class="btn btn-sm btn-outline" onclick="skipPatient(${entry.queueId})">Skip</button>
            `;
        } else if (isInConsult) {
            actionHtml = `
                <button class="btn btn-sm btn-primary" onclick="completeConsultation(${entry.queueId})">Finish</button>
            `;
        }

        return `
            <tr>
                <td><strong>#${entry.queuePosition}</strong></td>
                <td><span style="font-family: monospace; font-weight: 800; font-size: 1.05rem; color: var(--teal-800);">${entry.queueNumber}</span></td>
                <td><strong>${entry.patientName}</strong></td>
                <td>${entry.appointmentTime ? entry.appointmentTime.substring(0, 5) : '--'}</td>
                <td><span class="badge badge-${entry.status.toLowerCase()}">${entry.status.replace('_', ' ')}</span></td>
                <td>${actionHtml}</td>
            </tr>
        `;
    }).join('');
}

// ==========================================
// Doctor Queue Actions
// ==========================================
async function callNextPatient(doctorId) {
    try {
        const called = await apiRequest(`/api/queue/doctor/${doctorId}/call-next`, { method: 'POST' });
        showToast(`Called patient: ${called.patientName} (${called.queueNumber})`, "success");
        await loadDoctorDashboard(doctorId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function callSpecificPatient(queueId) {
    try {
        await apiRequest(`/api/queue/${queueId}/call`, { method: 'PUT' });
        showToast("Patient status updated to IN_CONSULTATION.", "success");
        const session = getStoredSession();
        if (session) await loadDoctorDashboard(session.doctorId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function startConsultation(queueId) {
    try {
        await apiRequest(`/api/queue/${queueId}/start`, { method: 'PUT' });
        showToast("Consultation started.", "success");
        const session = getStoredSession();
        if (session) await loadDoctorDashboard(session.doctorId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function completeConsultation(queueId) {
    try {
        await apiRequest(`/api/queue/${queueId}/complete`, { method: 'PUT' });
        showToast("Consultation completed successfully! Next patient can now be called.", "success");
        const session = getStoredSession();
        if (session) await loadDoctorDashboard(session.doctorId);
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function skipPatient(queueId) {
    if (!confirm("Are you sure you want to skip this patient?")) return;
    try {
        await apiRequest(`/api/queue/${queueId}/skip`, { method: 'PUT' });
        showToast("Patient marked as SKIPPED.", "info");
        const session = getStoredSession();
        if (session) await loadDoctorDashboard(session.doctorId);
    } catch (err) {
        showToast(err.message, "error");
    }
}
