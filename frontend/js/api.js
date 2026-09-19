/**
 * SmartOP Centralized API Communication Helper
 * Pure Vanilla JavaScript (Fetch API)
 */

// Dynamically determine the backend base URL
// If frontend is served via Java server (http://localhost:8080/), origin is empty string or window.location.origin
const API_BASE_URL = (window.location.protocol === 'file:')
    ? 'http://localhost:8080'
    : window.location.origin;

/**
 * Centralized HTTP request helper
 * @param {string} endpoint - e.g. '/api/doctors'
 * @param {object} options - fetch options (method, headers, body)
 * @returns {Promise<any>} parsed JSON data payload
 */
async function apiRequest(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    // Attach Bearer token from localStorage if present
    const session = getStoredSession();
    if (session && session.token) {
        headers['Authorization'] = `Bearer ${session.token}`;
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(url, config);
        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMsg = (data && data.error) ? data.error : `HTTP Error ${response.status}: ${response.statusText}`;
            throw new Error(errorMsg);
        }

        return data ? (data.data !== undefined ? data.data : data) : null;
    } catch (err) {
        console.error(`[API Error] ${options.method || 'GET'} ${url}:`, err.message);
        throw err;
    }
}

/**
 * Toast notifications for user feedback
 */
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
    toast.innerHTML = `<span style="font-weight: bold;">${icon}</span><span>${message}</span>`;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function getStoredSession() {
    try {
        const raw = localStorage.getItem('smartop_session');
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        return null;
    }
}

function setStoredSession(session) {
    if (session) {
        localStorage.setItem('smartop_session', JSON.stringify(session));
    } else {
        localStorage.removeItem('smartop_session');
    }
}

function clearStoredSession() {
    localStorage.removeItem('smartop_session');
}
