# 📄 ফাইল: packages/ui-components/src/components/DashboardShell.tsx

**প্রকার:** .tsx  
**সাইজ:** 837 বাইট  
**আপডেট:** 2026-07-08T04:09:02.057539

---

## কোড

```tsx
import React from 'react';
import './styles.css';
import { LiveSujonBackground } from './LiveSujonBackground';

export function DashboardShell({ children, isServerOnline = false }: any) {
  return (
    <div className="relative min-h-screen flex bg-[#0b0f19] text-white">
      <LiveSujonBackground />
      <aside className="relative z-10 w-56 shrink-0 border-r border-white/[0.06] bg-[#080b13] flex flex-col">
        <div className="flex items-center gap-2 px-4 py-4 border-b border-white/[0.06]">
          <span className="text-blue-400 text-lg">▲</span>
          <h1 className="text-sm font-semibold tracking-wide m-0">SupremeAI</h1>
        </div>
      </aside>
      <main data-testid="dashboard-main" className="relative z-10 flex-1 min-w-0 overflow-y-auto flex flex-col">
        {children}
      </main>
    </div>
  );
}

```