# 📄 ফাইল: apps/studio-client/src/i18n/config.ts

**প্রকার:** .ts  
**সাইজ:** 231 বাইট  
**আপডেট:** 2026-07-05T19:37:54.404486

---

## কোড

```ts
export const locales = ['en', 'bn', 'es', 'zh'] as const;

export type Locale = (typeof locales)[number];

export const localeNames: Record<Locale, string> = {
  en: 'English',
  bn: 'Bengali',
  es: 'Spanish',
  zh: 'Chinese',
};

```