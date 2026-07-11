# 📄 ফাইল: apps/studio-client/src/providers/SwarmHealthContext.ts

**প্রকার:** .ts  
**সাইজ:** 460 বাইট  
**আপডেট:** 2026-07-11T13:28:09.044287

---

## কোড

```ts
import { createContext } from 'react';
import type { SwarmContextState, SwarmMetrics, SwarmLog, CircuitState } from '../../types/swarm';

// বাংলা মন্তব্য: SwarmHealthContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে MockSwarmProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const SwarmHealthContext = createContext<SwarmContextState | null>(null);
```