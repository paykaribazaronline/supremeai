import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { 
  Activity, Server, Database, Cloud, Wifi, WifiOff, 
  RefreshCw, AlertTriangle, CheckCircle, XCircle, Clock,
  ArrowUp, ArrowDown, Minus, ExternalLink, Bell, BellOff,
  GitBranch, Key, Shield
} from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';

// ══════════════════════════════════════════════════════════════════════════════
// TYPES
// ══════════════════════════════════════════════════════════════════════════════

interface ServiceHealth {
  service: string;
  timestamp: string;
  status: 'healthy' | 'degraded' | 'unhealthy' | 'unknown';
  responseTime: number | null;
  statusCode: number | null;
  error: string | null;
  details?: Record<string, any>;
}

interface GlobalHealthSummary {
  overall: 'healthy' | 'degraded' | 'unhealthy' | 'unknown';
  checkedAt: string;
  totals: {
    healthy: number;
    degraded: number;
    unhealthy: number;
    total: number;
    unknown: number;
  };
  services: Record<string, string>;
  criticalServicesHealthy: boolean;
}

interface ServiceConfig {
  name: string;
  displayName: string;
  url: string;
  description: string;
  critical: boolean;
  icon: React.ReactNode;
}

// ══════════════════════════════════════════════════════════════════════════════
// SERVICE REGISTRY (Must match Cloudflare Worker CONFIG.SERVICES)
// ══════════════════════════════════════════════════════════════════════════════

const SERVICE_REGISTRY: ServiceConfig[] = [
  // ─── CORE INFRASTRUCTURE ──────────────────────────────────────────────
  {
    name: 'render_backend',
    displayName: 'Render Backend',
    url: 'https://supremeai-backend-docker.onrender.com',
    description: 'Python/FastAPI Core API',
    critical: true,
    icon: <Server size={16} />,
    category: 'infrastructure',
  },

  {
    name: 'scraper_service',
    displayName: 'Scraper Service',
    url: 'https://supremeai-scraper-6nwi.onrender.com',
    description: 'Playwright Browser Automation',
    critical: false,
    icon: <Activity size={16} />,
    category: 'infrastructure',
  },

  // ─── DATABASE & AUTH ──────────────────────────────────────────────────
  {
    name: 'supabase_db',
    displayName: 'Supabase DB',
    url: 'https://<project>.supabase.co',
    description: 'PostgreSQL Database & Auth',
    critical: true,
    icon: <Database size={16} />,
    category: 'database',
  },
  {
    name: 'firebase_auth',
    displayName: 'Firebase Auth',
    url: 'https://identitytoolkit.googleapis.com',
    description: 'Authentication Service',
    critical: true,
    icon: <Shield size={16} />,
    category: 'auth',
  },
  {
    name: 'firebase_firestore',
    displayName: 'Firestore',
    url: 'https://firestore.googleapis.com',
    description: 'NoSQL Document Store',
    critical: true,
    icon: <Database size={16} />,
    category: 'database',
  },

  // ─── EDGE & CDN ───────────────────────────────────────────────────────
  {
    name: 'cloudflare_worker',
    displayName: 'Edge Worker',
    url: 'https://supremeai-edge.workers.dev',
    description: 'Cloudflare Edge Proxy',
    critical: true,
    icon: <Cloud size={16} />,
    category: 'edge',
  },

  // ─── CI/CD & REPOSITORY ───────────────────────────────────────────────
  {
    name: 'github_api',
    displayName: 'GitHub API',
    url: 'https://api.github.com',
    description: 'Git Repository & Actions',
    critical: false,
    icon: <GitBranch size={16} />,
    category: 'cicd',
  },
  {
    name: 'vercel_deploy',
    displayName: 'Vercel',
    url: 'https://vercel.com',
    description: 'Frontend Deployment',
    critical: false,
    icon: <Cloud size={16} />,
    category: 'cicd',
  },

  // ─── MONITORING ───────────────────────────────────────────────────────
  {
    name: 'krogger',
    displayName: 'Krogger',
    url: 'https://krogger.io',
    description: 'Uptime Monitoring',
    critical: false,
    icon: <Activity size={16} />,
    category: 'monitoring',
  },

  // ─── SECRETS ──────────────────────────────────────────────────────────
  {
    name: 'infisical',
    displayName: 'Infisical',
    url: 'https://app.infisical.com',
    description: 'Secrets Manager',
    critical: true,
    icon: <Key size={16} />,
    category: 'secrets',
  },
];

// ══════════════════════════════════════════════════════════════════════════════
// API FUNCTIONS
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Fetch global health summary from Cloudflare Worker
 */
async function fetchGlobalHealth(): Promise<GlobalHealthSummary> {
  try {
    // Try Cloudflare Worker health endpoint first
    const response = await fetch('/api/edge/health', {
      headers: { 'Accept': 'application/json' },
    });
    
    if (response.ok) {
      const data = await response.json();
      return data.global || { overall: 'unknown', checkedAt: new Date().toISOString(), totals: { healthy: 0, degraded: 0, unhealthy: 0, total: 0, unknown: 0 }, services: {}, criticalServicesHealthy: false };
    }
  } catch (e) {
    console.warn('[HealthMonitor] CF Worker health failed, trying direct...');
  }

  // Fallback: Direct health checks to each service
  const results = await Promise.allSettled(
    SERVICE_REGISTRY.map(async (service) => {
      const start = Date.now();
      try {
        const res = await fetch(`${service.url}/api/v1/health`, {
          signal: AbortSignal.timeout(8000),
        });
        return {
          service: service.name,
          status: res.ok ? 'healthy' as const : 'unhealthy' as const,
          responseTime: Date.now() - start,
          statusCode: res.status,
        };
      } catch (err) {
        return {
          service: service.name,
          status: 'unhealthy' as const,
          responseTime: Date.now() - start,
          error: err instanceof Error ? err.message : 'Unknown error',
        };
      }
    })
  );

  const serviceResults = results.map(r => 
    r.status === 'fulfilled' ? r.value : { service: 'unknown', status: 'unknown' as const }
  );

  const healthy = serviceResults.filter(s => s.status === 'healthy').length;
  const unhealthy = serviceResults.filter(s => s.status === 'unhealthy').length;

  return {
    overall: unhealthy > 0 ? 'unhealthy' : 'healthy',
    checkedAt: new Date().toISOString(),
    totals: {
      healthy,
      degraded: 0,
      unhealthy,
      total: serviceResults.length,
      unknown: serviceResults.length - healthy - unhealthy,
    },
    services: Object.fromEntries(serviceResults.map(s => [s.service, s.status])),
    criticalServicesHealthy: serviceResults
      .filter(s => SERVICE_REGISTRY.find(reg => reg.name === s.service)?.critical)
      .every(s => s.status === 'healthy'),
  };
}

/**
 * Fetch detailed health for a specific service
 */
async function fetchServiceHealth(serviceName: string): Promise<ServiceHealth | null> {
  try {
    const response = await fetch(`/api/edge/health/detailed?service=${serviceName}`);
    if (response.ok) {
      const data = await response.json();
      return data.services[serviceName] || null;
    }
  } catch (e) {
    console.warn(`[HealthMonitor] Failed to fetch health for ${serviceName}:`, e);
  }
  return null;
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENT
// ══════════════════════════════════════════════════════════════════════════════

interface ServiceHealthMonitorProps {
  autoRefresh?: boolean;       // Auto-refresh interval (seconds)
  showDetails?: boolean;       // Show detailed panel
  compact?: boolean;           // Compact mode for sidebars
  onServiceClick?: (service: ServiceConfig) => void;
  enableAlerts?: boolean;      // Show alert toggle
}

export const ServiceHealthMonitor: React.FC<ServiceHealthMonitorProps> = ({
  autoRefresh = 30,
  showDetails = true,
  compact = false,
  onServiceClick,
  enableAlerts = true,
}) => {
  const queryClient = useQueryClient();
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [alertsEnabled, setAlertsEnabled] = useState(true);
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

  // Global health query
  const { data: globalHealth, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['global-health'],
    queryFn: fetchGlobalHealth,
    refetchInterval: autoRefresh * 1000,
    staleTime: 15000, // Consider stale after 15 seconds
  });

  // Individual service details (lazy loaded)
  const { data: serviceDetail } = useQuery({
    queryKey: ['service-health', selectedService],
    queryFn: () => fetchServiceHealth(selectedService!),
    enabled: !!selectedService && showDetails,
    refetchInterval: autoRefresh * 1000,
  });

  // Manual refresh handler
  const handleRefresh = useCallback(async () => {
    await refetch();
    setLastRefresh(new Date());
    queryClient.invalidateQueries({ queryKey: ['service-health'] });
  }, [refetch, queryClient]);

  // Compute derived values
  const statusColor = useMemo(() => {
    switch (globalHealth?.overall) {
      case 'healthy': return 'text-emerald-400';
      case 'degraded': return 'text-yellow-400';
      case 'unhealthy': return 'text-red-400';
      default: return 'text-gray-400';
    }
  }, [globalHealth?.overall]);

  const statusBg = useMemo(() => {
    switch (globalHealth?.overall) {
      case 'healthy': return 'bg-emerald-500/10 border-emerald-500/30';
      case 'degraded': return 'bg-yellow-500/10 border-yellow-500/30';
      case 'unhealthy': return 'bg-red-500/10 border-red-500/30';
      default: return 'bg-gray-500/10 border-gray-500/30';
    }
  }, [globalHealth?.overall]);

  const StatusIcon = useMemo(() => {
    switch (globalHealth?.overall) {
      case 'healthy': return CheckCircle;
      case 'degraded': return AlertTriangle;
      case 'unhealthy': return XCircle;
      default: return Minus;
    }
  }, [globalHealth?.overall]);

  // Loading skeleton for compact mode
  if (compact && isLoading) {
    return (
      <div className="bg-[var(--bg-panel)] border border-[var(--border-accent)] rounded-xl p-3 animate-pulse">
        <div className="h-4 bg-gray-700 rounded w-32 mb-2" />
        <div className="h-3 bg-gray-700 rounded w-20" />
      </div>
    );
  }

  return (
    <div className={`bg-[var(--bg-panel)] border ${statusBg} rounded-xl shadow-lg backdrop-blur-xl overflow-hidden transition-all duration-500 ${
      compact ? 'w-80 p-3' : 'p-4'
    }`}>
      
      {/* ── Header ── */}
      <div className={`flex items-center justify-between mb-3 ${!compact && 'pb-3 border-b border-[var(--border-accent)]'}`}>
        <div className="flex items-center gap-2">
          <StatusIcon size={compact ? 14 : 18} className={`${statusColor} animate-pulse`} />
          <h3 className={`font-bold uppercase tracking-wider font-mono ${statusColor} ${
            compact ? 'text-xs' : 'text-sm'
          }`}>
            System Health
          </h3>
        </div>
        
        <div className="flex items-center gap-2">
          {/* Alerts Toggle */}
          {enableAlerts && (
            <button
              onClick={() => setAlertsEnabled(!alertsEnabled)}
              className="p-1 hover:bg-[var(--bg-cell)] rounded transition-colors"
              title={alertsEnabled ? 'Disable alerts' : 'Enable alerts'}
            >
              {alertsEnabled ? <Bell size={14} className="text-[var(--accent-primary)]" /> : <BellOff size={14} className="text-[var(--text-secondary)]" />}
            </button>
          )}
          
          {/* Refresh Button */}
          <button
            onClick={handleRefresh}
            disabled={isFetching}
            className={`p-1 hover:bg-[var(--bg-cell)] rounded transition-colors ${isFetching ? 'animate-spin' : ''}`}
            title="Refresh health status"
          >
            <RefreshCw size={14} className={isFetching ? 'text-[var(--accent-primary)]' : 'text-[var(--text-secondary)]'} />
          </button>
        </div>
      </div>

      {/* ── Summary Stats ── */}
      {!compact && globalHealth?.totals && (
        <div className="grid grid-cols-4 gap-2 mb-4">
          {[
            { label: 'Healthy', value: globalHealth.totals.healthy, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
            { label: 'Degraded', value: globalHealth.totals.degraded, color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
            { label: 'Unhealthy', value: globalHealth.totals.unhealthy, color: 'text-red-400', bg: 'bg-red-500/10' },
            { label: 'Total', value: globalHealth.totals.total, color: 'text-[var(--text-main)]', bg: 'bg-[var(--bg-cell)]' },
          ].map((stat) => (
            <div key={stat.label} className={`${stat.bg} rounded-lg p-2 text-center`}>
              <div className={`text-lg font-bold font-mono ${stat.color}`}>{stat.value}</div>
              <div className="text-[9px] text-[var(--text-secondary)] uppercase tracking-wider">{stat.label}</div>
            </div>
          ))}
        </div>
      )}

      {/* ── Service List ── */}
      <div className="space-y-1.5">
        {SERVICE_REGISTRY.map((service) => {
          const serviceStatus = globalHealth?.services?.[service.name] || 'unknown';
          const isSelected = selectedService === service.name;
          
          return (
            <motion.button
              key={service.name}
              onClick={() => {
                setSelectedService(isSelected ? null : service.name);
                onServiceClick?.(service);
              }}
              className={`w-full flex items-center gap-2.5 p-2 rounded-lg transition-all duration-200 ${
                isSelected 
                  ? 'bg-[var(--accent-primary)]/10 border border-[var(--accent-primary)]/30' 
                  : 'hover:bg-[var(--bg-cell)] border border-transparent'
              }`}
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.99 }}
            >
              {/* Icon */}
              <div className={`${
                serviceStatus === 'healthy' ? 'text-emerald-400' :
                serviceStatus === 'degraded' ? 'text-yellow-400' :
                serviceStatus === 'unhealthy' ? 'text-red-400' :
                'text-gray-400'
              }`}>
                {serviceStatus === 'healthy' ? <Wifi size={14} /> : 
                 serviceStatus === 'unhealthy' ? <WifiOff size={14} /> :
                 service.icon}
              </div>

              {/* Name & Description */}
              <div className="flex-1 text-left min-w-0">
                <div className={`text-xs font-medium truncate ${
                  service.critical ? 'text-[var(--text-main)]' : 'text-[var(--text-secondary)]'
                }`}>
                  {service.displayName}
                  {service.critical && <span className="ml-1 text-[8px] text-red-400 uppercase">Critical</span>}
                </div>
                {!compact && (
                  <div className="text-[9px] text-[var(--text-secondary)] truncate">{service.description}</div>
                )}
              </div>

              {/* Status Badge */}
              <StatusBadge status={serviceStatus} compact={compact} />

              {/* External Link */}
              <ExternalLink 
                size={10} 
                className="text-[var(--text-secondary)] opacity-0 group-hover:opacity-100 transition-opacity" 
              />
            </motion.button>
          );
        })}
      </div>

      {/* ── Selected Service Details ── */}
      <AnimatePresence>
        {showDetails && selectedService && serviceDetail && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="mt-3 pt-3 border-t border-[var(--border-accent)] overflow-hidden"
          >
            <ServiceDetailPanel 
              service={SERVICE_REGISTRY.find(s => s.name === selectedService)!}
              health={serviceDetail}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Footer ── */}
      {!compact && (
        <div className="mt-3 pt-3 border-t border-[var(--border-accent)] flex items-center justify-between text-[9px] text-[var(--text-secondary)] font-mono">
          <span>Last check: {lastRefresh.toLocaleTimeString()}</span>
          <span>Refresh: {autoRefresh}s</span>
          {globalHealth?.checkedAt && (
            <span className="flex items-center gap-1">
              <Clock size={8} />
              {new Date(globalHealth.checkedAt).toLocaleTimeString()}
            </span>
          )}
        </div>
      )}
    </div>
  );
};

// ══════════════════════════════════════════════════════════════════════════════
// SUB-COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

function StatusBadge({ status, compact }: { status: string; compact?: boolean }) {
  const config = {
    healthy: { color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/50', icon: CheckCircle, label: 'Online' },
    degraded: { color: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/50', icon: AlertTriangle, label: 'Degraded' },
    unhealthy: { color: 'bg-red-500/20 text-red-400 border-red-500/50', icon: XCircle, label: 'Offline' },
    unknown: { color: 'bg-gray-500/20 text-gray-400 border-gray-500/50', icon: Minus, label: 'Unknown' },
  };

  const c = config[status as keyof typeof config] || config.unknown;
  const Icon = c.icon;

  return (
    <span className={`flex items-center gap-1 px-1.5 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider border ${c.color}`}>
      <Icon size={compact ? 8 : 10} />
      {!compact && c.label}
    </span>
  );
}

function ServiceDetailPanel({ service, health }: { service: ServiceConfig; health: ServiceHealth }) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h4 className="text-xs font-bold text-[var(--accent-primary)] flex items-center gap-1.5">
          {service.icon}
          {service.displayName} Details
        </h4>
        <a 
          href={service.url} 
          target="_blank" 
          rel="noopener noreferrer"
          className="text-[9px] text-[var(--accent-primary)] hover:underline flex items-center gap-1"
        >
          Open <ExternalLink size={8} />
        </a>
      </div>

      <div className="grid grid-cols-2 gap-2 text-[10px]">
        <DetailItem label="Status" value={health.status} highlight />
        <DetailItem label="Response Time" value={health.responseTime ? `${health.responseTime}ms` : 'N/A'} />
        <DetailItem label="Status Code" value={health.statusCode?.toString() || 'N/A'} />
        <DetailItem label="Last Check" value={new Date(health.timestamp).toLocaleTimeString()} />
      </div>

      {health.error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded p-2 text-[10px] text-red-400 font-mono">
          Error: {health.error}
        </div>
      )}

      {health.details && Object.keys(health.details).length > 0 && (
        <details className="group">
          <summary className="text-[10px] text-[var(--text-secondary)] cursor-pointer hover:text-[var(--text-main)]">
            Raw Response Data ▾
          </summary>
          <pre className="mt-1 p-2 bg-[var(--bg-cell)] rounded text-[9px] font-mono overflow-x-auto">
            {JSON.stringify(health.details, null, 2)}
          </pre>
        </details>
      )}
    </div>
  );
}

function DetailItem({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="bg-[var(--bg-cell)] rounded p-1.5">
      <div className="text-[8px] text-[var(--text-secondary)] uppercase tracking-wider">{label}</div>
      <div className={`font-mono font-medium ${highlight ? 'text-[var(--accent-primary)]' : 'text-[var(--text-main)]'}`}>
        {value}
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// EXPORT HOOK FOR PROGRAMMATIC USE
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Hook for accessing global health status anywhere in the app
 */
export function useSystemHealth() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['global-health'],
    queryFn: fetchGlobalHealth,
    refetchInterval: 30000,
  });

  return {
    health: data,
    isLoading,
    isError,
    isHealthy: data?.overall === 'healthy',
    isDegraded: data?.overall === 'degraded',
    isUnhealthy: data?.overall === 'unhealthy',
    refresh: refetch,
    healthyCount: data?.totals?.healthy ?? 0,
    unhealthyCount: data?.totals?.unhealthy ?? 0,
    totalServices: data?.totals?.total ?? 0,
  };
}

export default ServiceHealthMonitor;