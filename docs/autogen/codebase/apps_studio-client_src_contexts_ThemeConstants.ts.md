# 📄 ফাইল: apps/studio-client/src/contexts/ThemeConstants.ts

**প্রকার:** .ts  
**সাইজ:** 538 বাইট  
**আপডেট:** 2026-07-11T09:15:34.077302

---

## কোড

```ts
// বাংলা মন্তব্য: থিম সাইকেল অর্ডার (টগল বাটনে ক্লিক করলে পরবর্তী থিমে যাবে)
// Theme type এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে ThemeContext.ts এর সাথে সাইকেল ইম্পোর্ট না হয়
export type Theme = 'dark' | 'light' | 'sunset' | 'matrix';

export const THEME_ORDER: Theme[] = ['dark', 'light', 'sunset', 'matrix'];
```