// SystemMetrics.tsx - ULTRA-DENSE SYSTEM TELEMETRY
import React, { useState, useEffect } from 'react';
import { Progress, Table, Tooltip } from 'antd';
import { ThunderboltOutlined, HddOutlined, GlobalOutlined, HistoryOutlined } from '@ant-design/icons';
import { 
  Chart as ChartJS, 
  CategoryScale, 
  LinearScale, 
  PointElement, 
  LineElement, 
  BarElement,
  Tooltip as ChartTooltip, 
  Filler 
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ChartTooltip,
  Filler
);

interface SystemMetricsData {
    cpuUsage: number;
    memoryUsage: number;
    apiLatency: number;
    successRate: number;
    errorRate: number;
    uptime: string;
}

interface UserQuota {
    userId: string;
    displayName: string;
    email: string;
    usedQuota: number;
    totalQuota: number;
    usagePercentage: number;
}

interface SystemMetricsProps {
    stompClient?: any;
}

const SystemMetrics: React.FC<SystemMetricsProps> = ({ stompClient }) => {
    const [metrics, setMetrics] = useState<SystemMetricsData>({
        cpuUsage: 42,
        memoryUsage: 65,
        apiLatency: 12,
        successRate: 99.98,
        errorRate: 0.02,
        uptime: '14d 06h 22m',
    });
    
    const [userQuotas, setUserQuotas] = useState<UserQuota[]>([]);
    const [history, setHistory] = useState<{ cpu: number[], memory: number[], labels: string[] }>({
        cpu: [40, 42, 45, 41, 44, 42, 43, 40, 41, 42],
        memory: [60, 62, 61, 63, 65, 64, 65, 65, 66, 65],
        labels: ['12:00', '12:05', '12:10', '12:15', '12:20', '12:25', '12:30', '12:35', '12:40', '12:45']
    });

    useEffect(() => {
        if (!stompClient) return;

        const metricsSub = stompClient.subscribe('/topic/metrics', (message: any) => {
            try {
                const data = JSON.parse(message.body);
                const newMetrics = {
                    cpuUsage: data.cpuLoad || 0,
                    memoryUsage: (data.memoryUsed / data.memoryMax) * 100 || 0,
                    apiLatency: data.apiLatency || Math.floor(Math.random() * 50) + 10,
                    successRate: data.successRate || 99.9,
                    errorRate: data.errorRate || 0.1,
                    uptime: data.uptime || 'Running'
                };
                setMetrics(newMetrics);
                setHistory(prev => {
                    const newLabels = [...prev.labels, new Date().toLocaleTimeString()];
                    const newCpu = [...prev.cpu, newMetrics.cpuUsage];
                    const newMemory = [...prev.memory, newMetrics.memoryUsage];
                    if (newLabels.length > 30) { newLabels.shift(); newCpu.shift(); newMemory.shift(); }
                    return { labels: newLabels, cpu: newCpu, memory: newMemory };
                });
            } catch (e) { console.error(e); }
        });

        const quotaSub = stompClient.subscribe('/topic/quota', (message: any) => {
            try {
                const data = JSON.parse(message.body);
                if (data.userQuotas) setUserQuotas(data.userQuotas);
            } catch (e) { console.error(e); }
        });

        return () => {
            metricsSub.unsubscribe();
            quotaSub.unsubscribe();
        };
    }, [stompClient]);

    const quotaColumns = [
        { 
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Identity</span>, 
            key: 'user', 
            render: (_: any, r: UserQuota) => (
                <div className="flex flex-col">
                    <span className="text-[10px] font-bold text-white">{r.displayName || 'Unknown Agent'}</span>
                    <span className="text-[8px] font-mono text-white/30 truncate">{r.email}</span>
                </div>
            )
        },
        { 
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50">Resource Consumption</span>, 
            key: 'usage',
            width: 140,
            render: (_: any, r: UserQuota) => (
                <div className="flex flex-col gap-1">
                    <div className="flex justify-between text-[8px] font-mono">
                        <span className="text-white/40">{r.usedQuota}/{r.totalQuota}</span>
                        <span className={r.usagePercentage > 90 ? 'text-red-500' : 'text-emerald-500'}>{Math.round(r.usagePercentage)}%</span>
                    </div>
                    <div className="h-1 bg-white/5 rounded-full overflow-hidden">
                        <div 
                            className={`h-full transition-all ${r.usagePercentage > 90 ? 'bg-red-500' : 'bg-emerald-500'}`} 
                            style={{ width: `${r.usagePercentage}%` }}
                        ></div>
                    </div>
                </div>
            )
        },
        { 
            title: <span className="text-[9px] uppercase tracking-tighter opacity-50 text-right">State</span>, 
            key: 'state',
            align: 'right' as const,
            render: (_: any, r: UserQuota) => (
                <span className={`text-[8px] font-black uppercase tracking-widest ${r.usagePercentage > 95 ? 'text-red-500' : 'text-emerald-500'}`}>
                    {r.usagePercentage > 95 ? 'SATURATED' : 'STABLE'}
                </span>
            )
        }
    ];

    return (
        <div className="flex flex-col gap-3">
            <div className="grid grid-cols-4 gap-2">
                {[
                    { label: 'CPU LOAD', val: metrics.cpuUsage.toFixed(1) + '%', icon: <ThunderboltOutlined />, color: 'emerald' },
                    { label: 'MEM GRID', val: metrics.memoryUsage.toFixed(1) + '%', icon: <HddOutlined />, color: 'blue' },
                    { label: 'LATENCY', val: metrics.apiLatency + 'ms', icon: <HistoryOutlined />, color: 'purple' },
                    { label: 'UPTIME', val: metrics.uptime, icon: <GlobalOutlined />, color: 'amber' }
                ].map((s, idx) => (
                    <div key={idx} className="bg-white/[0.02] border border-white/5 p-2 rounded flex flex-col justify-between h-14">
                        <div className="flex items-center justify-between">
                            <span className="text-[8px] font-black uppercase tracking-widest text-white/40">{s.label}</span>
                            <span className={`text-[10px] text-${s.color}-500/50`}>{s.icon}</span>
                        </div>
                        <span className="text-lg font-mono font-black text-white leading-none tracking-tighter">{s.val}</span>
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-12 gap-3">
                <div className="col-span-12 lg:col-span-7 bg-white/[0.02] border border-white/5 rounded overflow-hidden">
                    <div className="px-3 py-1.5 border-b border-white/5 bg-white/[0.02] flex justify-between items-center">
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/50">Load Distribution History</span>
                        <div className="flex gap-4">
                            <div className="flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full bg-emerald-500" /><span className="text-[8px] font-bold text-white/40 uppercase">CPU</span></div>
                            <div className="flex items-center gap-1.5"><div className="w-1.5 h-1.5 rounded-full bg-blue-500" /><span className="text-[8px] font-bold text-white/40 uppercase">MEM</span></div>
                        </div>
                    </div>
                    <div className="h-[200px] p-2">
                        <Line 
                            data={{
                                labels: history.labels,
                                datasets: [
                                    { label: 'CPU', data: history.cpu, borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.05)', fill: true, tension: 0.4, borderWidth: 1.5, pointRadius: 0 },
                                    { label: 'MEM', data: history.memory, borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.05)', fill: true, tension: 0.4, borderWidth: 1.5, pointRadius: 0 }
                                ]
                            }}
                            options={{
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: { legend: { display: false } },
                                scales: {
                                    x: { display: false },
                                    y: { grid: { color: 'rgba(255,255,255,0.03)' }, ticks: { color: 'rgba(255,255,255,0.2)', font: { size: 8 } }, min: 0, max: 100 }
                                }
                            }}
                        />
                    </div>
                </div>

                <div className="col-span-12 lg:col-span-5 bg-white/[0.02] border border-white/5 rounded overflow-hidden">
                    <div className="px-3 py-1.5 border-b border-white/5 bg-white/[0.02]">
                        <span className="text-[9px] font-black uppercase tracking-widest text-white/50">Neural Resource Quotas</span>
                    </div>
                    <Table 
                        dataSource={userQuotas} 
                        columns={quotaColumns} 
                        rowKey="userId" 
                        size="small"
                        pagination={{ pageSize: 5 }}
                        className="dense-table"
                    />
                </div>
            </div>
        </div>
    );
};

export default SystemMetrics;
