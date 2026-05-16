/**
 * SupremeAI Authentication Helper (Modular Firebase v10+)
 *
 * Centralizes Firebase initialization and token management using modern SDK.
 */

import { initializeApp, getApp, getApps } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import { 
    getAuth, 
    onAuthStateChanged, 
    signOut, 
    getIdToken 
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';

window.AuthHelper = {
    firebaseConfig: null,
    app: null,
    auth: null,

    /**
     * Load Firebase configuration from a secure source
     */
    async loadFirebaseConfig() {
        if (window.__FIREBASE_CONFIG__) {
            return window.__FIREBASE_CONFIG__;
        }

        try {
            const response = await fetch('/api/config/firebase', {
                credentials: 'include'
            });
            if (response.ok) {
                return await response.json();
            }
        } catch (e) {
            console.warn('Failed to fetch Firebase config from backend:', e.message);
        }

        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            console.error('Firebase configuration missing.');
            throw new Error('Firebase config not available. Run backend server or inject config.');
        }

        throw new Error('Firebase configuration not available');
    },

    /**
     * Initialize Firebase if not already initialized
     */
    async initFirebase() {
        if (this.app) return this.app;

        try {
            this.firebaseConfig = await this.loadFirebaseConfig();
            
            // Avoid re-initialization
            if (getApps().length === 0) {
                this.app = initializeApp(this.firebaseConfig);
            } else {
                this.app = getApp();
            }
            
            this.auth = getAuth(this.app);
            return this.app;
        } catch (error) {
            console.error("Firebase init failed:", error);
            throw error;
        }
    },

    _authInitialized: false,
    _authPromise: null,

    /**
     * Initialize Firebase and wait for auth state
     */
    async initializeAuth(redirectToLoginIfNotAuth = true) {
        if (this._authPromise) return this._authPromise;

        await this.initFirebase();

        this._authPromise = new Promise((resolve) => {
            onAuthStateChanged(this.auth, (user) => {
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
     * Get the current Firebase ID token
     */
    async getIdToken() {
        await this.initializeAuth(false);
        const user = this.auth?.currentUser;
        if (user) {
            try {
                return await getIdToken(user);
            } catch (e) {
                console.error("Error getting ID token", e);
                return null;
            }
        }
        return null;
    },

    getUser() {
        const userStr = localStorage.getItem('supremeai_user');
        return userStr ? JSON.parse(userStr) : null;
    },
    
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

        if (this.auth) {
            try {
                await signOut(this.auth);
            } catch (e) {
                console.warn("Sign out failed", e);
            }
        }
        window.location.href = '/login.html';
    },
    
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
                console.warn("401 Unauthorized - redirecting to login");
                this.logout();
            }
            
            return response;
        } catch (error) {
            console.error('API call failed:', error);
            throw error;
        }
    },
    
    displayUserInfo(avatarElementId, usernameElementId) {
        const user = this.getUser();
        const fbUser = this.auth?.currentUser;

        const username = user?.username || fbUser?.displayName || fbUser?.email?.split('@')[0] || 'Admin';
        const email = user?.email || fbUser?.email || '';
        
        if (usernameElementId) {
            const elem = document.getElementById(usernameElementId);
            if (elem) elem.textContent = `${username} (Admin)`;
        }
        
        if (avatarElementId) {
            const elem = document.getElementById(avatarElementId);
            if (elem) {
                elem.textContent = username.charAt(0).toUpperCase();
                elem.title = email;
            }
        }
    },
    
    applyPermissionVisibility() {
        document.querySelectorAll('[data-admin-only]').forEach(el => {
            el.style.display = 'block';
        });
    }
};

window.apiCall = async (url, options = {}) => {
    return AuthHelper.apiCall(url, options);
};
