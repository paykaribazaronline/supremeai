"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { getSupabase } from "@/lib/supabase";
import LibraryCard from "@/components/LibraryCard";
import { useDebounce } from "@/hooks/useDebounce";

interface LibraryItem {
  id: string;
  repo_full_name: string;
  prompt: string;
  created_at: string;
  view_count?: number;
}

export default function LibraryPage() {
  const [items, setItems] = useState<LibraryItem[]>([]);
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState<"newest" | "trending" | "oldest">("newest");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const debouncedSearch = useDebounce(search, 300);

  useEffect(() => {
    loadItems();
  }, [debouncedSearch, sort]);

  const loadItems = async () => {
    setLoading(true);
    setError(null);

    try {
      const supabase = getSupabase();
      
      if (!supabase) {
        // Supabase not configured - show empty with message
        setItems([]);
        setHasMore(false);
        setLoading(false);
        return;
      }

      let query = supabase
        .from("quick_reverse_cache")
        .select("*")
        .range(page * 24, (page + 1) * 24 - 1);

      if (debouncedSearch) {
        query = query.ilike("repo_full_name", `%${debouncedSearch}%`);
      }

      switch (sort) {
        case "newest":
          query = query.order("created_at", { ascending: false });
          break;
        case "oldest":
          query = query.order("created_at", { ascending: true });
          break;
        case "trending":
          query = query.order("view_count", { ascending: false });
          break;
      }

      const { data, error: supabaseError } = await query;

      if (supabaseError) {
        throw supabaseError;
      }

      if (page === 0) {
        setItems(data || []);
      } else {
        setItems((prev) => [...prev, ...(data || [])]);
      }

      setHasMore((data || []).length === 24);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load library");
    } finally {
      setLoading(false);
    }
  };

  const loadMore = () => {
    setPage((prev) => prev + 1);
  };

  // Reload when page changes
  useEffect(() => {
    if (page > 0) {
      loadItems();
    }
  }, [page]);

  return (
    <main className="max-w-7xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">
          Prompt <span className="text-accent">Library</span>
        </h1>
        <p className="text-gray-600">
          Browse and discover prompts generated from popular repositories
        </p>
      </div>

      {/* Search and Sort */}
      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <div className="flex-1 relative">
          <div className="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
          <input
            type="text"
            placeholder="Search repositories..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="relative w-full px-4 py-3 border-4 border-zinc-900 rounded-lg bg-input-bg focus:outline-none focus:ring-2 focus:ring-accent"
          />
        </div>
        <div className="relative">
          <div className="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
          <select
            value={sort}
            onChange={(e) => {
              setSort(e.target.value as typeof sort);
              setPage(0);
            }}
            className="relative px-4 py-3 border-4 border-zinc-900 rounded-lg bg-input-bg appearance-none pr-10 cursor-pointer"
          >
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="trending">Most Viewed</option>
          </select>
        </div>
      </div>

      {/* Loading State */}
      {loading && page === 0 && (
        <div className="text-center py-12">
          <div className="inline-block w-12 h-12 border-4 border-accent border-t-transparent rounded-full animate-spin"></div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="p-4 bg-red-50 border-4 border-red-500 rounded-lg mb-8">
          <p className="text-red-700">{error}</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && items.length === 0 && (
        <div className="text-center py-16 px-4">
          {!getSupabase() ? (
            <>
              <div className="text-6xl mb-4">📚</div>
              <h3 className="text-2xl font-bold mb-2">Library Unavailable</h3>
              <p className="text-gray-600 max-w-md mx-auto">
                The prompt library requires Supabase configuration. Add
                SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to enable this
                feature.
              </p>
            </>
          ) : (
            <>
              <div className="text-6xl mb-4">🔍</div>
              <h3 className="text-2xl font-bold mb-2">No Prompts Yet</h3>
              <p className="text-gray-600 max-w-md mx-auto">
                Be the first to create one! Use the home page to generate a
                prompt from any GitHub repository.
              </p>
              <Link
                href="/"
                className="inline-block mt-6 px-6 py-3 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-button-hover btn-hover-effect"
              >
                Generate Prompt
              </Link>
            </>
          )}
        </div>
      )}

      {/* Grid */}
      {items.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((item) => (
            <LibraryCard key={item.id} item={item} />
          ))}
        </div>
      )}

      {/* Load More */}
      {hasMore && items.length > 0 && (
        <div className="text-center mt-12">
          <button
            onClick={loadMore}
            disabled={loading}
            className="px-8 py-3 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-button-hover btn-hover-effect disabled:opacity-50"
          >
            {loading ? "Loading..." : "Load More"}
          </button>
        </div>
      )}
    </main>
  );
}
