const functions = require('firebase-functions');
const admin = require('firebase-admin');

// ============ SMART PROVIDER DISCOVERY ============
// Discovers AI models from multiple sources:
// 1. Firestore (user-added API keys)
// 2. Environment config (Firebase, Vertex AI)
// 3. Cloud Run service discovery (deployed models)

async function discoverProviders() {
  const providers = [];

  // ── Source 1: Firestore (user-configured API keys) ──
  try {
    const db = admin.firestore();
    const snap = await db.collection('ai_providers').where('status', '==', 'active').get();
    snap.forEach(doc => {
      const data = doc.data();
      providers.push({
        id: doc.id,
        name: data.name || doc.id,
        type: data.type || 'api',
        deploymentSource: data.deploymentSource || 'api',
        status: 'active',
        apiKeyConfigured: !!data.apiKey,
        models: data.models || [],
        roles: data.roles || ['general_chat'],
        source: 'firestore',
      });
    });
  } catch (_) {
    // Firestore may not be available in emulator
  }

  // ── Source 2: Environment config ──
  const envProviders = discoverFromEnv();
  providers.push(...envProviders);

  // ── Source 3: Cloud Run deployed services ──
  const cloudRunProviders = getCloudRunModels();
  providers.push(...cloudRunProviders);

  return providers;
}

function discoverFromEnv() {
  const providers = [];

  // Firebase / Gemini API (always available via client SDK)
  if (process.env.FIREBASE_API_KEY || process.env.GEMINI_API_KEY) {
    providers.push({
      id: 'firebase-gemini',
      name: 'Firebase Gemini API',
      type: 'firebase',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: true,
      models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro-vision'],
      roles: ['general_chat', 'reasoning', 'multimodal', 'vision'],
      source: 'env',
    });
  }

  // Vertex AI
  if (process.env.VERTEX_AI_PROJECT || process.env.GCLOUD_PROJECT) {
    providers.push({
      id: 'vertex-ai',
      name: 'Vertex AI (Google Cloud)',
      type: 'vertex',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: true,
      models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'claude-3-5-sonnet', 'text-embedding-004'],
      roles: ['coding', 'reasoning', 'security', 'embedding'],
      source: 'env',
    });
  }

  // OpenAI
  if (process.env.OPENAI_API_KEY) {
    providers.push({
      id: 'openai',
      name: 'OpenAI',
      type: 'openai',
      deploymentSource: 'api',
      status: 'active',
      apiKeyConfigured: true,
      models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'o1-preview'],
      roles: ['coding', 'reasoning', 'general_chat'],
      source: 'env',
    });
  }

  // Anthropic
  if (process.env.ANTHROPIC_API_KEY) {
    providers.push({
      id: 'anthropic',
      name: 'Anthropic Claude',
      type: 'anthropic',
      deploymentSource: 'api',
      status: 'active',
      apiKeyConfigured: true,
      models: ['claude-3-5-sonnet', 'claude-3-haiku', 'claude-3-opus'],
      roles: ['coding', 'reasoning', 'security'],
      source: 'env',
    });
  }

  return providers;
}

function getCloudRunModels() {
  // These are the actual deployed Cloud Run AI model services in supremeai-a
  return [
    {
      id: 'cloudrun-deepseek-pro',
      name: 'DeepSeek Pro (Cloud Run)',
      type: 'ollama',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: false,
      endpoint: 'https://supreme-ai-deepseek-pro-lhlwyikwlq-uc.a.run.app',
      models: ['deepseek-coder-v2', 'deepseek-r1'],
      roles: ['coding', 'reasoning'],
      source: 'cloudrun',
    },
    {
      id: 'cloudrun-llama-3-1',
      name: 'Llama 3.1 (Cloud Run)',
      type: 'ollama',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: false,
      endpoint: 'https://supreme-ai-llama-3-1-lhlwyikwlq-uc.a.run.app',
      models: ['llama-3.1-8b', 'llama-3.1-70b'],
      roles: ['general_chat', 'reasoning', 'coding'],
      source: 'cloudrun',
    },
    {
      id: 'cloudrun-phi-3',
      name: 'Phi-3 (Cloud Run)',
      type: 'ollama',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: false,
      endpoint: 'https://supreme-ai-phi-3-lhlwyikwlq-uc.a.run.app',
      models: ['phi-3-mini', 'phi-3-medium'],
      roles: ['fast_chat', 'general_chat'],
      source: 'cloudrun',
    },
    {
      id: 'cloudrun-qwen-coder',
      name: 'Qwen Coder (Cloud Run)',
      type: 'ollama',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: false,
      endpoint: 'https://supreme-ai-qwen-coder-lhlwyikwlq-uc.a.run.app',
      models: ['qwen-2.5-coder-7b', 'qwen-2.5-7b'],
      roles: ['coding', 'reasoning'],
      source: 'cloudrun',
    },
    {
      id: 'cloudrun-nomic-embed',
      name: 'Nomic Embed (Cloud Run)',
      type: 'ollama',
      deploymentSource: 'gcloud',
      status: 'active',
      apiKeyConfigured: false,
      endpoint: 'https://supreme-ai-nomic-embed-lhlwyikwlq-uc.a.run.app',
      models: ['nomic-embed-text-v1.5'],
      roles: ['embedding'],
      source: 'cloudrun',
    },
  ];
}

// ============ API ENDPOINTS ============

exports.getConfiguredProviders = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        providers,
        total: providers.length,
        active: providers.length,
        sources: [...new Set(providers.map(p => p.source))],
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

exports.getProviderHealthStats = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        total: providers.length,
        active: providers.filter(p => p.status === 'active').length,
        error: 0,
        bySource: {
          firestore: providers.filter(p => p.source === 'firestore').length,
          env: providers.filter(p => p.source === 'env').length,
          cloudrun: providers.filter(p => p.source === 'cloudrun').length,
        }
      }
    });
  } catch (err) {
    res.json({ success: true, data: { total: 0, active: 0, error: 0 } });
  }
});
