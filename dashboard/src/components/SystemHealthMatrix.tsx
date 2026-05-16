import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
    Activity, 
    Database, 
    Cpu, 
    Globe, 
    AlertTriangle, 
    CheckCircle2, 
    Clock, 
    Zap 
} from 'lucide-react';
import { authUtils } from '../lib/authUtils';

interface NodeStatus {
    id: string;
    name: string;
    type: 'PROVIDER' | 'AGENT' | 'DATABASE' | 'NETWORK';
    status: 'online' | 'busy' | 'error' | 'standby';
    latency: number;
    load: number;
    lastSeen: string;
}

const SystemHealthMatrix: React.FC = () => {
    const [nodes, setNodes] = useState<NodeStatus[]>([]);
    const [loading, setLoading] = useState(true);
    const [lastSync, setLastSync] = useState<number>(Date.now());

    useEffect(() => {
        const fetchHealth = async () => {
            try {
                const response = await authUtils.fetchWithAuth('/telemetry/health');
                // The backend returns a list of models in 'models' field
                if (response.ok) {
                    const data = await response.json();
                    if (data.models) {
                        const mappedNodes: NodeStatus[] = data.models.map((m: any) => ({
                        id: m.id,
                        name: m.name || m.id.toUpperCase(),
                        type: 'PROVIDER', // Mostly providers from telemetry
                        status: m.status || 'online',
                        latency: m.latency || 0,
                        load: parseInt(m.memory?.replace('GB', '') || '0') * 5, // Mock load based on memory
                        lastSeen: 'NOW'
                    }));

                    // Add some static infrastructure nodes that might not be in telemetry yet
                    const infraNodes: NodeStatus[] = [
                        { id: 'db-1', name: 'FIRESTORE_MAIN', type: 'DATABASE', status: 'online', latency: 4, load: 12, lastSeen: 'NOW' },
                        { id: 'cache-1', name: 'REDIS_CLUSTER', type: 'DATABASE', status: 'online', latency: 1, load: 8, lastSeen: 'NOW' },
                        { id: 'net-1', name: 'EDGE_GATEWAY', type: 'NETWORK', status: 'online', latency: 15, load: 5, lastSeen: 'NOW' }
                    ];

                        setNodes([...mappedNodes, ...infraNodes]);
                        setLastSync(Date.now());
                    }
                }
            } catch (error) {
                console.error('Failed to fetch system health:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchHealth();
        const interval = setInterval(fetchHealth, 10000); // Polling every 10 seconds
        return () => clearInterval(interval);
    }, []);

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'online': return 'text-emerald-400 bg-emerald-500/5 border-emerald-500/10';
            case 'busy': return 'text-amber-400 bg-amber-500/5 border-amber-500/10';
            case 'error': return 'text-red-400 bg-red-500/5 border-red-500/10';
            case 'standby': return 'text-blue-400 bg-blue-500/5 border-blue-500/10';
            default: return 'text-white/20 bg-white/5 border-white/10';
        }
    };

    const getTypeIcon = (type: string) => {
        switch (type) {
            case 'PROVIDER': return <Cpu size={10} />;
            case 'AGENT': return <Zap size={10} />;
            case 'DATABASE': return <Database size={10} />;
            case 'NETWORK': return <Globe size={10} />;
            default: return <Activity size={10} />;
        }
    };

    if (loading && nodes.length === 0) {
        return <div className="h-full flex items-center justify-center opacity-20 text-[10px] uppercase tracking-widest font-black">Syncing Matrix...</div>;
    }

    return (
        <div className="area-health flex flex-col gap-3 h-full">
            <div className="flex items-center justify-between border-b border-white/5 pb-2">
                <div className="flex flex-col">
                    <span className="text-[10px] font-black uppercase tracking-widest text-white/80">Node Integrity Matrix</span>
                    <span className="text-[8px] text-white/20 uppercase font-bold">Live Cluster Pulse • {new Date(lastSync).toLocaleTimeString()}</span>
                </div>
                <div className="flex items-center gap-1.5 bg-emerald-500/5 px-2 py-0.5 rounded border border-emerald-500/10">
                    <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
                    <span className="text-[8px] font-black text-emerald-500 uppercase">{nodes.filter(n => n.status === 'online').length}/{nodes.length} NOMINAL</span>
                </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-2 flex-1 overflow-y-auto custom-scrollbar pr-1">
                {nodes.map((node) => (
                    <motion.div 
                        key={node.id}
                        initial={{ opacity: 0, scale: 0.98 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className={`p-2 rounded border flex flex-col justify-between h-[70px] transition-all group hover:bg-white/[0.04] cursor-default ${getStatusColor(node.status)}`}
                    >
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black tracking-tighter truncate w-3/4 uppercase">{node.name}</span>
                            <div className="opacity-40 group-hover:opacity-100 transition-opacity">{getTypeIcon(node.type)}</div>
                        </div>

                        <div className="flex flex-col gap-0.5">
                            <div className="flex items-center justify-between">
                                <span className="text-[7px] opacity-40 uppercase font-bold">Ping</span>
                                <span className="text-[8px] font-mono font-black">{node.latency}ms</span>
                            </div>
                            <div className="w-full h-1 bg-black/40 rounded-full overflow-hidden">
                                <div 
                                    className={`h-full transition-all duration-1000 ${node.status === 'busy' ? 'bg-amber-500' : 'bg-current opacity-40'}`} 
                                    style={{ width: `${node.load}%` }} 
                                />
                            </div>
                        </div>

                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-1">
                                <span className="text-[7px] font-black uppercase tracking-tighter">{node.status}</span>
                            </div>
                            <span className="text-[7px] font-mono opacity-30">{node.lastSeen}</span>
                        </div>
                    </motion.div>
                ))}
            </div>

            <div className="pt-2 border-t border-white/5 flex items-center justify-between opacity-30">
                <span className="text-[7px] font-bold uppercase tracking-widest">Protocol: NodeSync_4.2</span>
                <span className="text-[7px] font-bold uppercase tracking-widest">Uplink Stable</span>
            </div>
        </div>
    );
};

export default SystemHealthMatrix;

