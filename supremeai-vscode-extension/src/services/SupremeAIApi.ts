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

    constructor(baseUrl: string) {
        this.baseUrl = baseUrl;
    }

    async generateApp(request: GenerateAppRequest): Promise<GenerateAppResponse> {
        try {
            console.log(`Calling API: ${this.baseUrl}/api/project/generate`, request);

            const response = await fetch(`${this.baseUrl}/api/project/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
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
            // Implementation for learning endpoint
            fetch(`${this.baseUrl}/api/learn`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify(data)
            }).catch(e => console.error('Learning sync failed', e));
        } catch (error) {
            console.error('Learning API Error:', error);
        }
    }
}
