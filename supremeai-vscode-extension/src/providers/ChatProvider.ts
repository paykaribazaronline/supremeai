import * as vscode from 'vscode';

export class ChatProvider implements vscode.WebviewViewProvider {
    public static readonly viewType = 'supremeai-chat';

    constructor(private readonly _extensionUri: vscode.Uri) {}

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        _context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken,
    ) {
        webviewView.webview.options = {
            enableScripts: true,
            localResourceRoots: [this._extensionUri]
        };

        webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

        webviewView.webview.onDidReceiveMessage(data => {
            switch (data.type) {
                case 'sendMessage':
                    vscode.window.showInformationMessage(`SupremeAI: Processing "${data.value}"`);
                    // Send to AI API here
                    break;
            }
        });
    }

    private _getHtmlForWebview(webview: vscode.Webview) {
        return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <style>
                    body { font-family: sans-serif; padding: 10px; }
                    #chat { height: 300px; overflow-y: auto; border: 1px solid #ccc; margin-bottom: 10px; padding: 5px; }
                    input { width: 100%; padding: 5px; }
                </style>
            </head>
            <body>
                <h3>SupremeAI Chat</h3>
                <div id="chat">Welcome! How can I help you today?</div>
                <input type="text" id="input" placeholder="Type a command or question..." />
                <script>
                    const vscode = acquireVsCodeApi();
                    const input = document.getElementById('input');
                    input.addEventListener('keypress', (e) => {
                        if (e.key === 'Enter') {
                            const val = input.value;
                            vscode.postMessage({ type: 'sendMessage', value: val });
                            input.value = '';
                        }
                    });
                </script>
            </body>
            </html>`;
    }
}
