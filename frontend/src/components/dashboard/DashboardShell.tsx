import React, { useState } from "react";
import { DashboardLayout } from "./DashboardLayout";
import { StatCard } from "../ui/StatCard";
import { PageHeader } from "../ui/PageHeader";
import { LiveTelemetryChart } from "./LiveTelemetryChart";
import { QuickActionsPanel } from "./QuickActionsPanel";

interface DashboardShellProps {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  isServerOnline: boolean;
  workspace: React.ReactNode;
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
}

export const DashboardShell: React.FC<DashboardShellProps> = ({
  isServerOnline,
  workspace
}) => {
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    { id: '1', sender: 'user', text: 'How can I optimize this function?' },
  ]);
  const [chatInput, setChatInput] = useState('');

  const handleSendMessage = () => {
    const trimmed = chatInput.trim();
    if (!trimmed) return;

    // Add user message
    setChatMessages(prev => [...prev, { id: crypto.randomUUID(), sender: 'user', text: trimmed }]);
    setChatInput('');

    // Simulate AI response
    setTimeout(() => {
      setChatMessages(prev => [...prev, {
        id: crypto.randomUUID(),
        sender: 'ai',
        text: 'I can help optimize that. Could you share the full function and the performance concern you are facing?'
      }]);
    }, 600);
  };

  return (
    <DashboardLayout title="Dashboard">
      {/* Ambient Aurora Glow Background */}
      <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden opacity-30 dark:opacity-40" aria-hidden="true">
        <div className="absolute -top-40 -left-40 w-96 h-96 rounded-full bg-gradient-to-br from-cyan-500/20 to-transparent blur-3xl animate-pulse" style={{ animationDuration: '8s' }} />
        <div className="absolute top-1/3 -right-40 w-96 h-96 rounded-full bg-gradient-to-bl from-purple-500/20 to-transparent blur-3xl animate-pulse" style={{ animationDuration: '10s' }} />
      </div>

      <div className="relative z-10 space-y-6">
        <PageHeader
          eyebrow="SupremeAI Studio"
          title="Dashboard"
          subtitle="Manage your self-learning AI agents from a single high-availability console."
          crumbItems={[{ label: "Home", href: "/" }, { label: "Dashboard" }]}
        />

        {/* Top KPI Row */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Active Projects"
            value="24"
            delta="+4 this week"
            deltaTone="positive"
            sparklineData={[12, 16, 14, 20, 18, 22, 24]}
            spotlightColor="cyan"
            hint="Active project count"
          />
          <StatCard
            label="Tasks Completed"
            value="142"
            delta="+18% velocity"
            deltaTone="positive"
            sparklineData={[80, 95, 110, 105, 125, 138, 142]}
            spotlightColor="purple"
            hint="Completed task count"
          />
          <StatCard
            label="Inference Latency"
            value="38ms"
            delta="-12ms opt"
            deltaTone="positive"
            sparklineData={[58, 52, 46, 44, 40, 39, 38]}
            spotlightColor="cyan"
            hint="Average roundtrip latency"
          />
          <StatCard
            label="Knowledge Cards"
            value="52"
            delta="100% recalled"
            deltaTone="positive"
            sparklineData={[24, 24, 30, 36, 42, 48, 52]}
            spotlightColor="purple"
            hint="Crown Jewel Knowledge Base"
          />
        </div>

        {/* Middle Section: Live Telemetry Chart + Quick Actions Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <LiveTelemetryChart />
          </div>
          <div>
            <QuickActionsPanel />
          </div>
        </div>

        {/* Bottom Section: Workspace Viewport + Chat */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Workspace */}
          <div className="lg:col-span-2">
            {workspace}

            {/* Code Editor Panel */}
            <div className="mt-6 bg-slate-950/80 text-white rounded-xl shadow-xl border border-white/10 overflow-hidden backdrop-blur-md">
              <div className="bg-slate-900/90 px-4 py-2.5 border-b border-white/5 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse"></span>
                  <span className="text-xs font-mono font-medium text-slate-300">index.tsx</span>
                </div>
                <span className="text-[10px] font-mono text-cyan-400/80">React 19 + TypeScript</span>
              </div>
              <div className="p-4 font-mono text-sm">
                <pre className="text-slate-300 leading-relaxed">
                  <code>
                    {`import React from 'react';\n\nexport const App = () => {\n  return <div>Hello World!</div>;\n};`}
                  </code>
                </pre>
              </div>
            </div>
          </div>

          {/* Right Column - Server Status & Assistant */}
          <div className="space-y-6">
            {/* Server Status Indicator */}
            <div className="rounded-xl border border-white/10 bg-slate-900/60 backdrop-blur-md p-4 shadow-lg">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className={`w-3 h-3 rounded-full mr-2.5 ${isServerOnline ? 'bg-emerald-400 shadow-[0_0_10px_rgba(52,211,153,0.6)] animate-pulse' : 'bg-rose-500'}`}></div>
                  <span className="text-sm font-semibold text-slate-200">
                    Server Status: {isServerOnline ? 'Online' : 'Offline'}
                  </span>
                </div>
                <span className="text-[10px] font-mono text-emerald-400">99.99% Uptime</span>
              </div>
            </div>

            {/* AI Assistant Chat Panel */}
            <div className="rounded-xl border border-white/10 bg-slate-900/60 backdrop-blur-md flex flex-col h-[360px] shadow-xl overflow-hidden">
              <div className="p-4 border-b border-white/5 bg-slate-900/80 flex items-center justify-between">
                <h3 className="font-bold text-sm text-slate-200 flex items-center gap-2">
                  <span className="text-cyan-400">⚡</span> AI Assistant
                </h3>
                <span className="text-[10px] font-mono text-purple-400">Supreme Swarm</span>
              </div>
              <div className="flex-1 p-4 overflow-y-auto flex flex-col gap-3">
                {chatMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`text-xs max-w-[85%] p-3 rounded-xl ${msg.sender === 'user'
                        ? 'bg-purple-600/20 border border-purple-500/30 text-purple-200 self-end shadow-[0_0_12px_rgba(168,85,247,0.15)]'
                        : 'bg-slate-800/80 border border-white/5 text-slate-200 self-start shadow-md'
                      }`}
                  >
                    {msg.text}
                  </div>
                ))}
              </div>
              <div className="p-3 border-t border-white/5 bg-slate-900/80">
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                    placeholder="Ask AI anything..."
                    className="flex-1 px-3 py-2 border border-slate-700/80 rounded-lg text-xs bg-slate-950/80 text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500"
                  />
                  <button
                    onClick={handleSendMessage}
                    className="px-3.5 py-2 bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white text-xs font-semibold rounded-lg transition-all shadow-[0_0_12px_rgba(0,243,255,0.25)]"
                  >
                    Send
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default DashboardShell;
