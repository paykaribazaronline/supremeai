import { create } from 'zustand';
import { getApiBaseUrl } from '../utils/api';

interface AdminState {
  adminAuthenticated: boolean;
  adminPassword: string;
  setAdminPassword: (val: string) => void;
  adminError: string;
  setAdminError: (val: string) => void;
  handleAdminLogin: () => Promise<void>;
  handleAdminLogout: () => void;
  actionStatus: string;
  setActionStatus: (val: string) => void;
  adminSubTab: string;
  setAdminSubTab: (tab: string) => void;
  adminEmail: string;
  setAdminEmail: (val: string) => void;
  otpRequired: boolean;
  setOtpRequired: (val: boolean) => void;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
  totpSetupRequired: boolean;
  setTotpSetupRequired: (val: boolean) => void;
  totpSecret: string;
  setTotpSecret: (val: string) => void;
  provisioningUri: string;
  setProvisioningUri: (val: string) => void;
}

export const useAdminStore = create<AdminState>((set, get) => ({
  adminAuthenticated: false,
  adminPassword: '',
  setAdminPassword: (val) => set({ adminPassword: val }),
  adminError: '',
  setAdminError: (val) => set({ adminError: val }),
  actionStatus: '',
  setActionStatus: (val) => set({ actionStatus: val }),
  adminSubTab: 'dashboard',
  setAdminSubTab: (tab) => set({ adminSubTab: tab }),
  adminEmail: '',
  setAdminEmail: (val) => set({ adminEmail: val }),
  otpRequired: false,
  setOtpRequired: (val) => set({ otpRequired: val }),
  adminOtp: '',
  setAdminOtp: (val) => set({ adminOtp: val }),
  totpSetupRequired: false,
  setTotpSetupRequired: (val) => set({ totpSetupRequired: val }),
  totpSecret: '',
  setTotpSecret: (val) => set({ totpSecret: val }),
  provisioningUri: '',
  setProvisioningUri: (val) => set({ provisioningUri: val }),
  handleAdminLogin: async () => {
    const { adminEmail, adminPassword, otpRequired, adminOtp, totpSetupRequired } = get();
    const cleanEmail = adminEmail.trim();
    const cleanPassword = adminPassword.trim();

    if (!otpRequired && (!cleanEmail || !cleanPassword)) return;
    set({ adminError: '' });

    try {
      const API_BASE = getApiBaseUrl();

      // Dynamic import to avoid hydration issues if firebase isn't initialized yet
      const { getAuth, signInWithEmailAndPassword } = await import('firebase/auth');
      const auth = getAuth();

      let idToken = '';

      if (!otpRequired) {
        // Step 1: Firebase Email/Password Authentication
        try {
          const userCredential = await signInWithEmailAndPassword(auth, cleanEmail, cleanPassword);
          idToken = await userCredential.user.getIdToken(true);
        } catch (fbErr: any) {
          set({ adminError: 'Invalid email or password.' });
          return;
        }

        // Step 2: Send Firebase Token to Backend for Role/TOTP check
        const res = await fetch(`${API_BASE}/api/admin/firebase-login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ id_token: idToken }),
        });

        const data = await res.json();

        if (res.ok) {
          if (data.status === 'otp_required') {
            set({ otpRequired: true });
          } else if (data.status === 'totp_setup_required') {
            // Initiate TOTP Setup
            const setupRes = await fetch(`${API_BASE}/api/admin/firebase-totp-setup`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              credentials: 'include',
              body: JSON.stringify({ id_token: idToken }),
            });
            const setupData = await setupRes.json();
            if (setupRes.ok) {
              set({
                totpSetupRequired: true,
                otpRequired: true,
                totpSecret: setupData.secret,
                provisioningUri: setupData.provisioning_uri
              });
            } else {
              set({ adminError: setupData.detail || 'Failed to setup TOTP.' });
            }
          }
        } else {
          set({ adminError: data.detail || 'Not authorized as admin.' });
        }
      } else {
        // Step 3: Verify TOTP
        // We need the ID token again. Since the user just logged in, they are in auth.currentUser
        const user = auth.currentUser;
        if (!user) {
          set({ adminError: 'Session expired. Please login again.' });
          set({ otpRequired: false });
          return;
        }
        idToken = await user.getIdToken();

        const endpoint = totpSetupRequired ? '/api/admin/firebase-totp-verify' : '/api/admin/firebase-totp-verify';

        const res = await fetch(`${API_BASE}${endpoint}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ id_token: idToken, otp: adminOtp.trim() }),
        });

        if (res.ok) {
          const data = await res.json();
          // Token is now set via httpOnly cookie from the backend.
          set({ adminAuthenticated: true, otpRequired: false, totpSetupRequired: false, adminOtp: '', adminPassword: '' });
        } else {
          const data = await res.json();
          set({ adminError: data.detail || 'Invalid verification code.' });
        }
      }
    } catch (err: any) {
      set({ adminError: 'Connection failed: ' + err.message });
    }
  },
  handleAdminLogout: async () => {
    try {
      const { getAuth, signOut } = await import('firebase/auth');
      await signOut(getAuth());
      const API_BASE = getApiBaseUrl();
      await fetch(`${API_BASE}/api/admin/logout`, { method: 'POST', credentials: 'include' });
    } catch(e) {
      console.error('Logout failed:', e);
    }
    set({ adminAuthenticated: false, adminPassword: '', otpRequired: false, adminOtp: '', adminError: '' });
  },
}));
