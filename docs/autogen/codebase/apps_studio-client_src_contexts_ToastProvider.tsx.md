# 📄 ফাইল: apps/studio-client/src/contexts/ToastProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,060 বাইট  
**আপডেট:** 2026-07-11T13:28:09.044819

---

## কোড

```tsx
import React, { useState, useCallback, useEffect } from 'react';
import { ToastContext, globalShowToastRef } from './ToastContext';
import type { ToastType } from './ToastContext';

// বাংলা মন্তব্য: ToastContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
// useToast hook একে অপর ফাইলে সরানো হয়েছে (useToast.ts)
export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Array<{ id: string; type: ToastType; message: string }>>([]);

  const showToast = useCallback((type: ToastType, message: string) => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, message }]);
    
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  useEffect(() => {
    // Update the global ref to point to the current showToast function
    globalShowToastRef.current = showToast;
  }, [showToast]);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div style={styles.container}>
        {toasts.map((t) => (
          <div key={t.id} style={{ ...styles.toast, ...styles[t.type] }}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'fixed',
    top: '20px',
    right: '20px',
    zIndex: 9999,
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
  },
  toast: {
    padding: '12px 20px',
    borderRadius: '10px',
    color: '#fff',
    fontSize: '0.875rem',
    fontWeight: 600,
    boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
    animation: 'fade-in 0.3s ease-out forwards',
  },
  success: {
    background: '#065f46',
  },
  error: {
    background: '#7f1d1d',
  },
  info: {
    background: '#1e3a8a',
  },
};
```