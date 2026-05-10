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
  type: 'info' | 'warning' | 'error' | 'success' | 'system';
  timestamp: Date;
  message: string;
  source: string;
  payload?: Record<string, any>;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: number;
}

export type UserRole = 'admin' | 'user';

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

export interface Provider {
  id: string;
  name: string;
  type: string;
  enabled: boolean;
  config?: Record<string, any>;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  updatedAt: string;
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
    systemHealthStatus: 'healthy' | 'warning' | 'critical';
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
    userHistory?: Array<{t: string, active: number, total: number}>;
    projectHistory?: Array<{t: string, running: number, completed: number}>;
  };
  navigation: NavItem[];
  components: DashboardComponentDescriptor[];
  apiEndpoints: Record<string, any>;
}

export interface SystemHealthNode {
  id: string;
  name: string;
  type: 'PROVIDER' | 'AGENT' | 'DATABASE' | 'NETWORK';
  status: 'online' | 'busy' | 'error' | 'standby';
  latency: number;
  load: number;
  lastSeen: string;
}
