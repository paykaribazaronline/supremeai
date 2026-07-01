import React, { useState, useEffect, useRef } from 'react';
import { Terminal, Globe, Send, RefreshCw, Eye, EyeOff, Layout, TerminalSquare, Compass } from 'lucide-react';
import { useDashboardStore } from '../../store/dashboardStore';
// বাংলা মন্তব্য: চ্যাট মেসেজ ইন্টারফেস
interface Message {
  id: string;
  sender: 'user' | 'system';
  text: string;
  timestamp: string;
}

// বাংলা মন্তব্য: প্রপ্স ডিক্লেয়ারেশন যা কাস্টমার ড্যাশবোর্ড থেকে পাস করা হবে
interface InteractiveChatTabProps {
  messages?: any[];
  input?: string;
  onInputChange?: (val: string) => void;
  onSend?: () => void;
  loading?: boolean;
}

export function InteractiveChatTab({
  messages: propMessages,
  input: propInput,
  onInputChange,
  onSend,
  loading = false,
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
      id: '1',
      sender: 'system',
      text: 'স্বাগতম! আমি SupremeAI 2.0 অ্যাসিস্ট্যান্ট। আমি আপনাকে অর্কেস্ট্রেশন ও সিস্টেম ডেপ্লয়মেন্টে সাহায্য করতে পারি।',
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [internalInput, setInternalInput] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  // বাংলা মন্তব্য: প্রপ্স থাকলে তা ব্যবহার করা হবে, অন্যথায় ইন্টারনাল স্টেট
  const activeMessages = propMessages
    ? propMessages.map((m, idx) => ({
        id: m.id ? String(m.id) : String(idx),
        sender: String(m.sender).toLowerCase() === 'user' ? 'user' : ('system' as const),
        text: m.text,
        timestamp: m.timestamp || new Date().toLocaleTimeString(),
      }))
    : internalMessages;

  const activeInput = propInput !== undefined ? propInput : internalInput;
  const setActiveInput = onInputChange || setInternalInput;

  // --- টার্মিনাল স্ট্যাটস ---
  const [terminalHistory, setTerminalHistory] = useState<string[]>([
    'SupremeAI Terminal v2.0.0 (Secure Read-Only Mock Shell)',
    'Type "help" for a list of available commands.',
    '',
  ]);
  const [terminalInput, setTerminalInput] = useState('');
  const terminalEndRef = useRef<HTMLDivElement>(null);

  // --- ব্রাউজার স্ট্যাটস ---
  const [browserUrl, setBrowserUrl] = useState('https://supremeai.dev/docs');
  const [browserHistory, setBrowserHistory] = useState<string[]>(['https://supremeai.dev/docs']);
  const [currentBrowserIndex, setCurrentBrowserIndex] = useState(0);

  // অটো-স্ক্রোল ইফেক্ট (জেএসডম টেস্টে টাইপ এরর এড়াতে চেক যোগ করা হয়েছে)
  useEffect(() => {
    if (chatEndRef.current && typeof chatEndRef.current.scrollIntoView === 'function') {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeMessages]);

  useEffect(() => {
    if (terminalEndRef.current && typeof terminalEndRef.current.scrollIntoView === 'function') {
      terminalEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [terminalHistory]);

  // চ্যাট সাবমিট
  const handleSendChat = () => {
    const inputVal = activeInput;
    if (!inputVal.trim()) return;

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
    setInternalMessages((prev) => [...prev, userMsg]);
    setActiveInput('');

    // কৃত্রিম রেসপন্স তৈরি
    setTimeout(() => {
      let botResponse = `Received: "${inputVal}". Processing on core node...`;
      if (inputVal.toLowerCase().includes('status')) {
        botResponse = 'সিস্টেম স্ট্যাটাস বর্তমানে সম্পূর্ণ সচল (HEALTHY)। কোনো মেমোরি লিক বা জিপিইউ ওভারলোড সনাক্ত করা যায়নি।';
      } else if (inputVal.toLowerCase().includes('help')) {
        botResponse = 'আপনি টার্মিনালে "help" লিখে কমান্ডের তালিকা দেখতে পারেন অথবা সরাসরি যেকোনো টেকনিক্যাল জিজ্ঞাসা করতে পারেন।';
      }
      setInternalMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          sender: 'system',
          text: botResponse,
          timestamp: new Date().toLocaleTimeString(),
        },
      ]);
    }, 800);
  };

  // টার্মিনাল কমান্ড প্রসেসর
  const handleTerminalSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!terminalInput.trim()) return;
    const cmd = terminalInput.trim();

    // বাংলা মন্তব্য: clear কমান্ডটি আগে হ্যান্ডেল করা হলো যেন ESLint কোনো অব্যবহৃত অ্যাসাইনমেন্ট ট্র্যাকিং এরর না দেয়
    if (cmd.toLowerCase() === 'clear') {
      setTerminalHistory([]);
      setTerminalInput('');
      return;
    }

    const newHistory = [...terminalHistory, `supremeai-user$ ${cmd}`];
    const args = cmd.toLowerCase().split(' ');
    const primaryCmd = args[0];

    // রিড-অনলি কমান্ডের লজিক (নিরাপত্তা নিশ্চিতকরণের জন্য)
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
\\____/\\__,_/ .___/_/   \\___/_/ /_/_/  /_/   /___/   
          /_/                                       
OS: SupremeAI Enterprise OS v2.0
Kernel: Cloud-Orchestrated Linux (v5.15)
Uptime: 4 days, 12 hours
Shell: supreme-sh
CPU: Virtualized AMD EPYC (8 Cores)
RAM: 32 GB (Active: 12.4 GB)
`;
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

  // ব্রাউজার নেভিগেশন
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

  const isSimple = dashboardMode === 'simple';

  return (
    <div className={`w-full h-[calc(100vh-140px)] flex flex-col transition-all duration-500 ${isSimple ? 'text-slate-800' : 'text-slate-200'}`}>
      
      {/* কন্ট্রোল প্যানেল (হাইড/আনহাইড করার বোতামসমূহ) - কেবল অ্যাডভান্সড মোডে দৃশ্যমান */}
      {!isSimple && (
        <div className="flex justify-between items-center px-4 py-2 bg-[#090b11] border-b border-slate-800">
          <span className="text-[10px] font-mono text-[#00f3ff] uppercase tracking-widest">// Interactive Layout Controller</span>
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
        
        {/* চ্যাট প্যানেল (সিম্পল মোডে ১২ কলাম জুড়ে থাকবে, অ্যাডভান্সডে ডায়নামিক) */}
        <div className={`h-full flex flex-col border-r border-slate-800 transition-all duration-500 ${
          isSimple 
            ? 'col-span-12 bg-white text-slate-900' 
            : `${!chatTabTerminalOpen && !chatTabBrowserOpen ? 'col-span-12' : (chatTabTerminalOpen && chatTabBrowserOpen ? 'col-span-6' : 'col-span-9')} bg-[#05070c]/50 text-slate-200`
        }`}>
          {/* চ্যাট হেডার */}
          <div className={`h-10 border-b flex items-center justify-between px-4 ${isSimple ? 'bg-slate-100 border-slate-200' : 'bg-[#080a10] border-slate-800'}`}>
            <span data-testid="chat-header" className={`text-xs font-bold uppercase tracking-wider ${isSimple ? 'text-indigo-600' : 'text-slate-200'}`}>
              SupremeAI Chat
            </span>
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full bg-emerald-500 ${!isSimple && 'animate-pulse'}`}></span>
              <span className="text-[10px] font-mono text-emerald-500 font-semibold">SECURE CORRIDOR</span>
            </div>
          </div>

          {/* চ্যাট হিস্ট্রি */}
          <div className={`flex-1 p-4 overflow-y-auto flex flex-col gap-4 ${isSimple ? 'bg-indigo-50/20' : 'bg-[#030509]/30'}`}>
            {activeMessages.map((msg) => (
              <div
                key={msg.id}
                className={`max-w-[85%] flex flex-col gap-1 ${
                  msg.sender === 'user' ? 'self-end items-end' : 'self-start items-start'
                }`}
              >
                <div
                  className={`p-3.5 rounded-2xl text-xs leading-relaxed ${
                    msg.sender === 'user'
                      ? isSimple
                        ? 'bg-indigo-600 text-white rounded-tr-none shadow-md'
                        : 'bg-gradient-to-br from-[#00f3ff] to-blue-600 text-black font-semibold rounded-tr-none'
                      : isSimple
                        ? 'bg-white border border-slate-200 text-slate-800 rounded-tl-none shadow-sm'
                        : 'bg-[#0e111a]/90 border border-slate-800 text-slate-300 rounded-tl-none'
                  }`}
                >
                  {msg.text}
                </div>
                <span className="text-[9px] text-slate-500 px-1 font-mono">{msg.timestamp}</span>
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>

          {/* চ্যাট ইনপুট */}
          <div className={`p-4 border-t ${isSimple ? 'bg-white border-slate-200' : 'bg-[#05070c] border-slate-800'}`}>
            <div className="flex gap-2">
              <input
                data-testid="chat-input"
                type="text"
                placeholder="Ask anything or generate code..."
                value={activeInput}
                onChange={(e) => setActiveInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendChat()}
                className={`flex-grow border rounded-xl px-4 py-2.5 text-xs outline-none transition-colors ${
                  isSimple 
                    ? 'bg-slate-50 border-slate-300 focus:border-indigo-500 text-slate-900' 
                    : 'bg-[#0c0e14] border-slate-700 focus:border-[#00f3ff] text-white'
                }`}
              />
              <button
                data-testid="chat-submit"
                onClick={handleSendChat}
                className={`px-4 rounded-xl flex items-center justify-center gap-1.5 transition-all ${
                  isSimple 
                    ? 'bg-indigo-600 hover:bg-indigo-700 text-white font-bold' 
                    : 'bg-[#00f3ff] hover:bg-cyan-400 text-black font-extrabold shadow-[0_0_15px_rgba(0,243,255,0.4)]'
                }`}
              >
                <Send size={14} />
                <span>Send</span>
              </button>
            </div>
          </div>
        </div>

        {/* টার্মিনাল উইন্ডো (অ্যাডভান্সড মোডে এবং ওপেন থাকলে দৃশ্যমান) */}
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

        {/* ব্রাউজার উইন্ডো (অ্যাডভান্সড মোডে এবং ওপেন থাকলে দৃশ্যমান) */}
        {!isSimple && chatTabBrowserOpen && (
          <div className={`h-full flex flex-col bg-[#0f1118] ${
            chatTabTerminalOpen ? 'col-span-3' : 'col-span-6'
          }`}>
            {/* ব্রাউজার কন্ট্রোলস */}
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

            {/* ব্রাউজার কন্টেন্ট এরিয়া */}
            <div className="flex-1 bg-white overflow-y-auto">
              {/* নিরাপত্তা নিশ্চিত করতে sandbox অ্যাট্রিবিউটসহ iframe ব্যবহার */}
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
