import { ReactFlow, Background, Controls } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useSwarmGraph } from '../hooks/useSwarmGraph';
import { AgentNode } from './nodes/AgentNode';
import { SkillNode } from './nodes/SkillNode';

const nodeTypes = {
  agent: AgentNode,
  skill: SkillNode,
};

const defaultEdgeOptions = {
  animated: true,
  style: { stroke: '#06b6d4', strokeWidth: 2 },
};

export default function SwarmMap() {
  const { nodes, edges, onNodesChange, onEdgesChange } = useSwarmGraph();

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={nodeTypes}
        defaultEdgeOptions={defaultEdgeOptions}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        fitView
        snapToGrid
      >
        <Background />
        <Controls />
      </ReactFlow>
    </div>
  );
}
