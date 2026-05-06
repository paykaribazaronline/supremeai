"use client";

import { useState, useCallback } from "react";

interface ResultDisplayProps {
  content: string;
  repoUrl: string;
}

export default function ResultDisplay({ content, repoUrl }: ResultDisplayProps) {
  const [copied, setCopied] = useState(false);
  const [showStats, setShowStats] = useState(false);

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error("Failed to copy:", err);
    }
  }, [content]);

  // Calculate stats
  const wordCount = content.split(/\s+/).filter(Boolean).length;
  const charCount = content.length;
  const estimatedTokens = Math.ceil(charCount / 4);

  return (
    <div className="space-y-6">
      {/* Stats Bar */}
      <div className="p-4 bg-white border-4 border-zinc-900 rounded-lg shadow-offset">
        <div className="flex flex-wrap gap-6 items-center justify-between">
          <div className="flex gap-6 text-sm">
            <div>
              <span className="text-gray-600">Words:</span>{" "}
              <span className="font-bold">{wordCount.toLocaleString()}</span>
            </div>
            <div>
              <span className="text-gray-600">Characters:</span>{" "}
              <span className="font-bold">{charCount.toLocaleString()}</span>
            </div>
            <div>
              <span className="text-gray-600">Est. Tokens:</span>{" "}
              <span className="font-bold">{estimatedTokens.toLocaleString()}</span>
            </div>
          </div>
          <button
            onClick={() => setShowStats(!showStats)}
            className="text-sm text-accent hover:underline"
          >
            {showStats ? "Hide Details" : "Show Details"}
          </button>
        </div>

        {/* Additional Stats */}
        {showStats && (
          <div className="mt-4 pt-4 border-t-2 border-gray-200">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div className="p-3 bg-input-bg rounded border-2 border-zinc-900">
                <div className="text-gray-600">Source</div>
                <div className="font-mono text-xs mt-1 truncate">{repoUrl}</div>
              </div>
              <div className="p-3 bg-input-bg rounded border-2 border-zinc-900">
                <div className="text-gray-600">Lines</div>
                <div className="font-bold">{content.split("\n").length}</div>
              </div>
              <div className="p-3 bg-input-bg rounded border-2 border-zinc-900">
                <div className="text-gray-600">Paragraphs</div>
                <div className="font-bold">
                  {content.split("\n\n").filter(Boolean).length}
                </div>
              </div>
              <div className="p-3 bg-input-bg rounded border-2 border-zinc-900">
                <div className="text-gray-600">Avg Words/Para</div>
                <div className="font-bold">
                  {Math.round(
                    wordCount /
                      (content.split("\n\n").filter(Boolean).length || 1)
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex gap-4">
        <button
          onClick={handleCopy}
          className="px-6 py-3 bg-accent text-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-button-hover btn-hover-effect flex items-center gap-2"
        >
          {copied ? (
            <>
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
              Copied!
            </>
          ) : (
            <>
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3"
                />
              </svg>
              Copy to Clipboard
            </>
          )}
        </button>

        <a
          href={`https://github.com/${repoUrl
            .replace("https://github.com/", "")
            .replace(/\/$/, "")}`}
          target="_blank"
          rel="noopener noreferrer"
          className="px-6 py-3 bg-white font-bold border-4 border-zinc-900 rounded-lg hover:bg-gray-50 btn-hover-effect flex items-center gap-2"
        >
          <svg
            className="w-5 h-5"
            viewBox="0 0 16 16"
            fill="currentColor"
          >
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" />
          </svg>
          View on GitHub
        </a>
      </div>

      {/* Prompt Content */}
      <div className="border-4 border-zinc-900 rounded-lg overflow-hidden shadow-offset">
        <div className="bg-input-bg px-4 py-3 border-b-4 border-zinc-900 flex justify-between items-center">
          <span className="font-bold text-sm">Generated Prompt</span>
          <span className="text-xs text-gray-600">
            Ready to paste into AI assistant
          </span>
        </div>
        <pre className="p-6 text-sm overflow-x-auto bg-background max-h-[70vh] overflow-y-auto">
          <code className="font-mono leading-relaxed">{content}</code>
        </pre>
      </div>

      {/* Tips */}
      <div className="p-4 bg-blue-50 border-2 border-blue-200 rounded-lg">
        <h4 className="font-bold text-blue-900 mb-2">💡 Tips for using this prompt:</h4>
        <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
          <li>Paste this prompt into ChatGPT, Claude, or any AI coding assistant</li>
          <li>
            Add specific requirements like "Use TypeScript" or "Include tests"
          </li>
          <li>
            The prompt captures intent - feel free to modify it before sending
          </li>
        </ul>
      </div>
    </div>
  );
}
