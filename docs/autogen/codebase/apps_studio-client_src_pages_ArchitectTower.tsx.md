# 📄 ফাইল: apps/studio-client/src/pages/ArchitectTower.tsx

**প্রকার:** .tsx  
**সাইজ:** 8,596 বাইট  
**আপডেট:** 2026-07-07T18:25:59.934768

---

## কোড

```tsx
import React, { useState, useEffect } from 'react';
import { ShieldAlert, Activity, CheckCircle, Database } from 'lucide-react';
import { FixPreviewModal } from '../components/FixPreviewModal';
import { getApiBaseUrl } from '../utils/api';
import { getAdminToken } from '../services/adminTokenStore';

export const ArchitectTower: React.FC = () => {
  const [fixes, setFixes] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedFix, setSelectedFix] = useState<any>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchFixes = async () => {
    setLoading(true);
    try {
      const token = getAdminToken();
      // If no token in standard store, we might be using the dev/local fallback in the backend, but let's pass what we have
      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes?tenant_id=supremeai-a`, {
        headers: {
          'Authorization': `Bearer ${token || 'mock_token'}`
        }
      });
      if (!res.ok) throw new Error('Failed to fetch pending fixes');
      const data = await res.json();
      setFixes(data.fixes || []);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFixes();
  }, []);

  const handleApprove = async () => {
    if (!selectedFix) return;
    setActionLoading(true);
    try {
      const token = getAdminToken();
      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes/${selectedFix.id}/approve?tenant_id=supremeai-a`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token || 'mock_token'}`,
          'Content-Type': 'application/json'
        }
      });
      if (!res.ok) throw new Error('Failed to approve fix');
      
      setSelectedFix(null);
      fetchFixes(); // Refresh list
    } catch (err: any) {
      alert(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedFix) return;
    setActionLoading(true);
    try {
      const token = getAdminToken();
      const res = await fetch(`${getApiBaseUrl()}/api/admin/fixes/${selectedFix.id}/reject?tenant_id=supremeai-a`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token || 'mock_token'}`,
          'Content-Type': 'application/json'
        }
      });
      if (!res.ok) throw new Error('Failed to reject fix');
      
      setSelectedFix(null);
      fetchFixes(); // Refresh list
    } catch (err: any) {
      alert(err.message);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-300 p-8">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* Header Section */}
        <div className="flex items-center gap-4 border-b border-slate-800 pb-6">
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
            <ShieldAlert className="w-8 h-8 text-emerald-400" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white tracking-tight">Architectural Control Tower</h1>
            <p className="text-slate-400 mt-1">Self-Healing System & HITL Operations Dashboard</p>
          </div>
        </div>

        {/* Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
            <div className="p-3 bg-rose-500/10 rounded-lg">
              <Activity className="w-6 h-6 text-rose-400" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Pending Reviews</p>
              <p className="text-2xl font-bold text-white">{fixes.length}</p>
            </div>
          </div>
          
          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
            <div className="p-3 bg-emerald-500/10 rounded-lg">
              <CheckCircle className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">System Health</p>
              <p className="text-2xl font-bold text-white">Nominal</p>
            </div>
          </div>

          <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex items-center gap-4">
            <div className="p-3 bg-blue-500/10 rounded-lg">
              <Database className="w-6 h-6 text-blue-400" />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">Active Nodes</p>
              <p className="text-2xl font-bold text-white">4</p>
            </div>
          </div>
        </div>

        {/* Pending Fixes Table */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
          <div className="p-6 border-b border-slate-800">
            <h2 className="text-xl font-bold text-white">Action Required: Pending Fixes</h2>
          </div>
          
          {loading ? (
            <div className="p-12 flex justify-center">
              <div className="w-8 h-8 border-2 border-emerald-500/30 border-t-emerald-500 rounded-full animate-spin" />
            </div>
          ) : error ? (
            <div className="p-6 text-rose-400 bg-rose-950/20">{error}</div>
          ) : fixes.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              No pending fixes. System is running optimally.
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-800/50">
                  <th className="p-4 font-semibold text-slate-400 text-sm">Fix ID / Context</th>
                  <th className="p-4 font-semibold text-slate-400 text-sm">Error Type</th>
                  <th className="p-4 font-semibold text-slate-400 text-sm">Impact Score</th>
                  <th className="p-4 font-semibold text-slate-400 text-sm">Created At</th>
                  <th className="p-4 font-semibold text-slate-400 text-sm text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/50">
                {fixes.map((fix) => (
                  <tr key={fix.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="p-4">
                      <div className="font-mono text-emerald-400 text-sm">{fix.id}</div>
                      <div className="text-xs text-slate-500 mt-1">{fix.target_file || 'Unknown file'}</div>
                    </td>
                    <td className="p-4">
                      <span className="px-2 py-1 bg-rose-950/50 text-rose-400 rounded text-xs border border-rose-900/50">
                        {fix.error_type}
                      </span>
                    </td>
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <div className="w-full bg-slate-800 rounded-full h-1.5 w-16">
                          <div 
                            className="bg-amber-500 h-1.5 rounded-full" 
                            style={{ width: `${Math.min(100, (fix.impact_score || 0) * 10)}%` }} 
                          />
                        </div>
                        <span className="text-sm font-medium">{fix.impact_score}</span>
                      </div>
                    </td>
                    <td className="p-4 text-sm text-slate-400">
                      {new Date(fix.created_at).toLocaleString()}
                    </td>
                    <td className="p-4 text-right">
                      <button 
                        onClick={() => setSelectedFix(fix)}
                        className="px-4 py-2 bg-emerald-600/10 hover:bg-emerald-600/20 text-emerald-400 border border-emerald-500/20 rounded-lg text-sm font-medium transition-colors"
                      >
                        Preview Fix
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <FixPreviewModal 
        isOpen={!!selectedFix}
        onClose={() => setSelectedFix(null)}
        onApprove={handleApprove}
        onReject={handleReject}
        fix={selectedFix}
        loading={actionLoading}
      />
    </div>
  );
};

```