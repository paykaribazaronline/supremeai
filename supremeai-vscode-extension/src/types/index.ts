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
  type: 'CODE_EDIT' | 'ERROR_REPORT' | 'SUGGESTION_FEEDBACK' | 'CODE_ANALYSIS';
  data: CodeEdit | ErrorReport | SuggestionFeedback;
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
}

export interface NotificationMessage {
  type: 'GITHUB_PIPELINE' | 'SYSTEM_ALERT' | 'LEARNING_UPDATE';
  status: 'success' | 'failure' | 'warning' | 'info';
  message: string;
  timestamp: string;
  details?: Record<string, any>;
}
