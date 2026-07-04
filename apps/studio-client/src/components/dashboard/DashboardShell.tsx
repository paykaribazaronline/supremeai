// বাংলা মন্তব্য: Devin-স্টাইল ড্যাশবোর্ড শেল — বাম সাইডবার নেভিগেশন সহ ইউজার ও অ্যাডমিন উভয়ের জন্য মূল লেআউট
import type { ReactNode } from 'react';
import {
  LayoutList,
  Boxes,
  BookOpen,
  KeyRound,
  BarChart3,
  Settings,
  ShieldCheck,
  Plus,
} from 'lucide-react';
import { useHashRoute, type DashboardRoute } from './useHashRoute';
import { SessionsPage } from './SessionsPage';
import { SessionDetailPage } from './SessionDetailPage';
import { KnowledgePage } from './KnowledgePage';
import { SecretsPage } from './SecretsPage';
import { UsagePage } from './UsagePage';
import { SettingsPage } from './SettingsPage';

interface NavItem {
  id: DashboardRoute;
  label: string;
  icon: ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  { id: 'sessions', label: 'Sessions', icon: <LayoutList size={15} /> },
  { id: 'workspace', label: 'Workspace', icon: <Boxes size={15} /> },
  { id: 'knowledge', label: 'Knowledge', icon: <BookOpen size={15} /> },
  { id: 'secrets', label: 'Secrets', icon: <KeyRound size={15} /> },
  { id: 'usage', label: 'Usage', icon: <BarChart3 size={15} /> },
  { id: 'settings', label: 'Settings', icon: <Settings size={15} /> },
];

interface DashboardShellProps {
  theme: 'dark' | 'light';
  toggleTheme: () => void;
  isServerOnline: boolean;
  // বাংলা মন্তব্য: লিগ্যাসি SupremeAI ওয়ার্কস্পেস (চ্যাট, প্রিসেট, ব্রাউজার প্রিভিউ ইত্যাদি) Workspace ট্যাবে রেন্ডার হয়
  workspace: ReactNode;
}

export function DashboardShell({ theme, toggleTheme, isServerOnline, workspace }: DashboardShellProps) {
  const [route, navigate] = useHashRoute();

  const renderPage = () => {
    switch (route.page) {
      case 'session':
        return (
          <SessionDetailPage
            sessionId={route.param || ''}
            onBack={() => navigate('sessions')}
          />
        );
      case 'workspace':
        return workspace;
      case 'knowledge':
        return <KnowledgePage />;
      case 'secrets':
        return <SecretsPage />;
      case 'usage':
        return <UsagePage />;
      case 'settings':
        return <SettingsPage theme={theme} toggleTheme={toggleTheme} />;
      case 'sessions':
      default:
        return <SessionsPage onOpenSession={(id) => navigate('session', id)} />;
    }
  };

  const activeNav = route.page === 'session' ? 'sessions' : route.page;

  return (
    <div className="min-h-screen flex bg-[#0b0f19] text-white">
      <aside
        data-testid="dashboard-sidebar"
        className="w-56 shrink-0 border-r border-white/[0.06] bg-[#080b13] flex flex-col"
      >
        <div className="flex items-center gap-2 px-4 py-4 border-b border-white/[0.06]">
          <span className="text-blue-400 text-lg">▲</span>
          <span className="text-sm font-semibold tracking-wide">SupremeAI</span>
        </div>

        <button
          data-testid="new-session-nav"
          onClick={() => navigate('sessions')}
          className="mx-3 mt-3 mb-2 flex items-center justify-center gap-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium py-2 transition-colors"
        >
          <Plus size={13} />
          New Session
        </button>

        <nav className="flex-1 px-2 py-1 flex flex-col gap-0.5">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              data-testid={`nav-${item.id}`}
              onClick={() => navigate(item.id)}
              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs transition-colors ${
                activeNav === item.id
                  ? 'bg-white/[0.08] text-white'
                  : 'text-slate-400 hover:text-white hover:bg-white/[0.04]'
              }`}
            >
              {item.icon}
              {item.label}
            </button>
          ))}

          {/* বাংলা মন্তব্য: অ্যাডমিন কনসোল আলাদা রুটে (/admin) — সেখানে TOTP লগইনসহ সম্পূর্ণ অ্যাডমিন ফিচার আছে */}
          <a
            data-testid="nav-admin"
            href="/admin"
            className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs text-slate-400 hover:text-white hover:bg-white/[0.04] transition-colors"
          >
            <ShieldCheck size={15} />
            Admin Console
          </a>
        </nav>

        <div className="px-4 py-3 border-t border-white/[0.06] flex items-center justify-between">
          <span
            data-testid="sidebar-server-status"
            className={`text-[10px] font-medium ${isServerOnline ? 'text-emerald-400' : 'text-rose-400'}`}
          >
            ● {isServerOnline ? 'Online' : 'Offline'}
          </span>
          <span className="text-[10px] text-slate-500">Free plan</span>
        </div>
      </aside>

      <main className="flex-1 min-w-0 overflow-y-auto">{renderPage()}</main>
    </div>
  );
}
