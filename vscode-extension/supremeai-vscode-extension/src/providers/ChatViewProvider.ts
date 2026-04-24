import * as vscode from 'vscode';

export class ChatViewProvider implements vscode.WebviewViewProvider {
    public static readonly viewType = 'supremeai-chat-view';
    private _view?: vscode.WebviewView;

    constructor(
        private readonly _extensionUri: vscode.Uri,
    ) { }

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        _context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken,
    ) {
        this._view = webviewView;

        webviewView.webview.options = {
            enableScripts: true,
            localResourceRoots: [
                this._extensionUri
            ]
        };

        webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

        webviewView.webview.onDidReceiveMessage(data => {
            switch (data.type) {
                case 'sendMessage':
                    {
                        // Handle message from webview
                        vscode.window.showInformationMessage(`SupremeAI received: ${data.value}`);
                        // Mock response
                        this._view?.webview.postMessage({ type: 'addResponse', value: `I'm processing your request: "${data.value}". As an AI, I can help you with coding, debugging, and project management.` });
                        break;
                    }
            }
        });
    }

    public sendMessage(message: string) {
        if (this._view) {
            this._view.show?.(true);
            this._view.webview.postMessage({ type: 'addQuestion', value: message });
        }
    }

    private _getHtmlForWebview(_webview: vscode.Webview) {
        return `<!DOCTYPE html>
			<html lang="en">
			<head>
				<meta charset="UTF-8">
				<meta name="viewport" content="width=device-width, initial-scale=1.0">
				<style>
					body {
						font-family: var(--vscode-font-family);
						padding: 10px;
						color: var(--vscode-foreground);
						background-color: var(--vscode-sideBar-background);
					}
					.chat-container {
						display: flex;
						flex-direction: column;
						height: 100vh;
					}
					#chat-history {
						flex: 1;
						overflow-y: auto;
						margin-bottom: 10px;
						display: flex;
						flex-direction: column;
						gap: 10px;
					}
					.message {
						padding: 8px 12px;
						border-radius: 6px;
						max-width: 90%;
						word-wrap: break-word;
					}
					.user-message {
						align-self: flex-end;
						background-color: var(--vscode-button-background);
						color: var(--vscode-button-foreground);
					}
					.ai-message {
						align-self: flex-start;
						background-color: var(--vscode-editor-background);
						border: 1px solid var(--vscode-panel-border);
					}
					.input-container {
						display: flex;
						gap: 5px;
						padding-bottom: 20px;
					}
					input {
						flex: 1;
						background-color: var(--vscode-input-background);
						color: var(--vscode-input-foreground);
						border: 1px solid var(--vscode-input-border);
						padding: 6px;
						border-radius: 3px;
					}
					button {
						background-color: var(--vscode-button-background);
						color: var(--vscode-button-foreground);
						border: none;
						padding: 6px 12px;
						cursor: pointer;
						border-radius: 3px;
					}
					button:hover {
						background-color: var(--vscode-button-hoverBackground);
					}
				</style>
			</head>
			<body>
				<div class="chat-container">
					<div id="chat-history">
						<div class="message ai-message">Hello! How can I help you today?</div>
					</div>
					<div class="input-container">
						<input type="text" id="chat-input" placeholder="Type a message...">
						<button id="send-button">Send</button>
					</div>
				</div>

				<script>
					const vscode = acquireVsCodeApi();
					const chatHistory = document.getElementById('chat-history');
					const chatInput = document.getElementById('chat-input');
					const sendButton = document.getElementById('send-button');

					function addMessage(text, type) {
						const div = document.createElement('div');
						div.className = 'message ' + type;
						div.textContent = text;
						chatHistory.appendChild(div);
						chatHistory.scrollTop = chatHistory.scrollHeight;
					}

					sendButton.addEventListener('click', () => {
						const text = chatInput.value;
						if (text) {
							addMessage(text, 'user-message');
							vscode.postMessage({ type: 'sendMessage', value: text });
							chatInput.value = '';
						}
					});

					chatInput.addEventListener('keypress', (e) => {
						if (e.key === 'Enter') {
							sendButton.click();
						}
					});

					window.addEventListener('message', event => {
						const message = event.data;
						switch (message.type) {
							case 'addResponse':
								addMessage(message.value, 'ai-message');
								break;
							case 'addQuestion':
								addMessage(message.value, 'user-message');
                                // Trigger AI response logic would go here
								break;
						}
					});
				</script>
			</body>
			</html>`;
    }
}
