# 📄 ফাইল: apps/studio-client/src/contexts/ThemeContext.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,949 বাইট  
**আপডেট:** 2026-07-04T13:24:28.404875

---

## কোড

```tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import { getAdminToken } from '../services/adminTokenStore';
import { getApiBaseUrl } from '../utils/api';

// বাংলা মন্তব্য: ৪টি থিম সাপোর্ট করা হচ্ছে — Dark Space, Sky Blue, Sunset Ember, Emerald Matrix
type Theme = 'dark' | 'light' | 'sunset' | 'matrix';

// থিম সাইকেল অর্ডার (টগল বাটনে ক্লিক করলে পরবর্তী থিমে যাবে)
const THEME_ORDER: Theme[] = ['dark', 'light', 'sunset', 'matrix'];

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>('dark'); // ডিফল্ট Deep Space (dark)

  useEffect(() => {
    // বাংলা মন্তব্য: Race Condition এড়াতে AbortController ব্যবহার করা হয়েছে
    const controller = new AbortController();
    const token = getAdminToken();

    if (!token) return;

    // 1. লোকাল স্টোরেজ থেকে থিম পড়া (Optimistic Load)
    const localTheme = localStorage.getItem('supremeai_theme') as Theme | null;
    if (localTheme && THEME_ORDER.includes(localTheme)) {
      setTheme(localTheme);
    }
    
    // 2. ব্যাকএন্ড থেকে ফেচ করা (Cross-device sync)
    const API_BASE = getApiBaseUrl();
    fetch(`${API_BASE}/api/v1/preferences`, {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      signal: controller.signal
    })
      .then(res => res.ok ? res.json() : Promise.reject())
      .then(data => {
        if (data?.theme) {
          setTheme(data.theme);
          localStorage.setItem('supremeai_theme', data.theme);
        }
      })
      .catch(err => {
        if (err.name !== 'AbortError') {
          console.error('Theme sync failed:', err);
        }
      });

    return () => controller.abort(); // কম্পোনেন্ট আনমাউন্ট হলে রিকোয়েস্ট বাতিল
  }, []);

  useEffect(() => {
    // বাংলা মন্তব্য: HTML root এলিমেন্টে থিম ক্লাস অ্যাড করা হচ্ছে
    const root = window.document.documentElement;
    root.classList.remove('light', 'dark', 'sunset', 'matrix');
    root.classList.add(theme);
    root.setAttribute('data-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    // বাংলা মন্তব্য: পরবর্তী থিমে সাইকেল করা হচ্ছে (dark → light → sunset → matrix → dark)
    const currentIndex = THEME_ORDER.indexOf(theme);
    const nextIndex = (currentIndex + 1) % THEME_ORDER.length;
    const newTheme = THEME_ORDER[nextIndex];
    
    // Optimistic UI Update
    setTheme(newTheme);
    localStorage.setItem('supremeai_theme', newTheme);

    // ব্যাকএন্ডে async সিঙ্ক করা
    const API_BASE = getApiBaseUrl();
    const token = getAdminToken();
    fetch(`${API_BASE}/api/v1/preferences`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ theme: newTheme })
    }).catch(err => console.error('Failed to sync theme to DB:', err));
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

```