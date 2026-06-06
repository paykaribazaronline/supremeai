/**
 * SupremeAI Chat Provider - Real-time conversational AI assistant
 */

import * as vscode from 'vscode';
import { getSupremeAIService } from '../services/SupremeAIService';
import { AuthService } from '../services/AuthService';
import { ChatMessage, ChatSession } from '../types';
import { SupremeAIChatView } from './SupremeAIChatView';

export class SupremeAIChatProvider implements vscode.WebviewViewProvider {
  private readonly viewId: string;
  private webview: vscode.WebviewView | null = null;
  private messageHistory: ChatMessage[] = [];
  private currentSession: ChatSession | null = null;

  constructor(context: vscode.ExtensionContext | string) {
    this.viewId = typeof context === 'string' ? context : 'supremeaiChat';
  }

  public resolveWebviewView(
    webviewView: vscode.WebviewView,
    context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ): void {
    this.webview = webviewView;

    // Retain context when hidden to preserve state
    webviewView.webview.options = {
      enableScripts: true,
      localResourceRoots: [],
      enableCommandUris: true
    };

    // Restore previous state if available
    const state = context.state as any;
    if (state) {
      this.messageHistory = state.messageHistory || [];
      this.currentSession = state.currentSession || null;
    }

    this.setupWebviewMessageListener(webviewView);

    // Set initial loading state
    webviewView.webview.html = this.getLoadingHTML();

    this.updateContent(webviewView);
  }

  private getLoadingHTML(): string {
    return '<!DOCTYPE html><html><body style="display:flex;justify-content:center;align-items:center;height:100vh;color:var(--vscode-descriptionForeground);font-family:sans-serif;background:var(--vscode-sideBar-background);"><div>🤖 Initializing AI Assistant...</div></body></html>';
  }

  private setupWebviewMessageListener(webviewView: vscode.WebviewView): void {
    webviewView.webview.onDidReceiveMessage(
      async (data) => {
        switch (data.type) {
          case 'sendMessage':
            await this.handleSendMessage(data.message, data.context);
            break;
          case 'newChat':
            await this.handleNewChat();
            break;
          case 'clearChat':
            await this.handleClearChat();
            break;
          case 'explainCode':
            await this.handleExplainCode();
            break;
          case 'fixCode':
            await this.handleFixCode();
            break;
          case 'refactorCode':
            await this.handleRefactorCode();
            break;
          case 'login':
            vscode.commands.executeCommand('supremeai.login').then(() => {
              this.updateContent(webviewView);
            });
            break;
          case 'loginAsGuest':
            vscode.commands.executeCommand('supremeai.loginAsGuest').then(() => {
              this.updateContent(webviewView);
            });
            break;
          case 'logout':
            vscode.commands.executeCommand('supremeai.logout').then(() => {
              this.updateContent(webviewView);
            });
            break;
          case 'openSettings':
            vscode.commands.executeCommand('workbench.action.openSettings', 'supremeai');
            break;
        }
      },
      undefined
    );
  }

  private async handleSendMessage(message: string, context?: any): Promise<void> {
    if (!message.trim()) return;

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: message,
      timestamp: new Date().toISOString()
    };

    this.addMessage(userMessage);

    // Incremental update instead of full HTML reload
    this.webview?.webview.postMessage({ type: 'addMessage', message: userMessage });

    // Show thinking indicator
    this.showThinkingIndicator();

    try {
      const service = getSupremeAIService();
      const editor = vscode.window.activeTextEditor;
      let codeContext = '';

      if (editor && context?.includeCode !== false) {
        const document = editor.document;
        const selection = editor.selection;
        const selectedText = document.getText(selection);
        const fullText = document.getText();

        codeContext = '\n\n--- Code Context ---\n' +
          `Language: ${document.languageId}\n` +
          `File: ${document.fileName}\n` +
          (selectedText ? `Selected: ${selectedText}\n` : '') +
          `Relevant code:\n${this.getRelevantCode(fullText, message)}`;
      }

      const response = await this.sendChatRequest(message, codeContext);

      this.removeThinkingIndicator();

      const aiMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: response,
        timestamp: new Date().toISOString()
      };

      this.addMessage(aiMessage);
      this.updateContent(this.webview!);
    } catch (error: any) {
      this.removeThinkingIndicator();
      const errorMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: `❌ Error: ${error.message}. Please check your connection and try again.`,
        timestamp: new Date().toISOString(),
        error: true
      };
      this.addMessage(errorMessage);
      this.webview?.webview.postMessage({ type: 'addMessage', message: errorMessage });
    }
  }

  private async sendChatRequest(message: string, codeContext: string = ''): Promise<string> {
    const service = getSupremeAIService();

    try {
      const history = this.messageHistory.filter(m => !m.thinking);
      const response = await (service as any).client.post('/api/chat/stream', {
        message: message + codeContext,
        sessionId: service.getSessionId(),
        messages: history,
        context: {
          source: 'vscode',
          timestamp: new Date().toISOString()
        }
      });

      return response.data?.response || response.data?.message || 'I received your message!';
    } catch (error: any) {
      // Fallback to local AI processing (using only original user message, not code context)
      return this.generateLocalResponse(message);
    }
  }

  private generateLocalResponse(message: string): string {
    const lowerMsg = message.toLowerCase();

    if (/\b(hello|hi|hey)\b/.test(lowerMsg)) {
      return 'Hello! I\'m your SupremeAI assistant. How can I help you with your code today?';
    }

    if (/\b(bug|error|fix)\b/.test(lowerMsg)) {
      return 'I can help you debug! Please share the error message or the problematic code, and I\'ll analyze it for you.';
    }

    if (/\b(refactor|improve|optimize)\b/.test(lowerMsg)) {
      return 'I can help refactor your code! Please share the code you\'d like to improve, and I\'ll suggest optimizations.';
    }

    if (/\b(explain|understand)\b/.test(lowerMsg)) {
      return 'I can explain code concepts! Please share the code or concept you\'d like me to explain.';
    }

    if (lowerMsg.includes('time')) {
      return `The current time is: ${new Date().toLocaleTimeString()}`;
    }

    return 'I\'m here to help with your coding needs! You can ask me to:\n' +
      '• Explain code\n' +
      '• Fix bugs\n' +
      '• Refactor code\n' +
      '• Review code\n' +
      '• Answer programming questions\n\n' +
      'Please share your code or question, and I\'ll do my best to help!';
  }

  private getRelevantCode(fullText: string, message: string): string {
    const lines = fullText.split('\n');
    const maxLines = 50;

    if (lines.length <= maxLines) {
      return fullText;
    }

    // Return first and last parts
    const firstPart = lines.slice(0, 25).join('\n');
    const lastPart = lines.slice(-25).join('\n');

    return `${firstPart}\n\n... [${lines.length - 50} lines omitted] ...\n\n${lastPart}`;
  }

  private async handleNewChat(): Promise<void> {
    this.messageHistory = [];
    this.currentSession = null;
    this.updateContent(this.webview!);

    vscode.window.showInformationMessage('New chat session started');
  }

  private async handleClearChat(): Promise<void> {
    this.messageHistory = [];
    this.updateContent(this.webview!);

    vscode.window.showInformationMessage('Chat history cleared');
  }

  private async handleExplainCode(): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor to explain code from');
      return;
    }

    const selection = editor.selection;
    const code = editor.document.getText(selection);

    if (!code) {
      vscode.window.showWarningMessage('Please select code to explain');
      return;
    }

    const message = `Please explain this code:\n\n${code}`;
    await this.handleSendMessage(message, { includeCode: false });
  }

  private async handleFixCode(): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor to fix code from');
      return;
    }

    const selection = editor.selection;
    const code = editor.document.getText(selection);

    if (!code) {
      vscode.window.showWarningMessage('Please select code to fix');
      return;
    }

    const message = `Please help fix this code:\n\n${code}`;
    await this.handleSendMessage(message, { includeCode: false });
  }

  private async handleRefactorCode(): Promise<void> {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor to refactor code from');
      return;
    }

    const selection = editor.selection;
    const code = editor.document.getText(selection);

    if (!code) {
      vscode.window.showWarningMessage('Please select code to refactor');
      return;
    }

    const message = `Please suggest improvements for this code:\n\n${code}`;
    await this.handleSendMessage(message, { includeCode: false });
  }

  private addMessage(message: ChatMessage): void {
    this.messageHistory.push(message);

    // Keep only last 100 messages
    if (this.messageHistory.length > 100) {
      this.messageHistory = this.messageHistory.slice(-100);
    }
  }

  private showThinkingIndicator(): void {
    if (this.webview) {
      const thinkingMsg: ChatMessage = {
        id: 'thinking',
        role: 'assistant',
        content: '🤔 Thinking...',
        timestamp: new Date().toISOString(),
        thinking: true
      };
      this.addMessage(thinkingMsg);
      this.updateContent(this.webview);
    }
  }

  private removeThinkingIndicator(): void {
    this.messageHistory = this.messageHistory.filter(m => m.id !== 'thinking');
  }

  private async updateContent(webviewView: vscode.WebviewView): Promise<void> {
    const authService = AuthService.getInstance();
    if (!authService || !authService.isAuthenticated()) {
      webviewView.webview.html = SupremeAIChatView.getLoginHTML();
      return;
    }

    const isGuest = authService.getUser()?.username === "Guest User";
    const username = authService.getUser()?.username || 'Guest User';
    const config = vscode.workspace.getConfiguration('supremeai');
    const hasApiKey = !!config.get<string>('aiApiKey');

    webviewView.webview.html = SupremeAIChatView.getHTMLContent(isGuest, username, hasApiKey, this.messageHistory);
  }

  public dispose(): void {
    this.webview = null;
  }

  // Save state when webview is disposed
  public getState(): any {
    return {
      messageHistory: this.messageHistory,
      currentSession: this.currentSession
    };
  }
}
