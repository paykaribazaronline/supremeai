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
    private com.supremeai.repository.ProviderRepository mockRepository;
    private com.supremeai.service.ProviderMetadataService mockMetadataService;
    private com.supremeai.service.ProviderTypeRegistry mockTypeRegistry;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AIProviderFactory();

        mockService = Mockito.mock(AIProviderService.class);
        when(mockService.getActiveKey(anyString())).thenAnswer(inv -> {
            String provider = inv.getArgument(0);
            return "test-key-for-" + provider;
        });

        mockRankingService = Mockito.mock(ContextualAIRankingService.class);
        mockRepository = Mockito.mock(com.supremeai.repository.ProviderRepository.class);

        List<com.supremeai.model.APIProvider> providers = java.util.stream.Stream.of(
            "gpt4", "claude", "gemini", "deepseek",
            "gcp_llama", "gcp_phi",
            "hf_codellama", "hf_phi_vision", "hf_e5_large",
            "render_tinyllama", "render_phi2", "render_qwen",
            "openai", "anthropic", "groq", "huggingface", "kimi", "mistral", "stepfun", "codegeex4",
            "codegeex", "gcp_qwen", "gcp_nomic", "hf_deepseek", "hf_mistral", "hf_llama", "hf_phi", "render_phi3"
        ).map(name -> {
            com.supremeai.model.APIProvider p = new com.supremeai.model.APIProvider();
            p.setName(name);
            p.setId(name);
            p.setType(name);
            p.setStatus("ACTIVE");
            p.setBaseUrl("https://api." + name + ".com");
            return p;
        }).collect(java.util.stream.Collectors.toList());

        when(mockRepository.findAll()).thenReturn(reactor.core.publisher.Flux.fromIterable(providers));
        when(mockRepository.findByStatus(anyString())).thenReturn(reactor.core.publisher.Flux.fromIterable(providers));

        mockMetadataService = Mockito.mock(com.supremeai.service.ProviderMetadataService.class);
        when(mockMetadataService.getDefaultModel(anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        mockTypeRegistry = Mockito.mock(com.supremeai.service.ProviderTypeRegistry.class);

        com.supremeai.model.ProviderTypeConfig mockTypeConfig = Mockito.mock(com.supremeai.model.ProviderTypeConfig.class);
        when(mockTypeConfig.getDefaultBaseUrl()).thenReturn("https://api.openai.com/v1");
        when(mockTypeConfig.getDefaultModel()).thenReturn("gpt-4");
        when(mockTypeRegistry.getTypeConfig(anyString())).thenReturn(mockTypeConfig);

        setField(factory, "aiProviderService", mockService);
        setField(factory, "contextualRankingService", mockRankingService);
        setField(factory, "providerRepository", mockRepository);
        setField(factory, "providerMetadataService", mockMetadataService);
        setField(factory, "providerTypeRegistry", mockTypeRegistry);

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
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_anthropic_shouldCreateAnthropicProvider() {
        AIProvider provider = factory.getProvider("anthropic");
        assertNotNull(provider);
        assertEquals("anthropic", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_gemini_shouldCreateGeminiProvider() {
        AIProvider provider = factory.getProvider("gemini");
        assertNotNull(provider);
        assertEquals("gemini", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_groq_shouldCreateGroqProvider() {
        AIProvider provider = factory.getProvider("groq");
        assertNotNull(provider);
        assertEquals("groq", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_deepseek_shouldCreateDeepSeekProvider() {
        AIProvider provider = factory.getProvider("deepseek");
        assertNotNull(provider);
        assertEquals("deepseek", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_huggingface_shouldCreateHuggingFaceProvider() {
        AIProvider provider = factory.getProvider("huggingface");
        assertNotNull(provider);
        assertEquals("huggingface", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_kimi_shouldCreateKimiProvider() {
        AIProvider provider = factory.getProvider("kimi");
        assertNotNull(provider);
        assertEquals("kimi", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_mistral_shouldCreateMistralProvider() {
        AIProvider provider = factory.getProvider("mistral");
        assertNotNull(provider);
        assertEquals("mistral", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_stepfun_shouldCreateStepFunProvider() {
        AIProvider provider = factory.getProvider("stepfun");
        assertNotNull(provider);
        assertEquals("stepfun", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_codegeex4_shouldCreateCodeGeeX4Provider() {
        AIProvider provider = factory.getProvider("codegeex4");
        assertNotNull(provider);
        assertEquals("codegeex4", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void getProvider_ollama_shouldThrowWhenNotAvailable() {
        when(mockMetadataService.getAllMetadata()).thenReturn(java.util.Map.of());
        when(mockRepository.findByStatus(anyString())).thenReturn(reactor.core.publisher.Flux.empty());
        when(mockTypeRegistry.getTypeConfig(eq("ollama"))).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> factory.getProvider("ollama"));
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

        assertEquals("gpt4", gpt4.getName());
        assertEquals("claude", claude.getName());
    }

    @Test
    void getProvider_withOverrideKey_shouldUseOverride() {
        AIProvider provider = factory.getProvider("openai", "sk-override-key");
        assertNotNull(provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    void getProvider_unknownProvider_shouldThrow() {
        when(mockMetadataService.getAllMetadata()).thenReturn(java.util.Map.of());
        when(mockRepository.findByStatus(anyString())).thenReturn(reactor.core.publisher.Flux.empty());
        when(mockTypeRegistry.getTypeConfig(eq("unknown-ai"))).thenReturn(null);
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

        assertEquals(28, providers.length);
        List<String> providerList = java.util.Arrays.asList(providers);
        
        // Primary
        assertTrue(providerList.contains("gpt4"));
        assertTrue(providerList.contains("claude"));
        assertTrue(providerList.contains("gemini"));
        assertTrue(providerList.contains("deepseek"));
        
        // GCP
        assertTrue(providerList.contains("gcp_llama"));
        assertTrue(providerList.contains("gcp_phi"));
        
        // HuggingFace
        assertTrue(providerList.contains("hf_codellama"));
        assertTrue(providerList.contains("hf_phi_vision"));
        assertTrue(providerList.contains("hf_e5_large"));
        
        // Render
        assertTrue(providerList.contains("render_tinyllama"));
        assertTrue(providerList.contains("render_phi2"));
        assertTrue(providerList.contains("render_qwen"));
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

        AIProvider provider = factory.getBestProviderForTask("code_generation").block();
        assertNotNull(provider);
    }

    @Test
    void getBestProviderForTask_withNullRankings_shouldFallBackToDefault() {
        when(mockRankingService.getRankingsForTask(any())).thenReturn(null);

        AIProvider provider = factory.getBestProviderForTask("code_generation").block();
        assertNotNull(provider);
    }

    @Test
    void getBestProviderForTask_withRankingException_shouldFallBackToDefault() {
        when(mockRankingService.getRankingsForTask(any())).thenThrow(new RuntimeException("Ranking error"));

        AIProvider provider = factory.getBestProviderForTask("code_generation").block();
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
        Map<String, String> expectedProviders = Map.of(
                "openai", "openai",
                "claude", "claude",
                "gemini", "gemini",
                "groq", "groq",
                "deepseek", "deepseek",
                "huggingface", "huggingface",
                "kimi", "kimi",
                "mistral", "mistral",
                "stepfun", "stepfun",
                "codegeex4", "codegeex4"
        );

        for (Map.Entry<String, String> entry : expectedProviders.entrySet()) {
            AIProvider provider = factory.getProvider(entry.getKey());
            assertEquals(entry.getValue(), provider.getName(),
                    "Provider name mismatch for: " + entry.getKey());
            assertTrue(provider instanceof SupremeCloudProvider,
                    "Provider type mismatch for: " + entry.getKey());
        }
    }
}