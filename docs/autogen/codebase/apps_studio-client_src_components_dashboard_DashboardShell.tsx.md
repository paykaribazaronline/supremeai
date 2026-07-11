# 📄 ফাইল: apps/studio-client/src/components/dashboard/DashboardShell.tsx

**প্রকার:** .tsx  
**সাইজ:** 8,721 বাইট  
**আপডেট:** 2026-07-11T11:29:21.285396

---

## কোড

```tsx
// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল — বাম সাইডবার নেভিগেশন সহ ইউজার ও অ্যাডমিন উভয়ের জন্য মূল লেআউট
// হ্যাশ-ভিত্তিক রাউটিং, Sujon ব্যাকগ্রাউন্ড ইন্টিগ্রেশন ও পেজ রেন্ডারিং
import { type ReactNode, useMemo } from 'react';
import {
  LayoutList,
  Boxes,
  BookOpen,
  KeyRound,
  BarChart3,
  Settings,
  Vault,
  ListChecks,
  Table2,
  Cpu,
  Shield,
  Wifi,
  WifiOff,
  Activity,
} from 'lucide-react';
import { useHashRoute, type DashboardRoute, parseHash } from './useHashRoute';
import { SessionsPage } from './SessionsPage';
import { SessionDetailPage } from './SessionDetailPage';
import { KnowledgePage } from './KnowledgePage';
import { SecretsPage } from './SecretsPage';
import { UsagePage } from './UsagePage';
import { SettingsPage } from './SettingsPage';
import { VaultPage } from './VaultPage';
import { AutomationQueuePage } from './AutomationQueuePage';
import { SiteActionsPage } from './SiteActionsPage';
import { LlmGatewayPage } from './LlmGatewayPage';
import { LiveSujonBackground } from '../LiveSujonBackground';
import { setSujonState, type SujonState } from '../sujon-utils';
import { MockSwarmProvider } from '../../providers/MockSwarmProvider';
import { SwarmHealthDashboard } from '../swarm/SwarmHealthDashboard';

interface NavItem {
  id: DashboardRoute;
  label: string;
  icon: ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  { id: 'sessions', label: 'Sessions', icon: <LayoutList size={15} /> },
  { id: 'workspace', label: 'Workspace', icon: <Boxes size={15} /> },
  { id: 'vault', label: 'Auth Vault', icon: <Vault size={15} /> },
  { id: 'automation', label: 'Automation', icon: <ListChecks size={15} /> },
  { id: 'knowledge', label: 'Knowledge', icon: <BookOpen size={15} /> },
  { id: 'secrets', label: 'Secrets', icon: <KeyRound size={15} /> },
  { id: 'usage', label: 'Usage', icon: <BarChart3 size={15} /> },
  { id: 'settings', label: 'Settings', icon: <Settings size={15} /> },
];

// বাংলা মন্তব্য: সুপার-অ্যাডমিন কন্ট্রোল লেয়ার — সাইট অ্যাকশন রেজিস্ট্রি ও LLM গেটওয়ে
const ADMIN_NAV_ITEMS: NavItem[] = [
  { id: 'site-actions', label: 'Site Actions', icon: <Table2 size={15} /> },
  { id: 'llm-gateway', label: 'LLM Gateway', icon: <Cpu size={15} /> },
  { id: 'swarm-health', label: 'Swarm Health', icon: <Activity size={15} /> },
  { id: 'admin', label: 'Admin Console', icon: <Shield size={15} /> },
];

interface DashboardShellProps {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
  isServerOnline: boolean;
  // বাংলা মন্তব্য: লিগ্যাসি SupremeAI ওয়ার্কস্পেস (চ্যাট, প্রিসেট, ব্রাউজার প্রিভিউ ইত্যাদি) Workspace ট্যাবে রেন্ডার হয়
  workspace: ReactNode;
}

export function DashboardShell(props: DashboardShellProps) {
  const [route, navigate] = useHashRoute();

  // বাংলা মন্তব্য: রাউটের ভিত্তিতে Sujon স্টেট সেট করা — টাস্ক এক্সিকিউশন আরম্ভ হলে processing, সেশন শেষে idle
  useMemo(() => {
    const sujonState: Record<DashboardRoute, SujonState> = {
      sessions: 'idle',
      session: 'processing',
      workspace: 'idle',
      vault: 'idle',
      automation: 'processing',
      'site-actions': 'idle',
      'llm-gateway': 'idle',
      'swarm-health': 'idle',
      knowledge: 'idle',
      secrets: 'idle',
      usage: 'idle',
      settings: 'idle',
      admin: 'idle',
    };
    setSujonState(sujonState[route.page] || 'idle');
  }, [route.page]);

  const handleOpenSession = (id: string) => {
    navigate('session', id);
  };

  // বাংলা মন্তব্য: হ্যাশ রাউটের ভিত্তিতে সংশ্লিষ্ট পেজ রেন্ডার করা হয়
  const renderPage = () => {
    switch (route.page) {
      case 'session':
        return <SessionDetailPage sessionId={route.param || ''} />;
      case 'workspace':
        return <>{props.workspace}</>;
      case 'vault':
        return <VaultPage />;
      case 'automation':
        return <AutomationQueuePage />;
      case 'site-actions':
        return <SiteActionsPage />;
      case 'llm-gateway':
        return <LlmGatewayPage />;
      case 'swarm-health':
        return <SwarmHealthDashboard />;
      case 'knowledge':
        return <KnowledgePage />;
      case 'secrets':
        return <SecretsPage />;
      case 'usage':
        return <UsagePage />;
      case 'settings':
        return <SettingsPage />;
      case 'admin':
        // বাংলা মন্তব্য: অ্যাডমিন কনসোলের জন্য #/admin রুট
        return <div className="p-6 text-text-secondary text-xs">Admin console (use /admin subdomain)</div>;
      case 'sessions':
      default:
        return <SessionsPage onOpenSession={handleOpenSession} />;
    }
  };

  const navItems = [...NAV_ITEMS, ...ADMIN_NAV_ITEMS];

  return (
    <MockSwarmProvider>
      <div className="relative min-h-screen flex bg-background text-foreground">
        {/* বাংলা মন্তব্য: Sujon অ্যাম্বিয়েন্ট ব্যাকগ্রাউন্ড */}
        <LiveSujonBackground />

        {/* বাংলা মন্তব্য: বাম প্যানেল ব্যাকগ্রাউন্ড গ্রেডিয়েন্ট */}
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-background to-card-bg opacity-80" />

      {/* সাইডবার */}
      <aside
        data-testid="dashboard-sidebar"
        className="relative z-10 w-56 shrink-0 border-r border-border-accent bg-card-bg flex flex-col"
      >
        {/* হেডার */}
        <div className="flex items-center gap-2 px-4 py-4 border-b border-border-accent">
          <span className="text-neon-blue text-lg">▲</span>
          <span className="text-sm font-semibold tracking-wide">SupremeAI</span>
        </div>

        {/* সাইডবার নেভিগেশন লিংক */}
        <nav className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5">
          {navItems.map((item) => {
            const isActive = route.page === item.id;
            return (
              <button
                key={item.id}
                data-testid={`nav-${item.id}`}
                onClick={() => navigate(item.id)}
                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition-colors text-left ${
                  isActive
                    ? 'bg-accent-primary/20 text-neon-blue border border-neon-blue/20'
                    : 'text-text-secondary hover:text-foreground hover:bg-input-bg'
                }`}
              >
                {item.icon}
                {item.label}
              </button>
            );
          })}
        </nav>

        {/* স্ট্যাটাস ও থিম */}
        <div className="px-3 py-3 border-t border-border-accent space-y-2">
          <div
            data-testid="sidebar-server-status"
            className="flex items-center gap-2 text-[11px]"
          >
            {props.isServerOnline ? (
              <>
                <Wifi size={11} className="text-success" />
                <span className="text-success font-medium">Online</span>
              </>
            ) : (
              <>
                <WifiOff size={11} className="text-danger" />
                <span className="text-danger font-medium">Offline</span>
              </>
            )}
          </div>
          <button
            onClick={props.toggleTheme}
            className="w-full flex items-center gap-2 px-3 py-1.5 rounded-lg text-[11px] text-text-secondary hover:text-foreground hover:bg-input-bg transition-colors"
          >
            <Shield size={11} />
            {props.theme === 'dark' ? 'Dark' : 'Light'} mode
          </button>
        </div>
      </aside>

      {/* মূল কন্টেন্ট এলাকা */}
      <main className="relative z-10 flex-1 min-w-0 overflow-y-auto">
        {renderPage()}
      </main>
    </div>
    </MockSwarmProvider>
  );
}

```