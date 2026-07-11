# 📄 ফাইল: apps/studio-client/src/utils/cn.ts

**প্রকার:** .ts  
**সাইজ:** 169 বাইট  
**আপডেট:** 2026-07-11T17:00:45.049594

---

## কোড

```ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

```