package org.example.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AIAPIServiceTest {

    @Test
    void normalizesProviderAliasesToCanonicalModels() {
        AIAPIService service = new AIAPIService(new HashMap<>());

        assertEquals("GPT4", service.normalizeModelName("openai-gpt4"));
        assertEquals("CLAUDE", service.normalizeModelName("anthropic-claude"));
        assertEquals("LLAMA", service.normalizeModelName("meta-llama"));
        assertEquals("DEEPSEEK", service.normalizeModelName("deepseek"));
        assertEquals("GEMINI", service.normalizeModelName("google-gemini"));
        assertEquals("COHERE", service.normalizeModelName("cohere"));
        assertEquals("XAI", service.normalizeModelName("xai-grok"));
    }

    @Test
    void providesDeterministicFallbackChainPerProvider() {
        AIAPIService service = new AIAPIService(new HashMap<>());

        List<String> chain = service.getFallbackChainForProvider("xai-grok");

        assertFalse(chain.isEmpty());
        assertEquals("XAI", chain.get(0));
        assertTrue(chain.contains("GPT4"));
        assertTrue(chain.contains("CLAUDE"));
    }

    @Test
    void reportsNativeConnectorStatusForDirectProviders() {
        Map<String, String> keys = new HashMap<>();
        keys.put("GEMINI", "gemini-test-key");
        AIAPIService service = new AIAPIService(keys);

        assertTrue(service.hasNativeConnector("google-gemini"));
        assertTrue(service.isProviderConfigured("google-gemini"));
        assertEquals("GEMINI", service.getProviderConnectorStatus("google-gemini").get("canonicalModel"));
    }
}