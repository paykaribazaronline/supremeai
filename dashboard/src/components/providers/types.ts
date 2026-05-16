export interface Provider {
  id?: string;
  name: string;
  type: string;
  baseUrl: string;
  apiKey?: string;
  status: 'active' | 'inactive' | 'error';
  models?: string[];
  priority?: number;
  assignedRoles?: string[];
  createdAt?: string;
  hints?: string;
}

export interface ProviderHealthStats {
  active: number;
  error: number;
  dead: number;
  healthScore: number;
  avgLatency?: number;
}
