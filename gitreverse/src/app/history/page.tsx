"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { Trash2 } from "lucide-react";

interface HistoryItem {
  url: string;
  prompt: string;
  timestamp: number;
}

export default function HistoryPage() {
  const [history, setHistory] = useState<HistoryItem[]>([]);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = () => {
    try {
      const stored = localStorage.getItem("gitreverse_history");
      if (stored) {
        const items = JSON.parse(stored);
        setHistory(items.slice(0, 20)); // Last 20 entries
      }
    } catch (err) {
      console.error("Failed to load history:", err);
    }
  };

  const clearHistory = () => {
    localStorage.removeItem("gitreverse_history");
    setHistory([]);
  };

  const removeItem = (timestamp: number) => {
    const updated = history.filter((item) => item.timestamp !== timestamp);
    setHistory(updated);
    localStorage.setItem("gitreverse_history", JSON.stringify(updated));
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const getRepoName = (url: string) => {
    const match = url.match(/github\.com\/([^/]+\/[^/]+)/);
    return match ? match[1] : url;
  };

  return (
    <main className="max-w-4xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-4xl font-bold mb-2">
            Your <span className="text-accent">History</span>
          </h1>
          <p className="text-gray-600">
            Last 20 repositories you've reversed
          </p>
        </div>
        {history.length > 0 && (
          <button
            onClick={clearHistory}
            className="px-4 py-2 text-red-600 hover:text-red-800 font-medium flex items-center gap-2"
          >
            <Trash2 className="w-4 h-4" />
            Clear All
          </button>
        )}
      </div>

      {/* Empty State */}
      {history.length === 0 && (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🕐</div>
          <h3 className="text-2xl font-bold mb-2">No History Yet</h3>
          <p className="text-gray-600 max-w-md mx-auto">
            Your recently reversed repositories will appear here. Start by
            generating a prompt from any GitHub repo.
          </p>
          <Link
            href="/"
            className="inline-block mt-6 px-6 py-3 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-button-hover btn-hover-effect"
          >
            Go to Home
          </Link>
        </div>
      )}

      {/* History List */}
      {history.length > 0 && (
        <div className="space-y-4">
          {history.map((item) => (
            <div
              key={item.timestamp}
              className="p-4 bg-white border-4 border-zinc-900 rounded-lg shadow-offset hover:shadow-lg transition-shadow"
            >
              <div className="flex justify-between items-start gap-4">
                <div className="flex-1 min-w-0">
                  <Link
                    href={`/${getRepoName(item.url).replace(
                      /\//g,
                      "/"
                    )}`}
                    className="font-bold text-lg hover:text-accent transition-colors truncate block"
                  >
                    {getRepoName(item.url)}
                  </Link>
                  <p className="text-sm text-gray-600 mt-1">
                    {formatDate(item.timestamp)}
                  </p>
                  <p className="text-sm text-gray-500 mt-2 line-clamp-2">
                    {item.prompt.substring(0, 150)}...
                  </p>
                </div>
                <button
                  onClick={() => removeItem(item.timestamp)}
                  className="p-2 text-gray-400 hover:text-red-600 transition-colors"
                  title="Remove from history"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
