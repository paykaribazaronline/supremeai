# 📄 ফাইল: apps/studio-client/src/components/swarm/HoldToKillButton.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,544 বাইট  
**আপডেট:** 2026-07-11T13:46:44.194822

---

## কোড

```tsx
import React, { useState, useRef } from 'react';

interface Props {
  onTrigger: () => void;
}

export const HoldToKillButton: React.FC<Props> = ({ onTrigger }) => {
  const [isHolding, setIsHolding] = useState(false);
  const triggerRef = useRef(false); // To prevent double triggers

  const handlePointerDown = () => {
    setIsHolding(true);
    triggerRef.current = false;
  };

  const handlePointerUp = () => {
    setIsHolding(false);
  };

  const handleTransitionEnd = () => {
    if (isHolding && !triggerRef.current) {
      triggerRef.current = true;
      onTrigger();
      setIsHolding(false); // Reset visual state
    }
  };

  return (
    <div className="relative inline-block overflow-hidden rounded-md cursor-pointer select-none"
         onPointerDown={handlePointerDown}
         onPointerUp={handlePointerUp}
         onPointerLeave={handlePointerUp}>
      
      {/* Background/Progress Layer (Fills up in 2 seconds) */}
      <div 
        className="absolute left-0 top-0 h-full bg-danger opacity-80"
        style={{
          width: isHolding ? '100%' : '0%',
          transition: isHolding ? 'width 2s linear' : 'width 0.3s ease-out'
        }}
        onTransitionEnd={handleTransitionEnd}
      />

      {/* Button Text & Styling */}
      <div className="relative px-6 py-3 font-mono font-bold text-foreground border-2 border-danger rounded-md shadow-[0_0_15px_var(--color-danger)] z-10 pointer-events-none">
        {isHolding ? 'HOLDING TO KILL...' : 'HOLD TO HALT SWARM'}
      </div>
    </div>
  );
};

```