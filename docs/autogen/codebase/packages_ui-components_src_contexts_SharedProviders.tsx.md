# 📄 ফাইল: packages/ui-components/src/contexts/SharedProviders.tsx

**প্রকার:** .tsx  
**সাইজ:** 476 বাইট  
**আপডেট:** 2026-07-08T19:34:18.838893

---

## কোড

```tsx
import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const SharedProviders: React.FC<{children: React.ReactNode}> = ({ children }) => {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

export default SharedProviders;

```