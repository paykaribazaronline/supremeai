# 📄 ফাইল: tools/vscode-extension/test/auth-service.test.ts

**প্রকার:** .ts  
**সাইজ:** 2,239 বাইট  
**আপডেট:** 2026-07-07T16:46:48.630409

---

## কোড

```ts
import * as vscode from 'vscode';
import { AuthService } from '../src/services/AuthService';

describe('AuthService', () => {
  let authService: any;

  beforeEach(() => {
    AuthService.resetInstance();
    authService = AuthService.getInstance({
      backendUrl: 'http://127.0.0.1:8080',
      enableRealTimeLearning: true,
      autoReportErrors: true,
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    authService.logout();
  });

  describe('initialization', () => {
    test('initial state is unauthenticated', () => {
      expect(authService.isAuthenticated()).toBe(false);
      expect(authService.getToken()).toBeNull();
      expect(authService.getUser()).toBeNull();
    });
  });

  describe('login', () => {
    test('opens browser URL and returns false', async () => {
      const result = await authService.login();
      expect(result).toBe(false);
      expect(vscode.env.openExternal).toHaveBeenCalled();
    });
  });

  describe('completeLogin', () => {
    test('completes login, sets token/user and sets context to authenticated', async () => {
      const mockToken = 'mock-jwt-token';
      const mockUser = { username: 'dev-user' };
      await authService.completeLogin(mockToken, mockUser);

      expect(authService.isAuthenticated()).toBe(true);
      expect(authService.getToken()).toBe(mockToken);
      expect(authService.getUser()).toEqual(mockUser);
      expect(vscode.commands.executeCommand).toHaveBeenCalledWith(
        'setContext',
        'supremeai.authenticated',
        true
      );
    });
  });

  describe('logout', () => {
    test('clears token and user, resets VS Code context', async () => {
      authService.setToken('existing-token');
      authService.setUser({ username: 'dev' });

      await authService.logout();

      expect(authService.isAuthenticated()).toBe(false);
      expect(authService.getToken()).toBeNull();
      expect(authService.getUser()).toBeNull();
      expect(vscode.commands.executeCommand).toHaveBeenCalledWith(
        'setContext',
        'supremeai.authenticated',
        false
      );
      expect(vscode.window.showInformationMessage).toHaveBeenCalledWith(
        expect.stringContaining('Logged out')
      );
    });
  });
});

```