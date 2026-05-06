"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/router";
import LoadingSpinner from "@/components/LoadingSpinner";
import ResultDisplay from "@/components/ResultDisplay";

interface PageProps {
  params: {
    owner: string;
    repo: string;
  };
}

export default function RepoDetailPage({ params }: PageProps) {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const repoFullName = `${params.owner}/${params.repo}`;

  // Auto-submit if no cached result
  useEffect(() => {
    checkCacheAndSubmit();
  }, []);

  const checkCacheAndSubmit = async () => {
    // Check localStorage for cached result
    try {
      const stored = localStorage.getItem("gitreverse_history");
      if (stored) {
        const history = JSON.parse(stored);
        const cached = history.find(
          (item: any) => item.url.includes(repoFullName)
        );
        if (cached) {
          setResult(cached.prompt);
          return;
        }
      }
    } catch (err) {
      // Ignore cache errors
    }

    // No cache, auto-submit
    await handleSubmit();
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch("/api/reverse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          url: `https://github.com/${repoFullName}`,
        }),
      });

      if (!response.ok) {
        if (response.status === 429 || response.status === 503) {
          const data = await response.json();
          setError(
            data.message || "Service temporarily unavailable"
          );
          return;
        }
        throw new Error("Failed to generate prompt");
      }

      const data = await response.json();
      setResult(data.prompt);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  // Handle GitHub-style URL with tree/branch/path
  useEffect(() => {
    const { path } = router.query;
    if (path) {
      // Handle /owner/repo/tree/branch/path redirects
      // This prevents 404s on GitHub-style URLs
    }
  }, [router.query]);

  return (
    <main>
      <div className="max-w-4xl mx-auto px-4 py-12">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold mb-2">
            {repoFullName}
          </h1>
          <p className="text-gray-600">
            Reversed prompt for this repository
          </p>
        </div>

        {/* Loading State */}
        {loading && (
          <LoadingSpinner flavorText="Reversing repository..." />
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="p-6 bg-red-50 border-4 border-red-500 rounded-lg shadow-offset">
            <p className="text-red-700 font-medium">{error}</p>
          </div>
        )}

        {/* Result */}
        {result && !loading && (
          <ResultDisplay
            content={result}
            repoUrl={`https://github.com/${repoFullName}`}
          />
        )}
      </div>
    </main>
  );
}
