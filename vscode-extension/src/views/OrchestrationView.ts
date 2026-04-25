
import * as vscode from 'vscode';
import { OrchestrationProvider, OrchestrationItem } from '../providers/OrchestrationProvider';

export class OrchestrationView {
    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly provider: OrchestrationProvider
    ) {}

    public async showOrchestrationDetails(item: OrchestrationItem) {
        const panel = vscode.window.createWebviewPanel(
            'supremeai.orchestration',
            'Agent Orchestration Details',
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );

        const orchestration = this.provider.getCurrentOrchestration();

        panel.webview.html = this.getWebviewContent(orchestration);
    }

    private getWebviewContent(orchestration: any): string {
        if (!orchestration) {
            return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: var(--vscode-font-family);
            padding: 20px;
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
        }
        .no-data {
            text-align: center;
            padding: 40px;
            color: var(--vscode-descriptionForeground);
        }
        .icon {
            font-size: 48px;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <div class="no-data">
        <div class="icon">⚠️</div>
        <h2>No Active Orchestration</h2>
        <p>Start a new chat to begin agent orchestration.</p>
    </div>
</body>
</html>`;
        }

        const questions = orchestration.context?.questions || [];
        const decisions = orchestration.context?.decisions || [];
        const genContext = orchestration.generationContext || {};

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: var(--vscode-font-family);
            padding: 20px;
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
        }
        .section {
            margin-bottom: 30px;
            padding: 15px;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border-radius: 6px;
            border-left: 4px solid var(--vscode-textLink-foreground);
        }
        .section h2 {
            margin-top: 0;
            color: var(--vscode-textLink-foreground);
            font-size: 18px;
        }
        .item {
            padding: 10px;
            margin: 8px 0;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
        }
        .item-label {
            font-weight: bold;
            color: var(--vscode-textLink-foreground);
            margin-bottom: 5px;
        }
        .item-value {
            color: var(--vscode-foreground);
            line-height: 1.5;
        }
        .status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }
        .status.completed {
            background-color: #4CAF50;
            color: white;
        }
        .status.pending {
            background-color: #FF9800;
            color: white;
        }
        .status.active {
            background-color: #2196F3;
            color: white;
        }
        .context-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
            gap: 10px;
        }
        .context-item {
            padding: 10px;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
        }
        .context-key {
            font-weight: bold;
            color: var(--vscode-textLink-foreground);
            margin-bottom: 5px;
        }
        .context-value {
            color: var(--vscode-foreground);
            word-break: break-word;
        }
    </style>
</head>
<body>
    <div class="section">
        <h2>📊 Orchestration Status</h2>
        <span class="status ${orchestration.status.toLowerCase()}">${orchestration.status}</span>
        <p><strong>Requirement:</strong> ${orchestration.requirement}</p>
        <p><strong>Completed At:</strong> ${new Date(orchestration.completedAt).toLocaleString()}</p>
    </div>

    ${questions.length > 0 ? `
    <div class="section">
        <h2>❓ Generated Questions (${questions.length})</h2>
        ${questions.map((q: any, i: number) => `
            <div class="item">
                <div class="item-label">Question ${i + 1}</div>
                <div class="item-value">${q.text || q.question || 'No question text'}</div>
            </div>
        `).join('')}
    </div>
    ` : ''}

    ${decisions.length > 0 ? `
    <div class="section">
        <h2>✅ Agent Decisions (${decisions.length})</h2>
        ${decisions.map((d: any, i: number) => `
            <div class="item">
                <div class="item-label">${d.decisionKey || 'Decision ' + (i + 1)}</div>
                <div class="item-value">
                    <strong>AI Consensus:</strong> ${d.aiConsensus || 'No consensus'}<br>
                    ${d.reasoning ? `<strong>Reasoning:</strong> ${d.reasoning}` : ''}
                </div>
            </div>
        `).join('')}
    </div>
    ` : ''}

    ${Object.keys(genContext).length > 0 ? `
    <div class="section">
        <h2>🎯 Generation Context</h2>
        <div class="context-grid">
            ${Object.entries(genContext).map(([key, value]) => `
                <div class="context-item">
                    <div class="context-key">${key}</div>
                    <div class="context-value">${String(value)}</div>
                </div>
            `).join('')}
        </div>
    </div>
    ` : ''}
</body>
</html>`;
    }
}
