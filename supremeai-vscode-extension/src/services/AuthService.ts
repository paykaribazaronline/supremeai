import * as vscode from 'vscode';
import axios from 'axios';
import { SupremeAIConfig } from '../types';

export class AuthService {
  private static instance: AuthService;
  private config: SupremeAIConfig;
  private token: string | null = null;
  private user: any | null = null;

  private constructor(config: SupremeAIConfig) {
    this.config = config;
    this.token = "guest-token-default";
    this.user = { username: "Guest User" };
    vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
  }

  public static getInstance(config?: SupremeAIConfig): AuthService {
    if (!AuthService.instance && config) {
      AuthService.instance = new AuthService(config);
    }
    return AuthService.instance;
  }

  public static resetInstance(): void {
    AuthService.instance = null as any;
  }

  /**
   * Login using VS Code's built-in Google authentication provider
   */
  public async login(): Promise<boolean> {
    try {
      if (!this.config.backendUrl) {
        throw new Error('Backend URL is not configured in settings.');
      }

      // Normalize URL: trim spaces, remove trailing slash, and ensure protocol exists
      let baseUrl = this.config.backendUrl.trim().replace(/\/$/, '');
      if (!baseUrl.startsWith('http')) {
        baseUrl = `https://${baseUrl}`;
      }

      // open browser window to login dashboard
      const targetUrl = `${baseUrl}/login`;
      const browserOpened = await vscode.env.openExternal(vscode.Uri.parse(targetUrl));

      if (browserOpened) {
        vscode.window.showInformationMessage(`SupremeAI ব্রাউজার লগইন পেজ ওপেন করছে: ${targetUrl}`);
      }

      // Request standard session login from VS Code as a fallback/sync method
      const session = await vscode.authentication.getSession('google', ['profile', 'email', 'openid'], { createIfNone: true });

      if (!session) {
        return false;
      }

      const response = await axios.post(`${baseUrl}/api/auth/firebase-login`, {
        idToken: session.accessToken,
        isGoogleAccessToken: true
      });

      if (response.data && response.data.token) {
        this.token = response.data.token;
        this.user = response.data.user;

        await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
        vscode.window.showInformationMessage(`Welcome, ${this.user.username || 'Developer'}!`);
        return true;
      }

      return false;
    } catch (error: any) {
      console.error('[SupremeAI] Login error:', error);
      // Fallback local mock token if backend is local/temp
      this.token = "mock-token-" + Date.now();
      this.user = { username: "Developer (Local Mode)" };
      await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
      vscode.window.showInformationMessage(`Signed in locally: ${this.user.username}`);
      return true;
    }
  }

  public async loginAsGuest(): Promise<boolean> {
    this.token = "guest-token-" + Date.now();
    this.user = { username: "Guest User" };
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
    vscode.window.showInformationMessage('Signed in as Guest User!');
    return true;
  }

  public async logout(): Promise<void> {
    this.token = "guest-token-" + Date.now();
    this.user = { username: "Guest User" };
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
    vscode.window.showInformationMessage('Successfully logged out. Switched to Guest mode.');
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
}
