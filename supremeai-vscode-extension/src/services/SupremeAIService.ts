/**
 * SupremeAI Service - Communication with Backend Learning Engine
 * Handles all real-time learning data transmission
 */

import * as vscode from 'vscode';
import axios, { AxiosInstance } from 'axios';
import { 
  LearningUpload, 
  LearningResponse, 
  SupremeAIConfig,
  CodeEdit,
  ErrorReport,
  SuggestionFeedback,
  ChatMessage,
  ChatRequest,
  ChatResponse,
  CodeAnalysis
} from '../types';

export class SupremeAIService {
  private client: AxiosInstance;
  private config: SupremeAIConfig;
  private sessionId: string;

  constructor(config: SupremeAIConfig) {
    this.config = config;
    this.sessionId = this.generateSessionId();
    
    // Configure Axios instance with defaults
    this.client = axios.create({
      baseURL: config.backendUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for logging
    this.client.interceptors.request.use((request) => {
      console.log(`[SupremeAI] Sending ${request.method?.toUpperCase()} to ${request.url}`);
      return request;
    });

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        console.error(`[SupremeAI] API Error: ${error.message}`);
        return Promise.reject(error);
      }
    );
  }

  /**
   * Send code edit event for learning
   * POST /api/knowledge/learn
   */
  async sendCodeEdit(edit: CodeEdit): Promise<LearningResponse> {
    if (!this.config.enableRealTimeLearning) {
      return { success: false, message: 'Real-time learning disabled' };
    }

    try {
      const payload: LearningUpload = {
        type: 'CODE_EDIT',
        data: edit,
        sessionId: this.sessionId,
      };

      const response = await this.client.post<LearningResponse>('/api/knowledge/learn', payload);
      console.log(`[SupremeAI] Code edit learned: ${edit.taskId}`);
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to send code edit: ${error.message}`);
      return {
        success: false,
        message: error.message || 'Failed to send code edit',
      };
    }
  }

  /**
   * Report error for learning
   * POST /api/knowledge/failure
   */
  async reportError(error: ErrorReport): Promise<LearningResponse> {
    if (!this.config.autoReportErrors) {
      return { success: false, message: 'Auto-error reporting disabled' };
    }

    try {
      const payload: LearningUpload = {
        type: 'ERROR_REPORT',
        data: error,
        sessionId: this.sessionId,
      };

      const response = await this.client.post<LearningResponse>('/api/knowledge/failure', payload);
      console.log(`[SupremeAI] Error reported: ${error.errorType} at ${error.filePath}:${error.lineNumber}`);
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to report error: ${error.message}`);
      return {
        success: false,
        message: error.message || 'Failed to report error',
      };
    }
  }

  /**
   * Send suggestion feedback (accept/reject)
   * POST /api/knowledge/feedback
   */
  async sendFeedback(feedback: SuggestionFeedback): Promise<LearningResponse> {
    try {
      const payload: LearningUpload = {
        type: 'SUGGESTION_FEEDBACK',
        data: feedback,
        sessionId: this.sessionId,
      };

      const response = await this.client.post<LearningResponse>('/api/knowledge/feedback', payload);
      console.log(`[SupremeAI] Feedback sent: ${feedback.accepted ? 'accepted' : 'rejected'}`);
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to send feedback: ${error.message}`);
      return {
        success: false,
        message: error.message || 'Failed to send feedback',
      };
    }
  }

  /**
   * Send code analysis snapshot
   * POST /api/knowledge/analysis
   */
  async sendCodeAnalysis(filePath: string, code: string, language: string): Promise<LearningResponse> {
    if (!this.config.enableRealTimeLearning) {
      return { success: false, message: 'Real-time learning disabled' };
    }

    try {
      const analysis = {
        filePath,
        code,
        language,
        timestamp: new Date().toISOString(),
        metrics: this.analyzeCodeMetrics(code, language),
      };

      const response = await this.client.post<LearningResponse>('/api/knowledge/analysis', {
        ...analysis,
        sessionId: this.sessionId,
      });

      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to send analysis: ${error.message}`);
      return { success: false, message: error.message };
    }
  }

  /**
   * Send chat message
   * POST /api/chat/message
   */
  async sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
    try {
      const response = await this.client.post<ChatResponse>('/api/chat/message', {
        ...request,
        sessionId: this.sessionId,
      });
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Chat error: ${error.message}`);
      return {
        success: false,
        message: error.message,
        response: this.generateFallbackResponse(request.message),
        sessionId: this.sessionId,
        timestamp: new Date().toISOString()
      };
    }
  }

  /**
   * Stream chat response
   * POST /api/chat/stream
   */
  async streamChatResponse(request: ChatRequest): Promise<ReadableStream> {
    try {
      const response = await this.client.post('/api/chat/stream', request, {
        responseType: 'stream'
      });
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Stream error: ${error.message}`);
      throw error;
    }
  }

  /**
   * Get chat history
   * GET /api/chat/history
   */
  async getChatHistory(sessionId?: string): Promise<ChatMessage[]> {
    try {
      const response = await this.client.get('/api/chat/history', {
        params: { sessionId: sessionId || this.sessionId }
      });
      return response.data.messages || [];
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to get chat history: ${error.message}`);
      return [];
    }
  }

  /**
   * Clear chat history
   * DELETE /api/chat/history
   */
  async clearChatHistory(sessionId?: string): Promise<boolean> {
    try {
      await this.client.delete('/api/chat/history', {
        data: { sessionId: sessionId || this.sessionId }
      });
      return true;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to clear chat history: ${error.message}`);
      return false;
    }
  }

  /**
   * Generate fallback response when backend is unavailable
   */
  private generateFallbackResponse(message: string): string {
    const lowerMsg = message.toLowerCase();
    
    if (lowerMsg.includes('hello') || lowerMsg.includes('hi') || lowerMsg.includes('hey')) {
      return 'Hello! I\'m your SupremeAI assistant. How can I help you with your code today?';
    }
    
    if (lowerMsg.includes('bug') || lowerMsg.includes('error') || lowerMsg.includes('fix')) {
      return 'I can help you debug! Please share the error message or the problematic code, and I\'ll analyze it for you.';
    }
    
    if (lowerMsg.includes('refactor') || lowerMsg.includes('improve') || lowerMsg.includes('optimize')) {
      return 'I can help refactor your code! Please share the code you\'d like to improve, and I\'ll suggest optimizations.';
    }
    
    if (lowerMsg.includes('explain') || lowerMsg.includes('understand')) {
      return 'I can explain code concepts! Please share the code or concept you\'d like me to explain.';
    }

    return 'I\'m here to help with your coding needs! You can ask me to:\n' +
      '• Explain code\n' +
      '• Fix bugs\n' +
      '• Refactor code\n' +
      '• Review code\n' +
      '• Answer programming questions\n\n' +
      'Please share your code or question, and I\'ll do my best to help!';
  }

  /**
   * Get learning statistics
   * GET /api/knowledge/stats
   */
  async getLearningStats(): Promise<any> {
    try {
      const response = await this.client.get('/api/knowledge/stats');
      return response.data;
    } catch (error: any) {
      console.error(`[SupremeAI] Failed to get stats: ${error.message}`);
      return null;
    }
  }

  /**
   * Generate unique session ID for this VS Code session
   */
  private generateSessionId(): string {
    const timestamp = Date.now().toString(36);
    const random = Math.random().toString(36).substring(7);
    return `vscode-${timestamp}-${random}`;
  }

  /**
   * Quick metrics analysis for code
   */
  private analyzeCodeMetrics(code: string, language: string): Record<string, number> {
    const lines = code.split('\n');
    return {
      linesOfCode: lines.length,
      nonEmptyLines: lines.filter(l => l.trim().length > 0).length,
      commentLines: language === 'typescript' || language === 'javascript'
        ? lines.filter(l => l.trim().startsWith('//') || l.trim().startsWith('/*')).length
        : 0,
      complexityEstimate: this.estimateComplexity(code),
    };
  }

  private estimateComplexity(code: string): number {
    // Simple cyclomatic complexity approximation
    const decisionPoints = (code.match(/\b(if|else|for|while|switch|case|catch)\b/g) || []).length;
    return decisionPoints + 1;
  }

  /**
   * Update configuration at runtime
   */
  updateConfig(newConfig: Partial<SupremeAIConfig>): void {
    this.config = { ...this.config, ...newConfig };
  }

  /**
   * Get current session ID
   */
  getSessionId(): string {
    return this.sessionId;
  }
}

/**
 * Singleton instance
 */
let supremeAIService: SupremeAIService | null = null;

export function getSupremeAIService(config?: SupremeAIConfig): SupremeAIService {
  if (!supremeAIService && config) {
    supremeAIService = new SupremeAIService(config);
  }
  if (!supremeAIService) {
    throw new Error('SupremeAIService not initialized. Call getSupremeAIService(config) first.');
  }
  return supremeAIService;
}

export function setSupremeAIService(service: SupremeAIService): void {
  supremeAIService = service;
}
