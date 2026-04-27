import * as vscode from 'vscode';
import { ChatViewProvider } from './providers/ChatViewProvider';
import { ProjectsProvider } from './providers/ProjectsProvider';
import { AgentsProvider } from './providers/AgentsProvider';
import { OrchestrationProvider } from './providers/OrchestrationProvider';
import { SupremeAIApi } from './services/SupremeAIApi';
import { OrchestrationView } from './views/OrchestrationView';
import { ProjectsView } from './views/ProjectsView';
import { AgentsView } from './views/AgentsView';
import { ChatView } from './views/ChatView';
import { DeploymentView } from './views/DeploymentView';

 export function activate(context: vscode.ExtensionContext) {
     console.log('SupremeAI extension is now active!');

     const config = vscode.workspace.getConfiguration('supremeai');
      const apiEndpoint = config.get<string>('apiEndpoint', 'https://supremeai-a.web.app');
     const apiKey = config.get<string>('apiKey', '');
     
     // Register Chat Webview Provider
     const chatViewProvider = new ChatViewProvider(context.extensionUri, apiEndpoint, apiKey);
     context.subscriptions.push(
         vscode.window.registerWebviewViewProvider(ChatViewProvider.viewType, chatViewProvider)
     );

     // Register projects provider
     const projectsProvider = new ProjectsProvider(apiEndpoint, apiKey);
     vscode.window.registerTreeDataProvider('supremeai-projects', projectsProvider);

     // Register agents provider
     const agentsProvider = new AgentsProvider(apiEndpoint, apiKey);
     vscode.window.registerTreeDataProvider('supremeai-agents', agentsProvider);

     // Register orchestration provider
     const orchestrationProvider = new OrchestrationProvider(apiEndpoint, apiKey);
     vscode.window.registerTreeDataProvider('supremeai-orchestration', orchestrationProvider);

     // Initialize views
     const orchestrationView = new OrchestrationView(context, orchestrationProvider);
     const projectsView = new ProjectsView(context, projectsProvider);
     const agentsView = new AgentsView(context, agentsProvider);

     // Status Bar Item
     const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
     statusBarItem.command = 'supremeai-chat-view.focus';
     statusBarItem.text = '$(sparkle) SupremeAI';
     statusBarItem.tooltip = 'Open SupremeAI Chat';
     statusBarItem.show();
     context.subscriptions.push(statusBarItem);

     // Register commands
     const api = new SupremeAIApi(apiEndpoint, apiKey);

     context.subscriptions.push(
         vscode.commands.registerCommand('supremeai.generateApp', async () => {
             const name = await vscode.window.showInputBox({
                 prompt: 'Enter the name of your new Android app',
                 placeHolder: 'MyAwesomeApp'
             });

             if (name) {
                 vscode.window.withProgress({
                     location: vscode.ProgressLocation.Notification,
                     title: `Generating ${name}...`,
                     cancellable: false
                 }, async (_progress) => {
                     const response = await api.generateApp({
                         name: name,
                         type: 'android',
                         features: ['auth', 'database']
                     });

                     if (response.success) {
                         vscode.window.showInformationMessage(`Success! Project ${response.projectId} created. APK: ${response.apkUrl}`);
                     } else {
                         vscode.window.showErrorMessage(`Failed to generate app: ${response.message}`);
                     }
                 });
             }
         }),
         vscode.commands.registerCommand('supremeai.addFeature', async () => {
             const feature = await vscode.window.showInputBox({
                 prompt: 'What feature would you like to add?',
                 placeHolder: 'e.g., Push notifications'
             });
             if (feature) {
                 chatViewProvider.sendMessage(`Add feature: ${feature}`);
             }
         }),
         vscode.commands.registerCommand('supremeai.reviewCode', () => {
             const editor = vscode.window.activeTextEditor;
             if (editor) {
                 chatViewProvider.sendMessage("Please review the current file for potential bugs and performance issues.");
             } else {
                 vscode.window.showWarningMessage("Open a file first to review code.");
             }
         }),
         vscode.commands.registerCommand('supremeai.deploy', () => {
             const deploymentView = DeploymentView.getInstance();
             deploymentView.showDeployment();
         }),
          vscode.commands.registerCommand('supremeai.chat', async () => {
              const input = await vscode.window.showInputBox({
                  prompt: 'Enter your message to SupremeAI',
                  placeHolder: 'e.g., Build a login page with Firebase auth'
              });
              
              if (input) {
                  chatViewProvider.sendMessage(input);
              }
          }),
          vscode.commands.registerCommand('supremeai.askAboutCode', () => {
              const editor = vscode.window.activeTextEditor;
              if (editor) {
                  const selection = editor.selection;
                  const text = editor.document.getText(selection);
                  if (text) {
                      chatViewProvider.sendMessage(`Explain this code:\n\n\`\`\`\n${text}\n\`\`\``);
                  }
              }
          }),
          vscode.commands.registerCommand('supremeai.explainFile', (uri: vscode.Uri) => {
              if (uri) {
                  chatViewProvider.sendMessage(`Explain the file: ${uri.fsPath}`);
              }
          }),
          vscode.commands.registerCommand('supremeai.refreshProjects', () => {
              projectsProvider.refresh();
          }),
          vscode.commands.registerCommand('supremeai.refreshAgents', () => {
              agentsProvider.refresh();
          })
     );
 }

export function deactivate() {}