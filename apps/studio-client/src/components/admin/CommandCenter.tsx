import { useEffect, useState, useMemo } from 'react';
import ReactFlow, {
  Background,
  useNodesState,
  useEdgesState,
} from 'reactflow';
import 'reactflow/dist/style.css';
import './AethelCoreStyles.css';
import { 
  Cpu, 
  Send,
  Mic,
  Maximize2,
  Settings,
  Activity,
  Shield,
  LifeBuoy,
  FileText
} from 'lucide-react';
import AethelNode from './AethelNode';
import { useAdminStore } from '../../store/adminStore';

// বাংলা মন্তব্য: চ্যাট এবং ভয়েস ওভাররাইডের জন্য ডামি কথোপকথন ডাটা ডিক্লেয়ার করা হচ্ছে
const initialChat = [
  { id: 1, sender: 'Admin', text: 'Execute deployment check on Node 47.' },
  { id: 2, sender: 'Aethel', text: 'Analyzing Node 47 (Analytics). Status: Nominal. Load: 38%. Connected.' },
  { id: 3, sender: 'System', text: 'Optimizing cluster nodes...' },
];

export function CommandCenter() {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges] = useEdgesState([]);
  const [chatMessages, setChatMessages] = useState(initialChat);
  const [chatInput, setChatInput] = useState('');
  const [voiceActive] = useState(true);
  const [isCentralPanelOpen, setIsCentralPanelOpen] = useState(false);
  const setAdminSubTab = useAdminStore(state => state.setAdminSubTab);

  // Custom node types for ReactFlow
  const nodeTypes = useMemo(() => ({ aethel: AethelNode }), []);

  const handleSendChat = () => {
    if (!chatInput.trim()) return;
    setChatMessages(prev => [
      ...prev,
      { id: Date.now(), sender: 'Admin', text: chatInput },
      { id: Date.now() + 1, sender: 'Aethel', text: `Processing command "${chatInput}"... Authorization confirmed.` }
    ]);
    setChatInput('');
  };

  const handleNodeClick = (_, node) => {
    if (node.id === 'central-orb') {
      setIsCentralPanelOpen(prev => !prev);
    } else if (node.data?.type) {
      // Map node types to tabs
      const typeMap: Record<string, string> = {
        'model-router': 'model-router',
        'provider-map': 'model-router',
        'skills': 'skills',
        'memory': 'memory',
        'real-time-logs': 'logs',
        'observability': 'observability',
        'threats': 'threats',
        'rules': 'rules',
        'rate-limits': 'rate-limits',
        'cloud': 'cloud',
        'cicd': 'cicd',
        'github': 'github',
        'backups': 'backups',
        'sandbox': 'sandbox',
        'costs': 'costs',
        'users': 'users',
        'config': 'config'
      };
      if (typeMap[node.data.type]) {
        setAdminSubTab(typeMap[node.data.type]);
      }
    }
  };

  useEffect(() => {
    const initialNodes = [
      {
        id: 'central-orb',
        type: 'default',
        data: {
          label: (
            <div className="flex flex-col items-center justify-center p-2 text-center h-full w-full">
              <span className="font-bold text-[10px] tracking-widest text-[#00f3ff] uppercase mb-4">AETHEL CENTRAL ORC</span>
              <div className="central-orb-outer" style={{ width: '80px', height: '80px' }}>
                <div className="central-orb-inner" style={{ width: '60px', height: '60px' }}>
                  <div className="central-orb-core flex items-center justify-center bg-[#00f3ff] w-[45px] h-[45px] rounded-full shadow-[0_0_35px_#00f3ff]">
                    <Cpu size={24} className="text-slate-950" />
                  </div>
                </div>
              </div>
              <div className="mt-4 flex flex-col gap-1">
                <span className="text-[10px] text-[#00ff66] font-mono font-bold">v.10.0</span>
                <span className="text-[9px] text-slate-400 font-mono tracking-widest">NEXUS ONLINE</span>
              </div>
            </div>
          )
        },
        position: { x: 450, y: 250 },
        className: 'border-none flex items-center justify-center bg-transparent z-50',
        style: { width: 240, height: 280 }
      },
      // 🌌 CLUSTER 1: AI & Brain (Top Left)
      { id: 'node-models', type: 'aethel', data: { type: 'model-router', status: 'Nominal', label: 'MODELS' }, position: { x: 150, y: 80 } },
      { id: 'node-providers', type: 'aethel', data: { type: 'provider-map', status: 'Nominal', label: 'PROVIDERS' }, position: { x: 300, y: 30 } },
      { id: 'node-skills', type: 'aethel', data: { type: 'skills', status: 'Nominal', label: 'SKILLS' }, position: { x: 450, y: -20 } },
      { id: 'node-memory', type: 'aethel', data: { type: 'memory', status: 'Nominal', label: 'MEMORY' }, position: { x: 600, y: 30 } },

      // 🛡️ CLUSTER 2: Security & Ops (Bottom Left)
      { id: 'node-logs', type: 'aethel', data: { type: 'real-time-logs', status: 'Nominal', label: 'RT_LOGS' }, position: { x: 50, y: 200 } },
      { id: 'node-obs', type: 'aethel', data: { type: 'observability', status: 'Nominal', label: 'OBSERVE' }, position: { x: 100, y: 320 } },
      { id: 'node-threats', type: 'aethel', data: { type: 'threats', status: 'Nominal', label: 'THREATS' }, position: { x: 200, y: 420 } },
      { id: 'node-rules', type: 'aethel', data: { type: 'rules', status: 'Nominal', label: 'RULES' }, position: { x: 350, y: 500 } },
      { id: 'node-limits', type: 'aethel', data: { type: 'rate-limits', status: 'Nominal', label: 'LIMITS' }, position: { x: 500, y: 540 } },

      // ⚙️ CLUSTER 3: DevOps & Infra (Top Right)
      { id: 'node-cloud', type: 'aethel', data: { type: 'cloud', status: 'Nominal', label: 'CLOUD' }, position: { x: 750, y: 80 } },
      { id: 'node-cicd', type: 'aethel', data: { type: 'cicd', status: 'Nominal', label: 'CI/CD' }, position: { x: 900, y: 160 } },
      { id: 'node-github', type: 'aethel', data: { type: 'github', status: 'Nominal', label: 'GITHUB' }, position: { x: 980, y: 280 } },
      { id: 'node-backups', type: 'aethel', data: { type: 'backups', status: 'Nominal', label: 'BACKUPS' }, position: { x: 900, y: 400 } },
      { id: 'node-sandbox', type: 'aethel', data: { type: 'sandbox', status: 'Nominal', label: 'SANDBOX' }, position: { x: 750, y: 480 } },

      // 👥 CLUSTER 4: Management & Finance (Bottom Right)
      { id: 'node-costs', type: 'aethel', data: { type: 'costs', status: 'Nominal', label: 'FINANCE' }, position: { x: 300, y: 220 } },
      { id: 'node-users', type: 'aethel', data: { type: 'users', status: 'Nominal', label: 'USERS' }, position: { x: 250, y: 320 } },
      { id: 'node-config', type: 'aethel', data: { type: 'config', status: 'Nominal', label: 'CONFIG' }, position: { x: 650, y: 480 } }
    ];

    const initialEdges = [
      // Cluster 1 Edges (AI) - Cyan
      { id: 'e-nexus-models', source: 'central-orb', target: 'node-models', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-nexus-providers', source: 'central-orb', target: 'node-providers', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-nexus-skills', source: 'central-orb', target: 'node-skills', animated: true, style: { stroke: '#00f3ff', strokeWidth: 2 } },
      { id: 'e-nexus-memory', source: 'central-orb', target: 'node-memory', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      
      // Cluster 2 Edges (Security) - Green/Red
      { id: 'e-nexus-logs', source: 'central-orb', target: 'node-logs', animated: true, style: { stroke: '#00ff66', strokeWidth: 1.5 } },
      { id: 'e-nexus-obs', source: 'central-orb', target: 'node-obs', animated: true, style: { stroke: '#00ff66', strokeWidth: 1.5 } },
      { id: 'e-nexus-threats', source: 'central-orb', target: 'node-threats', animated: true, style: { stroke: '#ff003c', strokeWidth: 2 } },
      { id: 'e-nexus-rules', source: 'central-orb', target: 'node-rules', animated: true, style: { stroke: '#ffbd2e', strokeWidth: 1.5 } },
      { id: 'e-nexus-limits', source: 'central-orb', target: 'node-limits', animated: true, style: { stroke: '#ffbd2e', strokeWidth: 1.5 } },

      // Cluster 3 Edges (Infra) - Blue/Purple
      { id: 'e-nexus-cloud', source: 'central-orb', target: 'node-cloud', animated: true, style: { stroke: '#8b5cf6', strokeWidth: 2 } },
      { id: 'e-nexus-cicd', source: 'central-orb', target: 'node-cicd', animated: true, style: { stroke: '#8b5cf6', strokeWidth: 1.5 } },
      { id: 'e-nexus-github', source: 'central-orb', target: 'node-github', animated: true, style: { stroke: '#8b5cf6', strokeWidth: 1.5 } },
      { id: 'e-nexus-backups', source: 'central-orb', target: 'node-backups', animated: true, style: { stroke: '#3b82f6', strokeWidth: 1.5 } },
      { id: 'e-nexus-sandbox', source: 'central-orb', target: 'node-sandbox', animated: true, style: { stroke: '#3b82f6', strokeWidth: 2 } },

      // Cluster 4 Edges (Management) - Yellow
      { id: 'e-nexus-costs', source: 'central-orb', target: 'node-costs', animated: true, style: { stroke: '#f59e0b', strokeWidth: 1.5 } },
      { id: 'e-nexus-users', source: 'central-orb', target: 'node-users', animated: true, style: { stroke: '#f59e0b', strokeWidth: 1.5 } },
      { id: 'e-nexus-config', source: 'central-orb', target: 'node-config', animated: true, style: { stroke: '#f59e0b', strokeWidth: 1.5 } }
    ];

    setNodes(initialNodes);
    setEdges(initialEdges);
  }, []);

  return (
    <div className="flex-grow flex flex-col overflow-hidden bg-[#030611] text-white scanline relative h-screen font-mono p-4">
      {/* ── TOP HUD HEADER BAR ────────────────────────────────────── */}
      <header className="flex justify-between items-center border-b border-[#00f3ff]/15 pb-2 mb-4">
        <div className="flex items-center gap-2">
          <span className="text-[#00f3ff] animate-pulse">▲</span>
          <span className="text-xs font-bold tracking-widest text-[#00f3ff] uppercase">AETHEL ORCHESTRATOR | ADM-01</span>
        </div>
        <div className="text-sm font-bold tracking-widest text-[#00f3ff] uppercase">
          AETHEL CORE
        </div>
        <div className="flex items-center gap-4 text-[10px] text-[#00f3ff] font-bold">
          <span>14:32 | OCT 26</span>
          <span className="text-[#00ff66]">📶 SYSTEM ONLINE</span>
        </div>
      </header>

      {/* ── MAIN WORKSPACE CONTENT ────────────────────────────────── */}
      <div className="flex-1 flex flex-row gap-4 overflow-hidden mb-4">
        
        {/* Core React Flow Telemetry Canvas */}
        <div className="flex-1 bg-[#050917]/50 border border-[#00f3ff]/15 rounded-xl relative overflow-hidden flex flex-col">
          <div className="flex-1 w-full h-full relative">
            <ReactFlow
              nodes={nodes}
              edges={edges}
              nodeTypes={nodeTypes}
              onNodesChange={onNodesChange}
              fitView
              zoomOnScroll={false}
              zoomOnDoubleClick={false}
              preventScrolling={true}
              panOnDrag={false}
              nodesDraggable={true}
              onNodeClick={handleNodeClick}
            >
              <Background color="#00f3ff" gap={24} style={{ opacity: 0.03 }} />
            </ReactFlow>
          </div>

          {/* Floating Telemetry Waveform Panel at Bottom of Canvas */}
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 w-[380px] bg-[#060b1b]/90 border border-[#00f3ff]/25 rounded-lg p-3 shadow-[0_0_20px_rgba(0,243,255,0.15)] backdrop-blur-md z-20 flex flex-col items-center">
            <div className="flex justify-between w-full text-[9px] text-slate-400 font-bold mb-1">
              <span>CMD | v3.0</span>
              <div className="flex items-center gap-1.5">
                <Mic size={10} className={voiceActive ? 'text-[#00f3ff] animate-pulse' : 'text-slate-500'} />
                <span>VOICE CONTROLS</span>
              </div>
            </div>

            {/* Glowing Waveform representation */}
            <div className="flex items-center justify-center h-8 my-1 w-full">
              <span className="waveform-bar" style={{ height: '8px' }} />
              <span className="waveform-bar pulse-delay-1" style={{ height: '14px' }} />
              <span className="waveform-bar pulse-delay-2" style={{ height: '22px' }} />
              <span className="waveform-bar pulse-delay-3" style={{ height: '12px' }} />
              <span className="waveform-bar pulse-delay-4" style={{ height: '26px' }} />
              <span className="waveform-bar pulse-delay-5" style={{ height: '10px' }} />
              <span className="waveform-bar pulse-delay-1" style={{ height: '18px' }} />
              <span className="waveform-bar pulse-delay-2" style={{ height: '6px' }} />
            </div>

            <div className="flex justify-between w-full text-[8px] text-slate-500 font-bold mt-1">
              <span className="text-[#00ff66]">Voice Recognition: Listening...</span>
              <span>CPU: 74% MEM: 68%</span>
            </div>
          </div>
        </div>

        {/* Right Sidebar: AI Assistant Glassmorphic Chat Panel (SLIDING OVERLAY) */}
        <div className={`absolute top-0 right-0 h-full w-[350px] bg-[#050917]/80 border-l border-[#00f3ff]/30 shadow-[-10px_0_30px_rgba(0,243,255,0.05)] backdrop-blur-xl transform transition-transform duration-500 ease-in-out z-50 flex flex-col ${isCentralPanelOpen ? 'translate-x-0' : 'translate-x-full'}`}>
          <div className="border-b border-[#00f3ff]/20 p-4 flex justify-between items-center bg-[#070d22]/80">
            <div className="flex items-center gap-2">
              <Cpu size={16} className="text-[#00f3ff] animate-pulse" />
              <span className="text-sm font-black tracking-widest text-[#00f3ff] uppercase drop-shadow-[0_0_8px_#00f3ff]">AETHEL NEXUS</span>
            </div>
            <Maximize2 size={14} className="text-[#00f3ff] cursor-pointer hover:scale-110 transition-transform" onClick={() => setIsCentralPanelOpen(false)} />
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {chatMessages.map(msg => (
              <div key={msg.id} className="text-[11px] leading-relaxed border-b border-cyan-900/10 pb-3">
                <span className={`font-bold tracking-wider mr-1.5 ${
                  msg.sender === 'Admin' ? 'text-slate-300' : msg.sender === 'Aethel' ? 'text-[#00f3ff]' : 'text-emerald-500'
                }`}>
                  {msg.sender}:
                </span>
                <span className="text-slate-300">{msg.text}</span>
              </div>
            ))}
          </div>

          <div className="p-4 border-t border-[#00f3ff]/20 bg-[#060b1c]/90 flex gap-2">
            <input
              type="text"
              value={chatInput}
              onChange={e => setChatInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSendChat()}
              placeholder="[Aethel Nexus Command...]"
              className="flex-grow bg-[#030611] border border-cyan-500/30 focus:border-[#00f3ff]/80 rounded-lg px-3 py-2 text-[11px] text-[#00f3ff] outline-none placeholder:text-slate-600 font-mono tracking-wide"
            />
            <button onClick={handleSendChat} className="bg-[#00f3ff]/20 hover:bg-[#00f3ff]/40 hover:shadow-[0_0_10px_#00f3ff] text-[#00f3ff] p-2 rounded-lg transition-all">
              <Send size={14} />
            </button>
          </div>
        </div>

      </div>

      {/* ── BOTTOM DECK CONTROLLER / ICON PANELS ──────────────────── */}
      <footer className="flex justify-between items-center border-t border-[#00f3ff]/15 pt-2 text-[10px] text-slate-400 font-bold">
        {/* Left Deck Actions */}
        <div className="flex gap-4">
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <Settings size={12} className="text-[#00f3ff]" />
            <span>Settings</span>
          </button>
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <Activity size={12} className="text-[#00ff66]" />
            <span>Metrics</span>
          </button>
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <FileText size={12} className="text-yellow-500" />
            <span>Logs</span>
          </button>
        </div>

        {/* Right Deck Status Indicator */}
        <div className="flex gap-4">
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <FileText size={12} className="text-[#00f3ff]" />
            <span>Logs</span>
          </button>
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <Shield size={12} className="text-[#00ff66]" />
            <span>Security</span>
          </button>
          <button className="flex items-center gap-1.5 hover:text-white transition-colors">
            <LifeBuoy size={12} className="text-yellow-500" />
            <span>Health</span>
          </button>
        </div>
      </footer>
    </div>
  );
}
