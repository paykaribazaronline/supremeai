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
  SuggestionFeedback 
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
