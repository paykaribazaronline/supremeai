# 📄 ফাইল: apps/studio-client/src/hooks/useDashboardData.ts

**প্রকার:** .ts  
**সাইজ:** 6,016 বাইট  
**আপডেট:** 2026-07-11T17:11:02.748276

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
  cpu_usage_percent?: number;
  gpu_usage_percent?: number;
  memory_usage_percent?: number;
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

// বাংলা মন্তব্য: পোলিং বন্ধ করে SSE-এর মাধ্যমে ডেটা আপডেট করা হবে
export function useMetrics() {
  return useQuery({
    queryKey: ['dashboard', 'metrics'],
    queryFn: () => apiClient.get<MetricsData>('/admin-api/metrics'),
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
  });
}

export function useCostReport() {
  return useQuery({
    queryKey: ['dashboard', 'costs'],
    queryFn: () => apiClient.get<CostReport>('/admin-api/costs'),
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
  });
}

export function useHealthMap() {
  return useQuery({
    queryKey: ['dashboard', 'health'],
    queryFn: () => apiClient.get<HealthMapData>('/admin-api/health-map'),
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
  });
}

export function useCIReports(limit = 5) {
  return useQuery({
    queryKey: ['dashboard', 'ci-logs', limit],
    queryFn: () => apiClient.get<CIReport[]>(`/admin-api/ci-logs?limit=${limit}`),
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
  });
}

export function useThreatScan() {
  return useQuery({
    queryKey: ['dashboard', 'security-scan'],
    queryFn: () => apiClient.get<ThreatScanResult>('/admin-api/security-scan'),
    // বাংলা মন্তব্য: পোলিং বন্ধ করে SSE-এর মাধ্যমে ডেটা আপডেট করা হবে
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
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
export function useDashboardEvents(limit = 50) {
  return useQuery({
    queryKey: ['dashboard', 'events', limit],
    queryFn: () => apiClient.get<DashboardEvent[]>(`/admin-api/events?limit=${limit}`),
    refetchInterval: false,
    enabled: hasToken(),
    staleTime: Infinity,
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

// SSE Listener Hook
import { useEffect } from 'react';

export function useDashboardSSE() {
  const qc = useQueryClient();

  useEffect(() => {
    if (!hasToken()) return;
    const backendUrl = import.meta.env.VITE_API_URL || 'http://localhost:8000';
    const sse = new EventSource(`${backendUrl}/api/dashboard/stream`);

    sse.addEventListener('dashboard_events', (e) => {
      try {
        const data = JSON.parse(e.data);
        qc.setQueryData(['dashboard', 'events', 50], data); // update cache directly
      } catch (err) {
        console.error('Failed to parse dashboard_events:', err);
      }
    });

    sse.addEventListener('metrics_events', (e) => {
      try {
        const data = JSON.parse(e.data);
        qc.setQueryData(['dashboard', 'metrics'], data);
      } catch (err) {
        console.error('Failed to parse metrics_events:', err);
      }
    });

    return () => {
      sse.close();
    };
  }, [qc]);
}


```