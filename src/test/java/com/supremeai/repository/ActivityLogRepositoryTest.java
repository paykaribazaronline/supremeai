package com.supremeai.repository;

import com.supremeai.model.ActivityLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogRepositoryTest {ActivityLogRepositorypublic ActivityLogRepositoryTest(ActivityLogRepository activityLogRepository) {
ActivityLogRepository    this.activityLogRepository = activityLogRepository;
ActivityLogRepository}




    @Test
    void findByCategoryOrderByTimestampDesc_shouldReturnLogsForCategory() {
        ActivityLog log1 = new ActivityLog("LOGIN", "user1", "auth", "info", "User logged in", "success", "127.0.0.1");
        log1.setId("log-1");
        ActivityLog log2 = new ActivityLog("LOGOUT", "user1", "auth", "info", "User logged out", "success", "127.0.0.1");
        log2.setId("log-2");

        when(activityLogRepository.findByCategoryOrderByTimestampDesc("auth"))
                .thenReturn(Flux.fromIterable(List.of(log1, log2)));

        StepVerifier.create(activityLogRepository.findByCategoryOrderByTimestampDesc("auth"))
                .expectNextMatches(l -> "log-1".equals(l.getId()) && "auth".equals(l.getCategory()))
                .expectNextMatches(l -> "log-2".equals(l.getId()) && "auth".equals(l.getCategory()))
                .verifyComplete();
    }

    @Test
    void findByCategoryOrderByTimestampDesc_shouldReturnEmpty_whenNoLogs() {
        when(activityLogRepository.findByCategoryOrderByTimestampDesc("nonexistent"))
                .thenReturn(Flux.empty());

        StepVerifier.create(activityLogRepository.findByCategoryOrderByTimestampDesc("nonexistent"))
                .verifyComplete();
    }

    @Test
    void findBySeverityOrderByTimestampDesc_shouldReturnLogsForSeverity() {
        ActivityLog log = new ActivityLog("ERROR", "user1", "system", "critical", "System failure", "failure", "127.0.0.1");
        log.setId("log-3");

        when(activityLogRepository.findBySeverityOrderByTimestampDesc("critical"))
                .thenReturn(Flux.just(log));

        StepVerifier.create(activityLogRepository.findBySeverityOrderByTimestampDesc("critical"))
                .expectNextMatches(l -> "critical".equals(l.getSeverity()) && "failure".equals(l.getOutcome()))
                .verifyComplete();
    }

    @Test
    void findBySeverityOrderByTimestampDesc_shouldReturnEmpty_whenNoMatchingSeverity() {
        when(activityLogRepository.findBySeverityOrderByTimestampDesc("debug"))
                .thenReturn(Flux.empty());

        StepVerifier.create(activityLogRepository.findBySeverityOrderByTimestampDesc("debug"))
                .verifyComplete();
    }

    @Test
    void save_shouldPersistActivityLog() {
        ActivityLog log = new ActivityLog("ACTION", "user2", "data", "warning", "Data modified", "success", "10.0.0.1");
        when(activityLogRepository.save(log)).thenReturn(Mono.just(log));

        StepVerifier.create(activityLogRepository.save(log))
                .expectNextMatches(l -> "ACTION".equals(l.getAction()) && "user2".equals(l.getUser()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllLogs() {
        ActivityLog log1 = new ActivityLog("A", "u1", "c1", "info", "d1", "success", "1.1.1.1");
        ActivityLog log2 = new ActivityLog("B", "u2", "c2", "warning", "d2", "failure", "2.2.2.2");

        when(activityLogRepository.findAll()).thenReturn(Flux.just(log1, log2));

        StepVerifier.create(activityLogRepository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }
}
