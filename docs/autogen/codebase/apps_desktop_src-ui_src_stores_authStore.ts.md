# 📄 ফাইল: apps/desktop/src-ui/src/stores/authStore.ts

**প্রকার:** .ts  
**সাইজ:** 780 বাইট  
**আপডেট:** 2026-07-07T17:46:01.355365

---

## কোড

```ts
import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      isAuthenticated: false,
      login: (token: string) => {
        localStorage.setItem('jwt', token);
        set({ token, isAuthenticated: true });
      },
      logout: () => {
        localStorage.removeItem('jwt');
        set({ token: null, isAuthenticated: false });
      },
    }),
    {
      name: "auth-storage", // name of the item in localStorage (must be unique)
      partialize: (state) => ({ isAuthenticated: state.isAuthenticated }),
    }
  )
);
```