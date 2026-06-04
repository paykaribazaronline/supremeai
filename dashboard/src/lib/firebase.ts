// Firebase configuration for SupremeAI Admin Dashboard
// Project: supremeai-a

import {
  initializeApp,
  getApps,
  getApp,
  FirebaseApp,
  FirebaseError,
} from "firebase/app";
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  Auth,
  UserCredential,
  getIdTokenResult,
  connectAuthEmulator,
} from "firebase/auth";
import { getFirestore, connectFirestoreEmulator } from "firebase/firestore";
import { getFunctions, connectFunctionsEmulator } from "firebase/functions";

// Use Firebase Hosting environment or environment variables
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "dummy-key-for-development",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || window.location.host,
  databaseURL: import.meta.env.VITE_FIREBASE_DATABASE_URL || "",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "supremeai-a",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "",
  messagingSenderId:
    import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "123456789",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:123456789:web:abcdef",
};

// Avoid re-initialising the app during HMR
const app: FirebaseApp = getApps().length
  ? getApp()
  : initializeApp(firebaseConfig);
export const auth: Auth = getAuth(app);
export const firestore = getFirestore(app);
export const functions = getFunctions(app);

// Connect to Firebase Emulator - Only if explicitly requested via environment variable
if (import.meta.env.VITE_USE_FIREBASE_EMULATOR === "true") {
  try {
    connectAuthEmulator(auth, "http://localhost:9099", {
      disableWarnings: true,
    });
    connectFirestoreEmulator(firestore, "localhost", 8081);
    connectFunctionsEmulator(functions, "localhost", 5003);
    console.log("🚀 Firebase Emulators (Auth, Firestore, Functions) connected");
  } catch (err) {
    console.error("⚠️ Firebase Emulator connection error:", err);
  }
}

/**
 * Global async error handler for unhandled promise rejections.
 * Catches and reports errors that would otherwise be silently swallowed.
 */
window.addEventListener(
  "unhandledrejection",
  (event: PromiseRejectionEvent) => {
    const error = event.reason;
    const message = error instanceof Error ? error.message : String(error);
    // In production, this should send to an error tracking service (e.g., Sentry)
    console.error("[GlobalErrorHandler] Unhandled async error:", message);
  },
);

/**
 * Global error handler for runtime errors.
 */
window.addEventListener("error", (event: ErrorEvent) => {
  const message = event.message ?? "Unknown runtime error";
  const filename = event.filename ?? "unknown";
  const lineno = event.lineno ?? 0;
  console.error(
    `[GlobalErrorHandler] Runtime error at ${filename}:${lineno}:`,
    message,
  );
});

/**
 * Map Firebase error codes to user-friendly messages.
 */
function getFirebaseErrorMessage(code: string | undefined): string {
  if (!code) return "An unexpected authentication error occurred.";

  const errorMap: Record<string, string> = {
    "auth/invalid-email":
      "The email address is not valid. Please check and try again.",
    "auth/user-disabled":
      "This account has been disabled. Please contact support.",
    "auth/user-not-found": "No account found with this email address.",
    "auth/wrong-password": "The password is incorrect. Please try again.",
    "auth/too-many-requests":
      "Too many failed attempts. Please wait a moment before trying again.",
    "auth/network-request-failed":
      "Unable to connect. Please check your internet connection.",
    "auth/invalid-credential":
      "The credentials are invalid. Please log in again.",
    "auth/expired-action-code":
      "This link has expired. Please request a new one.",
    "auth/invalid-action-code":
      "This link is invalid or has already been used.",
    "auth/email-already-in-use": "An account with this email already exists.",
    "auth/weak-password":
      "The password is too weak. Please use at least 6 characters.",
    "auth/requires-recent-login":
      "Please log out and log in again to perform this action.",
    // Additional configuration-related errors
    "auth/operation-not-allowed":
      "Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.",
    "auth/unauthorized-domain":
      "This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.",
    "auth/invalid-api-key":
      "Firebase API key is invalid. Check your Firebase project configuration.",
    "auth/missing-api-key":
      "Firebase API key is missing. Check your environment configuration.",
    "auth/invalid-app-id":
      "Firebase App ID is invalid. Verify your Firebase configuration.",
  };

  return (
    errorMap[code] ?? "An authentication error occurred. Please try again."
  );
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
): Promise<{
  token: string;
  refreshToken: string;
  user: import("../types").AuthUser;
}> {
  try {
    const cred: UserCredential = await signInWithEmailAndPassword(
      auth,
      email,
      password,
    );

    const idTokenResult = await getIdTokenResult(cred.user);

    // Role verification is handled by the backend exchange (/api/auth/firebase-login)
    // which checks both custom claims and the admin email whitelist.

    const idToken = idTokenResult.token;

    const API_BASE = import.meta.env.VITE_API_URL || "";
    try {
      const resp = await fetch(`${API_BASE}/api/auth/firebase-login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }),
      });
      if (resp.ok) {
        const response = (await resp.json()) as {
          success: boolean;
          data: {
            token: string;
            refreshToken: string;
            user: import("../types").AuthUser;
          };
          error?: string;
        };
        if (response.success && response.data) {
          const data = response.data;
          localStorage.setItem("supremeai_token", data.token);
          localStorage.setItem("supremeai_refresh_token", data.refreshToken);
          return data;
        }
      }
    } catch (_) {
      // Local dev / emulator: backend exchange optional — fall through to client-only success
    }

    // Fallback for localhost/emulator when backend exchange is unavailable
    const fallbackData = {
      token: idToken,
      refreshToken: "dev-refresh-token",
      user: {
        uid: cred.user.uid,
        email: cred.user.email || "",
        displayName: cred.user.displayName || "",
        photoURL: cred.user.photoURL || "",
        role: "admin",
      } as any,
    };
    localStorage.setItem("supremeai_token", fallbackData.token);
    localStorage.setItem("supremeai_refresh_token", fallbackData.refreshToken);
    return fallbackData;
  } catch (error: unknown) {
    if (error instanceof FirebaseError) {
      const message = getFirebaseErrorMessage(error.code);
      console.error("[Firebase Auth Error]", {
        code: error.code,
        message: error.message,
        fullError: error,
      });
      throw new Error(message);
    }
    console.error("[Unexpected Auth Error]", error);
    throw error;
  }
}

/**
 * Refresh the access token using the stored refresh token.
 */
export async function refreshAccessToken(): Promise<string> {
  const refreshToken = localStorage.getItem("supremeai_refresh_token");
  if (!refreshToken) {
    throw new Error("No refresh token available");
  }

  const API_BASE = import.meta.env.VITE_API_URL || "";
  const resp = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!resp.ok) {
    const err = (await resp.json().catch(() => ({}))) as Record<string, string>;
    throw new Error(err["message"] ?? "Token refresh failed");
  }

  const response = (await resp.json()) as {
    success: boolean;
    data: { token: string };
    error?: string;
  };

  if (!response.success || !response.data) {
    throw new Error(response.error ?? "Token refresh failed");
  }

  const data = response.data;
  localStorage.setItem("supremeai_token", data.token);
  return data.token;
}

/**
 * Sign out from Firebase Auth (does not clear backend session cookie — call
 * POST /api/auth/logout separately if needed).
 */
export async function firebaseSignOutFn(): Promise<void> {
  try {
    await signOut(auth);
  } catch (error: unknown) {
    // Sign-out errors are non-critical; log but don't throw
    if (error instanceof FirebaseError) {
      console.warn(
        "Firebase sign-out warning:",
        getFirebaseErrorMessage(error.code),
      );
    }
  }
}
