
import * as vscode from 'vscode';
import { ProjectsProvider } from '../providers/ProjectsProvider';

export class ProjectsView {
    constructor(
        private readonly context: vscode.ExtensionContext,
        private readonly provider: ProjectsProvider
    ) {}

    public async showProjectDetails(projectId: string) {
        const panel = vscode.window.createWebviewPanel(
            'supremeai.project',
            `Project: ${projectId}`,
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                retainContextWhenHidden: true
            }
        );

        panel.webview.html = this.getProjectWebviewContent(projectId);
    }

    private getProjectWebviewContent(projectId: string): string {
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
        .project-id {
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
        .file-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .file-item {
            padding: 8px 12px;
            margin: 5px 0;
            background-color: var(--vscode-editor-background);
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .file-name {
            flex: 1;
        }
        .file-actions {
            display: flex;
            gap: 5px;
        }
        .file-action-btn {
            padding: 4px 8px;
            font-size: 12px;
            border-radius: 3px;
            border: none;
            cursor: pointer;
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="project-id">${projectId}</div>
        <span class="status-badge active">Active</span>
    </div>

    <div class="section">
        <h3>📊 Project Information</h3>
        <div class="info-grid">
            <div class="info-item">
                <div class="info-label">Created</div>
                <div class="info-value">Just now</div>
            </div>
            <div class="info-item">
                <div class="info-label">Platform</div>
                <div class="info-value">Android</div>
            </div>
            <div class="info-item">
                <div class="info-label">Status</div>
                <div class="info-value">Building</div>
            </div>
            <div class="info-item">
                <div class="info-label">Build Time</div>
                <div class="info-value">~5 min</div>
            </div>
        </div>
    </div>

    <div class="section">
        <h3>📁 Generated Files</h3>
        <ul class="file-list">
            <li class="file-item">
                <span class="file-name">app/src/main/java/com/example/app/MainActivity.java</span>
                <div class="file-actions">
                    <button class="file-action-btn">View</button>
                    <button class="file-action-btn">Edit</button>
                </div>
            </li>
            <li class="file-item">
                <span class="file-name">app/src/main/AndroidManifest.xml</span>
                <div class="file-actions">
                    <button class="file-action-btn">View</button>
                    <button class="file-action-btn">Edit</button>
                </div>
            </li>
            <li class="file-item">
                <span class="file-name">app/build.gradle</span>
                <div class="file-actions">
                    <button class="file-action-btn">View</button>
                    <button class="file-action-btn">Edit</button>
                </div>
            </li>
        </ul>
    </div>

    <div class="action-buttons">
        <button class="btn btn-primary">📦 Download APK</button>
        <button class="btn btn-secondary">🔄 Rebuild</button>
        <button class="btn btn-secondary">⚙️ Configure</button>
    </div>
</body>
</html>`;
    }
}
