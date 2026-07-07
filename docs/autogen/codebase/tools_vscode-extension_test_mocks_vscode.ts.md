# 📄 ফাইল: tools/vscode-extension/test/mocks/vscode.ts

**প্রকার:** .ts  
**সাইজ:** 571 বাইট  
**আপডেট:** 2026-07-07T18:37:32.443698

---

## কোড

```ts
export const window = {
  showInformationMessage: () => {},
  showErrorMessage: () => {},
  showWarningMessage: () => {},
};

export const commands = {
  executeCommand: async () => {},
};

export const authentication = {
  getSession: () => undefined,
};

export const env = {
  openExternal: async () => true,
};

export const Uri = {
  parse: (val: string) => ({ toString: () => val }),
};

export const workspace = {
  getConfiguration: () => ({
    get: () => '',
    update: async () => {},
  }),
};

export const extensions = {
  getExtension: () => undefined,
};

```