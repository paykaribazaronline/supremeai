import * as vscode from 'vscode';

export class SupremeWebviewProvider implements vscode.WebviewViewProvider {
    public static readonly viewType = 'supremeai.sidebarViews';
    constructor(private readonly _extensionUri: vscode.Uri) {}

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken,
    ) {
        webviewView.webview.options = {
            enableScripts: true, // স্ক্রিপ্ট এক্সিকিউশন এনাবল করা
            localResourceRoots: [this._extensionUri]
        };
        webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

        // 📡 IPC মেসেজ লিসেনার
        this._setupMessageListener(webviewView.webview);
    }

    private _getHtmlForWebview(webview: vscode.Webview): string {
        // এখানে আমাদের ডাইনামিক রেসিপি লিস্ট এবং অ্যাকশন বাটনের HTML/CSS বান্ডেল রেন্ডার হবে
        return `<html>
<body>
    <h3>SupremeAI Recipe Factory</h3>
    <div id="recipe-list"></div>
    <button id="testBtn">Execute Recipe</button>
    <script>
        const vscode = acquireVsCodeApi();
        document.getElementById('testBtn').addEventListener('click', () => {
            vscode.postMessage({ command: 'executeLocalRecipe', recipeName: 'Demo Recipe' });
        });
    </script>
</body>
</html>`;
    }

    private _setupMessageListener(webview: vscode.Webview) {
        webview.onDidReceiveMessage(async (message) => {
            switch (message.command) {
                case 'executeLocalRecipe':
                    // আমাদের সদ্য ফিক্স করা ক্লাউড রান ব্যাকএন্ড এপিআই-তে টাস্ক পুশ করবে
                    vscode.window.showInformationMessage(`🚀 Triggering Recipe: ${message.recipeName}`);
                    break;
                case 'showError':
                    vscode.window.showErrorMessage(`🔴 Webview Error: ${message.text}`);
                    break;
            }
        });
    }
}
