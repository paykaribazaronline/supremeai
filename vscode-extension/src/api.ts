import axios, { AxiosInstance } from 'axios';
import * as vscode from 'vscode';

export interface AppGenerationRequest {
    name: string;
    features: string[];
}

export interface AppGenerationResponse {
    downloadUrl: string;
    success: boolean;
}

export interface CodeReviewIssue {
    line: number;
    description: string;
    severity: 'high' | 'medium' | 'low';
}

export interface CodeReviewResponse {
    issues: CodeReviewIssue[];
    success: boolean;
}

export class SupremeAIApi {
    private client: AxiosInstance;

    constructor(baseUrl: string) {
        this.client = axios.create({
            baseURL: baseUrl,
            timeout: 30000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
    }

    async generateApp(request: AppGenerationRequest): Promise<AppGenerationResponse> {
        try {
            const response = await this.client.post('/api/generate-app', request);
            return response.data;
        } catch (error) {
            throw new Error(`App generation failed: ${error}`);
        }
    }

    async reviewCode(code: string): Promise<CodeReviewResponse> {
        try {
            const response = await this.client.post('/api/code-review', { code });
            return response.data;
        } catch (error) {
            throw new Error(`Code review failed: ${error}`);
        }
    }
}
