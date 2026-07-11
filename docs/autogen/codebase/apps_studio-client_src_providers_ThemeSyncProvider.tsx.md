# 📄 ফাইল: apps/studio-client/src/providers/ThemeSyncProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,520 বাইট  
**আপডেট:** 2026-07-11T15:50:11.390377

---

## কোড

```tsx
import React, { useEffect, useState } from 'react';
import { ThemeSyncContext } from './ThemeSyncContext';

// বাংলা মন্তব্য: ThemeSyncContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
// useThemeSync hook একে অপর ফাইলে সরানো হয়েছে (useThemeSync.ts)
export const ThemeSyncProvider: React.FC<{ children: React.ReactNode; userId?: string }> = ({ 
  children, 
  userId = 'default' 
}) => {
  const [theme, setThemeState] = useState<string>('dark');

  useEffect(() => {
    // Listen for Server-Sent Events from FastAPI
    const eventSource = new EventSource(`http://127.0.0.1:8000/api/preferences/${userId}/stream`);

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.event === 'theme_changed' && data.theme) {
          console.log('[ThemeSync] Theme updated via SSE:', data.theme);
          setThemeState(data.theme);
        }
      } catch (err) {
        console.error('[ThemeSync] Error parsing SSE message:', err);
      }
    };

    if (typeof eventSource.addEventListener === 'function') {
      eventSource.addEventListener('connected', () => {
        console.log('[ThemeSync] Connected to SSE Stream for user:', userId);
      });
    }

    eventSource.onerror = (err) => {
      console.error('[ThemeSync] SSE Connection Error:', err);
    };

    return () => {
      eventSource.close();
    };
  }, [userId]);

  // Apply theme class to HTML body/root whenever it changes
  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('dark', 'light', 'sunset');
    
    // Add the new theme class if it's not the default root theme
    if (theme === 'dark' || theme === 'sunset') {
      root.classList.add(theme);
    }
  }, [theme]);

  const setTheme = async (newTheme: string) => {
    setThemeState(newTheme);
    // Push the change to backend
    try {
      await fetch(`http://127.0.0.1:8000/api/preferences/?user_id=${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ theme: newTheme }),
      });
    } catch (err) {
      console.error('[ThemeSync] Failed to push theme to API:', err);
    }
  };

  return (
    <ThemeSyncContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeSyncContext.Provider>
  );
};
```