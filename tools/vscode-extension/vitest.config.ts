import { defineConfig } from 'vitest/config';
import { fileURLToPath, URL } from 'url';

export default defineConfig({
  test: {
    environment: 'node',
    globals: true,
    include: ['test/**/*.test.ts'],
  },
  resolve: {
    alias: {
      vscode: fileURLToPath(new URL('./test/mocks/vscode.ts', import.meta.url)),
    },
  },
});
