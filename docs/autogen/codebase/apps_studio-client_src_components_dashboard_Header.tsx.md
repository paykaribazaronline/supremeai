# 📄 ফাইল: apps/studio-client/src/components/dashboard/Header.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,105 বাইট  
**আপডেট:** 2026-07-11T19:00:24.761393

---

## কোড

```tsx
import React from 'react';
import { Search, Bell, Moon, Sun, User } from 'lucide-react';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useAuthStore } from '../../store/authStore';

export const Header = ({
  theme,
  toggleTheme,
}: {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}) => {
  const user = useAuthStore((state) => state.user);

  return (
    <div className="w-full flex items-center justify-between">
      <div className="flex-1 max-w-md">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--supremeai-color-neutral-500)]" />
          <Input 
            placeholder="Search AI models, integrations..." 
            className="pl-9 bg-[var(--supremeai-color-neutral-100)] dark:bg-[var(--supremeai-color-neutral-900)] border-none"
          />
        </div>
      </div>
      
      <div className="flex items-center space-x-4">
        <Button variant="ghost" className="w-9 h-9 p-0 rounded-full">
          <Bell className="w-5 h-5 text-[var(--supremeai-color-neutral-500)]" />
        </Button>
        <Button 
          variant="ghost" 
          onClick={toggleTheme} 
          className="w-9 h-9 p-0 rounded-full"
        >
          {theme === 'dark' ? (
            <Sun className="w-5 h-5 text-[var(--supremeai-color-neutral-500)]" />
          ) : (
            <Moon className="w-5 h-5 text-[var(--supremeai-color-neutral-500)]" />
          )}
        </Button>
        
        <div className="h-8 w-8 rounded-full bg-[var(--supremeai-color-neutral-100)] dark:bg-[var(--supremeai-color-neutral-900)] flex items-center justify-center border border-[var(--supremeai-color-border-accent-light)] dark:border-[var(--supremeai-color-border-accent-dark)] overflow-hidden">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt={user.name} className="h-full w-full object-cover" />
          ) : (
            <User className="w-4 h-4 text-[var(--supremeai-color-neutral-500)]" />
          )}
        </div>
      </div>
    </div>
  );
};

```