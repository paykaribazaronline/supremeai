import { create } from 'zustand';
import { getWebSocketBaseUrl } from '../utils/api';

export type SujonState =
  | 'idle'
  | 'scanning'
  | 'executing'
  | 'circuit_open'
  | 'self_healing'
  | 'awaiting_human'
  | 'success'
  | 'failed'
  | 'processing';

export interface LogEntry {
  id: string;
  ts: string;
  log_type: string;
  payload: any;
}

export interface FileNode {
  name: string;
  path: string;
  type: 'file' | 'directory';
  status: 'new' | 'modified' | 'deleted' | 'unchanged';
}

export interface ReasoningEntry {
  id: string;
  ts: string;
  token: string;
}

interface SessionCockpitState {
  sessionId: string | null;
  // We use a normal array but we will cap it at 10,000 in our mutations
  logBuffer: LogEntry[];
  // Zustand isn't great with Maps in reactive state if they mutate often,
  // but for the sake of the store structure we define it.
  // The actual FileTreePanel uses a useRef<Map> for performance.
  fileTreeData: any;
  reasoningChain: ReasoningEntry[];
  agentState: SujonState;
  controlMode: 'agent' | 'human';
  sseRef: EventSource | null;
  wsRef: WebSocket | null;

  resetSessionState: () => void;
  connectSSE: (sessionId: string) => void;
  disconnectSSE: () => void;
  connectTakeoverWS: (sessionId: string, token: string) => void;
  disconnectTakeoverWS: () => void;

  // Buffers
  addLog: (log: LogEntry) => void;
}

const MAX_LOGS = 10000;

export const useSessionCockpitStore = create<SessionCockpitState>((set, get) => ({
  sessionId: null,
  logBuffer: [],
  fileTreeData: null,
  reasoningChain: [],
  agentState: 'idle',
  controlMode: 'agent',
  sseRef: null,
  wsRef: null,

  resetSessionState: () => {
    const { sseRef, wsRef } = get();
    if (sseRef) {
      sseRef.close();
    }
    if (wsRef) {
      wsRef.close();
    }
    set({
      sessionId: null,
      logBuffer: [],
      fileTreeData: null,
      reasoningChain: [],
      agentState: 'idle',
      controlMode: 'agent',
      sseRef: null,
      wsRef: null,
    });
  },

  connectSSE: (sessionId: string) => {
    get().disconnectSSE(); // Ensure previous is closed
    const sse = new EventSource(`/api/session/${sessionId}/stream`);
    sse.onmessage = (event) => {
      try {
        const parsed = JSON.parse(event.data);
        if (parsed.channel === 'logs') {
          get().addLog(parsed.data);
        } else if (parsed.channel === 'state') {
          set({ agentState: parsed.data.current_state });
        }
      } catch (err) {
        console.error("SSE parse error", err);
      }
    };
    set({ sseRef: sse, sessionId });
  },

  disconnectSSE: () => {
    const { sseRef } = get();
    if (sseRef) {
      sseRef.close();
      set({ sseRef: null });
    }
  },

  connectTakeoverWS: (sessionId: string, token: string) => {
    get().disconnectTakeoverWS();
    // বাংলা মন্তব্য: প্রোডাকশন ক্লাউড সার্ভারের সাথে WSS সংযোগ স্থাপনের জন্য getWebSocketBaseUrl ব্যবহার
    const baseUrl = getWebSocketBaseUrl();
    const ws = new WebSocket(`${baseUrl}/ws/session/${sessionId}/takeover?token=${token}`);

    ws.onopen = () => {
      set({ controlMode: 'human' });
    };
    ws.onclose = () => {
      set({ controlMode: 'agent' });
    };
    set({ wsRef: ws });
  },

  disconnectTakeoverWS: () => {
    const { wsRef } = get();
    if (wsRef) {
      wsRef.close();
      set({ wsRef: null });
    }
  },

  addLog: (log: LogEntry) => {
    set((state) => {
      const newBuffer = [...state.logBuffer, log];
      if (newBuffer.length > MAX_LOGS) {
        return { logBuffer: newBuffer.slice(newBuffer.length - MAX_LOGS) };
      }
      return { logBuffer: newBuffer };
    });
  }
}));
