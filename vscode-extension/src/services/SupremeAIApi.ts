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

export interface OrchestrateRequest {
    requirement: string;
}

export interface OrchestrateResponse {
    status: string;
    requirement: string;
    mode?: string;
    context: Record<string, unknown>;
    generationContext: Record<string, unknown>;
    completedAt: string;
}

export interface GenerateWithContextRequest {
    context: Record<string, string>;
}

export interface GenerateWithContextResponse {
    appName: string;
    files: Record<string, string>;
    fileCount: number;
    status: string;
    decisions: Record<string, string>;
    message: string;
}

export interface PublishingPlanRequest {
    platform: string;
    config?: Record<string, string>;
}

export interface PublishingPlanResponse {
    status: string;
    platform: string;
    publishingPlan: Record<string, string>;
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

    private getHeaders(): Record<string, string> {
        const headers: Record<string, string> = {
            'Content-Type': 'application/json'
        };

        // Use API key if provided, otherwise use admin token
        if (this.apiKey) {
            headers['Authorization'] = `Bearer ${this.apiKey}`;
        } else {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        return headers;
    }

    async generateApp(request: GenerateAppRequest): Promise<GenerateAppResponse> {
        try {
            console.log(`Calling API: ${this.baseUrl}/api/project/generate`, request);

            const response = await fetch(`${this.baseUrl}/api/project/generate`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify({
                    projectName: request.name,
                    platform: request.type,
                    features: request.features
                })
            });

            const result = await response.json() as Record<string, unknown>;

            if (response.ok) {
                return {
                    success: true,
                    projectId: (result.projectId as string) || "proj_" + Math.random().toString(36).substr(2, 9),
                    apkUrl: (result.apkUrl as string) || "https://storage.googleapis.com/supremeai-builds/app-debug.apk",
                    message: (result.message as string) || "App generation started successfully"
                };
            } else {
                return {
                    success: false,
                    projectId: "",
                    message: (result.message as string) || `Backend Error: ${response.status}`
                };
            }
        } catch (error: unknown) {
            console.error('API Error:', error);
            const errorMessage = error instanceof Error ? error.message : String(error);
            return {
                success: false,
                projectId: "",
                message: `Failed to connect to SupremeAI API: ${errorMessage}`
            };
        }
    }

    /**
     * এজেন্ট অর্কেস্ট্রেশন API কল করে প্রয়োজনীয়তা বিশ্লেষণ করে
     */
    async orchestrate(request: OrchestrateRequest): Promise<OrchestrateResponse> {
        try {
            console.log(`Calling orchestration API: ${this.baseUrl}/api/orchestrate/requirement`, request);

            const response = await fetch(`${this.baseUrl}/api/orchestrate/requirement`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(request)
            });

            const result = await response.json() as Record<string, unknown>;

            if (response.ok) {
                return {
                    status: (result.status as string) || "UNKNOWN",
                    requirement: (result.requirement as string) || request.requirement,
                    context: (result.context as Record<string, unknown>) || {},
                    generationContext: (result.generationContext as Record<string, unknown>) || {},
                    completedAt: (result.completedAt as string) || new Date().toISOString()
                };
            } else {
                throw new Error(`Orchestration failed: ${result.message || response.status}`);
            }
        } catch (error: unknown) {
            console.error('Orchestration API Error:', error);
            const errorMessage = error instanceof Error ? error.message : String(error);
            throw new Error(`Failed to orchestrate: ${errorMessage}`);
        }
    }

    /**
     * এজেন্ট অর্কেস্ট্রেশন এবং কোড জেনারেশন API কল করে
     */
    async orchestrateAndGenerate(request: OrchestrateRequest): Promise<OrchestrateResponse & { generatedApp: GenerateWithContextResponse }> {
        try {
            console.log(`Calling orchestration+generation API: ${this.baseUrl}/api/orchestrate/generate`, request);

            const response = await fetch(`${this.baseUrl}/api/orchestrate/generate`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(request)
            });

            const result = await response.json() as Record<string, unknown>;

            if (response.ok) {
                return {
                    status: (result.status as string) || "UNKNOWN",
                    requirement: (result.requirement as string) || request.requirement,
                    context: (result.context as Record<string, unknown>) || {},
                    generationContext: (result.generationContext as Record<string, unknown>) || {},
                    completedAt: (result.completedAt as string) || new Date().toISOString(),
                    generatedApp: (result.generatedApp as GenerateWithContextResponse) || {
                        appName: "",
                        files: {},
                        fileCount: 0,
                        status: "FAILED",
                        decisions: {},
                        message: "Failed to generate app"
                    }
                };
            } else {
                throw new Error(`Orchestration+Generation failed: ${result.message || response.status}`);
            }
        } catch (error: unknown) {
            console.error('Orchestration+Generation API Error:', error);
            const errorMessage = error instanceof Error ? error.message : String(error);
            throw new Error(`Failed to orchestrate and generate: ${errorMessage}`);
        }
    }

    /**
     * কনটেক্সট থেকে কোড জেনারেট করে
     */
    async generateWithContext(request: GenerateWithContextRequest): Promise<GenerateWithContextResponse> {
        try {
            console.log(`Calling generate-with-context API: ${this.baseUrl}/api/orchestrate/generate-with-context`, request);

            const response = await fetch(`${this.baseUrl}/api/orchestrate/generate-with-context`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify({ context: request.context })
            });

            const result = await response.json() as Record<string, unknown>;

            if (response.ok) {
                return {
                    appName: (result.appName as string) || "GeneratedApp",
                    files: (result.files as Record<string, string>) || {},
                    fileCount: (result.fileCount as number) || 0,
                    status: (result.status as string) || "UNKNOWN",
                    decisions: (result.decisions as Record<string, string>) || {},
                    message: (result.message as string) || "App generated successfully"
                };
            } else {
                throw new Error(`Generate-with-context failed: ${result.message || response.status}`);
            }
        } catch (error: unknown) {
            console.error('Generate-with-context API Error:', error);
            const errorMessage = error instanceof Error ? error.message : String(error);
            throw new Error(`Failed to generate with context: ${errorMessage}`);
        }
    }

    /**
     * পাবলিশিং প্ল্যান তৈরি করে
     */
    async createPublishingPlan(request: PublishingPlanRequest): Promise<PublishingPlanResponse> {
        try {
            console.log(`Calling publishing-plan API: ${this.baseUrl}/api/orchestrate/publishing-plan`, request);

            const response = await fetch(`${this.baseUrl}/api/orchestrate/publishing-plan`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(request)
            });

            const result = await response.json() as Record<string, unknown>;

            if (response.ok) {
                return {
                    status: (result.status as string) || "UNKNOWN",
                    platform: (result.platform as string) || request.platform,
                    publishingPlan: (result.publishingPlan as Record<string, string>) || {}
                };
            } else {
                throw new Error(`Publishing plan creation failed: ${result.message || response.status}`);
            }
        } catch (error: unknown) {
            console.error('Publishing plan API Error:', error);
            const errorMessage = error instanceof Error ? error.message : String(error);
            throw new Error(`Failed to create publishing plan: ${errorMessage}`);
        }
    }

    async learn(data: Record<string, unknown>): Promise<void> {
        try {
            const response = await fetch(`${this.baseUrl}/api/learn`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(data)
            }).catch(e => console.error('Learning sync failed', e));

            if (response && !response.ok) {
                console.error('Learning API Error:', response.status);
            }
        } catch (error) {
            console.error('Learning API Error:', error);
        }
    }
}
