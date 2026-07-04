# 📄 ফাইল: apps/studio-client/src/components/dashboard/VaultPage.tsx

**প্রকার:** .tsx  
**সাইজ:** 8,675 বাইট  
**আপডেট:** 2026-07-04T05:33:40.965801

---

## কোড

```tsx
// বাংলা মন্তব্য: Target Web Authorization Vault UI — ইউজার টার্গেট সাইটের সেশন কুকি/টোকেন
// ইমপোর্ট করতে, সেশন সিঙ্ক ট্রিগার করতে এবং কানেকশন স্ট্যাটাস (Connected/Expired) দেখতে পারেন।
// র‌্যাশ ক্রেডেনশিয়াল কখনো UI-তে দেখানো হয় না — ব্যাকএন্ড masked মান রিটার্ন করে।
import { useState, useEffect, useCallback } from 'react';
import { ShieldCheck, Plus, Trash2, RefreshCw, Loader2, CircleCheck, CircleAlert } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface VaultCredential {
  id: string;
  serviceName: string;
  username: string;
  // বাংলা মন্তব্য: ব্যাকএন্ড থেকে masked মান আসে (যেমন ***masked***), কাঁচা টোকেন নয়
  password?: string;
  token?: string;
}

interface SurfStatus {
  browsing: boolean;
  currentUrl?: string;
}

export function VaultPage() {
  const [creds, setCreds] = useState<VaultCredential[]>([]);
  const [status, setStatus] = useState<SurfStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [serviceName, setServiceName] = useState('');
  const [username, setUsername] = useState('');
  const [secret, setSecret] = useState('');
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    Promise.all([
      apiClient.get<{ credentials: VaultCredential[] }>('/api/browser/credentials?userId=default'),
      apiClient.get<SurfStatus>('/api/browser/surf/status'),
    ])
      .then(([c, s]) => {
        setCreds(c.credentials || []);
        setStatus(s);
        setError('');
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load vault'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // বাংলা মন্তব্য: নতুন সেশন কুকি/টোকেন ভল্টে সংরক্ষণ (এনক্রিপ্টেড হয়ে ব্যাকএন্ডে যায়)
  const handleImport = async () => {
    if (!serviceName.trim() || !secret.trim() || saving) return;
    setSaving(true);
    setError('');
    try {
      await apiClient.post('/api/browser/credentials', {
        serviceName: serviceName.trim(),
        username: username.trim() || 'session',
        password: secret.trim(),
        userId: 'default',
      });
      setServiceName('');
      setUsername('');
      setSecret('');
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to import session');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await apiClient.delete(`/api/browser/credentials/${id}`);
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove credential');
    }
  };

  // বাংলা মন্তব্য: সেশন সিঙ্ক ট্রিগার — হেডলেস ব্রাউজার সার্ফ শুরু করে কানেকশন যাচাই করে
  const handleSync = async () => {
    setSyncing(true);
    setError('');
    try {
      await apiClient.post('/api/browser/surf/start');
      refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sync failed');
    } finally {
      setSyncing(false);
    }
  };

  const connected = status?.browsing;

  return (
    <div className="max-w-2xl mx-auto px-6 py-8">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-lg font-semibold text-white flex items-center gap-2">
          <ShieldCheck size={17} className="text-blue-400" />
          Web Authorization Vault
        </h1>
        <button
          data-testid="vault-sync-btn"
          onClick={handleSync}
          disabled={syncing}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] disabled:opacity-50 transition-colors"
        >
          {syncing ? <Loader2 size={12} className="animate-spin" /> : <RefreshCw size={12} />}
          Sync session
        </button>
      </div>
      <p className="text-xs text-slate-500 mb-5">
        Import target site session tokens/cookies for the boundless automation agent. Raw
        credentials are encrypted and never displayed.
      </p>

      <div
        data-testid="vault-connection-status"
        className={`flex items-center gap-2 rounded-lg px-3 py-2 mb-5 text-xs ${
          connected
            ? 'border border-emerald-500/30 bg-emerald-500/[0.06] text-emerald-300'
            : 'border border-amber-500/30 bg-amber-500/[0.06] text-amber-300'
        }`}
      >
        {connected ? <CircleCheck size={13} /> : <CircleAlert size={13} />}
        {connected ? 'Connected — active browser session' : 'Expired — no active session'}
      </div>

      <div className="rounded-xl border border-white/[0.08] bg-white/[0.02] p-4 mb-6 flex flex-col gap-2">
        <div className="flex gap-2">
          <input
            data-testid="vault-service"
            value={serviceName}
            onChange={(e) => setServiceName(e.target.value)}
            placeholder="Target site (e.g. example.com)"
            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <input
            data-testid="vault-username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Label / username (optional)"
            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
        </div>
        <div className="flex gap-2">
          <input
            data-testid="vault-secret"
            type="password"
            value={secret}
            onChange={(e) => setSecret(e.target.value)}
            placeholder="Paste session cookie / storage token"
            className="flex-1 rounded-lg bg-black/30 border border-white/10 px-3 py-2 text-xs text-white placeholder-slate-500 outline-none focus:border-blue-500/50"
          />
          <button
            data-testid="vault-import-btn"
            onClick={handleImport}
            disabled={!serviceName.trim() || !secret.trim() || saving}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
          >
            {saving ? <Loader2 size={12} className="animate-spin" /> : <Plus size={12} />}
            Import
          </button>
        </div>
      </div>

      {error && <p className="text-xs text-rose-400 mb-4">{error}</p>}

      {loading ? (
        <div className="flex justify-center py-10 text-slate-500">
          <Loader2 size={18} className="animate-spin" />
        </div>
      ) : creds.length === 0 ? (
        <p className="text-sm text-slate-500 text-center py-8">No stored sessions yet.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {creds.map((c) => (
            <li
              key={c.id}
              data-testid="vault-row"
              className="flex items-center gap-3 p-3 rounded-lg border border-white/[0.06] bg-white/[0.02]"
            >
              <ShieldCheck size={14} className="text-slate-400" />
              <div className="flex-1 min-w-0">
                <p className="text-xs text-white truncate">{c.serviceName}</p>
                <p className="text-[11px] text-slate-500 font-mono truncate">
                  {c.username} · {c.password || c.token || '***masked***'}
                </p>
              </div>
              <button
                aria-label="Remove session"
                onClick={() => handleDelete(c.id)}
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