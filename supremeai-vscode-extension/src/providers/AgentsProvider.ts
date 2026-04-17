import * as vscode from 'vscode';
import { SupremeAIApi } from '../services/SupremeAIApi';

export class AgentsProvider implements vscode.TreeDataProvider<AgentItem> {
    private api: SupremeAIApi;

    constructor(apiEndpoint: string, apiKey?: string) {
        this.api = new SupremeAIApi(apiEndpoint);
        if (apiKey) {
            this.api.setApiKey(apiKey);
        }
    }
    private _onDidChangeTreeData: vscode.EventEmitter<AgentItem | undefined | void> = new vscode.EventEmitter<AgentItem | undefined | void>();
    readonly onDidChangeTreeData: vscode.Event<AgentItem | undefined | void> = this._onDidChangeTreeData.event;

    getTreeItem(element: AgentItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: AgentItem): Thenable<AgentItem[]> {
        if (element) {
            return Promise.resolve([]);
        } else {
            return Promise.resolve([
                new AgentItem('X-Builder', 'Active', vscode.TreeItemCollapsibleState.None, 'status-online'),
                new AgentItem('Y-Reviewer', 'Waiting', vscode.TreeItemCollapsibleState.None, 'status-warning'),
                new AgentItem('Z-Architect', 'Standby', vscode.TreeItemCollapsibleState.None, 'status-offline')
            ]);
        }
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }
}

class AgentItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        private readonly status: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        private readonly iconName: string
    ) {
        super(label, collapsibleState);
        this.tooltip = `${this.label} - ${this.status}`;
        this.description = this.status;
        this.iconPath = new vscode.ThemeIcon(this.iconName);
    }
}
