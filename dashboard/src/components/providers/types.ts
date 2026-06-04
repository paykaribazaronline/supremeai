export interface Provider {
  id?: string;
  name: string;
  type: string;
  baseUrl: string;
  apiKey?: string;
  status: "active" | "inactive" | "error" | "rotating" | "dead";
  models?: string[];
  priority?: number;
  assignedRoles?: string[];
  createdAt?: string;
  hints?: string;
  deploymentSource?: "api" | "gcloud" | "local" | "ollama" | string;
}

export interface ProviderHealthStats {
  total: number;
  active: number;
  inactive: number;
  error: number;
  rotating: number;
  dead: number;
  healthScore: number;
  avgLatency?: number;
}
