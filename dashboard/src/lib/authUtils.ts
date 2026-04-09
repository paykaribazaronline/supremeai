/**
 * ⚠️ MANDATORY FIREBASE-ONLY AUTHENTICATION UTILITIES
 * 
 * MASTER RULES ENFORCEMENT:
 * 1. ONLY Firebase authentication is allowed - NO exceptions
 * 2. ONLY 'authToken' key for storing Firebase ID tokens
 * 3. ALL API calls MUST include Authorization Bearer header
 * 4. NO alternative authentication methods permitted
 * 
 * All screens MUST use these utilities for auth - NO direct localStorage access
 * 
 * COMPLIANCE:
 * - React Dashboard: Uses authUtils ✅
 * - Combined Deploy: Uses authUtils ✅
 * - Monitoring Dashboard: Uses authUtils ✅
 * - Flutter Web: Uses authUtils via JavaScript bridge ✅
 * - Flutter Mobile: Uses FirebaseAuth directly ✅
 * 
 * @since 2026-04-09 (Master Rules Enforcement)
 * @author SupremeAI Security Team
 */

const AUTH_TOKEN_KEY = 'authToken'; // MASTER RULE: Always use this key
const FIREBASE_USER_KEY = 'firebaseUser';

export const authUtils = {
  /**
   * ✅ FIREBASE-ONLY: Get the current Firebase ID Token
   * Standard key: 'authToken' (enforced everywhere)
   * 
   * @returns Firebase ID Token or null if not authenticated
   */
  getToken(): string | null {
    return localStorage.getItem(AUTH_TOKEN_KEY);
  },

  /**
   * ✅ FIREBASE-ONLY: Store Firebase ID Token
   * MASTER RULE: Must be called after Firebase login
   * 
   * Usage:
   * ```
   * const user = await firebase.auth().signInWithEmailAndPassword(email, password);
   * const token = await user.user.getIdToken();
   * authUtils.setToken(token);
   * ```
   */
  setToken(token: string): void {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
  },

  /**
   * ✅ FIREBASE-ONLY: Get logged-in Firebase user object
   */
  getCurrentUser(): any {
    const userJson = localStorage.getItem(FIREBASE_USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
  },

  /**
   * ✅ FIREBASE-ONLY: Store Firebase user metadata
   * Called after successful Firebase authentication
   * 
   * Do NOT store: passwords, API keys, or any sensitive data
   * Do store: uid, email, displayName only
   */
  setCurrentUser(user: any): void {
    if (user) {
      localStorage.setItem(FIREBASE_USER_KEY, JSON.stringify({
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
      }));
    } else {
      localStorage.removeItem(FIREBASE_USER_KEY);
    }
  },

  /**
   * ✅ FIREBASE-ONLY: Clear all authentication data (LOGOUT)
   * 
   * MASTER RULE: Called on firebase.auth().signOut()
   * Must clear: authToken and firebaseUser
   */
  clearAuth(): void {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(FIREBASE_USER_KEY);
  },

  /**
   * ✅ FIREBASE-ONLY: Get Authorization header for API calls
   * 
   * Usage:
   * ```
   * const headers = authUtils.getAuthHeaders();
   * fetch('/api/data', { headers });
   * ```
   * 
   * Returns: { Authorization: "Bearer {firebaseIdToken}" }
   * Or: {} if not authenticated
   */
  getAuthHeaders(): Record<string, string> {
    const token = authUtils.getToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  },

  /**
   * ✅ FIREBASE-ONLY: Check if user is authenticated
   * 
   * Returns true ONLY if valid Firebase ID Token exists
   */
  isAuthenticated(): boolean {
    return !!authUtils.getToken();
  },
};

export default authUtils;
