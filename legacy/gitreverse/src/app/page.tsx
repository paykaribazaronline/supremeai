"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import RepoInput from "@/components/RepoInput";
import LoadingSpinner from "@/components/LoadingSpinner";
import ResultDisplay from "@/components/ResultDisplay";
import { saveToHistory } from "@/hooks/useHistory";

const FLAVOR_TEXTS = [
  "Gathering metadata...",
  "Shaping prompt...",
  "Consulting the AI oracle...",
  "Crafting the perfect prompt...",
  "Analyzing repository structure...",
  "Understanding the codebase...",
  "Generating insights...",
];

export default function HomePage() {
  const [loading, setLoading] = useState(false);
  const [flavorIndex, setFlavorIndex] = useState(0);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [repoUrl, setRepoUrl] = useState("");
  const router = useRouter();

  // Rotate flavor text every 450ms
  useEffect(() => {
    if (!loading) return;
    const interval = setInterval(() => {
      setFlavorIndex((prev) => (prev + 1) % FLAVOR_TEXTS.length);
    }, 450);
    return () => clearInterval(interval);
  }, [loading]);

  const handleSubmit = async (url: string, focus?: string) => {
    setLoading(true);
    setError(null);
    setRepoUrl(url);

    try {
      const response = await fetch("/api/reverse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, focus }),
      });

      if (!response.ok) {
        if (response.status === 429 || response.status === 503) {
          const data = await response.json();
          setError(
            data.message ||
              "Service temporarily unavailable. Please check our library for existing prompts."
          );
          return;
        }
        throw new Error("Failed to generate prompt");
      }

      const data = await response.json();
      setResult(data.prompt);

      // Save to history
      saveToHistory(url, data.prompt);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error occurred");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main>
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 py-12">
        {/* Logo/Title */}
        <div className="text-center mb-12">
          <h1 className="text-5xl font-bold mb-4">
            Git<span className="text-accent">Reverse</span>
          </h1>
          <p className="text-xl text-gray-600">
            Paste a repo, get a prompt to recreate it from scratch
          </p>
        </div>

        {/* Input Form */}
        <div className="mb-8">
          <RepoInput onSubmit={handleSubmit} />
        </div>

        {/* Loading State */}
        {loading && (
          <div className="mt-12">
            <LoadingSpinner flavorText={FLAVOR_TEXTS[flavorIndex]} />
          </div>
        )}

        {/* Error State */}
        {error && !loading && (
          <div className="mt-8 p-6 bg-red-50 border-4 border-red-500 rounded-lg shadow-offset">
            <p className="text-red-700 font-medium">{error}</p>
            <Link
              href="/library"
              className="inline-block mt-4 px-6 py-2 bg-accent text-white font-bold border-4 border-zinc-900 rounded-md btn-hover-effect"
            >
              Browse Library Instead
            </Link>
          </div>
        )}

        {/* Result */}
        {result && !loading && (
          <div className="mt-8">
            <ResultDisplay content={result} repoUrl={repoUrl} />
          </div>
        )}

        {/* Features Section */}
        {!result && !loading && (
          <div className="mt-16 grid md:grid-cols-3 gap-6">
            <div className="p-6 bg-white border-4 border-zinc-900 rounded-lg shadow-offset">
              <div className="text-3xl mb-2">📝</div>
              <h3 className="text-xl font-bold mb-2">Natural Language</h3>
              <p className="text-gray-600">
                Get human-readable prompts that capture project intent
              </p>
            </div>
            <div className="p-6 bg-white border-4 border-zinc-900 rounded-lg shadow-offset">
              <div className="text-3xl mb-2">🤖</div>
              <h3 className="text-xl font-bold mb-2">AI-Powered</h3>
              <p className="text-gray-600">
                Uses advanced LLMs to analyze and understand codebases
              </p>
            </div>
            <div className="p-6 bg-white border-4 border-zinc-900 rounded-lg shadow-offset">
              <div className="text-3xl mb-2">⚡</div>
              <h3 className="text-xl font-bold mb-2">Fast & Simple</h3>
              <p className="text-gray-600">
                Paste a URL, click a button, get your prompt
              </p>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
