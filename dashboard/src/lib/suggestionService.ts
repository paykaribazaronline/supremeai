import { useState, useEffect } from 'react';
import { authUtils } from './authUtils';

export interface AISuggestion {
  proposalId: string;
  title: string;
  description: string;
  category: string;
  impact: 'performance' | 'security' | 'cost' | 'capability';
  confidence: number;
  autoExecutable: boolean;
  isApproved: boolean;
  timestamp: number | string;
}

const API_BASE = (import.meta.env.VITE_API_URL || '') + '/api/admin/improvements';

// Listeners for state updates
const listeners: Set<(suggestions: AISuggestion[]) => void> = new Set();
let currentSuggestions: AISuggestion[] = [];

export const suggestionService = {
  fetchSuggestions: async () => {
    try {
      const response = await authUtils.fetchWithAuth(`${API_BASE}/pending`);
      if (!response.ok) throw new Error('Failed to fetch suggestions');
      const result = await response.json();
      
      if (result.success) {
        currentSuggestions = result.data.pending.map((s: any) => ({
          ...s,
          id: s.proposalId,
          impact: s.category === 'IMMUNITY_SYSTEM' ? 'security' : 
                  s.category === 'KNOWLEDGE_BASE' ? 'capability' : 'performance',
          confidence: 0.95,
          autoExecutable: true
        }));
        notify();
      }
    } catch (error) {
      console.error('Failed to fetch suggestions:', error);
    }
    return currentSuggestions;
  },

  getSuggestions: () => currentSuggestions,
  
  approve: async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`${API_BASE}/approve/${id}`, {
        method: 'POST'
      });
      if (!response.ok) throw new Error('Approval failed');
      const result = await response.json();
      
      if (result.success) {
        currentSuggestions = currentSuggestions.filter(s => s.proposalId !== id);
        notify();
        return true;
      }
    } catch (error) {
      console.error('Failed to approve suggestion:', error);
    }
    return false;
  },
  
  decline: async (id: string) => {
    try {
      const response = await authUtils.fetchWithAuth(`${API_BASE}/reject/${id}`, {
        method: 'POST'
      });
      if (!response.ok) throw new Error('Rejection failed');
      const result = await response.json();
      
      if (result.success) {
        currentSuggestions = currentSuggestions.filter(s => s.proposalId !== id);
        notify();
        return true;
      }
    } catch (error) {
      console.error('Failed to reject suggestion:', error);
    }
    return false;
  },
  
  subscribe: (listener: (suggestions: AISuggestion[]) => void): (() => void) => {
    listeners.add(listener);
    // Initial fetch
    suggestionService.fetchSuggestions();
    return () => listeners.delete(listener);
  }
};

function notify() {
  listeners.forEach(l => l([...currentSuggestions]));
}

export const useAISuggestions = () => {
  const [suggestions, setSuggestions] = useState<AISuggestion[]>(currentSuggestions);

  useEffect(() => {
    const unsubscribe = suggestionService.subscribe(setSuggestions);
    // Refresh periodically
    const interval = setInterval(() => {
      suggestionService.fetchSuggestions();
    }, 30000);
    
    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  return {
    suggestions,
    approve: suggestionService.approve,
    decline: suggestionService.decline,
    count: suggestions.length,
    refresh: suggestionService.fetchSuggestions
  };
};
