// Chat API Service for SupremeAI 2.0
// বাংলা মонтаব্য: চ্যাট ইন্টারফেস ও স্ট্রিমিং এপিআই এর সাথে যোগাযোগের জন্য ব্যবহৃত সার্ভিস। Prompt-to-Action সাপোর্ট সহ।

import { apiClient } from './apiClient';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  action?: {
    type: string;
    target?: string;
    label?: string;
    icon?: string;
    confidence?: number;
    requires_confirmation?: boolean;
    payload?: Record<string, unknown>;
  };
}

export interface ChatResponse {
  response: string;
  tokens_used?: number;
  provider?: string;
  duration?: number;
  action?: {
    type: string;
    target?: string;
    label?: string;
    icon?: string;
    confidence?: number;
    requires_confirmation?: boolean;
    payload?: Record<string, unknown>;
  };
}

// বাংলা মন্তব্য: ফাংশন ডিক্লেয়ারেশন সিনট্যাক্স এরর ঠিক করা হলো
export async function sendMessageStream(
  message: string,
  onToken: (token: string) => void,
  onDone: (action?: ChatResponse['action']) => void,
  onError: (error: string) => void,
  abortSignal?: AbortSignal,
): Promise<void> {
  const API_BASE = import.meta.env.VITE_API_BASE || '';
  try {
    const res = await fetch(`${API_BASE}/api/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message }),
      signal: abortSignal,
    });

    if (!res.ok) {
      onError(`HTTP ${res.status}: ${res.statusText}`);
      return;
    }

    const reader = res.body?.getReader();
    if (!reader) {
      onError('No stream body available');
      return;
    }

    const decoder = new TextDecoder();
    let fullText = '';

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split('\n');
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const payload = line.slice(6).trim();
          if (payload === '[DONE]') continue;
          try {
            const parsed = JSON.parse(payload);
            if (parsed.token) {
              fullText += parsed.token;
              onToken(parsed.token);
            }
          } catch {
            // flat token fallback
            fullText += payload;
            onToken(payload);
          }
        }
      }
    }

    // Prompt-to-Action metadata fallback (legacy path only)
    try {
      const actionRes = await fetch(`${API_BASE}/api/chat/prompt-action`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
      });
      if (actionRes.ok) {
        const actionData = await actionRes.json();
        onDone(actionData.action);
      } else {
        onDone(undefined);
      }
    } catch {
      onDone(undefined);
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      onError(err.message);
    }
  }
};

export const chatService = {
  sendMessage: async (message: string, history: ChatMessage[] = []): Promise<ChatResponse> => {
    return apiClient.post<ChatResponse>('/api/task/execute', {
      message,
      history,
    });
  },

  sendMessageStream,

  getVoices: async (): Promise<any[]> => {
    return apiClient.get<any[]>('/api/voice/voices');
  },
};
