# 📄 ফাইল: tools/vscode-extension/test/setup.ts

**প্রকার:** .ts  
**সাইজ:** 755 বাইট  
**আপডেট:** 2026-07-04T03:48:57.329020

---

## কোড

```ts
import { vi } from 'vitest';

vi.mock('vscode', () => ({
  window: {
    showInformationMessage: vi.fn(),
    showErrorMessage: vi.fn(),
    showWarningMessage: vi.fn(),
  },
  commands: {
    executeCommand: vi.fn().mockResolvedValue(undefined),
  },
  authentication: {
    getSession: vi.fn(),
  },
  env: {
    openExternal: vi.fn().mockResolvedValue(true),
  },
  Uri: {
    parse: vi.fn().mockImplementation((val: string) => ({ toString: () => val })),
  },
  workspace: {
    getConfiguration: vi.fn().mockReturnValue({
      update: vi.fn().mockResolvedValue(undefined),
      get: vi.fn().mockReturnValue(''),
    }),
    isTrusted: true,
  },
  extensions: {
    getExtension: vi.fn().mockReturnValue({
      extensionKind: 1,
    }),
  },
}));

```