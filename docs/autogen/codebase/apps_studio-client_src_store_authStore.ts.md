# 📄 ফাইল: apps/studio-client/src/store/authStore.ts

**প্রকার:** .ts  
**সাইজ:** 2,245 বাইট  
**আপডেট:** 2026-07-11T19:00:24.775528

---

## কোড

```ts
import { create } from 'zustand';
import { apiClient } from '../services/apiClient';

export enum AuthStatus {
  UNINITIALIZED = 'uninitialized',
  LOGGED_OUT = 'loggedOut',
  LOGGED_IN = 'loggedIn',
}

interface UserProfile {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
}

interface AuthState {
  status: AuthStatus;
  user: UserProfile | null;
  login: (email: string, name: string) => Promise<void>;
  logout: () => void;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  status: AuthStatus.UNINITIALIZED,
  user: null,

  login: async (email, name) => {
    try {
      // 🟢 Sprint 5: Call actual FastAPI Dev Login endpoint
      const response = await apiClient.post<any>('/auth/login', {
        username: email,
        password: 'dev_password' // Ignored by dev endpoint
      });
      
      const token = response.access_token;
      localStorage.setItem('supremeai_auth_token', token);
      
      set({
        status: AuthStatus.LOGGED_IN,
        user: {
          id: response.user_id,
          email,
          name,
          avatarUrl: `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random`,
        },
      });
    } catch (error) {
      console.error("Login failed:", error);
      throw error;
    }
  },

  logout: () => {
    localStorage.removeItem('supremeai_auth_token');
    set({ status: AuthStatus.LOGGED_OUT, user: null });
  },

  initialize: async () => {
    const token = localStorage.getItem('supremeai_auth_token');
    if (token) {
      try {
        const response = await apiClient.get<any>('/auth/me');
        set({
          status: AuthStatus.LOGGED_IN,
          user: {
            id: response.user_id,
            email: 'dev_user@example.com', // /auth/me doesn't return email right now, this is a placeholder
            name: response.role,
            avatarUrl: `https://ui-avatars.com/api/?name=${encodeURIComponent(response.role)}&background=random`,
          }
        });
      } catch (e) {
        localStorage.removeItem('supremeai_auth_token');
        set({ status: AuthStatus.LOGGED_OUT });
      }
    } else {
      set({ status: AuthStatus.LOGGED_OUT });
    }
  },
}));

```