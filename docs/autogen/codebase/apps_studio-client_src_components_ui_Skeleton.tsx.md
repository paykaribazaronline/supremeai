# 📄 ফাইল: apps\studio-client\src\components\ui\Skeleton.tsx

**প্রকার:** .tsx  
**সাইজ:** 156 বাইট  
**আপডেট:** 2026-07-03T19:44:07.100424

---

## কোড

```tsx
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-slate-800/50 rounded ${className}`} />;
}

```