"use client";

import { useState } from "react";

interface RepoInputProps {
  onSubmit: (url: string, focus?: string) => void;
}

export default function RepoInput({ onSubmit }: RepoInputProps) {
  const [url, setUrl] = useState("");
  const [focus, setFocus] = useState("");
  const [showFocus, setShowFocus] = useState(false);
  const [showPat, setShowPat] = useState(false);
  const [pat, setPat] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (url.trim()) {
      onSubmit(url.trim(), focus.trim() || undefined);
    }
  };

  const handleExampleClick = (exampleUrl: string) => {
    setUrl(exampleUrl);
  };

  return (
    <div className="max-w-3xl mx-auto">
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Main Input with Shadow */}
        <div className="relative">
          <div className="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
          <div className="relative flex gap-2">
            <input
              type="text"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="Paste GitHub URL or owner/repo (e.g., fastapi/fastapi)"
              className="flex-1 px-6 py-4 text-lg border-4 border-zinc-900 rounded-lg bg-input-bg focus:outline-none focus:ring-2 focus:ring-accent font-mono"
              required
            />
            <button
              type="submit"
              className="px-8 py-4 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-button-hover btn-hover-effect whitespace-nowrap"
            >
              Reverse
            </button>
          </div>
        </div>

        {/* Options Row */}
        <div className="flex flex-wrap gap-4 items-center">
          {/* Private Repo */}
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={showPat}
              onChange={(e) => setShowPat(e.target.checked)}
              className="w-5 h-5"
            />
            <span className="text-sm font-medium">Private repo</span>
          </label>

          {/* Manual Focus Mode */}
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={showFocus}
              onChange={(e) => setShowFocus(e.target.checked)}
              className="w-5 h-5"
            />
            <span className="text-sm font-medium">Manual focus mode</span>
          </label>
        </div>

        {/* PAT Input */}
        {showPat && (
          <div className="relative">
            <div className="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
            <input
              type="password"
              value={pat}
              onChange={(e) => setPat(e.target.value)}
              placeholder="GitHub PAT (ghp_...)"
              className="relative w-full px-4 py-3 border-4 border-zinc-900 rounded-lg bg-input-bg font-mono text-sm"
            />
          </div>
        )}

        {/* Focus Input */}
        {showFocus && (
          <div className="relative">
            <div className="absolute inset-0 translate-x-2 translate-y-2 bg-zinc-900 rounded-lg"></div>
            <textarea
              value={focus}
              onChange={(e) => setFocus(e.target.value)}
              placeholder="Describe what specific aspect you want to focus on..."
              className="relative w-full px-4 py-3 border-4 border-zinc-900 rounded-lg bg-input-bg min-h-[100px] resize-y"
            />
          </div>
        )}

        {/* Example Repos */}
        <div className="flex flex-wrap gap-2 items-center text-sm">
          <span className="text-gray-600">Try:</span>
          <button
            type="button"
            onClick={() => handleExampleClick("https://github.com/fastapi/fastapi")}
            className="text-accent hover:underline font-medium"
          >
            fastapi/fastapi
          </button>
          <button
            type="button"
            onClick={() => handleExampleClick("https://github.com/django/django")}
            className="text-accent hover:underline font-medium"
          >
            django/django
          </button>
          <button
            type="button"
            onClick={() => handleExampleClick("https://github.com/pallets/flask")}
            className="text-accent hover:underline font-medium"
          >
            pallets/flask
          </button>
        </div>
      </form>
    </div>
  );
}
