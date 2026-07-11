# 📄 ফাইল: apps/studio-client/src/providers/ThemeSyncContext.ts

**প্রকার:** .ts  
**সাইজ:** 499 বাইট  
**আপডেট:** 2026-07-11T09:15:34.076887

---

## কোড

```ts
import { createContext } from 'react';

// বাংলা মন্তব্য: ThemeSyncContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ThemeSyncProvider.tsx এ রেফ্রেশ সমস্যা না হয়
interface ThemeSyncContextType {
  theme: string;
  setTheme: (theme: string) => void;
}

export const ThemeSyncContext = createContext<ThemeSyncContextType>({
  theme: 'dark', // default theme
  setTheme: () => {},
});
```