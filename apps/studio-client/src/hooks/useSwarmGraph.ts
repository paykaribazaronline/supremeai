import { useQuery } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
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

  const onNodesChange = useCallback((changes) => setNodes((nds) => applyNodeChanges(changes, nds)), []);
  const onEdgesChange = useCallback((changes) => setEdges((eds) => applyEdgeChanges(changes, eds)), []);

  return { nodes, edges, onNodesChange, onEdgesChange };
};
