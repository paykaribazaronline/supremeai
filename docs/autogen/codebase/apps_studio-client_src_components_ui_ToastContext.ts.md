# 📄 ফাইল: apps/studio-client/src/components/ui/ToastContext.ts

**প্রকার:** .ts  
**সাইজ:** 810 বাইট  
**আপডেট:** 2026-07-11T09:05:57.934806

---

## কোড

```ts
import { createContext } from 'react';

// বাংলা মন্তব্য: Toast context types এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে Toast.tsx এ রেফ্রেশ সমস্যা না হয়
export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
}

// বাংলা মন্তব্য: ToastContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে Toast.tsx এ রেফ্রেশ সমস্যা না হয়
export const ToastContext = createContext<ToastContextType | undefined>(undefined);
```