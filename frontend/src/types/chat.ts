/**
 * ✅ UNIFIED TYPE DEFINITIONS - Single Source of Truth
 * 
 * PROBLEM SOLVED: Two incompatible ChatMessage types existed
 * - useStore.ts had: { id, role: "user"|"assistant", content, timestamp }
 * - chatStore.ts had: { id, role: "user"|"assistant"|"system", content, ts }
 * 
 * IMPACT: Components importing from different stores would BREAK
 */

// ═══════════════════════════════════════════════════════════════
// CORE CHAT TYPES
// ═══════════════════════════════════════════════════════════════

export interface UnifiedChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  timestamp: number; // Standardized field name (was 'ts' in some places)
  metadata?: MessageMetadata;
}

export type ChatRole = 'user' | 'assistant' | 'system' | 'tool' | 'function';

export interface MessageMetadata {
  model?: string;
  provider?: string;
  tokens?: number;
  cost?: number;
  /** Where did this message originate from? */
  source?: MessageSource;
  /** Parent message ID for threading/replies */
  parentId?: string;
  /** Was this message edited? */
  editedAt?: number;
  /** Attachments (files, images, etc.) */
  attachments?: Attachment[];
}

export type MessageSource = 
  | 'chat'           // Direct user chat
  | 'evolution'      // AI self-evolution
  | 'browser'        // Browser agent context
  | 'voice'          // Voice input transcribed
  | 'swarm'          // Multi-agent swarm
  | 'api'            // External API call
  | 'import';        // Imported conversation

export interface Attachment {
  id: string;
  type: 'image' | 'file' | 'code' | 'url';
  name: string;
  url: string;
  size?: number;
  mimeType?: string;
}

// ═══════════════════════════════════════════════════════════════
// CONVERSATION TYPES
// ═══════════════════════════════════════════════════════════════

export interface ChatConversation {
  id: string;
  title: string;
  messages: UnifiedChatMessage[];
  createdAt: number;
  updatedAt: number;
  /** User-defined tags for organization */
  tags?: string[];
  /** Is this conversation pinned? */
  isPinned?: boolean;
  /** Associated workspace/project ID */
  workspaceId?: string;
  /** Conversation metadata */
  metadata?: ConversationMetadata;
}

export interface ConversationMetadata {
  totalTokens: number;
  totalCost: number;
  messageCount: number;
  lastModelUsed?: string;
  /** RAG context used */
  ragSources?: string[];
}

// ═══════════════════════════════════════════════════════════════
// HELPER TYPES
// ═══════════════════════════════════════════════════════════════

export interface ChatState {
  conversations: ChatConversation[];
  activeConversationId: string | null;
  isLoading: boolean;
  error: string | null;
}

export interface SendMessagePayload {
  content: string;
  conversationId?: string;
  attachments?: Attachment[];
  metadata?: Partial<MessageMetadata>;
}

export interface StreamChunkPayload {
  token: string;
  messageId: string;
  isComplete: boolean;
  metadata?: Partial<MessageMetadata>;
}

// Type guards
export function isUserMessage(msg: UnifiedChatMessage): boolean {
  return msg.role === 'user';
}

export function isAssistantMessage(msg: UnifiedChatMessage): boolean {
  return msg.role === 'assistant';
}

export function hasAttachments(msg: UnifiedChatMessage): boolean {
  return (msg.metadata?.attachments?.length ?? 0) > 0;
}
