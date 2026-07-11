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
  Connection,
  Edge,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import AgentNode from './nodes/AgentNode';
// Import Sidebar here later

// Register custom node types
const nodeTypes = {
  agentNode: AgentNode,
};

const initialNodes = [
  {
    id: '1',
    type: 'agentNode',
    position: { x: 250, y: 100 },
    data: { label: 'System Architect', role: 'Architect', model: 'GPT-4o' },
  },
];

const EvolutionForgeCanvas = () => {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

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

      // We will grab the dragged node type from the Sidebar event later
      const type = event.dataTransfer.getData('application/reactflow');
      if (typeof type === 'undefined' || !type) return;

      const position = {
        x: event.clientX - (reactFlowWrapper.current?.getBoundingClientRect().left ?? 0),
        y: event.clientY - (reactFlowWrapper.current?.getBoundingClientRect().top ?? 0),
      };

      const newNode = {
        id: `node_${Date.now()}`,
        type,
        position,
        data: { label: `New ${type}`, role: 'Coder', model: 'Gemini-1.5-Pro' },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [setNodes]
  );

  return (
    <div className="flex h-[calc(100vh-64px)] w-full bg-background" ref={reactFlowWrapper}>
      {/* <ForgeSidebar /> will go here */}
      
      <div className="flex-grow h-full relative">
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
