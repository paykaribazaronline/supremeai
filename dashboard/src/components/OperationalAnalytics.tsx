import React from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Filler,
  Legend
);

interface OperationalAnalyticsProps {
  data?: number[];
  labels?: string[];
}

const OperationalAnalytics: React.FC<OperationalAnalyticsProps> = ({ 
  data = [65, 59, 80, 81, 56, 55, 40, 70, 90, 85, 75, 95],
  labels = ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
}) => {
  const chartData = {
    labels,
    datasets: [
      {
        fill: true,
        label: 'System Throughput (req/s)',
        data: data,
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.4,
        pointRadius: 0,
        borderWidth: 2,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        mode: 'index' as const,
        intersect: false,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleFont: { size: 10, family: 'Inter' },
        bodyFont: { size: 10, family: 'Inter' },
      },
    },
    scales: {
      x: {
        display: true,
        grid: { display: false },
        ticks: { color: 'rgba(255, 255, 255, 0.2)', font: { size: 8 } }
      },
      y: {
        display: true,
        grid: { color: 'rgba(255, 255, 255, 0.05)' },
        ticks: { color: 'rgba(255, 255, 255, 0.2)', font: { size: 8 } }
      },
    },
  };

  return (
    <div className="area-analytics glass-card p-4 flex flex-col gap-4 min-h-[280px]">
      <div className="flex justify-between items-center">
        <div className="flex flex-col gap-1">
          <span className="text-[10px] font-black uppercase tracking-widest text-white/60">Operational Throughput</span>
          <p className="text-[8px] font-bold text-white/20 uppercase tracking-widest">Global Request Flow</p>
        </div>
        <div className="px-2 py-1 rounded bg-blue-500/10 border border-blue-500/20">
          <span className="text-[10px] font-mono text-blue-400 font-bold">LIVE: 942 req/s</span>
        </div>
      </div>
      
      <div className="flex-1 relative">
        <Line data={chartData} options={options} />
      </div>

      <div className="flex gap-4 border-t border-white/5 pt-4">
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-[8px] font-bold text-white/20 uppercase">Avg latency</span>
          <span className="text-xs font-mono font-black text-white">124ms</span>
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-[8px] font-bold text-white/20 uppercase">Error rate</span>
          <span className="text-xs font-mono font-black text-emerald-400">0.02%</span>
        </div>
        <div className="flex-1 flex flex-col gap-1">
          <span className="text-[8px] font-bold text-white/20 uppercase">Peak load</span>
          <span className="text-xs font-mono font-black text-white">1.2k</span>
        </div>
      </div>
    </div>
  );
};

export default OperationalAnalytics;
