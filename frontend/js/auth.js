/**
 * SmartOP Authentication Module
 * Handles login, registration, role validation, session check, and logout.
 */

async function loginUser(email, password, expectedRole = null) {
    if (!email || !password) {
        throw new Error("Please enter both email and password.");
    }

    const payload = { email: email.trim(), password: password.trim() };
    const session = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (expectedRole && session.role !== expectedRole) {
        throw new Error(`Access denied: This portal is for ${expectedRole}s only.`);
    }

    setStoredSession(session);
    return session;
}

async function registerPatient(formData) {
    const session = await apiRequest('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(formData)
    });

    setStoredSession(session);
    return session;
}

async function logoutUser() {
    try {
        await apiRequest('/api/auth/logout', { method: 'POST' });
    } catch (e) {
        // Continue even if network error
    }
    clearStoredSession();
    window.location.href = '/index.html';
}

function checkSession(requiredRole = null) {
    const session = getStoredSession();
    if (!session) {
        redirectToLogin(requiredRole);
        return null;
    }

    if (requiredRole && session.role !== requiredRole) {
        alert(`Unauthorized: You must be an ${requiredRole} to access this page.`);
        redirectToLogin(requiredRole);
        return null;
    }

    return session;
}

function redirectToLogin(role) {
    if (role === 'PATIENT') window.location.href = '/patient/login.html';
    else if (role === 'DOCTOR') window.location.href = '/doctor/login.html';
    else if (role === 'ADMIN') window.location.href = '/admin/login.html';
    else window.location.href = '/index.html';
}

function populateUserDisplay(session) {
    if (!session) return;
    const nameEls = document.querySelectorAll('.user-display-name');
    nameEls.forEach(el => el.textContent = session.name || 'User');

    const emailEls = document.querySelectorAll('.user-display-email');
    emailEls.forEach(el => el.textContent = session.email || '');

    const roleEls = document.querySelectorAll('.user-display-role');
    roleEls.forEach(el => el.textContent = session.role || '');

    const avatarEls = document.querySelectorAll('.user-avatar');
    avatarEls.forEach(el => {
        if (session.name) {
            el.textContent = session.name.charAt(0).toUpperCase();
        }
    });
}
