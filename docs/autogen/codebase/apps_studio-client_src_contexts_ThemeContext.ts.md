# 📄 ফাইল: apps/studio-client/src/contexts/ThemeContext.ts

**প্রকার:** .ts  
**সাইজ:** 476 বাইট  
**আপডেট:** 2026-07-11T15:05:35.322795

---

## কোড

```ts
import { createContext } from 'react';
import type { Theme} from './ThemeConstants';

// বাংলা মন্তব্য: ThemeContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ThemeProvider.tsx এ রেফ্রেশ সমস্যা না হয়
interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeContextType | undefined>(undefined);
```