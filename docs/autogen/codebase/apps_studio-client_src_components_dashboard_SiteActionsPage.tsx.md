# 📄 ফাইল: apps/studio-client/src/components/dashboard/SiteActionsPage.tsx

**প্রকার:** .tsx  
**সাইজ:** 10,978 বাইট  
**আপডেট:** 2026-07-04T12:59:56.904209

---

## কোড

```tsx
// বাংলা মন্তব্য: site_actions_registry ভিজুয়াল এডিটর (Super-Admin) — টার্গেট ওয়েবসাইটের URL,
// DOM সিলেক্টর ও ইন্টার‌্যাকশন রুল ডায়নামিক CRUD টেবিলে ম্যানেজ করা যায় (হার্ডকোড ছাড়াই)।
// ব্যাকএন্ড /api/admin/site-actions — অ্যাডমিন রোল বাধ্যতামূলক।
import { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, Pencil, Loader2, Table2, X, Check } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface SiteAction {
  id: number;
  site_name: string;
  url_pattern: string;
  action_name: string;
  selector: string;
  action_type: string;
  notes: string;
  enabled: boolean;
}

type DraftAction = Omit<SiteAction, 'id'>;

const EMPTY_DRAFT: DraftAction = {
  site_name: '',
  url_pattern: '',
  action_name: '',
  selector: '',
  action_type: 'click',
  notes: '',
  enabled: true,
};

const ACTION_TYPES = ['click', 'type', 'navigate', 'extract', 'wait', 'scroll'];

export function SiteActionsPage() {
  const [actions, setActions] = useState<SiteAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [draft, setDraft] = useState<DraftAction>(EMPTY_DRAFT);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    apiClient
      .get<{ items: SiteAction[] }>('/api/admin/site-actions/')
      .then((data) => {
        setActions(data.items || []);
        setError('');
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load registry'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const resetForm = () => {
    setDraft(EMPTY_DRAFT);
    setEditingId(null);
  };

  // বাংলা মন্তব্য: নতুন রুল তৈরি অথবা বিদ্যমান রুল আপডেট (editingId থাকলে PUT, নয়তো POST)
  const handleSave = async () => {
    if (!draft.site_name.trim() || !draft.url_pattern.trim() || !draft.selector.trim() || saving) return;
    setSaving(true);
    setError('');
    try {
      if (editingId != null) {
        await apiClient.put(`/api/admin/site-actions/${editingId}`, draft);
      } else {
        await apiClient.post('/api/admin/site-actions/', draft);
      }
      resetForm();
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save action');
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (a: SiteAction) => {
    setEditingId(a.id);
    const { id: _id, ...rest } = a;
    void _id;
    setDraft(rest);
  };

  const handleDelete = async (id: number) => {
    try {
      await apiClient.delete(`/api/admin/site-actions/${id}`);
      if (editingId === id) resetForm();
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete action');
    }
  };

  const setField = (field: keyof DraftAction, value: string | boolean) =>
    setDraft((d) => ({ ...d, [field]: value }));

  return (
    <div className="max-w-4xl mx-auto px-6 py-8">
      <h1 className="text-lg font-semibold text-white flex items-center gap-2 mb-1">
        <Table2 size={17} className="text-blue-400" />
        Site Actions Registry
      </h1>
      <p className="text-xs text-slate-500 mb-5">
        Super-Admin editor mapping target site selectors & DOM interaction rules that power the
        database-driven action engine.
      </p>

      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 mb-6">
        <div className="grid grid-cols-2 gap-2 mb-2">
          <input
            data-testid="sa-site-name"
            value={draft.site_name}
            onChange={(e) => setField('site_name', e.target.value)}
            placeholder="Site name (e.g. Example Dashboard)"
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <input
            data-testid="sa-url-pattern"
            value={draft.url_pattern}
            onChange={(e) => setField('url_pattern', e.target.value)}
            placeholder="URL pattern (e.g. https://example.com/*)"
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <input
            data-testid="sa-action-name"
            value={draft.action_name}
            onChange={(e) => setField('action_name', e.target.value)}
            placeholder="Action name (e.g. login_submit)"
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <input
            data-testid="sa-selector"
            value={draft.selector}
            onChange={(e) => setField('selector', e.target.value)}
            placeholder="CSS/XPath selector (e.g. #submit-btn)"
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <select
            data-testid="sa-action-type"
            value={draft.action_type}
            onChange={(e) => setField('action_type', e.target.value)}
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white outline-none focus:border-blue-500/50"
          >
            {ACTION_TYPES.map((t) => (
              <option key={t} value={t} className="bg-slate-900">
                {t}
              </option>
            ))}
          </select>
          <input
            data-testid="sa-notes"
            value={draft.notes}
            onChange={(e) => setField('notes', e.target.value)}
            placeholder="Notes (optional)"
            className="rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
        </div>
        <div className="flex items-center justify-between">
          <label className="flex items-center gap-2 text-xs text-slate-400">
            <input
              type="checkbox"
              checked={draft.enabled}
              onChange={(e) => setField('enabled', e.target.checked)}
              className="accent-blue-500"
            />
            Enabled
          </label>
          <div className="flex items-center gap-2">
            {editingId != null && (
              <button
                onClick={resetForm}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] transition-colors"
              >
                <X size={12} />
                Cancel
              </button>
            )}
            <button
              data-testid="sa-save-btn"
              onClick={handleSave}
              disabled={
                !draft.site_name.trim() ||
                !draft.url_pattern.trim() ||
                !draft.selector.trim() ||
                saving
              }
              className="flex items-center gap-2 px-4 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
            >
              {saving ? (
                <Loader2 size={12} className="animate-spin" />
              ) : editingId != null ? (
                <Check size={12} />
              ) : (
                <Plus size={12} />
              )}
              {editingId != null ? 'Update rule' : 'Add rule'}
            </button>
          </div>
        </div>
      </div>

      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}

      {loading ? (
        <div className="flex justify-center py-10 text-slate-500">
          <Loader2 size={18} className="animate-spin" />
        </div>
      ) : actions.length === 0 ? (
        <p className="text-sm text-slate-500 text-center py-8">No site actions defined yet.</p>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-white/[0.06]">
          <table className="w-full text-left text-xs">
            <thead className="bg-white/[0.03] text-slate-400">
              <tr>
                <th className="px-3 py-2 font-medium">Site</th>
                <th className="px-3 py-2 font-medium">URL pattern</th>
                <th className="px-3 py-2 font-medium">Action</th>
                <th className="px-3 py-2 font-medium">Selector</th>
                <th className="px-3 py-2 font-medium">Type</th>
                <th className="px-3 py-2 font-medium">On</th>
                <th className="px-3 py-2" />
              </tr>
            </thead>
            <tbody>
              {actions.map((a) => (
                <tr
                  key={a.id}
                  data-testid="sa-row"
                  className="border-t border-white/[0.06] text-slate-200"
                >
                  <td className="px-3 py-2">{a.site_name}</td>
                  <td className="px-3 py-2 font-mono text-slate-400 truncate max-w-[160px]">
                    {a.url_pattern}
                  </td>
                  <td className="px-3 py-2">{a.action_name}</td>
                  <td className="px-3 py-2 font-mono text-slate-400 truncate max-w-[140px]">
                    {a.selector}
                  </td>
                  <td className="px-3 py-2">{a.action_type}</td>
                  <td className="px-3 py-2">{a.enabled ? '✓' : '—'}</td>
                  <td className="px-3 py-2">
                    <div className="flex items-center gap-1 justify-end">
                      <button
                        aria-label="Edit action"
                        onClick={() => handleEdit(a)}
                        className="p-1.5 rounded text-slate-500 hover:text-blue-400 transition-colors"
                      >
                        <Pencil size={12} />
                      </button>
                      <button
                        aria-label="Delete action"
                        onClick={() => handleDelete(a.id)}
                        className="p-1.5 rounded text-slate-500 hover:text-rose-400 transition-colors"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

```