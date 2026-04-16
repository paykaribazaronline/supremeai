export interface GenerateAppRequest {
    name: string;
    type: string;
    features: string[];
}

export interface GenerateAppResponse {
    success: boolean;
    projectId: string;
    apkUrl?: string;
    message?: string;
}

export class SupremeAIApi {
    private baseUrl: string;
    private token: string = 'dev-admin-token-local';
    private apiKey?: string;

    constructor(baseUrl: string, apiKey?: string) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    setApiKey(apiKey: string) {
        this.apiKey = apiKey;
    }

    async generateApp(request: GenerateAppRequest): Promise<GenerateAppResponse> {
        try {
            console.log(`Calling API: ${this.baseUrl}/api/project/generate`, request);

            const headers: any = {
                'Content-Type': 'application/json'
            };

            // Use API key if provided, otherwise use admin token
            if (this.apiKey) {
                headers['X-API-Key'] = this.apiKey;
            } else {
                headers['Authorization'] = `Bearer ${this.token}`;
            }

            const response = await fetch(`${this.baseUrl}/api/project/generate`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    projectName: request.name,
                    platform: request.type,
                    features: request.features
                })
            });

            const result = await response.json() as any;

            if (response.ok) {
                return {
                    success: true,
                    projectId: result.projectId || "proj_" + Math.random().toString(36).substr(2, 9),
                    apkUrl: result.apkUrl || "https://storage.googleapis.com/supremeai-builds/app-debug.apk",
                    message: result.message || "App generation started successfully"
                };
            } else {
                return {
                    success: false,
                    projectId: "",
                    message: result.message || `Backend Error: ${response.status}`
                };
            }
        } catch (error: any) {
            console.error('API Error:', error);
            return {
                success: false,
                projectId: "",
                message: `Failed to connect to SupremeAI API: ${error.message}`
            };
        }
    }

    async learn(data: any): Promise<void> {
        try {
            const headers: any = {
                'Content-Type': 'application/json'
            };

            // Use API key if provided, otherwise use admin token
            if (this.apiKey) {
                headers['X-API-Key'] = this.apiKey;
            } else {
                headers['Authorization'] = `Bearer ${this.token}`;
            }

            // Implementation for learning endpoint
            fetch(`${this.baseUrl}/api/learn`, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(data)
            }).catch(e => console.error('Learning sync failed', e));
        } catch (error) {
            console.error('Learning API Error:', error);
        }
    }
}
