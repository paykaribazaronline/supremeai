# 📄 ফাইল: apps/studio-client/src/components/ui/Skeleton.tsx

**প্রকার:** .tsx  
**সাইজ:** 156 বাইট  
**আপডেট:** 2026-07-08T04:03:20.385732

---

## কোড

```tsx
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-slate-800/50 rounded ${className}`} />;
}

```