import { create } from 'zustand';

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
  initialize: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  status: AuthStatus.UNINITIALIZED,
  user: null,

  login: async (email, name) => {
    // Mock login delay
    await new Promise((resolve) => setTimeout(resolve, 1000));
    set({
      status: AuthStatus.LOGGED_IN,
      user: {
        id: 'user_123',
        email,
        name,
        avatarUrl: `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random`,
      },
    });
  },

  logout: () => {
    set({ status: AuthStatus.LOGGED_OUT, user: null });
  },

  initialize: () => {
    // Mock initialization (e.g., checking tokens in local storage)
    setTimeout(() => {
      set({ status: AuthStatus.LOGGED_OUT });
    }, 500);
  },
}));
