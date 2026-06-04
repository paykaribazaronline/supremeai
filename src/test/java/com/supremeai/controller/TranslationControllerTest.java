package com.supremeai.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.supremeai.dto.LanguagePreference;
import com.supremeai.dto.TranslationRequest;
import com.supremeai.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TranslationControllerTest {

  @Mock private TranslationService translationService;

  private TranslationController controller;

  @BeforeEach
  void setUp() {
    controller = new TranslationController();
    // Use reflection to set private field
    try {
      java.lang.reflect.Field field =
          TranslationController.class.getDeclaredField("translationService");
      field.setAccessible(true);
      field.set(controller, translationService);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void translate_shouldReturnTranslatedText() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hello");
    request.setFromLanguage("en");
    request.setToLanguage("es");

    when(translationService.translate("Hello", "en", "es")).thenReturn(Mono.just("Hola"));

    StepVerifier.create(controller.translate(request))
        .expectNextMatches(
            response -> {
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody().isSuccess()
                  && "Hola".equals(response.getBody().getTranslatedText());
            })
        .verifyComplete();
  }

  @Test
  void translate_shouldReturnBadRequestOnError() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hello");
    request.setFromLanguage("en");
    request.setToLanguage("es");

    when(translationService.translate("Hello", "en", "es"))
        .thenReturn(Mono.error(new RuntimeException("Translation failed")));

    StepVerifier.create(controller.translate(request))
        .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
        .verifyComplete();
  }

  @Test
  void translateFromEnglish_shouldReturnTranslatedText() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hello");
    request.setToLanguage("es");

    when(translationService.translateFromEnglish("Hello", "es")).thenReturn(Mono.just("Hola"));

    StepVerifier.create(controller.translateFromEnglish(request))
        .expectNextMatches(
            response -> {
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody().isSuccess()
                  && "Hola".equals(response.getBody().getTranslatedText());
            })
        .verifyComplete();
  }

  @Test
  void translateFromEnglish_shouldReturnBadRequestOnError() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hello");
    request.setToLanguage("es");

    when(translationService.translateFromEnglish("Hello", "es"))
        .thenReturn(Mono.error(new RuntimeException("Translation failed")));

    StepVerifier.create(controller.translateFromEnglish(request))
        .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
        .verifyComplete();
  }

  @Test
  void translateToEnglish_shouldReturnTranslatedText() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hola");
    request.setFromLanguage("es");

    when(translationService.translateToEnglish("Hola", "es")).thenReturn(Mono.just("Hello"));

    StepVerifier.create(controller.translateToEnglish(request))
        .expectNextMatches(
            response -> {
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody().isSuccess()
                  && "Hello".equals(response.getBody().getTranslatedText());
            })
        .verifyComplete();
  }

  @Test
  void translateToEnglish_shouldReturnBadRequestOnError() {
    TranslationRequest request = new TranslationRequest();
    request.setText("Hola");
    request.setFromLanguage("es");

    when(translationService.translateToEnglish("Hola", "es"))
        .thenReturn(Mono.error(new RuntimeException("Translation failed")));

    StepVerifier.create(controller.translateToEnglish(request))
        .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
        .verifyComplete();
  }

  @Test
  void getSupportedLanguages_shouldReturnAllLanguagePreferences() {
    StepVerifier.create(controller.getSupportedLanguages())
        .expectNextMatches(
            response -> {
              return response.getStatusCode().is2xxSuccessful()
                  && response.getBody().length == LanguagePreference.values().length;
            })
        .verifyComplete();
  }
}
