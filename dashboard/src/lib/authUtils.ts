import { AuthUser } from '../types';

const AUTH_TOKEN_KEY = 'supremeai_token';
const FIREBASE_USER_KEY = 'supremeai_user';

export const authUtils = {
  getToken(): string | null {
    const token = localStorage.getItem(AUTH_TOKEN_KEY) || sessionStorage.getItem(AUTH_TOKEN_KEY);
    return token || 'GUEST_MODE';
  },

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'admin' || user?.tier === 'admin' || user?.tier === 'ADMIN';
  },

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && token !== 'GUEST_MODE';
  },

  isGuest(): boolean {
    return this.getToken() === 'GUEST_MODE';
  },

  clearAuth() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(FIREBASE_USER_KEY);
    sessionStorage.removeItem(FIREBASE_USER_KEY);
    localStorage.removeItem('supremeai_refresh_token');
  },

  async logout() {
    try {
      const { firebaseSignOutFn } = await import('./firebase');
      await firebaseSignOutFn();
    } catch (e) {
      console.warn('Firebase logout failed', e);
    }
    this.clearAuth();
    window.location.href = '/login';
  },

  getCurrentUser(): AuthUser | null {
    const userStr = sessionStorage.getItem(FIREBASE_USER_KEY) || localStorage.getItem(FIREBASE_USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch (e) {
      return null;
    }
  },

  setToken(token: string) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    sessionStorage.setItem(AUTH_TOKEN_KEY, token);
  },

  setCurrentUser(user: any) {
    const userStr = JSON.stringify(user);
    localStorage.setItem(FIREBASE_USER_KEY, userStr);
    sessionStorage.setItem(FIREBASE_USER_KEY, userStr);
  },

  getAuthHeaders(): HeadersInit {
    const token = this.getToken();
    if (token && token !== 'GUEST_MODE') {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  },

  async fetchWithAuth(url: string, options: any = {}) {
    const token = this.getToken();
    const API_BASE = import.meta.env.VITE_API_URL || '';
    const fullUrl = url.startsWith('/') && API_BASE ? `${API_BASE}${url}` : url;
    
    const headers = new Headers(options.headers || {});
    if (token && token !== 'GUEST_MODE') {
      headers.set('Authorization', `Bearer ${token}`);
    } else {
      // In Guest mode, we still send GUEST_MODE header to tell the backend
      headers.set('X-Guest-Access', 'true');
    }

    let response = await fetch(fullUrl, { ...options, headers });

    // Handle Token Expiry
    if (response.status === 401 && token !== 'GUEST_MODE') {
      try {
        const { refreshAccessToken } = await import('./firebase');
        const newToken = await refreshAccessToken();
        headers.set('Authorization', `Bearer ${newToken}`);
        response = await fetch(fullUrl, { ...options, headers });
      } catch (err) {
        console.error('Session expired, logging out...');
        this.clearAuth();
        window.location.href = '/';
      }
    }
    
    return response;
  }
};

export const fetchWithAuth = authUtils.fetchWithAuth.bind(authUtils);
export const getAuthHeaders = authUtils.getAuthHeaders.bind(authUtils);
export default authUtils;

