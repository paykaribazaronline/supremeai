import { useEffect, useState, useMemo, useRef } from 'react';
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
  FileText,
  Sun,
  Moon
} from 'lucide-react';
import AethelNode from './AethelNode';
import { useAdminStore } from '../../store/adminStore';
import { useTheme } from '../../contexts/ThemeContext';
import { AudioRecorderService } from '../../services/audio/AudioRecorderService';
import { AudioPlaybackService } from '../../services/audio/AudioPlaybackService';
import { WaveformVisualizer } from '../audio/WaveformVisualizer';
import { ServiceHealthMetrics } from './ServiceHealthMetrics';

// বাংলা মন্তব্য: চ্যাট এবং ভয়েস ওভাররাইডের জন্য ডামি কথোপকথন ডাটা ডিক্লেয়ার করা হচ্ছে
const initialChat = [
  { id: 1, sender: 'Admin', text: 'Execute deployment check on Node 47.' },
  { id: 2, sender: 'SupremeAI', text: 'Analyzing Node 47 (Analytics). Status: Nominal. Load: 38%. Connected.' },
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
  const { theme, toggleTheme } = useTheme();

  // Audio Engine State
  const [isRecording, setIsRecording] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const recorderRef = useRef<AudioRecorderService | null>(null);
  const [playbackService, setPlaybackService] = useState<AudioPlaybackService | null>(null);

  useEffect(() => {
    // Initialize Audio Services
    const service = new AudioPlaybackService();
    setPlaybackService(service);
    
    // Using relative URL or assuming backend runs on same domain + /api/voice/ws/voice or ws://localhost:8000/api/voice/ws/voice
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsHost = import.meta.env.VITE_API_URL ? new URL(import.meta.env.VITE_API_URL).host : '127.0.0.1:8000';
    const wsUrl = `${wsProtocol}//${wsHost}/api/voice/ws/voice`;
    
    recorderRef.current = new AudioRecorderService(wsUrl);

    recorderRef.current.onTranscript((text) => {
      setChatMessages(prev => [
        ...prev,
        { id: Date.now(), sender: 'Admin', text: text }
      ]);
    });

    const handleAethelSpeak = (e: any) => {
      const text = e.detail;
      setIsSpeaking(true);
      setChatMessages(prev => [
        ...prev,
        { id: Date.now(), sender: 'SupremeAI', text: text }
      ]);
      service.play(text);
      
      // Rough estimation to stop visualizer (in a real app, bind to onend)
      setTimeout(() => setIsSpeaking(false), text.length * 50 + 1000);
    };

    window.addEventListener('supremeai_speak', handleAethelSpeak);
    return () => {
      window.removeEventListener('supremeai_speak', handleAethelSpeak);
    };
  }, []);

  const toggleVoiceRecording = async () => {
    if (!recorderRef.current) return;
    
    if (isRecording) {
      recorderRef.current.stopRecording();
      setIsRecording(false);
    } else {
      await recorderRef.current.startRecording();
      setIsRecording(true);
    }
  };

  // Custom node types for ReactFlow
  const nodeTypes = useMemo(() => ({ aethel: AethelNode }), []);

  const handleSendChat = () => {
    if (!chatInput.trim()) return;
    setChatMessages(prev => [
      ...prev,
      { id: Date.now(), sender: 'Admin', text: chatInput }
    ]);
    
    if (recorderRef.current && (recorderRef.current as any).sendText) {
      (recorderRef.current as any).sendText(chatInput);
    } else {
      setChatMessages(prev => [
        ...prev,
        { id: Date.now() + 1, sender: 'SupremeAI', text: `Text module not connected. Offline processing command "${chatInput}"...` }
      ]);
    }
    
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
        // Open the chat panel
        setIsCentralPanelOpen(true);
        // Add a message
        setChatMessages(prev => [
          ...prev,
          { id: Date.now(), sender: 'System', text: `Accessing diagnostics for ${node.data.label}...` }
        ]);
        // Open the overlay
        setAdminSubTab(typeMap[node.data.type]);
      }
    }
  };

  useEffect(() => {
    const initialNodes = [
      {
        id: 'central-orb',
        type: 'aethel',
        position: { x: 450, y: 250 },
        data: { label: 'SUPREMEAI CENTRAL ORC', isCentral: true },
      },
      // Cluster 1: AI & Brain (Top Left)
      { id: 'node-router', type: 'aethel', position: { x: 250, y: 100 }, data: { label: 'Model Router', type: 'model-router' } },
      { id: 'node-provider', type: 'aethel', position: { x: 150, y: 200 }, data: { label: 'Provider Map', type: 'provider-map' } },
      { id: 'node-skills', type: 'aethel', position: { x: 200, y: 300 }, data: { label: 'Skills', type: 'skills' } },
      { id: 'node-memory', type: 'aethel', position: { x: 300, y: 380 }, data: { label: 'Memory', type: 'memory' } },

      // Cluster 2: Security & Ops (Top Right)
      { id: 'node-logs', type: 'aethel', position: { x: 650, y: 100 }, data: { label: 'Real-time Logs', type: 'real-time-logs' } },
      { id: 'node-observability', type: 'aethel', position: { x: 750, y: 200 }, data: { label: 'Observability', type: 'observability' } },
      { id: 'node-threats', type: 'aethel', position: { x: 700, y: 300 }, data: { label: 'Threats', type: 'threats' } },
      { id: 'node-rules', type: 'aethel', position: { x: 600, y: 380 }, data: { label: 'Rules', type: 'rules' } },
      { id: 'node-rate-limits', type: 'aethel', position: { x: 800, y: 350 }, data: { label: 'Rate Limits', type: 'rate-limits' } },

      // Cluster 3: DevOps & Infra (Bottom Left)
      { id: 'node-cloud', type: 'aethel', position: { x: 150, y: 450 }, data: { label: 'Cloud Mesh', type: 'cloud' } },
      { id: 'node-cicd', type: 'aethel', position: { x: 250, y: 550 }, data: { label: 'CI/CD Pipelines', type: 'cicd' } },
      { id: 'node-github', type: 'aethel', position: { x: 100, y: 550 }, data: { label: 'GitHub Sync', type: 'github' } },
      { id: 'node-backups', type: 'aethel', position: { x: 350, y: 600 }, data: { label: 'Backups', type: 'backups' } },
      { id: 'node-sandbox', type: 'aethel', position: { x: 200, y: 650 }, data: { label: 'Orchestrator Sandbox', type: 'sandbox' } },

      // Cluster 4: Management & Finance (Bottom Right)
      { id: 'node-costs', type: 'aethel', position: { x: 750, y: 450 }, data: { label: 'Cost Auditor', type: 'costs' } },
      { id: 'node-users', type: 'aethel', position: { x: 650, y: 550 }, data: { label: 'User Manager', type: 'users' } },
      { id: 'node-config', type: 'aethel', position: { x: 750, y: 650 }, data: { label: 'Config Editor', type: 'config' } }
    ];

    const initialEdges = [
      // Cluster 1 Edges (AI/Brain) - Cyan
      { id: 'e-nexus-router', source: 'central-orb', target: 'node-router', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-nexus-provider', source: 'central-orb', target: 'node-provider', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-nexus-skills', source: 'central-orb', target: 'node-skills', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },
      { id: 'e-nexus-memory', source: 'central-orb', target: 'node-memory', animated: true, style: { stroke: '#00f3ff', strokeWidth: 1.5 } },

      // Cluster 2 Edges (Security) - Green/Red
      { id: 'e-nexus-logs', source: 'central-orb', target: 'node-logs', animated: true, style: { stroke: '#10b981', strokeWidth: 1.5 } },
      { id: 'e-nexus-obs', source: 'central-orb', target: 'node-observability', animated: true, style: { stroke: '#10b981', strokeWidth: 1.5 } },
      { id: 'e-nexus-threats', source: 'central-orb', target: 'node-threats', animated: true, style: { stroke: '#ef4444', strokeWidth: 2 } },
      { id: 'e-nexus-rules', source: 'central-orb', target: 'node-rules', animated: true, style: { stroke: '#10b981', strokeWidth: 1.5 } },
      { id: 'e-nexus-ratelimits', source: 'central-orb', target: 'node-rate-limits', animated: true, style: { stroke: '#10b981', strokeWidth: 1.5 } },

      // Cluster 3 Edges (DevOps) - Blue/Purple
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
    <div className="flex-grow flex flex-col overflow-hidden bg-[var(--bg-main)] text-[var(--text-main)] scanline relative h-screen font-mono p-4 transition-colors duration-500">
      {/* ── TOP HUD HEADER BAR ────────────────────────────────────── */}
      <header className="flex justify-between items-center border-b border-[var(--border-accent)] pb-2 mb-4 transition-colors duration-500">
        <div className="flex items-center gap-2">
          <span className="text-[var(--accent-primary)] animate-pulse">▲</span>
          <span className="text-xs font-bold tracking-widest text-[var(--accent-primary)] uppercase">SUPREMEAI ORCHESTRATOR | ADM-01</span>
        </div>
        <div className="text-sm font-bold tracking-widest text-[var(--accent-primary)] uppercase flex items-center gap-4">
          SUPREMEAI CORE
          <button 
            onClick={toggleTheme} 
            className="p-1.5 rounded-full bg-[var(--bg-panel)] border border-[var(--border-accent)] hover:scale-110 transition-transform cursor-pointer"
            title="Toggle Dimension"
          >
            {theme === 'dark' ? <Sun size={14} className="text-yellow-400" /> : <Moon size={14} className="text-indigo-600" />}
          </button>
        </div>
        <div className="flex items-center gap-4 text-[10px] text-[var(--accent-primary)] font-bold">
          <span 
            onClick={useAdminStore(state => state.handleAdminLogout)}
            className="cursor-pointer hover:opacity-80 transition-opacity"
            title="Return to Login"
          >
            14:32 | OCT 26
          </span>
          <span className="text-[#00ff66]">📶 SYSTEM ONLINE</span>
        </div>
      </header>

      {/* ── MAIN WORKSPACE CONTENT ────────────────────────────────── */}
      <div className="flex-1 flex flex-row gap-4 overflow-hidden mb-4">
        
        {/* Core React Flow Telemetry Canvas */}
        <div className="flex-1 bg-[var(--bg-panel)] border border-[var(--border-accent)] rounded-xl relative overflow-hidden flex flex-col transition-colors duration-500">
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
              colorMode={theme === 'dark' ? 'dark' : 'light'}
            >
              <Background color="var(--accent-primary)" gap={24} style={{ opacity: theme === 'dark' ? 0.03 : 0.1 }} />
            </ReactFlow>
          </div>

          {/* Java Worker Metrics Widget Overlay */}
          <div className="absolute top-4 left-4 w-80 z-20">
            <ServiceHealthMetrics />
          </div>

          {/* Floating Telemetry Waveform Panel at Bottom of Canvas */}
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 w-[380px] bg-[#060b1b]/90 border border-[#00f3ff]/25 rounded-lg p-3 shadow-[0_0_20px_rgba(0,243,255,0.15)] backdrop-blur-md z-20 flex flex-col items-center">
            <div className="flex justify-between w-full text-[9px] text-slate-400 font-bold mb-1">
              <span>CMD | v3.0</span>
              <button 
                onClick={toggleVoiceRecording}
                className="flex items-center gap-1.5 hover:bg-[#00f3ff]/10 p-1 rounded transition-colors"
              >
                <Mic size={10} className={isRecording ? 'text-[#ef4444] animate-pulse' : 'text-[#00f3ff]'} />
                <span className={isRecording ? 'text-[#ef4444]' : 'text-slate-300'}>
                  {isRecording ? 'RECORDING...' : 'VOICE CONTROLS'}
                </span>
              </button>
            </div>

            {/* Glowing Waveform representation */}
            <div className="flex items-center justify-center h-8 my-1 w-full">
              <WaveformVisualizer 
                analyser={playbackService ? playbackService.getAnalyser() : null} 
                isActive={isRecording || isSpeaking} 
                color={isRecording ? '#ef4444' : '#00f3ff'}
              />
            </div>

            <div className="flex justify-between w-full text-[8px] text-slate-500 font-bold mt-1">
              <span className={isRecording ? "text-[#ef4444]" : "text-[#00ff66]"}>
                {isRecording ? "Voice Recognition: Listening..." : "Voice Recognition: Standby"}
              </span>
              <span>CPU: 74% MEM: 68%</span>
            </div>
          </div>
        </div>

        {/* Right Sidebar: AI Assistant Glassmorphic Chat Panel (SLIDING OVERLAY) */}
        <div className={`absolute top-0 right-0 h-full w-[350px] bg-[#050917]/80 border-l border-[#00f3ff]/30 shadow-[-10px_0_30px_rgba(0,243,255,0.05)] backdrop-blur-xl transform transition-transform duration-500 ease-in-out z-50 flex flex-col ${isCentralPanelOpen ? 'translate-x-0' : 'translate-x-full'}`}>
          <div className="border-b border-[#00f3ff]/20 p-4 flex justify-between items-center bg-[#070d22]/80">
            <div className="flex items-center gap-2">
              <Cpu size={16} className="text-[#00f3ff] animate-pulse" />
              <span className="text-sm font-black tracking-widest text-[#00f3ff] uppercase drop-shadow-[0_0_8px_#00f3ff]">SUPREMEAI NEXUS</span>
            </div>
            <Maximize2 size={14} className="text-[#00f3ff] cursor-pointer hover:scale-110 transition-transform" onClick={() => setIsCentralPanelOpen(false)} />
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {chatMessages.map(msg => (
              <div key={msg.id} className="text-[11px] leading-relaxed border-b border-cyan-900/10 pb-3">
                <span className={`font-bold tracking-wider mr-1.5 ${
                  msg.sender === 'Admin' ? 'text-slate-300' : msg.sender === 'SupremeAI' ? 'text-[#00f3ff]' : 'text-emerald-500'
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
              placeholder="[SupremeAI Nexus Command...]"
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
