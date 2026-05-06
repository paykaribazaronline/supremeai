"use client";

import Link from "next/link";
import { formatDistanceToNow } from "date-fns";

interface LibraryCardProps {
  item: {
    id: string;
    repo_full_name: string;
    prompt: string;
    created_at: string;
    view_count?: number;
  };
}

export default function LibraryCard({ item }: LibraryCardProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    try {
      await navigator.clipboard.writeText(item.prompt);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error("Failed to copy:", err);
    }
  };

  const createdAt = new Date(item.created_at);

  return (
    <div className="p-6 bg-white border-4 border-zinc-900 rounded-lg shadow-offset card-hover">
      {/* Header */}
      <div className="flex justify-between items-start mb-4">
        <Link
          href={`/${item.repo_full_name}`}
          className="font-bold text-lg hover:text-accent transition-colors"
        >
          {item.repo_full_name}
        </Link>

        <button
          onClick={handleCopy}
          className="px-3 py-1 text-xs font-bold bg-accent text-white border-2 border-zinc-900 rounded hover:bg-button-hover transition-colors"
        >
          {copied ? "Copied!" : "Copy"}
        </button>
      </div>

      {/* Prompt Preview */}
      <p className="text-sm text-gray-600 line-clamp-4 mb-4">
        {item.prompt.substring(0, 200)}
        {item.prompt.length > 200 && "..."}
      </p>

      {/* Footer */}
      <div className="flex justify-between items-center text-xs text-gray-500 pt-4 border-t-2 border-gray-100">
        <span>
          {formatDistanceToNow(createdAt, { addSuffix: true })}
        </span>
        <div className="flex items-center gap-3">
          {item.view_count !== undefined && (
            <span className="flex items-center gap-1">
              <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path
                  fillRule="evenodd"
                  d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z"
                  clipRule="evenodd"
                />
              </svg>
              {item.view_count}
            </span>
          )}
          <span>{item.prompt.split(/\s+/).length} words</span>
        </div>
      </div>
    </div>
  );
}
