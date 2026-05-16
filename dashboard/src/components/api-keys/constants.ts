// constants.ts - API Key Providers & Popular Models

import { PopularModel } from './types';

export const POPULAR_MODELS: PopularModel[] = [
    // Google Gemini
    { id: 'gemini-2.5-pro-preview-03-25', name: 'Gemini 2.5 Pro', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Latest Gemini Pro — best reasoning & coding', category: 'Gemini' },
    { id: 'gemini-2.5-flash-preview-04-17', name: 'Gemini 2.5 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Fast & efficient Gemini Flash model', category: 'Gemini' },
    { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Stable Gemini 2.0 Flash — great for most tasks', category: 'Gemini' },
    { id: 'gemini-2.0-flash-lite', name: 'Gemini 2.0 Flash Lite', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Cost-optimized Gemini Flash', category: 'Gemini' },
    { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: '1M+ token context window', category: 'Gemini' },
    { id: 'gemini-1.5-flash', name: 'Gemini 1.5 Flash', provider: 'google', providerTitle: 'Google AI', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', description: 'Fast Gemini 1.5 with long context', category: 'Gemini' },
    // OpenAI
    { id: 'gpt-4.1', name: 'GPT-4.1', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Latest GPT-4.1 — advanced coding & reasoning', category: 'OpenAI' },
    { id: 'gpt-4.1-mini', name: 'GPT-4.1 Mini', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Fast & affordable GPT-4.1', category: 'OpenAI' },
    { id: 'gpt-4.1-nano', name: 'GPT-4.1 Nano', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Ultra-fast & cheapest GPT-4.1', category: 'OpenAI' },
    { id: 'gpt-4o', name: 'GPT-4o', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Multimodal GPT-4 Omni', category: 'OpenAI' },
    { id: 'o4-mini', name: 'o4-mini', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Reasoning model — efficient', category: 'OpenAI' },
    { id: 'o3', name: 'o3', provider: 'openai', providerTitle: 'OpenAI', baseUrl: 'https://api.openai.com/v1', description: 'Advanced reasoning model', category: 'OpenAI' },
    // Anthropic
    { id: 'claude-sonnet-4-20250514', name: 'Claude Sonnet 4', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Latest Claude — best coding & analysis', category: 'Anthropic' },
    { id: 'claude-3-7-sonnet-20250219', name: 'Claude 3.7 Sonnet', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Extended thinking, strong coding', category: 'Anthropic' },
    { id: 'claude-3-5-haiku-20241022', name: 'Claude 3.5 Haiku', provider: 'anthropic', providerTitle: 'Anthropic', baseUrl: 'https://api.anthropic.com/v1', description: 'Fast & affordable Claude', category: 'Anthropic' },
    // Meta Llama
    { id: 'llama-4-maverick-17b-128e-instruct', name: 'Llama 4 Maverick', provider: 'meta', providerTitle: 'Meta (via OpenRouter)', baseUrl: 'https://openrouter.ai/api/v1', description: 'Llama 4 MoE model', category: 'Meta' },
    { id: 'llama-3.3-70b-instruct', name: 'Llama 3.3 70B', provider: 'meta', providerTitle: 'Meta (via OpenRouter)', baseUrl: 'https://openrouter.ai/api/v1', description: 'Latest Llama 3.3 70B', category: 'Meta' },
    // Mistral
    { id: 'mistral-large-latest', name: 'Mistral Large', provider: 'mistral', providerTitle: 'Mistral AI', baseUrl: 'https://api.mistral.ai/v1', description: 'Top-tier Mistral model', category: 'Mistral' },
    { id: 'mistral-small-latest', name: 'Mistral Small', provider: 'mistral', providerTitle: 'Mistral AI', baseUrl: 'https://api.mistral.ai/v1', description: 'Fast Mistral model', category: 'Mistral' },
    // Groq (fast inference)
    { id: 'llama-3.3-70b-versatile', name: 'Llama 3.3 70B (Groq)', provider: 'groq', providerTitle: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', description: 'Ultra-fast Llama inference on Groq', category: 'Groq' },
    { id: 'llama-3.1-8b-instant', name: 'Llama 3.1 8B (Groq)', provider: 'groq', providerTitle: 'Groq', baseUrl: 'https://api.groq.com/openai/v1', description: 'Fastest model on Groq', category: 'Groq' },
    // DeepSeek
    { id: 'deepseek-chat', name: 'DeepSeek V3', provider: 'deepseek', providerTitle: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', description: 'DeepSeek V3 — strong coding model', category: 'DeepSeek' },
    { id: 'deepseek-reasoner', name: 'DeepSeek R1', provider: 'deepseek', providerTitle: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', description: 'DeepSeek reasoning model', category: 'DeepSeek' },
    // xAI
    { id: 'grok-3', name: 'Grok 3', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Latest Grok model by xAI', category: 'xAI' },
    { id: 'grok-3-mini', name: 'Grok 3 Mini', provider: 'xai', providerTitle: 'xAI', baseUrl: 'https://api.x.ai/v1', description: 'Fast Grok 3 Mini', category: 'xAI' },
    // StepFun
    { id: 'step-3.5-flash', name: 'Step 3.5 Flash', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Free & fast - excellent reasoning & coding', category: 'StepFun' },
    { id: 'step-3.5-pro', name: 'Step 3.5 Pro', provider: 'stepfun', providerTitle: 'StepFun (阶跃星辰)', baseUrl: 'https://api.stepfun.com/v1', description: 'Advanced reasoning & coding capabilities', category: 'StepFun' },
];

export const PROVIDER_ENDPOINTS: Record<string, string> = {
    openai: 'https://api.openai.com/v1',
    anthropic: 'https://api.anthropic.com/v1',
    google: 'https://generativelanguage.googleapis.com/v1beta/openai',
    gemini: 'https://generativelanguage.googleapis.com/v1beta/openai',
    huggingface: 'https://api-inference.huggingface.co',
    meta: 'https://openrouter.ai/api/v1',
    openrouter: 'https://openrouter.ai/api/v1',
    mistral: 'https://api.mistral.ai/v1',
    groq: 'https://api.groq.com/openai/v1',
    deepseek: 'https://api.deepseek.com/v1',
    xai: 'https://api.x.ai/v1',
    perplexity: 'https://api.perplexity.ai',
    cohere: 'https://api.cohere.ai/v1',
    together: 'https://api.together.xyz/v1',
    anyscale: 'https://api.endpoints.anyscale.com/v1',
    azure: 'https://YOUR_RESOURCE.openai.azure.com',
    aws: 'https://YOUR_REGION.bedrock.aws.amazon.com',
    stepfun: 'https://api.stepfun.com/v1',
};

export const getProviderEndpoint = (providerName: string): string => {
    const lower = providerName.toLowerCase();
    return PROVIDER_ENDPOINTS[lower] || `https://${lower}.com/api/v1`;
};
