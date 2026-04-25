
import * as vscode from 'vscode';
import { AgentsProvider } from '../providers/AgentsProvider';

export class AgentsView {
    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly provider: AgentsProvider
    ) {}

    public async showAgentDetails(agentId: string) {
        const panel = vscode.window.createWebviewPanel(
            'supremeai.agent',
            `Agent: ${agentId}`,
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );

        panel.webview.html = this.getAgentWebviewContent(agentId);
    }

    private getAgentWebviewContent(agentId: string): string {
        const agentConfig = this.getAgentConfig(agentId);

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
        .header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }
        .agent-name {
            font-size: 24px;
            font-weight: bold;
            color: var(--vscode-textLink-foreground);
        }
        .status-badge {
            padding: 6px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }
        .status-badge.active {
            background-color: #4CAF50;
            color: white;
        }
        .section {
            margin-bottom: 25px;
            padding: 15px;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border-radius: 6px;
        }
        .section h3 {
            margin-top: 0;
            color: var(--vscode-textLink-foreground);
            font-size: 16px;
        }
        .info-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 15px;
        }
        .info-item {
            padding: 10px;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
        }
        .info-label {
            font-size: 12px;
            color: var(--vscode-descriptionForeground);
            margin-bottom: 5px;
        }
        .info-value {
            font-weight: bold;
            color: var(--vscode-foreground);
        }
        .capability-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .capability-item {
            padding: 8px 12px;
            margin: 5px 0;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .capability-icon {
            font-size: 18px;
        }
        .capability-name {
            flex: 1;
            font-weight: bold;
        }
        .capability-desc {
            font-size: 12px;
            color: var(--vscode-descriptionForeground);
        }
        .action-buttons {
            display: flex;
            gap: 10px;
            margin-top: 20px;
        }
        .btn {
            padding: 8px 16px;
            border-radius: 4px;
            border: none;
            cursor: pointer;
            font-weight: bold;
            transition: all 0.2s;
        }
        .btn-primary {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
        }
        .btn-primary:hover {
            background-color: var(--vscode-button-hoverBackground);
        }
        .btn-secondary {
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }
        .btn-secondary:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }
        .config-section {
            margin-top: 15px;
        }
        .config-item {
            padding: 10px;
            margin: 8px 0;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
        }
        .config-label {
            font-weight: bold;
            color: var(--vscode-textLink-foreground);
            margin-bottom: 5px;
        }
        .config-value {
            color: var(--vscode-foreground);
            font-family: monospace;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="agent-name">${agentConfig.name}</div>
        <span class="status-badge active">${agentConfig.status}</span>
    </div>

    <div class="section">
        <h3>📊 Agent Information</h3>
        <div class="info-grid">
            <div class="info-item">
                <div class="info-label">Type</div>
                <div class="info-value">${agentConfig.type}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Model</div>
                <div class="info-value">${agentConfig.model}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Tasks Completed</div>
                <div class="info-value">${agentConfig.tasksCompleted}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Success Rate</div>
                <div class="info-value">${agentConfig.successRate}%</div>
            </div>
        </div>
    </div>

    <div class="section">
        <h3>⚡ Capabilities</h3>
        <ul class="capability-list">
            ${agentConfig.capabilities.map(cap => `
                <li class="capability-item">
                    <span class="capability-icon">${cap.icon}</span>
                    <div class="capability-name">${cap.name}</div>
                    <div class="capability-desc">${cap.description}</div>
                </li>
            `).join('')}
        </ul>
    </div>

    <div class="section">
        <h3>⚙️ Configuration</h3>
        <div class="config-section">
            ${Object.entries(agentConfig.config).map(([key, value]) => `
                <div class="config-item">
                    <div class="config-label">${key}</div>
                    <div class="config-value">${String(value)}</div>
                </div>
            `).join('')}
        </div>
    </div>

    <div class="action-buttons">
        <button class="btn btn-primary">🔄 Restart Agent</button>
        <button class="btn btn-secondary">⚙️ Configure</button>
        <button class="btn btn-secondary">📊 View Logs</button>
    </div>
</body>
</html>`;
    }

    private getAgentConfig(agentId: string): any {
        const agentConfigs: Record<string, any> = {
            'x-builder': {
                name: 'X-Builder',
                status: 'Active',
                type: 'Code Generator',
                model: 'GPT-4',
                tasksCompleted: 156,
                successRate: 94,
                capabilities: [
                    { icon: '🔧', name: 'Code Generation', description: 'Generate production-ready code' },
                    { icon: '📝', name: 'Documentation', description: 'Auto-generate docs' },
                    { icon: '🧪', name: 'Testing', description: 'Create unit tests' }
                ],
                config: {
                    'Temperature': 0.7,
                    'Max Tokens': 2000,
                    'Timeout': '30s'
                }
            },
            'z-architect': {
                name: 'Z-Architect',
                status: 'Active',
                type: 'System Architect',
                model: 'Claude 3.5',
                tasksCompleted: 89,
                successRate: 97,
                capabilities: [
                    { icon: '🏗️', name: 'Architecture Design', description: 'Design system architecture' },
                    { icon: '📊', name: 'Performance Analysis', description: 'Analyze and optimize' },
                    { icon: '🔒', name: 'Security Review', description: 'Security assessment' }
                ],
                config: {
                    'Temperature': 0.5,
                    'Max Tokens': 4000,
                    'Timeout': '60s'
                }
            }
        };

        return agentConfigs[agentId] || {
            name: agentId,
            status: 'Unknown',
            type: 'Custom',
            model: 'Default',
            tasksCompleted: 0,
            successRate: 0,
            capabilities: [],
            config: {}
        };
    }
}
