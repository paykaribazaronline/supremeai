package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import com.supremeai.service.ContextualAIRankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AIProviderFactoryEnhancedTest {

    private AIProviderFactory factory;
    private AIProviderService mockService;
    private ContextualAIRankingService mockRankingService;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AIProviderFactory();

        mockService = Mockito.mock(AIProviderService.class);
        when(mockService.getActiveKey(anyString())).thenAnswer(inv -> {
            String provider = inv.getArgument(0);
            return "test-key-for-" + provider;
        });

        mockRankingService = Mockito.mock(ContextualAIRankingService.class);

        setField(factory, "aiProviderService", mockService);
        setField(factory, "contextualRankingService", mockRankingService);

        // Pre-populate health cache to avoid real network calls
        @SuppressWarnings("unchecked")
        Map<String, Boolean> healthCache = (Map<String, Boolean>) getField(factory, "providerHealthCache");
        for (String p : factory.getSupportedProviders()) {
            healthCache.put(p, true);
        }
        // Also add aliases
        healthCache.put("openai", true);
        healthCache.put("anthropic", true);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = AIProviderFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AIProviderFactory.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getProvider_openai_shouldCreateOpenAIProvider() {
        AIProvider provider = factory.getProvider("openai");
        assertNotNull(provider);
        assertEquals("openai", provider.getName());
        assertTrue(provider instanceof OpenAIProvider);
    }

    @Test
    void getProvider_anthropic_shouldCreateAnthropicProvider() {
        AIProvider provider = factory.getProvider("anthropic");
        assertNotNull(provider);
        assertEquals("anthropic", provider.getName());
        assertTrue(provider instanceof AnthropicProvider);
    }

    @Test
    void getProvider_gemini_shouldCreateGeminiProvider() {
        AIProvider provider = factory.getProvider("gemini");
        assertNotNull(provider);
        assertEquals("gemini", provider.getName());
        assertTrue(provider instanceof GeminiProvider);
    }

    @Test
    void getProvider_groq_shouldCreateGroqProvider() {
        AIProvider provider = factory.getProvider("groq");
        assertNotNull(provider);
        assertEquals("groq", provider.getName());
        assertTrue(provider instanceof GroqProvider);
    }

    @Test
    void getProvider_deepseek_shouldCreateDeepSeekProvider() {
        AIProvider provider = factory.getProvider("deepseek");
        assertNotNull(provider);
        assertEquals("deepseek", provider.getName());
        assertTrue(provider instanceof DeepSeekProvider);
    }

    @Test
    void getProvider_huggingface_shouldCreateHuggingFaceProvider() {
        AIProvider provider = factory.getProvider("huggingface");
        assertNotNull(provider);
        assertEquals("huggingface", provider.getName());
        assertTrue(provider instanceof HuggingFaceProvider);
    }

    @Test
    void getProvider_kimi_shouldCreateKimiProvider() {
        AIProvider provider = factory.getProvider("kimi");
        assertNotNull(provider);
        assertEquals("kimi", provider.getName());
        assertTrue(provider instanceof KimiProvider);
    }

    @Test
    void getProvider_mistral_shouldCreateMistralProvider() {
        AIProvider provider = factory.getProvider("mistral");
        assertNotNull(provider);
        assertEquals("mistral", provider.getName());
        assertTrue(provider instanceof MistralProvider);
    }

    @Test
    void getProvider_stepfun_shouldCreateStepFunProvider() {
        AIProvider provider = factory.getProvider("stepfun");
        assertNotNull(provider);
        assertEquals("stepfun", provider.getName());
        assertTrue(provider instanceof StepFunProvider);
    }

    @Test
    void getProvider_codegeex4_shouldCreateCodeGeeX4Provider() {
        AIProvider provider = factory.getProvider("codegeex4");
        assertNotNull(provider);
        assertEquals("codegeex4", provider.getName());
        assertTrue(provider instanceof CodeGeeX4Provider);
    }

    @Test
    void getProvider_ollama_shouldThrowWhenNotAvailable() {
        assertThrows(IllegalStateException.class, () -> factory.getProvider("ollama"));
    }

    @Test
    void getProvider_caseInsensitive_shouldWork() {
        AIProvider p1 = factory.getProvider("OpenAI");
        AIProvider p2 = factory.getProvider("OPENAI");
        AIProvider p3 = factory.getProvider("openai");

        assertEquals("openai", p1.getName());
        assertEquals("openai", p2.getName());
        assertEquals("openai", p3.getName());
    }

    @Test
    void getProvider_alternativeNames_shouldWork() {
        AIProvider gpt4 = factory.getProvider("gpt4");
        AIProvider claude = factory.getProvider("claude");

        assertEquals("openai", gpt4.getName());
        assertEquals("anthropic", claude.getName());
    }

    @Test
    void getProvider_withOverrideKey_shouldUseOverride() {
        AIProvider provider = factory.getProvider("openai", "sk-override-key");
        assertNotNull(provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    void getProvider_unknownProvider_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getProvider("unknown-ai"));
        assertTrue(ex.getMessage().contains("Unknown AI provider"));
    }

    @Test
    void getProvider_emptyString_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> factory.getProvider(""));
    }

    @Test
    void getProvider_null_shouldThrow() {
        assertThrows(Exception.class, () -> factory.getProvider(null));
    }

    @Test
    void getSupportedProviders_shouldReturnAllProviders() {
        String[] providers = factory.getSupportedProviders();

        assertEquals(15, providers.length);
        assertTrue(java.util.Arrays.asList(providers).contains("gpt4"));
        assertTrue(java.util.Arrays.asList(providers).contains("claude"));
        assertTrue(java.util.Arrays.asList(providers).contains("gemini"));
        assertTrue(java.util.Arrays.asList(providers).contains("groq"));
        assertTrue(java.util.Arrays.asList(providers).contains("deepseek"));
        assertTrue(java.util.Arrays.asList(providers).contains("ollama"));
        assertTrue(java.util.Arrays.asList(providers).contains("huggingface"));
        assertTrue(java.util.Arrays.asList(providers).contains("kimi"));
        assertTrue(java.util.Arrays.asList(providers).contains("mistral"));
        assertTrue(java.util.Arrays.asList(providers).contains("stepfun"));
        assertTrue(java.util.Arrays.asList(providers).contains("codegeex4"));
        assertTrue(java.util.Arrays.asList(providers).contains("gcp_qwen"));
        assertTrue(java.util.Arrays.asList(providers).contains("gcp_llama"));
        assertTrue(java.util.Arrays.asList(providers).contains("gcp_phi"));
        assertTrue(java.util.Arrays.asList(providers).contains("hf_deepseek"));
    }

    @Test
    void getAllProviderNames_shouldReturnSameAsSupported() {
        String[] supported = factory.getSupportedProviders();
        String[] allNames = factory.getAllProviderNames();

        assertArrayEquals(supported, allNames);
    }

    @Test
    void getAllProviders_shouldReturnListOfProviders() {
        List<AIProvider> providers = factory.getAllProviders();

        assertNotNull(providers);
        assertTrue(providers.size() >= 10);
    }

@Test
    void getAvailableProviderIds_shouldReturnAllIds() {
        List<String> ids = factory.getAvailableProviderIds();

        assertNotNull(ids);
        assertTrue(ids.contains("gpt4"));
        assertTrue(ids.contains("claude"));
    }

    @Test
    void clearHealthCache_shouldNotThrow() {
        assertDoesNotThrow(() -> factory.clearHealthCache());
    }

    @Test
    void getBestProviderForTask_withEmptyRankings_shouldFallBackToDefault() {
        when(mockRankingService.getRankingsForTask(any())).thenReturn(List.of());

        AIProvider provider = factory.getBestProviderForTask("code_generation");
        assertNotNull(provider);
    }

    @Test
    void getBestProviderForTask_withNullRankings_shouldFallBackToDefault() {
        when(mockRankingService.getRankingsForTask(any())).thenReturn(null);

        AIProvider provider = factory.getBestProviderForTask("code_generation");
        assertNotNull(provider);
    }

    @Test
    void getBestProviderForTask_withRankingException_shouldFallBackToDefault() {
        when(mockRankingService.getRankingsForTask(any())).thenThrow(new RuntimeException("Ranking error"));

        AIProvider provider = factory.getBestProviderForTask("code_generation");
        assertNotNull(provider);
    }

    @Test
    void providerCapabilities_shouldBeNonNull() {
        AIProvider provider = factory.getProvider("openai");
        assertNotNull(provider.getCapabilities());
        assertTrue(provider.getCapabilities() instanceof Map);
    }

    @Test
    void providersCreatedByFactory_shouldHaveCorrectNames() {
        Map<String, Class<?>> expectedProviders = Map.of(
                "openai", OpenAIProvider.class,
                "claude", AnthropicProvider.class,
                "gemini", GeminiProvider.class,
                "groq", GroqProvider.class,
                "deepseek", DeepSeekProvider.class,
                "huggingface", HuggingFaceProvider.class,
                "kimi", KimiProvider.class,
                "mistral", MistralProvider.class,
                "stepfun", StepFunProvider.class,
                "codegeex4", CodeGeeX4Provider.class
        );

        for (Map.Entry<String, Class<?>> entry : expectedProviders.entrySet()) {
            AIProvider provider = factory.getProvider(entry.getKey());
            String expectedName = entry.getKey().equals("claude") ? "anthropic" : entry.getKey();
            assertEquals(expectedName, provider.getName(),
                    "Provider name mismatch for: " + entry.getKey());
            assertTrue(entry.getValue().isInstance(provider),
                    "Provider type mismatch for: " + entry.getKey());
        }
    }
}