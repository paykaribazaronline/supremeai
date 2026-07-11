# 📄 ফাইল: apps/studio-client/src/pages/EvolutionForge/ForgeSidebar.tsx

**প্রকার:** .tsx  
**সাইজ:** 2,551 বাইট  
**আপডেট:** 2026-07-11T13:53:46.607267

---

## কোড

```tsx
import React from 'react';

// Pre-defined nodes that users can drag
const AVAILABLE_NODES = [
  { type: 'agentNode', label: 'Architect Agent', role: 'Architect', model: 'GPT-4o', borderColor: 'border-neon-purple' },
  { type: 'agentNode', label: 'Code Ninja', role: 'Coder', model: 'Gemini-1.5-Pro', borderColor: 'border-neon-blue' },
  { type: 'agentNode', label: 'Strict Reviewer', role: 'Reviewer', model: 'Claude-3.5', borderColor: 'border-brand-primary' },
  // Future node type preparation
  { type: 'taskNode', label: 'Input Trigger', role: 'Task', model: 'System', borderColor: 'border-text-muted' },
];

export const ForgeSidebar = () => {
  const onDragStart = (event: React.DragEvent, nodeData: any) => {
    // Pass the type for React Flow
    event.dataTransfer.setData('application/reactflow', nodeData.type);
    // Pass the actual data (role, model, label) as a JSON string
    event.dataTransfer.setData('application/json', JSON.stringify(nodeData));
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <aside className="w-72 h-full border-r border-border-subtle bg-card-bg/80 backdrop-blur-md p-4 flex flex-col gap-6 z-10 shadow-[4px_0_24px_rgba(0,0,0,0.2)]">
      <div>
        <h2 className="font-brand text-xl font-bold text-text-primary tracking-wide">Evolution Forge</h2>
        <p className="text-xs text-text-muted mt-1">Drag agents to the canvas to build your swarm.</p>
      </div>

      <div className="flex flex-col gap-3">
        <h3 className="text-sm font-bold text-text-secondary uppercase tracking-widest border-b border-border-subtle pb-2">Swarm Agents</h3>
        
        {AVAILABLE_NODES.map((node, index) => (
          <div
            key={index}
            draggable
            onDragStart={(event) => onDragStart(event, node)}
            className={`
              p-3 rounded-lg border-2 ${node.borderColor} bg-background/50 cursor-grab 
              hover:bg-background hover:shadow-[0_0_15px_var(--color-brand-primary)] 
              transition-all duration-fast ease-out
            `}
          >
            <div className="flex justify-between items-center mb-1">
              <span className="font-bold text-text-primary text-sm">{node.label}</span>
            </div>
            <div className="flex justify-between items-center text-xs">
              <span className="text-text-muted">{node.role}</span>
              <span className="text-brand-primary font-mono">{node.model}</span>
            </div>
          </div>
        ))}
      </div>
    </aside>
  );
};

```