// বাংলা মন্তব্য: EvolutionForge কম্পোনেন্টে মিসিং useEffect ইম্পোর্ট যোগ করা হলো
import React, { useState, useEffect, useCallback, useRef } from 'react';
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
  type Node,
  useReactFlow,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import AgentNode from './nodes/AgentNode';
import TaskNode from './nodes/TaskNode';
import { ForgeSidebar } from './ForgeSidebar';
import { useForgeAutosave } from './hooks/useForgeAutosave';
import { DebateOverlay } from './DebateOverlay';
import { getApiBaseUrl } from '../../../utils/api';
import { apiClient } from '../../../services/apiClient';
import { eventBus, Events } from '../../../lib/eventBus';
import { Sparkles, Upload, CheckCircle, AlertCircle } from 'lucide-react';

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

import { useToast } from '../../../components/ui/Toast';

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

  const autosaved = loadAutosavedFlow();
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>(autosaved?.nodes || initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>(autosaved?.edges || []);

  const [isSaving, setIsSaving] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [isDebateOpen, setIsDebateOpen] = useState(false);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [debateLogs, setDebateLogs] = useState<any[]>([]);
  const { toObject } = useReactFlow();
  const { showToast } = useToast();

  const [showDeployDialog, setShowDeployDialog] = useState(false);
  const [deployStatus, setDeployStatus] = useState('idle');
  const [deployError, setDeployError] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('supremeai_auth_token');
    const sse = new EventSource(`${getApiBaseUrl()}/api/v1/swarm/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`);

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

  // Listen for newly installed skills
  useEffect(() => {
    const handleSkillCreated = (payload: any) => {
      showToast(`New skill integrated: ${payload.skillId}`, 'success');
    };
    eventBus.on(Events.SKILL_AUTO_CREATED, handleSkillCreated);
    return () => {
      eventBus.off(Events.SKILL_AUTO_CREATED, handleSkillCreated);
    };
  }, [showToast]);

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

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const buildForgePayload = (name: string, flow: any) => ({
    name,
    description: "Visual Swarm Architecture",
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    nodes: flow.nodes.map((n: any) => ({
      id: n.id,
      type: n.type,
      position: n.position,
      data: n.data
    })),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
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

      await apiClient.post('/api/v1/swarm/forge', payload);

      showToast('Swarm blueprint saved successfully! 🚀', 'success');

      if (payload.nodes && payload.nodes.length > 0) {
        eventBus.emit(Events.SKILL_AUTO_CREATED, {
          name: payload.name,
          agents: payload.nodes.filter((n: any) => n.type === 'agentNode').map((n: any) => n.data.role || 'Agent'),
          nodeCount: payload.nodes.length,
          source: 'evolution_forge',
          canDeploy: true,
          timestamp: Date.now(),
        });
        
        setShowDeployDialog(true);
      }
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

      await apiClient.post(`/api/v1/swarm/forge/${flowId}/execute`, payload);

      showToast('Swarm execution started successfully! 🚀 Check Swarm Health Dashboard for live telemetry.', 'success');
    } catch (error: any) {
      console.error('Execution failed', error);
      showToast(`Failed to execute swarm: ${error?.response?.data?.detail || error.message || 'Unknown error'}`, 'error');
    } finally {
      setIsExecuting(false);
    }
  };

  const handleDeployToMarketplace = async () => {
    setDeployStatus('deploying');
    setDeployError(null);
    
    try {
      const payload = buildForgePayload(`Swarm_${Date.now()}`, toObject());
      const result = await apiClient.post('/api/skills/deploy-blueprint', {
        name: payload.name,
        description: payload.description || `Auto-generated skill from Evolution Forge`,
        agents: payload.nodes.filter((n: any) => n.type === 'agentNode').map((n: any) => n.data.role || 'Agent'),
        nodes: payload.nodes,
        edges: payload.edges,
        category: 'automation',
      });
      
      setDeployStatus('success');
      
      // Notify listeners
      eventBus.emit(Events.SKILL_APPROVAL_NEEDED, {
        skillId: (result as any).data?.skillId || `skill_${Date.now()}`,
        name: payload.name,
        status: 'pending_review',
        timestamp: Date.now(),
      });
      
      eventBus.emit('deployment_status' as Events, {
        type: 'skill_published',
        skillId: (result as any).data?.skillId || `skill_${Date.now()}`,
        status: 'pending',
        timestamp: Date.now(),
      });
      
      setTimeout(() => {
        setShowDeployDialog(false);
        setDeployStatus('idle');
      }, 2500);
      
    } catch (e: any) {
      setDeployStatus('error');
      setDeployError(e.message || 'Deployment failed');
      console.error('[EvolutionForge] Deploy failed:', e);
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

      {/* Deploy to Marketplace Dialog */}
      {showDeployDialog && (
        <div className="absolute inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-[#161b22] border border-cyan-500/30 rounded-xl shadow-2xl p-6 w-[450px]">
            <h3 className="text-xl font-bold text-white flex items-center gap-2 mb-4">
              <Sparkles className="text-cyan-400" />
              Deploy to Skill Marketplace
            </h3>
            
            <div className="bg-[#0d1117] p-4 rounded-lg mb-6 border border-slate-700">
              <p className="text-sm text-slate-300 mb-2">Ready to deploy <strong>Current Blueprint</strong>:</p>
              <ul className="text-xs text-slate-400 space-y-1 list-disc pl-4">
                <li><strong>{nodes.filter(n => n.type === 'agentNode').length}</strong> agents configured</li>
                <li><strong>{nodes.length}</strong> nodes in workflow</li>
                <li>Type: General Automation</li>
              </ul>
            </div>
            
            {deployStatus === 'success' ? (
              <div className="text-center py-4">
                <CheckCircle size={48} className="text-green-400 mx-auto mb-3" />
                <h4 className="text-lg font-bold text-white">Successfully Submitted!</h4>
                <p className="text-sm text-slate-400">Your skill is now pending review.</p>
              </div>
            ) : deployStatus === 'error' ? (
              <div className="text-center py-4">
                <AlertCircle size={48} className="text-red-400 mx-auto mb-3" />
                <h4 className="text-lg font-bold text-white">Deployment Failed</h4>
                <p className="text-sm text-red-300 mt-2">{deployError}</p>
              </div>
            ) : (
              <div className="flex items-center gap-3 justify-end">
                <button
                  onClick={() => setShowDeployDialog(false)}
                  className="px-4 py-2 text-sm text-slate-300 hover:text-white transition-colors"
                  disabled={deployStatus === 'deploying'}
                >
                  Cancel
                </button>
                <button
                  onClick={handleDeployToMarketplace}
                  className="px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg flex items-center gap-2 transition-colors disabled:opacity-50"
                  disabled={deployStatus === 'deploying'}
                >
                  {deployStatus === 'deploying' ? (
                    'Deploying...'
                  ) : (
                    <>
                      <Upload size={16} />
                      Publish to Marketplace
                    </>
                  )}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
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
