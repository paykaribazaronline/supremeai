package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitySummaryControllerTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    private ActivitySummaryController controller;

    @BeforeEach
    void setUp() {
        controller = new ActivitySummaryController(activityLogRepository);
    }

    @Test
    void getActivitySummary_withNoFilters_shouldReturnRecentAndTotal() {
        ActivityLog log1 = new ActivityLog("LOGIN", "user1", "auth", "info", "Login", "success", "127.0.0.1");
        ActivityLog log2 = new ActivityLog("LOGOUT", "user1", "auth", "info", "Logout", "success", "127.0.0.1");

        when(activityLogRepository.findAll()).thenReturn(Flux.just(log1, log2));
        when(activityLogRepository.count()).thenReturn(Mono.just(2L));

        StepVerifier.create(controller.getActivitySummary(null, null))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertNotNull(body);
                    assertEquals(2L, body.get("totalActions"));
                    List<?> recent = (List<?>) body.get("recentActions");
                    assertEquals(2, recent.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getActivitySummary_withCategoryFilter_shouldUseCategoryQuery() {
        ActivityLog log = new ActivityLog("LOGIN", "user1", "auth", "info", "Login", "success", "127.0.0.1");

        when(activityLogRepository.findByCategoryOrderByTimestampDesc("AUTH")).thenReturn(Flux.just(log));
        when(activityLogRepository.count()).thenReturn(Mono.just(1L));

        StepVerifier.create(controller.getActivitySummary("auth", null))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertEquals(1L, body.get("totalActions"));
                    return true;
                })
                .verifyComplete();

        verify(activityLogRepository).findByCategoryOrderByTimestampDesc("AUTH");
        verify(activityLogRepository, never()).findBySeverityOrderByTimestampDesc(any());
    }

    @Test
    void getActivitySummary_withSeverityFilter_shouldUseSeverityQuery() {
        ActivityLog log = new ActivityLog("ERROR", "user1", "system", "critical", "Error", "failure", "127.0.0.1");

        when(activityLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL")).thenReturn(Flux.just(log));
        when(activityLogRepository.count()).thenReturn(Mono.just(1L));

        StepVerifier.create(controller.getActivitySummary(null, "critical"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertEquals(1L, body.get("totalActions"));
                    return true;
                })
                .verifyComplete();

        verify(activityLogRepository).findBySeverityOrderByTimestampDesc("CRITICAL");
    }

    @Test
    void getActivitySummary_withBothFilters_shouldPreferSeverity() {
        when(activityLogRepository.findBySeverityOrderByTimestampDesc("WARN")).thenReturn(Flux.empty());
        when(activityLogRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(controller.getActivitySummary("auth", "warn"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();

        verify(activityLogRepository).findBySeverityOrderByTimestampDesc("WARN");
        verify(activityLogRepository, never()).findByCategoryOrderByTimestampDesc(any());
    }

    @Test
    void getActivitySummary_shouldReturnEmptyList_whenNoLogs() {
        when(activityLogRepository.findAll()).thenReturn(Flux.empty());
        when(activityLogRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(controller.getActivitySummary(null, null))
                .expectNextMatches(response -> {
                    Map<String, Object> body = response.getBody();
                    assertEquals(0L, body.get("totalActions"));
                    List<?> recent = (List<?>) body.get("recentActions");
                    assertTrue(recent.isEmpty());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void logActivity_shouldSaveAndReturnLog() {
        ActivityLog input = new ActivityLog("ACTION", "user1", "test", "info", "Test action", "success", "10.0.0.1");
        ActivityLog saved = new ActivityLog("ACTION", "user1", "test", "info", "Test action", "success", "10.0.0.1");
        saved.setId("log-1");

        when(activityLogRepository.save(input)).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.logActivity(input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("log-1", response.getBody().getId());
                    assertEquals("ACTION", response.getBody().getAction());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void logActivity_shouldPreserveAllFields() {
        ActivityLog input = new ActivityLog("DEPLOY", "admin", "deployment", "info", "Deployed v2.0", "success", "192.168.1.1");

        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(controller.logActivity(input))
                .expectNextMatches(response -> {
                    ActivityLog body = response.getBody();
                    assertEquals("DEPLOY", body.getAction());
                    assertEquals("admin", body.getUser());
                    assertEquals("deployment", body.getCategory());
                    assertEquals("info", body.getSeverity());
                    assertEquals("Deployed v2.0", body.getDetails());
                    assertEquals("success", body.getOutcome());
                    assertEquals("192.168.1.1", body.getIp());
                    return true;
                })
                .verifyComplete();
    }
}
