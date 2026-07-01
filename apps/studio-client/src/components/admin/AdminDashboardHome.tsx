import React, { useState, useEffect } from 'react';
import { Play, Activity, Server, AlertTriangle, Monitor, Sparkles, Cpu, Layers } from 'lucide-react';
import { motion } from 'framer-motion';
import { RealTimeMetricsPanel } from './RealTimeMetricsPanel';

// বাংলা মন্তব্য: এডমিন ড্যাশবোর্ডের মূল ৬টি প্যানেল গ্রিড লেআউট (Admin Dashboard Home)
// এটি রেফারেন্স ইমেজ অনুযায়ী রিচ ভিজ্যুয়াল ও ডাটা ইন্ডিকেটর দিয়ে সাজানো হয়েছে।
export const AdminDashboardHome: React.FC = () => {
  const [modelId, setModelId] = useState('NEURAL_CORE_v5');
  const [currentTime, setCurrentTime] = useState(new Date().toLocaleTimeString());

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date().toLocaleTimeString());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="flex-1 overflow-y-auto bg-[#030611] p-6 font-mono text-slate-300">
      
      {/* 1. TOP SECTION: AI Fleet Status */}
      <section className="mb-6 bg-slate-950/60 border border-[#00f3ff]/15 rounded-xl p-5 shadow-[0_0_15px_rgba(0,243,255,0.02)]">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xs uppercase tracking-widest text-[#00f3ff] font-bold">AI Fleet Status</h2>
          <button className="text-[10px] text-slate-500 hover:text-white border border-slate-800 rounded px-2 py-0.5 transition-all">
            VIEW DETAILS
          </button>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Active Agents Gauge */}
          <div className="bg-[#040814]/80 border border-slate-900 rounded-lg p-4 flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-500 uppercase block mb-1">Active Agents</span>
              <span className="text-2xl font-bold text-[#00f3ff]">1,489</span>
            </div>
            {/* SVG mini gauge */}
            <svg className="w-16 h-12" viewBox="0 0 100 60">
              <path d="M 10 50 A 40 40 0 0 1 90 50" fill="none" stroke="#0f172a" strokeWidth="8" strokeLinecap="round"/>
              <path d="M 10 50 A 40 40 0 0 1 75 20" fill="none" stroke="url(#cyanGrad)" strokeWidth="8" strokeLinecap="round" strokeDasharray="180"/>
              <defs>
                <linearGradient id="cyanGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#09101f" />
                  <stop offset="100%" stopColor="#00f3ff" />
                </linearGradient>
              </defs>
            </svg>
          </div>

          {/* Active Tasks Bar Chart */}
          <div className="bg-[#040814]/80 border border-slate-900 rounded-lg p-4 flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-500 uppercase block mb-1">Active Tasks</span>
              <span className="text-2xl font-bold text-[#b5179e]">8,762</span>
            </div>
            {/* Mini bar animated chart */}
            <div className="flex items-end gap-1.5 h-10">
              {[20, 45, 15, 60, 30, 80, 50, 95, 40].map((h, i) => (
                <div 
                  key={i} 
                  style={{ height: `${h}%` }} 
                  className="w-1.5 bg-gradient-to-t from-purple-800 to-[#b5179e] rounded-sm"
                />
              ))}
            </div>
          </div>

          {/* Latency Metric */}
          <div className="bg-[#040814]/80 border border-slate-900 rounded-lg p-4 flex items-center justify-between">
            <div>
              <span className="text-[10px] text-slate-500 uppercase block mb-1">Network Latency</span>
              <span className="text-2xl font-bold text-emerald-400">42ms</span>
            </div>
            {/* Mini sparkline */}
            <svg className="w-20 h-10" viewBox="0 0 100 40">
              <path d="M 0 30 Q 15 10 30 25 T 60 15 T 90 35 L 100 20" fill="none" stroke="#10b981" strokeWidth="2"/>
            </svg>
          </div>
        </div>
      </section>

      {/* 1.5 Real-Time Metrics Panel (Mini-Grafana) */}
      <div className="mb-6">
        <RealTimeMetricsPanel />
      </div>

      {/* 2. MIDDLE ROW: 3 Columns Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        
        {/* Card A: Model Performance Analytics */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[380px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-4">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Model Perf. Analytics</span>
            <select 
              value={modelId} 
              onChange={e => setModelId(e.target.value)}
              className="bg-[#040814] border border-slate-800 rounded px-2 py-0.5 text-[10px] text-slate-300 outline-none"
            >
              <option value="NEURAL_CORE_v5">NEURAL_CORE_v5</option>
              <option value="LLAMA_8B_INST">LLAMA_8B_INST</option>
              <option value="QWEN_CODER_32B">QWEN_CODER_32B</option>
            </select>
          </div>

          <div className="flex gap-4 mb-2">
            <div>
              <div className="text-[9px] text-slate-500">F1-Score</div>
              <div className="text-emerald-400 text-lg font-bold">0.965</div>
            </div>
            <div>
              <div className="text-[9px] text-slate-500">Loss</div>
              <div className="text-rose-500 text-lg font-bold">0.112</div>
            </div>
          </div>

          {/* 3D Wireframe Peak plot using SVGs */}
          <div className="flex-1 flex items-center justify-center py-2 relative">
            <svg className="w-full h-40" viewBox="0 0 200 120">
              {/* Back grid lines */}
              <line x1="20" y1="90" x2="180" y2="90" stroke="#1e293b" strokeWidth="1" strokeDasharray="2,2"/>
              <line x1="40" y1="20" x2="40" y2="100" stroke="#1e293b" strokeWidth="1" strokeDasharray="2,2"/>
              
              {/* Fake 3D Wireframe surface peaks */}
              <path 
                d="M 30 90 L 50 70 L 80 85 L 100 40 L 120 75 L 150 50 L 180 90" 
                fill="none" 
                stroke="#00f3ff" 
                strokeWidth="1.5"
                opacity="0.8"
              />
              <path 
                d="M 25 95 L 45 75 L 75 90 L 95 45 L 115 80 L 145 55 L 175 95" 
                fill="none" 
                stroke="#b5179e" 
                strokeWidth="1.5"
                opacity="0.6"
              />
              <path 
                d="M 20 100 L 40 80 L 70 95 L 90 50 L 110 85 L 140 60 L 170 100" 
                fill="none" 
                stroke="#10b981" 
                strokeWidth="1"
                opacity="0.4"
              />
            </svg>
          </div>

          <div className="flex justify-between items-center text-[10px] mt-2">
            <span className="text-emerald-400 bg-emerald-950/20 px-2 py-0.5 rounded border border-emerald-800/30">RUNNING</span>
            <span className="text-cyan-400 bg-cyan-950/20 px-2 py-0.5 rounded border border-cyan-800/30">OPTIMIZED</span>
          </div>
        </div>

        {/* Card B: Workflow Pipeline Visualization */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[380px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-4">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Workflow Pipeline</span>
            <div className="flex gap-2 text-[9px]">
              <span className="text-emerald-400">ACTIVE: 7</span>
              <span className="text-purple-400">QUEUED: 3</span>
            </div>
          </div>

          {/* Simple Vector flow pipeline nodes */}
          <div className="flex-1 flex flex-col gap-4 justify-center py-2">
            <div className="flex justify-between items-center">
              <div className="border border-emerald-500/30 bg-[#040814] rounded p-2 text-center w-20 text-[10px] shadow-[0_0_8px_rgba(16,185,129,0.1)]">
                <div className="text-slate-500 text-[8px]">Node</div>
                <div className="text-emerald-400 font-bold">Alpha</div>
              </div>
              <svg className="w-12 h-6" viewBox="0 0 50 20">
                <line x1="0" y1="10" x2="50" y2="10" stroke="#10b981" strokeWidth="2" strokeDasharray="3,3"/>
              </svg>
              <div className="border border-cyan-500/30 bg-[#040814] rounded p-2 text-center w-20 text-[10px]">
                <div className="text-slate-500 text-[8px]">Node</div>
                <div className="text-cyan-400 font-bold">Beta</div>
              </div>
            </div>

            <div className="flex justify-between items-center">
              <div className="border border-purple-500/30 bg-[#040814] rounded p-2 text-center w-20 text-[10px]">
                <div className="text-slate-500 text-[8px]">Node</div>
                <div className="text-purple-400 font-bold">Gamma</div>
              </div>
              <svg className="w-12 h-6" viewBox="0 0 50 20">
                <path d="M 0 10 Q 25 20 50 10" fill="none" stroke="#b5179e" strokeWidth="2"/>
              </svg>
              <div className="border border-pink-500/30 bg-[#040814] rounded p-2 text-center w-20 text-[10px] shadow-[0_0_8px_rgba(244,63,94,0.1)]">
                <div className="text-slate-500 text-[8px]">Node</div>
                <div className="text-pink-500 font-bold">Flow</div>
              </div>
            </div>
          </div>

          <div className="flex gap-2">
            <button className="flex-1 bg-[#00f3ff]/10 hover:bg-[#00f3ff]/20 text-[#00f3ff] text-[10px] font-bold py-2 rounded transition-all border border-[#00f3ff]/20">
              OPTIMIZE
            </button>
            <button className="flex-1 bg-purple-950/20 hover:bg-purple-900/30 text-purple-400 text-[10px] font-bold py-2 rounded transition-all border border-purple-800/20">
              MONITOR
            </button>
          </div>
        </div>

        {/* Card C: Compute Resource Allocation */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[380px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-4">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Compute Resource</span>
            <span className="text-[9px] text-slate-500">DYNAMIC UTILIZATION</span>
          </div>

          {/* Hexagonal grid placeholder */}
          <div className="flex-grow flex items-center justify-center py-4">
            <div className="grid grid-cols-3 gap-2">
              {[78, 65, 91, 45, 80, 52].map((val, i) => (
                <div 
                  key={i} 
                  className={`w-14 h-16 bg-[#040814] border flex flex-col items-center justify-center relative shadow-[inset_0_0_10px_rgba(0,0,0,0.6)] ${
                    val > 80 ? 'border-rose-500/40 text-rose-400' : 'border-emerald-500/30 text-emerald-400'
                  }`}
                  style={{ clipPath: 'polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%)' }}
                >
                  <Cpu size={12} className="opacity-60 mb-1" />
                  <span className="text-[11px] font-bold">{val}%</span>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-2 text-[10px]">
            <div className="flex justify-between">
              <span className="text-slate-500">CPU Usage:</span>
              <span className="text-emerald-400 font-bold">78%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">GPU Usage:</span>
              <span className="text-cyan-400 font-bold">65%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Memory Allocation:</span>
              <span className="text-rose-500 font-bold">91%</span>
            </div>
          </div>
        </div>

      </div>

      {/* 3. BOTTOM ROW: 3 Columns Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Card D: Live Event Log */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[300px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-3">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Live Event Log</span>
            <span className="text-[9px] text-slate-500">SYNC ACTIVE</span>
          </div>

          <div className="flex-1 bg-black/40 border border-slate-900 rounded-lg p-3 overflow-y-auto max-h-[180px] text-[10px] font-mono space-y-2 text-[#00ff66]">
            <div><span className="text-slate-500">[{currentTime}]</span> Model NEURAL_CORE deployed successfully.</div>
            <div><span className="text-slate-500">[{currentTime}]</span> Active task queue synchronized.</div>
            <div><span className="text-slate-500">[{currentTime}]</span> Node Alpha load average: 34%.</div>
            <div><span className="text-slate-500">[{currentTime}]</span> Connection established to Cloud Run.</div>
            <div className="text-rose-400"><span className="text-slate-500">[{currentTime}]</span> WARNING: Node Flow latency peak 120ms.</div>
          </div>
        </div>

        {/* Card E: Active Agents Map */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[300px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-3">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Active Agents Map</span>
            <span className="text-[9px] text-emerald-400">GLOBAL ACTIVITY STATUS</span>
          </div>

          {/* Simple wireframe outline of world map */}
          <div className="flex-grow flex items-center justify-center py-2 opacity-85">
            <svg className="w-full h-36" viewBox="0 0 320 160">
              {/* Continents outlines (fake simple shapes) */}
              <path d="M 40 40 Q 60 20 80 50 T 120 70 L 100 120 Q 80 130 50 100 Z" fill="none" stroke="#1e293b" strokeWidth="1.5"/>
              <path d="M 180 50 Q 220 30 260 40 T 290 80 L 260 130 Q 220 140 200 100 Z" fill="none" stroke="#1e293b" strokeWidth="1.5"/>
              
              {/* Activity neon dots */}
              <circle cx="80" cy="50" r="4" fill="#00f3ff" className="animate-ping"/>
              <circle cx="80" cy="50" r="3" fill="#00f3ff"/>
              
              <circle cx="220" cy="60" r="4" fill="#b5179e" className="animate-ping"/>
              <circle cx="220" cy="60" r="3" fill="#b5179e"/>
              
              <circle cx="250" cy="90" r="4" fill="#10b981" className="animate-ping"/>
              <circle cx="250" cy="90" r="3" fill="#10b981"/>
            </svg>
          </div>
        </div>

        {/* Card F: Task Queue */}
        <div className="bg-slate-950/60 border border-slate-900 rounded-xl p-5 flex flex-col justify-between min-h-[300px] shadow-[0_0_15px_rgba(0,0,0,0.3)]">
          <div className="flex justify-between items-center mb-3">
            <span className="text-[10px] text-[#00f3ff] uppercase font-bold tracking-wider">Task Queue</span>
            <span className="text-[9px] text-slate-500">PENDING QUEUE</span>
          </div>

          <div className="flex-grow space-y-2 overflow-y-auto max-h-[180px]">
            {[
              { id: 'TASK-1029', name: 'Neural Core Optimization', type: 'Optimize' },
              { id: 'TASK-3928', name: 'Database Replication Sync', type: 'Sync' },
              { id: 'TASK-4821', name: 'Observability Log Archive', type: 'Archive' }
            ].map(task => (
              <div key={task.id} className="bg-[#040814] border border-slate-900 rounded p-2.5 flex justify-between items-center text-[10px]">
                <div>
                  <div className="font-bold text-slate-200">{task.id}</div>
                  <div className="text-slate-500 text-[8px]">{task.name}</div>
                </div>
                <button className="bg-cyan-950/30 hover:bg-[#00f3ff]/20 text-[#00f3ff] border border-[#00f3ff]/30 rounded px-2.5 py-1 transition-all">
                  MONITOR
                </button>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
};
