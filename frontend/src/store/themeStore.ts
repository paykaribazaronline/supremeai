import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { apiClient } from '../services/apiClient';
import { eventBus, Events } from '../lib/eventBus';

interface ThemeState {
  theme: 'dark' | 'light' | 'system';
  isSyncing: boolean;
  lastSyncedAt: number | null;
  toggleTheme: () => Promise<void>;
  setTheme: (theme: 'dark' | 'light' | 'system') => Promise<void>;
  initializeFromBackend: () => Promise<void>;
  syncToBackend: (theme: 'dark' | 'light' | 'system') => Promise<void>;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: 'dark',
      isSyncing: false,
      lastSyncedAt: null,
      
      toggleTheme: async () => {
        const newTheme = get().theme === 'dark' ? 'light' : 'dark';
        await get().setTheme(newTheme);
      },
      
      setTheme: async (newTheme) => {
        set({ theme: newTheme });
        
        try {
          set({ isSyncing: true });
          await get().syncToBackend(newTheme);
          set({ isSyncing: false, lastSyncedAt: Date.now() });
        } catch (e) {
          console.warn('Theme sync failed, applied locally only:', e);
          set({ isSyncing: false });
        }
        
        eventBus.emit(Events.THEME_CHANGED, {
          theme: newTheme,
          isDark: newTheme === 'dark',
          timestamp: Date.now(),
          source: 'theme_store',
        });
        
        if (newTheme === 'dark') {
          eventBus.emit(Events.THEME_DARK_MODE, { timestamp: Date.now() });
        } else {
          eventBus.emit(Events.THEME_LIGHT_MODE, { timestamp: Date.now() });
        }
      },
      
      initializeFromBackend: async () => {
        try {
          const response = await apiClient.get('/api/user/preferences');
          const prefs = response.data;
          
          if (prefs?.theme && ['light', 'dark', 'system'].includes(prefs.theme)) {
            set({ 
              theme: prefs.theme,
              lastSyncedAt: Date.now()
            });
            console.log(`[ThemeStore] Loaded theme from backend: ${prefs.theme}`);
          }
        } catch (e) {
          console.warn('[ThemeStore] Failed to load from backend, using local:', e);
        }
      },
      
      syncToBackend: async (theme) => {
        await apiClient.put('/api/user/preferences', { 
          theme,
          updatedAt: new Date().toISOString()
        });
      },
    }),
    {
      name: 'supremeai-theme-storage',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
