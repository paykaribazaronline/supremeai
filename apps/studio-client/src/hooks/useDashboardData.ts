import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/apiClient';

export interface MetricsData {
  latency_p50_ms: number;
  latency_p95_ms: number;
  latency_p99_ms: number;
  error_rate: number;
  requests_per_second: number;
  total_requests_24h: number;
  cost_per_hour: number;
  cost_projected_monthly: number;
  active_providers: string[];
  model_call_distribution: Record<string, number>;
}

export interface CostReport {
  report: string;
}

export interface HealthMapData {
  gcp: { region: string; status: string; latency?: number };
  railway: { region: string; status: string; latency?: number };
  render: { region: string; status: string; latency?: number };
}

export interface CIReport {
  id: string;
  status: 'success' | 'failure' | 'failed' | 'running';
  message: string;
  commit_message?: string;
  branch?: string;
  created_at?: number;
}

export interface ThreatScanResult {
  scan_time: string;
  findings: Array<{
    id: string;
    severity: 'critical' | 'high' | 'medium' | 'low';
    title: string;
    description: string;
  }>;
  total_findings: number;
}

export function useMetrics(intervalMs = 30000) {
  return useQuery({
    queryKey: ['dashboard', 'metrics'],
    queryFn: () => apiClient.get<MetricsData>('/admin-api/metrics'),
    refetchInterval: intervalMs,
  });
}

export function useCostReport(intervalMs = 60000) {
  return useQuery({
    queryKey: ['dashboard', 'costs'],
    queryFn: () => apiClient.get<CostReport>('/admin-api/costs'),
    refetchInterval: intervalMs,
  });
}

export function useHealthMap(intervalMs = 30000) {
  return useQuery({
    queryKey: ['dashboard', 'health'],
    queryFn: () => apiClient.get<HealthMapData>('/admin-api/health-map'),
    refetchInterval: intervalMs,
  });
}

export function useCIReports(limit = 5, intervalMs = 15000) {
  return useQuery({
    queryKey: ['dashboard', 'ci-logs', limit],
    queryFn: () => apiClient.get<CIReport[]>(`/admin-api/ci-logs?limit=${limit}`),
    refetchInterval: intervalMs,
  });
}

export function useThreatScan() {
  return useQuery({
    queryKey: ['dashboard', 'security-scan'],
    queryFn: () => apiClient.get<ThreatScanResult>('/admin-api/security-scan'),
    refetchInterval: 30000,
  });
}

export function useDeploy() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiClient.post<{ message: string }>('/admin-api/deploy', {}),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['dashboard'] }),
  });
}

export function useUpdateRules() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (rules: Record<string, unknown>) =>
      apiClient.post<{ message: string }>('/admin-api/rules', rules),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['dashboard'] }),
  });
}
