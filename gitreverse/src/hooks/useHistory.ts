import { useState, useEffect } from "react";

const HISTORY_KEY = "gitreverse_history";
const MAX_HISTORY = 20;

export interface HistoryItem {
  url: string;
  prompt: string;
  timestamp: number;
}

export function useHistory() {
  const [history, setHistory] = useState<HistoryItem[]>([]);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = () => {
    try {
      const stored = localStorage.getItem(HISTORY_KEY);
      if (stored) {
        const items: HistoryItem[] = JSON.parse(stored);
        setHistory(items.slice(0, MAX_HISTORY));
      }
    } catch (err) {
      console.error("Failed to load history:", err);
    }
  };

  const saveToHistory = (url: string, prompt: string) => {
    try {
      const newItem: HistoryItem = {
        url,
        prompt,
        timestamp: Date.now(),
      };

      const updated = [newItem, ...history.filter((item) => item.url !== url)].slice(
        0,
        MAX_HISTORY
      );

      setHistory(updated);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    } catch (err) {
      console.error("Failed to save to history:", err);
    }
  };

  const removeFromHistory = (timestamp: number) => {
    try {
      const updated = history.filter((item) => item.timestamp !== timestamp);
      setHistory(updated);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
    } catch (err) {
      console.error("Failed to remove from history:", err);
    }
  };

  const clearHistory = () => {
    try {
      setHistory([]);
      localStorage.removeItem(HISTORY_KEY);
    } catch (err) {
      console.error("Failed to clear history:", err);
    }
  };

  return {
    history,
    saveToHistory,
    removeFromHistory,
    clearHistory,
    loadHistory,
  };
}

export function saveToHistory(url: string, prompt: string) {
  try {
    const newItem: HistoryItem = {
      url,
      prompt,
      timestamp: Date.now(),
    };

    const stored = localStorage.getItem(HISTORY_KEY);
    const existing: HistoryItem[] = stored ? JSON.parse(stored) : [];

    const updated = [newItem, ...existing.filter((item) => item.url !== url)].slice(
      0,
      MAX_HISTORY
    );

    localStorage.setItem(HISTORY_KEY, JSON.stringify(updated));
  } catch (err) {
    console.error("Failed to save to history:", err);
  }
}
