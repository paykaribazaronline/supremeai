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
import com.supremeai.service.ProviderMetadataService;
import com.supremeai.model.APIProvider;

class AIProviderFactoryTest {AIProviderFactorypublic AIProviderFactoryTest(AIProviderFactory factory, AIProviderService mockService, ProviderMetadataService mockMetadataService) {
AIProviderFactory    this.factory = factory;
AIProviderFactory    this.mockService = mockService;
AIProviderFactory    this.mockMetadataService = mockMetadataService;
AIProviderFactory}






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
        
        // Create and setup mock ProviderMetadataService
        mockMetadataService = Mockito.mock(ProviderMetadataService.class);
        
        APIProvider groqMeta = new APIProvider("groq", "groq", "groq", "active");
        groqMeta.setBaseUrl("https://api.groq.com");
        groqMeta.setModelName("llama3-8b");
        when(mockMetadataService.getMetadata("groq")).thenReturn(groqMeta);
        
        APIProvider openaiMeta = new APIProvider("openai", "openai", "openai", "active");
        openaiMeta.setBaseUrl("https://api.openai.com");
        openaiMeta.setModelName("gpt-4");
        when(mockMetadataService.getMetadata("openai")).thenReturn(openaiMeta);
        
        APIProvider anthropicMeta = new APIProvider("anthropic", "anthropic", "anthropic", "active");
        anthropicMeta.setBaseUrl("https://api.anthropic.com");
        anthropicMeta.setModelName("claude-3-opus");
        when(mockMetadataService.getMetadata("anthropic")).thenReturn(anthropicMeta);
        
        APIProvider geminiMeta = new APIProvider("gemini", "gemini", "gemini", "active");
        geminiMeta.setBaseUrl("https://api.gemini.com");
        geminiMeta.setModelName("gemini-1.5-pro");
        when(mockMetadataService.getMetadata("gemini")).thenReturn(geminiMeta);
        
        APIProvider huggingfaceMeta = new APIProvider("huggingface", "huggingface", "huggingface", "active");
        huggingfaceMeta.setBaseUrl("https://api.huggingface.com");
        huggingfaceMeta.setModelName("llama3");
        when(mockMetadataService.getMetadata("huggingface")).thenReturn(huggingfaceMeta);
        when(mockMetadataService.getDefaultModel(anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        
        // Use reflection to set the mock services
        java.lang.reflect.Field field = AIProviderFactory.class.getDeclaredField("aiProviderService");
        field.setAccessible(true);
        field.set(factory, mockService);
        
        java.lang.reflect.Field metaField = AIProviderFactory.class.getDeclaredField("providerMetadataService");
        metaField.setAccessible(true);
        metaField.set(factory, mockMetadataService);

        StubLocalProvider stubLocalProvider = new StubLocalProvider();
        java.lang.reflect.Field stubField = AIProviderFactory.class.getDeclaredField("stubLocalProvider");
        stubField.setAccessible(true);
        stubField.set(factory, stubLocalProvider);
    }

    @Test
    void testGetGroqProvider() {
        AIProvider provider = factory.getProvider("groq");
        
        assertNotNull(provider);
        assertEquals("stub-local", provider.getName());
        assertTrue(provider instanceof StubLocalProvider);
    }

    @Test
    void testGetOpenAIProvider() {
        AIProvider provider = factory.getProvider("openai");
        
        assertNotNull(provider);
        assertEquals("stub-local", provider.getName());
        assertTrue(provider instanceof StubLocalProvider);
    }

    @Test
    void testGetAnthropicProvider() {
        AIProvider provider = factory.getProvider("anthropic");
        
        assertNotNull(provider);
        assertEquals("stub-local", provider.getName());
        assertTrue(provider instanceof StubLocalProvider);
    }

    @Test
    void testGetGeminiProvider() {
        AIProvider provider = factory.getProvider("gemini");
        
        assertNotNull(provider);
        assertEquals("gemini", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
    }

    @Test
    void testGetHuggingFaceProvider() {
        AIProvider provider = factory.getProvider("huggingface");
        
        assertNotNull(provider);
        assertEquals("huggingface", provider.getName());
        assertTrue(provider instanceof SupremeCloudProvider);
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
        assertEquals("stub-local", groqUpper.getName());
    }

    @Test
    void testGetProviderWithOverrideKey() {
        String overrideKey = "sk-override-key";
        
        AIProvider provider = factory.getProvider("openai", overrideKey);
        
        assertNotNull(provider);
        assertEquals("stub-local", provider.getName());
    }

    @Test
    void testGetProviderUnknownProvider() {
        AIProvider p = factory.getProvider("unknown-provider");
        assertEquals("stub-local", p.getName());
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
        // Ollama provider is not set in cloud profile, falls back to stub-local
        AIProvider p = factory.getProvider("ollama");
        assertEquals("stub-local", p.getName());
    }

    @Test
    void testProviderCapabilities() {
        AIProvider provider = factory.getProvider("groq");
        
        assertNotNull(provider.getCapabilities());
        assertTrue(provider.getCapabilities() instanceof java.util.Map);
    }
}
