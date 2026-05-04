/**
 * SupremeAI VS Code Extension - Type Definitions
 * Types for real-time learning and feedback
 */

export interface CodeEdit {
  taskId: string;
  originalCode: string;
  editedCode: string;
  context: string;
  language: string;
  timestamp: string;
  filePath: string;
  lineNumber?: number;
}

export interface ErrorReport {
  errorType: 'compilation' | 'runtime' | 'lint' | 'security' | 'performance';
  errorMessage: string;
  errorCode?: string;
  filePath: string;
  lineNumber: number;
  columnNumber?: number;
  stackTrace?: string;
  codeSnippet?: string;
  severity: 'error' | 'warning' | 'info';
  timestamp: string;
}

export interface SuggestionFeedback {
  suggestionId: string;
  accepted: boolean;
  modifiedCode?: string;
  context: string;
  taskId: string;
  timestamp: string;
}

export interface LearningUpload {
  type: 'CODE_EDIT' | 'ERROR_REPORT' | 'SUGGESTION_FEEDBACK' | 'CODE_ANALYSIS' | 'CHAT_MESSAGE';
  data: CodeEdit | ErrorReport | SuggestionFeedback | ChatMessage | CodeAnalysis;
  sessionId: string;
  userId?: string;
}

export interface LearningResponse {
  success: boolean;
  message: string;
  learnedPatterns?: string[];
}

export interface SupremeAIConfig {
  backendUrl: string;
  enableRealTimeLearning: boolean;
  autoReportErrors: boolean;
  enableChat?: boolean;
}

export interface NotificationMessage {
  type: 'GITHUB_PIPELINE' | 'SYSTEM_ALERT' | 'LEARNING_UPDATE' | 'CHAT_MESSAGE';
  status: 'success' | 'failure' | 'warning' | 'info';
  message: string;
  timestamp: string;
  details?: Record<string, any>;
}

// Chat Types
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  error?: boolean;
  thinking?: boolean;
  context?: any;
}

export interface ChatSession {
  id: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
  context?: {
    language?: string;
    filePath?: string;
    projectInfo?: any;
  };
}

export interface ChatRequest {
  message: string;
  sessionId: string;
  context?: {
    source: 'vscode' | 'intellij' | 'web' | 'flutter';
    language?: string;
    filePath?: string;
    codeSnippet?: string;
    timestamp: string;
  };
}

export interface ChatResponse {
  success: boolean;
  message: string;
  response: string;
  sessionId: string;
  timestamp: string;
  suggestions?: string[];
}

export interface CodeAnalysis {
  filePath: string;
  code: string;
  language: string;
  timestamp: string;
  metrics: {
    linesOfCode: number;
    nonEmptyLines: number;
    commentLines: number;
    complexityEstimate: number;
  };
}
