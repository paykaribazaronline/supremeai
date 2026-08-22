// SupremeAI Unified Command Registry
// বাংলা মন্তব্য: একটাই command source-of-truth — Admin ও User দুই portal-ই এখান থেকে palette পায়।
// CommandBar এই registry consume করে; নতুন command যোগ করতে হলে শুধু এখানে entry দিন।

import {
  Terminal,
  Folder,
  Cpu,
  Shield,
  Zap,
  Sparkles,
  Layers,
  CreditCard,
  User,
  Plug,
  LayoutDashboard,
  Bell,
  HardDrive,
  Server,
  GitMerge,
  BarChart3,
  Settings,
  Users,
} from 'lucide-react';
import type { ElementType } from 'react';

export type CommandCategory = 'Navigation' | 'Actions' | 'AI Models' | 'System';
export type PortalType = 'user' | 'admin';

export interface CommandDefinition {
  id: string;
  title: string;
  category: CommandCategory;
  icon: ElementType;
  /** Declarative route — CommandBar navigate() করবে। action না থাকলে required। */
  route?: string;
  /** Non-route custom action (self-heal trigger ইত্যাদি) */
  action?: () => void;
  shortcut?: string;
  /** কোন কোন portal-এ এই command দেখা যাবে */
  portals: PortalType[];
}

export const COMMAND_REGISTRY: CommandDefinition[] = [
  // ─── Navigation: shared ───────────────────────────────────────────
  {
    id: 'nav-workspace',
    title: 'User Workspace & Dashboard',
    category: 'Navigation',
    icon: LayoutDashboard,
    route: '/workspace',
    shortcut: 'Shift+W',
    portals: ['user', 'admin'],
  },
  {
    id: 'nav-admin',
    title: 'Admin Console (God Mode)',
    category: 'Navigation',
    icon: Shield,
    route: '/admin',
    shortcut: 'Shift+M',
    portals: ['admin'],
  },

  // ─── Navigation: user workspaces ─────────────────────────────────
  {
    id: 'nav-agent',
    title: 'Agent Studio Workspace',
    category: 'Navigation',
    icon: Terminal,
    route: '/workspace/agent',
    shortcut: 'Shift+A',
    portals: ['user'],
  },
  {
    id: 'nav-ide',
    title: 'Cloud IDE Workspace',
    category: 'Navigation',
    icon: Folder,
    route: '/workspace/ide',
    shortcut: 'Shift+I',
    portals: ['user'],
  },
  {
    id: 'nav-swarm',
    title: 'Swarm Telemetry & Heatmap',
    category: 'Navigation',
    icon: Cpu,
    route: '/swarm',
    shortcut: 'Shift+S',
    portals: ['user'],
  },
  {
    id: 'nav-evolution',
    title: 'Evolution Forge & Genetic Tuning',
    category: 'Navigation',
    icon: Sparkles,
    route: '/evolution-forge',
    shortcut: 'Shift+E',
    portals: ['user'],
  },
  {
    id: 'nav-architect',
    title: 'Architect Tower',
    category: 'Navigation',
    icon: Shield,
    route: '/architect-tower',
    portals: ['user'],
  },
  {
    id: 'nav-skills',
    title: 'Skills Catalog & Marketplace',
    category: 'Navigation',
    icon: Sparkles,
    route: '/skills-catalog',
    shortcut: 'Shift+K',
    portals: ['user'],
  },
  {
    id: 'nav-integrations',
    title: 'Cloud Integrations & Vault',
    category: 'Navigation',
    icon: Plug,
    route: '/integrations',
    portals: ['user'],
  },
  {
    id: 'nav-billing',
    title: 'Billing & Token Usage',
    category: 'Navigation',
    icon: CreditCard,
    route: '/billing',
    portals: ['user'],
  },
  {
    id: 'nav-profile',
    title: 'User Profile & Security',
    category: 'Navigation',
    icon: User,
    route: '/profile',
    portals: ['user'],
  },

  // ─── Actions ──────────────────────────────────────────────────────
  {
    id: 'action-heal',
    title: 'Trigger Autonomous Self-Healer',
    category: 'Actions',
    icon: Zap,
    shortcut: 'Ctrl+H',
    action: () => { console.warn('Self healer triggered'); },
    portals: ['user', 'admin'],
  },
  {
    id: 'action-gap',
    title: 'Run Gap Finder Codebase Audit',
    category: 'Actions',
    icon: Shield,
    action: () => { console.warn('Gap finder triggered'); },
    portals: ['admin'],
  },
  {
    id: 'action-distill',
    title: 'Inject Multi-Model Knowledge Vector',
    category: 'Actions',
    icon: Layers,
    action: () => { console.warn('Knowledge injection triggered'); },
    portals: ['admin'],
  },

  // ─── AI Models ────────────────────────────────────────────────────
  {
    id: 'model-deepseek',
    title: 'Switch to SupremeAI Deep (Coding Expert)',
    category: 'AI Models',
    icon: Cpu,
    action: () => { console.warn('Switched to SupremeAI Deep'); },
    portals: ['user', 'admin'],
  },
  {
    id: 'model-kimi',
    title: 'Switch to SupremeAI Reason (Bangla & Reasoning)',
    category: 'AI Models',
    icon: Sparkles,
    action: () => { console.warn('Switched to SupremeAI Reason'); },
    portals: ['user', 'admin'],
  },

  // ─── Admin Console Modules (God Mode subtabs) ─────────────────────
  ...([
    ['admin-nav-dashboard', 'Admin: Dashboard Overview', LayoutDashboard],
    ['admin-nav-alerts', 'Admin: System Alerts & Diagnostics', Bell],
    ['admin-nav-interactive-chat', 'Admin: Interactive Chat (Browser & Terminal)', Terminal],
    ['admin-nav-command-center', 'Admin: SupremeAI Nexus (Canvas)', Layers],
    ['admin-nav-logs', 'Admin: Real-time Logs', Terminal],
    ['admin-nav-costs', 'Admin: Cost Auditor', CreditCard],
    ['admin-nav-health', 'Admin: Health Map', Zap],
    ['admin-nav-users', 'Admin: User Manager / Agents', Users],
    ['admin-nav-config', 'Admin: Config Editor', Settings],
    ['admin-nav-model-router', 'Admin: Model Router', Cpu],
    ['admin-nav-skills', 'Admin: Skill Marketplace', Sparkles],
    ['admin-nav-memory', 'Admin: Memory Browser', HardDrive],
    ['admin-nav-cloud', 'Admin: Cloud Orchestrator', Server],
    ['admin-nav-observability', 'Admin: Observability', BarChart3],
    ['admin-nav-threats', 'Admin: Threat Detection', Shield],
    ['admin-nav-rules', 'Admin: Rules Builder', Settings],
    ['admin-nav-cicd', 'Admin: CI/CD Pipelines', GitMerge],
    ['admin-nav-github', 'Admin: GitHub Integration', GitMerge],
    ['admin-nav-backups', 'Admin: Backup & Restore', HardDrive],
    ['admin-nav-rate-limits', 'Admin: Rate Limits', Zap],
    ['admin-nav-security-dashboard', 'Admin: Security & Memory Dashboard', Shield],
  ] as Array<[string, string, ElementType]>).map(([id, title, icon]) => ({
    id,
    title,
    category: 'System' as CommandCategory,
    icon,
    action: () => dispatchAdminSubtab(id.replace('admin-nav-', '')),
    portals: ['admin'] as PortalType[],
  })),
];

/** বাংলা মন্তব্য: Admin console-এর ভেতরের subtab navigation-এর জন্য shared event */
export const ADMIN_SUBTAB_EVENT = 'supremeai-admin-subtab';

export function dispatchAdminSubtab(tabId: string): void {
  window.dispatchEvent(new CustomEvent(ADMIN_SUBTAB_EVENT, { detail: tabId }));
}

/**
 * বাংলা মন্তব্য: নির্দিষ্ট portal-এর জন্য filtered command list।
 * CommandBar ও ভবিষ্যৎের অন্য consumer-রা এটাই ব্যবহার করবে।
 */
export function getCommandsForPortal(portal: PortalType): CommandDefinition[] {
  return COMMAND_REGISTRY.filter((cmd) => cmd.portals.includes(portal));
}

/** Runtime portal detect — VITE_PORTAL_TYPE env (user/admin) */
export function getCurrentPortal(): PortalType {
  return import.meta.env.VITE_PORTAL_TYPE === 'admin' ? 'admin' : 'user';
}