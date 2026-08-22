import { create } from "zustand";
// বাংলা মন্তব্য: raw fetch() সরিয়ে apiClient ব্যবহার — auth header সব request এ যাবে
import { apiClient } from '../services/apiClient';
import { AppDefaults } from '../config/constants';
import type { UnifiedChatMessage } from '../types/chat';


type ChatMessage = UnifiedChatMessage;

interface DeployGateInfo {
  // বাংলা মন্তব্য: আগে duplicate status field ছিল — TypeScript compile error। একটি রাখা হলো।
  status: "LOCKED" | "UNLOCKED";
  reason: string;
  updated_at?: string;
}

interface ConfigState {
  systemConfig: typeof AppDefaults;
  isConfigLoaded: boolean;
  setConfig: (config: Partial<typeof AppDefaults>) => void;
}

interface EvolutionState {
  isForging: boolean;
  forgeFeedback: string | null;
  forgeSuccessCode: string | null;

  // ⚡ Evolution Action
  forgeNewSkill: (skillName: string, userDemand: string) => Promise<void>;
}

interface SupremeState extends EvolutionState, ConfigState {
  isServerOnline: boolean;
  sessionId: string | null;
  currentIdempotencyKey: string | null;
  isOrchestrating: boolean;
  chatHistory: ChatMessage[];
  activeTaskType: string;
  executionError: string | null;
  streamLogs: string[];

  // 🛡️ New Autonomous Gate States
  deployGate: DeployGateInfo | null;
  isGateLoading: boolean;

  setServerStatus: (online: boolean) => void;
  initializeSession: (id: string) => void;
  generateIdempotencyKey: () => string;
  addMessage: (message: Omit<ChatMessage, "id" | "timestamp">) => void;
  clearHistory: () => void;
  triggerOrchestration: (active: boolean, error?: string | null) => void;

  // ⚡ New Gate Actions
  fetchGateStatus: () => Promise<void>;
  executeGateOverride: (targetStatus: string, reason: string, secret: string) => Promise<{ success: boolean; message: string }>;
}

export const useStore = create<SupremeState>((set) => ({
  systemConfig: AppDefaults,
  isConfigLoaded: false,
  setConfig: (config) => set((state) => ({ systemConfig: { ...state.systemConfig, ...config }, isConfigLoaded: true })),

  isServerOnline: false,
  sessionId: null,
  currentIdempotencyKey: null,
  isOrchestrating: false,
  chatHistory: [],
  activeTaskType: "general",
  executionError: null,
  streamLogs: [],

  // Default States
  deployGate: null,
  isGateLoading: false,

  isForging: false,
  forgeFeedback: null,
  forgeSuccessCode: null,

  setServerStatus: (online) => set({ isServerOnline: online }),
  initializeSession: (id) => set({ sessionId: id }),
  generateIdempotencyKey: () => {
    const uniqueKey = crypto.randomUUID();
    set({ currentIdempotencyKey: uniqueKey });
    return uniqueKey;
  },
  addMessage: (message) => set((state) => ({
    chatHistory: [...state.chatHistory, { ...message, id: crypto.randomUUID(), timestamp: Date.now() }]
  })),
  clearHistory: () => set({ chatHistory: [], executionError: null }),
  triggerOrchestration: (active, error = null) => set({ isOrchestrating: active, executionError: error }),

  // ── 🛡️ Autonomous Gate Management Actions ────────────────────────
  fetchGateStatus: async () => {
    // বাংলা মন্তব্য: raw fetch() → apiClient — Authorization header এখন সব request এ যাচ্ছে
    const adminToken = localStorage.getItem('supreme_admin_jwt');
    if (!adminToken) {
      // 401 Error এড়াতে অ্যাডমিন টোকেন না থাকলে API কল করা থেকে বিরত থাকা হচ্ছে
      return;
    }
    set({ isGateLoading: true });
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data = await apiClient.get<any>('/api/admin/metrics/dashboard');
      set({ deployGate: {
        status: data.status === "HEALTHY" ? "UNLOCKED" : "LOCKED",
        reason: data.error || "System operating within safe deployment thresholds."
      }});
    } catch (err) {
      console.error("Failed to sync deploy gate telemetry:", err);
    } finally {
      set({ isGateLoading: false });
    }
  },

  executeGateOverride: async (targetStatus, reason, secret) => {
    // বাংলা মন্তব্য: raw fetch() → apiClient — auth header এখন যাচ্ছে। admin_secret এখনো body তে, HTTPS চ্যানেলে safe।
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data = await apiClient.post<any>('/api/admin/gate/override', {
        target_status: targetStatus,
        reason,
        admin_secret: secret,
      });
      if (data.success) {
        set({ deployGate: { status: data.forced_status, reason: `👑 Forced: ${reason}` } });
        return { success: true, message: data.message };
      }
      return { success: false, message: data.detail || "Override verification rejected." };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      return { success: false, message: err.message || "Network isolation error." };
    }
  },

  forgeNewSkill: async (skillName, userDemand) => {
    // বাংলা মন্তব্য: raw fetch() → apiClient — Authorization header সহ, 402/429 status properly throw হবে
    set({ isForging: true, forgeFeedback: "🧠 Self-Evolution Core is structuring your request...", forgeSuccessCode: null });
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data = await apiClient.post<any>('/api/evolution/forge', {
        skill_name: skillName,
        user_demand: userDemand,
      });
      if (data.success) {
        set({
          isForging: false,
          forgeFeedback: `🏆 Success! Skill '${data.skill_name}' is fully deployed to Firestore.`,
          forgeSuccessCode: data.generated_code,
        });
      } else {
        set({
          isForging: false,
          forgeFeedback: `🚨 Evolution Blocked: ${data.detail || data.error || "Sandbox Verification Failed."}`,
        });
      }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      set({
        isForging: false,
        forgeFeedback: `❌ Infrastructure Error: ${err.message || "Network Failure."}`,
      });
    }
  }
}));
