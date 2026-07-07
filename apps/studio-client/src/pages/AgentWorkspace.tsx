import React, { useState, useEffect, useRef } from 'react';
import Editor from '@monaco-editor/react';
import { Terminal } from 'xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebContainer } from '@webcontainer/api'; // 🟢 নতুন ইমপোর্ট
import 'xterm/css/xterm.css'; // টার্মিনালের স্টাইল

// টাইপ ডেফিনিশন
interface Message {
  role: 'user' | 'agent';
  content: string;
  source?: 'ai_api' | 'memory';
}

export const AgentWorkspace: React.FC = () => {
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [generatedCode, setGeneratedCode] = useState<string>('// SupremeAI Agent Ready.\n// Type a prompt on the left to generate code...');
  const [isLoading, setIsLoading] = useState(false);

  const terminalRef = useRef<HTMLDivElement>(null);
  const xtermRef = useRef<Terminal | null>(null);
  const webcontainerRef = useRef<WebContainer | null>(null); // 🟢 WebContainer Ref
  const wsRef = useRef<WebSocket | null>(null);
  const shellWriterRef = useRef<WritableStreamDefaultWriter<string> | null>(null);

  useEffect(() => {
    let term: Terminal;

    const initTerminalAndWebContainer = async () => {
      if (terminalRef.current && !xtermRef.current) {
        // ১. টার্মিনাল সেটআপ
        term = new Terminal({
          theme: { background: '#1e1e1e', foreground: '#d4d4d4' },
          fontFamily: '"Fira Code", monospace',
          fontSize: 13,
          cursorBlink: true,
        });
        const fitAddon = new FitAddon();
        term.loadAddon(fitAddon);
        term.open(terminalRef.current);
        fitAddon.fit();
        xtermRef.current = term;

        term.writeln('🚀 \x1b[1;34mSupremeAI Hybrid Engine\x1b[0m initializing...');
        term.writeln('⏳ Booting Zero-Cost Node.js environment in browser...');

        try {
          // ২. WebContainer বুট করা (Zero-Cost Environment)
          const webcontainerInstance = await WebContainer.boot();
          webcontainerRef.current = webcontainerInstance;
          term.writeln('✅ \x1b[1;32mWebContainer Booted Successfully!\x1b[0m\r\n');

          // ৩. WebContainer-এ একটি Shell (jsh) স্টার্ট করা
          const shellProcess = await webcontainerInstance.spawn('jsh');

          // ৪. Shell এর আউটপুট টার্মিনালে দেখানো
          shellProcess.output.pipeTo(
            new WritableStream({
              write(data) {
                term.write(data);
              },
            })
          );

          // ৫. ইউজারের টাইপ করা ইনপুট Shell-এ পাঠানো
          const input = shellProcess.input.getWriter();
          shellWriterRef.current = input; // 🟢 এটি নতুন লাইন
          term.onData((data) => {
            input.write(data);
          });

        } catch (error) {
          term.writeln('\r\n❌ \x1b[1;31mFailed to boot WebContainer. Please check Vite COOP/COEP headers.\x1b[0m');
          console.error(error);
        }

        window.addEventListener('resize', () => fitAddon.fit());
      }
    };

    initTerminalAndWebContainer();

    return () => {
      xtermRef.current?.dispose();
      xtermRef.current = null;
      // WebContainer cleanup (অটোমেটিক্যালি হয়, তবে সতর্কতার জন্য)
      if (webcontainerRef.current) {
        webcontainerRef.current.teardown();
        webcontainerRef.current = null;
      }
    };
  }, []);

  const handleExecute = async () => {
    if (!prompt.trim()) return;

    // ইউজারের মেসেজ অ্যাড করা
    const newMessages = [...messages, { role: 'user', content: prompt } as Message];
    setMessages(newMessages);
    setPrompt('');
    setIsLoading(true);

    try {
      // ব্যাকএন্ড API কল (আপনার FastAPI সার্ভারের URL)
      const response = await fetch('http://localhost:8000/api/v1/agent/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          prompt: prompt,
          project_id: 'proj_123'
        }),
      });

      const data = await response.json();

      if (data.status === 'success') {
        // এআই এর রেসপন্স এবং সোর্স (API নাকি Memory) অ্যাড করা
        setMessages([
          ...newMessages, 
          { 
            role: 'agent', 
            content: data.message,
            source: data.source 
          }
        ]);
        // Monaco Editor এ কোড আপডেট করা
        setGeneratedCode(data.code);
      }
    } catch (error) {
      console.error("Error executing agent command:", error);
      setMessages([...newMessages, { role: 'agent', content: '⚠️ Connection error to SupremeAI Backend.' }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRunCode = async () => {
    if (!webcontainerRef.current || !shellWriterRef.current) {
      console.warn("⚠️ Sandbox is not fully loaded yet.");
      return;
    }

    try {
      // ১. Monaco Editor-এর কোড WebContainer-এর ভার্চুয়াল ফাইলে সেভ করা
      await webcontainerRef.current.fs.writeFile('/index.js', generatedCode);
      
      // ২. টার্মিনালকে কমান্ড পাঠানো (node index.js রান করতে বলা)
      // \r মানে হলো Enter প্রেস করা
      await shellWriterRef.current.write('node index.js\r');
      
    } catch (error) {
      console.error("Failed to execute code in sandbox:", error);
    }
  };

  return (
    <div className="flex h-screen w-full bg-gray-900 text-white overflow-hidden">
      {/* 🟢 LEFT PANEL: Chat & Planner */}
      <div className="w-1/3 border-r border-gray-700 flex flex-col bg-gray-800">
        <div className="p-4 border-b border-gray-700 bg-gray-900 font-bold text-lg text-blue-400">
          🧠 SupremeAI Agent
        </div>
        
        {/* Chat History */}
        <div className="flex-1 p-4 overflow-y-auto space-y-4">
          {messages.map((msg, idx) => (
            <div key={idx} className={`p-3 rounded-lg max-w-[90%] ${msg.role === 'user' ? 'bg-blue-600 ml-auto' : 'bg-gray-700 mr-auto'}`}>
              <p className="text-sm">{msg.content}</p>
              {msg.source && (
                <span className={`text-xs mt-2 block px-2 py-1 rounded inline-block ${msg.source === 'memory' ? 'bg-green-500/20 text-green-300' : 'bg-purple-500/20 text-purple-300'}`}>
                  ⚡ Source: {msg.source === 'memory' ? 'Zero-Cost Memory' : 'Premium AI'}
                </span>
              )}
            </div>
          ))}
          {isLoading && (
            <div className="p-3 rounded-lg bg-gray-700 w-32 text-center text-sm animate-pulse">
              Agent is thinking...
            </div>
          )}
        </div>

        {/* Input Area */}
        <div className="p-4 border-t border-gray-700 bg-gray-900">
          <textarea
            className="w-full bg-gray-800 border border-gray-700 rounded p-3 text-white focus:outline-none focus:border-blue-500 resize-none"
            rows={3}
            placeholder="E.g., Create a responsive login form in React..."
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleExecute();
              }
            }}
          />
          <button 
            onClick={handleExecute}
            disabled={isLoading || !prompt.trim()}
            className="mt-2 w-full bg-blue-600 hover:bg-blue-500 disabled:bg-gray-600 text-white font-bold py-2 px-4 rounded transition-colors"
          >
            Execute Command
          </button>
        </div>
      </div>
      
      {/* 🔴 RIGHT PANEL: Live Code Editor & Terminal */}
      <div className="w-2/3 h-full flex flex-col bg-[#1e1e1e]">
        
        {/* Top 70%: Code Editor */}
        <div className="flex-1 flex flex-col min-h-0 border-b border-gray-700">
          <div className="p-2 text-sm text-gray-400 bg-[#252526] flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span>📄 index.js</span>
              <span className="text-xs bg-gray-700 px-2 py-1 rounded">JavaScript</span>
            </div>
            
            {/* 🟢 নতুন Run Button */}
            <button 
              onClick={handleRunCode}
              className="bg-green-600 hover:bg-green-500 text-white text-xs font-bold py-1 px-3 rounded flex items-center transition-colors"
            >
              ▶ Run Code
            </button>

          </div>
          <div className="flex-1">
            <Editor
              height="100%"
              theme="vs-dark"
              defaultLanguage="javascript" // 🟢 typescript থেকে javascript করে দিন টেস্টিংয়ের সুবিধার জন্য
              value={generatedCode}
              onChange={(value) => setGeneratedCode(value || '')} // 🟢 ইউজার ম্যানুয়ালি কোড এডিট করলে স্টেট আপডেট হবে
              options={{ minimap: { enabled: false } }}
            />
          </div>
        </div>

        {/* Bottom 30%: Live Terminal */}
        <div className="h-72 flex flex-col bg-[#1e1e1e]">
          <div className="p-2 text-sm text-gray-400 bg-[#252526] flex items-center shadow-md z-10">
            <span>🖥️ Execution Terminal (Hybrid Mode)</span>
          </div>
          {/* xterm.js ক্যানভাস এখানে মাউন্ট হবে */}
          <div ref={terminalRef} className="flex-1 p-2 overflow-hidden bg-[#1e1e1e]" />
        </div>

      </div>
    </div>
  );
};
