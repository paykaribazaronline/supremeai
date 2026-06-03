import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';

export const TerminalLogs = () => {
  const [logs, setLogs] = useState<string[]>([
    "[SYSTEM] Initializing SupremeAI Kernel v4.2.0...",
    "[NETWORK] Establishing secure uplink to Node-07...",
    "[AUTH] Permission level: ADMINISTRATIVE",
    "[SECURITY] Firewall active. Zero-day monitoring enabled.",
  ]);
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const interval = setInterval(() => {
      const messages = [
        `[PROCESS] Request handled in ${Math.floor(Math.random() * 50)}ms`,
        "[DB] Firestore sync complete",
        "[AI] Model weight recalibration successful",
        "[USER] New session detected in SG region",
        "[WARN] Latency spike detected in EU-West-2",
        "[SYSTEM] Memory optimization routine started",
      ];
      setLogs(prev => [...prev.slice(-15), messages[Math.floor(Math.random() * messages.length)]]);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  return (
    <div className="terminal-box" style={{ height: 250 }}>
      <div className="terminal-header">
        <span>কোর_লগ_স্ট্রিম (CORE_LOGS)</span>
        <span>{new Date().toLocaleTimeString()}</span>
      </div>
      <div style={{ overflowY: 'auto', height: '180px' }}>
        {logs.map((log, i) => (
          <div key={i} style={{ marginBottom: 4, opacity: (i + 1) / logs.length, fontSize: 11 }}>
            <span style={{ color: log.includes('WARN') ? 'var(--warning)' : log.includes('SYSTEM') ? 'var(--neon-purple)' : 'var(--neon-blue)' }}>{"> "}</span>
            {log}
          </div>
        ))}
        <div ref={logEndRef} />
        <span className="terminal-cursor" />
      </div>
    </div>
  );
};
