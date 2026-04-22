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
  apiKey: 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8',
  authDomain: 'supremeai-a.firebaseapp.com',
  databaseURL: 'https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/',
  projectId: 'supremeai-a',
  storageBucket: 'supremeai-a.firebasestorage.app',
  messagingSenderId: '565236080752',
  appId: '1:565236080752:web:572bb9313db9afb355d4b5',
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
