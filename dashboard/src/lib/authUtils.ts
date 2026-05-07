import { AuthUser } from '../types';

const AUTH_TOKEN_KEY = 'supremeai_token';
const FIREBASE_USER_KEY = 'supremeai_user';

export const authUtils = {
  getToken(): string | null {
    return localStorage.getItem(AUTH_TOKEN_KEY) || sessionStorage.getItem(AUTH_TOKEN_KEY) || null;
  },
  setToken(token: string): void {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
  },
  getCurrentUser(): AuthUser | null {
    const userJson = localStorage.getItem(FIREBASE_USER_KEY) || sessionStorage.getItem(FIREBASE_USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  },
  setCurrentUser(user: AuthUser | Record<string, unknown> | null): void {
    if (user) {
      localStorage.setItem(FIREBASE_USER_KEY, JSON.stringify({ 
          id: user.id || (user as Record<string, unknown>).uid || '', 
          email: user.email, 
          displayName: user.displayName || user.username || 'User',
          role: user.role || (user as Record<string, unknown>).role || 'user',
          tier: user.tier || (user as Record<string, unknown>).tier || 'free'
      }));
    } else {
      localStorage.removeItem(FIREBASE_USER_KEY);
      sessionStorage.removeItem(FIREBASE_USER_KEY);
    }
  },
  clearAuth(): void {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(FIREBASE_USER_KEY);
    localStorage.removeItem('supremeai_refresh_token');
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(FIREBASE_USER_KEY);
  },
  clearToken(): void {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
  },
  getAuthHeaders(): Record<string, string> {
    const token = authUtils.getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  },
  isAuthenticated(): boolean {
    return !!authUtils.getToken();
  },
  async fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
    const headers = new Headers(options.headers || {});
    const token = authUtils.getToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    
    const API_BASE = import.meta.env.VITE_API_URL || import.meta.env.REACT_APP_API_URL || '';
    const fullUrl = url.startsWith('/') && API_BASE ? `${API_BASE}${url}` : url;
    
    let response = await fetch(fullUrl, { ...options, headers });
    
    if (response.status === 401) {
      try {
        const { refreshAccessToken } = await import('./firebase');
        const newToken = await refreshAccessToken();
        headers.set('Authorization', `Bearer ${newToken}`);
        response = await fetch(fullUrl, { ...options, headers });
      } catch (err) {
        authUtils.clearAuth();
        window.location.href = '/admin';
      }
    }
    return response;
  }
};
export default authUtils;
