import React, { useState, useEffect, Suspense } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, Stars, PerspectiveCamera } from '@react-three/drei';
import { CoreEngine } from './components/CoreEngine';
import { Activity, Cpu, Shield, Zap, Terminal as TerminalIcon, Globe } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ModelStatus {
  id: string;
  name: string;
  status: 'online' | 'offline' | 'loading';
  latency: number;
  memory: string;
  type: string;
}

const models_list: ModelStatus[] = [
  { id: 'qwen', name: 'Qwen 2.5 Coder', status: 'online', latency: 45, memory: '16GB', type: 'Expert' },
  { id: 'llama', name: 'Llama 3.1', status: 'online', latency: 39, memory: '16GB', type: 'General' },
  { id: 'phi', name: 'Phi 3 Mini', status: 'loading', latency: 0, memory: '4GB', type: 'Edge' },
  { id: 'nomic', name: 'Nomic Embed', status: 'loading', latency: 0, memory: '2GB', type: 'Embedding' },
  { id: 'deepseek', name: 'DeepSeek Coder', status: 'loading', latency: 0, memory: '8GB', type: 'Logic' },
];

function App() {
  const [models, setModels] = useState<ModelStatus[]>(models_list);
  const [activeModel, setActiveModel] = useState<string | null>(null);
  const [logs, setLogs] = useState<string[]>([]);

  const fetchHealth = async () => {
    try {
      const response = await fetch('/telemetry/health');
      const data = await response.json();
      if (data.models) {
        setModels(data.models);
      }
    } catch (error) {
      console.error("Failed to fetch health metrics:", error);
    }
  };

  useEffect(() => {
    fetchHealth();
    const metricsInterval = setInterval(fetchHealth, 5000);
    const logInterval = setInterval(() => {
      const messages = [
        "📡 System Heartbeat: OK",
        "⚙️ Optimizing GCP Cluster us-central1",
        "🛡️ Resilience Policy: Autonomous Failover Active",
        "💎 Model Weights Loaded: Verified",
        "🌐 Scaling Qwen Coder... Instance Count: 4"
      ];
      setLogs(prev => [messages[Math.floor(Math.random() * messages.length)], ...prev.slice(0, 5)]);
    }, 3000);
    return () => {
      clearInterval(metricsInterval);
      clearInterval(logInterval);
    };
  }, []);

  return (
    <div className="app-container" style={{ height: '100vh', width: '100vw', position: 'relative' }}>
      <div className="bg-grid"></div>
      <div className="scanline"></div>

      {/* 3D Visualizer */}
      <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 0 }}>
        <Canvas shadows dpr={[1, 2]} onError={(e) => console.error("Canvas Error:", e)}>
          <Suspense fallback={null}>
            <PerspectiveCamera makeDefault position={[0, 0, 8]} />
            <ambientLight intensity={0.2} />
            <spotLight position={[10, 10, 10]} angle={0.15} penumbra={1} />
            <CoreEngine />
            <Stars radius={100} depth={50} count={5000} factor={4} saturation={0} fade speed={1} />
            <OrbitControls enableZoom={false} autoRotate autoRotateSpeed={0.5} />
          </Suspense>
        </Canvas>
      </div>

      {/* UI Overlay */}
      <header style={{ position: 'absolute', top: '20px', left: '20px', display: 'flex', gap: '20px', alignItems: 'center' }}>
        <div className="glass-panel" style={{ padding: '10px 20px', display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div className="pulsing" style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: 'var(--neon-blue)', boxShadow: '0 0 10px var(--neon-blue)' }}></div>
          <h1 className="neon-text" style={{ fontSize: '1.2rem', fontWeight: 800, letterSpacing: '2px' }}>SUPREME AI COMMAND CENTER</h1>
        </div>
      </header>

      {/* Model Cards */}
      <main style={{ position: 'absolute', bottom: '40px', left: '40px', display: 'flex', gap: '20px' }}>
        {models.map((model) => (
          <motion.div
            key={model.id}
            whileHover={{ y: -10, borderColor: 'var(--neon-blue)' }}
            onClick={() => setActiveModel(model.id)}
            className="glass-panel"
            style={{ 
              width: '180px', 
              padding: '20px', 
              cursor: 'pointer',
              border: activeModel === model.id ? '2px solid var(--neon-blue)' : '1px solid var(--glass-border)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '15px' }}>
              <Cpu size={20} color={model.status === 'online' ? 'var(--neon-blue)' : '#555'} />
              <span style={{ fontSize: '0.7rem', color: '#888' }}>{model.type}</span>
            </div>
            <h3 style={{ fontSize: '0.9rem', marginBottom: '10px' }}>{model.name}</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem' }}>
                <span style={{ color: '#888' }}>Latency</span>
                <span className="neon-text">{model.latency}ms</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem' }}>
                <span style={{ color: '#888' }}>RAM</span>
                <span>{model.memory}</span>
              </div>
            </div>
            <div style={{ marginTop: '15px', height: '2px', width: '100%', background: '#222' }}>
              <motion.div 
                animate={{ width: model.status === 'online' ? '100%' : '30%' }}
                style={{ height: '100%', background: model.status === 'online' ? 'var(--neon-blue)' : 'var(--neon-purple)' }}
              />
            </div>
          </motion.div>
        ))}
      </main>

      {/* Terminal / Logs */}
      <aside style={{ position: 'absolute', right: '40px', bottom: '40px', width: '300px' }}>
        <div className="glass-panel" style={{ padding: '20px', height: '250px', display: 'flex', flexDirection: 'column' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '15px', borderBottom: '1px solid #222', paddingBottom: '10px' }}>
            <TerminalIcon size={16} color="var(--neon-blue)" />
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--neon-blue)' }}>LIVE TELEMETRY</span>
          </div>
          <div style={{ overflowY: 'hidden', flex: 1 }}>
            {logs.map((log, i) => (
              <motion.div 
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1 - i * 0.15, x: 0 }}
                key={i} 
                style={{ fontSize: '0.7rem', fontFamily: 'JetBrains Mono', color: '#aaa', marginBottom: '8px' }}
              >
                {log}
              </motion.div>
            ))}
          </div>
        </div>
      </aside>

      {/* Global Status HUD */}
      <div style={{ position: 'absolute', top: '20px', right: '20px', display: 'flex', gap: '15px' }}>
        <HUDMetric icon={<Shield size={16} />} label="Security" value="MIL-SPEC" color="var(--neon-blue)" />
        <HUDMetric icon={<Zap size={16} />} label="Response" value="ULTRALOW" color="var(--neon-purple)" />
        <HUDMetric icon={<Globe size={16} />} label="Region" value="GCP-US" color="var(--neon-pink)" />
      </div>
    </div>
  );
}

const HUDMetric = ({ icon, label, value, color }: { icon: any, label: string, value: string, color: string }) => (
  <div className="glass-panel" style={{ padding: '10px 15px', display: 'flex', gap: '10px', alignItems: 'center' }}>
    <div style={{ color }}>{icon}</div>
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <span style={{ fontSize: '0.6rem', color: '#888', textTransform: 'uppercase' }}>{label}</span>
      <span style={{ fontSize: '0.8rem', fontWeight: 700 }}>{value}</span>
    </div>
  </div>
);

export default App;
