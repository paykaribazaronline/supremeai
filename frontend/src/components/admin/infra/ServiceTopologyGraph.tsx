/**
 * ServiceTopologyGraph - Visual Dependency Graph Component
 * 
 * Displays all SupremeAI services as an interactive node graph.
 * Shows real-time health status with animated connections.
 * 
 * Features:
 * - Force-directed graph layout
 * - Real-time status colors (green/yellow/red)
 * - Click to see service details
 * - Category grouping
 * - Responsive design
 */

import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Server, Database, Cloud, GitBranch, Activity, 
  Key, Shield, AlertTriangle, CheckCircle, XCircle,
  Minus, Clock, Zap, ExternalLink, Info, Maximize2
} from 'lucide-react';

// ══════════════════════════════════════════════════════════════════════════════
// TYPES
// ══════════════════════════════════════════════════════════════════════════════

interface TopologyNode {
  id: string;
  label: string;
  category: string;
  status: 'healthy' | 'degraded' | 'unhealthy' | 'unknown' | 'maintenance';
  critical: boolean;
  responseTime: number;
  position: { x: number; y: number };
}

interface TopologyEdge {
  source: string;
  target: string;
  type: string;
}

interface TopologyData {
  nodes: TopologyNode[];
  edges: TopologyEdge[];
  generatedAt: string;
}

interface ServiceHealthData {
  name: string;
  display_name: string;
  category: string;
  status: string;
  response_time_ms: number;
  error?: string;
  url: string;
  critical: boolean;
}

interface TopologyResponse {
  timestamp: string;
  overall_status: string;
  services: ServiceHealthData[];
  topology: TopologyData;
  summary: Record<string, number>;
  alerts: string[];
}

// ══════════════════════════════════════════════════════════════════════════════
// CATEGORY CONFIG
// ══════════════════════════════════════════════════════════════════════════════

const CATEGORY_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  infrastructure: { icon: <Server size={14} />, color: '#3B82F6', label: 'Infrastructure' },
  database: { icon: <Database size={14} />, color: '#10B981', label: 'Database' },
  auth: { icon: <Shield size={14} />, color: '#8B5CF6', label: 'Authentication' },
  edge: { icon: <Cloud size={14} />, color: '#06B6D4', label: 'Edge/CDN' },
  ci_cd: { icon: <GitBranch size={14} />, color: '#F59E0B', label: 'CI/CD' },
  monitoring: { icon: <Activity size={14} />, color: '#EF4444', label: 'Monitoring' },
  secrets: { icon: <Key size={14} />, color: '#EC4899', label: 'Secrets' },
};

// ══════════════════════════════════════════════════════════════════════════════
// API FUNCTIONS
// ══════════════════════════════════════════════════════════════════════════════

async function fetchTopology(): Promise<TopologyResponse> {
  const response = await fetch('/api/admin-api/service-topology');
  if (!response.ok) throw new Error('Failed to fetch topology');
  return response.json();
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENT
// ══════════════════════════════════════════════════════════════════════════════

interface ServiceTopologyGraphProps {
  compact?: boolean;
  autoRefresh?: number; // seconds
  onServiceClick?: (service: ServiceHealthData) => void;
  showLegend?: boolean;
}

export const ServiceTopologyGraph: React.FC<ServiceTopologyGraphProps> = ({
  compact = false,
  autoRefresh = 30,
  onServiceClick,
  showLegend = true,
}) => {
  const [selectedNode, setSelectedNode] = useState<TopologyNode | null>(null);
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);
  const [showFullscreen, setShowFullscreen] = useState(false);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['service-topology'],
    queryFn: fetchTopology,
    refetchInterval: autoRefresh * 1000,
  });

  // Find selected service details
  const selectedService = useMemo(() => {
    if (!selectedNode || !data) return null;
    return data.services.find(s => s.name === selectedNode.id);
  }, [selectedNode, data]);

  // Status color helper
  const getStatusColor = useCallback((status: string) => {
    switch (status) {
      case 'healthy': return '#10B981';
      case 'degraded': return '#F59E0B';
      case 'unhealthy': return '#EF4444';
      case 'maintenance': return '#6366F1';
      default: return '#6B7280';
    }
  }, []);

  // Status icon helper
  const getStatusIcon = useCallback((status: string) => {
    switch (status) {
      case 'healthy': return <CheckCircle size={12} />;
      case 'degraded': return <AlertTriangle size={12} />;
      case 'unhealthy': return <XCircle size={12} />;
      case 'maintenance': return <Minus size={12} />;
      default: return <Minus size={12} />;
    }
  }, []);

  // Loading state
  if (isLoading) {
    return (
      <div className={