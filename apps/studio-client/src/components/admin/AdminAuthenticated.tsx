import { useEffect, useState } from 'react';
import type { AdminSubTab, GcpHealth, CloudStats } from '../../types';
import { SubTabContent } from './AdminSubTabContent';
import { Search } from 'lucide-react';

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

export function AuthenticatedView(props: AuthenticatedViewProps) {
  const { adminSubTab, setAdminSubTab } = props;
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

  const navigationOptions = [
    { id: 'command-center', label: 'SupremeAI Nexus (Canvas)' },
    { id: 'logs', label: 'Real-time Logs' },
    { id: 'costs', label: 'Cost Auditor' },
    { id: 'health', label: 'Health Map' },
    { id: 'users', label: 'User Manager' },
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
    <div className="flex-1 flex flex-col overflow-hidden relative">
      {/* Main Content Area (Full Screen) */}
      <div className="flex-1 flex flex-col min-w-0">
        <SubTabContent {...props} />
      </div>

      {/* Command Palette Overlay */}
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