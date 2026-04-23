import * as vscode from 'vscode';
import { SupremeAIApi } from './api';

export function activate(context: vscode.ExtensionContext) {
    const config = vscode.workspace.getConfiguration('supremeai');
    const apiUrl = config.get<string>('apiUrl') || 'https://supremeai-lhlwyikwlq-uc.a.run.app';
    const api = new SupremeAIApi(apiUrl);

    let generateApp = vscode.commands.registerCommand('supremeai.generateApp', async () => {
        try {
            const appName = await vscode.window.showInputBox({ prompt: 'App Name' });
            if (!appName) return;

            const features = await vscode.window.showQuickPick(
                ['Auth', 'Database', 'Push Notification', 'Payment'],
                { canPickMany: true, placeHolder: 'Select features' }
            );
            if (!features) return;

            await vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: 'Generating App...',
                cancellable: false
            }, async () => {
                const result = await api.generateApp({ name: appName, features });
                vscode.window.showInformationMessage(`App generated: ${result.downloadUrl}`);
            });
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to generate app: ${error}`);
        }
    });

    let codeReview = vscode.commands.registerCommand('supremeai.codeReview', async () => {
        try {
            const editor = vscode.window.activeTextEditor;
            if (!editor) {
                vscode.window.showWarningMessage('No active editor found');
                return;
            }

            const code = editor.document.getText();
            const review = await api.reviewCode(code);

            const diagnostics: vscode.Diagnostic[] = review.issues.map((issue: any) => {
                const line = issue.line || 0;
                return new vscode.Diagnostic(
                    new vscode.Range(line, 0, line, 100),
                    issue.description,
                    issue.severity === 'high'
                        ? vscode.DiagnosticSeverity.Error
                        : vscode.DiagnosticSeverity.Warning
                );
            });

            const collection = vscode.languages.createDiagnosticCollection('supremeai');
            collection.set(editor.document.uri, diagnostics);
            vscode.window.showInformationMessage(`Code review complete: ${diagnostics.length} issues found`);
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to review code: ${error}`);
        }
    });

    context.subscriptions.push(generateApp, codeReview);
}
