# 📄 ফাইল: tools/vscode-extension/src/providers/StreamingChatProvider.ts

**প্রকার:** .ts  
**সাইজ:** 1,203 বাইট  
**আপডেট:** 2026-07-07T17:20:39.936719

---

## কোড

```ts
import * as vscode from 'vscode';
import { getSupremeAIService } from '../services/SupremeAIService';
import { ChatRequest } from '../types';

export class StreamingChatProvider {
  private static instance: StreamingChatProvider | undefined;
  private disposables: vscode.Disposable[] = [];

  static getInstance(): StreamingChatProvider {
    if (!StreamingChatProvider.instance) {
      StreamingChatProvider.instance = new StreamingChatProvider();
    }
    return StreamingChatProvider.instance;
  }

  register(context: vscode.ExtensionContext): void {
    context.subscriptions.push(new vscode.Disposable(() => this.dispose()));
    vscode.commands.executeCommand('setContext', 'supremeai.streamingEnabled', true);
  }

  async sendMessageStream(message: string, onToken?: (token: string) => void): Promise<string> {
    const service = getSupremeAIService();
    const request: ChatRequest = {
      message,
      sessionId: service.getSessionId(),
      context: { source: 'vscode', timestamp: new Date().toISOString() },
    };
    return service.streamChatCompletion(request, onToken);
  }

  dispose(): void {
    this.disposables.forEach(d => d.dispose());
    this.disposables = [];
  }
}

```