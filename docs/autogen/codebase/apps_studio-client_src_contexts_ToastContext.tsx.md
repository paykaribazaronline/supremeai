# 📄 ফাইল: apps/studio-client/src/contexts/ToastContext.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,405 বাইট  
**আপডেট:** 2026-07-05T18:19:45.302286

---

## কোড

```tsx
import React, { createContext, useContext, useState, useCallback, ReactNode, useEffect } from 'react';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  showToast: (type: ToastType, message: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export let globalShowToast: (type: ToastType, message: string) => void = () => {
  console.warn("ToastProvider is not mounted yet.");
};

if (typeof window !== 'undefined') {
  (window as any).showGlobalToast = (type: ToastType, message: string) => {
    globalShowToast(type, message);
  };
}

export const ToastProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((type: ToastType, message: string) => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, message }]);
    
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  useEffect(() => {
    globalShowToast = showToast;
    if (typeof window !== 'undefined') {
      (window as any).showGlobalToast = showToast;
    }
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

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
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