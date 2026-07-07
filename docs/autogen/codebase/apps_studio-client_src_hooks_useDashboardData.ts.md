# 📄 ফাইল: apps/studio-client/src/hooks/useDashboardData.ts

**প্রকার:** .ts  
**সাইজ:** 5,495 বাইট  
**আপডেট:** 2026-07-07T19:14:31.253186

---

## কোড

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../services/apiClient';
import { getAdminToken } from '../services/adminTokenStore';

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

// বাংলা মন্তব্য: টোকেন চেক হেল্পার — টোকেন না থাকলে কোয়েরি enabled=false হবে, 401 স্টর্ম ঠেকাবে
const hasToken = (): boolean => !!getAdminToken();

// বাংলা মন্তব্য: রিফেচ ইন্টারভালগুলো আলাদা আলাদা সময়ে সেট করা হয়েছে যাতে সব কোয়েরি একসাথে ফায়ার না হয়
export function useMetrics(intervalMs = 30000) {
  return useQuery({
    queryKey: ['dashboard', 'metrics'],
    queryFn: () => apiClient.get<MetricsData>('/admin-api/metrics'),
    refetchInterval: (query: any) => query.state.error ? false : intervalMs,
    enabled: hasToken(),
    staleTime: 15_000,
  });
}

export function useCostReport(intervalMs = 60000) {
  return useQuery({
    queryKey: ['dashboard', 'costs'],
    queryFn: () => apiClient.get<CostReport>('/admin-api/costs'),
    refetchInterval: (query: any) => query.state.error ? false : intervalMs,
    enabled: hasToken(),
    staleTime: 30_000,
  });
}

export function useHealthMap(intervalMs = 45000) {
  return useQuery({
    queryKey: ['dashboard', 'health'],
    queryFn: () => apiClient.get<HealthMapData>('/admin-api/health-map'),
    refetchInterval: (query: any) => query.state.error ? false : intervalMs,
    enabled: hasToken(),
    staleTime: 20_000,
  });
}

export function useCIReports(limit = 5, intervalMs = 30000) {
  return useQuery({
    queryKey: ['dashboard', 'ci-logs', limit],
    queryFn: () => apiClient.get<CIReport[]>(`/admin-api/ci-logs?limit=${limit}`),
    refetchInterval: (query: any) => query.state.error ? false : intervalMs,
    enabled: hasToken(),
    staleTime: 15_000,
  });
}

export function useThreatScan() {
  return useQuery({
    queryKey: ['dashboard', 'security-scan'],
    queryFn: () => apiClient.get<ThreatScanResult>('/admin-api/security-scan'),
    // বাংলা মন্তব্য: সিকিউরিটি স্ক্যান কম ঘন ঘন চলবে — ১২০ সেকেন্ড ইন্টারভাল
    refetchInterval: (query: any) => query.state.error ? false : 120000,
    enabled: hasToken(),
    staleTime: 60_000,
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

export interface DashboardEvent {
  timestamp: string;
  level: string;
  message: string;
  source: string;
}

export interface ReportsResponse {
  reports: string[];
}

export interface ReportDetail {
  name: string;
  content: string;
}

// বাংলা মন্তব্য: রিয়েল-টাইম ইভেন্ট ডেটা ফেচ করার জন্য রিয়্যাক্ট কোয়েরি হুক
export function useDashboardEvents(limit = 50, intervalMs = 30000) {
  return useQuery({
    queryKey: ['dashboard', 'events', limit],
    queryFn: () => apiClient.get<DashboardEvent[]>(`/admin-api/events?limit=${limit}`),
    refetchInterval: (query: any) => query.state.error ? false : intervalMs,
    enabled: hasToken(),
    staleTime: 15_000,
  });
}

// বাংলা মন্তব্য: দৈনিক রিপোর্ট ও তার কন্টেন্ট রিট্রিভ করার জন্য রিয়্যাক্ট কোয়েরি হুক
export function useDashboardReports(reportName?: string) {
  return useQuery({
    queryKey: ['dashboard', 'reports', reportName],
    queryFn: () => {
      const url = reportName ? `/admin-api/reports?report_name=${reportName}` : '/admin-api/reports';
      return apiClient.get<any>(url);
    },
    enabled: hasToken(),
    staleTime: 60_000,
  });
}


```