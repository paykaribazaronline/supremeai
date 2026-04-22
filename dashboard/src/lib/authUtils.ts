import Cookies from 'js-cookie';
const AUTH_TOKEN_KEY = 'authToken';
const FIREBASE_USER_KEY = 'firebaseUser';
export const authUtils = {
  getToken(): string | null {
    return Cookies.get(AUTH_TOKEN_KEY) || null;
  },
  setToken(token: string): void {
    Cookies.set(AUTH_TOKEN_KEY, token, { secure: window.location.protocol === 'https:', sameSite: 'strict', expires: 7 });
  },
  getCurrentUser(): any {
    const userJson = Cookies.get(FIREBASE_USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  },
  setCurrentUser(user: any): void {
    if (user) {
      Cookies.set(FIREBASE_USER_KEY, JSON.stringify({ uid: user.uid, email: user.email, displayName: user.displayName }), { secure: window.location.protocol === 'https:', sameSite: 'strict', expires: 7 });
    } else {
      Cookies.remove(FIREBASE_USER_KEY);
    }
  },
  clearAuth(): void {
    Cookies.remove(AUTH_TOKEN_KEY);
    Cookies.remove(FIREBASE_USER_KEY);
  },
  getAuthHeaders(): Record<string, string> {
    const token = authUtils.getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  },
  isAuthenticated(): boolean {
    return !!authUtils.getToken();
  }
};
export default authUtils;
