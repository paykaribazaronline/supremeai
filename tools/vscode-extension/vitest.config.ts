import { fileURLToPath, URL } from 'url';

export default {
  test: {
    environment: 'node',
    globals: true,
    include: ['test/**/*.test.ts'],
  },
  alias: [
    { find: /^vscode$/, replacement: fileURLToPath(new URL('./test/mocks/vscode.ts', import.meta.url)) },
  ],
} as any;
