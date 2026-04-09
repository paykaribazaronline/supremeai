/**
 * SupremeAI Authentication Helper (cookie-session mode)
 *
 * Uses backend HttpOnly cookie session only.
 * No token is stored in localStorage or attached as Bearer header.
 */

const AuthHelper = {
    
    /**
     * Get the stored user object
     */
    getUser() {
        const userStr = localStorage.getItem('supremeai_user');
        return userStr ? JSON.parse(userStr) : null;
    },
    
    // Cookie auth does not require Authorization headers from frontend.
    getAuthHeader() {
        return {};
    },

    setUser(user) {
        localStorage.setItem('supremeai_user', JSON.stringify(user));
    },
    
    /**
     * Clear all authentication data
     */
    logout() {
        localStorage.removeItem('supremeai_user');
        localStorage.removeItem('supremeai_remembered_username');
        fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
            .catch(() => {})
            .finally(() => {
                window.location.href = '/login.html';
            });
    },
    
    /**
     * Make an authenticated API call with automatic token refresh
     */
    async apiCall(url, options = {}) {
        try {
            options.headers = {
                ...options.headers,
                ...this.getAuthHeader(),
                'Content-Type': options.headers?.['Content-Type'] || 'application/json'
            };
            options.credentials = options.credentials || 'include';
            
            const response = await fetch(url, options);
            
            if (response.status === 401) {
                this.logout();
            }
            
            return response;
            
        } catch (error) {
            console.error('API call failed:', error);
            throw error;
        }
    },
    
    async initializeAuth(redirectToLoginIfNotAuth = true) {
        try {
            const res = await fetch('/api/auth/me', { credentials: 'include' });
            if (!res.ok) {
                if (redirectToLoginIfNotAuth && !window.location.pathname.includes('login')) {
                    window.location.href = '/login.html';
                }
                return false;
            }
            const data = await res.json().catch(() => ({}));
            if (data.user) {
                this.setUser(data.user);
            }
            return true;
        } catch (_) {
            if (redirectToLoginIfNotAuth && !window.location.pathname.includes('login')) {
                window.location.href = '/login.html';
            }
            return false;
        }
    },
    
    /**
     * Display user info in the UI
     */
    displayUserInfo(avatarElementId, usernameElementId) {
        const user = this.getUser();
        if (!user) return;
        
        // Update username
        if (usernameElementId) {
            const elem = document.getElementById(usernameElementId);
            if (elem) {
                elem.textContent = `${user.username} (Admin)`;
            }
        }
        
        // Update avatar
        if (avatarElementId) {
            const elem = document.getElementById(avatarElementId);
            if (elem) {
                const initial = user.username.charAt(0).toUpperCase();
                elem.textContent = initial;
                elem.title = user.email;
            }
        }
    },
    
    /**
     * Show/hide UI elements based on permissions
     */
    applyPermissionVisibility() {
        // All authenticated users are admins in this system
        document.querySelectorAll('[data-admin-only]').forEach(el => {
            el.style.display = 'block';
        });
    },
    
    /**
     * Show logout button and handle logout
     */
    addLogoutButton(containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        
        const btn = document.createElement('button');
        btn.className = 'btn btn-danger';
        btn.innerHTML = '🚪 Logout';
        btn.onclick = () => this.logout();
        container.appendChild(btn);
    }
};

/**
 * On page load, check authentication
 */
document.addEventListener('DOMContentLoaded', () => {
    AuthHelper.initializeAuth(true).then((ok) => {
        if (!ok) return;
        AuthHelper.displayUserInfo('userAvatar', 'userName');
        AuthHelper.applyPermissionVisibility();
        const logoutBtn = document.querySelector('[data-action="logout"]');
        if (logoutBtn) {
            logoutBtn.onclick = () => AuthHelper.logout();
        }
    });
});

/**
 * Global error handler for API calls
 */
window.apiCall = async (url, options = {}) => {
    return AuthHelper.apiCall(url, options);
};
