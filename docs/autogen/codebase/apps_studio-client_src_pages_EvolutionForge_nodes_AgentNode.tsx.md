# 📄 ফাইল: apps/studio-client/src/pages/EvolutionForge/nodes/AgentNode.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,596 বাইট  
**আপডেট:** 2026-07-11T13:53:46.607646

---

## কোড

```tsx
import React, { memo } from 'react';
import { Handle, Position, type NodeProps } from '@xyflow/react';

// Custom data type for our Agent Node
export type AgentNodeData = {
  label: string;
  role: 'Architect' | 'Coder' | 'Reviewer' | 'Deployer';
  model: string;
};

const AgentNode = ({ data, selected }: NodeProps<AgentNodeData>) => {
  return (
    <div className={`
      relative min-w-[200px] p-4 rounded-xl border-2 backdrop-blur-md transition-all duration-fast
      ${selected 
        ? 'border-neon-blue shadow-[0_0_20px_var(--color-neon-blue)] bg-card-bg/90' 
        : 'border-border-subtle bg-card-bg/50 hover:border-text-secondary'}
    `}>
      {/* Incoming Data/Task Handle */}
      <Handle 
        type="target" 
        position={Position.Top} 
        className="w-3 h-3 bg-neon-purple border-2 border-background" 
      />

      {/* Node Content */}
      <div className="flex flex-col gap-2">
        <div className="flex justify-between items-center">
          <span className="font-brand font-bold text-text-primary text-lg">
            {data.label}
          </span>
          <span className="text-xs px-2 py-1 rounded-full bg-background border border-border-accent text-brand-primary">
            {data.model}
          </span>
        </div>
        <p className="text-sm text-text-muted">{data.role}</p>
      </div>

      {/* Outgoing Result Handle */}
      <Handle 
        type="source" 
        position={Position.Bottom} 
        className="w-3 h-3 bg-neon-blue border-2 border-background" 
      />
    </div>
  );
};

export default memo(AgentNode);

```