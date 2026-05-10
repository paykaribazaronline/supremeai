import React from 'react';

interface GaugeProps {
    label: string;
    value: number;
    color: string;
}

const Gauge: React.FC<GaugeProps> = ({ label, value, color }) => (
    <div className="flex flex-col items-center gap-3">
        <div className="relative w-20 h-20">
            <svg className="w-full h-full -rotate-90">
                <circle cx="40" cy="40" r="34" fill="none" stroke="currentColor" strokeWidth="4" className="text-white/5" />
                <circle 
                    cx="40" cy="40" r="34" fill="none" stroke={color} strokeWidth="5" 
                    strokeDasharray={213.6} 
                    strokeDashoffset={213.6 * (1 - value / 100)} 
                    strokeLinecap="round"
                    className="transition-all duration-1000"
                    style={{ filter: `drop-shadow(0 0 12px ${color}88)` }}
                />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-sm font-mono font-black text-white">{value}%</span>
            </div>
        </div>
        <span className="text-[12px] font-black text-cyan-400 uppercase tracking-[0.2em]">{label}</span>
    </div>
);

const ResourceGauges: React.FC = () => {
    return (
        <div className="area-sidebar flex flex-col gap-8 h-full">
            <div className="flex flex-col gap-2">
                <span className="text-[14px] font-black uppercase tracking-widest text-white">RESOURCE_SATURATION</span>
                <p className="text-[10px] font-black text-yellow-400 uppercase tracking-[0.2em]">REAL_TIME_PROFILING // LIVE</p>
            </div>

            <div className="flex flex-col gap-10 items-center justify-center flex-1">
                <Gauge label="Neural Core" value={42} color="#22d3ee" />
                <Gauge label="Memory Bank" value={68} color="#fbbf24" />
                <Gauge label="Network Link" value={15} color="#10b981" />
            </div>

            <div className="mt-auto space-y-4">
                <div className="flex justify-between items-center text-[11px] font-black">
                    <span className="text-white uppercase tracking-widest">THERMAL_LOAD</span>
                    <span className="text-emerald-500 font-bold bg-emerald-500/10 px-2 py-0.5 rounded">STABLE_NOMINAL</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden border border-white/5">
                    <div className="h-full bg-emerald-500 w-[35%]" />
                </div>
            </div>
        </div>
    );
};

export default ResourceGauges;
