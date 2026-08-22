import { create } from "zustand";
import { UnifiedChatMessage } from '../types/chat';
import { eventBus, Events } from '../lib/eventBus';

const MAX_MESSAGES = 1000;

interface ChatState {
  messages: UnifiedChatMessage[];
  input: string;
  setInput: (text: string) => void;
  addMessage: (msg: Omit<UnifiedChatMessage, "id" | "timestamp">) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  input: "",
  setInput: (text) => set({ input: text }),
  addMessage: (msg) =>
    set((s) => {
      const newMsg: UnifiedChatMessage = {
        ...msg,
        id: crypto.randomUUID ? crypto.randomUUID() : `msg_${Date.now()}_${Math.random()}`,
        timestamp: Date.now(),
      };
      
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
    }),
  clearMessages: () => set({ messages: [] }),
}));
