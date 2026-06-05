/**
 * SupremeAI Chat Provider - Real-time conversational AI assistant
 */

import * as vscode from 'vscode';
import { getSupremeAIService } from '../services/SupremeAIService';
import { AuthService } from '../services/AuthService';
import { ChatMessage, ChatSession } from '../types';

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
    this.updateContent(this.webview!);

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

      const response = await this.sendChatRequest(message + codeContext);
      
      const aiMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: response,
        timestamp: new Date().toISOString()
      };

      this.addMessage(aiMessage);
      this.updateContent(this.webview!);
    } catch (error: any) {
      const errorMessage: ChatMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: `❌ Error: ${error.message}. Please check your connection and try again.`,
        timestamp: new Date().toISOString(),
        error: true
      };
      this.addMessage(errorMessage);
      this.updateContent(this.webview!);
    }
  }

  private async sendChatRequest(message: string): Promise<string> {
    const service = getSupremeAIService();
    
    try {
      const history = this.messageHistory.filter(m => !m.thinking);
      const response = await (service as any).client.post('/api/chat/stream', {
        message,
        sessionId: service.getSessionId(),
        messages: history,
        context: {
          source: 'vscode',
          timestamp: new Date().toISOString()
        }
      });
      
      return response.data?.response || response.data?.message || 'I received your message!';
    } catch (error: any) {
      // Fallback to local AI processing
      return this.generateLocalResponse(message);
    }
  }

  private generateLocalResponse(message: string): string {
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

  private async updateContent(webviewView: vscode.WebviewView): Promise<void> {
    const authService = AuthService.getInstance();
    if (!authService || !authService.isAuthenticated()) {
      webviewView.webview.html = this.getLoginHTML();
      return;
    }

    const content = this.getHTMLContent();
    webviewView.webview.html = content;
  }

  private getLoginHTML(): string {
    return `<!DOCTYPE html>
<html lang="bn">
<head>
  <meta charset="UTF-8">
  <style>
    body {
      font-family: var(--vscode-font-family);
      padding: 20px;
      color: var(--vscode-foreground);
      background-color: var(--vscode-sideBar-background);
      text-align: center;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      height: 80vh;
    }
    .logo {
      font-size: 50px;
      margin-bottom: 20px;
    }
    .title {
      font-size: 18px;
      font-weight: bold;
      margin-bottom: 10px;
    }
    .desc {
      font-size: 13px;
      color: var(--vscode-descriptionForeground);
      margin-bottom: 24px;
      line-height: 1.5;
    }
    .btn {
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
      font-weight: bold;
      width: 100%;
      max-width: 200px;
    }
    .btn:hover {
      background: var(--vscode-button-hoverBackground);
    }
  </style>
</head>
<body>
  <div class="logo">🤖</div>
  <div class="title">SupremeAI-তে লগইন করুন</div>
  <div class="desc">আপনার কোড অ্যাসিস্ট্যান্ট ব্যবহার করতে এবং চ্যাট ইন্টারফেসে চ্যাট করতে সাইন-ইন করা প্রয়োজন।</div>
  <button class="btn" id="loginBtn">লগইন করুন</button>
  <button class="btn btn-secondary" id="guestBtn" style="margin-top: 10px; background: var(--vscode-button-secondaryBackground, #3a3d41); color: var(--vscode-button-secondaryForeground, #ffffff);">গেস্ট হিসেবে ব্যবহার করুন</button>

  <script>
    const vscode = acquireVsCodeApi();
    document.getElementById('loginBtn').addEventListener('click', () => {
      vscode.postMessage({ type: 'login' });
    });
    document.getElementById('guestBtn').addEventListener('click', () => {
      vscode.postMessage({ type: 'loginAsGuest' });
    });
  </script>
</body>
</html>`;
  }

  private getHTMLContent(): string {
    const messagesHtml = this.messageHistory.map(msg => this.renderMessage(msg)).join('');
    const emptyState = this.messageHistory.length === 0 ? this.getEmptyState() : '';

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: var(--vscode-font-family);
      background: var(--vscode-sideBar-background);
      color: var(--vscode-foreground);
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    .header {
      padding: 12px 16px;
      border-bottom: 1px solid var(--vscode-panel-border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: var(--vscode-sideBarSectionHeader-background);
    }
    .header h2 {
      font-size: 14px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .header-actions {
      display: flex;
      gap: 8px;
    }
    .btn {
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
      border: none;
      padding: 6px 12px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
      transition: all 0.2s;
    }
    .btn:hover {
      background: var(--vscode-button-hoverBackground);
    }
    .btn-secondary {
      background: var(--vscode-toolbar-hoverBackground);
      color: var(--vscode-foreground);
    }
    .messages {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }
    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--vscode-descriptionForeground);
    }
    .empty-state-icon {
      font-size: 48px;
      margin-bottom: 16px;
      opacity: 0.5;
    }
    .empty-state h3 {
      font-size: 16px;
      margin-bottom: 8px;
      color: var(--vscode-foreground);
    }
    .empty-state p {
      font-size: 13px;
      line-height: 1.5;
    }
    .quick-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 20px;
      justify-content: center;
    }
    .quick-btn {
      background: var(--vscode-list-hoverBackground);
      border: 1px solid var(--vscode-panel-border);
      color: var(--vscode-foreground);
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 12px;
      transition: all 0.2s;
    }
    .quick-btn:hover {
      background: var(--vscode-list-activeSelectionBackground);
      border-color: var(--vscode-focusBorder);
    }
    .input-area {
      padding: 12px 16px;
      border-top: 1px solid var(--vscode-panel-border);
      background: var(--vscode-sideBar-background);
    }
    .input-wrapper {
      display: flex;
      gap: 8px;
      align-items: flex-end;
    }
    textarea {
      flex: 1;
      background: var(--vscode-input-background);
      color: var(--vscode-input-foreground);
      border: 1px solid var(--vscode-input-border);
      border-radius: 6px;
      padding: 10px 12px;
      font-family: var(--vscode-font-family);
      font-size: 13px;
      resize: none;
      min-height: 40px;
      max-height: 120px;
    }
    textarea:focus {
      outline: none;
      border-color: var(--vscode-focusBorder);
    }
    .send-btn {
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
      border: none;
      padding: 10px 20px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.2s;
    }
    .send-btn:hover {
      background: var(--vscode-button-hoverBackground);
    }
    .send-btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
      animation: slideIn 0.3s ease;
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .message.user { flex-direction: row-reverse; }
    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      flex-shrink: 0;
      margin-top: 4px;
    }
    .avatar.user-avatar {
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
    }
    .avatar.ai-avatar {
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: white;
    }
    .message-content {
      flex: 1;
      padding: 12px 16px;
      border-radius: 12px;
      font-size: 13px;
      line-height: 1.6;
      word-wrap: break-word;
    }
    .user .message-content {
      background: var(--vscode-button-background);
      color: var(--vscode-button-foreground);
      border-bottom-right-radius: 4px;
    }
    .assistant .message-content {
      background: var(--vscode-list-hoverBackground);
      color: var(--vscode-foreground);
      border-bottom-left-radius: 4px;
    }
    .message-content.error {
      background: var(--vscode-errorBackground);
      color: var(--vscode-errorForeground);
    }
    .message-meta {
      font-size: 11px;
      color: var(--vscode-descriptionForeground);
      margin-top: 4px;
    }
    .thinking {
      opacity: 0.7;
    }
    .thinking::after {
      content: '';
      animation: dots 1.5s infinite;
    }
    @keyframes dots {
      0%, 20% { content: ''; }
      40% { content: '.'; }
      60% { content: '..'; }
      80%, 100% { content: '...'; }
    }
    ::-webkit-scrollbar {
      width: 8px;
    }
    ::-webkit-scrollbar-track {
      background: var(--vscode-sideBar-background);
    }
    ::-webkit-scrollbar-thumb {
      background: var(--vscode-scrollbarSlider-background);
      border-radius: 4px;
    }
    ::-webkit-scrollbar-thumb:hover {
      background: var(--vscode-scrollbarSlider-hoverBackground);
    }
  </style>
</head>
<body>
  <div class="header">
    <h2>🤖 SupremeAI Assistant</h2>
    <div class="header-actions">
      <button class="btn btn-secondary" onclick="newChat()">New Chat</button>
      <button class="btn btn-secondary" onclick="clearChat()">Clear</button>
      <button class="btn btn-secondary" onclick="openSettings()">⚙️</button>
    </div>
  </div>
  
  <div class="messages" id="messages">
    ${emptyState}
    ${messagesHtml}
  </div>
  
  <div class="input-area">
    <div class="input-wrapper">
      <textarea 
        id="messageInput" 
        placeholder="Ask SupremeAI anything... (e.g., 'Explain this code', 'Fix this bug', 'Refactor this function')"
        rows="1"
        onkeydown="handleKeydown(event)"
        oninput="autoResize(this)"
      ></textarea>
      <button class="send-btn" id="sendBtn" onclick="sendMessage()">Send</button>
    </div>
    <div class="quick-actions">
      <button class="quick-btn" onclick="quickAction('explain')">📄 Explain Code</button>
      <button class="quick-btn" onclick="quickAction('fix')">🐛 Fix Code</button>
      <button class="quick-btn" onclick="quickAction('refactor')">🔄 Refactor</button>
      <button class="quick-btn" onclick="quickAction('review')">👀 Review</button>
    </div>
  </div>

  <script>
    const vscode = acquireVsCodeApi();

    function sendMessage() {
      const input = document.getElementById('messageInput');
      const message = input.value.trim();
      if (!message) return;
      
      vscode.postMessage({ 
        type: 'sendMessage', 
        message: message 
      });
      
      input.value = '';
      input.style.height = '40px';
    }

    function handleKeydown(event) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
      }
    }

    function autoResize(textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
    }

    function newChat() {
      vscode.postMessage({ type: 'newChat' });
    }

    function clearChat() {
      vscode.postMessage({ type: 'clearChat' });
    }

    function openSettings() {
      vscode.postMessage({ type: 'openSettings' });
    }

    function quickAction(action) {
      const editor = vscode;
      const actions = {
        explain: { 
          command: 'explainCode',
          prompt: 'Please explain this code'
        },
        fix: { 
          command: 'fixCode', 
          prompt: 'Please help fix this code'
        },
        refactor: { 
          command: 'refactorCode', 
          prompt: 'Please suggest improvements'
        },
        review: { 
          command: 'sendMessage', 
          message: 'Please review this code and suggest improvements:'
        }
      };
      
      vscode.postMessage({ type: actions[action].command });
    }

    // Auto-scroll to bottom
    const messagesDiv = document.getElementById('messages');
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
  </script>
</body>
</html>`;
  }

  private renderMessage(msg: ChatMessage): string {
    let time = '';
    try {
      time = new Date(msg.timestamp).toLocaleTimeString();
    } catch (e) {
      time = new Date().toLocaleTimeString();
    }
    
    const isThinking = msg.thinking;
    const isError = msg.error;
    const role = msg.role || 'assistant';
    const content = msg.content || '';
    
    return `
      <div class="message ${role}">
        <div class="avatar ${role}-avatar">
          ${role === 'user' ? 'U' : 'AI'}
        </div>
        <div class="message-content ${isError ? 'error' : ''} ${isThinking ? 'thinking' : ''}">
          ${content}
        </div>
      </div>
      <div class="message-meta" style="margin-left: ${role === 'user' ? 'auto' : '44px'}; text-align: ${role === 'user' ? 'right' : 'left'};">
        ${time}
      </div>
    `;
  }

  private getEmptyState(): string {
    return `
      <div class="empty-state">
        <div class="empty-state-icon">🤖</div>
        <h3>Welcome to SupremeAI Assistant</h3>
        <p>Your intelligent coding companion is ready to help!</p>
        <div class="quick-actions">
          <button class="quick-btn" onclick="quickAction('explain')">📄 Explain Code</button>
          <button class="quick-btn" onclick="quickAction('fix')">🐛 Fix Code</button>
          <button class="quick-btn" onclick="quickAction('refactor')">🔄 Refactor</button>
          <button class="quick-btn" onclick="quickAction('review')">👄 Review</button>
        </div>
        <p style="margin-top: 20px; font-size: 12px;">Or type your question below to get started</p>
      </div>
    `;
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
