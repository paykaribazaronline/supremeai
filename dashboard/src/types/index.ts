/**
 * Core type definitions for SupremeAI Dashboard.
 */

export interface SystemStats {
  activeUsers: number;
  activeAgents: number;
  healthScore: number;
  runningTasks: number;
  completedTasks: number;
  systemLoad: number;
  memoryUsage: number;
  lastUpdate: Date;
}

export interface SystemEvent {
  id: string;
  type: "info" | "warning" | "error" | "success" | "system";
  timestamp: Date;
  message: string;
  source: string;
  payload?: Record<string, unknown>;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: number;
}

export type UserRole = "admin" | "user";

export interface AuthUser {
  id: string;
  uid?: string; // Compatibility with Firebase user objects
  email: string | null;
  displayName?: string | null;
  username?: string | null;
  role: UserRole;
  tier: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: AuthUser;
}

export interface NavItem {
  key: string;
  label: string;
  icon: string;
  description: string;
  enabled: boolean;
}

export interface DashboardComponentDescriptor {
  key: string;
  label: string;
  component: string;
  enabled: boolean;
}

export interface DashboardContract {
  contractVersion: string;
  title: string;
  description: string;
  stats: {
    totalUsers: number;
    activeUsers: number;
    activeAIAgents: number;
    systemHealthScore: number;
    runningTasks: number;
    runningProjects: number;
    completedTasks: number;
    successRate: number;
    systemHealthStatus: "healthy" | "warning" | "critical";
    systemHealthReason: string;
    knowledgeBaseSize: number;
    activeConnections: number;
    totalProviders: number;
    activeProviders: number;
    backendConnected: boolean;
    databaseConnected: boolean;
    lastStartTime: number;
    serverUptime: string;
    lastUpdateAt: number;
    latency?: number;
    systemHealthNodes?: SystemHealthNode[];
    userHistory?: Array<{ t: string; active: number; total: number }>;
    projectHistory?: Array<{ t: string; running: number; completed: number }>;
  };
  navigation: NavItem[];
  components: DashboardComponentDescriptor[];
  apiEndpoints: Record<string, unknown>;
}

export interface SystemHealthNode {
  id: string;
  name: string;
  type: "PROVIDER" | "AGENT" | "DATABASE" | "NETWORK";
  status: "online" | "busy" | "error" | "standby";
  latency: number;
  load: number;
  lastSeen: string;
}

// Code Analysis Types
export interface AnalysisFinding {
  id: string;
  jobId: string;
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO";
  category: string;
  file: string;
  line: number;
  message: string;
  suggestion: string;
  pattern?: string;
  codeSnippet?: string;
}

export interface AnalysisJob {
  id: string;
  projectName: string;
  projectType?: string;
  gitUrl?: string;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED";
  startTime: string;
  endTime?: string;
  durationMs?: number;
  errorMessage?: string;
  filesAnalyzed: number;
  totalFindings: number;
  findingsBySeverity: Record<string, number>;
  completed: boolean;
  initiatedBy?: string;
  findings?: AnalysisFinding[];
}

export interface AnalysisResponse {
  jobId: string;
  status: string;
  durationMs: number;
  project: string;
  filesAnalyzed: number;
  totalFiles?: number;
  totalFindings: number;
  summary: Record<string, number>;
  findings: AnalysisFinding[];
  fixes?: AnalysisFix[];
  completed: boolean;
  errorMessage?: string;
  ragUsed?: boolean;
  incrementalUsed?: boolean;
  changedFiles?: number;
  cachedFindings?: number;
}

export interface AnalysisRequest {
  projectType: string;
  gitUrl?: string;
  branch?: string;
  zipFile?: File;
  includeDependencies?: boolean;
  agents?: Record<string, boolean>;
  maxFiles?: number;
  maxSizeBytes?: number;
  ragEnabled?: boolean;
  incrementalEnabled?: boolean;
  fixesEnabled?: boolean;
  baselineCommit?: string;
  projectId?: string;
}

export interface AgentConfig {
  key: string;
  name: string;
  description: string;
  enabled: boolean;
  category: "SECURITY" | "QUALITY" | "DEPENDENCIES" | "ARCHITECTURE";
}

export interface AnalysisFix {
  id: string;
  jobId: string;
  findingId: string;
  file: string;
  line: number;
  originalCode: string;
  fixedCode: string;
  explanation: string;
  confidence: number;
  applied: boolean;
  createdAt: string;
}
