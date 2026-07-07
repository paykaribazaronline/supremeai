# 📄 ফাইল: apps/studio-client/src/i18n/I18nProvider.tsx

**প্রকার:** .tsx  
**সাইজ:** 659 বাইট  
**আপডেট:** 2026-07-07T11:35:20.620442

---

## কোড

```tsx
import { createContext, useContext } from 'react';
import { useTranslation } from '../hooks/useTranslation';

export const I18nContext = createContext({ t: (key: string) => key, locale: 'en', setLocale: (_next: string) => {} } satisfies Record<string, any>);

export const TranslationProvider = ({ locale, children }: { locale: string; children: React.ReactNode }) => {
  const { t, setLocale } = useTranslation(locale as any || 'en');
  return (
    <I18nContext.Provider value={{ t: t as any, locale: locale || 'en', setLocale: setLocale as any }}>
      {children}
    </I18nContext.Provider>
  );
};

export const useI18n = () => useContext(I18nContext);

```