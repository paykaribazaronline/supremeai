# 📄 ফাইল: apps/studio-client/src/i18n/I18nProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 771 বাইট  
**আপডেট:** 2026-07-11T11:14:17.625533

---

## কোড

```tsx
import { useTranslation } from '../hooks/useTranslation';
import { I18nContext } from './I18nContext';

// বাংলা মন্তব্য: I18nContext একে অপর ফাইল থেকে ইম্পোর্ট করা হয়েছে, যাতে react-refresh সতর্কতা দূর হয়
// useI18n hook একে অপর ফাইলে সরানো হয়েছে (useI18n.ts)
export const TranslationProvider = ({ locale, children }: { locale: string; children: React.ReactNode }) => {
  const { t, setLocale } = useTranslation(locale as any || 'en');
  return (
    <I18nContext.Provider value={{ t: t as any, locale: locale || 'en', setLocale: setLocale as any }}>
      {children}
    </I18nContext.Provider>
  );
};
```