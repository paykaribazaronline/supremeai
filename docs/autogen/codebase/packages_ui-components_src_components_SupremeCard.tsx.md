# 📄 ফাইল: packages/ui-components/src/components/SupremeCard.tsx

**প্রকার:** .tsx  
**সাইজ:** 923 বাইট  
**আপডেট:** 2026-07-11T09:05:57.835480

---

## কোড

```tsx
import React from 'react';

export interface SupremeCardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  glow?: boolean;
  blur?: boolean;
}

export const SupremeCard: React.FC<SupremeCardProps> = ({ 
  children, 
  glow = false, 
  blur = true,
  className = '',
  ...props 
}) => {
  const baseStyle = "rounded-3xl border border-border-accent bg-card-bg transition-all";
  const motionStyle = "duration-[var(--supremeai-motion-duration-normal)] ease-[var(--supremeai-motion-easing-bounce)]";
  const glowStyle = glow ? "shadow-[0_0_15px_var(--supremeai-color-brand-primary-dark)] hover:shadow-[0_0_25px_var(--supremeai-color-brand-primary-dark)]" : "shadow-xl";
  const blurStyle = blur ? "backdrop-blur-xl" : "";

  return (
    <div 
      className={`${baseStyle} ${motionStyle} ${glowStyle} ${blurStyle} p-6 ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};

```