# 📄 ফাইল: packages/ui-components/src/components/SupremeHeader.tsx

**প্রকার:** .tsx  
**সাইজ:** 837 বাইট  
**আপডেট:** 2026-07-11T11:14:17.514998

---

## কোড

```tsx
import React from 'react';

export interface SupremeHeaderProps extends React.HTMLAttributes<HTMLHeadingElement> {
  children: React.ReactNode;
  subtitle?: React.ReactNode;
  gradient?: boolean;
}

export const SupremeHeader: React.FC<SupremeHeaderProps> = ({ 
  children, 
  subtitle,
  gradient = false,
  className = '',
  ...props 
}) => {
  const titleColor = gradient 
    ? "bg-gradient-to-r from-accent-primary to-neon-purple bg-clip-text text-transparent" 
    : "text-foreground";

  return (
    <div className={`mb-6 ${className}`}>
      <h1 className={`text-2xl md:text-3xl font-bold tracking-tight ${titleColor}`} {...props}>
        {children}
      </h1>
      {subtitle && (
        <p className="mt-2 text-sm text-text-secondary font-mono tracking-wide">
          {subtitle}
        </p>
      )}
    </div>
  );
};

```