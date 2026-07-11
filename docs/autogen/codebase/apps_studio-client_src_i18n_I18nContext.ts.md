# 📄 ফাইল: apps/studio-client/src/i18n/I18nContext.ts

**প্রকার:** .ts  
**সাইজ:** 418 বাইট  
**আপডেট:** 2026-07-11T11:14:17.625279

---

## কোড

```ts
import { createContext } from 'react';

// বাংলা মন্তব্য: I18nContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে I18nProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const I18nContext = createContext({ t: (key: string) => key, locale: 'en', setLocale: (_next: string) => {} } satisfies Record<string, any>);
```