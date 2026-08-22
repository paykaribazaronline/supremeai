import type { UnifiedChatMessage } from './types/chat';

export type ChatMessage = UnifiedChatMessage;

export const legacyToUnified = (legacy: { sender: string; text: string; timestamp: string }): UnifiedChatMessage => ({
  id: `migrated_${Date.now()}`,
  role: legacy.sender === 'ai' ? 'assistant' : 'user',
  content: legacy.text,
  timestamp: new Date(legacy.timestamp).getTime(),
});

export interface Skill {
  id: string;
  name: string;
  version: string;
  description: string;
  dependencies?: string;
  installed: boolean;
  source: string;
}

export interface Checkpoint {
  task_id: string;
  step_index: number;
  state: Record<string, unknown>;
}

export interface CloudStats {
  distribution: Record<string, unknown>;
  total_requests: number;
  active_providers: number;
  strategy: string;
}

export interface GcpHealth {
  status: string;
  cloud_run: Record<string, unknown>;
  firestore_mode: string;
  pubsub_mode: string;
  cloud_functions: Record<string, unknown>;
}

export interface HealthMap {
  gcp: { status: string; latency: string; region: string };
  railway: { status: string; latency: string; region: string };
  render: { status: string; latency: string; region: string };
}

export interface AdminUser {
  username: string;
  role: string;
  permissions: string[];
}

export interface SystemAlert {
  id: string;
  level: 'info' | 'warning' | 'error' | 'critical';
  message: string;
  resolved: boolean;
  created_at: string;
  resolved_at: string | null;
}

// বাংলা মন্তব্য: অ্যাডমিন সাবট্যাব ইউনিয়নে 'interactive-chat' ও 'alerts' যোগ করা হলো
export type AdminSubTab = 'dashboard' | 'sandbox' | 'logs' | 'costs' | 'health' | 'users' | 'config' | 'command-center' | 'model-router' | 'skills' | 'memory' | 'cloud' | 'observability' | 'threats' | 'rules' | 'cicd' | 'github' | 'backups' | 'rate-limits' | 'security-dashboard' | 'interactive-chat' | 'alerts';

export interface CIReport {
  id: number;
  run_id: number;
  run_number: number;
  event_name: string;
  actor: string;
  workflow_name: string;
  status: string;
  runtime_seconds: number;
  commit_sha: string;
  branch: string;
  jobs_summary: Record<string, unknown> | null;
  error_logs: string | null;
  created_at: number;
}
