
import * as vscode from 'vscode';
import { SupremeAIApi } from '../services/SupremeAIApi';

export class DeploymentView {
    private static instance: DeploymentView;
    private currentPanel?: vscode.WebviewPanel;
    private disposables: vscode.Disposable[] = [];
    private api: SupremeAIApi;

    private constructor() {
        const config = vscode.workspace.getConfiguration('supremeai');
        const apiEndpoint = config.get<string>('apiEndpoint', 'https://supremeai-a.web.app');
        const apiKey = config.get<string>('apiKey', '');
        this.api = new SupremeAIApi(apiEndpoint, apiKey);
    }

    public static getInstance(): DeploymentView {
        if (!DeploymentView.instance) {
            DeploymentView.instance = new DeploymentView();
        }
        return DeploymentView.instance;
    }

    public showDeployment(projectId?: string): void {
        if (this.currentPanel) {
            this.currentPanel.reveal(vscode.ViewColumn.One);
            return;
        }

        this.currentPanel = vscode.window.createWebviewPanel(
            'supremeai.deployment',
            'Deployment',
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );

        this.currentPanel.webview.html = this.getWebviewContent(projectId);

        this.currentPanel.onDidDispose(() => {
            this.currentPanel = undefined;
            this.dispose();
        });

        this.currentPanel.webview.onDidReceiveMessage(
            async message => {
                switch (message.type) {
                    case 'deploy':
                        await this.handleDeploy(message.platform, message.config);
                        break;
                    case 'checkStatus':
                        await this.checkDeploymentStatus(message.deploymentId);
                        break;
                    case 'viewLogs':
                        await this.viewDeploymentLogs(message.deploymentId);
                        break;
                }
            },
            null,
            this.disposables
        );
    }

    private async handleDeploy(platform: string, config: any): Promise<void> {
        try {
            const response = await this.api.createPublishingPlan({
                platform,
                config
            });

            if (response.status === 'SUCCESS') {
                vscode.window.showInformationMessage(`Deployment plan created for ${platform}`);
                this.currentPanel?.webview.postMessage({
                    type: 'deploymentStarted',
                    deploymentId: response.publishingPlan.id,
                    platform
                });
            } else {
                vscode.window.showErrorMessage('Failed to create deployment plan');
            }
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);
            vscode.window.showErrorMessage(`Deployment failed: ${errorMessage}`);
        }
    }

    private async checkDeploymentStatus(deploymentId: string): Promise<void> {
        // Implementation for checking deployment status
        vscode.window.showInformationMessage(`Checking status for deployment: ${deploymentId}`);
    }

    private async viewDeploymentLogs(deploymentId: string): Promise<void> {
        // Implementation for viewing deployment logs
        vscode.window.showInformationMessage(`Opening logs for deployment: ${deploymentId}`);
    }

    private getWebviewContent(projectId?: string): string {
        const platforms = [
            { id: 'android', name: 'Android', icon: '🤖' },
            { id: 'ios', name: 'iOS', icon: '🍎' },
            { id: 'web', name: 'Web', icon: '🌐' },
            { id: 'cloud', name: 'Cloud', icon: '☁️' }
        ];

        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Deployment</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: var(--vscode-font-family);
            height: 100vh;
            display: flex;
            flex-direction: column;
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
            padding: 20px;
        }

        .header {
            margin-bottom: 30px;
            padding-bottom: 15px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }

        .header h1 {
            font-size: 24px;
            font-weight: 600;
            color: var(--vscode-textLink-foreground);
            margin-bottom: 10px;
        }

        .project-info {
            color: var(--vscode-descriptionForeground);
            font-size: 14px;
        }

        .section {
            margin-bottom: 30px;
            padding: 20px;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border-radius: 8px;
        }

        .section h2 {
            font-size: 18px;
            color: var(--vscode-textLink-foreground);
            margin-bottom: 20px;
        }

        .platform-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 15px;
        }

        .platform-card {
            padding: 20px;
            background-color: var(--vscode-editor-background);
            border: 2px solid var(--vscode-panel-border);
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
            text-align: center;
        }

        .platform-card:hover {
            border-color: var(--vscode-textLink-foreground);
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        }

        .platform-icon {
            font-size: 48px;
            margin-bottom: 10px;
        }

        .platform-name {
            font-weight: bold;
            color: var(--vscode-foreground);
            margin-bottom: 5px;
        }

        .platform-status {
            font-size: 12px;
            color: var(--vscode-descriptionForeground);
        }

        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: bold;
            text-transform: uppercase;
        }

        .status-badge.ready {
            background-color: #4CAF50;
            color: white;
        }

        .status-badge.deploying {
            background-color: #2196F3;
            color: white;
        }

        .status-badge.failed {
            background-color: #f44336;
            color: white;
        }

        .deployment-status {
            margin-top: 20px;
            padding: 15px;
            background-color: var(--vscode-editor-background);
            border-radius: 6px;
            border: 1px solid var(--vscode-panel-border);
        }

        .status-item {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid var(--vscode-panel-border);
        }

        .status-item:last-child {
            border-bottom: none;
        }

        .status-label {
            color: var(--vscode-descriptionForeground);
        }

        .status-value {
            font-weight: bold;
            color: var(--vscode-foreground);
        }

        .action-buttons {
            display: flex;
            gap: 10px;
            margin-top: 20px;
        }

        .btn {
            padding: 10px 20px;
            border-radius: 6px;
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

        .logs-container {
            margin-top: 20px;
            padding: 15px;
            background-color: var(--vscode-editor-background);
            border-radius: 6px;
            border: 1px solid var(--vscode-panel-border);
            max-height: 300px;
            overflow-y: auto;
        }

        .log-entry {
            padding: 5px 0;
            font-family: monospace;
            font-size: 12px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }

        .log-entry:last-child {
            border-bottom: none;
        }

        .log-time {
            color: var(--vscode-descriptionForeground);
            margin-right: 10px;
        }

        .log-message {
            color: var(--vscode-foreground);
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🚀 Deploy Your App</h1>
        <div class="project-info">
            ${projectId ? `Project: ${projectId}` : 'No project selected'}
        </div>
    </div>

    <div class="section">
        <h2>📱 Choose Platform</h2>
        <div class="platform-grid">
            ${platforms.map(platform => `
                <div class="platform-card" data-platform="${platform.id}">
                    <div class="platform-icon">${platform.icon}</div>
                    <div class="platform-name">${platform.name}</div>
                    <div class="platform-status">Ready to deploy</div>
                </div>
            `).join('')}
        </div>
    </div>

    <div class="section" id="deployment-status-section" style="display: none;">
        <h2>📊 Deployment Status</h2>
        <div class="deployment-status">
            <div class="status-item">
                <span class="status-label">Status</span>
                <span class="status-value"><span class="status-badge deploying">Deploying</span></span>
            </div>
            <div class="status-item">
                <span class="status-label">Platform</span>
                <span class="status-value" id="status-platform">-</span>
            </div>
            <div class="status-item">
                <span class="status-label">Started</span>
                <span class="status-value" id="status-started">-</span>
            </div>
            <div class="status-item">
                <span class="status-label">Progress</span>
                <span class="status-value" id="status-progress">0%</span>
            </div>
        </div>

        <div class="action-buttons">
            <button class="btn btn-primary" id="check-status-btn">Check Status</button>
            <button class="btn btn-secondary" id="view-logs-btn">View Logs</button>
        </div>

        <div class="logs-container" id="logs-container">
            <div class="log-entry">
                <span class="log-time">${new Date().toLocaleTimeString()}</span>
                <span class="log-message">Deployment initialized...</span>
            </div>
        </div>
    </div>

    <script>
        const vscode = acquireVsCodeApi();
        const platformCards = document.querySelectorAll('.platform-card');
        const statusSection = document.getElementById('deployment-status-section');
        const statusPlatform = document.getElementById('status-platform');
        const statusStarted = document.getElementById('status-started');
        const statusProgress = document.getElementById('status-progress');
        const checkStatusBtn = document.getElementById('check-status-btn');
        const viewLogsBtn = document.getElementById('view-logs-btn');
        const logsContainer = document.getElementById('logs-container');

        let currentDeploymentId = null;

        platformCards.forEach(card => {
            card.addEventListener('click', () => {
                const platform = card.dataset.platform;
                vscode.postMessage({
                    type: 'deploy',
                    platform: platform,
                    config: {}
                });
            });
        });

        window.addEventListener('message', event => {
            const message = event.data;
            switch (message.type) {
                case 'deploymentStarted':
                    currentDeploymentId = message.deploymentId;
                    statusSection.style.display = 'block';
                    statusPlatform.textContent = message.platform;
                    statusStarted.textContent = new Date().toLocaleString();
                    statusProgress.textContent = '0%';
                    addLog('Deployment started for ' + message.platform);
                    break;
                case 'deploymentProgress':
                    statusProgress.textContent = message.progress + '%';
                    addLog(message.message);
                    break;
                case 'deploymentComplete':
                    statusProgress.textContent = '100%';
                    addLog('Deployment completed successfully!');
                    break;
                case 'deploymentError':
                    addLog('Error: ' + message.error);
                    break;
            }
        });

        function addLog(message) {
            const logEntry = document.createElement('div');
            logEntry.className = 'log-entry';
            logEntry.innerHTML = \`
                <span class="log-time">\${new Date().toLocaleTimeString()}</span>
                <span class="log-message">\${message}</span>
            \`;
            logsContainer.appendChild(logEntry);
            logsContainer.scrollTop = logsContainer.scrollHeight;
        }

        checkStatusBtn.addEventListener('click', () => {
            if (currentDeploymentId) {
                vscode.postMessage({
                    type: 'checkStatus',
                    deploymentId: currentDeploymentId
                });
            }
        });

        viewLogsBtn.addEventListener('click', () => {
            if (currentDeploymentId) {
                vscode.postMessage({
                    type: 'viewLogs',
                    deploymentId: currentDeploymentId
                });
            }
        });
    </script>
</body>
</html>`;
    }

    private dispose(): void {
        this.disposables.forEach(d => d.dispose());
        this.disposables = [];
    }
}
