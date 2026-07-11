# 📄 ফাইল: apps/studio-client/src/components/ui/Toast.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,645 বাইট  
**আপডেট:** 2026-07-11T11:14:17.634094

---

## কোড

```tsx
import React, { useState } from 'react';
import { ToastContext } from './ToastContext';
import type { ToastType, ToastMessage } from './ToastContext';

// বাংলা মন্তব্য: ToastContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
// useToast hook একে অপর ফাইলে সরানো হয়েছে (useToastUI.ts)
export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = (message: string, type: ToastType = 'info') => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, message, type }]);
    
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
        {toasts.map((toast) => (
          <div 
            key={toast.id} 
            className={`px-4 py-3 rounded shadow-lg text-white font-brand text-sm transition-all animate-fade-in
              ${toast.type === 'success' ? 'bg-green-600' : 
                toast.type === 'error' ? 'bg-red-600' : 'bg-blue-600'}
            `}
          >
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

// Re-export for convenience
export type { ToastType, ToastMessage } from './ToastContext';
export { useToast } from './useToastUI';
```