package org.example.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KingModeAuditAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private AuditLogger auditLogger;

    private KingModeAuditAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new KingModeAuditAspect();
        ReflectionTestUtils.setField(aspect, "auditLogger", auditLogger);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin-user", "password")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditKingModeNonCriticalSuccessLogsBeforeAndAfter() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("rotateKeys");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"safe-input", "normal-value"});
        when(joinPoint.proceed()).thenReturn("done");

        Object result = aspect.auditKingMode(joinPoint);

        assertEquals("done", result);
        verify(auditLogger, times(2)).logSecurityEvent(
            eq("KING_MODE"), anyString(), eq("admin-user")
        );
    }

    @Test
    void auditKingModeFailureLogsFailureAndRethrows() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("rotateKeys");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"safe-input"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("rotation failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> aspect.auditKingMode(joinPoint));

        assertEquals("rotation failed", ex.getMessage());
        verify(auditLogger, times(2)).logSecurityEvent(
            eq("KING_MODE"), anyString(), eq("admin-user")
        );
    }

    @Test
    void auditKingModeCriticalActionQueuesPendingApproval() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("forceStop");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"node-a"});

        Object result = aspect.auditKingMode(joinPoint);

        KingModeAuditAspect.PendingActionResult pending =
            assertInstanceOf(KingModeAuditAspect.PendingActionResult.class, result);
        assertEquals("PENDING_APPROVAL", pending.getStatus());

        List<KingModeAuditAspect.PendingApproval> approvals = aspect.getPendingApprovals();
        assertEquals(1, approvals.size());
        verify(joinPoint, never()).proceed();
    }
}
