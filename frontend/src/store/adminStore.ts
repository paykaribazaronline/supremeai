import { create } from 'zustand';
import { getApiBaseUrl } from '../utils/api';
import { getFirebaseAuth } from '../firebase';
import { signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { eventBus, Events } from '../lib/eventBus';

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
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
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

// বাংলা মন্তব্য: ব্যাকএন্ড provisioning_uri না দিলে ক্লায়েন্ট-সাইডে otpauth URI তৈরি করে QR দেখানো হয়।
const buildProvisioningUri = (email: string, secret: string): string =>
  `otpauth://totp/SupremeAI:${encodeURIComponent(email)}?secret=${secret}&issuer=SupremeAI&digits=6&period=30`;

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
  resetTotpSetup: () => Promise<void>;
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
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } catch (authErr: any) {
          console.error("Firebase Auth Error:", authErr);
          const firebaseErrMsg = typeof authErr?.message === 'string' 
            ? authErr.message 
            : (authErr?.code ? `Auth error: ${authErr.code}` : (authErr && typeof authErr === 'object' ? JSON.stringify(authErr) : 'Invalid email or password.'));
          set({ adminError: String(firebaseErrMsg) });
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
                provisioningUri: setupData.provisioning_uri || buildProvisioningUri(cleanEmail, setupData.secret || '')
              });
            } else {
              const errStr = typeof setupData.detail === 'string' ? setupData.detail : (setupData.detail && typeof setupData.detail === 'object' ? JSON.stringify(setupData.detail) : 'Failed to setup TOTP.');
              set({ adminError: errStr });
            }
          }
        } else {
          const errStr = typeof data.detail === 'string' ? data.detail : (data.detail && typeof data.detail === 'object' ? JSON.stringify(data.detail) : 'Not authorized as admin.');
          set({ adminError: errStr });
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
            // 🔥 ফিক্স: সঠিক key `supreme_admin_jwt`-এ সেভ (adminTokenStore/getDecodedToken এটাই পড়ে)।
            // পুরনো `adminToken` key ব্যাকওয়ার্ড-কম্প্যাটের জন্য রেখে দেওয়া হলো।
            localStorage.setItem('supreme_admin_jwt', data.token);
            localStorage.setItem('adminToken', data.token);
            const decoded = decodeJwt(data.token);
            if (decoded && typeof decoded.role === 'string') {
              set({ adminRole: decoded.role });
            }
          }
          
          eventBus.emit(Events.AUTH_LOGIN, {
            source: 'admin_store',
            timestamp: Date.now()
          });
          
          set({ adminAuthenticated: true, otpRequired: false, totpSetupRequired: false, adminOtp: '' });
        } else {
          const data = await res.json();
          const errStr = typeof data.detail === 'string' ? data.detail : (data.detail && typeof data.detail === 'object' ? JSON.stringify(data.detail) : 'Invalid verification code.');
          set({ adminError: errStr });
        }
      }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      const msg = err && typeof err === 'object' && err.message ? String(err.message) : (typeof err === 'object' ? JSON.stringify(err) : String(err));
      set({ adminError: 'Connection failed: ' + msg });
    }
  },
  handleAdminLogout: async () => {
    try {
      const auth = await getFirebaseAuth();
      // 🔥 ফিক্স: সঠিক টোকেন keyগুলো পরিষ্কার করা হচ্ছে (আগে শুধু 'adminToken' remove হতো,
      // অথচ adminTokenStore 'supreme_admin_jwt' পড়ে — ফলে স্টেল টোকেন জমে থাকত)
      const TOKEN_KEYS = ['adminToken', 'supreme_admin_jwt', 'supremeai_auth_token'];
      TOKEN_KEYS.forEach((key) => localStorage.removeItem(key));

      // বাংলা মন্তব্য: backend-এ কোনো /api/admin/logout endpoint নাই (নিশ্চিত হয়ে দেখা গেছে)।
      // তাই সুইচ ব্যাকএন্ড অল বেস্ট-এফোর্ট call এড়িয়ে সরাসরি Firebase client signOut করা হলো।
      // JWT স্ট্যাটেলেস — client-side token মুছে ফেলাই যথেষ্ট।
      await signOut(auth);
      
      eventBus.emit(Events.AUTH_LOGOUT, {
        source: 'admin_store',
        timestamp: Date.now()
      });
    } catch(e) {
      console.error('Logout failed:', e);
    }
    set({ adminAuthenticated: false, adminRole: null, otpRequired: false, adminOtp: '', adminError: '', totpSetupRequired: false, totpSecret: '', provisioningUri: '' });
  },
  resetTotpSetup: async () => {
    set({ adminError: '' });
    try {
      const auth = await getFirebaseAuth();
      const user = auth.currentUser;
      if (!user) {
        set({ adminError: 'Session expired. Please login again.' });
        return;
      }
      const idToken = await user.getIdToken(true);
      const API_BASE = getApiBaseUrl();
      const email = (user.email || '').trim() || get().adminEmail.trim();

      const res = await fetch(`${API_BASE}/api/admin/firebase-totp-setup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ id_token: idToken }),
      });
      const data = await res.json();
      if (res.ok) {
        set({
          totpSetupRequired: true,
          otpRequired: true,
          totpSecret: data.secret,
          provisioningUri: data.provisioning_uri || buildProvisioningUri(email, data.secret || ''),
        });
      } else {
        const errStr = typeof data.detail === 'string' ? data.detail : (data.detail && typeof data.detail === 'object' ? JSON.stringify(data.detail) : 'Failed to generate QR code.');
        set({ adminError: errStr });
      }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      const msg = err && typeof err === 'object' && err.message ? String(err.message) : (typeof err === 'object' ? JSON.stringify(err) : String(err));
      set({ adminError: 'Connection failed: ' + msg });
    }
  },
}));
