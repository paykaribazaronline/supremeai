package org.example.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AIRouter}.
 *
 * <p>HTTP calls are mocked — no real network access is needed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIRouterTest {

    @Mock
    private AIConfigRepository configRepository;

    @Mock
    private RestTemplate restTemplate;

    private AIProviderProperties properties;
    private AIRouter router;

    @BeforeEach
    void setUp() {
        properties = buildTestProperties("kimi,deepseek,gemini");
        when(configRepository.loadPriorityOrder()).thenReturn(Optional.empty());
        router = new AIRouter(properties, configRepository, restTemplate);
        router.init();
    }

    // -------------------------------------------------------------------------
    // Sequence parsing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("parseSequence returns default when input is null")
    void parseSequence_null_returnsDefault() {
        List<String> result = AIRouter.parseSequence(null);
        assertThat(result).containsExactly("kimi", "deepseek", "gemini");
    }

    @Test
    @DisplayName("parseSequence returns default when input is blank")
    void parseSequence_blank_returnsDefault() {
        List<String> result = AIRouter.parseSequence("   ");
        assertThat(result).containsExactly("kimi", "deepseek", "gemini");
    }

    @Test
    @DisplayName("parseSequence trims whitespace and lowercases entries")
    void parseSequence_trimsAndLowercases() {
        List<String> result = AIRouter.parseSequence(" DeepSeek , KIMI ");
        assertThat(result).containsExactly("deepseek", "kimi");
    }

    @Test
    @DisplayName("parseSequence ignores empty tokens")
    void parseSequence_ignoresEmptyTokens() {
        List<String> result = AIRouter.parseSequence("kimi,,deepseek,");
        assertThat(result).containsExactly("kimi", "deepseek");
    }

    @Test
    @DisplayName("init uses persisted order from repository when available")
    void init_usesPersisted_whenAvailable() {
        when(configRepository.loadPriorityOrder()).thenReturn(Optional.of("deepseek,kimi"));

        AIRouter r = new AIRouter(properties, configRepository, restTemplate);
        r.init();

        assertThat(r.getCurrentSequence()).isEqualTo("deepseek,kimi");
    }

    @Test
    @DisplayName("init falls back to properties when repository is empty")
    void init_fallsBackToProperties_whenRepositoryEmpty() {
        when(configRepository.loadPriorityOrder()).thenReturn(Optional.empty());

        AIRouter r = new AIRouter(properties, configRepository, restTemplate);
        r.init();

        assertThat(r.getCurrentSequence()).isEqualTo("kimi,deepseek,gemini");
    }

    @Test
    @DisplayName("setAISequence updates current sequence and persists it")
    void setAISequence_updatesAndPersists() {
        router.setAISequence("gemini,kimi");

        assertThat(router.getCurrentSequence()).isEqualTo("gemini,kimi");
        verify(configRepository).savePriorityOrder("gemini,kimi");
    }

    // -------------------------------------------------------------------------
    // Routing / fallback
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generateCode returns first successful provider response")
    void generateCode_returnsFirstSuccess() {
        AIProvider mockProvider = mock(AIProvider.class);
        when(mockProvider.generate(anyString(), anyString()))
                .thenReturn(new AIRouter.AIResponse("fun main() {}", true));

        AIRouter r = routerWithProviders(Map.of("kimi", mockProvider), "kimi");
        AIRouter.AIResponse response = r.generateCode("write hello world", "code");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("fun main() {}");
        assertThat(response.getUsedAI()).isEqualTo("kimi");
    }

    @Test
    @DisplayName("generateCode falls back to next provider when first fails")
    void generateCode_fallsBackOnProviderFailure() {
        AIProvider failingProvider = mock(AIProvider.class);
        when(failingProvider.generate(anyString(), anyString()))
                .thenThrow(new AIProviderException("kimi", "timeout"));

        AIProvider successProvider = mock(AIProvider.class);
        when(successProvider.generate(anyString(), anyString()))
                .thenReturn(new AIRouter.AIResponse("println!(\"hi\")", true));

        AIRouter r = routerWithProviders(
                Map.of("kimi", failingProvider, "deepseek", successProvider),
                "kimi,deepseek");

        AIRouter.AIResponse response = r.generateCode("write hello world", "code");

        assertThat(response.getUsedAI()).isEqualTo("deepseek");
        assertThat(response.getCode()).isEqualTo("println!(\"hi\")");
    }

    @Test
    @DisplayName("generateCode throws AIAllProvidersFailedException when all providers fail")
    void generateCode_throwsWhenAllFail() {
        AIProvider p1 = mock(AIProvider.class);
        when(p1.generate(anyString(), anyString()))
                .thenThrow(new AIProviderException("kimi", "error1"));

        AIProvider p2 = mock(AIProvider.class);
        when(p2.generate(anyString(), anyString()))
                .thenThrow(new AIProviderException("deepseek", "error2"));

        AIRouter r = routerWithProviders(Map.of("kimi", p1, "deepseek", p2), "kimi,deepseek");

        assertThatThrownBy(() -> r.generateCode("prompt", "code"))
                .isInstanceOf(AIAllProvidersFailedException.class)
                .hasMessageContaining("kimi")
                .hasMessageContaining("deepseek");
    }

    @Test
    @DisplayName("generateCode skips unknown provider names gracefully")
    void generateCode_skipsUnknownProviders() {
        AIProvider goodProvider = mock(AIProvider.class);
        when(goodProvider.generate(anyString(), isNull()))
                .thenReturn(new AIRouter.AIResponse("ok", true));

        // sequence starts with "unknown" (not in registry), then "deepseek"
        AIRouter r = routerWithProviders(Map.of("deepseek", goodProvider), "unknown,deepseek");

        AIRouter.AIResponse response = r.generateCode("prompt", null);
        assertThat(response.getUsedAI()).isEqualTo("deepseek");
    }

    // -------------------------------------------------------------------------
    // Response / code extraction (via OpenAICompatibleProvider helper)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("extractCode returns raw text when no fences are present")
    void extractCode_noFences_returnsRaw() {
        String result = OpenAICompatibleProvider.extractCode("Hello world");
        assertThat(result).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("extractCode extracts content between fences")
    void extractCode_withFences_extractsCode() {
        String content = "Some text\n```kotlin\nfun main() {}\n```\nAfter";
        assertThat(OpenAICompatibleProvider.extractCode(content)).isEqualTo("fun main() {}");
    }

    @Test
    @DisplayName("extractCode returns everything after opening fence when closing fence is absent")
    void extractCode_unclosedFence_returnsRemainder() {
        String content = "Prefix\n```\nfun foo() {}";
        assertThat(OpenAICompatibleProvider.extractCode(content)).isEqualTo("fun foo() {}");
    }

    @Test
    @DisplayName("extractCode handles null input gracefully")
    void extractCode_null_returnsNull() {
        assertThat(OpenAICompatibleProvider.extractCode(null)).isNull();
    }

    @Test
    @DisplayName("extractCode handles blank input gracefully")
    void extractCode_blank_returnsBlank() {
        assertThat(OpenAICompatibleProvider.extractCode("  ")).isBlank();
    }

    // -------------------------------------------------------------------------
    // AIProviderException — secrets not leaked
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AIProviderException message does not expose API key")
    void aiProviderException_doesNotExposeKey() {
        String secretKey = "sk-super-secret-12345";
        AIProviderException ex = new AIProviderException("kimi",
                "HTTP call to provider 'kimi' failed: Connection refused");

        assertThat(ex.getMessage()).doesNotContain(secretKey);
        assertThat(ex.getProviderName()).isEqualTo("kimi");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates an {@link AIRouter} with the given providers injected via the
     * package-private test constructor — no reflection needed.
     */
    private AIRouter routerWithProviders(Map<String, AIProvider> providers, String sequence) {
        AIProviderProperties props = buildTestProperties(sequence);
        AIConfigRepository stubRepo = mock(AIConfigRepository.class);
        when(stubRepo.loadPriorityOrder()).thenReturn(Optional.empty());

        AIRouter r = new AIRouter(props, stubRepo, providers);
        r.init();
        return r;
    }

    private AIProviderProperties buildTestProperties(String order) {
        AIProviderProperties props = new AIProviderProperties();
        props.setPriorityOrder(order);

        AIProviderProperties.ProviderConfig kimi = new AIProviderProperties.ProviderConfig();
        kimi.setBaseUrl("http://localhost/mock/kimi");
        kimi.setModel("kimi-test");
        kimi.setApiKey("test-key");
        kimi.setEnabled(true);

        AIProviderProperties.ProviderConfig deepseek = new AIProviderProperties.ProviderConfig();
        deepseek.setBaseUrl("http://localhost/mock/deepseek");
        deepseek.setModel("deepseek-test");
        deepseek.setApiKey("test-key");
        deepseek.setEnabled(true);

        AIProviderProperties.ProviderConfig gemini = new AIProviderProperties.ProviderConfig();
        gemini.setBaseUrl("http://localhost/mock/gemini");
        gemini.setModel("gemini-test");
        gemini.setApiKey("test-key");
        gemini.setEnabled(true);

        props.getProviders().put("kimi", kimi);
        props.getProviders().put("deepseek", deepseek);
        props.getProviders().put("gemini", gemini);

        return props;
    }
}
