# 📄 ফাইল: apps/studio-client/src/hooks/useSwarmGraph.ts

**প্রকার:** .ts  
**সাইজ:** 2,837 বাইট  
**আপডেট:** 2026-07-11T19:00:24.776202

---

## কোড

```ts
import { useQuery } from '@tanstack/react-query';
import { useCallback, useState, useEffect } from 'react';
import { applyNodeChanges, applyEdgeChanges } from '@xyflow/react';

export const useSwarmGraph = () => {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);

  const { data: delta } = useQuery({
    queryKey: ['swarm-graph'],
    queryFn: async () => {
      const res = await fetch('/api/evolution/swarm-graph');
      return res.json(); // ব্যাকএন্ড থেকে {added: {nodes:[], edges:[]}, removed: {nodes:[], edges:[]}}
    },
    refetchInterval: 2000, // ২ সেকেন্ড পর পর পোলিং
    // @ts-expect-error Backend type mismatch
    onSuccess: (delta) => {
      // 🧠 Delta Merging Logic
      // ১. Remove old nodes/edges
      setNodes((nds) => nds.filter(n => !delta.removed.nodes.find(rn => rn.id === n.id)));
      setEdges((eds) => eds.filter(e => !delta.removed.edges.find(re => re.source === e.source && re.target === e.target)));

      // ২. Add new nodes/edges
      setNodes((nds) => [...nds, ...delta.added.nodes]);
      setEdges((eds) => [...eds, ...delta.added.edges]);
    }
  });

  // 🧬 New: Agent Health Polling
  const agentIds = nodes.filter(n => n.type === 'agent').map(n => n.id);
  
  const { data: healthData } = useQuery({
    queryKey: ['agent-health', agentIds],
    queryFn: async () => {
      if (agentIds.length === 0) return {};
      const res = await fetch('/api/health/agents', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ agent_ids: agentIds })
      });
      return res.json();
    },
    refetchInterval: 2000, // ২ সেকেন্ড পর পর হার্টবিট চেক
    enabled: agentIds.length > 0, // এজেন্ট থাকলেই কেবল পোলিং হবে
  });

  // Health Data নোডের সাথে মার্জ করা
  useEffect(() => {
    if (healthData) {
      // বাংলা মন্তব্য: set-state-in-effect ফিক্স — নোড আপডেট async ফাংশনের ভেতরে করা হয়েছে
      const updateNodeHealth = () => {
        setNodes((nds) => nds.map(node => {
          if (node.type === 'agent' && healthData[node.id]) {
            return { ...node, data: { ...node.data, health: healthData[node.id] } };
          }
          return node;
        }));
      };
      updateNodeHealth();
    }
  }, [healthData]);

  const onNodesChange = useCallback((changes) => setNodes((nds) => applyNodeChanges(changes, nds)), []);
  const onEdgesChange = useCallback((changes) => setEdges((eds) => applyEdgeChanges(changes, eds)), []);

  return { nodes, edges, onNodesChange, onEdgesChange };
};

```