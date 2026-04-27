import * as vscode from 'vscode';
import { SupremeAIApi, OrchestrateRequest, OrchestrateResponse } from '../services/SupremeAIApi';

export class ChatViewProvider implements vscode.WebviewViewProvider {
    public static readonly viewType = 'supremeai-chat-view';
    private _view?: vscode.WebviewView;
    private api: SupremeAIApi;

    constructor(
        private readonly _extensionUri: vscode.Uri,
        private readonly apiEndpoint: string,
        private readonly apiKey?: string
    ) {
        this.api = new SupremeAIApi(apiEndpoint, apiKey);
    }

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
                        this._handleUserMessage(data.value);
                        break;
                    }
            }
        });
    }

    public sendMessage(message: string) {
        if (this._view) {
            this._view.show?.(true);
            this._view.webview.postMessage({ type: 'addQuestion', value: message });
            this._handleUserMessage(message);
        }
    }

    /**
     * ব্যবহারকারীর মেসেজ প্রসেস করে এবং এজেন্ট অর্কেস্ট্রেশন API কল করে
     */
    private async _handleUserMessage(message: string) {
        try {
            // প্রথমে ব্যবহারকারীর মেসেজ চ্যাটে দেখানো হয়েছে

            // লোডিং ইন্ডিকেটর দেখানো
            this._view?.webview.postMessage({ type: 'showLoading' });

            // এজেন্ট অর্কেস্ট্রেশন API কল করা
            const request: OrchestrateRequest = { requirement: message };
            const response: OrchestrateResponse = await this.api.orchestrate(request);

            // লোডিং ইন্ডিকেটর লুকানো
            this._view?.webview.postMessage({ type: 'hideLoading' });

            // মোড আপডেট করা
            if (response.mode) {
                this._view?.webview.postMessage({ type: 'updateMode', value: response.mode });
            }

            // এজেন্ট থেকে প্রাপ্ত প্রতিক্রিয়া দেখানো
            let responseText = this._formatOrchestrationResponse(response);
            this._view?.webview.postMessage({ type: 'addResponse', value: responseText });

        } catch (error) {
            // লোডিং ইন্ডিকেটর লুকানো
            this._view?.webview.postMessage({ type: 'hideLoading' });

            // ত্রুটি বার্তা দেখানো
            const errorMessage = error instanceof Error ? error.message : String(error);
            this._view?.webview.postMessage({ 
                type: 'addResponse', 
                value: `দুঃখিত, আপনার অনুরোধ প্রসেস করতে সমস্যা হয়েছে: ${errorMessage}` 
            });
        }
    }

    /**
     * এজেন্ট অর্কেস্ট্রেশন রেসপন্স ফরম্যাট করে
     */
    private _formatOrchestrationResponse(response: OrchestrateResponse): string {
        let formatted = "আমি আপনার প্রয়োজনীয়তা বিশ্লেষণ করেছি। এখানে আমার পর্যালোচনা:\n\n";

        // প্রশ্ন এবং উত্তর যোগ করা
        if (response.context && response.context['questions']) {
            const questions = response.context['questions'] as any[];
            if (questions && questions.length > 0) {
                formatted += "**প্রশ্ন এবং উত্তর:**\n";
                questions.forEach((q, index) => {
                    formatted += `${index + 1}. ${q.text}\n`;
                });
                formatted += "\n";
            }
        }

        // সিদ্ধান্ত যোগ করা
        if (response.context && response.context['decisions']) {
            const decisions = response.context['decisions'] as any[];
            if (decisions && decisions.length > 0) {
                formatted += "**সিদ্ধান্ত:**\n";
                decisions.forEach((d, index) => {
                    formatted += `${index + 1}. ${d.decisionKey}: ${d.aiConsensus}\n`;
                });
                formatted += "\n";
            }
        }

        // জেনারেশন কনটেক্সট যোগ করা
        if (response.generationContext && Object.keys(response.generationContext).length > 0) {
            formatted += "**জেনারেশন কনটেক্সট:**\n";
            Object.entries(response.generationContext).forEach(([key, value]) => {
                formatted += `- ${key}: ${value}\n`;
            });
            formatted += "\n";
        }

        formatted += "আপনি কি এই সিদ্ধান্তগুলির ভিত্তিতে কোড জেনারেট করতে চান?";

        return formatted;
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
					.loading-indicator {
						display: none;
						padding: 10px;
						text-align: center;
						background-color: var(--vscode-editor-background);
						border: 1px solid var(--vscode-panel-border);
						border-radius: 4px;
						margin-bottom: 10px;
					}
					.loading-spinner {
						display: inline-block;
						width: 20px;
						height: 20px;
						border: 2px solid var(--vscode-button-background);
						border-radius: 50%;
						border-top-color: transparent;
						animation: spin 1s linear infinite;
						margin-right: 10px;
						vertical-align: middle;
					}
					@keyframes spin {
						to { transform: rotate(360deg); }
					}
					.action-buttons {
						display: flex;
						gap: 5px;
						margin-top: 10px;
					}
					.action-button {
						flex: 1;
						background-color: var(--vscode-button-secondaryBackground);
						color: var(--vscode-button-secondaryForeground);
						border: none;
						padding: 5px 10px;
						cursor: pointer;
						border-radius: 3px;
						font-size: 0.9em;
					}
					.action-button:hover {
						background-color: var(--vscode-button-secondaryHoverBackground);
					}
				</style>
			</head>
			<body>
				<div class="chat-container">
					<div id="mode-indicator" style="padding: 5px 10px; background: var(--vscode-badge-background); color: var(--vscode-badge-foreground); font-size: 0.8em; border-radius: 4px; align-self: flex-start; margin-bottom: 10px; display: none;">
						Mode: <span id="current-mode">Code</span>
					</div>
					<div id="chat-history">
						<div class="message ai-message">সুপ্রিমএআইতে স্বাগতম! আমি কিভাবে সাহায্য করতে পারি?</div>
					</div>
					<div id="loading-indicator" class="loading-indicator">
						<div class="loading-spinner"></div>
						<span>প্রসেস করা হচ্ছে...</span>
					</div>
					<div class="input-container">
						<input type="text" id="chat-input" placeholder="একটি বার্তা লিখুন...">
						<button id="send-button">পাঠান</button>
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
								break;
							case 'updateMode':
								const modeIndicator = document.getElementById('mode-indicator');
								const modeSpan = document.getElementById('current-mode');
								modeIndicator.style.display = 'block';
								modeSpan.textContent = message.value.charAt(0).toUpperCase() + message.value.slice(1);
								break;
							case 'showLoading':
								document.getElementById('loading-indicator').style.display = 'block';
								break;
							case 'hideLoading':
								document.getElementById('loading-indicator').style.display = 'none';
								break;
						}
					});
				</script>
			</body>
			</html>`;
    }
}
