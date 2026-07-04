# 📄 ফাইল: apps/studio-client/src/store/themeStore.ts

**প্রকার:** .ts  
**সাইজ:** 496 বাইট  
**আপডেট:** 2026-07-04T23:21:14.719367

---

## কোড

```ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface ThemeState {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'dark',
      toggleTheme: () => set((state) => ({ theme: state.theme === 'dark' ? 'light' : 'dark' })),
    }),
    {
      name: 'supremeai-theme-storage',
      storage: createJSONStorage(() => localStorage),
    }
  )
);

```