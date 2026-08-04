// apps/studio-client/src/components/dashboard/LivingDashboardShell.tsx
// বাংলা মন্তব্য: Split-Focus Layout — বাম/কেন্দ্র = Chat, নিচে = fluid Action-Dock, ডানে = সবসময়-দৃশ্যমান Magic Window (Live Simulator)
import { type ReactNode } from 'react';
import { DndContext } from '@dnd-kit/core';
import { AnimatePresence, motion } from 'framer-motion';
import { PanelRightOpen, PanelRightClose } from 'lucide-react';
import { useWorkspaceSettings, useEnabledIntegrations } from '../../hooks/useWorkspaceSettings';
import { useDynamicDock } from '../../hooks/useDynamicDock';

import { LivingActionDock } from './LivingActionDock';
import { LiveSimulator } from './LiveSimulator';
import { SidebarSettings } from './SidebarSettings';
import { HITLModal } from './HITLModal';
import { useAuthStore, AuthStatus } from '../../store/authStore';
import { Link } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';

interface LivingDashboardShellProps {
  // বাংলা মন্তব্য: কেন্দ্রীয় চ্যাট ইন্টারফেস — বিদ্যমান OperatorStudio/ChatPanel এখানে ইনজেক্ট হবে
  chatPanel: ReactNode;
  // বাংলা মন্তব্য: ড্র্যাগ করা আইটেমের id → প্রকৃত content/context
  resolveDraggedContent: (draggedId: string) => { content: string; context?: Record<string, unknown> };
  onOpenSession?: (id: string) => void;
}

// বাংলা মন্তব্য: Antigravity backend DAG-তে GitHub wire করেছে, তাই এখন আর এটি unsupported নয়!
const UNSUPPORTED_PLATFORMS: string[] = [];

const SIDEBAR_SPRING = { type: 'spring', stiffness: 320, damping: 32 } as const;

export function LivingDashboardShell({ chatPanel, resolveDraggedContent, onOpenSession }: LivingDashboardShellProps) {
  const isSidebarCollapsed = useWorkspaceSettings((s) => s.isSidebarCollapsed);
  const toggleSidebar = useWorkspaceSettings((s) => s.toggleSidebar);
  const enabledIntegrations = useEnabledIntegrations();
  // বাংলা: আগে `s.isAuthenticated` পড়া হতো যা এই স্টোরে কখনোই ছিল না (সবসময়
  // undefined -> falsy), ফলে ইউজার লগইন থাকলেও ড্যাশবোর্ড তাকে logged-out
  // হিসেবে দেখাতো। status থেকে derive করাই সঠিক।
  const isAuthenticated = useAuthStore((s) => s.status === AuthStatus.LOGGED_IN);

  const { nodeStatus, handleDragEnd, pendingAction, confirmAction, cancelAction } = useDynamicDock({
    resolveContent: resolveDraggedContent,
    unsupportedPlatforms: UNSUPPORTED_PLATFORMS,
  });

  const statuses = Object.values(nodeStatus);
  const magicWindowState = statuses.some((n) => n.status === 'pending')
    ? 'pending'
    : statuses.some((n) => n.status === 'error')
    ? 'error'
    : statuses.some((n) => n.status === 'success')
    ? 'success'
    : 'idle';

  return (
    <DndContext onDragEnd={handleDragEnd}>
      <HITLModal pendingAction={pendingAction} onConfirm={confirmAction} onCancel={cancelAction} />
      <div className="relative min-h-screen w-full flex bg-[var(--supremeai-color-bg-void-dark)] text-foreground overflow-hidden">
        {/* কোল্যাপসিবল লেফট সাইডবার — spring-based width animation, no layout thrash */}
        <motion.aside
          data-testid="living-sidebar"
          initial={false}
          animate={{ width: isSidebarCollapsed ? 64 : 256 }}
          transition={SIDEBAR_SPRING}
          className="shrink-0 border-r border-white/10 bg-[var(--supremeai-color-bg-elevated-dark)] overflow-hidden"
        >
          <button
            onClick={toggleSidebar}
            className="w-full flex items-center justify-center py-4 text-slate-400 hover:text-white transition-colors"
            aria-label="Toggle sidebar"
          >
            {isSidebarCollapsed ? <PanelRightOpen size={18} /> : <PanelRightClose size={18} />}
          </button>
          <AnimatePresence>
            {!isSidebarCollapsed && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.15 }}
                className="px-4 text-xs text-slate-500"
              >
                <SidebarSettings onOpenSession={onOpenSession ?? (() => {})} />
              </motion.div>
            )}
          </AnimatePresence>
        </motion.aside>

        {/* কেন্দ্র: চ্যাট + নিচে Action-Dock */}
        <div className="flex-1 flex flex-col min-w-0 relative">
          {!isAuthenticated && (
            <div className="shrink-0 bg-rose-500/10 border-b border-rose-500/20 px-4 py-2 flex items-center justify-center gap-3 shadow-sm z-10">
              <AlertCircle className="w-4 h-4 text-rose-400" />
              <span className="text-xs font-medium text-slate-300">
                You are exploring in <span className="text-rose-400">Guest Mode</span>.
              </span>
              <div className="h-3 w-px bg-slate-700"></div>
              <Link to="/login" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 hover:underline transition-colors">
                Login / Sign Up
              </Link>
              <span className="text-xs text-slate-500 hidden sm:inline">to save your progress.</span>
            </div>
          )}

          <main className="flex-1 overflow-y-auto pb-32">{chatPanel}</main>

          <AnimatePresence>
            {enabledIntegrations.length > 0 && (
              <LivingActionDock enabledIntegrations={enabledIntegrations} nodeStatus={nodeStatus} />
            )}
          </AnimatePresence>
        </div>

        {/* ডান দিক: সবসময়-দৃশ্যমান Magic Window (Live Simulator) */}
        <aside
          data-testid="magic-window"
          className="hidden lg:flex shrink-0 w-96 border-l border-white/10 bg-[var(--supremeai-color-bg-elevated-dark)] flex-col"
        >
          <div className="px-4 py-3 border-b border-white/10 text-xs font-semibold text-slate-300">
            Live Simulator — Transformation Map
          </div>
          <div className="flex-1 flex items-center justify-center text-xs text-slate-500">
            <LiveSimulator state={magicWindowState} nodeStatus={nodeStatus} enabledIntegrations={enabledIntegrations} />
          </div>
        </aside>
      </div>
    </DndContext>
  );
}
