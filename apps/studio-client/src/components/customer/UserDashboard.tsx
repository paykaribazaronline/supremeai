import { useState, lazy, Suspense } from 'react';
import {
  MessageSquare,
  FolderOpen,
  Zap,
  TrendingUp,
  Settings2,
  Play,
  ChevronRight,
  Activity,
  Clock,
  Sparkles,
  // বাংলা মন্তব্য: নতুন ম্যাপিং ট্যাবের জন্য আইকন
  Globe,
  Smartphone,
  BarChart3,
  Users,
  Shield,
  Server
} from 'lucide-react';
import { HomeFeed } from './HomeFeed';
import { QuickPresets } from './QuickPresets';
// বাংলা মন্তব্য: CodeEditor (Monaco) ভারী লাইব্রেরি — ইনিশিয়াল বান্ডিলে না যাওয়ার জন্য lazy load করা হয়েছে।
const CodeEditor = lazy(() => import('./CodeEditor').then(m => ({ default: m.CodeEditor })));
import { ChatPanel } from './ChatPanel';
// বাংলা মন্তব্য: নতুন ইন্টারঅ্যাক্টিভ চ্যাট ট্যাব ইম্পোর্ট করা হলো
import { InteractiveChatTab } from '../admin/InteractiveChatTab';
// বাংলা মন্তব্য: ব্রাউজার প্রিভিউ ও মোবাইল সিমুলেটর অ্যাক্টিভেট করা হলো
import { BrowserPreview } from './BrowserPreview';
import { MobileSimulator } from './MobileSimulator';
// বাংলা মন্তব্য: i18n হুক ইম্পোর্ট করা হলো
import { useI18n } from '../../i18n/useI18n';
import './UserDashboard.css';

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  role: 'viewer' | 'operator' | 'developer' | 'admin' | 'god';
  avatar_url?: string;
  preferences: {
    theme: 'dark' | 'light';
    sidebar_collapsed: boolean;
    default_project_id?: string;
    notification_enabled: boolean;
    sound_enabled: boolean;
    compact_mode: boolean;
    font_size: 'small' | 'medium' | 'large';
  };
  created_at: string;
  last_login: string;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  created_at: string;
  updated_at: string;
  owner_id: string;
  settings: {
    default_model: string;
    system_prompt: string;
    temperature: number;
    max_tokens: number;
    rag_enabled: boolean;
  };
}

export interface ChatMessage {
  id: number;
  sender: 'User' | 'SupremeAI';
  text: string;
  timestamp?: string;
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

export interface Widget {
  id: string;
  type: 'chat' | 'metrics' | 'history' | 'skills' | 'files' | 'preview';
  title: string;
  position: { x: number; y: number; w: number; h: number };
  settings: Record<string, unknown>;
}

interface UserDashboardProps {
  customerMessages: ChatMessage[];
  customerInput: string;
  setCustomerInput: (val: string) => void;
  loading: boolean;
  handleSendCustomer: () => void;
  theme: 'dark' | 'light';
  toggleTheme: () => void;
  code: string;
  setCode: (code: string) => void;
  isServerOnline?: boolean;
  deployGate?: { status?: string };
  user?: UserProfile | null;
  projects?: Project[];
  chatHistory?: ChatMessage[];
  widgets?: Widget[];
  onSaveToProject?: (code: string) => void;
  onPreview?: (code: string) => void;
}

export function UserDashboard({
  customerMessages,
  customerInput,
  setCustomerInput,
  loading,
  handleSendCustomer,
  theme,
  toggleTheme,
  code,
  setCode,
  isServerOnline = false,
  deployGate = {},
  user = null,
  projects = [],
  chatHistory = [],
  widgets = [],
  onSaveToProject,
  onPreview
}: UserDashboardProps) {
  // বাংলা মন্তব্য: অ্যাক্টিভ ট্যাব স্টেট ইউনিয়ন টাইপ বাড়ানো হলো
  const [activeTab, setActiveTab] = useState<'overview' | 'feed' | 'presets' | 'chat' | 'browser' | 'mobile' | 'analytics' | 'team' | 'security'>('overview');
  // বাংলা মন্তব্য: i18n হুক
  const { t } = useI18n();

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const recentChats = chatHistory.length > 0
    ? chatHistory.slice(-4)
    : customerMessages.slice(-4);

  return (
    <div className="min-h-screen bg-background text-foreground font-mono relative">
      <div className="scanline pointer-events-none fixed inset-0 z-50" />

      <header className="flex justify-between items-center border-b border-border-accent pb-3 px-6 pt-3 mb-4">
        <div className="flex items-center gap-3">
          <span className="text-neon-blue animate-pulse text-lg">▲</span>
          <div>
            {/* বাংলা মন্তব্য: টেস্টে সহজে ও নির্ভরযোগ্যভাবে সনাক্ত করার জন্য header-title data-testid যোগ করা হলো */}
            <h1 data-testid="header-title" className="text-sm font-bold tracking-widest text-neon-blue uppercase">
              {t('ud_welcome_back', { name: user?.username || 'User' })}
            </h1>
            <p className="text-[10px] text-slate-400 font-mono">
              {t('ud_last_login', { date: user?.last_login ? formatDate(user.last_login) : 'Today' })}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {/* বাংলা মন্তব্য: সার্ভার স্ট্যাটাস টেস্ট করার জন্য core-status data-testid ব্যবহার করা হলো */}
          <span data-testid="core-status" className={`text-xs font-bold ${isServerOnline ? 'text-neon-blue' : 'text-danger'}`}>
            CORE: {isServerOnline ? 'ONLINE' : 'OFFLINE'}
          </span>
          <span className="text-[10px] text-slate-400 font-mono">
            GATE: {deployGate?.status || 'SYNCING...'}
          </span>
          <button
            onClick={toggleTheme}
            className="text-xs font-bold text-neon-blue hover:text-accent-primary tracking-wider transition-colors px-3 py-1.5 rounded border border-border-accent"
          >
            {theme === 'dark' ? '☀️ Light' : '🌙 Dark'}
          </button>
        </div>
      </header>

      <div className="flex gap-2 px-6 mb-4 flex-wrap">
        {/* বাংলা মন্তব্য: টেস্টে নির্দিষ্ট ট্যাবে ক্লিক করার জন্য tab-* ডায়নামিক data-testid দেওয়া হলো */}
        {(['overview', 'feed', 'presets', 'chat', 'browser', 'mobile', 'analytics', 'team', 'security'] as const).map((tab) => (
          <button
            key={tab}
            data-testid={`tab-${tab}`}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 text-xs font-bold tracking-wider rounded-lg transition-all ${activeTab === tab
                ? 'bg-accent-primary/20 text-neon-blue border border-border-accent shadow-[0_0_15px_rgba(0,243,255,0.1)]'
                : 'text-text-secondary hover:text-foreground border border-transparent hover:border-border-accent'
              }`}
          >
            {tab === 'overview' && <><Activity size={10} className="inline mr-1" /> Overview</>}
            {tab === 'feed' && <><Sparkles size={10} className="inline mr-1" /> Home Feed</>}
            {tab === 'presets' && <><Play size={10} className="inline mr-1" /> Quick Presets</>}
            {tab === 'chat' && <><MessageSquare size={10} className="inline mr-1" /> Chat</>}
            {tab === 'browser' && <><Globe size={10} className="inline mr-1" /> Browser Preview</>}
            {tab === 'mobile' && <><Smartphone size={10} className="inline mr-1" /> Mobile Simulator</>}
            {tab === 'analytics' && <><BarChart3 size={10} className="inline mr-1" /> Analytics</>}
            {tab === 'team' && <><Users size={10} className="inline mr-1" /> Team</>}
            {tab === 'security' && <><Shield size={10} className="inline mr-1" /> Security</>}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <div className="px-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
            <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <div className="flex items-center justify-between mb-3">
                <div className="p-3 bg-indigo-500/20 rounded-lg">
                  <FolderOpen size={20} className="text-indigo-400" />
                </div>
                <span className="text-xs font-mono font-bold text-indigo-400">{projects.length} Active</span>
              </div>
              <p className="text-2xl font-bold text-white mb-1">{projects.length}</p>
              <p className="text-xs text-slate-400 font-mono uppercase tracking-widest">Projects</p>
              <div className="mt-3 h-2 bg-slate-700 rounded-full overflow-hidden">
                <div className="h-full bg-indigo-500 rounded-full" style={{ width: `${Math.min(projects.length * 10, 100)}%` }}></div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <div className="flex items-center justify-between mb-3">
                <div className="p-3 bg-emerald-500/20 rounded-lg">
                  <MessageSquare size={20} className="text-emerald-400" />
                </div>
                <span className="text-xs font-mono font-bold text-emerald-400">Live</span>
              </div>
              <p className="text-2xl font-bold text-white mb-1">{chatHistory.length + customerMessages.length}</p>
              <p className="text-xs text-slate-400 font-mono uppercase tracking-widest">Messages</p>
              <div className="mt-3 h-2 bg-slate-700 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-500 rounded-full" style={{ width: '75%' }}></div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <div className="flex items-center justify-between mb-3">
                <div className="p-3 bg-amber-500/20 rounded-lg">
                  <Zap size={20} className="text-amber-400" />
                </div>
                <span className="text-xs font-mono font-bold text-amber-400">Ready</span>
              </div>
              <p className="text-2xl font-bold text-white mb-1">{widgets.length}</p>
              <p className="text-xs text-slate-400 font-mono uppercase tracking-widest">Widgets</p>
              <div className="mt-3 h-2 bg-slate-700 rounded-full overflow-hidden">
                <div className="h-full bg-amber-500 rounded-full" style={{ width: `${Math.min(widgets.length * 20, 100)}%` }}></div>
              </div>
            </div>

            <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
              <div className="flex items-center justify-between mb-3">
                <div className="p-3 bg-purple-500/20 rounded-lg">
                  <TrendingUp size={20} className="text-purple-400" />
                </div>
                <span className="text-xs font-mono font-bold text-purple-400">Optimal</span>
              </div>
              <p className="text-2xl font-bold text-white mb-1">98%</p>
              <p className="text-xs text-slate-400 font-mono uppercase tracking-widest">Performance</p>
              <div className="mt-3 h-2 bg-slate-700 rounded-full overflow-hidden">
                <div className="h-full bg-purple-500 rounded-full" style={{ width: '98%' }}></div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg">
              <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-700">
                <h2 className="text-xs font-bold tracking-widest text-neon-blue uppercase flex items-center gap-2">
                  <FolderOpen size={12} /> Your Projects
                </h2>
                <button onClick={() => setActiveTab('feed')} className="text-[10px] text-slate-400 hover:text-neon-blue font-mono transition-colors flex items-center gap-1">
                  View All <ChevronRight size={8} />
                </button>
              </div>

              {projects.length > 0 ? (
                projects.map((project) => (
                  <div key={project.id} className="flex items-center justify-between p-4 rounded-lg bg-slate-800/50 border border-slate-700 mb-3 last:mb-0 hover:bg-slate-700/50 transition-colors">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-accent-primary/10 flex items-center justify-center text-neon-blue">
                        <FolderOpen size={16} />
                      </div>
                      <div>
                        <p className="text-sm font-bold text-white">{project.name}</p>
                        <p className="text-[10px] text-slate-400 font-mono">{formatDate(project.updated_at)}</p>
                      </div>
                    </div>
                    <span className="text-[10px] px-2 py-1 rounded bg-slate-700 text-slate-300 font-mono">{project.settings.default_model}</span>
                  </div>
                ))
              ) : (
                <div className="text-center py-8 text-slate-400 text-xs font-mono">
                  No projects yet. Create your first project to get started.
                </div>
              )}
            </div>

            <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg flex flex-col">
              <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-700">
                <h2 className="text-xs font-bold tracking-widest text-neon-blue uppercase flex items-center gap-2">
                  <Zap size={12} /> Quick Actions
                </h2>
              </div>
              <div className="flex flex-col gap-3 flex-1">
                <button
                  className="w-full flex items-center justify-between p-3 rounded-lg border border-slate-700 hover:bg-slate-700/50 text-left transition-all group"
                  onClick={() => setActiveTab('chat')}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-indigo-500/10 rounded-lg group-hover:bg-indigo-500/20 transition-colors">
                      <MessageSquare size={16} className="text-indigo-400" />
                    </div>
                    <div>
                      <span className="text-sm font-bold text-white">New Chat Session</span>
                    </div>
                  </div>
                  <ChevronRight size={16} className="text-slate-400" />
                </button>
                <button
                  className="w-full flex items-center justify-between p-3 rounded-lg border border-slate-700 hover:bg-slate-700/50 text-left transition-all group"
                  onClick={() => setActiveTab('presets')}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-purple-500/10 rounded-lg group-hover:bg-purple-500/20 transition-colors">
                      <Play size={16} className="text-purple-400" />
                    </div>
                    <div>
                      <span className="text-sm font-bold text-white">Launch Preset</span>
                    </div>
                  </div>
                  <ChevronRight size={16} className="text-slate-400" />
                </button>
                <button
                  className="w-full flex items-center justify-between p-3 rounded-lg border border-slate-700 hover:bg-slate-700/50 text-left transition-all group"
                  onClick={() => setActiveTab('feed')}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-amber-500/10 rounded-lg group-hover:bg-amber-500/20 transition-colors">
                      <Sparkles size={16} className="text-amber-400" />
                    </div>
                    <div>
                      <span className="text-sm font-bold text-white">Home Feed</span>
                    </div>
                  </div>
                  <ChevronRight size={16} className="text-slate-400" />
                </button>
                <button onClick={() => setActiveTab('analytics')} className="w-full flex items-center justify-between p-3 rounded-lg border border-slate-700 hover:bg-slate-700/50 text-left transition-all group">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-slate-700/20 rounded-lg group-hover:bg-slate-600/20 transition-colors">
                      <Settings2 size={16} className="text-slate-400" />
                    </div>
                    <div>
                      <span className="text-sm font-bold text-white">Project Settings</span>
                    </div>
                  </div>
                  <ChevronRight size={16} className="text-slate-400" />
                </button>
              </div>
            </div>
          </div>

          <div className="mt-6 bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg">
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-700">
              <h2 className="text-xs font-bold tracking-widest text-neon-blue uppercase flex items-center gap-2">
                <Activity size={12} /> Recent Activity
              </h2>
              <span className="text-[10px] text-slate-400 font-mono">Last 24 hours</span>
            </div>
            <div className="flex flex-col gap-2">
              {recentChats.length > 0 ? (
                recentChats.map((msg: ChatMessage, idx: number) => (
                  <div key={idx} className="flex items-center gap-3 p-3 rounded-lg bg-slate-800/30 border border-slate-700 text-[10px] font-mono">
                    <Clock size={12} className="text-slate-400" />
                    <span className="text-slate-400">{msg.sender === 'User' ? 'You' : 'AI'}:</span>
                    <span className="text-foreground flex-1 truncate">
                      {msg.text}
                      {msg.action?.label && (
                        <span className="ml-1 text-[9px] text-neon-purple">[{msg.action.icon} {msg.action.label}]</span>
                      )}
                    </span>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-400 font-mono text-center py-4">No recent activity</p>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'feed' && (
        <div className="px-6">
          <HomeFeed />
        </div>
      )}

      {activeTab === 'presets' && (
        <div className="px-6 grid grid-cols-1 lg:grid-cols-2 gap-4">
          <QuickPresets onSelectPreset={setCustomerInput} />
          <Suspense fallback={<div className="h-64 w-full animate-pulse rounded bg-zinc-800/40" />}>
            <CodeEditor code={code} onChange={setCode} />
          </Suspense>
        </div>
      )}

      {activeTab === 'chat' && (
        <div className="px-6 w-full">
          {/* বাংলা মন্তব্য: ইন্টারেক্টিভ চ্যাট ট্যাব (চ্যাট, টার্মিনাল ও ব্রাউজারসহ) এখানে রেন্ডার করা হলো */}
          <InteractiveChatTab
            messages={customerMessages}
            input={customerInput}
            onInputChange={setCustomerInput}
            onSend={handleSendCustomer}
            loading={loading}
            onSaveToProject={onSaveToProject}
            onPreview={onPreview}
          />
        </div>
      )}

      {activeTab === 'browser' && (
        <div className="px-6 w-full">
          {/* বাংলা মন্তব্লা মন্তব্য: ব্রাউজার প্রিভিউ ট্যাব রেন্ডার করা হলো যেখানে কোড এডিটর এর এইচটিএমএল আউটপুট দেখা যাবে */}
          <BrowserPreview html={code} />
        </div>
      )}

      {activeTab === 'mobile' && (
        <div className="px-6 w-full">
          {/* বাংলা মন্তব্য: মোবাইল সিমুলেটর ট্যাব রেন্ডার করা হলো যেখানে কোড এডিটর এর এইচটিএমএল বিভিন্ন ডিভাইসে রেসপনসিভ টেস্ট করা যাবে */}
          <MobileSimulator html={code} />
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="px-6">
          <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg mb-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <BarChart3 className="text-neon-blue" /> Analytics Overview
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <h3 className="text-sm font-bold text-slate-300 mb-2">Active Users</h3>
                <p className="text-2xl font-bold text-emerald-400">1,248</p>
                <p className="text-xs text-slate-400 mt-1">↑ 12% from last week</p>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <h3 className="text-sm font-bold text-slate-300 mb-2">Tasks Completed</h3>
                <p className="text-2xl font-bold text-amber-400">3,562</p>
                <p className="text-xs text-slate-400 mt-1">↑ 8% from last week</p>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <h3 className="text-sm font-bold text-slate-300 mb-2">System Uptime</h3>
                <p className="text-2xl font-bold text-indigo-400">99.98%</p>
                <p className="text-xs text-slate-400 mt-1">↓ 0.02% from last week</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'team' && (
        <div className="px-6">
          <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Users className="text-neon-blue" /> Team Members
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center">
                    <span className="text-indigo-400 font-bold">JD</span>
                  </div>
                  <div>
                    <h3 className="font-bold text-white">John Doe</h3>
                    <p className="text-xs text-slate-400">Developer</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs px-2 py-1 bg-emerald-500/20 text-emerald-400 rounded">Active</span>
                </div>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-purple-500/20 flex items-center justify-center">
                    <span className="text-purple-400 font-bold">AS</span>
                  </div>
                  <div>
                    <h3 className="font-bold text-white">Alice Smith</h3>
                    <p className="text-xs text-slate-400">Designer</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs px-2 py-1 bg-amber-500/20 text-amber-400 rounded">Away</span>
                </div>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-cyan-500/20 flex items-center justify-center">
                    <span className="text-cyan-400 font-bold">MR</span>
                  </div>
                  <div>
                    <h3 className="font-bold text-white">Mike Roberts</h3>
                    <p className="text-xs text-slate-400">Manager</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs px-2 py-1 bg-slate-600/20 text-slate-400 rounded">Offline</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'security' && (
        <div className="px-6">
          <div className="bg-gradient-to-br from-slate-800 to-slate-900 border border-slate-700 rounded-xl p-5 shadow-lg">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Shield className="text-neon-blue" /> Security Status
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-white">Firewall Status</h3>
                  <span className="text-xs px-2 py-1 bg-emerald-500/20 text-emerald-400 rounded">Active</span>
                </div>
                <p className="text-sm text-slate-400">All ports secured, no threats detected</p>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-white">SSL Certificate</h3>
                  <span className="text-xs px-2 py-1 bg-emerald-500/20 text-emerald-400 rounded">Valid</span>
                </div>
                <p className="text-sm text-slate-400">Expires in 89 days</p>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-white">Last Security Scan</h3>
                  <span className="text-xs px-2 py-1 bg-amber-500/20 text-amber-400 rounded">Recent</span>
                </div>
                <p className="text-sm text-slate-400">Completed 2 hours ago</p>
              </div>
              <div className="bg-slate-800/50 p-4 rounded-lg border border-slate-700">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-bold text-white">Active Sessions</h3>
                  <span className="text-xs px-2 py-1 bg-slate-600/20 text-slate-400 rounded">2</span>
                </div>
                <p className="text-sm text-slate-400">Current user sessions</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
