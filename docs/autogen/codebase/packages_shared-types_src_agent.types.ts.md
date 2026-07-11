# 📄 ফাইল: packages/shared-types/src/agent.types.ts

**প্রকার:** .ts  
**সাইজ:** 971 বাইট  
**আপডেট:** 2026-07-11T17:16:16.830532

---

## কোড

```ts
/**
 * Agent Actions represent the specific tool or internal action the agent is executing.
 */
export type AgentAction = 
  | 'READ_FILE'
  | 'WRITE_FILE'
  | 'RUN_COMMAND'
  | 'SEARCH_CODEBASE'
  | 'ANALYZE_ERROR'
  | 'RUN_SANDBOX'
  | 'IDLE';

/**
 * Agent Reasoning represents the internal chain-of-thought of the AI.
 * This is meant to be streamed or returned to the UI to show what the agent is thinking.
 */
export interface AgentReasoning {
  stepIndex: number;
  chainOfThought: string;
  confidenceScore: number; // 0.0 to 1.0
  toolCallSuggestion?: AgentAction; // Suggestion for the next tool call
  contextUsed?: string[]; // Files or context chunks used for this reasoning step
}

/**
 * The unified envelope for agent responses.
 */
export interface AgentResponse<T = any> {
  reasoningLog: AgentReasoning[];
  executedActions: { action: AgentAction; status: 'SUCCESS' | 'FAILED' | 'PENDING'; detail?: string }[];
  finalOutput: T;
  timestamp: string;
}

```