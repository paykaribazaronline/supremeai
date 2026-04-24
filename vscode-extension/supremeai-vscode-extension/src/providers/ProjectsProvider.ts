import * as vscode from 'vscode';
import { SupremeAIApi } from '../services/SupremeAIApi';

export class ProjectsProvider implements vscode.TreeDataProvider<ProjectItem> {
    private api: SupremeAIApi;

    constructor(apiEndpoint: string, apiKey?: string) {
        this.api = new SupremeAIApi(apiEndpoint);
        if (apiKey) {
            this.api.setApiKey(apiKey);
        }
    }
    private _onDidChangeTreeData: vscode.EventEmitter<ProjectItem | undefined | void> = new vscode.EventEmitter<ProjectItem | undefined | void>();
    readonly onDidChangeTreeData: vscode.Event<ProjectItem | undefined | void> = this._onDidChangeTreeData.event;

    getTreeItem(element: ProjectItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: ProjectItem): Thenable<ProjectItem[]> {
        if (element) {
            return Promise.resolve([]);
        } else {
            // Mock data for now, similar to Agents
            return Promise.resolve([
                new ProjectItem('SupremeAI Mobile', 'Android', vscode.TreeItemCollapsibleState.None),
                new ProjectItem('SupremeAI Web', 'React', vscode.TreeItemCollapsibleState.None),
                new ProjectItem('Admin Dashboard', 'Spring Boot', vscode.TreeItemCollapsibleState.None)
            ]);
        }
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }
}

class ProjectItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        private readonly type: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState
    ) {
        super(label, collapsibleState);
        this.tooltip = `${this.label} (${this.type})`;
        this.description = this.type;
        this.iconPath = new vscode.ThemeIcon('project');
    }
}
