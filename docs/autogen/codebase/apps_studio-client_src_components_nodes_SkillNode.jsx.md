# 📄 ফাইল: apps/studio-client/src/components/nodes/SkillNode.jsx

**প্রকার:** .jsx  
**সাইজ:** 727 বাইট  
**আপডেট:** 2026-07-11T11:05:10.267388

---

## কোড

```jsx
import { Handle, Position } from '@xyflow/react';
import { Zap } from 'lucide-react';
import { motion } from 'framer-motion';

export const SkillNode = ({ data }) => {
  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.5 }} 
      animate={{ opacity: 1, scale: 1 }} 
      transition={{ duration: 0.5 }}
      className="p-3 rounded-full bg-slate-800 border border-orange-500 flex items-center gap-2 shadow-[0_0_10px_rgba(249,115,22,0.3)]"
    >
      <Zap size={16} className="text-orange-400" />
      <span className="text-sm font-medium text-white">{data.label}</span>
      <Handle type="target" position={Position.Left} />
      <Handle type="source" position={Position.Right} />
    </motion.div>
  );
};

```