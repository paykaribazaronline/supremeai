# 📄 ফাইল: apps/studio-client/src/pages/EvolutionForge/EvolutionForge.tsx

**প্রকার:** .tsx  
**সাইজ:** 9,159 বাইট  
**আপডেট:** 2026-07-11T13:46:44.213288

---

## কোড

```tsx
import React, { useState, useCallback, useRef } from 'react';
import {
  ReactFlow,
  ReactFlowProvider,
  addEdge,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  MiniMap,
  type Connection,
  type Edge,
  useReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import AgentNode from './nodes/AgentNode';
import TaskNode from './nodes/TaskNode';
import { ForgeSidebar } from './ForgeSidebar';
import { useForgeAutosave } from './hooks/useForgeAutosave';
import { DebateOverlay } from './DebateOverlay';

// Register custom node types
const nodeTypes = {
  agentNode: AgentNode,
  taskNode: TaskNode,
};

const initialNodes = [
  {
    id: '1',
    type: 'agentNode',
    position: { x: 250, y: 100 },
    data: { label: 'System Architect', role: 'Architect', model: 'GPT-4o' },
  },
];

import { useToast } from '../../components/ui/Toast';

const loadAutosavedFlow = () => {
  try {
    const saved = localStorage.getItem('supremeai_forge_autosave');
    if (saved) {
      return JSON.parse(saved);
    }
  } catch (e) {
    console.error("[SupremeAI Forge] Failed to parse autosave data, clearing cache.", e);
    localStorage.removeItem('supremeai_forge_autosave');
  }
  return null;
};

const EvolutionForgeCanvas = () => {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  
  const [nodes, setNodes, onNodesChange] = useNodesState(() => loadAutosavedFlow()?.nodes || initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(() => loadAutosavedFlow()?.edges || []);
  
  const [isSaving, setIsSaving] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [isDebateOpen, setIsDebateOpen] = useState(false);
  const [debateLogs, setDebateLogs] = useState<any[]>([]);
  const { toObject } = useReactFlow();
  const { showToast } = useToast();

  useEffect(() => {
    const sse = new EventSource('/api/v1/swarm/stream');
    
    sse.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        if (payload.type === 'DEBATE_UPDATE') {
          setIsDebateOpen(true);
          const logData = payload.data;
          
          let message = '';
          if (logData.state === 'PROPOSING') message = `Starting Debate Cycle ${logData.iteration || ''}`;
          if (logData.proposals_count) message = `Generated ${logData.proposals_count} proposals. Judge evaluating...`;
          if (logData.feedback) message = `Rethinking based on feedback: ${logData.feedback}`;
          if (logData.winning_agent) message = `Consensus reached by ${logData.winning_agent}`;
          
          setDebateLogs(prev => [...prev, {
            agentName: 'ConsensusEngine',
            status: logData.state,
            message: message || `Status updated to ${logData.state}`
          }]);
        }
      } catch (err) {
        console.error("SSE Parse error", err);
      }
    };

    return () => sse.close();
  }, []);

  // 🔄 Attach auto-save listener
  useForgeAutosave(nodes, edges);

  const onConnect = useCallback(
    (params: Connection | Edge) => setEdges((eds) => addEdge({ ...params, animated: true }, eds)),
    [setEdges]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');
      if (typeof type === 'undefined' || !type) return;

      // Extract the JSON payload we sent from the sidebar
      const nodeDataString = event.dataTransfer.getData('application/json');
      const nodeData = nodeDataString ? JSON.parse(nodeDataString) : {};

      const position = {
        x: event.clientX - (reactFlowWrapper.current?.getBoundingClientRect().left ?? 0),
        y: event.clientY - (reactFlowWrapper.current?.getBoundingClientRect().top ?? 0),
      };

      const newNode = {
        id: `agent_${Date.now()}`,
        type,
        position,
        data: { 
          label: nodeData.label || 'New Node', 
          role: nodeData.role || 'Unknown', 
          model: nodeData.model || 'Unknown',
          prompt: '' 
        },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [setNodes]
  );

  const buildForgePayload = (name: string, flow: any) => ({
    name,
    description: "Visual Swarm Architecture",
    nodes: flow.nodes.map((n: any) => ({
      id: n.id,
      type: n.type,
      position: n.position,
      data: n.data
    })),
    edges: flow.edges.map((e: any) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      animated: e.animated || false
    }))
  });

  const handleSaveSwarm = async () => {
    const token = localStorage.getItem('supremeai_auth_token');
    if (!token) {
      showToast('Authentication required to save swarm.', 'error');
      return;
    }

    try {
      setIsSaving(true);
      const payload = buildForgePayload(`Swarm_${Date.now()}`, toObject());

      const response = await fetch('/api/v1/swarm/forge', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error('Failed to save swarm blueprint');
      showToast('Swarm blueprint saved successfully! 🚀', 'success');
    } catch (error) {
      console.error('Save failed:', error);
      showToast('Error saving swarm blueprint.', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  const handleExecuteSwarm = async () => {
    const token = localStorage.getItem('supremeai_auth_token');
    if (!token) {
      showToast('Authentication required to execute swarm.', 'error');
      return;
    }

    try {
      setIsExecuting(true);
      const payload = buildForgePayload(`Swarm_${Date.now()}`, toObject());

      // We use a dummy flow_id for now, in a real app this would be the saved swarm ID
      const flowId = `flow_${Date.now()}`;
      
      const response = await fetch(`/api/v1/swarm/forge/${flowId}/execute`, {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        showToast('Swarm execution started successfully! 🚀 Check Swarm Health Dashboard for live telemetry.', 'success');
      } else {
        const errorData = await response.json();
        showToast(`Failed to execute swarm: ${errorData.detail || 'Unknown error'}`, 'error');
      }
    } catch (error) {
      console.error('Execution failed', error);
      showToast('Error executing swarm blueprint.', 'error');
    } finally {
      setIsExecuting(false);
    }
  };

  return (
    <div className="flex h-[calc(100vh-64px)] w-full bg-background relative" ref={reactFlowWrapper}>
      <ForgeSidebar />
      <DebateOverlay isOpen={isDebateOpen} logs={debateLogs} />
      
      <div className="flex-grow h-full relative">
        <div className="absolute top-4 right-4 z-10 flex gap-3">
          <button 
            onClick={handleExecuteSwarm}
            disabled={isExecuting || isSaving}
            className={`
              px-6 py-2 rounded-lg font-brand font-bold text-sm text-background
              transition-all duration-fast shadow-[0_0_15px_var(--color-neon-purple)]
              ${isExecuting ? 'bg-neon-purple/50 cursor-not-allowed' : 'bg-neon-purple hover:bg-neon-purple/80'}
            `}
          >
            {isExecuting ? 'Executing...' : '⚡ Execute Flow'}
          </button>
          
          <button 
            onClick={handleSaveSwarm}
            disabled={isSaving || isExecuting}
            className={`
              px-6 py-2 rounded-lg font-brand font-bold text-sm text-background
              transition-all duration-fast shadow-[0_0_15px_var(--color-brand-primary)]
              ${isSaving ? 'bg-brand-primary/50 cursor-not-allowed' : 'bg-brand-primary hover:bg-brand-primary/80'}
            `}
          >
            {isSaving ? 'Saving...' : '💾 Save Swarm'}
          </button>
        </div>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onDrop={onDrop}
          onDragOver={onDragOver}
          nodeTypes={nodeTypes}
          fitView
          className="bg-bg-void"
        >
          <Background color="var(--color-border-subtle)" gap={16} />
          <Controls className="bg-card-bg border border-border-subtle fill-text-primary" />
          <MiniMap 
            nodeColor="var(--color-brand-primary)" 
            maskColor="var(--color-bg-surface)" 
            className="bg-background border border-border-subtle"
          />
        </ReactFlow>
      </div>
    </div>
  );
};

export default function EvolutionForge() {
  return (
    <ReactFlowProvider>
      <EvolutionForgeCanvas />
    </ReactFlowProvider>
  );
}

```