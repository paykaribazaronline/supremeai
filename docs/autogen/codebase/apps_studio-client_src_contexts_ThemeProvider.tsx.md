# 📄 ফাইল: apps/studio-client/src/contexts/ThemeProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,930 বাইট  
**আপডেট:** 2026-07-11T09:20:27.538946

---

## কোড

```tsx
import React, { useEffect, useState } from 'react';
import { getAdminToken } from '../services/adminTokenStore';
import { getApiBaseUrl } from '../utils/api';
import { THEME_ORDER } from './ThemeConstants';
import { ThemeContext } from './ThemeContext';
import type { Theme } from './ThemeConstants';

// বাংলা মন্তব্য: ThemeContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
// useTheme hook একে অপর ফাইলে সরানো হয়েছে (useTheme.ts)
export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>('dark'); // ডিফল্ট Deep Space (dark)

  useEffect(() => {
    // বাংলা মন্তব্য: Race Condition এড়াতে AbortController ব্যবহার করা হয়েছে
    const controller = new AbortController();
    const token = getAdminToken();

    if (!token) return;

    // বাংলা মন্তব্য: set-state-in-effect ফিক্স — থিম লোডিং async ফাংশনের ভেতরে করা হয়েছে
    const loadTheme = async () => {
      // 1. লোকাল স্টোরেজ থেকে থিম পড়া (Optimistic Load)
      const localTheme = localStorage.getItem('supremeai_theme') as Theme | null;
      if (localTheme && THEME_ORDER.includes(localTheme)) {
        setTheme(localTheme);
      }
      
      // 2. ব্যাকএন্ড থেকে ফেচ করা (Cross-device sync)
      const API_BASE = getApiBaseUrl();
      try {
        const res = await fetch(`${API_BASE}/api/v1/preferences`, {
          headers: {
            'Authorization': `Bearer ${token}`
          },
          signal: controller.signal
        });
        if (res.ok) {
          const data = await res.json();
          if (data?.theme) {
            setTheme(data.theme);
            localStorage.setItem('supremeai_theme', data.theme);
          }
        }
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          console.error('Theme sync failed:', err);
        }
      }
    };

    loadTheme();

    return () => controller.abort(); // কম্পোনেন্ট আনমাউন্ট হলে রিকোয়েস্ট বাতিল
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
```