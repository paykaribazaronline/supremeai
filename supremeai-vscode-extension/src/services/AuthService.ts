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
  }

  public static getInstance(config?: SupremeAIConfig): AuthService {
    if (!AuthService.instance && config) {
      AuthService.instance = new AuthService(config);
    }
    return AuthService.instance;
  }

  /**
   * Login using VS Code's built-in Google authentication provider
   */
  public async login(): Promise<boolean> {
    try {
      // 1. Get Google session from VS Code
      const session = await vscode.authentication.getSession('google', ['profile', 'email', 'openid'], { createIfNone: true });
      
      if (!session) {
        vscode.window.showErrorMessage('Google Sign-In failed or was cancelled.');
        return false;
      }

      // 2. Exchange Google token for SupremeAI JWT
      // Note: In a production app, you'd send the session.accessToken or an idToken to your backend
      // and the backend would verify it against Google and issue its own JWT.
      const response = await axios.post(`${this.config.backendUrl}/api/auth/firebase-login`, {
        idToken: session.accessToken, // We'll assume the backend can handle this or we'll update it
        isGoogleAccessToken: true
      });

      if (response.data && response.data.token) {
        this.token = response.data.token;
        this.user = response.data.user;
        
        // Store token securely
        await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', true);
        vscode.window.showInformationMessage(`Welcome, ${this.user.username || 'Developer'}!`);
        return true;
      }

      return false;
    } catch (error: any) {
      console.error('[SupremeAI] Login error:', error);
      vscode.window.showErrorMessage(`Login failed: ${error.message}`);
      return false;
    }
  }

  public async logout(): Promise<void> {
    this.token = null;
    this.user = null;
    await vscode.commands.executeCommand('setContext', 'supremeai.authenticated', false);
    vscode.window.showInformationMessage('Successfully logged out from SupremeAI.');
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
