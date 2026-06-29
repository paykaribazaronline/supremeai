import React, { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'dark' | 'light';

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>('dark'); // Default to Deep Space (dark)

  useEffect(() => {
    // 1. Check local storage first (Optimistic Load)
    const localTheme = localStorage.getItem('supremeai_theme') as Theme | null;
    if (localTheme) {
      setTheme(localTheme);
    }
    
    // 2. Fetch from backend (Cross-device sync / Incognito)
    fetch('/api/v1/preferences', {
      headers: {
        // Typically credentials include cookies or Authorization headers
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
    // Apply theme to document element
    const root = window.document.documentElement;
    root.classList.remove('light', 'dark');
    root.classList.add(theme);
    
    // For React Flow or custom variables
    root.setAttribute('data-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    const newTheme = theme === 'dark' ? 'light' : 'dark';
    
    // Optimistic UI Update
    setTheme(newTheme);
    localStorage.setItem('supremeai_theme', newTheme);

    // Sync back to backend async
    fetch('/api/v1/preferences', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
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
