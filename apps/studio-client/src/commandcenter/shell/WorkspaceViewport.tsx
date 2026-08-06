import React, { Suspense, lazy } from 'react';
import { useCommandCenterStore } from '../state/useCommandCenterStore';
import { EmptyState } from '../kit';

// ═══════════════════════════════════════════════════════════════════════════
// AETHEL Command Center — Workspace Viewport
// বাংলা মন্তব্য: অ্যাক্টিভ মডিউল ভিউপোর্ট — React.lazy code-splitting
// ═══════════════════════════════════════════════════════════════════════════

// Lazy-loaded module components (code-splitting per module with defensive fallback)
const CommandDeck = lazy(() => import('../modules/deck/CommandDeck').then(m => ({ default: m.default || m.CommandDeck })));
const LiveMetrics = lazy(() => import('../modules/observe/LiveMetrics').then(m => ({ default: m.default || m.LiveMetrics })));
const LiveLogs = lazy(() => import('../modules/observe/LiveLogs').then(m => ({ default: m.default || m.LiveLogs })));
const EventsExplorer = lazy(() => import('../modules/observe/EventsExplorer').then(m => ({ default: m.default || m.EventsExplorer })));
const CICDPipelines = lazy(() => import('../modules/observe/CICDPipelines').then(m => ({ default: m.default || m.CICDPipelines })));
const HealthMap = lazy(() => import('../modules/observe/HealthMap').then(m => ({ default: m.default || m.HealthMap })));
const TrafficMonitor = lazy(() => import('../modules/observe/TrafficMonitor').then(m => ({ default: m.default || m.TrafficMonitor })));
const Agents = lazy(() => import('../modules/operate/Agents').then(m => ({ default: m.default || m.Agents })));
const Swarm = lazy(() => import('../modules/operate/Swarm').then(m => ({ default: m.default || m.Swarm })));
const TasksQueues = lazy(() => import('../modules/operate/TasksQueues').then(m => ({ default: m.default || m.TasksQueues })));
const Sessions = lazy(() => import('../modules/operate/Sessions').then(m => ({ default: m.default || m.Sessions })));
const TenantsUsers = lazy(() => import('../modules/operate/TenantsUsers').then(m => ({ default: m.default || m.TenantsUsers })));
const ModelRouter = lazy(() => import('../modules/build/ModelRouter').then(m => ({ default: m.default || m.ModelRouter })));
const Providers = lazy(() => import('../modules/build/Providers').then(m => ({ default: m.default || m.Providers })));
const Skills = lazy(() => import('../modules/build/Skills').then(m => ({ default: m.default || m.Skills })));
const MemoryKnowledge = lazy(() => import('../modules/build/MemoryKnowledge').then(m => ({ default: m.default || m.MemoryKnowledge })));
const Threats = lazy(() => import('../modules/secure/Threats').then(m => ({ default: m.default || m.Threats })));
const AuditExplorer = lazy(() => import('../modules/secure/AuditExplorer').then(m => ({ default: m.default || m.AuditExplorer })));
const ApprovalQueue = lazy(() => import('../modules/secure/ApprovalQueue').then(m => ({ default: m.default || m.ApprovalQueue })));
const RulesPolicy = lazy(() => import('../modules/secure/RulesPolicy').then(m => ({ default: m.default || m.RulesPolicy })));
const SecretsHealth = lazy(() => import('../modules/secure/SecretsHealth').then(m => ({ default: m.default || m.SecretsHealth })));
const RateLimits = lazy(() => import('../modules/secure/RateLimits').then(m => ({ default: m.default || m.RateLimits })));
const CostAuditor = lazy(() => import('../modules/money/CostAuditor').then(m => ({ default: m.default || m.CostAuditor })));
const UsageBilling = lazy(() => import('../modules/money/UsageBilling').then(m => ({ default: m.default || m.UsageBilling })));
const ROISavings = lazy(() => import('../modules/money/ROISavings').then(m => ({ default: m.default || m.ROISavings })));
const ConfigEditor = lazy(() => import('../modules/system/ConfigEditor').then(m => ({ default: m.default || m.ConfigEditor })));
const FeatureFlags = lazy(() => import('../modules/system/FeatureFlags').then(m => ({ default: m.default || m.FeatureFlags })));
const Workspaces = lazy(() => import('../modules/system/Workspaces').then(m => ({ default: m.default || m.Workspaces })));
const Backups = lazy(() => import('../modules/system/Backups').then(m => ({ default: m.default || m.Backups })));
const DeployGate = lazy(() => import('../modules/system/DeployGate').then(m => ({ default: m.default || m.DeployGate })));

const MODULE_MAP: Record<string, React.LazyExoticComponent<React.ComponentType>> = {
  deck: CommandDeck,
  metrics: LiveMetrics,
  logs: LiveLogs,
  events: EventsExplorer,
  ci: CICDPipelines,
  health: HealthMap,
  traffic: TrafficMonitor,
  agents: Agents,
  swarm: Swarm,
  tasks: TasksQueues,
  sessions: Sessions,
  tenants: TenantsUsers,
  router: ModelRouter,
  providers: Providers,
  skills: Skills,
  memory: MemoryKnowledge,
  threats: Threats,
  audit: AuditExplorer,
  approvals: ApprovalQueue,
  rules: RulesPolicy,
  secrets: SecretsHealth,
  ratelimits: RateLimits,
  cost: CostAuditor,
  usage: UsageBilling,
  roi: ROISavings,
  config: ConfigEditor,
  flags: FeatureFlags,
  workspaces: Workspaces,
  backups: Backups,
  deploy: DeployGate,
};

export function WorkspaceViewport() {
  const { activeModule } = useCommandCenterStore();
  const ModuleComponent = MODULE_MAP[activeModule];

  return (
    <main className="flex-1 overflow-y-auto p-4">
      <Suspense
        fallback={
          <div className="flex items-center justify-center h-full">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-[#00f3ff]/30 border-t-[#00f3ff]" />
          </div>
        }
      >
        {ModuleComponent ? (
          <ModuleComponent />
        ) : (
          <EmptyState title="মডিউল পাওয়া যায়নি" message="এই মডিউলটি এখনো তৈরি হয়নি।" />
        )}
      </Suspense>
    </main>
  );
}