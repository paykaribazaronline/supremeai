# 📄 ফাইল: apps/studio-client/src/pages/AgentWorkspace.tsx

**প্রকার:** .tsx  
**সাইজ:** 14,794 বাইট  
**আপডেট:** 2026-07-07T19:02:10.590249

---

## কোড

```tsx
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
  const [isHealing, setIsHealing] = useState(false);

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

  const handleRunAndEvaluate = async (codeToRun = generatedCode, retryCount = 0) => {
    if (!webcontainerRef.current || !xtermRef.current) return;
    const term = xtermRef.current;
    
    setIsHealing(true);
    try {
      term.writeln(`\r\n⚙️ \x1b[1;36m[Execution] Running code... (Attempt ${retryCount + 1}/3)\x1b[0m`);
      
      // ১. ফাইল সেভ করা
      await webcontainerRef.current.fs.writeFile('/index.js', codeToRun);
      
      // ২. সরাসরি Node.js প্রসেস স্পন (Spawn) করা যাতে Exit Code ধরতে পারি
      const process = await webcontainerRef.current.spawn('node', ['index.js']);
      
      let processOutput = '';
      
      // ৩. আউটপুট ক্যাপচার করা এবং টার্মিনালে দেখানো
      process.output.pipeTo(
        new WritableStream({
          write(data) {
            processOutput += data;
            term.write(data);
          }
        })
      );
      
      // ৪. প্রসেস শেষ হওয়ার জন্য অপেক্ষা করা (The Evaluation)
      const exitCode = await process.exit;
      
      if (exitCode !== 0) {
        // ❌ এরর পেয়েছে! (Self-Healing Loop)
        if (retryCount < 2) {
          term.writeln(`\r\n⚠️ \x1b[1;33m[Auto-Heal] Code failed with exit code ${exitCode}. Requesting AI fix...\x1b[0m`);
          
          // ব্যাকএন্ডে এরর মেসেজসহ ফিক্সের জন্য রিকোয়েস্ট পাঠানো
          const fixResponse = await fetch('http://localhost:8000/api/v1/agent/execute', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              prompt: `I tried to run this code but got an error. \n\nCODE:\n${codeToRun}\n\nERROR:\n${processOutput}\n\nPlease fix the bug and return ONLY the full working code.`,
              project_id: 'proj_123'
            }),
          });
          
          const fixData = await fixResponse.json();
          if (fixData.status === 'success') {
            const fixedCode = fixData.code;
            setGeneratedCode(fixedCode); // এডিটরে নতুন কোড বসবে
            setMessages(prev => [...prev, { role: 'agent', content: `🔧 I analyzed the error and fixed the code. Retrying...` }]);
            
            // রিকার্সিভ কল (নতুন কোড দিয়ে আবার টেস্ট করবে)
            await handleRunAndEvaluate(fixedCode, retryCount + 1);
          }
        } else {
          term.writeln(`\r\n❌ \x1b[1;31m[System] Self-healing failed after 3 attempts. Manual intervention required.\x1b[0m`);
        }
      } else {
        // ✅ কোড পারফেক্টলি রান করেছে! (The Learning Phase)
        term.writeln(`\r\n✅ \x1b[1;32m[Success] Execution flawless! Committing to Memory Vault...\x1b[0m`);
        
        // ব্যাকএন্ডকে কনফার্ম করা যে কোডটি কাজ করেছে, মেমোরিতে সেভ করো
        await fetch('http://localhost:8000/api/v1/agent/learn', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            prompt: prompt, // অরিজিনাল প্রম্পট
            working_code: codeToRun
          }),
        });
        
        setMessages(prev => [...prev, { role: 'agent', content: `🎯 Execution verified! I have memorized this solution in the Zero-Cost Vault.`, source: 'memory' }]);

        // 🟢 ২. GitHub-এ Auto-PR তৈরি করা (The New Magic)
        term.writeln(`\r\n🐙 \x1b[1;34m[GitHub] Pushing verified code to repository as a PR...\x1b[0m`);
        
        const prResponse = await fetch('http://localhost:8000/api/v1/agent/github/pr', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            user_id: 'admin_123', // সেশন থেকে নেবেন
            repo_name: 'YOUR_GITHUB_USERNAME/YOUR_TEST_REPO', // আপনার টেস্ট রিপোর নাম দিন
            file_path: 'src/auto_generated.js',
            code: codeToRun,
            prompt: prompt
          }),
        });

        const prData = await prResponse.json();
        if (prData.status === 'success') {
          term.writeln(`\r\n🎉 \x1b[1;32m[GitHub] PR Created Successfully! Link: ${prData.pr_url}\x1b[0m`);
          setMessages(prev => [...prev, { role: 'agent', content: `🚀 I have autonomously created a Pull Request in your GitHub repo! Check it out: ${prData.pr_url}` }]);
        } else {
          term.writeln(`\r\n❌ \x1b[1;31m[GitHub Error] Failed to create PR: ${prData.message}\x1b[0m`);
        }
      }
    } catch (error) {
      console.error("Execution error:", error);
    } finally {
      setIsHealing(false);
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
              onClick={() => handleRunAndEvaluate(generatedCode, 0)}
              disabled={isHealing}
              className="bg-green-600 hover:bg-green-500 disabled:bg-gray-600 text-white text-xs font-bold py-1 px-3 rounded flex items-center transition-colors"
            >
              {isHealing ? '⏳ Healing...' : '▶ Run & Auto-Evaluate'}
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

```