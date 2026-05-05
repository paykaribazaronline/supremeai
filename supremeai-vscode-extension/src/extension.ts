/**
 * SupremeAI VS Code Extension - Main Entry Point
 * Real-time learning and AI assistance integration
 */

import * as vscode from 'vscode';
import { SupremeAIService, getSupremeAIService, setSupremeAIService } from './services/SupremeAIService';
import { CodeEditHandler } from './handlers/CodeEditHandler';
import { ErrorHandler } from './handlers/ErrorHandler';
import { FeedbackHandler } from './handlers/FeedbackHandler';
import { CodeFlowHandler, setCodeFlowHandler } from './handlers/CodeFlowHandler';
import { SupremeAIConfig } from './types';
import { SupremeAISidebarProvider } from './providers/SupremeAISidebarProvider';
import { SupremeAIActivityProvider } from './providers/SupremeAIActivityProvider';
import { SupremeAIChatProvider } from './providers/SupremeAIChatProvider';

let supremeAIService: SupremeAIService;
let codeEditHandler: CodeEditHandler;
let errorHandler: ErrorHandler;
let feedbackHandler: FeedbackHandler;
let codeFlowHandler: CodeFlowHandler;

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
  codeFlowHandler = new CodeFlowHandler(context);
  setCodeFlowHandler(codeFlowHandler);

  // Register handlers
  codeEditHandler.register();
  errorHandler.register();
  feedbackHandler.register();
  codeFlowHandler.register();

  // Register sidebar views
  registerSidebarViews(context);

  // Register chat provider
  registerChatProvider(context);

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

    const document = editor.document;
    const code = document.getText();
    const language = document.languageId;

    try {
      await supremeAIService.sendCodeAnalysis(document.fileName, code, language);
      vscode.window.showInformationMessage('Code analysis sent for learning');
    } catch (error: any) {
      vscode.window.showErrorMessage(`Failed to send code analysis: ${error.message}`);
    }
  });

  // CodeFlow analysis
  const analyzeCodeFlowCommand = vscode.commands.registerCommand('supremeai.analyzeCodeFlow', () => {
    codeFlowHandler.analyzeCodeFlow();
  });

  // Resolve error
  const resolveErrorCommand = vscode.commands.registerCommand('supremeai.resolveError', () => {
    codeFlowHandler.resolveError();
  });

  // Show security issues
  const showSecurityIssuesCommand = vscode.commands.registerCommand('supremeai.showSecurityIssues', () => {
    codeFlowHandler.showSecurityIssues();
  });

  // Show dependencies
  const showDependenciesCommand = vscode.commands.registerCommand('supremeai.showDependencies', () => {
    codeFlowHandler.showDependencies();
  });

  // Open CodeFlow dashboard
  const openCodeFlowDashboardCommand = vscode.commands.registerCommand('supremeai.openCodeFlowDashboard', () => {
    codeFlowHandler.openCodeFlowDashboard();
  });

  // Refresh CodeFlow analysis
  const refreshCodeFlowCommand = vscode.commands.registerCommand('supremeai.refreshCodeFlow', () => {
    codeFlowHandler.refreshAnalysis();
  });

  context.subscriptions.push(
    forceLearnCommand,
    analyzeCodeFlowCommand,
    resolveErrorCommand,
    showSecurityIssuesCommand,
    showDependenciesCommand,
    openCodeFlowDashboardCommand,
    refreshCodeFlowCommand
  );
}

function registerSidebarViews(context: vscode.ExtensionContext): void {
  const sidebarProvider = new SupremeAISidebarProvider(context);
  const activityProvider = new SupremeAIActivityProvider(context);

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('supremeai.sidebar', sidebarProvider),
    vscode.window.registerWebviewViewProvider('supremeai.activity', activityProvider)
  );
}

function registerChatProvider(context: vscode.ExtensionContext): void {
  const chatProvider = new SupremeAIChatProvider(context);
  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider('supremeai.chat', chatProvider)
  );
}

function registerStatusBar(context: vscode.ExtensionContext): void {
  const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
  statusBarItem.text = '$(brain) SupremeAI';
  statusBarItem.tooltip = 'SupremeAI Assistant';
  statusBarItem.command = 'supremeai.forceLearn';
  statusBarItem.show();
  context.subscriptions.push(statusBarItem);
}

export function deactivate() {
  console.log('[SupremeAI] VS Code Extension deactivating...');
}
