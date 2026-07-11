# 📄 ফাইল: apps/studio-client/src/contexts/useTheme.ts

**প্রকার:** .ts  
**সাইজ:** 517 বাইট  
**আপডেট:** 2026-07-11T17:00:45.051776

---

## কোড

```ts
import { useContext } from 'react';
import { ThemeContext } from './ThemeContext';

// বাংলা মন্তব্য: useTheme hook এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ThemeProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
```