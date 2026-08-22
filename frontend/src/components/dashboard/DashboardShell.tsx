import React, { useState, useEffect, useRef } from "react";
import { History, Plus, MessageSquare, Trash2 } from "lucide-react";
import { DashboardLayout } from "./DashboardLayout";
import { StatCard } from "../ui/StatCard";
import { PageHeader } from "../ui/PageHeader";
import { EmptyState } from "../ui/EmptyState";
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

interface ChatSession {
  id: string;
  title: string;
  timestamp: string;
  messages: ChatMessage[];
}

export const DashboardShell: React.FC<DashboardShellProps> = ({
  isServerOnline,
  workspace
}) => {
  const [sessions, setSessions] = useState<ChatSession[]>([
    {
      id: 'session-1',
      title: 'Code Optimization Routine',
      timestamp: 'Today, 05:45 AM',
      messages: [
        { id: '1', sender: 'user', text: 'How can I optimize this function?' },
        {
          id: '2',
          sender: 'ai',
          text: 'I can help optimize that. You can leverage useMemo or memoization patterns to reduce unnecessary re-renders.'
        },
      ],
    },
    {
      id: 'session-2',
      title: 'Swarm Telemetry Audit',
      timestamp: 'Yesterday',
      messages: [
        { id: 's2-1', sender: 'user', text: 'Audit active memory vectors' },
        { id: 's2-2', sender: 'ai', text: 'All 52 Crown Jewel knowledge cards are 100% recalled with 38ms latency.' },
      ],
    },
  ]);

  const [activeSessionId, setActiveSessionId] = useState<string>('session-1');
  const [showHistory, setShowHistory] = useState<boolean>(false);
  const [chatInput, setChatInput] = useState('');

  const activeSessionIdRef = useRef(activeSessionId);
  const aiTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    activeSessionIdRef.current = activeSessionId;
  }, [activeSessionId]);

  useEffect(() => {
    return () => {
      if (aiTimeoutRef.current) clearTimeout(aiTimeoutRef.current);
    };
  }, []);

  const activeSession = sessions.find((s) => s.id === activeSessionId) || sessions[0];
  const chatMessages = activeSession ? activeSession.messages : [];

  const handleCreateNewChat = () => {
    const newSession: ChatSession = {
      id: `session-${Date.now()}`,
      title: 'New Conversation',
      timestamp: 'Just now',
      messages: [],
    };
    setSessions((prev) => [newSession, ...prev]);
    setActiveSessionId(newSession.id);
    setShowHistory(false);
  };

  const handleDeleteSession = (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (sessions.length === 1) return;
    const remaining = sessions.filter((s) => s.id !== sessionId);
    setSessions(remaining);
    if (activeSessionId === sessionId) {
      setActiveSessionId(remaining[0].id);
    }
  };

  const handleSendMessage = (textToSend?: string) => {
    const text = (textToSend || chatInput).trim();
    if (!text) return;

    const userMsg: ChatMessage = { id: crypto.randomUUID(), sender: 'user', text };
    
    // Update active session messages
    setSessions((prev) =>
      prev.map((s) => {
        if (s.id === activeSessionId) {
          const isFirstMessage = s.messages.length === 0;
          return {
            ...s,
            title: isFirstMessage ? (text.length > 25 ? text.slice(0, 25) + '...' : text) : s.title,
            messages: [...s.messages, userMsg],
          };
        }
        return s;
      })
    );

    if (!textToSend) setChatInput('');

    // Simulate AI response
    if (aiTimeoutRef.current) clearTimeout(aiTimeoutRef.current);
    aiTimeoutRef.current = setTimeout(() => {
      const aiMsg: ChatMessage = {
        id: crypto.randomUUID(),
        sender: 'ai',
        text: 'I can help optimize that. Could you share the full function and the performance concern you are facing?'
      };
      setSessions((prev) =>
        prev.map((s) =>
          s.id === activeSessionIdRef.current ? { ...s, messages: [...s.messages, aiMsg] } : s
        )
      );
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

        {/* Bottom Section: Workspace Viewport + Chat with History Drawer */}
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

          {/* Right Column - Server Status & Assistant with Chat History */}
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

            {/* AI Assistant Chat Panel with History Drawer */}
            <div className="rounded-xl border border-white/10 bg-slate-900/60 backdrop-blur-md flex flex-col h-[400px] shadow-xl overflow-hidden relative">
              {/* Header */}
              <div className="p-3.5 border-b border-white/5 bg-slate-900/80 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm text-slate-200 flex items-center gap-2">
                    <span className="text-cyan-400">⚡</span> AI Assistant
                  </h3>
                  <span className="text-[10px] font-mono text-purple-400">Swarm</span>
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => setShowHistory((prev) => !prev)}
                    title="Toggle Chat History"
                    aria-label="Chat History"
                    className={`p-1.5 rounded-lg text-xs font-medium flex items-center gap-1 transition-all ${
                      showHistory
                        ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    <History className="w-3.5 h-3.5" />
                    <span className="hidden sm:inline">History</span>
                  </button>

                  <button
                    onClick={handleCreateNewChat}
                    title="Start New Chat Session"
                    aria-label="New Chat"
                    className="p-1.5 rounded-lg text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-800 border border-white/5 flex items-center gap-1 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5 text-cyan-400" />
                    <span className="hidden sm:inline">New</span>
                  </button>
                </div>
              </div>

              {/* Chat History Overlay Drawer */}
              {showHistory && (
                <div className="absolute inset-x-0 top-[49px] bottom-0 z-20 bg-slate-950/95 backdrop-blur-xl p-3 flex flex-col justify-between border-b border-white/5 animate-in fade-in zoom-in-95 duration-150">
                  <div className="space-y-1 overflow-y-auto max-h-72">
                    <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 px-2 py-1">
                      Past Conversations ({sessions.length})
                    </p>
                    {sessions.map((session) => (
                      <div
                        key={session.id}
                        onClick={() => {
                          setActiveSessionId(session.id);
                          setShowHistory(false);
                        }}
                        className={`group flex items-center justify-between p-2.5 rounded-xl text-xs cursor-pointer transition-all ${
                          session.id === activeSessionId
                            ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 shadow-[0_0_10px_rgba(0,243,255,0.15)]'
                            : 'text-slate-300 hover:bg-slate-900 border border-transparent'
                        }`}
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <MessageSquare className="w-3.5 h-3.5 flex-shrink-0 text-cyan-400" />
                          <div className="min-w-0">
                            <p className="font-semibold truncate">{session.title}</p>
                            <span className="text-[10px] text-slate-500">{session.timestamp}</span>
                          </div>
                        </div>

                        {sessions.length > 1 && (
                          <button
                            onClick={(e) => handleDeleteSession(session.id, e)}
                            title="Delete Session"
                            className="opacity-0 group-hover:opacity-100 p-1 hover:text-rose-400 text-slate-500 transition-opacity"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>

                  <button
                    onClick={handleCreateNewChat}
                    className="w-full py-2 bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white text-xs font-semibold rounded-xl transition-all shadow-[0_0_12px_rgba(0,243,255,0.25)] flex items-center justify-center gap-1.5 mt-2"
                  >
                    <Plus className="w-3.5 h-3.5" /> Start New Chat
                  </button>
                </div>
              )}

              {/* Chat Messages Area */}
              <div className="flex-1 p-4 overflow-y-auto flex flex-col gap-3">
                {chatMessages.length === 0 ? (
                  <EmptyState
                    title="Start a Conversation"
                    description="Ask your AI co-engineer to generate code, run diagnostics, or optimize architecture."
                    actionLabel="⚡ Optimize Code"
                    onAction={() => handleSendMessage('How can I optimize this function?')}
                    secondaryActionLabel="🔍 Audit Swarm"
                    onSecondaryAction={() => handleSendMessage('Audit active memory vectors and health.')}
                    className="my-auto border-dashed border-white/5 bg-transparent p-4"
                  />
                ) : (
                  chatMessages.map((msg) => (
                    <div
                      key={msg.id}
                      className={`text-xs max-w-[85%] p-3 rounded-xl ${msg.sender === 'user'
                          ? 'bg-purple-600/20 border border-purple-500/30 text-purple-200 self-end shadow-[0_0_12px_rgba(168,85,247,0.15)]'
                          : 'bg-slate-800/80 border border-white/5 text-slate-200 self-start shadow-md'
                        }`}
                    >
                      {msg.text}
                    </div>
                  ))
                )}
              </div>

              {/* Chat Input */}
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
                    onClick={() => handleSendMessage()}
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
