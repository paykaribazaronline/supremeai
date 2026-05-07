/**
 * Core type definitions for SupremeAI Dashboard.
 */

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

export interface DashboardContract {
  contractVersion: string;
  title: string;
  description: string;
  stats: {
    totalUsers: number;
    activeAIAgents: number;
    systemHealthScore: number;
    runningTasks: number;
    completedTasks: number;
    successRate: number;
    systemHealthStatus: 'healthy' | 'warning' | 'critical';
    systemHealthReason: string;
    knowledgeBaseSize: number;
    activeConnections: number;
  };
  navigation: NavItem[];
}
