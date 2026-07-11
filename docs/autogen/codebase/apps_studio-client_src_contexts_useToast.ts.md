# 📄 ফাইল: apps/studio-client/src/contexts/useToast.ts

**প্রকার:** .ts  
**সাইজ:** 504 বাইট  
**আপডেট:** 2026-07-11T17:16:16.950505

---

## কোড

```ts
import { useContext } from 'react';
import { ToastContext } from './ToastContext';

// বাংলা মন্তব্য: useToast hook এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ToastProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};
```