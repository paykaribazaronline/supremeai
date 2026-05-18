import { AuthUser } from '../types';

let _inMemoryToken: string | null = null;
let _inMemoryRefreshToken: string | null = null;

const AUTH_TOKEN_KEY = 'supremeai_token';
const FIREBASE_USER_KEY = 'supremeai_user';
const REFRESH_TOKEN_KEY = 'supremeai_refresh_token';

// Simple obfuscation to prevent plain-text inspection in browser storage
function obfuscate(value: string): string {
  if (!value) return '';
  try {
    return btoa(encodeURIComponent(value).split('').map((char, index) => {
      return String.fromCharCode(char.charCodeAt(0) ^ (index % 5 + 42));
    }).join(''));
  } catch (e) {
    return value;
  }
}

function deobfuscate(value: string): string {
  if (!value) return '';
  try {
    const decoded = atob(value);
    return decodeURIComponent(decoded.split('').map((char, index) => {
      return String.fromCharCode(char.charCodeAt(0) ^ (index % 5 + 42));
    }).join(''));
  } catch (e) {
    return value;
  }
}

export const authUtils = {
  getToken(): string | null {
    if (_inMemoryToken) return _inMemoryToken;
    const raw = sessionStorage.getItem(AUTH_TOKEN_KEY);
    if (raw) {
      const token = deobfuscate(raw);
      _inMemoryToken = token;
      return token;
    }
    return 'GUEST_MODE';
  },

  getRefreshToken(): string | null {
    if (_inMemoryRefreshToken) return _inMemoryRefreshToken;
    const raw = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    if (raw) {
      const token = deobfuscate(raw);
      _inMemoryRefreshToken = token;
      return token;
    }
    return null;
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
    _inMemoryToken = null;
    _inMemoryRefreshToken = null;
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(FIREBASE_USER_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    
    // Clear legacy local storage tokens for security cleanup
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(FIREBASE_USER_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
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
    const raw = sessionStorage.getItem(FIREBASE_USER_KEY);
    if (!raw) return null;
    try {
      const userStr = deobfuscate(raw);
      return JSON.parse(userStr);
    } catch (e) {
      return null;
    }
  },

  setToken(token: string) {
    _inMemoryToken = token;
    sessionStorage.setItem(AUTH_TOKEN_KEY, obfuscate(token));
  },

  setRefreshToken(token: string) {
    _inMemoryRefreshToken = token;
    sessionStorage.setItem(REFRESH_TOKEN_KEY, obfuscate(token));
  },

  setCurrentUser(user: any) {
    const userStr = JSON.stringify(user);
    sessionStorage.setItem(FIREBASE_USER_KEY, obfuscate(userStr));
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

