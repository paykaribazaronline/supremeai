# 📄 ফাইল: apps/studio-client/src/hooks/__tests__/useTranslation.test.ts

**প্রকার:** .ts  
**সাইজ:** 923 বাইট  
**আপডেট:** 2026-07-03T15:08:06.663661

---

## কোড

```ts
import { renderHook } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import { useTranslation } from '../useTranslation';

describe('useTranslation', () => {
  test('returns English fallback for known key', () => {
    const { result } = renderHook(() => useTranslation('en'));
    const value = result.current.t('appName');
    expect(value).toBe('SupremeAI Studio');
  });

  test('returns Bangla locale when requested', () => {
    const { result } = renderHook(() => useTranslation('bn'));
    const value = result.current.t('send');
    expect(value).toBe('পাঠান');
  });

  test('returns Spanish and Chinese', () => {
    const { result: es } = renderHook(() => useTranslation('es'));
    const { result: zh } = renderHook(() => useTranslation('zh'));
    expect(es.current.t('thinking')).toBe('Pensando...');
    expect(zh.current.t('newChat')).toBe('新对话');
  });
});

```