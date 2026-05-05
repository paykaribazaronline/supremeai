package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIProviderFactory.
 * Tests provider creation, key resolution, and error handling.
 */
class AIProviderFactoryTest {

    private AIProviderFactory factory;
    private AIProviderService mockService;

    @BeforeEach
    void setUp() throws Exception {
        factory = new AIProviderFactory();
        
        // Create mock AIProviderService
        mockService = Mockito.mock(AIProviderService.class);
        
        // Set up mock to return test keys
        when(mockService.getActiveKey("groq")).thenReturn("gsk-test-groq-key");
        when(mockService.getActiveKey("openai")).thenReturn("sk-test-openai-key");
        when(mockService.getActiveKey("anthropic")).thenReturn("sk-ant-test-anthropic-key");
        when(mockService.getActiveKey("gemini")).thenReturn("AIzaSy-test-gemini-key");
        when(mockService.getActiveKey("huggingface")).thenReturn("hf-test-huggingface-key");
        
        // Use reflection to set the mock service
        java.lang.reflect.Field field = AIProviderFactory.class.getDeclaredField("aiProviderService");
        field.setAccessible(true);
        field.set(factory, mockService);
    }

    @Test
    void testGetGroqProvider() {
        AIProvider provider = factory.getProvider("groq");
        
        assertNotNull(provider);
        assertEquals("groq", provider.getName());
        assertTrue(provider instanceof GroqProvider);
    }

    @Test
    void testGetOpenAIProvider() {
        AIProvider provider = factory.getProvider("openai");
        
        assertNotNull(provider);
        assertEquals("openai", provider.getName());
        assertTrue(provider instanceof OpenAIProvider);
    }

    @Test
    void testGetAnthropicProvider() {
        AIProvider provider = factory.getProvider("anthropic");
        
        assertNotNull(provider);
        assertEquals("anthropic", provider.getName());
        assertTrue(provider instanceof AnthropicProvider);
    }

    @Test
    void testGetGeminiProvider() {
        AIProvider provider = factory.getProvider("gemini");
        
        assertNotNull(provider);
        assertEquals("gemini", provider.getName());
        assertTrue(provider instanceof GeminiProvider);
    }

    @Test
    void testGetHuggingFaceProvider() {
        AIProvider provider = factory.getProvider("huggingface");
        
        assertNotNull(provider);
        assertEquals("huggingface", provider.getName());
        assertTrue(provider instanceof HuggingFaceProvider);
    }

    @Test
    void testGetProviderCaseInsensitive() {
        // Provider names should be case-insensitive
        AIProvider groqUpper = factory.getProvider("GROQ");
        AIProvider groqMixed = factory.getProvider("GrOq");
        AIProvider groqLower = factory.getProvider("groq");
        
        assertNotNull(groqUpper);
        assertNotNull(groqMixed);
        assertNotNull(groqLower);
    }

    @Test
    void testGetProviderWithOverrideKey() {
        String overrideKey = "sk-override-key";
        
        AIProvider provider = factory.getProvider("openai", overrideKey);
        
        assertNotNull(provider);
        assertEquals("openai", provider.getName());
    }

    @Test
    void testGetProviderUnknownProvider() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getProvider("unknown-provider");
        });
    }

    @Test
    void testGetProviderEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getProvider("");
        });
    }

    @Test
    void testGetProviderNullName() {
        // Null name causes NullPointerException in toLowerCase() before IllegalArgumentException
        assertThrows(Exception.class, () -> {
            factory.getProvider(null);
        });
    }

    @Test
    void testGetOllamaProviderNotAvailable() {
        // Ollama provider is not set in cloud profile
        assertThrows(IllegalStateException.class, () -> {
            factory.getProvider("ollama");
        });
    }

    @Test
    void testProviderCapabilities() {
        AIProvider provider = factory.getProvider("groq");
        
        assertNotNull(provider.getCapabilities());
        assertTrue(provider.getCapabilities() instanceof java.util.Map);
    }
}
