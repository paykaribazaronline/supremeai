/**
 * SupremeAI VS Code Extension - Main Entry Point
 * Real-time learning and AI assistance integration
 */

import * as vscode from 'vscode';
import { SupremeAIService, getSupremeAIService, setSupremeAIService } from './services/SupremeAIService';
import { CodeEditHandler } from './handlers/CodeEditHandler';
import { ErrorHandler } from './handlers/ErrorHandler';
import { FeedbackHandler } from './handlers/FeedbackHandler';
import { SupremeAIConfig } from './types';

let supremeAIService: SupremeAIService;
let codeEditHandler: CodeEditHandler;
let errorHandler: ErrorHandler;
let feedbackHandler: FeedbackHandler;

export function activate(context: vscode.ExtensionContext) {
  console.log('[SupremeAI] VS Code Extension activating...');

  // Read configuration
  const config = vscode.workspace.getConfiguration('supremeai');
  const backendUrl = config.get<string>('backendUrl', 'https://supremeai-lhlwyikwlq-uc.a.run.app');
  
  const supremeConfig: SupremeAIConfig = {
    backendUrl,
    enableRealTimeLearning: config.get<boolean>('enableRealTimeLearning', true),
    autoReportErrors: config.get<boolean>('autoReportErrors', true),
  };

  // Initialize SupremeAI Service
  supremeAIService = new SupremeAIService(supremeConfig);
  setSupremeAIService(supremeAIService);

  // Initialize Handlers
  codeEditHandler = new CodeEditHandler(context);
  errorHandler = new ErrorHandler(context);
  feedbackHandler = new FeedbackHandler(context);

  // Register handlers
  codeEditHandler.register();
  errorHandler.register();
  feedbackHandler.register();

  // Register commands
  registerCommands(context);

  // Register status bar item
  registerStatusBar(context);

  // Notify user extension is active
  vscode.window.showInformationMessage(
    '🚀 SupremeAI Real-Time Learning is active!',
    'Settings'
  ).then(selection => {
    if (selection === 'Settings') {
      vscode.commands.executeCommand('workbench.action.openSettings', 'supremeai');
    }
  });

  console.log('[SupremeAI] Extension fully activated');
}

function registerCommands(context: vscode.ExtensionContext): void {
  // Force learn from current file
  const forceLearnCommand = vscode.commands.registerCommand('supremeai.forceLearn', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showWarningMessage('No active editor to learn from');
      return;
    }

    const filePath = editor.document.uri.fsPath;
    const code = editor.document.getText();
    
    vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: 'SupremeAI Learning',
        cancellable: false,
      },
      async (progress) => {
        progress.report({ increment: 0, message: 'Analyzing code...' });
        
        const service = getSupremeAIService();
        const result = await service.sendCodeAnalysis(filePath, code, editor.document.languageId);
        
        progress.report({ increment: 100, message: 'Complete!' });
        
        if (result.success) {
          vscode.window.showInformationMessage('✅ Code analysis sent to SupremeAI learning engine');
        } else {
          vscode.window.showErrorMessage(`❌ Learning failed: ${result.message}`);
        }
      }
    );
  });

  // Report error manually
  const reportErrorCommand = vscode.commands.registerCommand('supremeai.reportError', async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      return;
    }

    const selection = editor.selection;
    const lineNumber = selection.start.line + 1;
    const errorMessage = await vscode.window.showInputBox({
      prompt: 'Enter error message',
      placeHolder: 'e.g., TypeError: Cannot read property...',
    });

    if (errorMessage) {
      await errorHandler.reportManualError(
        editor.document.uri.fsPath,
        lineNumber,
        errorMessage,
        'compilation'
      );
      vscode.window.showInformationMessage('✅ Error reported to SupremeAI');
    }
  });

  // Send feedback command
  const sendFeedbackCommand = vscode.commands.registerCommand('supremeai.sendFeedback', async () => {
    await feedbackHandler['showFeedbackDialog']();
  });

  context.subscriptions.push(
    forceLearnCommand,
    reportErrorCommand,
    sendFeedbackCommand
  );

  console.log('[SupremeAI] Commands registered');
}

function registerStatusBar(context: vscode.ExtensionContext): void {
  const statusBarItem = vscode.window.createStatusBarItem(
    vscode.StatusBarAlignment.Right,
    100
  );
  
  statusBarItem.text = '$(circuit-board) SupremeAI';
  statusBarItem.tooltip = 'SupremeAI Real-Time Learning Active';
  statusBarItem.command = 'supremeai.forceLearn';
  statusBarItem.show();

  // Update status periodically
  setInterval(() => {
    const service = getSupremeAIService();
    const stats = service.getLearningStats();
    if (stats) {
      statusBarItem.tooltip = `SupremeAI: ${stats.learningCount || 0} patterns learned`;
    }
  }, 30000);

  context.subscriptions.push(statusBarItem);
}

export function deactivate(): void {
  console.log('[SupremeAI] VS Code Extension deactivating...');
}
