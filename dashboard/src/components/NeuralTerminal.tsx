import React, { useEffect, useRef } from 'react';

export interface LogEntry {
    id: string;
    timestamp: string;
    level: 'INFO' | 'WARN' | 'CRIT';
    message: string;
    source: string;
}

interface NeuralTerminalProps {
    logs: LogEntry[];
}

const NeuralTerminal: React.FC<NeuralTerminalProps> = ({ logs }) => {
    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [logs]);

    return (
        <div className="flex flex-col h-full bg-[#050505] rounded-lg border border-white/5 overflow-hidden shadow-2xl">
            {/* Terminal Header */}
            <div className="px-4 py-2 bg-black border-b border-white/10 flex justify-between items-center">
                <div className="flex items-center gap-2 min-w-0">
                    <div className="flex gap-1.5 shrink-0">
                        <div className="w-2 h-2 rounded-full bg-red-500/40" />
                        <div className="w-2 h-2 rounded-full bg-amber-500/40" />
                        <div className="w-2 h-2 rounded-full bg-emerald-500/40" />
                    </div>
                    <div className="h-4 w-[1px] bg-white/10 mx-2 shrink-0" />
                    <span className="text-[12px] font-black uppercase tracking-[0.1em] text-white whitespace-nowrap overflow-hidden text-ellipsis">
                        Neural Activity Stream // LIVE_BROADCAST
                    </span>
                </div>
                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-1.5 bg-emerald-500 px-3 py-1 rounded-sm">
                        <div className="w-2 h-2 rounded-full bg-white animate-pulse" />
                        <span className="text-[10px] font-black text-black uppercase tracking-tighter">ONLINE</span>
                    </div>
                </div>
            </div>
            
            {/* Terminal Body */}
            <div 
                ref={scrollRef}
                className="flex-1 p-4 font-mono text-[12px] overflow-y-auto custom-scrollbar bg-black relative"
            >
                {/* Scanline Effect Overlay */}
                <div className="absolute inset-0 pointer-events-none opacity-[0.03] bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_2px,3px_100%]" />
                
                <div className="flex flex-col gap-1 relative z-10">
                    {logs.map((log) => (
                        <div key={log.id} className="flex gap-3 group items-start">
                            <span className="text-white/10 shrink-0 select-none font-bold">[{log.timestamp}]</span>
                            <span className={`font-black shrink-0 px-1 rounded-[2px] ${
                                log.level === 'CRIT' ? 'bg-red-600 text-white' : 
                                log.level === 'WARN' ? 'bg-amber-500 text-black' : 
                                'bg-cyan-500 text-black'
                            }`}>
                                {log.level}
                            </span>
                            <span className="text-emerald-500/40 shrink-0 font-bold">{log.source}</span>
                            <span className="text-white/90 leading-relaxed break-all">
                                {log.message}
                            </span>
                        </div>
                    ))}
                    {logs.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20">
                            <span className="text-3xl font-black uppercase tracking-[0.5em] animate-pulse text-white">UPLINK_IDLE</span>
                            <span className="text-[12px] font-bold uppercase tracking-widest mt-4 text-yellow-400 bg-black px-2">Awaiting system telemetry broadcast...</span>
                        </div>
                    )}
                </div>
            </div>

            {/* Terminal Footer */}
            <div className="px-4 py-2 bg-black border-t border-white/20 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <span className="text-[10px] font-black text-white uppercase">STATUS</span>
                    <span className="text-[10px] font-mono text-cyan-400 font-bold uppercase">SYSTEM_READY_V4.0</span>
                </div>
                <div className="flex items-center gap-4 text-[10px] font-black text-white uppercase">
                    <span>RECORDS: {logs.length}</span>
                    <span className="text-emerald-500">ENCODING: UTF-8</span>
                </div>
            </div>
        </div>
    );
};

export default NeuralTerminal;