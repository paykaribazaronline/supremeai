# 📄 ফাইল: apps/studio-client/src/contexts/ToastContext.ts

**প্রকার:** .ts  
**সাইজ:** 1,450 বাইট  
**আপডেট:** 2026-07-11T15:50:11.391766

---

## কোড

```ts
import { createContext, ReactNode } from 'react';

// বাংলা মন্তব্য: Toast context types এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ToastProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  showToast: (type: ToastType, message: string) => void;
}

// বাংলা মন্তব্য: ToastContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ToastProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const ToastContext = createContext<ToastContextValue | undefined>(undefined);

// Global toast function for external access - using ref pattern
// বাংলা মন্তব্য: globalShowToastRef একে অপর ফাইলে সরানো হয়েছে, যাতে ToastProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const globalShowToastRef = { current: (() => {
  console.warn("ToastProvider is not mounted yet.");
}) as (type: ToastType, message: string) => void };

if (typeof window !== 'undefined') {
  (window as any).showGlobalToast = (type: ToastType, message: string) => {
    globalShowToastRef.current(type, message);
  };
}
```