package com.supremeai.controller;

import com.supremeai.service.NativeVisionService;
import com.supremeai.service.VisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OCRControllerTest {

    @Mock
    private NativeVisionService nativeVisionService;

    @Mock
    private VisionService visionService;

    @InjectMocks
    private OCRController ocrController;

    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile("file", "test-doc.pdf", "application/pdf", "dummy content".getBytes());
    }

    // ==================== Health Endpoint Tests ====================

    @Test
    void health_NativeModelLoaded_ReturnsReady() {
        when(nativeVisionService.isModelLoaded()).thenReturn(true);

        StepVerifier.create(ocrController.health())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals("READY", response.getBody().get("native_ocr"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void health_NativeModelNotLoaded_ReturnsFallback() {
        when(nativeVisionService.isModelLoaded()).thenReturn(false);

        StepVerifier.create(ocrController.health())
                .expectNextMatches(response -> {
                    assertEquals("FALLBACK", response.getBody().get("native_ocr"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== Process OCR Tests ====================

    @Test
    void processOCR_NativeVisionSuccess_ExtractsText() {
        when(nativeVisionService.isModelLoaded()).thenReturn(true);
        
        NativeVisionService.NativeVisionResult mockResult = mock(NativeVisionService.NativeVisionResult.class);
        when(mockResult.isSuccess()).thenReturn(true);
        when(mockResult.getResult()).thenReturn("Native Extracted Text");
        when(mockResult.getConfidence()).thenReturn(0.95f);
        
        when(nativeVisionService.processImageNative(anyString(), any()))
                .thenReturn(Mono.just(mockResult));

        StepVerifier.create(ocrController.processOCR(mockFile, "ben"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertTrue((Boolean) body.get("success"));
                    assertEquals("Native Extracted Text", body.get("text"));
                    assertEquals(95, body.get("confidence"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void processOCR_FallbackLocalExtraction_ExtractsText() {
        // Using PDF file to trigger document local extraction rule
        when(nativeVisionService.isModelLoaded()).thenReturn(false);

        StepVerifier.create(ocrController.processOCR(mockFile, "ben"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertTrue(body.get("text").toString().contains("[DOCUMENT]"));
                    assertEquals(70, body.get("confidence"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void processOCR_ExceptionThrown_HandlesErrorGracefully() {
        when(nativeVisionService.isModelLoaded()).thenThrow(new RuntimeException("Simulated crash"));

        StepVerifier.create(ocrController.processOCR(mockFile, "ben"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertEquals(50, body.get("confidence")); // Error fallback confidence
                    assertTrue(body.get("text").toString().contains("ত্রুটি হয়েছে"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== History & Export Operations Tests ====================

    @Test
    void historyOperations_ExportAndDelete() {
        // Mock storing an item by calling processOCR with local fallback first
        when(nativeVisionService.isModelLoaded()).thenReturn(false);
        Map<String, Object> processBody = ocrController.processOCR(mockFile, "ben").block().getBody();
        String generatedId = (String) processBody.get("id");

        // 1. Verify it exists in export
        StepVerifier.create(ocrController.exportResult(generatedId, "json"))
                .expectNextMatches(res -> {
                    Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
                    return generatedId.equals(data.get("id"));
                })
                .verifyComplete();

        // 2. Delete it
        StepVerifier.create(ocrController.deleteResult(generatedId))
                .expectNextMatches(res -> (Boolean) res.getBody().get("ok"))
                .verifyComplete();
    }

    @Test
    void getHistory_ReturnsList() {
        StepVerifier.create(ocrController.getHistory())
                .expectNextMatches(res -> {
                    assertEquals(HttpStatus.OK, res.getStatusCode());
                    assertNotNull(res.getBody().get("results"));
                    assertNotNull(res.getBody().get("total"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void processOCR_FallbackLocalExtraction_EnglishText() {
        when(nativeVisionService.isModelLoaded()).thenReturn(false);
        MockMultipartFile englishFile = new MockMultipartFile("file", "image.png", "image/png", "dummy content".getBytes());

        StepVerifier.create(ocrController.processOCR(englishFile, "en"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().get("text").toString().contains("Sample extracted text"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void exportResult_NotFound() {
        StepVerifier.create(ocrController.exportResult("invalid-id", "json"))
                .expectNextMatches(res -> {
                    Map<String, Object> data = (Map<String, Object>) res.getBody().get("data");
                    return "no data".equals(data.get("text"));
                })
                .verifyComplete();
    }
}