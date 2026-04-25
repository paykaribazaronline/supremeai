
import * as vscode from 'vscode';

export class ChatView {
    private static instance: ChatView;
    private currentPanel?: vscode.WebviewPanel;
    private disposables: vscode.Disposable[] = [];

    private constructor() {}

    public static getInstance(): ChatView {
        if (!ChatView.instance) {
            ChatView.instance = new ChatView();
        }
        return ChatView.instance;
    }

    public showChat(extensionUri: vscode.Uri): void {
        if (this.currentPanel) {
            this.currentPanel.reveal(vscode.ViewColumn.One);
            return;
        }

        this.currentPanel = vscode.window.createWebviewPanel(
            'supremeai.chat',
            'SupremeAI Chat',
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                retainContextWhenHidden: true,
                localResourceRoots: [extensionUri]
            }
        );

        this.currentPanel.webview.html = this.getWebviewContent();

        this.currentPanel.onDidDispose(() => {
            this.currentPanel = undefined;
            this.dispose();
        });

        this.currentPanel.webview.onDidReceiveMessage(
            message => {
                switch (message.type) {
                    case 'sendMessage':
                        vscode.commands.executeCommand('supremeai.chat', message.text);
                        break;
                    case 'changeLanguage':
                        vscode.workspace.getConfiguration('supremeai').update('language', message.language, vscode.ConfigurationTarget.Global);
                        break;
                }
            },
            null,
            this.disposables
        );
    }

    private getWebviewContent(): string {
        const currentLanguage = vscode.workspace.getConfiguration('supremeai').get('language', 'en');

        return `<!DOCTYPE html>
<html lang="${currentLanguage}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SupremeAI Chat</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: var(--vscode-font-family);
            height: 100vh;
            display: flex;
            flex-direction: column;
            color: var(--vscode-foreground);
            background-color: var(--vscode-editor-background);
        }

        .header {
            padding: 15px 20px;
            background-color: var(--vscode-sideBar-background);
            border-bottom: 1px solid var(--vscode-panel-border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .header h1 {
            font-size: 18px;
            font-weight: 600;
            color: var(--vscode-textLink-foreground);
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .header h1::before {
            content: '✨';
        }

        .language-selector {
            display: flex;
            gap: 8px;
        }

        .lang-btn {
            padding: 6px 12px;
            border-radius: 4px;
            border: 1px solid var(--vscode-panel-border);
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
            cursor: pointer;
            font-size: 12px;
            font-weight: bold;
            transition: all 0.2s;
        }

        .lang-btn:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }

        .lang-btn.active {
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
        }

        .chat-container {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .message {
            max-width: 80%;
            padding: 12px 16px;
            border-radius: 8px;
            line-height: 1.5;
            animation: fadeIn 0.3s ease-in;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .user-message {
            align-self: flex-end;
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
        }

        .ai-message {
            align-self: flex-start;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border: 1px solid var(--vscode-panel-border);
        }

        .message-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 8px;
            font-size: 12px;
            font-weight: bold;
            color: var(--vscode-textLink-foreground);
        }

        .message-content {
            white-space: pre-wrap;
            word-wrap: break-word;
        }

        .input-area {
            padding: 20px;
            background-color: var(--vscode-sideBar-background);
            border-top: 1px solid var(--vscode-panel-border);
        }

        .input-container {
            display: flex;
            gap: 10px;
            max-width: 100%;
        }

        .chat-input {
            flex: 1;
            padding: 12px;
            border: 1px solid var(--vscode-input-border);
            border-radius: 6px;
            background-color: var(--vscode-input-background);
            color: var(--vscode-input-foreground);
            font-size: 14px;
            outline: none;
            transition: border-color 0.2s;
        }

        .chat-input:focus {
            border-color: var(--vscode-textLink-foreground);
        }

        .send-btn {
            padding: 12px 24px;
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-weight: bold;
            transition: background-color 0.2s;
        }

        .send-btn:hover {
            background-color: var(--vscode-button-hoverBackground);
        }

        .loading-indicator {
            display: none;
            align-items: center;
            gap: 10px;
            padding: 12px;
            background-color: var(--vscode-editor-inactiveSelectionBackground);
            border-radius: 6px;
            margin: 10px 0;
        }

        .loading-spinner {
            width: 20px;
            height: 20px;
            border: 2px solid var(--vscode-button-background);
            border-radius: 50%;
            border-top-color: transparent;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        .quick-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
            margin-top: 10px;
        }

        .quick-action-btn {
            padding: 6px 12px;
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
            border: 1px solid var(--vscode-panel-border);
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            transition: all 0.2s;
        }

        .quick-action-btn:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>SupremeAI Chat</h1>
        <div class="language-selector">
            <button class="lang-btn ${currentLanguage === 'en' ? 'active' : ''}" data-lang="en">EN</button>
            <button class="lang-btn ${currentLanguage === 'bn' ? 'active' : ''}" data-lang="bn">বাংলা</button>
        </div>
    </div>

    <div class="chat-container" id="chat-container">
        <div class="message ai-message">
            <div class="message-header">
                <span>🤖 SupremeAI</span>
            </div>
            <div class="message-content">${currentLanguage === 'en' ? 'Hello! I'm SupremeAI, your AI-powered development assistant. How can I help you today?' : 'হ্যালো! আমি সুপ্রিমএআই, আপনার AI-চালিত ডেভেলপমেন্ট সহকারী। আজ আমি কিভাবে সাহায্য করতে পারি?'}</div>
        </div>
    </div>

    <div class="loading-indicator" id="loading-indicator">
        <div class="loading-spinner"></div>
        <span>${currentLanguage === 'en' ? 'Processing...' : 'প্রসেস করা হচ্ছে...'}</span>
    </div>

    <div class="input-area">
        <div class="input-container">
            <input type="text" class="chat-input" id="chat-input" placeholder="${currentLanguage === 'en' ? 'Type your message...' : 'আপনার বার্তা টাইপ করুন...'}">
            <button class="send-btn" id="send-btn">${currentLanguage === 'en' ? 'Send' : 'পাঠান'}</button>
        </div>
        <div class="quick-actions">
            <button class="quick-action-btn" data-action="generate-app">${currentLanguage === 'en' ? 'Generate App' : 'অ্যাপ তৈরি করুন'}</button>
            <button class="quick-action-btn" data-action="add-feature">${currentLanguage === 'en' ? 'Add Feature' : 'ফিচার যোগ করুন'}</button>
            <button class="quick-action-btn" data-action="review-code">${currentLanguage === 'en' ? 'Review Code' : 'কোড রিভিউ করুন'}</button>
        </div>
    </div>

    <script>
        const vscode = acquireVsCodeApi();
        const chatContainer = document.getElementById('chat-container');
        const chatInput = document.getElementById('chat-input');
        const sendBtn = document.getElementById('send-btn');
        const loadingIndicator = document.getElementById('loading-indicator');
        const langBtns = document.querySelectorAll('.lang-btn');
        const quickActionBtns = document.querySelectorAll('.quick-action-btn');

        function addMessage(text, type) {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message ' + type;
            messageDiv.innerHTML = \`
                <div class="message-header">
                    <span>\${type === 'user-message' ? '👤 You' : '🤖 SupremeAI'}</span>
                </div>
                <div class="message-content">\${text}</div>
            \`;
            chatContainer.appendChild(messageDiv);
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }

        function sendMessage() {
            const text = chatInput.value.trim();
            if (text) {
                addMessage(text, 'user-message');
                vscode.postMessage({ type: 'sendMessage', text: text });
                chatInput.value = '';
                loadingIndicator.style.display = 'flex';
            }
        }

        sendBtn.addEventListener('click', sendMessage);
        chatInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        langBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                langBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                vscode.postMessage({ type: 'changeLanguage', language: btn.dataset.lang });
            });
        });

        quickActionBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const action = btn.dataset.action;
                const messages = {
                    'generate-app': {
                        en: 'Generate a new Android app with authentication and database',
                        bn: 'একটি নতুন অ্যান্ড্রয়েড অ্যাপ তৈরি করুন যা প্রমাণীকরণ এবং ডাটাবেস সহ'
                    },
                    'add-feature': {
                        en: 'Add a new feature to the current project',
                        bn: 'বর্তমান প্রকল্পে একটি নতুন ফিচার যোগ করুন'
                    },
                    'review-code': {
                        en: 'Review the current file for bugs and improvements',
                        bn: 'বর্তমান ফাইলটি বাগ এবং উন্নতির জন্য পর্যালোচনা করুন'
                    }
                };
                const currentLang = document.querySelector('.lang-btn.active').dataset.lang;
                chatInput.value = messages[action][currentLang];
                sendMessage();
            });
        });

        window.addEventListener('message', event => {
            const message = event.data;
            switch (message.type) {
                case 'addResponse':
                    loadingIndicator.style.display = 'none';
                    addMessage(message.text, 'ai-message');
                    break;
            }
        });
    </script>
</body>
</html>`;
    }

    private dispose(): void {
        while (this.disposables.length) {
            const disposable = this.disposables.pop();
            if (disposable) {
                disposable.dispose();
            }
        }
    }
}
