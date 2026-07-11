# 📄 ফাইল: apps/studio-client/src/pages/ArchitectTower.tsx

**প্রকার:** .tsx  
**সাইজ:** 4,925 বাইট  
**আপডেট:** 2026-07-11T10:59:17.920475

---

## কোড

```tsx
import React, { useState, useEffect } from 'react';
import { ShieldAlert, Activity, CheckCircle, Database } from 'lucide-react';
import { OneClickPatch } from '../components/admin/OneClickPatch';
import { getApiBaseUrl } from '../utils/api';
import { getAdminToken } from '../services/adminTokenStore';

export const ArchitectTower: React.FC = () => {
  const [fixes, setFixes] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
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
    let isMounted = true;
    
    const loadFixes = async () => {
      await fetchFixes();
    };
    
    loadFixes();
    
    return () => {
      isMounted = false;
    };
  }, []);

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
            <OneClickPatch 
              proposals={fixes.map(f => ({
                id: f.id,
                issueId: f.target_file,
                description: `Error Type: ${f.error_type} | Impact Score: ${f.impact_score}`,
                beforeCode: f.metadata?.original_code || "// Original code missing",
                afterCode: f.metadata?.proposed_code || "// Proposed fix missing",
                status: 'pending_review'
              }))}
              onPatchApplied={fetchFixes}
            />
          )}
        </div>
      </div>
    </div>
  );
};

```