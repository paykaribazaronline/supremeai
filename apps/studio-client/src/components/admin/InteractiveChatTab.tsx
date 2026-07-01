import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Terminal, Globe, Send, RefreshCw, Eye, EyeOff, Layout, TerminalSquare, Compass } from 'lucide-react';
import { useDashboardStore } from '../../store/dashboardStore';
import { UnifiedChatBubble, TypingIndicator } from '../chat';

// বাংলা মন্তব্য: চ্যাট মেসেজ ইন্টারফেস — Prompt-to-Action আর্কিটেকচার সাপোর্ট সহ
interface Message {
  id: string;
  sender: 'user' | 'system';
  text: string;
  timestamp: string;
  action?: {
    type: string;
    target?: string;
    label?: string;
    icon?: string;
    confidence?: number;
    requires_confirmation?: boolean;
    payload?: Record<string, unknown>;
  };
}

// বাংলা মন্তব্য: প্রপ্স ডিক্লেয়ারেশন যা কাস্টমার ড্যাশবোর্ড থেকে পাস করা হবে
interface InteractiveChatTabProps {
  messages?: any[];
  input?: string;
  onInputChange?: (val: string) => void;
  onSend?: () => void;
  loading?: boolean;
}

const API_BASE = import.meta.env.VITE_API_BASE || '';

export function InteractiveChatTab({
  messages: propMessages,
  input: propInput,
  onInputChange,
  onSend,
  loading = false,
  onSaveToProject,
  onPreview,
}: InteractiveChatTabProps) {
  const {
    dashboardMode,
    chatTabTerminalOpen,
    chatTabBrowserOpen,
    toggleTerminal,
    toggleBrowser,
  } = useDashboardStore();

  // --- চ্যাট স্ট্যাটস ---
  const [internalMessages, setInternalMessages] = useState<Message[]>([
    {
      id: 'init',
      sender: 'system',
      text: 'স্বাগতম! আমি SupremeAI 2.0 অ্যাসিস্ট্যান্ট। আমি চ্যাট, কোড, রিসার্চ এবং ডেপ্লয়মেন্ট — সবকিছু এই Unified Command Portal থেকে সাপোর্ট করি।órm',
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [internalInput, setInternalInput] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  // --- API কলে স্ক্রোল লোডিং ---
  const [isStreaming, setIsStreaming] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  // বাংলা মন্তব্য: প্রপ্স থাকলে তা ব্যবহার করা হবে, অন্যথায় ইন্টারনাল স্টেট
  const activeMessages = propMessages
    ? propMessages.map((m, idx) => ({
        id: m.id ? String(m.id) : String(idx),
        sender: String(m.sender).toLowerCase() === 'user' ? 'user' : ('system' as const),
        text: m.text,
        timestamp: m.timestamp || new Date().toLocaleTimeString(),
        action: m.action || undefined,
      }))
    : internalMessages;

  const activeInput = propInput !== undefined ? propInput : internalInput;
  const setActiveInput = onInputChange || setInternalInput;

  // --- অটো-স্ক্রোল ---
  useEffect(() => {
    if (chatEndRef.current && typeof chatEndRef.current.scrollIntoView === 'function') {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeMessages, isStreaming]);

  // --- বোনাস API কল (Prompt Action metadata) ---
  const fetchActionMetadata = useCallback(async (prompt: string): Promise<Message['action']> => {
    try {
      const res = await fetch(`${API_BASE}/api/chat/prompt-action`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: prompt }),
      });
      if (!res.ok) return undefined;
      const data = await res.json();
      return data.action;
    } catch {
      return undefined;
    }
  }, [API_BASE]);

  // --- লাইভ স্ট্রিমিং API কল ---
  const streamChatResponse = useCallback(async (userPrompt: string) => {
    setIsStreaming(true);
    const botMsgId = crypto.randomUUID();
    const now = new Date().toLocaleTimeString();

    let fullText = '';
    setInternalMessages(prev => [
      ...prev,
      { id: botMsgId, sender: 'system', text: '', timestamp: now },
    ]);

    try {
      const res = await fetch(`${API_BASE}/api/chat/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userPrompt }),
        signal: (abortControllerRef.current = new AbortController()).signal,
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const reader = res.body?.getReader();
      if (!reader) throw new Error('No stream body');

      const decoder = new TextDecoder();
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const payload = line.slice(6).trim();
            if (payload === '[DONE]') continue;
            try {
              const parsed = JSON.parse(payload);
              if (parsed.token) {
                fullText += parsed.token;
                setInternalMessages(prev =>
                  prev.map(m => m.id === botMsgId ? { ...m, text: fullText } : m)
                );
              }
            } catch {
              fullText += payload;
              setInternalMessages(prev =>
                prev.map(m => m.id === botMsgId ? { ...m, text: fullText } : m)
              );
            }
          }
        }
      }
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        fullText = `⚠️ Error: ${err.message}. Please try again.`;
        setInternalMessages(prev =>
          prev.map(m => m.id === botMsgId ? { ...m, text: fullText } : m)
        );
      }
    } finally {
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  }, [API_BASE]);

  // --- চ্যাট সাবমিট ---
  const handleSendChat = async () => {
    const inputVal = activeInput;
    if (!inputVal.trim() || isStreaming) return;

    if (onSend) {
      onSend();
      return;
    }

    const now = new Date().toLocaleTimeString();
    const userMsg: Message = {
      id: crypto.randomUUID(),
      sender: 'user',
      text: inputVal,
      timestamp: now,
    };

    setInternalMessages(prev => [...prev, userMsg]);
    setActiveInput('');

    // Prompt-to-Action: metadata প্রত্যেকachus Georgian মেসেজের সাথে
    const actionMeta = await fetchActionMetadata(inputVal);

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    await streamChatResponse(inputVal);

    // শেষ বাবলের action স sandwichedetect এবং আপডেট
    setInternalMessages(prev => {
      const last = prev[prev.length - 1];
      if (last && last.sender === 'system' && actionMeta) {
        return prev.map((m, i) =>
          i === prev.length - 1 ? { ...m, action: actionMeta } : m
        );
      }
      return prev;
    });
  };

  // --- টার্মিনাল স্ট্যাটস ---
  const [terminalHistory, setTerminalHistory] = useState<string[]>([
    'SupremeAI Terminal v2.0.0 (Secure Read-Only Mock Shell)',
    'Type "help" for a list of available commands.',
    '',
  ]);
  const [terminalInput, setTerminalInput] = useState('');
  const terminalEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (terminalEndRef.current && typeof terminalEndRef.current.scrollIntoView === 'function') {
      terminalEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [terminalHistory]);

  // --- ব্রাউজার স্ট্যাটস ---
  const [browserUrl, setBrowserUrl] = useState('https://supremeai.dev/docs');
  const [browserHistory, setBrowserHistory] = useState<string[]>(['https://supremeai.dev/docs']);
  const [currentBrowserIndex, setCurrentBrowserIndex] = useState(0);

  const isSimple = dashboardMode === 'simple';

  // --- টার্মিনাল কমান্ড প্রসেসর ---
  const handleTerminalSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!terminalInput.trim()) return;
    const cmd = terminalInput.trim();

    if (cmd.toLowerCase() === 'clear') {
      setTerminalHistory([]);
      setTerminalInput('');
      return;
    }

    const newHistory = [...terminalHistory, `supremeai-user$ ${cmd}`];
    const args = cmd.toLowerCase().split(' ');
    const primaryCmd = args[0];
    let output: string;

    switch (primaryCmd) {
      case 'help':
        output = 'Available Commands:\n  help          - Show this screen\n  status        - Show global system status\n  system-check  - Run automated diagnostics\n  clear         - Clear terminal history\n  neofetch      - Show system branding\n  list-skills   - Show registered AI agent skills';
        break;
      case 'status':
        output = 'SYSTEM POWER STATUS: ONLINE\nActive Nodes: 4/4\nAPI gateway: 0.05ms latency\nEncryption Key Status: VERIFIED\nLOCAL_CHAOS_MODE: false';
        break;
      case 'system-check':
        output = 'Running diagnostics...\n[OK] Database (Redis Pub/Sub)\n[OK] Celery Workers\n[OK] Firebase Cloud Authentication\n[OK] OpenAI/Gemini/Anthropic Gateways\nAll subsystems normal.';
        break;
      case 'neofetch':
        output = `
    ______                            __  ___    ____
   / ____/_  ______  ________  ____  /  |/  /   /  _/
  / / __/ / / / __ \\/ ___/ _ \\/ __ \\/ /|_/ /    / /
 / /_/ / /_/ / /_/ / /  /  __/ / / / /  / /   _/ /
 \\____/\\__,_/ .___/_/   \\___/_/ /_/_/  /_/   /___
           /_/
 OS: SupremeAI Enterprise OS v2.0
 Kernel: Cloud-Orchestrated Linux (v5.15)
 Uptime: 4 days, 12 hours
 Shell: supreme-sh
 CPU: Virtualized AMD EPYC (8 Cores)
 RAM: 32 GB (Active: 12.4 GB)`;
        break;
      case 'list-skills':
        output = 'ACTIVE AGENT SKILLS:\n- CodeArch Swarm Connector [v1.0]\n- DataAnalyzer Mesh [v1.2]\n- WebResearch Gateway [v2.0]\n- Custom AST Evolution Node [v1.0]';
        break;
      default:
        output = `command not found: ${cmd}. Type "help" for support.`;
    }

    setTerminalHistory([...newHistory, output, '']);
    setTerminalInput('');
  };

  // --- ব্রাউজার নেভিগেশন ---
  const handleBrowserGo = (e: React.FormEvent) => {
    e.preventDefault();
    if (!browserUrl.startsWith('http://') && !browserUrl.startsWith('https://')) {
      const formatted = `https://${browserUrl}`;
      setBrowserUrl(formatted);
      setBrowserHistory([...browserHistory.slice(0, currentBrowserIndex + 1), formatted]);
      setCurrentBrowserIndex(currentBrowserIndex + 1);
    } else {
      setBrowserHistory([...browserHistory.slice(0, currentBrowserIndex + 1), browserUrl]);
      setCurrentBrowserIndex(currentBrowserIndex + 1);
    }
  };

  return (
    <div className={`w-full h-[calc(100vh-140px)] flex flex-col transition-all duration-500 ${isSimple ? 'text-slate-800' : 'text-slate-200'}`}>

      {/* কন্ট্রোল প্যানেল — অ্যাডভান্সড মোডে */}
      {!isSimple && (
        <div className="flex justify-between items-center px-4 py-2 bg-[#090b11] border-b border-slate-800">
          <span className="text-[10px] font-mono text-[#00f3ff] uppercase tracking-widest">// Unified Command Portal</span>
          <div className="flex gap-2">
            <button
              onClick={toggleTerminal}
              className={`flex items-center gap-1.5 px-2.5 py-1 text-[10px] font-mono rounded border transition-all ${
                chatTabTerminalOpen
                  ? 'bg-[#00f3ff]/10 text-[#00f3ff] border-[#00f3ff]/30'
                  : 'bg-slate-900 text-slate-500 border-slate-800'
              }`}
            >
              <Terminal size={12} />
              {chatTabTerminalOpen ? 'Hide Terminal' : 'Show Terminal'}
            </button>
            <button
              onClick={toggleBrowser}
              className={`flex items-center gap-1.5 px-2.5 py-1 text-[10px] font-mono rounded border transition-all ${
                chatTabBrowserOpen
                  ? 'bg-[#00ff66]/10 text-[#00ff66] border-[#00ff66]/30'
                  : 'bg-slate-900 text-slate-500 border-slate-800'
              }`}
            >
              <Globe size={12} />
              {chatTabBrowserOpen ? 'Hide Browser' : 'Show Browser'}
            </button>
          </div>
        </div>
      )}

      {/* মূল বডি গ্রিড লেআউট */}
      <div className="flex-1 grid grid-cols-12 overflow-hidden bg-black/10">

        {/* চ্যাট প্যানেল */}
        <div className={`h-full flex flex-col border-r border-slate-800 transition-all duration-500 ${
          isSimple
            ? 'col-span-12 bg-white text-slate-900'
            : `${!chatTabTerminalOpen && !chatTabBrowserOpen ? 'col-span-12' : (chatTabTerminalOpen && chatTabBrowserOpen ? 'col-span-6' : 'col-span-9')} bg-[#05070c]/50 text-slate-200`
        }`}>
          {/* চ্যাট হেডার */}
          <div className={`h-10 border-b flex items-center justify-between px-4 ${isSimple ? 'bg-slate-100 border-slate-200' : 'bg-[#080a10] border-slate-800'}`}>
            <span data-testid="chat-header" className={`text-xs font-bold uppercase tracking-wider ${isSimple ? 'text-indigo-600' : 'text-slate-200'}`}>
              Unified Command Portal
            </span>
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full bg-emerald-500 ${!isSimple && 'animate-pulse'}`}></span>
              <span className="text-[10px] font-mono text-emerald-500 font-semibold">SECURE CORRIDOR</span>
            </div>
          </div>

          {/* চ্যატ */}
          <div className={`flex-1 p-4 overflow-y-auto flex flex-col gap-4 ${isSimple ? 'bg-indigo-50/20' : 'bg-[#030509]/30'}`}>
            {activeMessages.map((msg) => (
              <UnifiedChatBubble
                key={msg.id}
                text={msg.text}
                sender={msg.sender}
                timestamp={msg.timestamp}
                action={msg.action}
                onSaveToProject={onSaveToProject}
                onPreview={onPreview}
              />
            ))}

            {isStreaming && <TypingIndicator />}
            <div ref={chatEndRef} />
          </div>

          {/* ইনপুট */}
          <div className={`p-4 border-t ${isSimple ? 'bg-white border-slate-200' : 'bg-[#05070c] border-slate-800'}`}>
            <div className="flex gap-2">
              <input
                data-testid="chat-input"
                type="text"
                placeholder="Ask anything, generate code, or run deployments…"
                value={activeInput}
                onChange={(e) => setActiveInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendChat()}
                disabled={isStreaming}
                className={`flex-grow border rounded-xl px-4 py-2.5 text-xs outline-none transition-colors ${
                  isSimple
                    ? 'bg-slate-50 border-slate-300 focus:border-indigo-500 text-slate-900'
                    : 'bg-[#0c0e14] border-slate-700 focus:border-[#00f3ff] text-white disabled:opacity-50'
                }`}
              />
              <button
                data-testid="chat-submit"
                onClick={handleSendChat}
                disabled={isStreaming}
                className={`px-4 rounded-xl flex items-center justify-center gap-1.5 transition-all ${
                  isSimple
                    ? 'bg-indigo-600 hover:bg-indigo-700 text-white font-bold disabled:opacity-50'
                    : 'bg-[#00f3ff] hover:bg-cyan-400 text-black font-extrabold shadow-[0_0_15px_rgba(0,243,255,0.4)] disabled:opacity-50'
                }`}
              >
                <Send size={14} />
                <span>Send</span>
              </button>
            </div>
          </div>
        </div>

        {/* টার্মিনাল */}
        {!isSimple && chatTabTerminalOpen && (
          <div className={`h-full flex flex-col border-r border-slate-800 bg-[#020306] ${
            chatTabBrowserOpen ? 'col-span-3' : 'col-span-6'
          }`}>
            <div className="h-10 border-b border-slate-800 bg-[#07090e] px-4 flex items-center justify-between">
              <span className="text-[10px] font-bold text-[#00ff66] tracking-wider uppercase font-mono flex items-center gap-1.5">
                <TerminalSquare size={12} /> Live Shell
              </span>
              <span className="text-[8px] bg-slate-900 text-slate-500 px-1 py-0.5 rounded font-mono">READONLY</span>
            </div>
            <div className="flex-1 p-3 overflow-y-auto font-mono text-[10px] text-[#00ff66] flex flex-col gap-1 bg-[#010204]">
              {terminalHistory.map((line, idx) => (
                <div key={idx} className="whitespace-pre-wrap leading-tight">
                  {line}
                </div>
              ))}
              <div ref={terminalEndRef} />
            </div>
            <form onSubmit={handleTerminalSubmit} className="p-2 border-t border-slate-800 bg-[#04060b]">
              <div className="flex items-center gap-1">
                <span className="text-[#00ff66] font-mono text-[10px] select-none">$</span>
                <input
                  type="text"
                  placeholder="Type command..."
                  value={terminalInput}
                  onChange={(e) => setTerminalInput(e.target.value)}
                  className="flex-grow bg-transparent border-none outline-none font-mono text-[10px] text-[#00ff66] placeholder-slate-700"
                />
              </div>
            </form>
          </div>
        )}

        {/* ব্রাউজার */}
        {!isSimple && chatTabBrowserOpen && (
          <div className={`h-full flex flex-col bg-[#0f1118] ${
            chatTabTerminalOpen ? 'col-span-3' : 'col-span-6'
          }`}>
            <div className="h-10 border-b border-slate-800 bg-[#08090d] px-2.5 flex items-center gap-2">
              <div className="flex gap-1.5">
                <div className="w-2.5 h-2.5 rounded-full bg-red-500/80"></div>
                <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/80"></div>
                <div className="w-2.5 h-2.5 rounded-full bg-green-500/80"></div>
              </div>
              <form onSubmit={handleBrowserGo} className="flex-grow flex items-center gap-1.5 bg-[#141822] border border-slate-800 rounded-md px-2 py-0.5">
                <Compass size={10} className="text-slate-500" />
                <input
                  type="text"
                  value={browserUrl}
                  onChange={(e) => setBrowserUrl(e.target.value)}
                  className="flex-grow bg-transparent border-none outline-none font-mono text-[9px] text-slate-300"
                />
                <button type="submit" className="text-slate-400 hover:text-white transition-colors">
                  <RefreshCw size={8} />
                </button>
              </form>
            </div>
            <div className="flex-1 bg-white overflow-y-auto">
              <iframe
                title="Browser sandbox"
                src={browserUrl}
                sandbox="allow-scripts allow-same-origin"
                className="w-full h-full border-none bg-white"
                onError={() => console.log('Iframe load error')}
              />
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
