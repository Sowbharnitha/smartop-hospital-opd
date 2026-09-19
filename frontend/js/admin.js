/**
 * SmartOP Admin Operations Module
 * Handles Statistics, Doctor Management, Department Management, and Queue Monitoring.
 */

let adminPollTimer = null;

async function initAdminDashboard() {
    const session = checkSession('ADMIN');
    if (!session) return;
    populateUserDisplay(session);

    // Initial data load
    await loadAdminStats();
    await loadActiveQueues();
    await loadDoctorsList();
    await loadDepartmentsList();

    // Start 5s live polling for queues & stats
    if (adminPollTimer) clearInterval(adminPollTimer);
    adminPollTimer = setInterval(async () => {
        await loadAdminStats(true);
        await loadActiveQueues(true);
    }, 5000);
}

// ==========================================
// 1. Stats & Queue Monitor
// ==========================================
async function loadAdminStats(isPolling = false) {
    try {
        const stats = await apiRequest('/api/admin/statistics');
        if (!stats) return;

        updateStatEl('stat-patients', stats.totalPatients || 0);
        updateStatEl('stat-doctors', stats.totalDoctors || 0);
        updateStatEl('stat-depts', stats.totalDepartments || 0);
        updateStatEl('stat-appts', stats.todayAppointments || 0);
        updateStatEl('stat-waiting', stats.waitingPatients || 0);
        updateStatEl('stat-completed', stats.completedConsultations || 0);
    } catch (err) {
        if (!isPolling) console.error("Error loading stats:", err);
    }
}

function updateStatEl(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

async function loadActiveQueues(isPolling = false) {
    const tbody = document.getElementById('admin-queues-tbody');
    if (!tbody) return;

    try {
        const queues = await apiRequest('/api/admin/queues');
        if (!queues || queues.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding: 2rem; color: var(--text-muted);">No active OPD queues found.</td></tr>`;
            return;
        }

        tbody.innerHTML = queues.map(q => `
            <tr>
                <td><strong>${q.departmentName}</strong></td>
                <td><strong>${q.doctorName}</strong></td>
                <td><span style="font-family: monospace; font-weight: 800; font-size: 1.1rem; color: var(--teal-800);">${q.currentQueueNumber || 'None'}</span></td>
                <td>${q.currentPatientName || 'None'}</td>
                <td><span class="badge badge-waiting">${q.waitingCount} waiting</span></td>
                <td><span class="badge badge-completed">${q.completedCount} done</span></td>
                <td><span class="badge badge-${(q.status || 'ACTIVE').toLowerCase()}">${q.status}</span></td>
            </tr>
        `).join('');
    } catch (err) {
        if (!isPolling) console.error("Error loading active queues:", err);
    }
}

// ==========================================
// 2. Doctor Management
// ==========================================
async function loadDoctorsList() {
    const tbody = document.getElementById('admin-doctors-tbody');
    if (!tbody) return;

    try {
        const doctors = await apiRequest('/api/doctors');
        tbody.innerHTML = doctors.map(d => `
            <tr>
                <td><strong>#${d.doctorId}</strong></td>
                <td><strong>${d.name}</strong></td>
                <td>${d.departmentName}</td>
                <td>${d.specialization}</td>
                <td>${d.email}<br><small style="color:var(--text-muted);">${d.phone}</small></td>
                <td><span class="badge badge-${d.status.toLowerCase()}">${d.status}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="openEditDoctorModal(${d.doctorId}, '${encodeURIComponent(JSON.stringify(d))}')">Edit</button>
                    ${d.status === 'ACTIVE' ? `
                        <button class="btn btn-sm btn-danger" onclick="toggleDoctorStatus(${d.doctorId})">Deactivate</button>
                    ` : ''}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error("Error loading doctors:", err);
    }
}

async function handleAddDoctorSubmit(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('doc-name').value,
        email: document.getElementById('doc-email').value,
        password: document.getElementById('doc-password').value,
        phone: document.getElementById('doc-phone').value,
        departmentId: parseInt(document.getElementById('doc-dept-select').value),
        specialization: document.getElementById('doc-spec').value,
        availability: document.getElementById('doc-avail').value || '09:00 AM - 01:00 PM'
    };

    try {
        await apiRequest('/api/doctors', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast("Doctor added successfully!", "success");
        closeModal('doctor-modal');
        document.getElementById('doctor-form').reset();
        await loadDoctorsList();
        await loadAdminStats();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function toggleDoctorStatus(docId) {
    if (!confirm("Are you sure you want to deactivate this doctor?")) return;
    try {
        await apiRequest(`/api/doctors/${docId}`, { method: 'DELETE' });
        showToast("Doctor deactivated.", "info");
        await loadDoctorsList();
        await loadAdminStats();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ==========================================
// 3. Department Management
// ==========================================
async function loadDepartmentsList() {
    const tbody = document.getElementById('admin-depts-tbody');
    const docDeptSelect = document.getElementById('doc-dept-select');
    if (!tbody) return;

    try {
        const depts = await apiRequest('/api/departments?all=true');

        // Populate table
        tbody.innerHTML = depts.map(d => `
            <tr>
                <td><strong>#${d.departmentId}</strong></td>
                <td><strong>${d.departmentName}</strong></td>
                <td>${d.description || '--'}</td>
                <td><span class="badge badge-${d.status.toLowerCase()}">${d.status}</span></td>
                <td>
                    ${d.status === 'ACTIVE' ? `
                        <button class="btn btn-sm btn-danger" onclick="toggleDeptStatus(${d.departmentId})">Deactivate</button>
                    ` : ''}
                </td>
            </tr>
        `).join('');

        // Populate doctor modal dropdown
        if (docDeptSelect) {
            docDeptSelect.innerHTML = '<option value="">-- Select Department --</option>' +
                depts.filter(d => d.status === 'ACTIVE').map(d => `<option value="${d.departmentId}">${d.departmentName}</option>`).join('');
        }
    } catch (err) {
        console.error("Error loading departments:", err);
    }
}

async function handleAddDepartmentSubmit(e) {
    e.preventDefault();
    const payload = {
        departmentName: document.getElementById('dept-name-input').value,
        description: document.getElementById('dept-desc-input').value
    };

    try {
        await apiRequest('/api/departments', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        showToast("Department created successfully!", "success");
        closeModal('dept-modal');
        document.getElementById('dept-form').reset();
        await loadDepartmentsList();
        await loadAdminStats();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function toggleDeptStatus(deptId) {
    if (!confirm("Are you sure you want to deactivate this department?")) return;
    try {
        await apiRequest(`/api/departments/${deptId}`, { method: 'DELETE' });
        showToast("Department deactivated.", "info");
        await loadDepartmentsList();
        await loadAdminStats();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Modal Helpers
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}
