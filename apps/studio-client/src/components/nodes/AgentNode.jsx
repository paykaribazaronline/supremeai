import { Handle, Position } from '@xyflow/react';
import { Bot } from 'lucide-react';
import { motion } from 'framer-motion';

export const AgentNode = ({ data }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.5 }} 
      animate={{ opacity: 1, scale: 1 }} 
      transition={{ duration: 0.5 }}
      className="p-4 rounded-lg bg-slate-900 border-2 border-cyan-500 shadow-[0_0_15px_rgba(6,182,212,0.3)] min-w-[150px]"
    >
      <div className="flex items-center gap-2 text-cyan-400">
        <Bot size={20} />
        <span className="font-bold">{data.label}</span>
      </div>
      <div className="text-xs text-slate-400 mt-2">{data.status || 'Active'}</div>
      {/* Target & Source Handles */}
      <Handle type="target" position={Position.Top} />
      <Handle type="source" position={Position.Bottom} />
    </motion.div>
  );
};
