# 📄 ফাইল: apps/studio-client/src/store/adminStore.ts

**প্রকার:** .ts  
**সাইজ:** 2,820 বাইট  
**আপডেট:** 2026-07-04T08:43:35.225614

---

## কোড

```ts
import { create } from 'zustand';
import { setAdminToken, clearAdminToken } from '../services/adminTokenStore';

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
  otpRequired: boolean;
  setOtpRequired: (val: boolean) => void;
  adminOtp: string;
  setAdminOtp: (val: string) => void;
}

export const useAdminStore = create<AdminState>((set, get) => ({
  adminAuthenticated: true,
  adminPassword: '',
  setAdminPassword: (val) => set({ adminPassword: val }),
  adminError: '',
  setAdminError: (val) => set({ adminError: val }),
  actionStatus: '',
  setActionStatus: (val) => set({ actionStatus: val }),
  adminSubTab: 'dashboard',
  setAdminSubTab: (tab) => set({ adminSubTab: tab }),
  otpRequired: false,
  setOtpRequired: (val) => set({ otpRequired: val }),
  adminOtp: '',
  setAdminOtp: (val) => set({ adminOtp: val }),
  handleAdminLogin: async () => {
    const { adminPassword, otpRequired, adminOtp } = get();
    const cleanPassword = adminPassword.trim();
    if (!cleanPassword) return;
    set({ adminError: '' });
    
    try {
      const API_BASE = getApiBaseUrl();
      if (!otpRequired) {
        const res = await fetch(`${API_BASE}/api/admin/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password: cleanPassword }),
        });
        if (res.ok) {
          const data = await res.json();
          if (data.status === 'otp_required') {
            set({ otpRequired: true });
          }
        } else {
          const data = await res.json();
          set({ adminError: data.detail || 'Invalid password.' });
        }
      } else {
        const res = await fetch(`${API_BASE}/api/admin/verify`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password: cleanPassword, otp: adminOtp.trim() }),
        });
        if (res.ok) {
          const data = await res.json();
          set({ adminAuthenticated: true, otpRequired: false, adminOtp: '' });
          setAdminToken(data.token);
        } else {
          const data = await res.json();
          set({ adminError: data.detail || 'Invalid verification code.' });
        }
      }
    } catch (err: any) {
      set({ adminError: 'Connection failed: ' + err.message });
    }
  },
  handleAdminLogout: () => {
    clearAdminToken();
    set({ adminAuthenticated: false, adminPassword: '', otpRequired: false, adminOtp: '' });
  },
}));

```