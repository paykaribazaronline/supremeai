/**
 * SupremeAI Authentication Helper (Bearer token mode)
 *
 * Centralizes Firebase initialization and token management.
 */

const AuthHelper = {
    firebaseConfig: null, // Dynamically loaded from backend or injected config

    /**
     * Load Firebase configuration from a secure source
     * Priority: 1) window.__FIREBASE_CONFIG__ (server-injected), 2) /api/config/firebase, 3) local dev fallback
     */
    async loadFirebaseConfig() {
        // 1. Check for server-injected config (most secure)
        if (window.__FIREBASE_CONFIG__) {
            return window.__FIREBASE_CONFIG__;
        }

        // 2. Fetch from backend config endpoint
        try {
            const response = await fetch('/api/config/firebase', {
                credentials: 'include'
            });
            if (response.ok) {
                const config = await response.json();
                return config;
            }
        } catch (e) {
            console.warn('Failed to fetch Firebase config from backend:', e.message);
        }

        // 3. Local development fallback only
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            console.warn('Using local Firebase config - set up proper config for production');
            return {
                apiKey: "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",
                authDomain: "supremeai-a.firebaseapp.com",
                databaseURL: "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",
                projectId: "supremeai-a",
                storageBucket: "supremeai-a.firebasestorage.app",
                messagingSenderId: "565236080752",
                appId: "1:565236080752:web:572bb9313db9afb355d4b5"
            };
        }

        throw new Error('Firebase configuration not available');
    },

    /**
     * Initialize Firebase if not already initialized
     */
    async initFirebase() {
        if (!window.firebase) {
            console.error("Firebase SDK not loaded");
            return null;
        }
        if (window.firebase.apps.length > 0) {
            return window.firebase;
        }
        // Load config dynamically
        this.firebaseConfig = await this.loadFirebaseConfig();
        window.firebase.initializeApp(this.firebaseConfig);
        return window.firebase;
    },

    _authInitialized: false,
    _authPromise: null,

    /**
     * Initialize Firebase if not already initialized and wait for auth state
     */
    async initializeAuth(redirectToLoginIfNotAuth = true) {
        if (this._authPromise) return this._authPromise;

        this.initFirebase();

        this._authPromise = new Promise((resolve) => {
            const fb = window.firebase;
            if (!fb) {
                console.error("Firebase not found");
                resolve(false);
                return;
            }

            fb.auth().onAuthStateChanged((user) => {
                this._authInitialized = true;
                if (user) {
                    localStorage.setItem('supremeai_firebase_authenticated', 'true');
                    resolve(true);
                } else {
                    localStorage.removeItem('supremeai_firebase_authenticated');
                    if (redirectToLoginIfNotAuth && !window.location.pathname.includes('login.html')) {
                        window.location.href = '/login.html';
                    }
                    resolve(false);
                }
            });
        });

        return this._authPromise;
    },

    /**
     * Get the current Firebase ID token, waiting for initialization if needed
     */
    async getIdToken() {
        await this.initializeAuth(false);
        const user = window.firebase?.auth()?.currentUser;
        if (user) {
            try {
                return await user.getIdToken();
            } catch (e) {
                console.error("Error getting ID token", e);
                return null;
            }
        }
        return null;
    },

    /**
     * Get the stored user object
     */
    getUser() {
        const userStr = localStorage.getItem('supremeai_user');
        return userStr ? JSON.parse(userStr) : null;
    },
    
    /**
     * Get Authorization headers with Bearer token
     */
    async getAuthHeader() {
        const token = await this.getIdToken();
        if (token) {
            return { 'Authorization': `Bearer ${token}` };
        }
        return {};
    },

    setUser(user) {
        localStorage.setItem('supremeai_user', JSON.stringify(user));
    },
    
    /**
     * Clear all authentication data
     */
    async logout() {
        localStorage.removeItem('supremeai_firebase_authenticated');
        localStorage.removeItem('supremeai_user');
        localStorage.removeItem('supremeai_remembered_username');
        try {
            await fetch('/api/auth/logout', {
                method: 'POST',
                credentials: 'include'
            });
        } catch (_) {}

        const fb = this.initFirebase();
        if (fb && fb.auth) {
            fb.auth().signOut().catch(() => {}).finally(() => {
                window.location.href = '/login.html';
            });
            return;
        }
        window.location.href = '/login.html';
    },
    
    /**
     * Make an authenticated API call with automatic token attach
     */
    async apiCall(url, options = {}) {
        try {
            const authHeader = await this.getAuthHeader();
            options.headers = {
                ...options.headers,
                ...authHeader,
                'Content-Type': options.headers?.['Content-Type'] || 'application/json'
            };
            options.credentials = options.credentials || 'include';
            
            const response = await fetch(url, options);
            
            if (response.status === 401) {
                // Potential token expiration or unauthorized
                console.warn("401 Unauthorized - redirecting to login");
                this.logout();
            }
            
            return response;
            
        } catch (error) {
            console.error('API call failed:', error);
            throw error;
        }
    },
    
    /**
     * Display user info in the UI
     */
    displayUserInfo(avatarElementId, usernameElementId) {
        const user = this.getUser();
        const fbUser = window.firebase?.auth()?.currentUser;

        const username = user?.username || fbUser?.displayName || fbUser?.email?.split('@')[0] || 'Admin';
        const email = user?.email || fbUser?.email || '';
        
        // Update username
        if (usernameElementId) {
            const elem = document.getElementById(usernameElementId);
            if (elem) {
                elem.textContent = `${username} (Admin)`;
            }
        }
        
        // Update avatar
        if (avatarElementId) {
            const elem = document.getElementById(avatarElementId);
            if (elem) {
                const initial = username.charAt(0).toUpperCase();
                elem.textContent = initial;
                elem.title = email;
            }
        }
    },
    
    /**
     * Show/hide UI elements based on permissions
     */
    applyPermissionVisibility() {
        document.querySelectorAll('[data-admin-only]').forEach(el => {
            el.style.display = 'block';
        });
    }
};

/**
 * On page load, check authentication
 */
document.addEventListener('DOMContentLoaded', () => {
    // Check if we are on a page that needs auth (not login.html)
    if (!window.location.pathname.includes('login.html')) {
        AuthHelper.initializeAuth(true).then((ok) => {
            if (!ok) return;
            AuthHelper.displayUserInfo('userAvatar', 'userName');
            AuthHelper.applyPermissionVisibility();

            const logoutBtn = document.querySelector('[data-action="logout"]');
            if (logoutBtn) {
                logoutBtn.onclick = (e) => {
                    e.preventDefault();
                    AuthHelper.logout();
                };
            }
        });
    }
});

/**
 * Global helper for API calls
 */
window.apiCall = async (url, options = {}) => {
    return AuthHelper.apiCall(url, options);
};

