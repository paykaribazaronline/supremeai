# 📄 ফাইল: apps/studio-client/src/providers/useThemeSync.ts

**প্রকার:** .ts  
**সাইজ:** 396 বাইট  
**আপডেট:** 2026-07-11T19:51:42.272195

---

## কোড

```ts
import { useContext } from 'react';
import { ThemeSyncContext } from './ThemeSyncContext';

// বাংলা মন্তব্য: useThemeSync hook এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ThemeSyncProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const useThemeSync = () => useContext(ThemeSyncContext);
```