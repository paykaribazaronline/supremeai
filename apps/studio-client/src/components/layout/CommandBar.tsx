// apps/studio-client/src/components/layout/CommandBar.tsx
// Universal Command Palette (Ctrl+K / Cmd+K)
// বাংলা মন্তব্য: ইউনিভার্সাল কমান্ড প্যালেট — দ্রুত অ্যাকশন, মডেল সুইচিং এবং পেজ ন্যাভিগেশনের জন্য।

import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Terminal, Cpu, Shield, Zap, Sparkles, Folder, Command, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface CommandItem {
  id: string;
  title: string;
  category: 'Actions' | 'Navigation' | 'AI Models' | 'System';
  icon: React.ElementType;
  shortcut?: string;
  action: () => void;
}

interface CommandBarProps {
  isOpen?: boolean;
  onClose?: () => void;
}

export const CommandBar: React.FC<CommandBarProps> = ({ isOpen: controlledOpen, onClose }) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const [query, setQuery] = useState('');
  const navigate = useNavigate();

  const isOpen = controlledOpen !== undefined ? controlledOpen : internalOpen;

  const handleClose = () => {
    if (onClose) onClose();
    else setInternalOpen(false);
    setQuery('');
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        if (controlledOpen === undefined) {
          setInternalOpen(prev => !prev);
        } else if (isOpen) {
          handleClose();
        }
      }
      if (e.key === 'Escape' && isOpen) {
        handleClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, controlledOpen]);

  const commands: CommandItem[] = [
    {
      id: 'nav-agent',
      title: 'Agent Workspace',
      category: 'Navigation',
      icon: Terminal,
      shortcut: 'Shift+A',
      action: () => { navigate('/workspace/agent'); handleClose(); }
    },
    {
      id: 'nav-ide',
      title: 'Cloud IDE Workspace',
      category: 'Navigation',
      icon: Folder,
      shortcut: 'Shift+I',
      action: () => { navigate('/workspace/ide'); handleClose(); }
    },
    {
      id: 'nav-swarm',
      title: 'Swarm Telemetry & Heatmap',
      category: 'Navigation',
      icon: Cpu,
      shortcut: 'Shift+S',
      action: () => { navigate('/swarm'); handleClose(); }
    },
    {
      id: 'nav-architect',
      title: 'Architect Tower',
      category: 'Navigation',
      icon: Shield,
      action: () => { navigate('/architect-tower'); handleClose(); }
    },
    {
      id: 'nav-skills',
      title: 'Skills Catalog',
      category: 'Navigation',
      icon: Sparkles,
      action: () => { navigate('/skills-catalog'); handleClose(); }
    },
    {
      id: 'action-heal',
      title: 'Trigger Autonomous Self-Healer',
      category: 'Actions',
      icon: Zap,
      shortcut: 'Ctrl+H',
      action: () => { console.warn('Self healer triggered'); handleClose(); }
    },
    {
      id: 'model-deepseek',
      title: 'Switch to DeepSeek-V3 (Coding Expert)',
      category: 'AI Models',
      icon: Cpu,
      action: () => { console.warn('Switched to DeepSeek-V3'); handleClose(); }
    },
    {
      id: 'model-kimi',
      title: 'Switch to Kimi K2.5 (Bangla & Reasoning)',
      category: 'AI Models',
      icon: Sparkles,
      action: () => { console.warn('Switched to Kimi K2.5'); handleClose(); }
    },
  ];

  const filteredCommands = commands.filter(item =>
    item.title.toLowerCase().includes(query.toLowerCase()) ||
    item.category.toLowerCase().includes(query.toLowerCase())
  );

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 px-4 bg-slate-950/80 backdrop-blur-md">
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -10 }}
            className="w-full max-w-2xl overflow-hidden rounded-xl border border-slate-700 bg-slate-900 shadow-2xl"
          >
            {/* Header / Search Input */}
            <div className="flex items-center border-b border-slate-800 px-4 py-3 bg-slate-900/90">
              <Search className="mr-3 h-5 w-5 text-slate-400" />
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Type a command or search workspace... (Ctrl+K)"
                className="flex-1 bg-transparent text-sm text-slate-100 placeholder-slate-500 focus:outline-none"
                autoFocus
              />
              <button
                onClick={handleClose}
                className="rounded p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-200"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {/* Command List */}
            <div className="max-h-96 overflow-y-auto p-2">
              {filteredCommands.length === 0 ? (
                <div className="p-6 text-center text-sm text-slate-500">
                  No commands found matching "{query}"
                </div>
              ) : (
                filteredCommands.map((item) => {
                  const Icon = item.icon;
                  return (
                    <button
                      key={item.id}
                      onClick={item.action}
                      className="w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-left text-sm text-slate-300 hover:bg-cyan-500/10 hover:text-cyan-400 transition-colors group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="rounded p-1.5 bg-slate-800 text-slate-400 group-hover:bg-cyan-500/20 group-hover:text-cyan-400">
                          <Icon className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="font-medium">{item.title}</p>
                          <span className="text-[10px] text-slate-500">{item.category}</span>
                        </div>
                      </div>
                      {item.shortcut && (
                        <kbd className="hidden sm:inline-block rounded bg-slate-800 px-2 py-0.5 text-[10px] font-mono text-slate-400">
                          {item.shortcut}
                        </kbd>
                      )}
                    </button>
                  );
                })
              )}
            </div>

            {/* Footer */}
            <div className="flex items-center justify-between border-t border-slate-800/80 px-4 py-2 bg-slate-950/50 text-[11px] text-slate-500 font-mono">
              <span className="flex items-center gap-1">
                <Command className="h-3 w-3" /> SupremeAI Autonomous Shell
              </span>
              <span>ESC to cancel</span>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

export default CommandBar;
