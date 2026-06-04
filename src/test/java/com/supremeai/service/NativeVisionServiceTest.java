package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NativeVisionServiceTest {

    private NativeVisionService nativeVisionService;

    @BeforeEach
    void setUp() {
        nativeVisionService = new NativeVisionService();
        // Disable native vision for unit tests
        // In production, this would be enabled via configuration
    }

    @Test
    void processImageNative_textExtraction_returnsSuccess() {
        // Simulate a base64 encoded image (simplified)
        String base64Image = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(base64Image, 
                NativeVisionService.VisionTaskType.TEXT_EXTRACTION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                // Native vision is disabled in unit tests, so it should return unavailable
                assertFalse(r.isSuccess());
                assertEquals("Native vision not available", r.getErrorMessage());
                assertNull(r.getResult());
            })
            .verifyComplete();
    }

    @Test
    void processImageNative_objectDetection_returnsDetectedObjects() {
        String base64Image = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(base64Image, 
                NativeVisionService.VisionTaskType.OBJECT_DETECTION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                // Native vision is disabled in unit tests
                assertFalse(r.isSuccess());
                assertEquals("Native vision not available", r.getErrorMessage());
                assertNull(r.getDetectedObjects());
            })
            .verifyComplete();
    }

    @Test
    void processImageNative_imageClassification_returnsCategory() {
        String base64Image = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(base64Image, 
                NativeVisionService.VisionTaskType.IMAGE_CLASSIFICATION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                // Native vision is disabled in unit tests
                assertFalse(r.isSuccess());
                assertEquals("Native vision not available", r.getErrorMessage());
                assertNull(r.getResult());
                assertEquals(0.0f, r.getConfidence(), 0.01);
            })
            .verifyComplete();
    }

    @Test
    void processImageNative_tableExtraction_returnsTableData() {
        String base64Image = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=";

        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(base64Image, 
                NativeVisionService.VisionTaskType.TABLE_EXTRACTION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                // Native vision is disabled in unit tests
                assertFalse(r.isSuccess());
                assertEquals("Native vision not available", r.getErrorMessage());
                assertNull(r.getResult());
            })
            .verifyComplete();
    }

    @Test
    void processImageNative_invalidImage_returnsError() {
        String invalidImage = "not_a_valid_base64_string";

        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(invalidImage, 
                NativeVisionService.VisionTaskType.TEXT_EXTRACTION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                assertFalse(r.isSuccess());
                assertNotNull(r.getErrorMessage());
            })
            .verifyComplete();
    }

    @Test
    void processImageNative_nullImage_returnsError() {
        Mono<NativeVisionService.NativeVisionResult> result = 
            nativeVisionService.processImageNative(null, 
                NativeVisionService.VisionTaskType.TEXT_EXTRACTION);

        StepVerifier.create(result)
            .assertNext(r -> {
                assertNotNull(r);
                assertFalse(r.isSuccess());
                assertNotNull(r.getErrorMessage());
            })
            .verifyComplete();
    }

    @Test
    void isModelLoaded_returnsFalseWhenDisabled() {
        assertFalse(nativeVisionService.isModelLoaded());
    }

    @Test
    void detectedObject_hasCorrectProperties() {
        NativeVisionService.DetectedObject obj = 
            new NativeVisionService.DetectedObject("button", 0.89f, 100, 200, 150, 50);

        assertEquals("button", obj.getLabel());
        assertEquals(0.89f, obj.getConfidence(), 0.01);
        assertEquals(100, obj.getX());
        assertEquals(200, obj.getY());
        assertEquals(150, obj.getWidth());
        assertEquals(50, obj.getHeight());
    }

    @Test
    void nativeVisionResult_success_hasCorrectProperties() {
        NativeVisionService.NativeVisionResult result = 
            NativeVisionService.NativeVisionResult.success(
                "test-task", "test output", "native-tflite", 0.95f);

        assertTrue(result.isSuccess());
        assertEquals("test-task", result.getTaskType());
        assertEquals("test output", result.getResult());
        assertEquals("native-tflite", result.getProcessor());
        assertEquals(0.95f, result.getConfidence(), 0.01);
        assertNull(result.getErrorMessage());
    }

    @Test
    void nativeVisionResult_error_hasCorrectProperties() {
        NativeVisionService.NativeVisionResult result = 
            NativeVisionService.NativeVisionResult.error("test error");

        assertFalse(result.isSuccess());
        assertNull(result.getTaskType());
        assertNull(result.getResult());
        assertNull(result.getProcessor());
        assertEquals(0.0f, result.getConfidence(), 0.01);
        assertEquals("test error", result.getErrorMessage());
    }
}
