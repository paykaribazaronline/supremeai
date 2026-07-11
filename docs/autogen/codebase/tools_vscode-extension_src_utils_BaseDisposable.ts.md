# 📄 ফাইল: tools/vscode-extension/src/utils/BaseDisposable.ts

**প্রকার:** .ts  
**সাইজ:** 514 বাইট  
**আপডেট:** 2026-07-11T13:36:50.257857

---

## কোড

```ts
import * as vscode from 'vscode';

export abstract class BaseDisposable implements vscode.Disposable {
    protected disposables: vscode.Disposable[] = [];

    protected register<T extends vscode.Disposable>(disposable: T): T {
        this.disposables.push(disposable);
        return disposable;
    }

    public dispose(): void {
        while (this.disposables.length) {
            const item = this.disposables.pop();
            if (item) {
                item.dispose();
            }
        }
    }
}

```