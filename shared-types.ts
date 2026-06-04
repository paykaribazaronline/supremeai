export interface SharedChatMessage {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp?: string;
}

export interface SharedCodeEdit {
  taskId: string;
  originalCode: string;
  editedCode: string;
  context: string;
  language: string;
  timestamp: string;
  filePath: string;
}

export interface SharedErrorResolutionRequest {
  errorType: string;
  errorMessage: string;
  stackTrace?: string;
  filePath?: string;
  codeSnippet?: string;
  context: string;
}
