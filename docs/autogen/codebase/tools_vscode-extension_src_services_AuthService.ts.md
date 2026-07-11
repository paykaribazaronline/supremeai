# 📄 ফাইল: tools/vscode-extension/src/services/AuthService.ts

**প্রকার:** .ts  
**সাইজ:** 7,125 বাইট  
**আপডেট:** 2026-07-11T19:00:24.819558

---

## কোড

```ts
import * as vscode from 'vscode';

import { SupremeAIConfig } from '../types';

export class AuthService {
  private static instance: AuthService;
  private config: SupremeAIConfig;
  private token: string | null = null;
  private user: any | null = null;
  private authState: string | null = null;

  private readonly _onAuthStateChanged = new vscode.EventEmitter<boolean>();
  public readonly onAuthStateChanged = this._onAuthStateChanged.event;

  private secrets: vscode.SecretStorage | null = null;

  private constructor(config: SupremeAIConfig, secrets?: vscode.SecretStorage) {
    this.config = config;
    this.token = null;
    this.user = null;
    if (secrets) {
      this.secrets = secrets;
    }
    vscode.commands.executeCommand('setContext', 'supremeai.authenticated', false);
  }

  public static getInstance(config?: SupremeAIConfig, secrets?: vscode.SecretStorage): AuthService {
    if (!AuthService.instance && config) {
      AuthService.instance = new AuthService(config, secrets);
    }
    return AuthService.instance;
  }

  public static resetInstance(): void {
    AuthService.instance = null as any;
  }

  public async initialize(): Promise<void> {
    if (this.secrets) {
      const storedToken = await this.secrets.get('supremeai.aiApiKey');
      if (storedToken) {
        this.token = storedToken;
        await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
        this._onAuthStateChanged.fire(true);
      }
    }
  }

  private resolveBaseUrl(): string {
    let baseUrl = (this.config.backendUrl || '').trim().replace(/\/$/, '');
    if (!baseUrl.startsWith('http')) {
      baseUrl = `https://${baseUrl}`;
    }
    return baseUrl;
  }

  public async login(): Promise<boolean> {
    try {
      if (!this.config.backendUrl) {
        throw new Error('Backend URL is not configured in settings.');
      }

      const baseUrl = this.resolveBaseUrl();
      this.authState = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
      const loginUrl = `${baseUrl}/auth/login?state=${this.authState}`;
      console.log('[SupremeAI] Opening browser for login:', loginUrl);
      await vscode.env.openExternal(vscode.Uri.parse(loginUrl));
      vscode.window.showInformationMessage('Login page opened in your browser. After signing in, the extension will detect the callback and complete authentication.');
      return false;
    } catch (error: any) {
      console.error('[SupremeAI] Login error:', error);
      vscode.window.showErrorMessage(`Login failed: ${error.message}`);
      return false;
    }
  }

  public async register(username: string, password: string): Promise<boolean> {
    try {
      const baseUrl = this.resolveBaseUrl();
      const response = await fetch(`${baseUrl}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
      }
      return true;
    } catch (error: any) {
      console.error('[SupremeAI] Register error:', error);
      vscode.window.showErrorMessage(`Registration failed: ${error.message}`);
      return false;
    }
  }

  public async completeLogin(token: string, user: Record<string, any>): Promise<void> {
    const store = vscode.workspace.isTrusted;
    if (this.secrets) {
      await this.secrets.store('supremeai.aiApiKey', token);
    } else {
      await vscode.workspace.getConfiguration('supremeai').update('aiApiKey', token, true);
    }
    this.token = token;
    this.user = user;
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
    this._onAuthStateChanged.fire(true);
  }

  public async loginAsGuest(): Promise<boolean> {
    this.token = null;
    this.user = null;
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', false);
    this._onAuthStateChanged.fire(false);
    return false;
  }

  public async logout(): Promise<void> {
    if (this.secrets) {
      await this.secrets.delete('supremeai.aiApiKey');
    }
    this.token = null;
    this.user = null;
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', false);
    this._onAuthStateChanged.fire(false);
    vscode.window.showInformationMessage('Logged out successfully.');
  }

  public getToken(): string | null {
    return this.token;
  }

  public getUser(): any | null {
    return this.user;
  }

  public isAuthenticated(): boolean {
    return !!this.token;
  }

  public getAuthState(): string | null {
    return this.authState;
  }

  public clearAuthState(): void {
    this.authState = null;
  }

  public async verifyToken(token: string): Promise<boolean> {
    try {
      const baseUrl = this.resolveBaseUrl();
      const response = await fetch(`${baseUrl}/auth/verify`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      return response.ok;
    } catch (error) {
      console.error('[SupremeAI] Token verification failed:', error);
      return false;
    }
  }

  public async fetchUserProfile(token: string): Promise<any | null> {
    try {
      const baseUrl = this.resolveBaseUrl();
      const response = await fetch(`${baseUrl}/auth/me`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (!response.ok) return null;
      return await response.json();
    } catch (error) {
      console.error('[SupremeAI] Profile fetch failed:', error);
      return null;
    }
  }

  public async rotateApiKey(): Promise<string | null> {
    try {
      const baseUrl = this.resolveBaseUrl();
      const response = await fetch(`${baseUrl}/admin/keys/rotate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(this.isAuthenticated() && this.token ? { Authorization: `Bearer ${this.token}` } : {}),
        },
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = (await response.json()) as any;
      const newKey = data.api_key ?? data.access_token;
      if (typeof newKey === 'string') {
        this.token = newKey;
        await vscode.workspace.getConfiguration('supremeai').update('aiApiKey', newKey, true);
      }
      return this.token;
    } catch (error: any) {
      console.error('[SupremeAI] API key rotation failed:', error);
      vscode.window.showErrorMessage(`API key rotation failed: ${error.message}`);
      return null;
    }
  }

  public setToken(token: string): void {
    this.token = token;
    vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
    this._onAuthStateChanged.fire(true);
  }

  public setUser(user: any): void {
    this.user = user;
    const isAdmin = user && (user.role === 'admin' || user.is_superuser === true);
    vscode.commands.executeCommand('setContext', 'supremeai.isAdmin', !!isAdmin);
  }
}

```