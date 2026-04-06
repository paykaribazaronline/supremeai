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
        assertTrue(chain.indexOf("DEEPSEEK") < chain.indexOf("GPT4"));
        assertTrue(chain.contains("GPT4"));
        assertTrue(chain.contains("CLAUDE"));
    }

    @Test
    void enforcesPromptAndOutputCaps() {
        AIAPIService service = new AIAPIService(new HashMap<>(), 7000, 2, 250, 10, 8, 60, 3, 30000, 200, 10, 1000);

        String longText = "x".repeat(5000);
        String cappedPrompt = service.applyPromptTokenCap(longText);
        String cappedOutput = service.applyOutputTokenCap(longText);

        assertTrue(cappedPrompt.length() < longText.length());
        assertTrue(cappedOutput.length() < longText.length());
    }

    @Test
    void operationalMetricsExposeCheapFirstDefaultChain() {
        AIAPIService service = new AIAPIService(new HashMap<>());

        @SuppressWarnings("unchecked")
        List<String> defaultChain = (List<String>) service.getOperationalMetrics().get("defaultFallbackChain");

        assertEquals("GROQ", defaultChain.get(0));
        assertEquals("DEEPSEEK", defaultChain.get(1));
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

    @Test
    void supportsAirLlmAsLocalEndpointProvider() {
        AIAPIService service = new AIAPIService(new HashMap<>());

        assertEquals("airllm-local", service.getCanonicalProviderId("airllm"));
        assertEquals("AIRLLM", service.normalizeModelName("airllm-local"));
        assertEquals("AirLLM Local", service.getProviderDisplayName("airllm-local"));
        assertTrue(service.hasNativeConnector("airllm-local"));
        assertFalse(service.requiresApiKey("airllm-local"));
        assertEquals("AIRLLM", service.getFallbackChainForProvider("airllm").get(0));
    }
}