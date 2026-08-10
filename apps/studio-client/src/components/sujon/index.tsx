// SupremeAI — Sujon Core Dashboard Components
// ============================================
// Refactored dashboard components for better maintainability

export interface SujonWidgetProps {
  title: string;
  value: string | number;
  trend?: 'up' | 'down' | 'neutral';
  icon?: string;
}

export interface MetricData {
  id: string;
  label: string;
  value: number;
  unit?: string;
}

// Core hook for real-time metrics
import { useEffect, useState } from 'react';

// বাংলা মন্তব্য: এই ফাইলটি হুক ও কনস্ট্যান্ট এক্সপোর্ট করে (শুধু কম্পোনেন্ট নয়) — react-refresh নিয়ম ইচ্ছাকৃতভাবে disable।
/* eslint-disable-next-line react-refresh/only-export-components */
export function useSujonMetrics() {
  const [metrics, setMetrics] = useState<MetricData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const response = await fetch('/api/admin/metrics/realtime');
        const data = await response.json();
        setMetrics(data.metrics || []);
      } catch (error) {
        console.error('Failed to fetch metrics:', error);
      } finally {
        setLoading(false);
      }
    };

    const interval = setInterval(fetchMetrics, 5000);
    fetchMetrics();

    return () => clearInterval(interval);
  }, []);

  return { metrics, loading };
}

// Health indicator component
export function SujonHealthIndicator({ status }: { status: 'healthy' | 'warning' | 'critical' }) {
  const colors = {
    healthy: 'bg-green-500',
    warning: 'bg-yellow-500',
    critical: 'bg-red-500',
  };

  return (
    <div className={`w-3 h-3 rounded-full ${colors[status]} animate-pulse`} />
  );
}

// Dashboard grid layout
export function SujonDashboardGrid({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 p-4">
      {children}
    </div>
  );
}
