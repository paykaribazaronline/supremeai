import React, { useEffect, useState } from 'react';
import { Activity, Server, Database, Cpu, Clock, CheckCircle } from 'lucide-react';
import { fetchJavaWorkerHealth, JavaWorkerHealth } from '../../services/api/microserviceMonitor';

export const ServiceHealthMetrics: React.FC = () => {
  const [metrics, setMetrics] = useState<JavaWorkerHealth | null>(null);

  useEffect(() => {
    const loadMetrics = async () => {
      const data = await fetchJavaWorkerHealth();
      setMetrics(data);
    };
    
    loadMetrics();
    const interval = setInterval(loadMetrics, 5000); // refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  if (!metrics) return <div className="text-gray-400 p-4">Loading Worker Metrics...</div>;

  return (
    <div className="bg-[#1A1A2E] border border-blue-500/30 rounded-xl p-6 shadow-lg relative overflow-hidden group">
      <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 to-transparent opacity-50 pointer-events-none group-hover:opacity-100 transition-opacity duration-500"></div>
      
      <div className="flex items-center justify-between mb-4 relative z-10">
        <h3 className="text-xl font-bold text-white flex items-center gap-2">
          <Server className="text-blue-400" />
          Java Background Worker
        </h3>
        <span className={`px-3 py-1 rounded-full text-xs font-semibold ${metrics.status === 'HEALTHY' ? 'bg-green-500/20 text-green-400 border border-green-500/50' : 'bg-red-500/20 text-red-400 border border-red-500/50'}`}>
          {metrics.status}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-4 relative z-10">
        <div className="bg-black/40 rounded-lg p-3 border border-white/5 flex flex-col justify-center">
          <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
            <Cpu size={14} className="text-pink-400" /> CPU Load
          </div>
          <div className="text-2xl font-mono text-white">{metrics.cpuLoadPercentage}%</div>
        </div>

        <div className="bg-black/40 rounded-lg p-3 border border-white/5 flex flex-col justify-center">
          <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
            <Database size={14} className="text-cyan-400" /> Memory
          </div>
          <div className="text-2xl font-mono text-white">{metrics.memoryUsageMb} MB</div>
        </div>

        <div className="bg-black/40 rounded-lg p-3 border border-white/5 flex flex-col justify-center">
          <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
            <Activity size={14} className="text-orange-400" /> Active Tasks
          </div>
          <div className="text-2xl font-mono text-white">{metrics.activeTasks}</div>
        </div>

        <div className="bg-black/40 rounded-lg p-3 border border-white/5 flex flex-col justify-center">
          <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
            <CheckCircle size={14} className="text-green-400" /> Processed
          </div>
          <div className="text-2xl font-mono text-white">{metrics.totalTasksProcessed}</div>
        </div>
      </div>
    </div>
  );
};
