# 📄 ফাইল: apps/studio-client/src/test/setup.ts

**প্রকার:** .ts  
**সাইজ:** 874 বাইট  
**আপডেট:** 2026-07-08T12:03:41.274234

---

## কোড

```ts
import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
  writable: true
});

class EventSourceMock {
  onopen: (() => void) | null = null;
  onmessage: ((event: any) => void) | null = null;
  onerror: (() => void) | null = null;
  close = vi.fn();
  url: string;
  constructor(url: string) {
    this.url = url;
  }
}
Object.defineProperty(global, 'EventSource', {
  value: EventSourceMock,
  writable: true,
});

```