import * as vscode from 'vscode';
import { ChatProvider } from './providers/ChatProvider';
import { ProjectsProvider } from './providers/ProjectsProvider';
import { AgentsProvider } from './providers/AgentsProvider';

export function activate(context: vscode.ExtensionContext) {
    console.log('SupremeAI extension is now active!');

    // Get configuration
    const config = vscode.workspace.getConfiguration('supremeai');
    const apiKey = config.get<string>('apiKey', '');
    const apiEndpoint = config.get<string>('apiEndpoint', 'https://supremeai-lhlwyikwlq-uc.a.run.app');

    // Register chat provider
    const chatProvider = new ChatProvider(apiEndpoint, apiKey);
    vscode.window.registerTreeDataProvider('supremeai-chat', chatProvider);

    // Register projects provider
    const projectsProvider = new ProjectsProvider(apiEndpoint, apiKey);
    vscode.window.registerTreeDataProvider('supremeai-projects', projectsProvider);

    // Register agents provider
    const agentsProvider = new AgentsProvider(apiEndpoint, apiKey);
    vscode.window.registerTreeDataProvider('supremeai-agents', agentsProvider);

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('supremeai.generateApp', () => {
            vscode.window.showInformationMessage('Generating Android app...');
        }),
        vscode.commands.registerCommand('supremeai.addFeature', () => {
            vscode.window.showInformationMessage('Adding feature...');
        }),
        vscode.commands.registerCommand('supremeai.reviewCode', () => {
            vscode.window.showInformationMessage('Reviewing code...');
        }),
        vscode.commands.registerCommand('supremeai.deploy', () => {
            vscode.window.showInformationMessage('Deploying...');
        })
    );
}

export function deactivate() {}