# 📄 ফাইল: apps/studio-client/src/components/FixPreviewModal.tsx

**প্রকার:** .tsx  
**সাইজ:** 3,437 বাইট  
**আপডেট:** 2026-07-08T03:57:12.478079

---

## কোড

```tsx
import React from 'react';
import { X, Check, XCircle } from 'lucide-react';

interface FixPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApprove: () => void;
  onReject: () => void;
  fix: any;
  loading: boolean;
}

export const FixPreviewModal: React.FC<FixPreviewModalProps> = ({
  isOpen,
  onClose,
  onApprove,
  onReject,
  fix,
  loading
}) => {
  if (!isOpen || !fix) return null;

  const oldCode = fix.metadata?.original_code || "// Original code not provided";
  const newCode = fix.metadata?.proposed_code || "// Proposed fix not provided";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-xl w-full max-w-5xl max-h-[90vh] flex flex-col overflow-hidden shadow-2xl">
        
        {/* Header */}
        <div className="flex justify-between items-center p-4 border-b border-slate-700 bg-slate-800">
          <div>
            <h2 className="text-xl font-bold text-white">Review Fix: {fix.id}</h2>
            <p className="text-slate-400 text-sm mt-1">
              Error Type: <span className="font-mono text-rose-400">{fix.error_type}</span> | 
              Impact Score: <span className="font-mono text-emerald-400">{fix.impact_score || 0}</span>
            </p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-slate-700 rounded-lg text-slate-400 hover:text-white transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Diff Viewer */}
        <div className="flex-1 overflow-auto p-4 bg-slate-950 flex gap-4">
          <div className="flex-1 border border-slate-700 rounded bg-slate-900 flex flex-col">
            <div className="p-2 border-b border-slate-700 font-bold text-slate-300">Current Code</div>
            <pre className="p-4 text-sm font-mono text-slate-300 overflow-auto">{oldCode}</pre>
          </div>
          <div className="flex-1 border border-emerald-900/50 rounded bg-slate-900 flex flex-col shadow-[0_0_15px_rgba(16,185,129,0.1)]">
            <div className="p-2 border-b border-emerald-900/50 font-bold text-emerald-400">SelfHealer Proposed Fix</div>
            <pre className="p-4 text-sm font-mono text-emerald-300 overflow-auto">{newCode}</pre>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t border-slate-700 bg-slate-800 flex justify-end gap-3">
          <button 
            onClick={onReject} 
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-slate-700 hover:bg-rose-900/50 text-rose-400 rounded-lg transition-colors border border-transparent hover:border-rose-500/50"
          >
            <XCircle size={18} />
            Reject
          </button>
          
          <button 
            onClick={onApprove} 
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg shadow-lg shadow-emerald-500/20 transition-all font-medium disabled:opacity-50"
          >
            {loading ? (
              <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            ) : (
              <Check size={18} />
            )}
            Approve & Apply
          </button>
        </div>
      </div>
    </div>
  );
};

```