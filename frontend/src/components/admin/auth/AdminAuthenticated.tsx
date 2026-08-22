import React, { useEffect } from 'react';
import type { AdminSubTab, GcpHealth, CloudStats, Skill, Checkpoint, ChatMessage, AdminUser, HealthMap } from '../../types';
import { SubTabContent } from './AdminSubTabContent';
import { AdminTopNav } from './AdminTopNav';
import { ADMIN_SUBTAB_EVENT } from '../../config/commandRegistry';
import {
  LayoutDashboard,
  GitMerge,
  Server,
  BarChart3,
  Users,
  Settings,
  Terminal,
  Shield,
  BrainCircuit,
  HardDrive,
  Bell
} from 'lucide-react';

interface AuthenticatedViewProps {
  gcpHealth?: GcpHealth | null;
  cloudStats?: CloudStats | null;
  skillQuery: string;
  setSkillQuery: (val: string) => void;
  skills: Skill[];
  handleInstallSkill: (name: string) => void;
  checkpoints: Checkpoint[];
  handleDeleteCheckpoint: (taskId: string) => void;
  adminSubTab: AdminSubTab;
  setAdminSubTab: (tab: AdminSubTab) => void;
  handleTriggerDeploy: () => void;
  adminMessages: ChatMessage[];
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
  healthMap: HealthMap;
  newUsername: string;
  setNewUsername: (val: string) => void;
  newUserRole: string;
  setNewUserRole: (val: string) => void;
  newUserPerms: string;
  setNewUserPerms: (val: string) => void;
  handleSaveUser?: () => void;
  adminUsers?: AdminUser[];
  envConfig?: Record<string, string>;
  setEnvConfig?: React.Dispatch<React.SetStateAction<Record<string, string>>>;
  handleSaveConfig?: () => void;
  actionStatus: string;
  handleAdminLogout: () => void;
  theme: 'dark' | 'light';
  toggleTheme: () => void;
}

/**
 * Supreme God Mode - Authenticated Layout (Redesigned)
 * This component implements the vision from the SUPREMEAI_GOD_CONTROL_CENTER_PLAN.md,
 * featuring a top navigation bar, a multi-module sidebar, and a main content panel.
 * It also integrates a command palette for quick navigation.
 *
 * বাংলা মন্তব্য: সুপ্রিম গড মোড অথেনটিকেটেড লেআউট (পুনঃডিজাইনকৃত)
 * এই কম্পোনেন্টটি SUPREMEAI_GOD_CONTROL_CENTER_PLAN.md-এর পরিকল্পনাকে বাস্তবায়ন করে।
 * এতে একটি টপ নেভিগেশন বার, একাধিক মডিউলসহ সাইডবার এবং মূল কন্টেন্ট প্যানেল রয়েছে।
 * দ্রুত নেভিগেশনের জন্য একটি কমান্ড প্যালেটও যুক্ত করা হয়েছে।
 */
export function AuthenticatedView(props: AuthenticatedViewProps) {
  const { adminSubTab, setAdminSubTab, handleAdminLogout } = props;

  // বাংলা মন্তব্য: Palette এখন global CommandBar (unified registry) — admin subtab navigation
  // 'supremeai-admin-subtab' custom event-এর মাধ্যমে আসে। Double-palette conflict এড়াতে
  // এখানে আলাদা Ctrl+K handler রাখা হয়নি।
  useEffect(() => {
    const handleSubtabEvent = (e: Event) => {
      const tabId = (e as CustomEvent<string>).detail;
      setAdminSubTab(tabId as AdminSubTab);
    };
    window.addEventListener(ADMIN_SUBTAB_EVENT, handleSubtabEvent);
    return () => window.removeEventListener(ADMIN_SUBTAB_EVENT, handleSubtabEvent);
  }, [setAdminSubTab]);

  // As per SUPREMEAI_GOD_CONTROL_CENTER_PLAN.md, the sidebar is module-driven.
  const sidebarItems = [
    { id: 'dashboard', label: 'DASHBOARD', icon: <LayoutDashboard size={16} /> },
    { id: 'alerts', label: 'SYSTEM ALERTS', icon: <Bell size={16} /> },
    { id: 'model-router', label: 'AI CORE', icon: <BrainCircuit size={16} /> },
    { id: 'skills', label: 'SKILLS & AGENTS', icon: <Users size={16} /> },
    { id: 'memory', label: 'MEMORY', icon: <HardDrive size={16} /> },
    { id: 'cloud', label: 'INFRASTRUCTURE', icon: <Server size={16} /> },
    { id: 'cicd', label: 'DEPLOYMENTS', icon: <GitMerge size={16} /> },
    { id: 'observability', label: 'OBSERVABILITY', icon: <BarChart3 size={16} /> },
    { id: 'threats', label: 'SECURITY', icon: <Shield size={16} /> },
    { id: 'config', label: 'SETTINGS', icon: <Settings size={16} /> },
    { id: 'interactive-chat', label: 'TERMINAL', icon: <Terminal size={16} /> },
  ];

  // বাংলা মন্তব্য: কমান্ড প্যালেট অপশন এখন src/config/commandRegistry.ts-এ (unified registry)।

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* ১. টপ নেভিগেশন বার */}
      <AdminTopNav onLogout={handleAdminLogout} />

      {/* নিচের অংশ: সাইডবার + মূল কন্টেন্ট */}
      <div className="flex-1 flex overflow-hidden relative">

        {/* ২. বাম পাশের নেভিগেশন সাইডবার */}
        <aside className="w-56 bg-[#040814]/55 backdrop-blur-xl border-r border-white/5 flex flex-col justify-between py-6 font-sans text-slate-400 select-none z-20">
          <div className="space-y-1 px-3">
            {sidebarItems.map(item => {
              const isActive = adminSubTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setAdminSubTab(item.id as AdminSubTab)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-xs font-semibold tracking-wider transition-all duration-300 ${isActive
                      ? 'bg-[#00f3ff]/10 text-[#00f3ff] border-l-2 border-[#00f3ff] shadow-[inset_0_0_12px_rgba(0,243,255,0.05)]'
                      : 'hover:bg-slate-900/50 hover:text-slate-200'
                    }`}
                >
                  <span className={isActive ? 'text-[#00f3ff]' : 'text-slate-400'}>
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
              className={`w-full flex items-center justify-center gap-2 px-3 py-2 rounded border border-[#00f3ff]/30 text-[#00f3ff] hover:bg-[#00f3ff]/10 text-xs font-mono font-bold tracking-widest uppercase transition-all duration-300 ${adminSubTab === 'command-center' ? 'bg-[#00f3ff]/20' : ''
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
        <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <SubTabContent {...props} />
        </main>
      </div>
    </div>
  );
}
