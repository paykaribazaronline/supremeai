# 📄 ফাইল: tools/vscode-extension/test/auth-service.test.js

**প্রকার:** .js  
**সাইজ:** 4,262 বাইট  
**আপডেট:** 2026-07-03T11:28:24.947034

---

## কোড

```js
"use strict";
vi.mock('axios');
vi.mock('vscode');
const axios = require('axios');
const vscode = require('vscode');
const { AuthService } = require('../src/services/AuthService');
beforeAll(() => {
    vscode.window = {
        showInformationMessage: vi.fn(),
        showErrorMessage: vi.fn(),
        showWarningMessage: vi.fn(),
    };
    vscode.commands = {
        executeCommand: vi.fn().mockResolvedValue(undefined),
    };
    vscode.authentication = {
        getSession: vi.fn(),
    };
});
describe('AuthService', () => {
    let authService;
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
        test('updates token and user on success', async () => {
            const mockUser = { username: 'dev', email: 'dev@test.com' };
            const mockToken = 'jwt-token-123';
            axios.post.mockResolvedValueOnce({
                data: { token: mockToken, user: mockUser },
            });
            vscode.authentication.getSession.mockResolvedValueOnce({
                accessToken: 'google-access-token',
            });
            const result = await authService.login();
            expect(result).toBe(true);
            expect(authService.isAuthenticated()).toBe(true);
            expect(authService.getToken()).toBe(mockToken);
            expect(authService.getUser()).toEqual(mockUser);
            expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/api/auth/firebase-login'), expect.objectContaining({
                idToken: 'google-access-token',
                isGoogleAccessToken: true,
            }));
            expect(vscode.window.showInformationMessage).toHaveBeenCalledWith(expect.stringContaining('dev'));
        });
        test('returns false and shows error on axios failure', async () => {
            axios.post.mockRejectedValueOnce(new Error('Network error'));
            vscode.authentication.getSession.mockResolvedValueOnce({
                accessToken: 'token',
            });
            const result = await authService.login();
            expect(result).toBe(false);
            expect(authService.isAuthenticated()).toBe(false);
            expect(vscode.window.showErrorMessage).toHaveBeenCalled();
        });
        test('returns false when Google session is cancelled', async () => {
            vscode.authentication.getSession.mockResolvedValueOnce(null);
            const result = await authService.login();
            expect(result).toBe(false);
            expect(authService.isAuthenticated()).toBe(false);
        });
        test('returns false when backend response lacks token', async () => {
            axios.post.mockResolvedValueOnce({ data: {} });
            vscode.authentication.getSession.mockResolvedValueOnce({
                accessToken: 'token',
            });
            const result = await authService.login();
            expect(result).toBe(false);
            expect(authService.isAuthenticated()).toBe(false);
        });
    });
    describe('logout', () => {
        test('clears token and user, resets VS Code context', async () => {
            authService['token'] = 'existing-token';
            authService['user'] = { username: 'dev' };
            await authService.logout();
            expect(authService.isAuthenticated()).toBe(false);
            expect(authService.getToken()).toBeNull();
            expect(authService.getUser()).toBeNull();
            expect(vscode.commands.executeCommand).toHaveBeenCalledWith('setContext', 'supremeai.authenticated', false);
            expect(vscode.window.showInformationMessage).toHaveBeenCalledWith(expect.stringContaining('logged out'));
        });
    });
});

```