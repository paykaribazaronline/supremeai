// types.ts - API Keys & Model Discovery Types

export interface SavedAPIKey {
    id: string;
    provider: string;
    label: string;
    apiKey: string;
    baseUrl: string;
    models: string[];
    status: 'active' | 'inactive' | 'error';
    addedAt: string;
    lastTested: string | null;
}

export interface HuggingFaceModel {
    id: string;
    modelId: string;
    author: string;
    sha: string;
    lastModified: string;
    private: boolean;
    tags: string[];
    pipeline_tag: string;
    downloads: number;
    likes: number;
    library_name: string;
}

export interface OpenRouterModel {
    id: string;
    name: string;
    description: string;
    pricing: { prompt: string; completion: string };
    context_length: number;
    architecture: { tokenizer: string; instruct_type: string };
    top_provider: { max_completion_tokens: number };
}

export interface GoogleAIModel {
    name: string;
    displayName: string;
    description: string;
    supportedGenerationMethods: string[];
    inputTokenLimit: number;
    outputTokenLimit: number;
    temperature?: number;
    topP?: number;
}

export interface PopularModel {
    id: string;
    name: string;
    provider: string;
    providerTitle: string;
    baseUrl: string;
    description: string;
    category: string;
}

export interface ModelSearchResult {
    id: string;
    name: string;
    provider: string;
    providerTitle: string;
    baseUrl: string;
    description: string;
    category: string;
    downloads?: number;
    likes?: number;
    pipelineTag?: string;
}

export interface APIHealthReport {
    id: string;
    totalKeysTested: number;
    activeKeys: number;
    deadKeys: number;
    rotationDueKeys: number;
    deadKeyDetails: any[];
    createdAt: string;
}
