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

        webviewView.webview.onDidReceiveMessage(async (data: any) => {
            switch (data.type) {
                case 'sendMessage':
                    webviewView.webview.postMessage({ type: 'addMessage', role: 'user', text: data.value });

                    try {
                        // Force real backend connection
                        const response = await fetch('https://supremeai-565236080752.us-central1.run.app/api/chat/send', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                'Authorization': 'Bearer dev-admin-token-local' // This token is allowed in your dev environment
                            },
                            body: JSON.stringify({ message: data.value, provider: 'meta-llama' })
                        });

                        const result = await response.json() as any;

                        if (response.ok) {
                            webviewView.webview.postMessage({
                                type: 'addMessage',
                                role: 'ai',
                                text: result.reply || result.message || "SupremeAI is thinking..."
                            });
                        } else {
                            throw new Error(result.message || "Backend Error");
                        }

                    } catch (error: any) {
                        // Fallback ONLY on network error
                        webviewView.webview.postMessage({
                            type: 'addMessage',
                            role: 'ai',
                            text: `[Error] SupremeAI: Could not connect to backend. Please ensure your token is valid. Details: ${error.message}`
                        });
                    }
                    break;
            }
        });
    }

    private _getHtmlForWebview(_webview: vscode.Webview) {
        return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <style>
                    body { font-family: sans-serif; padding: 10px; background-color: var(--vscode-sideBar-background); color: var(--vscode-foreground); }
                    #chat { height: 80vh; overflow-y: auto; margin-bottom: 10px; border: 1px solid var(--vscode-panel-border); padding: 5px; }
                    .message { margin-bottom: 10px; padding: 8px; border-radius: 5px; }
                    .user { background: #007acc; color: white; text-align: right; margin-left: 20%; }
                    .ai { background: #333; color: #ddd; margin-right: 20%; }
                    input { width: 100%; padding: 8px; box-sizing: border-box; background: var(--vscode-input-background); color: var(--vscode-input-foreground); border: 1px solid var(--vscode-input-border); }
                </style>
            </head>
            <body>
                <div id="chat"></div>
                <input type="text" id="input" placeholder="Type a message..." />
                <script>
                    const vscode = acquireVsCodeApi();
                    const chat = document.getElementById('chat');
                    const input = document.getElementById('input');

                    window.addEventListener('message', event => {
                        const message = event.data;
                        if (message.type === 'addMessage') {
                            const div = document.createElement('div');
                            div.className = 'message ' + message.role;
                            div.innerHTML = '<b>' + (message.role === 'user' ? 'You' : 'AI') + ':</b> ' + message.text;
                            chat.appendChild(div);
                            chat.scrollTop = chat.scrollHeight;
                        }
                    });

                    input.addEventListener('keypress', (e) => {
                        if (e.key === 'Enter') {
                            vscode.postMessage({ type: 'sendMessage', value: input.value });
                            input.value = '';
                        }
                    });
                </script>
            </body>
            </html>`;
    }
}
