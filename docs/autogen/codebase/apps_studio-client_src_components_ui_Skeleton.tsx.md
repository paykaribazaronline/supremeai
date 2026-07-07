# 📄 ফাইল: apps/studio-client/src/components/ui/Skeleton.tsx

**প্রকার:** .tsx  
**সাইজ:** 156 বাইট  
**আপডেট:** 2026-07-07T12:28:01.494999

---

## কোড

```tsx
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-slate-800/50 rounded ${className}`} />;
}

```