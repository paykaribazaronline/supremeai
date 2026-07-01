import { useEffect, useState } from 'react';
import type { AdminSubTab, GcpHealth, CloudStats } from '../../types';
import { SubTabContent } from './AdminSubTabContent';
import { AdminTopNav } from './AdminTopNav';
import { 
  Search, 
  LayoutDashboard, 
  FileCode, 
  GitMerge, 
  Server, 
  BarChart3, 
  Users, 
  Settings,
  Terminal
} from 'lucide-react';

interface AuthenticatedViewProps {
  gcpHealth: GcpHealth | null;
  cloudStats: CloudStats | null;
  skillQuery: string;
  setSkillQuery: (val: string) => void;
  skills: any[];
  handleInstallSkill: (name: string) => void;
  checkpoints: any[];
  handleDeleteCheckpoint: (taskId: string) => void;
  adminSubTab: AdminSubTab;
  setAdminSubTab: (tab: AdminSubTab) => void;
  handleTriggerDeploy: () => void;
  adminMessages: any[];
  loading: boolean;
  adminInput: string;
  setAdminInput: (val: string) => void;
  handleSendAdmin: () => void;
  rulesJson: string;
  setRulesJson: (val: string) => void;
  saveStatus: string;
  handleSaveRules: () => void;
  liveLogs: string[];
  setLiveLogs: (logs: string[]) => void;
  costReport: string;
  healthMap: any;
  newUsername: string;
  setNewUsername: (val: string) => void;
  newUserRole: string;
  setNewUserRole: (val: string) => void;
  newUserPerms: string;
  setNewUserPerms: (val: string) => void;
  handleSaveUser: () => void;
  adminUsers: any[];
  handleDeleteUser: (username: string) => void;
  envConfig: Record<string, string>;
  setEnvConfig: React.Dispatch<React.SetStateAction<Record<string, string>>>;
  handleSaveConfig: () => void;
  actionStatus: string;
  handleAdminLogout: () => void;
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

// বাংলা মন্তব্য: সুপ্রিম গড মোড অথেনটিকেটেড লেআউট (Authenticated Dashboard Layout)
// এটি ওপরের টপ নেভিগেশন বার, বাম পাশের ৭-আইটেম সাইডবার এবং মূল কন্টেন্ট প্যানেল যুক্ত করে রিডিজাইন করা হয়েছে।
export function AuthenticatedView(props: AuthenticatedViewProps) {
  const { adminSubTab, setAdminSubTab, handleAdminLogout } = props;
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Cmd+K to open Command Palette
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsPaletteOpen(prev => !prev);
      }
      if (e.key === 'Escape' && isPaletteOpen) {
        setIsPaletteOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isPaletteOpen]);

  // ৭টি ড্যাশবোর্ড সাইডবার আইটেম (রেফারেন্স ইমেজ অনুযায়ী) + ১টি নতুন ইন্টারেক্টিভ চ্যাট ট্যাব
  const sidebarItems = [
    { id: 'dashboard', label: 'DASHBOARD', icon: <LayoutDashboard size={16} /> },
    { id: 'interactive-chat', label: 'INTERACTIVE CHAT', icon: <Terminal size={16} /> },
    { id: 'model-router', label: 'MODEL REGISTRY', icon: <FileCode size={16} /> },
    { id: 'cicd', label: 'WORKFLOWS', icon: <GitMerge size={16} /> },
    { id: 'cloud', label: 'COMPUTING', icon: <Server size={16} /> },
    { id: 'observability', label: 'ANALYTICS', icon: <BarChart3 size={16} /> },
    { id: 'users', label: 'AGENTS', icon: <Users size={16} /> },
    { id: 'config', label: 'SETTINGS', icon: <Settings size={16} /> },
  ];

  // কমান্ড প্যালেট অপশনসমূহ
  const navigationOptions = [
    { id: 'dashboard', label: 'Dashboard Overview' },
    { id: 'interactive-chat', label: 'Interactive Chat (Browser & Terminal)' },
    { id: 'command-center', label: 'SupremeAI Nexus (Canvas)' },
    { id: 'logs', label: 'Real-time Logs' },
    { id: 'costs', label: 'Cost Auditor' },
    { id: 'health', label: 'Health Map' },
    { id: 'users', label: 'User Manager / Agents' },
    { id: 'config', label: 'Config Editor' },
    { id: 'model-router', label: 'Model Router' },
    { id: 'skills', label: 'Skill Marketplace' },
    { id: 'memory', label: 'Memory Browser' },
    { id: 'cloud', label: 'Cloud Orchestrator' },
    { id: 'observability', label: 'Observability' },
    { id: 'threats', label: 'Threat Detection' },
    { id: 'rules', label: 'Rules Builder' },
    { id: 'cicd', label: 'CI/CD Pipelines' },
    { id: 'github', label: 'GitHub Integration' },
    { id: 'backups', label: 'Backup & Restore' },
    { id: 'rate-limits', label: 'Rate Limits' },
    { id: 'security-dashboard', label: '🧠 Security & Memory Dashboard' }
  ];

  const filteredOptions = navigationOptions.filter(opt => 
    opt.label.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-[#030407]">
      {/* ১. টপ নেভিগেশন বার */}
      <AdminTopNav onLogout={handleAdminLogout} />

      {/* নিচের অংশ: সাইডবার + মূল কন্টেন্ট */}
      <div className="flex-1 flex overflow-hidden relative">
        
        {/* ২. বাম পাশের নেভিগেশন সাইডবার */}
        <aside className="w-64 bg-[#040814]/90 border-r border-[#00f3ff]/15 flex flex-col justify-between py-6 font-sans text-slate-400 select-none z-20">
          <div className="space-y-1 px-3">
            {sidebarItems.map(item => {
              const isActive = adminSubTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setAdminSubTab(item.id as AdminSubTab)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-xs font-semibold tracking-wider transition-all duration-300 ${
                    isActive 
                      ? 'bg-[#00f3ff]/10 text-[#00f3ff] border-l-2 border-[#00f3ff] shadow-[inset_0_0_12px_rgba(0,243,255,0.05)]' 
                      : 'hover:bg-slate-900/50 hover:text-slate-200'
                  }`}
                >
                  <span className={isActive ? 'text-[#00f3ff]' : 'text-slate-500'}>
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </button>
              );
            })}
          </div>

          {/* অতিরিক্ত অ্যাডমিন টুলস (অরবিট ক্যানভাস লিঙ্ক) */}
          <div className="px-6 border-t border-slate-900 pt-4">
            <button
              onClick={() => setAdminSubTab('command-center')}
              className={`w-full flex items-center justify-center gap-2 px-3 py-2 rounded border border-[#00f3ff]/30 text-[#00f3ff] hover:bg-[#00f3ff]/10 text-xs font-mono font-bold tracking-widest uppercase transition-all duration-300 ${
                adminSubTab === 'command-center' ? 'bg-[#00f3ff]/20' : ''
              }`}
            >
              <Terminal size={14} />
              <span>Core Canvas</span>
            </button>
            <div className="text-[9px] text-slate-600 text-center mt-3 font-mono">
              CTRL+K for command menu
            </div>
          </div>
        </aside>

        {/* ৩. মূল কন্টেন্ট প্যানেল */}
        <main className="flex-1 flex flex-col min-w-0 bg-[#030611] overflow-hidden">
          <SubTabContent {...props} />
        </main>
      </div>

      {/* ৪. কমান্ড প্যালেট ওভারলে (Cmd+K) */}
      {isPaletteOpen && (
        <div className="absolute inset-0 z-[100] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-lg bg-[#050917] border border-[#00f3ff]/30 rounded-xl shadow-[0_0_40px_rgba(0,243,255,0.15)] flex flex-col overflow-hidden">
            <div className="flex items-center gap-3 px-4 py-3 border-b border-[#00f3ff]/20">
              <Search className="text-[#00f3ff] w-5 h-5" />
              <input
                autoFocus
                type="text"
                placeholder="Navigate to... (e.g. Cost Auditor)"
                className="flex-1 bg-transparent border-none outline-none text-white font-mono placeholder:text-slate-500"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <span className="text-xs text-slate-500 font-mono">ESC to close</span>
            </div>
            <div className="max-h-[60vh] overflow-y-auto p-2">
              {filteredOptions.map((opt, i) => (
                <button
                  key={opt.id}
                  onClick={() => {
                    setAdminSubTab(opt.id as AdminSubTab);
                    setIsPaletteOpen(false);
                    setSearchQuery('');
                  }}
                  className={`w-full text-left px-4 py-3 rounded-lg font-mono transition-colors flex items-center gap-3 ${
                    i === 0 && searchQuery ? 'bg-[#00f3ff]/10 text-[#00f3ff]' : 'hover:bg-white/5 text-slate-300'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
              {filteredOptions.length === 0 && (
                <div className="px-4 py-8 text-center text-slate-500 font-mono">
                  No modules found.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}