import * as vscode from 'vscode';
import { SupremeAIApi } from '../services/SupremeAIApi';

export class ChatProvider implements vscode.TreeDataProvider<ChatItem> {
    private api: SupremeAIApi;

    constructor(apiEndpoint: string, apiKey?: string) {
        this.api = new SupremeAIApi(apiEndpoint);
        if (apiKey) {
            this.api.setApiKey(apiKey);
        }
    }
    getTreeItem(element: ChatItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: ChatItem): Thenable<ChatItem[]> {
        if (!element) {
            return Promise.resolve([
                new ChatItem('Welcome to SupremeAI!', vscode.TreeItemCollapsibleState.None),
                new ChatItem('Ask me anything about development', vscode.TreeItemCollapsibleState.None)
            ]);
        }
        return Promise.resolve([]);
    }
}

class ChatItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState
    ) {
        super(label, collapsibleState);
    }
}