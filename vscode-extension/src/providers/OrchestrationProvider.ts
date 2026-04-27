
import * as vscode from 'vscode';
import { SupremeAIApi, OrchestrateRequest, OrchestrateResponse } from '../services/SupremeAIApi';

export class OrchestrationItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        private readonly statusText: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        private readonly iconName: string,
        public readonly descriptionText?: string
    ) {
        super(label, collapsibleState);
        this.tooltip = `${this.label} - ${this.statusText}`;
        this.description = descriptionText || this.statusText;
        this.iconPath = new vscode.ThemeIcon(this.iconName);
    }
}

export class OrchestrationProvider implements vscode.TreeDataProvider<OrchestrationItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<OrchestrationItem | undefined | void> = new vscode.EventEmitter<OrchestrationItem | undefined | void>();
    readonly onDidChangeTreeData: vscode.Event<OrchestrationItem | undefined | void> = this._onDidChangeTreeData.event;

    private api: SupremeAIApi;
    private currentOrchestration?: OrchestrateResponse;

    constructor(apiEndpoint: string, apiKey?: string) {
        this.api = new SupremeAIApi(apiEndpoint, apiKey);
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: OrchestrationItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: OrchestrationItem): Thenable<OrchestrationItem[]> {
        if (element) {
            return Promise.resolve([]);
        } else {
            // মূল আইটেমগুলি ফেরত দিন
            const items: OrchestrationItem[] = [
                new OrchestrationItem(
                    'এজেন্ট অর্কেস্ট্রেশন', 
                    'সক্রিয়', 
                    vscode.TreeItemCollapsibleState.None, 
                    'server',
                    'প্রয়োজনীয়তা বিশ্লেষণ এবং সিদ্ধান্ত গ্রহণ'
                ),
                new OrchestrationItem(
                    'প্রশ্ন জেনারেশন', 
                    this.currentOrchestration ? 'সম্পন্ন' : 'অপেক্ষমাণ', 
                    vscode.TreeItemCollapsibleState.None, 
                    'question',
                    this.currentOrchestration ? `${(this.currentOrchestration.context['questions'] as any[])?.length || 0} প্রশ্ন` : 'কোন প্রশ্ন নেই'
                ),
                new OrchestrationItem(
                    'সিদ্ধান্ত গ্রহণ', 
                    this.currentOrchestration ? 'সম্পন্ন' : 'অপেক্ষমাণ', 
                    vscode.TreeItemCollapsibleState.None, 
                    'check',
                    this.currentOrchestration ? `${(this.currentOrchestration.context['decisions'] as any[])?.length || 0} সিদ্ধান্ত` : 'কোন সিদ্ধান্ত নেই'
                ),
                new OrchestrationItem(
                    'কোড জেনারেশন', 
                    this.currentOrchestration ? 'প্রস্তুত' : 'অপেক্ষমাণ', 
                    vscode.TreeItemCollapsibleState.None, 
                    'code',
                    'কোড জেনারেশন শুরু করতে প্রস্তুত'
                )
            ];

            return Promise.resolve(items);
        }
    }

    /**
     * নতুন অর্কেস্ট্রেশন শুরু করে
     */
    async startOrchestration(requirement: string): Promise<OrchestrateResponse> {
        try {
            const request: OrchestrateRequest = { requirement };
            this.currentOrchestration = await this.api.orchestrate(request);
            this.refresh();
            return this.currentOrchestration;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);
            vscode.window.showErrorMessage(`অর্কেস্ট্রেশন ব্যর্থ: ${errorMessage}`);
            throw error;
        }
    }

    /**
     * বর্তমান অর্কেস্ট্রেশন ফেরত দেয়
     */
    getCurrentOrchestration(): OrchestrateResponse | undefined {
        return this.currentOrchestration;
    }

    /**
     * অর্কেস্ট্রেশন রিসেট করে
     */
    resetOrchestration(): void {
        this.currentOrchestration = undefined;
        this.refresh();
    }
}
