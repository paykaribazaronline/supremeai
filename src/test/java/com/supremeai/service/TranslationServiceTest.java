package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private AIProvider groqProvider;

    private TranslationService translationService;

    @BeforeEach
    void setUp() throws Exception {
        translationService = new TranslationService();
        java.lang.reflect.Field field = TranslationService.class.getDeclaredField("providerFactory");
        field.setAccessible(true);
        field.set(translationService, providerFactory);
    }

    @Test
    void translateReturnsProviderResponse() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(org.mockito.ArgumentMatchers.contains("from English to Bengali")))
            .thenReturn("হ্যালো");

        StepVerifier.create(translationService.translate("Hello", "English", "Bengali"))
            .expectNext("হ্যালো")
            .verifyComplete();
    }

    @Test
    void translateFallsBackToOriginalTextWhenProviderLookupFails() {
        when(providerFactory.getProvider("groq")).thenThrow(new RuntimeException("provider unavailable"));

        StepVerifier.create(translationService.translate("Hello", "English", "French"))
            .expectNext("Hello")
            .verifyComplete();
    }
}
