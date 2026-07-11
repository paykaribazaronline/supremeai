import { createContext } from 'react';
import type { SwarmContextState, SwarmMetrics, SwarmLog, CircuitState } from '../../types/swarm';

// বাংলা মন্তব্য: SwarmHealthContext এখানে সরাসরি ডিফাইন করা হয়েছে, যাতে MockSwarmProvider.tsx এ রেফ্রেশ সমস্যা না হয়
export const SwarmHealthContext = createContext<SwarmContextState | null>(null);