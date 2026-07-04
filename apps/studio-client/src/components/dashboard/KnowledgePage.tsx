// বাংলা মন্তব্য: Devin-স্টাইল Knowledge পেজ — ব্যাকএন্ড /api/knowledge দিয়ে নলেজ সার্চ ও সিড করা হয়
import { useState } from 'react';
import { Search, BookOpen, Database, Loader2 } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

interface KnowledgeResult {
  id: string;
  title: string;
  content: string;
  score?: number | null;
  source?: string | null;
}

export function KnowledgePage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<KnowledgeResult[]>([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [seeding, setSeeding] = useState(false);
  const [status, setStatus] = useState('');

  const handleSearch = async () => {
    if (!query.trim() || loading) return;
    setLoading(true);
    setStatus('');
    try {
      const res = await apiClient.get<KnowledgeResult[]>(
        `/api/knowledge/search?query=${encodeURIComponent(query.trim())}&limit=10`
      );
      setResults(Array.isArray(res) ? res : []);
      setSearched(true);
    } catch (error) {
      setStatus(`Search failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  };

  // বাংলা মন্তব্য: নলেজ বেস ইনডেক্স/সিড করার হ্যান্ডলার
  const handleSeed = async () => {
    if (seeding) return;
    setSeeding(true);
    setStatus('');
    try {
      await apiClient.post('/api/knowledge/seed');
      setStatus('Knowledge base seeded successfully.');
    } catch (error) {
      setStatus(`Seed failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    } finally {
      setSeeding(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-6 py-8">
      <div className="flex items-center justify-between mb-1">
        <h1 className="text-lg font-semibold text-white">Knowledge</h1>
        <button
          data-testid="seed-knowledge-btn"
          onClick={handleSeed}
          disabled={seeding}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 text-xs text-slate-300 hover:bg-white/[0.05] disabled:opacity-50 transition-colors"
        >
          {seeding ? <Loader2 size={12} className="animate-spin" /> : <Database size={12} />}
          Seed knowledge base
        </button>
      </div>
      <p className="text-xs text-slate-400 mb-6">
        Search the indexed knowledge base that powers SupremeAI's answers.
      </p>

      <div className="flex items-center gap-2 mb-6">
        <div className="flex-1 flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.03] px-3 py-2 focus-within:border-blue-500/50 transition-colors">
          <Search size={14} className="text-slate-400" />
          <input
            data-testid="knowledge-search-input"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="Search knowledge..."
            className="flex-1 bg-transparent text-sm text-white placeholder-slate-500 outline-none"
          />
        </div>
        <button
          data-testid="knowledge-search-btn"
          onClick={handleSearch}
          disabled={!query.trim() || loading}
          className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 text-white text-xs font-medium transition-colors"
        >
          {loading ? <Loader2 size={12} className="animate-spin" /> : 'Search'}
        </button>
      </div>

      {status && <p className="text-xs text-slate-400 mb-4">{status}</p>}

      {searched && results.length === 0 && !loading && (
        <p className="text-sm text-slate-400 text-center py-8">No results found.</p>
      )}

      <ul className="flex flex-col gap-3">
        {results.map((r) => (
          <li key={r.id} className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-4">
            <div className="flex items-center gap-2 mb-1.5">
              <BookOpen size={13} className="text-blue-400" />
              <h3 className="text-xs font-medium text-white flex-1 truncate">{r.title}</h3>
              {typeof r.score === 'number' && (
                <span className="text-[10px] text-slate-400">score {r.score.toFixed(2)}</span>
              )}
            </div>
            <p className="text-[11px] text-slate-400 line-clamp-3 whitespace-pre-wrap">{r.content}</p>
            {r.source && <p className="text-[10px] text-slate-600 mt-1.5">source: {r.source}</p>}
          </li>
        ))}
      </ul>
    </div>
  );
}
