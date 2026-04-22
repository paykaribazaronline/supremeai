// Firebase configuration for SupremeAI Admin Dashboard
// Project: supremeai-a

import { initializeApp, getApps, getApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  Auth,
  UserCredential,
  getIdTokenResult,
} from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || 'supremeai-a.firebaseapp.com',
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL || 'https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || 'supremeai-a',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || 'supremeai-a.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '565236080752',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '1:565236080752:web:572bb9313db9afb355d4b5',
};

// Avoid re-initialising the app during HMR
const app: FirebaseApp = getApps().length ? getApp() : initializeApp(firebaseConfig);
export const firebaseAuth: Auth = getAuth(app);

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
}

/**
 * Sign out from Firebase Auth (does not clear backend session cookie — call
 * POST /api/auth/logout separately if needed).
 */
export async function firebaseSignOut(): Promise<void> {
  await signOut(firebaseAuth);
}
