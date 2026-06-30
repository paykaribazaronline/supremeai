import React, { createContext, useContext, useEffect, useState } from 'react';

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
    // 1. লোকাল স্টোরেজ থেকে থিম পড়া (Optimistic Load)
    const localTheme = localStorage.getItem('supremeai_theme') as Theme | null;
    if (localTheme && THEME_ORDER.includes(localTheme)) {
      setTheme(localTheme);
    }
    
    // 2. ব্যাকএন্ড থেকে ফেচ করা (Cross-device sync)
    const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';
    const token = localStorage.getItem('supremeai_admin_token') || '';
    fetch(`${API_BASE}/api/v1/preferences`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(res => res.json())
      .then(data => {
        if (data && data.theme && data.theme !== localTheme) {
          setTheme(data.theme);
          localStorage.setItem('supremeai_theme', data.theme);
        }
      })
      .catch(err => console.log('Background theme sync skipped or failed:', err));
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
    const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';
    const token = localStorage.getItem('supremeai_admin_token') || '';
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
