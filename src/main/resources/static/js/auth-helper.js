/**
 * SupremeAI Authentication Helper
 * 
 * Provides functions for:
 * - Token management (get, set, clear)
 * - User info retrieval
 * - Permission checking
 * - Automatic redirect to login if unauthorized
 * - Token refresh logic
 */

const AuthHelper = {
    
    /**
     * Get the stored JWT token
     */
    getToken() {
        return localStorage.getItem('supremeai_token');
    },
    
    /**
     * Get the stored refresh token
     */
    getRefreshToken() {
        return localStorage.getItem('supremeai_refresh_token');
    },
    
    /**
     * Get the stored user object
     */
    getUser() {
        const userStr = localStorage.getItem('supremeai_user');
        return userStr ? JSON.parse(userStr) : null;
    },
    
    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return !!this.getToken();
    },
    
    /**
     * Get authorization header for API calls
     */
    getAuthHeader() {
        const token = this.getToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    },
    
    /**
     * Set authenticated user and tokens
     */
    setTokens(token, refreshToken, user) {
        localStorage.setItem('supremeai_token', token);
        localStorage.setItem('supremeai_refresh_token', refreshToken);
        localStorage.setItem('supremeai_user', JSON.stringify(user));
    },
    
    /**
     * Clear all authentication data
     */
    logout() {
        localStorage.removeItem('supremeai_token');
        localStorage.removeItem('supremeai_refresh_token');
        localStorage.removeItem('supremeai_user');
        localStorage.removeItem('supremeai_remembered_username');
        localStorage.removeItem('supremeai_remembered_password');
        window.location.href = '/login.html';
    },
    
    /**
     * Refresh the access token using refresh token
     */
    async refreshAccessToken() {
        try {
            const refreshToken = this.getRefreshToken();
            if (!refreshToken) {
                this.logout();
                return false;
            }
            
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });
            
            if (!response.ok) {
                this.logout();
                return false;
            }
            
            const data = await response.json();
            this.setTokens(data.token, data.refreshToken, data.user);
            return true;
            
        } catch (error) {
            console.error('Token refresh failed:', error);
            this.logout();
            return false;
        }
    },
    
    /**
     * Make an authenticated API call with automatic token refresh
     */
    async apiCall(url, options = {}) {
        try {
            // Add authorization header
            options.headers = {
                ...options.headers,
                ...this.getAuthHeader(),
                'Content-Type': options.headers?.['Content-Type'] || 'application/json'
            };
            
            const response = await fetch(url, options);
            
            // If 401, try to refresh token and retry once
            if (response.status === 401) {
                const refreshed = await this.refreshAccessToken();
                if (refreshed) {
                    // Retry the request with new token
                    options.headers = {
                        ...options.headers,
                        ...this.getAuthHeader()
                    };
                    return fetch(url, options);
                } else {
                    this.logout();
                    return response;
                }
            }
            
            return response;
            
        } catch (error) {
            console.error('API call failed:', error);
            throw error;
        }
    },
    
    /**
     * Initialize authentication on page load
     * Redirects to login if not authenticated
     */
    initializeAuth(redirectToLoginIfNotAuth = true) {
        if (!this.isAuthenticated()) {
            if (redirectToLoginIfNotAuth && !window.location.pathname.includes('login')) {
                window.location.href = '/login.html';
            }
            return false;
        }
        return true;
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
    // Initialize auth (redirect to login if needed)
    AuthHelper.initializeAuth(true);
    
    // Display user info in top bar
    AuthHelper.displayUserInfo('userAvatar', 'userName');
    
    // Apply permission-based UI visibility
    AuthHelper.applyPermissionVisibility();
    
    // Handle logout button if it exists
    const logoutBtn = document.querySelector('[data-action="logout"]');
    if (logoutBtn) {
        logoutBtn.onclick = () => AuthHelper.logout();
    }
});

/**
 * Global error handler for API calls
 */
window.apiCall = async (url, options = {}) => {
    return AuthHelper.apiCall(url, options);
};
