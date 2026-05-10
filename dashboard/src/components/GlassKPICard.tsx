import React from 'react';

interface GlassKPICardProps {
    label: string;
    value: string | number;
    subValue?: string;
    change?: string;
    icon: React.ReactNode;
    color?: string;
    trend?: 'up' | 'down' | 'neutral';
}

const GlassKPICard: React.FC<GlassKPICardProps> = ({ 
    label, 
    value, 
    subValue, 
    change, 
    icon, 
    color = '#3b82f6',
    trend 
}) => {
    return (
        <div className="glass-card p-4 flex flex-col gap-3 relative group transition-all hover:border-white/20 hover:bg-white/[0.05] overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-[1px] opacity-10 group-hover:opacity-40 transition-opacity" style={{ background: `linear-gradient(90deg, ${color}, transparent)` }} />
            <div className="absolute top-0 left-0 w-[1px] h-full opacity-10 group-hover:opacity-40 transition-opacity" style={{ background: `linear-gradient(180deg, ${color}, transparent)` }} />
            
            <div className="flex justify-between items-start">
                <div className="p-2.5 rounded-xl bg-white/10 border border-white/20 text-white group-hover:text-cyan-400 transition-all duration-300">
                    <span className="text-2xl" style={{ color: trend === 'up' ? '#10b981' : trend === 'down' ? '#ef4444' : 'inherit' }}>
                        {icon}
                    </span>
                </div>
                <div className="flex flex-col items-end">
                    <span className="text-[12px] font-black uppercase tracking-widest text-yellow-400 transition-colors">
                        {label}
                    </span>
                    {change && (
                        <span className={`text-[12px] font-black mt-2 px-3 py-1 rounded-lg ${
                            trend === 'up' ? 'bg-emerald-600 text-white' : 
                            trend === 'down' ? 'bg-red-600 text-white' : 
                            'bg-white text-black'
                        }`}>
                            {change}
                        </span>
                    )}
                </div>
            </div>

            <div className="mt-4">
                <div className="text-4xl font-mono font-black text-white leading-none tracking-tight">
                    {value}
                </div>
                {subValue && (
                    <div className="text-[12px] font-black text-cyan-400 uppercase mt-3 tracking-[0.2em] flex items-center gap-2">
                        <div className="w-2 h-2 rounded-full animate-pulse bg-current" />
                        {subValue}
                    </div>
                )}
            </div>

            <div className="absolute -bottom-10 -right-10 w-24 h-24 blur-[50px] opacity-0 group-hover:opacity-20 transition-opacity duration-700 pointer-events-none" 
                 style={{ backgroundColor: color }} />
        </div>
    );
};

export default GlassKPICard;