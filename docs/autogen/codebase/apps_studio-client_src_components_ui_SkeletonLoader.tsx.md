# 📄 ফাইল: apps/studio-client/src/components/ui/SkeletonLoader.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,077 বাইট  
**আপডেট:** 2026-07-11T13:49:08.412948

---

## কোড

```tsx
import React from 'react';

interface SkeletonLoaderProps {
  className?: string;
  type?: 'card' | 'text' | 'avatar';
}

export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({ className = '', type = 'text' }) => {
  const baseClasses = "animate-pulse bg-slate-800/50 backdrop-blur-sm border border-slate-700/50 relative overflow-hidden";
  
  // Create a sweeping light effect inside the skeleton using ::after or just a sweeping div
  const shimmerEffect = (
    <div className="absolute inset-0 -translate-x-full animate-[shimmer_1.5s_infinite] bg-gradient-to-r from-transparent via-slate-600/20 to-transparent" />
  );

  if (type === 'card') {
    return (
      <div className={`rounded-2xl ${baseClasses} ${className}`}>
        {shimmerEffect}
      </div>
    );
  }

  if (type === 'avatar') {
    return (
      <div className={`rounded-full ${baseClasses} ${className}`}>
        {shimmerEffect}
      </div>
    );
  }

  // text lines
  return (
    <div className={`rounded-md ${baseClasses} h-4 ${className}`}>
      {shimmerEffect}
    </div>
  );
};

```