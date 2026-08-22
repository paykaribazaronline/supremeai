import { create } from "zustand";
import { UnifiedChatMessage, ChatConversation } from '../types/chat';
import { eventBus, Events } from '../lib/eventBus';
import { apiClient } from '../services/apiClient';

const MAX_MESSAGES = 1000;

interface ChatState {
  conversations: ChatConversation[];
  activeConversationId: string | null;
  messages: UnifiedChatMessage[];
  input: string;
  isLoading: boolean;
  isStreaming: boolean;
  error: string | null;
  setInput: (text: string) => void;
  addMessage: (msg: Omit<UnifiedChatMessage, "id" | "timestamp">) => void;
  clearMessages: () => void;
  loadConversations: () => Promise<void>;
  saveMessage: (message: UnifiedChatMessage) => Promise<void>;
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  messages: [],
  input: "",
  isLoading: false,
  isStreaming: false,
  error: null,
  setInput: (text) => set({ input: text }),

  loadConversations: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await apiClient.get<ChatConversation[]>('/api/memory/conversations');
      set({
        conversations: response.data || [],
        isLoading: false,
      });
      eventBus.emit(Events.METRICS_UPDATE_AVAILABLE, {
        source: 'chat_store_load',
        count: response.data?.length || 0,
        timestamp: Date.now(),
      });
    } catch (e) {
      console.warn('[ChatStore] Could not load history from backend, using local only');
      set({ isLoading: false, error: null });
    }
  },

  saveMessage: async (message: UnifiedChatMessage) => {
    const state = get();
    if (!['user', 'assistant'].includes(message.role)) return;
    try {
      await apiClient.post('/api/memory/conversations/messages', {
        conversation_id: state.activeConversationId,
        message: {
          id: message.id,
          role: message.role,
          content: message.content,
          timestamp: message.timestamp,
          metadata: message.metadata,
        }
      });
    } catch (e) {
      console.warn('[ChatStore] Failed to persist message to backend:', e);
    }
  },

  addMessage: (msg) => {
    const newMsg: UnifiedChatMessage = {
      ...msg,
      id: crypto.randomUUID ? crypto.randomUUID() : `msg_${Date.now()}_${Math.random()}`,
      timestamp: Date.now(),
    };
    
    set((s) => {
      // Emit events for passive listeners (dashboards, metrics, etc)
      if (newMsg.role === 'user') {
        eventBus.emit(Events.CHAT_MESSAGE_SENT, newMsg);
      } else if (newMsg.role === 'assistant') {
        eventBus.emit(Events.CHAT_MESSAGE_RECEIVED, newMsg);
      }
      
      const msgs = [...s.messages, newMsg];
      return {
        messages: msgs.length > MAX_MESSAGES ? msgs.slice(-MAX_MESSAGES) : msgs,
      };
    });
    
    get().saveMessage(newMsg);
  },
  clearMessages: () => set({ messages: [] }),
}));
