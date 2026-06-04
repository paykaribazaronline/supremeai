import { ApiOutlined, GlobalOutlined } from "@ant-design/icons";
import React from "react";

interface TelemetryBarProps {
  uptime: string;
  latency: number;
  activeNodes: number;
  load: number;
}

const TelemetryBar: React.FC<TelemetryBarProps> = ({
  uptime,
  latency,
  activeNodes,
  load,
}) => {
  return (
    <div className="area-telemetry glass-card px-6 py-3 flex items-center justify-between border-none bg-white/[0.02]">
      <div className="flex items-center gap-8">
        <div className="flex items-center gap-3">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shadow-[0_0_8px_rgba(16,185,129,0.5)]" />
          <span className="text-[10px] font-black uppercase tracking-widest text-white/90">
            System: Operational
          </span>
        </div>

        <div className="h-4 w-[1px] bg-white/10" />

        <div className="flex items-center gap-6">
          <div className="flex flex-col">
            <span className="text-[8px] font-bold text-white/30 uppercase tracking-widest">
              Uptime
            </span>
            <span className="text-[11px] font-mono text-white/80">
              {uptime}
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-[8px] font-bold text-white/30 uppercase tracking-widest">
              Latency
            </span>
            <span className="text-[11px] font-mono text-emerald-400">
              {latency}ms
            </span>
          </div>
          <div className="flex flex-col">
            <span className="text-[8px] font-bold text-white/30 uppercase tracking-widest">
              Active Nodes
            </span>
            <span className="text-[11px] font-mono text-blue-400">
              {activeNodes}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-8">
        <div className="flex items-center gap-4">
          <span className="text-[8px] font-bold text-white/30 uppercase tracking-widest">
            CPU LOAD
          </span>
          <div className="w-32 h-1 bg-white/5 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-blue-500 to-emerald-500 transition-all duration-1000"
              style={{ width: `${load}%` }}
            />
          </div>
          <span className="text-[10px] font-mono text-white/60">{load}%</span>
        </div>

        <div className="flex gap-2">
          <div className="w-8 h-8 rounded-lg bg-white/[0.03] border border-white/5 flex items-center justify-center text-white/20 hover:text-white/60 cursor-pointer transition-colors">
            <ApiOutlined className="text-sm" />
          </div>
          <div className="w-8 h-8 rounded-lg bg-white/[0.03] border border-white/5 flex items-center justify-center text-white/20 hover:text-white/60 cursor-pointer transition-colors">
            <GlobalOutlined className="text-sm" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default TelemetryBar;
