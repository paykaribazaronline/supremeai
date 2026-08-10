// Agent Operations Service for SupremeAI 2.0
// বাংলা মন্তব্য: এজেন্ট ডিপার্টমেন্ট, টাস্ক এক্সেকিউশন ও এজেন্টদের তথ্য আনার জন্য ব্যবহৃত সার্ভিস।

import { apiClient } from './apiClient';

export interface AgentTask {
  id: string;
  name: string;
  status: string;
  result?: string;
}

export const agentService = {
  // বাংলা মন্তব্য: agentId প্যারামিটার বর্তমানে ব্যবহৃত হচ্ছে না, তাই tsc/eslint warning এড়াতে '_' প্রিফিক্স দেওয়া হলো।
  executeAgentTask: async (_agentId: string, instruction: string): Promise<AgentTask> => {
    return apiClient.post<AgentTask>('/api/v1/agents/execute', {
      instruction,
    });
  },

  listAgents: async (): Promise<unknown[]> => {
    return apiClient.get<unknown[]>('/api/v1/agents');
  },

  getAgentStatus: async (agentId: string): Promise<{ status: string }> => {
    return apiClient.get<{ status: string }>(`/api/v1/agents/${agentId}/status`);
  },
};
