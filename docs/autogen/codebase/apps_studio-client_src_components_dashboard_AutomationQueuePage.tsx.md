# 📄 ফাইল: apps/studio-client/src/components/dashboard/AutomationQueuePage.tsx

**প্রকার:** .tsx  
**সাইজ:** 7,204 বাইট  
**আপডেট:** 2026-07-04T04:31:35.623027

---

## কোড

```tsx
// বাংলা মন্তব্য: Infinite Automation Workflow Queue — অ্যাক্টিভ Playwright ব্রাউজার টাস্ক সিকোয়েন্স,
// টাস্ক স্টেট (Queued/Running/Circuit_Open/Success/Failed), এক্সিকিউশন টাইম (৪৫s ক্যাপ) রিয়েল-টাইম তালিকা।
// টাস্ক স্টেটের ভিত্তিতে LiveSujonBackground-এর ভিজুয়াল স্টেটও আপডেট করা হয়।
import { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, Loader2, ListChecks } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { setSujonState } from '../LiveSujonBackground';

interface AutomationTask {
  id: string;
  goal: string;
  status: string;
  createdAt?: string;
  durationMs?: number;
}

const EXECUTION_CAP_MS = 45000;

// বাংলা মন্তব্য: ব্যাকএন্ড স্টেট → UI ব্যাজ স্টাইল ম্যাপিং
const stateBadge = (status: string): string => {
  const s = status.toUpperCase();
  if (s === 'RUNNING' || s === 'ACTIVE') return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
  if (s === 'SUCCESS') return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
  if (s === 'FAILED') return 'bg-rose-500/15 text-rose-300 border-rose-500/30';
  if (s === 'CIRCUIT_OPEN') return 'bg-red-600/20 text-red-300 border-red-600/40';
  return 'bg-slate-500/15 text-slate-300 border-slate-500/30';
};

export function AutomationQueuePage() {
  const [tasks, setTasks] = useState<AutomationTask[]>([]);
  const [goal, setGoal] = useState('');
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(() => {
    apiClient
      .get<{ tasks: AutomationTask[] }>('/api/browser/tasks')
      .then((data) => {
        const list = data.tasks || [];
        setTasks(list);
        setError('');
        // বাংলা মন্তব্য: কোনো টাস্ক CIRCUIT_OPEN হলে লাল সতর্ক-স্টেট, চলমান থাকলে processing, নয়তো idle
        const states = list.map((t) => t.status.toUpperCase());
        if (states.includes('CIRCUIT_OPEN')) setSujonState('circuit_open');
        else if (states.some((s) => s === 'RUNNING' || s === 'ACTIVE')) setSujonState('processing');
        else setSujonState('idle');
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load tasks'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
    // বাংলা মন্তব্য: রিয়েল-টাইম আপডেটের জন্য ৪s পোলিং; আনমাউন্টে ক্লিয়ার হয় (মেমরি লিক নেই)
    const interval = setInterval(refresh, 4000);
    return () => {
      clearInterval(interval);
      setSujonState('idle');
    };
  }, [refresh]);

  const handleCreate = async () => {
    if (!goal.trim() || creating) return;
    setCreating(true);
    setError('');
    try {
      await apiClient.post('/api/browser/tasks', { goal: goal.trim() });
      setGoal('');
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to queue task');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/api/browser/tasks/${id}`);
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete task');
    }
  };

  return (
    <div className="max-w-3xl mx-auto px-6 py-8">
      <h1 className="text-lg font-semibold text-white flex items-center gap-2 mb-1">
        <ListChecks size={17} className="text-blue-400" />
        Automation Workflow Queue
      </h1>
      <p className="text-xs text-slate-500 mb-5">
        Active Playwright automation sequences. Each task is capped at{' '}
        {EXECUTION_CAP_MS / 1000}s of execution time.
      </p>

      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-3 mb-6 flex items-center gap-2">
        <input
          data-testid="automation-goal"
          value={goal}
          onChange={(e) => setGoal(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
          placeholder="Describe an automation goal (e.g. 'Extract latest orders from dashboard')"
          className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
        />
        <button
          data-testid="automation-queue-btn"
          onClick={handleCreate}
          disabled={!goal.trim() || creating}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
        >
          {creating ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
          Queue task
        </button>
      </div>

      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}

      <div className="flex items-center justify-between mb-2">
        <h2 className="text-sm font-medium text-slate-300">Active sequences</h2>
        <span className="text-xs text-slate-500">{tasks.length} total</span>
      </div>

      {loading ? (
        <div className="flex justify-center py-10 text-slate-500">
          <Loader2 size={18} className="animate-spin" />
        </div>
      ) : tasks.length === 0 ? (
        <p className="text-sm text-slate-500 text-center py-8">No automation tasks queued.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {tasks.map((t) => (
            <li
              key={t.id}
              data-testid="automation-row"
              className="flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02]"
            >
              <div className="flex-1 min-w-0">
                <p className="text-xs text-white truncate">{t.goal}</p>
                <p className="text-[11px] text-slate-500">
                  {t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}
                  {typeof t.durationMs === 'number' && ` · ${(t.durationMs / 1000).toFixed(1)}s`}
                </p>
              </div>
              <span
                data-testid="automation-state"
                className={`text-[10px] px-2 py-0.5 rounded-full border font-medium ${stateBadge(t.status)}`}
              >
                {t.status.toUpperCase()}
              </span>
              <button
                aria-label="Delete task"
                onClick={() => handleDelete(t.id)}
                className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
              >
                <Trash2 size={13} />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

```