import * as vscode from 'vscode';
import { SupremeAIApi } from './services/SupremeAIApi';
import { AgentsProvider } from './providers/AgentsProvider';
import { ChatProvider } from './providers/ChatProvider';
import { ProjectsProvider } from './providers/ProjectsProvider';

export function activate(context: vscode.ExtensionContext) {
    console.log('SupremeAI Extension is now active!');

    const api = new SupremeAIApi('https://supremeai-565236080752.us-central1.run.app');

    // Register Chat Provider
    const chatProvider = new ChatProvider(context.extensionUri);
    vscode.window.registerWebviewViewProvider('supremeai-chat', chatProvider);

    // Register Agents Provider
    const agentsProvider = new AgentsProvider();
    vscode.window.registerTreeDataProvider('supremeai-agents', agentsProvider);

    // Register Projects Provider
    const projectsProvider = new ProjectsProvider();
    vscode.window.registerTreeDataProvider('supremeai-projects', projectsProvider);

    // Command: Generate App
    let generateAppCommand = vscode.commands.registerCommand('supremeai.generateApp', async () => {
        const appName = await vscode.window.showInputBox({
            prompt: 'Enter the name of your new Android app',
            placeHolder: 'MyAwesomeApp'
        });

        if (!appName) return;

        vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: "SupremeAI: Generating Android App...",
            cancellable: false
        }, async (_progress) => {
            const result = await api.generateApp({
                name: appName,
                type: 'android',
                features: ['auth', 'database', 'navigation']
            });

            if (result.success) {
                vscode.window.showInformationMessage(
                    `App "${appName}" generated successfully! APK: ${result.apkUrl}`,
                    'Open Dashboard'
                ).then(selection => {
                    if (selection === 'Open Dashboard') {
                        vscode.env.openExternal(vscode.Uri.parse('https://supremeai-565236080752.us-central1.run.app/dashboard'));
                    }
                });
            } else {
                vscode.window.showErrorMessage(`Failed to generate app: ${result.message}`);
            }
        });
    });

    // Learning Mode Integration
    const learningListener = vscode.workspace.onDidChangeTextDocument((event) => {
        const document = event.document;
        // Basic filter: only learn from supported languages
        if (['java', 'kotlin', 'typescript', 'javascript', 'python'].includes(document.languageId)) {
            const code = document.getText();
            api.learn({
                language: document.languageId,
                fileName: document.fileName,
                content: code,
                timestamp: Date.now()
            });
        }
    });

    context.subscriptions.push(generateAppCommand, learningListener);

    // Register simple commands for other features
    context.subscriptions.push(
        vscode.commands.registerCommand('supremeai.addFeature', () => {
            vscode.window.showInformationMessage('Feature added (Mock)');
        }),
        vscode.commands.registerCommand('supremeai.reviewCode', () => {
            vscode.window.showInformationMessage('Code review started (Mock)');
        }),
        vscode.commands.registerCommand('supremeai.deploy', () => {
            vscode.window.showInformationMessage('Deployment started (Mock)');
        })
    );
}

export function deactivate() {}
