# 📄 ফাইল: tools/vscode-extension/src/extension.ts

**প্রকার:** .ts  
**সাইজ:** 19,381 বাইট  
**আপডেট:** 2026-07-11T17:11:02.791687

---

## কোড

```ts
/**
 * SupremeAI VS Code Extension - Main Entry Point
 * Real-time learning and AI assistance integration
 */

import * as vscode from 'vscode';
import { SupremeAIService, setSupremeAIService } from './services/SupremeAIService';
import { AuthService } from './services/AuthService';
import { AuthHandler } from './handlers/AuthHandler';
import { CodeEditHandler } from './handlers/CodeEditHandler';
import { ErrorHandler } from './handlers/ErrorHandler';
import { FeedbackHandler } from './handlers/FeedbackHandler';
import { CodeFlowHandler, setCodeFlowHandler } from './handlers/CodeFlowHandler';
import { SupremeAIConfig } from './types';
import { SupremeAISidebarProvider } from './providers/SupremeAISidebarProvider';
import { SupremeAIActivityProvider } from './providers/SupremeAIActivityProvider';
import { SupremeAIChatProvider } from './providers/SupremeAIChatProvider';
import { SupremeAIAdminDashboardProvider } from './providers/SupremeAIAdminDashboardProvider';
import { SupremeAICustomerDashboardProvider } from './providers/SupremeAICustomerDashboardProvider';
import { StreamingChatProvider } from './providers/StreamingChatProvider';
import { CodeFlowPanel } from './providers/CodeFlowPanel';
import { AIService, getAIService, setAIService } from './ai/AIService';
import { CodeGenerationService, getCodeGenerationService, setCodeGenerationService } from './ai/CodeGenerationService';
import { CodeReviewService, getCodeReviewService, setCodeReviewService } from './ai/CodeReviewService';
import { detectOtherAiAgents } from './agentDetector'; // এজেন্ট ডিটেক্টর ইম্পোর্ট করা হলো
import { SupremeWebviewProvider } from './providers/SupremeWebviewProvider';
import { CrossAiObserverService } from './services/CrossAiObserverService';
import { SelfHealingService } from './services/SelfHealingService';
import { BrowserPreviewProvider } from './providers/BrowserPreviewProvider';

let supremeAIService: SupremeAIService;
let aiService: AIService;
let codeGenService: CodeGenerationService;
let codeReviewService: CodeReviewService;
let codeFlowHandler: CodeFlowHandler;

function escapeHtml(value: string): string {
  return String(value).replace(/[&<>"']/g, (c) => {
    switch (c) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '"':
        return '&quot;';
      case '\'':
        return '&#39;';
      default:
        return c;
    }
  });
}

export async function activate(context: vscode.ExtensionContext) {
  console.log('[SupremeAI] VS Code Extension activating...');

  // 📡 লোকাল ডিভাইসে অন্য AI এজেন্টদের অবজার্ভ করা শুরু করো
  CrossAiObserverService.initialize(context);

  const config = vscode.workspace.getConfiguration('supremeai');
  const backendUrl = config.get<string>('backendUrl', 'https://supremeai-a.web.app');

  const supremeConfig: SupremeAIConfig = {
    backendUrl,
    enableRealTimeLearning: config.get<boolean>('enableRealTimeLearning', true),
    autoReportErrors: config.get<boolean>('autoReportErrors', true),
  };

  supremeAIService = new SupremeAIService(supremeConfig);
  setSupremeAIService(supremeAIService);

  // 🩺 ইনিশিয়ালাইজ এজেন্ট-ইন-দ্য-লুপ (Self Healing)
  SelfHealingService.initialize(context, supremeAIService);
  const { HealingStatusBar } = require('./ui/HealingStatusBar');
  new HealingStatusBar(context);
  const { TelemetryTracker } = require('./services/TelemetryTracker');
  TelemetryTracker.initialize(context);
  
  // 💡 Register Explain Fix CodeAction
  const { SupremeAIActionProvider } = require('./providers/SupremeAIActionProvider');
  context.subscriptions.push(
      vscode.languages.registerCodeActionsProvider('*', new SupremeAIActionProvider(), {
          providedCodeActionKinds: [vscode.CodeActionKind.QuickFix]
      })
  );

  context.subscriptions.push(
      vscode.commands.registerCommand('supremeai.explainFix', async (uri: vscode.Uri, line: number) => {
          vscode.window.showInformationMessage(`SupremeAI: Generating explanation for the fix on line ${line}...`);
          // Here we would open the SupremeAI Sidebar Webview and trigger the chat with the explanation context.
          vscode.commands.executeCommand('supremeai.sidebar.focus');
      })
  );

  const auth = AuthService.getInstance(supremeConfig, context.secrets);
  await auth.initialize();
  await auth.loginAsGuest();

  // Register URI handler for OAuth callback
  AuthHandler.registerAuthCallback(context);

  aiService = getAIService();
  setAIService(aiService);

  // প্রো-টিপ: ইউজারের প্রাইভেসি নিশ্চিত করতে লোকাল স্ট্যাটিসটিক্স মুছে ফেলা হচ্ছে
  context.globalState.update('patternsLearned', undefined);
  context.globalState.update('codeEdits', undefined);
  context.globalState.update('errorsReported', undefined);
  context.globalState.update('feedbackGiven', undefined);

  // লার্নিং এর অংশ হিসেবে অন্য এআই এজেন্ট ডিটেক্ট করা এবং রিপোর্ট করা
  const agents = detectOtherAiAgents();
  if (agents.length > 0) {
    supremeAIService.sendCodeAnalysis('env-discovery', `Detected AI Agents in environment: ${agents.join(', ')}`, 'system-meta');

    // প্রতিটি ডিটেক্ট করা এজেন্টকে PROPOSED হিসেবে ব্যাকএন্ডে পাঠানো
    agents.forEach(agentName => {
      supremeAIService.registerProposedFeature({
        id: `ext-agent-${agentName.toLowerCase().replace(/\s+/g, '-')}`,
        name: agentName,
        category: 'EXTERNAL_AI_AGENT',
        provider: 'Detected on Host',
        status: 'PROPOSED', // অ্যাডমিন পরে এটি ACTIVE করতে পারবেন
        description: 'This agent was detected running on the user\'s VS Code environment.'
      });
    });
  }

  codeGenService = new CodeGenerationService();
  setCodeGenerationService(codeGenService);

  codeReviewService = new CodeReviewService();
  setCodeReviewService(codeReviewService);

  const editHandler = new CodeEditHandler(context);
  const errHandler = new ErrorHandler(context);
  const fbHandler = new FeedbackHandler(context);
  codeFlowHandler = new CodeFlowHandler(context);
  setCodeFlowHandler(codeFlowHandler);

  editHandler.register();
  errHandler.register();
  fbHandler.register();
  codeFlowHandler.register();

  // হেল্পার ফাংশন: বর্তমান প্রজেক্টের কন্টেক্সট (Language/Framework) সংগ্রহ করা
  async function getProjectContext(): Promise<string> {
    const folders = vscode.workspace.workspaceFolders;
    if (!folders) return 'No workspace context';

    // ফাইল চেক করে ফ্রেমওয়ার্ক ডিটেক্ট করা
    const packageJson = await vscode.workspace.findFiles('package.json', null, 1);
    const buildGradle = await vscode.workspace.findFiles('build.gradle', null, 1);

    let context = 'Context: ';
    if (packageJson.length) context += 'React/Node.js Project. ';
    if (buildGradle.length) context += 'Java Spring Boot Project. ';

    const activeEditor = vscode.window.activeTextEditor;
    if (activeEditor) {
      context += `Current File: ${activeEditor.document.fileName} (${activeEditor.document.languageId})`;
    }
    return context;
  }


  // ইউজারের জন্য শুধুমাত্র চ্যাট ট্যাব রাখা হচ্ছে, বাকিগুলো অ্যাডমিন ড্যাশবোর্ডের জন্য
  // registerSidebarViews(context); // ড্যাশবোর্ড এবং কোড ফ্লো ভিউ সরানো হলো

  const recipeProvider = new SupremeWebviewProvider(context.extensionUri);
  context.subscriptions.push(
      vscode.window.registerWebviewViewProvider(SupremeWebviewProvider.viewType, recipeProvider)
  );
  // registerActivityView(context); // অ্যাক্টিভিটি ভিউ সরানো হলো
  registerChatProvider(context);
  registerInlineCompletionProvider(context, fbHandler);

  registerCommands(context);
  registerStatusBar(context);

  // নতুন কমান্ড: স্ট্যাটাস বার ক্লিক করলে চ্যাট ট্যাব খোলার জন্য
  context.subscriptions.push(
    vscode.commands.registerCommand('supremeai.openChat', () => {
      vscode.commands.executeCommand('workbench.view.extension.supremeaiChat');
    })
  );

  vscode.window.showInformationMessage(
    'SupremeAI Real-Time Learning is active!',
    'Settings'
  ).then(selection => {
    if (selection === 'Settings') {
      vscode.commands.executeCommand('workbench.action.openSettings', 'supremeai');
    }
  });

  console.log('[SupremeAI] Extension fully activated');
}

function registerCommands(context: vscode.ExtensionContext): void {
  const forceLearnCommand = vscode.commands.registerCommand('supremeai.forceLearn', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor to learn from');
      return;
    }

    const document = editor.document;
    const code = document.getText();
    const language = document.languageId;

    try {
      await supremeAIService.sendCodeAnalysis(document.fileName, code, language);
      vscode.window.showInformationMessage('Code analysis sent for learning');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      vscode.window.showErrorMessage(`Failed to send code analysis: ${message}`);
    }
  });

  const explainCodeCommand = vscode.commands.registerCommand('supremeai.explainCode', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor selected.');
      return;
    }
    const selection = editor.selection;
    const text = selection.isEmpty ? editor.document.getText() : editor.document.getText(selection);
    if (!text.trim()) {
      vscode.window.showWarningMessage('No code selected to explain.');
      return;
    }
    
    vscode.window.withProgress({
      location: vscode.ProgressLocation.Notification,
      title: 'Explaining Code...',
      cancellable: false
    }, async () => {
      try {
        const response = await supremeAIService.sendChatMessage({
          message: `Please explain the following code in detail:\n\n\`\`\`${editor.document.languageId}\n${text}\n\`\`\``,
          sessionId: supremeAIService.getSessionId()
        });
        const panel = vscode.window.createWebviewPanel(
          'supremeaiExplanation',
          'Code Explanation',
          vscode.ViewColumn.Two,
          {}
        );
        panel.webview.html = `<html><body><pre style="white-space: pre-wrap; font-family: sans-serif; padding: 15px;">${escapeHtml(response.response)}</pre></body></html>`;
      } catch (error) {
        vscode.window.showErrorMessage(`Failed to explain code: ${error}`);
      }
    });
  });

  const reviewCodeCommand = vscode.commands.registerCommand('supremeai.reviewCode', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor selected.');
      return;
    }
    const selection = editor.selection;
    const text = selection.isEmpty ? editor.document.getText() : editor.document.getText(selection);
    if (!text.trim()) {
      vscode.window.showWarningMessage('No code selected to review.');
      return;
    }
    
    vscode.window.withProgress({
      location: vscode.ProgressLocation.Notification,
      title: 'Reviewing Code...',
      cancellable: false
    }, async () => {
      try {
        const response = await supremeAIService.sendChatMessage({
          message: `Please review the following code for bugs, style issues, and performance optimizations:\n\n\`\`\`${editor.document.languageId}\n${text}\n\`\`\``,
          sessionId: supremeAIService.getSessionId()
        });
        const panel = vscode.window.createWebviewPanel(
          'supremeaiReview',
          'Code Review',
          vscode.ViewColumn.Two,
          {}
        );
        panel.webview.html = `<html><body><pre style="white-space: pre-wrap; font-family: sans-serif; padding: 15px;">${escapeHtml(response.response)}</pre></body></html>`;
      } catch (error) {
        vscode.window.showErrorMessage(`Failed to review code: ${error}`);
      }
    });
  });

  const loginAsGuestCommand = vscode.commands.registerCommand('supremeai.loginAsGuest', async () => {
    const auth = AuthService.getInstance();
    if (auth) {
      await auth.loginAsGuest();
    }
  });

  const loginCommand = vscode.commands.registerCommand('supremeai.login', async () => {
    const auth = AuthService.getInstance();
    if (auth) {
      await auth.login();
    }
  });

  const logoutCommand = vscode.commands.registerCommand('supremeai.logout', async () => {
    const auth = AuthService.getInstance();
    if (auth) {
      await auth.logout();
    }
  });

  context.subscriptions.push(
    forceLearnCommand,
    explainCodeCommand,
    reviewCodeCommand,
    loginAsGuestCommand,
    loginCommand,
    logoutCommand
  );
}

function registerSidebarViews(context: vscode.ExtensionContext): void {
  const dashboardProvider = new SupremeAISidebarProvider(context.extensionUri, 'supremeaiDashboard');
  const codeFlowProvider = new SupremeAISidebarProvider(context.extensionUri, 'supremeaiCodeFlow');

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('supremeaiDashboard', dashboardProvider),
    vscode.window.registerWebviewViewProvider('supremeaiCodeFlow', codeFlowProvider)
  );
}

function registerChatProvider(context: vscode.ExtensionContext): void {
  const chatProvider = new SupremeAIChatProvider(context);
  const adminDashboardProvider = new SupremeAIAdminDashboardProvider(context.extensionUri);
  const customerDashboardProvider = new SupremeAICustomerDashboardProvider(context.extensionUri);

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('supremeaiChat', chatProvider),
    vscode.window.registerWebviewViewProvider('supremeaiAdminDashboard', adminDashboardProvider),
    vscode.window.registerWebviewViewProvider('supremeaiCustomerDashboard', customerDashboardProvider),
    vscode.commands.registerCommand('supremeai.sendMessageToChat', (message?: string) => {
      let finalMessage = message;
      if (!finalMessage) {
        const editor = vscode.window.activeTextEditor;
        if (editor) {
          const selection = editor.selection;
          const text = editor.document.getText(selection);
          if (text) {
            finalMessage = `Please check this code:\n\n\`\`\`${editor.document.languageId}\n${text}\n\`\`\``;
          }
        }
      }
      if (finalMessage) {
        chatProvider.postMessageToChat(finalMessage);
      } else {
        vscode.window.showWarningMessage('No message or selection found to send to chat.');
      }
    })
  );
}

function registerActivityView(context: vscode.ExtensionContext): void {
  const activityProvider = new SupremeAIActivityProvider();
  context.subscriptions.push(
    vscode.window.registerTreeDataProvider('supremeaiActivity', activityProvider)
  );
}

function registerStatusBar(context: vscode.ExtensionContext): void {
  const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
  statusBarItem.text = '$(brain) SupremeAI';
  statusBarItem.tooltip = 'SupremeAI Assistant (Chat)';
  statusBarItem.command = 'supremeai.openChat'; // স্ট্যাটাস বার ক্লিক করলে চ্যাট ট্যাব খুলবে
  statusBarItem.show();
  context.subscriptions.push(statusBarItem);
}

function registerInlineCompletionProvider(context: vscode.ExtensionContext, fbHandler: FeedbackHandler): void {
  let debounceTimeout: NodeJS.Timeout | undefined;

  const provider: vscode.InlineCompletionItemProvider = {
    async provideInlineCompletionItems(
      document: vscode.TextDocument,
      position: vscode.Position,
      context: vscode.InlineCompletionContext,
      token: vscode.CancellationToken
    ): Promise<vscode.InlineCompletionList | vscode.InlineCompletionItem[] | undefined> {
      
      const config = vscode.workspace.getConfiguration('supremeai');
      const enableRealTimeLearning = config.get<boolean>('enableRealTimeLearning', true);
      if (!enableRealTimeLearning) {
        return undefined;
      }

      if (debounceTimeout) {
        clearTimeout(debounceTimeout);
      }

      return new Promise<vscode.InlineCompletionList | undefined>((resolve) => {
        debounceTimeout = setTimeout(async () => {
          if (token.isCancellationRequested) {
            resolve(undefined);
            return;
          }

          try {
            const docText = document.getText();
            const offset = document.offsetAt(position);
            const prefix = docText.substring(0, offset);
            const suffix = docText.substring(offset);

            const response = await supremeAIService.getInlineCompletions(
              prefix,
              suffix,
              document.fileName,
              document.languageId
            );

            if (token.isCancellationRequested) {
              resolve(undefined);
              return;
            }

            if (!response.suggestions || response.suggestions.length === 0) {
              resolve(undefined);
              return;
            }

            const items: vscode.InlineCompletionItem[] = response.suggestions.map((text) => {
              const item = new vscode.InlineCompletionItem(text);
              const suggestionId = `inline-${Date.now()}`;
              
              fbHandler.captureSuggestionContext(
                suggestionId,
                `completion-${Date.now()}`,
                '', 
                text,
                `File: ${document.uri.fsPath}`,
                position
              );

              item.command = {
                title: 'Accept Suggestion',
                command: 'supremeai.acceptSuggestion',
                arguments: [document.fileName, text, document.languageId]
              };
              return item;
            });

            resolve({ items });
          } catch (error) {
            console.error('[SupremeAI] Error fetching inline completion:', error);
            resolve(undefined);
          }
        }, 400); // 400ms debounce
      });
    }
  };

  const disposable = vscode.languages.registerInlineCompletionItemProvider(
    { pattern: '**' },
    provider
  );
  context.subscriptions.push(disposable);
  console.log('[SupremeAI] InlineCompletionItemProvider registered');
}

export function deactivate() {
  console.log('[SupremeAI] VS Code Extension deactivating...');
}
```