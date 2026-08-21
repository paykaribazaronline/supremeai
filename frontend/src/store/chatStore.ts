// frontend/src/store/chatStore.ts
import { create } from "zustand";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant" | "system";
  content: string;
  ts: number;
}

const MAX_MESSAGES = 1000;

interface ChatState {
  messages: ChatMessage[];
  input: string;
  setInput: (text: string) => void;
  addMessage: (msg: Omit<ChatMessage, "id" | "ts">) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  input: "",
  setInput: (text) => set({ input: text }),
  addMessage: (msg) =>
    set((s) => {
      const newMsg: ChatMessage = {
        ...msg,
        id: crypto.randomUUID ? crypto.randomUUID() : `msg_${Date.now()}_${Math.random()}`,
        ts: Date.now(),
      };
      const msgs = [...s.messages, newMsg];
      return {
        messages: msgs.length > MAX_MESSAGES ? msgs.slice(-MAX_MESSAGES) : msgs,
      };
    }),
  clearMessages: () => set({ messages: [] }),
}));
