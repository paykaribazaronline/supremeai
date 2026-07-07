# 📄 ফাইল: apps/desktop/src-ui/src/types/index.ts

**প্রকার:** .ts  
**সাইজ:** 559 বাইট  
**আপডেট:** 2026-07-07T13:36:10.029382

---

## কোড

```ts
// Export common types
export interface Skill {
  id: string;
  name: string;
  description: string;
  category: string;
  parameters: Record<string, any>;
}

export interface ChatMessage {
  id: string;
  content: string;
  role: "user" | "assistant";
  timestamp: string;
}

export interface EvolutionRequest {
  skill_name: string;
  user_demand: string;
}

export interface GitHubConnection {
  repo_url: string;
  access_token?: string;
}

export interface AdminStats {
  total_requests: number;
  total_cost: number;
  average_response_time: number;
}


```