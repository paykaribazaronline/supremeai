# 📄 ফাইল: apps/studio-client/src/components/LiveSujonBackground.tsx

**প্রকার:** .tsx  
**সাইজ:** 4,831 বাইট  
**আপডেট:** 2026-07-04T12:59:56.902472

---

## কোড

```tsx
// বাংলা মন্তব্য: "Sujon" লাইভ ব্যাকগ্রাউন্ড — প্রজেক্টের রিয়েল-টাইম AI কোরের অ্যাম্বিয়েন্ট ভিজুয়াল।
// সম্পূর্ণ CSS-অ্যানিমেশন ভিত্তিক (transform/opacity-only) — GPU হার্ডওয়্যার-অ্যাক্সিলারেটেড,
// কোনো JS টাইমার/canvas লুপ নেই বলে মেমরি লিক বা CPU ওভারহেডের সুযোগ নেই (Zero Operating Cost)।
import { useEffect, useState } from 'react';

export type SujonState = 'idle' | 'processing' | 'circuit_open';

// বাংলা মন্তব্য: যেকোনো পেজ (যেমন Automation Queue) এই ইভেন্ট দিয়ে Sujon-এর ভিজুয়াল স্টেট বদলাতে পারে
export const SUJON_STATE_EVENT = 'supremeai:sujon-state';

export function setSujonState(state: SujonState): void {
  window.dispatchEvent(new CustomEvent<SujonState>(SUJON_STATE_EVENT, { detail: state }));
}

export function useSujonState(): SujonState {
  const [state, setState] = useState<SujonState>('idle');
  useEffect(() => {
    const onState = (e: Event) => setState((e as CustomEvent<SujonState>).detail);
    window.addEventListener(SUJON_STATE_EVENT, onState);
    return () => window.removeEventListener(SUJON_STATE_EVENT, onState);
  }, []);
  return state;
}

// বাংলা মন্তব্য: স্টেট-ভিত্তিক গ্রেডিয়েন্ট ও অ্যানিমেশন কনফিগ — idle=শান্ত নীল/ধূসর,
// processing=দ্রুতগতির সায়ানেটিক পার্টিকল, circuit_open=গাঢ় লাল সতর্ক-আভা
const STATE_STYLES: Record<SujonState, { orbA: string; orbB: string; speed: string; opacity: string }> = {
  idle: {
    orbA: 'bg-blue-500/10',
    orbB: 'bg-slate-400/10',
    speed: '14s',
    opacity: 'opacity-60',
  },
  processing: {
    orbA: 'bg-cyan-400/25',
    orbB: 'bg-fuchsia-500/20',
    speed: '3s',
    opacity: 'opacity-90',
  },
  circuit_open: {
    orbA: 'bg-red-600/30',
    orbB: 'bg-rose-500/25',
    speed: '1.2s',
    opacity: 'opacity-100',
  },
};

interface LiveSujonBackgroundProps {
  state?: SujonState;
}

export function LiveSujonBackground({ state: forcedState }: LiveSujonBackgroundProps) {
  const liveState = useSujonState();
  const state = forcedState ?? liveState;
  const cfg = STATE_STYLES[state];

  return (
    <div
      data-testid="sujon-background"
      data-sujon-state={state}
      aria-hidden="true"
      className={`pointer-events-none fixed inset-0 overflow-hidden transition-opacity duration-1000 ${cfg.opacity}`}
      style={{ zIndex: 0, contain: 'strict' }}
    >
      {/* বাংলা মন্তব্য: will-change + translate3d দিয়ে GPU কম্পোজিটিং লেয়ারে রেন্ডার নিশ্চিত করা হয় */}
      <div
        className={`absolute -top-32 -left-32 h-96 w-96 rounded-full blur-3xl ${cfg.orbA}`}
        style={{
          willChange: 'transform',
          animation: `sujon-drift ${cfg.speed} ease-in-out infinite alternate`,
        }}
      />
      <div
        className={`absolute -bottom-32 -right-32 h-96 w-96 rounded-full blur-3xl ${cfg.orbB}`}
        style={{
          willChange: 'transform',
          animation: `sujon-drift ${cfg.speed} ease-in-out infinite alternate-reverse`,
        }}
      />
      {state === 'processing' && (
        <div
          className="absolute inset-0"
          style={{
            backgroundImage:
              'repeating-linear-gradient(115deg, transparent 0px, transparent 38px, rgba(34,211,238,0.08) 40px), url(/icons.svg#sujon-cyber-lines)',
            willChange: 'transform',
            animation: 'sujon-scan 2.4s linear infinite',
          }}
        />
      )}
      {state === 'circuit_open' && (
        <div
          className="absolute inset-0 bg-red-900/20"
          style={{ animation: 'sujon-flash 1.6s ease-out infinite' }}
        />
      )}
      <style>{`
        @keyframes sujon-drift {
          from { transform: translate3d(0, 0, 0) scale(1); }
          to { transform: translate3d(60px, 40px, 0) scale(1.15); }
        }
        @keyframes sujon-scan {
          from { transform: translate3d(-40px, 0, 0); }
          to { transform: translate3d(0, 0, 0); }
        }
        @keyframes sujon-flash {
          0% { opacity: 0.9; }
          30% { opacity: 0.25; }
          100% { opacity: 0.45; }
        }
      `}</style>
    </div>
  );
}

```