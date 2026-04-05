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
import static org.mockito.Mockito.mock;
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

    @Test
    void auditKingModeCriticalByAnnotationQueuesPendingApproval() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("refreshProviderKey");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"provider-1"});

        KingMode annotation = mock(KingMode.class);
        when(annotation.requireSecondaryApproval()).thenReturn(true);
        Object result = aspect.auditKingMode(joinPoint, annotation);

        KingModeAuditAspect.PendingActionResult pending =
            assertInstanceOf(KingModeAuditAspect.PendingActionResult.class, result);
        assertEquals("PENDING_APPROVAL", pending.getStatus());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void approvePendingActionExecutesForDifferentAdmin() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("forceStop");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"node-a"});
        when(joinPoint.proceed()).thenReturn("approved");

        KingModeAuditAspect.PendingActionResult pending =
            (KingModeAuditAspect.PendingActionResult) aspect.auditKingMode(joinPoint);

        Object approved = aspect.approvePendingAction(
            pending.getActionId(),
            "second-admin",
            "Validated by duty admin"
        );

        assertEquals("approved", approved);
        assertEquals(0, aspect.getPendingApprovals().size());
        verify(auditLogger).logSecurityEvent(eq("KING_MODE"), anyString(), eq("second-admin"));
    }

    @Test
    void approvePendingActionFailsForSameAdmin() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("forceStop");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"node-a"});

        KingModeAuditAspect.PendingActionResult pending =
            (KingModeAuditAspect.PendingActionResult) aspect.auditKingMode(joinPoint);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> aspect.approvePendingAction(pending.getActionId(), "admin-user", "self approve")
        );

        assertEquals("Same admin cannot approve their own action", ex.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void rejectPendingActionRemovesFromQueue() throws Throwable {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("forceStop");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"node-a"});

        KingModeAuditAspect.PendingActionResult pending =
            (KingModeAuditAspect.PendingActionResult) aspect.auditKingMode(joinPoint);

        aspect.rejectPendingAction(pending.getActionId(), "review-admin", "Policy check failed");

        assertEquals(0, aspect.getPendingApprovals().size());
        verify(auditLogger).logSecurityEvent(eq("KING_MODE"), anyString(), eq("review-admin"));
    }
}
