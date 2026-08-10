import { create } from 'zustand';
import { getApiBaseUrl } from '../utils/api';
import { getFirebaseAuth } from '../firebase';
import { signInWithEmailAndPassword, signOut } from 'firebase/auth';

const decodeJwt = (token: string): Record<string, unknown> | null => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    const decoded = JSON.parse(jsonPayload);

    // Validate decoded structure
    if (!decoded || typeof decoded !== 'object') {
      throw new Error('Decoded JWT payload is not a valid object');
    }

    return decoded;
  } catch (e: any) {
    // 🛡️ অডিটর ফিক্স: সাইলেন্ট ফেইলর ব্লাস্ট করে ইন্টারনাল ডায়াগনস্টিক ট্রেস এনফোর্স
    console.warn("⚠️ [JWT_DECODE_LEAK]: Failed to safely parse admin JWT token.", {
      error_message: e?.message || 'Malformed JWT structure',
      token_length: token.length,
      timestamp: new Date().toISOString(),
      token_preview: token.substring(0, 20) + '...'
    });
    return null;
  }
};

interface AdminState {
  adminAuthenticated: boolean;
  adminRole: string | null;
  setAdminRole: (val: string | null) => void;
  adminError: string;
  setAdminError: (val: string) => void;
  handleAdminLogin: (password?: string) => Promise<void>;
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
  adminRole: null,
  setAdminRole: (val) => set({ adminRole: val }),
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
  handleAdminLogin: async (password?: string) => {
    const { adminEmail, otpRequired, adminOtp, totpSetupRequired } = get();
    const cleanEmail = adminEmail.trim();
    const cleanPassword = password?.trim() || '';

    if (!otpRequired && (!cleanEmail || !cleanPassword)) return;
    set({ adminError: '' });

    try {
      const API_BASE = getApiBaseUrl();

      // বাংলা মন্তব্য: getFirebaseAuth() ব্যবহার করা হচ্ছে যাতে Firebase app ইনিশিয়ালাইজেশন (initializeApp) সঠিকভাবে সম্পন্ন হয়
      const auth = await getFirebaseAuth();

      let idToken = '';

      if (!otpRequired) {
        // Step 1: Firebase Email/Password Authentication
        try {
          const userCredential = await signInWithEmailAndPassword(auth, cleanEmail, cleanPassword);
          idToken = await userCredential.user.getIdToken(true);
        } catch {
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
          // Token is returned from the backend in data.token
          if (data.token) {
            localStorage.setItem('adminToken', data.token);
            const decoded = decodeJwt(data.token);
            if (decoded && typeof decoded.role === 'string') {
              set({ adminRole: decoded.role });
            }
          }
          set({ adminAuthenticated: true, otpRequired: false, totpSetupRequired: false, adminOtp: '' });
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
      const auth = await getFirebaseAuth();
      await signOut(auth);
      const API_BASE = getApiBaseUrl();
      await fetch(`${API_BASE}/api/admin/logout`, { method: 'POST', credentials: 'include' });
      localStorage.removeItem('adminToken');
    } catch(e) {
      console.error('Logout failed:', e);
    }
    set({ adminAuthenticated: false, adminRole: null, otpRequired: false, adminOtp: '', adminError: '' });
  },
}));
