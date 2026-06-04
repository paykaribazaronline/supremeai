package com.supremeai.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for all exception classes in the project. Tests constructors, message handling, and
 * exception chaining.
 */
class ExceptionTest {

  @Test
  void testSimulatorException_DefaultConstructor() {
    SimulatorException ex = new SimulatorException("Test message");
    assertEquals("Test message", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testSimulatorException_WithCause() {
    RuntimeException cause = new RuntimeException("Root cause");
    SimulatorException ex = new SimulatorException("Wrapper message", cause);
    assertEquals("Wrapper message", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void testSimulatorResourceNotFoundException() {
    SimulatorResourceNotFoundException ex =
        new SimulatorResourceNotFoundException("Resource not found");
    assertTrue(ex instanceof SimulatorException);
    assertEquals("Resource not found", ex.getMessage());
  }

  @Test
  void testSimulatorSessionException() {
    SimulatorSessionException ex = new SimulatorSessionException("Session error");
    assertTrue(ex instanceof SimulatorException);
    assertEquals("Session error", ex.getMessage());
  }

  @Test
  void testSimulatorConflictException() {
    SimulatorConflictException ex = new SimulatorConflictException("Conflict occurred");
    assertTrue(ex instanceof SimulatorException);
    assertEquals("Conflict occurred", ex.getMessage());
  }

  @Test
  void testSimulatorQuotaExceededException_WithUsedAndLimit() {
    SimulatorQuotaExceededException ex = new SimulatorQuotaExceededException(100, 50);
    assertEquals(100, ex.getUsed());
    assertEquals(50, ex.getLimit());
    assertTrue(ex.getMessage().contains("100"));
    assertTrue(ex.getMessage().contains("50"));
  }

  @Test
  void testSimulatorQuotaExceededException_WithMessage() {
    SimulatorQuotaExceededException ex =
        new SimulatorQuotaExceededException("Custom quota message");
    assertEquals("Custom quota message", ex.getMessage());
    assertEquals(-1, ex.getUsed());
    assertEquals(-1, ex.getLimit());
  }

  @Test
  void testSimulatorDeploymentException() {
    SimulatorDeploymentException ex = new SimulatorDeploymentException("Deployment failed");
    assertTrue(ex instanceof SimulatorException);
    assertEquals("Deployment failed", ex.getMessage());
  }

  @Test
  void testSimulatorDeploymentException_WithCause() {
    Throwable cause = new IllegalStateException("Bad state");
    SimulatorDeploymentException ex = new SimulatorDeploymentException("Deployment failed", cause);
    assertEquals(cause, ex.getCause());
  }

  @Test
  void testAllSimulatorExceptionsAreRuntime() {
    assertTrue(new SimulatorException("test") instanceof RuntimeException);
    assertTrue(new SimulatorResourceNotFoundException("test") instanceof RuntimeException);
    assertTrue(new SimulatorSessionException("test") instanceof RuntimeException);
    assertTrue(new SimulatorConflictException("test") instanceof RuntimeException);
    assertTrue(new SimulatorQuotaExceededException(1, 2) instanceof RuntimeException);
    assertTrue(new SimulatorDeploymentException("test") instanceof RuntimeException);
  }

  @Test
  void testExceptionChaining_PreservesStack() {
    try {
      throw new SimulatorException("Level 1", new IllegalArgumentException("Level 2"));
    } catch (SimulatorException ex) {
      assertEquals("Level 1", ex.getMessage());
      assertNotNull(ex.getCause());
      assertEquals("Level 2", ex.getCause().getMessage());
      assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
  }
}
