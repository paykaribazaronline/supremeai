# 📄 ফাইল: apps/studio-client/src/components/ui/Input.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,605 বাইট  
**আপডেট:** 2026-07-11T17:11:02.732420

---

## কোড

```tsx
import React from 'react';
import { cn } from '../../utils/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, helperText, ...props }, ref) => {
    return (
      <div className="flex w-full flex-col gap-1.5">
        {label && (
          <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-foreground">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={cn(
            "flex h-10 w-full rounded-md border border-[var(--supremeai-color-neutral-100)] bg-transparent px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--supremeai-color-brand-500)] disabled:cursor-not-allowed disabled:opacity-50 text-foreground transition-colors",
            error && "border-[var(--supremeai-color-brand-danger-light)] focus-visible:ring-[var(--supremeai-color-brand-danger-light)]",
            className
          )}
          {...props}
        />
        {error && (
          <p className="text-sm font-medium text-[var(--supremeai-color-brand-danger-light)]">
            {error}
          </p>
        )}
        {helperText && !error && (
          <p className="text-sm text-[var(--supremeai-color-neutral-500)]">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

```