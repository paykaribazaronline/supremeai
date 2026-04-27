// Firebase configuration for SupremeAI Admin Dashboard
// Project: supremeai-a

import { initializeApp, getApps, getApp, FirebaseApp, FirebaseError } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  Auth,
  UserCredential,
  getIdTokenResult,
} from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

// Avoid re-initialising the app during HMR
const app: FirebaseApp = getApps().length ? getApp() : initializeApp(firebaseConfig);
export const firebaseAuth: Auth = getAuth(app);

/**
 * Global async error handler for unhandled promise rejections.
 * Catches and reports errors that would otherwise be silently swallowed.
 */
window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
  const error = event.reason;
  const message = error instanceof Error ? error.message : String(error);
  // In production, this should send to an error tracking service (e.g., Sentry)
  console.error('[GlobalErrorHandler] Unhandled async error:', message);
});

/**
 * Global error handler for runtime errors.
 */
window.addEventListener('error', (event: ErrorEvent) => {
  const message = event.message ?? 'Unknown runtime error';
  const filename = event.filename ?? 'unknown';
  const lineno = event.lineno ?? 0;
  console.error(`[GlobalErrorHandler] Runtime error at ${filename}:${lineno}:`, message);
});

/**
 * Map Firebase error codes to user-friendly messages.
 */
function getFirebaseErrorMessage(code: string | undefined): string {
  if (!code) return 'An unexpected authentication error occurred.';

  const errorMap: Record<string, string> = {
    'auth/invalid-email': 'The email address is not valid. Please check and try again.',
    'auth/user-disabled': 'This account has been disabled. Please contact support.',
    'auth/user-not-found': 'No account found with this email address.',
    'auth/wrong-password': 'The password is incorrect. Please try again.',
    'auth/too-many-requests': 'Too many failed attempts. Please wait a moment before trying again.',
    'auth/network-request-failed': 'Unable to connect. Please check your internet connection.',
    'auth/invalid-credential': 'The credentials are invalid. Please log in again.',
    'auth/expired-action-code': 'This link has expired. Please request a new one.',
    'auth/invalid-action-code': 'This link is invalid or has already been used.',
    'auth/email-already-in-use': 'An account with this email already exists.',
    'auth/weak-password': 'The password is too weak. Please use at least 6 characters.',
    'auth/requires-recent-login': 'Please log out and log in again to perform this action.',
  };

  return errorMap[code] ?? 'An authentication error occurred. Please try again.';
}

/**
 * Sign in with Firebase Auth, verify admin role, then exchange the ID token
 * for a SupremeAI backend session JWT via POST /api/auth/firebase-login.
 *
 * Returns the backend JWT string, or throws on failure.
 */
export async function firebaseSignIn(
  email: string,
  password: string,
): Promise<{ token: string; refreshToken: string; user: Record<string, unknown> }> {
  try {
    const cred: UserCredential = await signInWithEmailAndPassword(
      firebaseAuth,
      email,
      password,
    );

    // Check admin role via Firebase custom claims
    const idTokenResult = await getIdTokenResult(cred.user);
    const role = idTokenResult.claims['role'] as string | undefined;
    if (role !== 'admin') {
      await signOut(firebaseAuth);
      throw new Error('Access denied: You do not have admin privileges.');
    }

    const idToken = idTokenResult.token;

    const resp = await fetch('/api/auth/firebase-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({})) as Record<string, string>;
      throw new Error(err['message'] ?? 'Firebase token exchange failed');
    }

    const data = await resp.json() as {
      token: string;
      refreshToken: string;
      user: Record<string, unknown>;
    };
    return data;
  } catch (error: unknown) {
    if (error instanceof FirebaseError) {
      throw new Error(getFirebaseErrorMessage(error.code));
    }
    throw error;
  }
}

/**
 * Sign out from Firebase Auth (does not clear backend session cookie — call
 * POST /api/auth/logout separately if needed).
 */
export async function firebaseSignOutFn(): Promise<void> {
  try {
    await signOut(firebaseAuth);
  } catch (error: unknown) {
    // Sign-out errors are non-critical; log but don't throw
    if (error instanceof FirebaseError) {
      console.warn('Firebase sign-out warning:', getFirebaseErrorMessage(error.code));
    }
  }
}
