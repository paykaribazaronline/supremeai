import React from 'react';
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

interface NodeStatus {
    id: string;
    name: string;
    type: 'PROVIDER' | 'AGENT' | 'DATABASE' | 'NETWORK';
    status: 'online' | 'busy' | 'error' | 'standby';
    latency: number;
    load: number;
    lastSeen: string;
}

interface SystemHealthMatrixProps {
    nodes?: NodeStatus[];
}

const SystemHealthMatrix: React.FC<SystemHealthMatrixProps> = ({ nodes: propNodes }) => {
    // Simulated data if no nodes provided
    const defaultNodes: NodeStatus[] = [
        { id: '1', name: 'GPT-4O_CORE', type: 'PROVIDER', status: 'online', latency: 42, load: 12, lastSeen: 'NOW' },
        { id: '2', name: 'CLAUDE-3.5', type: 'PROVIDER', status: 'busy', latency: 156, load: 88, lastSeen: 'NOW' },
        { id: '3', name: 'FIRESTORE_MAIN', type: 'DATABASE', status: 'online', latency: 4, load: 5, lastSeen: 'NOW' },
        { id: '4', name: 'EDGE_NODE_LON', type: 'NETWORK', status: 'online', latency: 28, load: 14, lastSeen: 'NOW' },
        { id: '5', name: 'EDGE_NODE_NYC', type: 'NETWORK', status: 'error', latency: 0, load: 0, lastSeen: '2M_AGO' },
        { id: '6', name: 'GUARDIAN_AGENT', type: 'AGENT', status: 'standby', latency: 0, load: 0, lastSeen: '5M_AGO' },
        { id: '7', name: 'RESEARCH_AGENT', type: 'AGENT', status: 'online', latency: 85, load: 34, lastSeen: 'NOW' },
        { id: '8', name: 'GEMINI_PRO_1.5', type: 'PROVIDER', status: 'online', latency: 64, load: 22, lastSeen: 'NOW' },
        { id: '9', name: 'REDIS_CACHE', type: 'DATABASE', status: 'online', latency: 1, load: 2, lastSeen: 'NOW' },
        { id: '10', name: 'INTERNAL_DNS', type: 'NETWORK', status: 'online', latency: 2, load: 1, lastSeen: 'NOW' },
    ];

    const nodes = propNodes || defaultNodes;

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'online': return 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20';
            case 'busy': return 'text-amber-500 bg-amber-500/10 border-amber-500/20';
            case 'error': return 'text-red-500 bg-red-500/10 border-red-500/20';
            case 'standby': return 'text-blue-500 bg-blue-500/10 border-blue-500/20';
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

    return (
        <div className="area-health flex flex-col gap-3 h-full">
            <div className="flex items-center justify-between border-b border-white/5 pb-2">
                <div className="flex flex-col">
                    <span className="text-[10px] font-black uppercase tracking-widest text-white/80">Node Integrity Matrix</span>
                    <span className="text-[8px] text-white/20 uppercase font-bold">Real-time Cluster Observation</span>
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
                        className={`p-2 rounded border flex flex-col justify-between h-[70px] transition-all group hover:bg-white/[0.02] ${getStatusColor(node.status)}`}
                    >
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black tracking-tighter truncate w-3/4 uppercase">{node.name}</span>
                            <div className="opacity-40">{getTypeIcon(node.type)}</div>
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
