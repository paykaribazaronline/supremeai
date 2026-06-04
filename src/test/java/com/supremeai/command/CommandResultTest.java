package com.supremeai.command;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommandResultTest {

  @Test
  @DisplayName("Should create success result correctly")
  void testSuccessResult() {
    CommandResult result = CommandResult.success("test-command", Map.of("key", "value"));

    assertEquals("test-command", result.getCommandName());
    assertEquals(CommandResult.Status.SUCCESS, result.getStatus());
    assertEquals(Map.of("key", "value"), result.getData());
    assertTrue(result.isSuccess());
    assertFalse(result.isFailed());
  }

  @Test
  @DisplayName("Should create error result correctly")
  void testErrorResult() {
    CommandResult result = CommandResult.error("test-command", "ERROR_CODE", "Error message");

    assertEquals("test-command", result.getCommandName());
    assertEquals(CommandResult.Status.FAILED, result.getStatus());
    assertEquals("ERROR_CODE", result.getErrorCode());
    assertEquals("Error message", result.getErrorMessage());
    assertTrue(result.isFailed());
    assertFalse(result.isSuccess());
  }

  @Test
  @DisplayName("Should create pending result correctly")
  void testPendingResult() {
    CommandResult result = CommandResult.pending("test-command", "job-123");

    assertEquals("test-command", result.getCommandName());
    assertEquals(CommandResult.Status.PENDING, result.getStatus());
    assertEquals("job-123", result.getJobId());
    assertFalse(result.isSuccess());
  }

  @Test
  @DisplayName("Should create running result correctly")
  void testRunningResult() {
    CommandResult result = CommandResult.running("test-command", "job-456");

    assertEquals("test-command", result.getCommandName());
    assertEquals(CommandResult.Status.RUNNING, result.getStatus());
    assertEquals("job-456", result.getJobId());
    assertTrue(result.isRunning());
  }

  @Test
  @DisplayName("Should set execution time correctly")
  void testSetExecutionTime() {
    CommandResult result = CommandResult.success("test-command", null);

    result.setExecutionTime(1500L);

    assertEquals(1500L, result.getExecutionTimeMs());
  }

  @Test
  @DisplayName("Should set executed by correctly")
  void testSetExecutedBy() {
    CommandResult result = CommandResult.success("test-command", null);

    result.setExecutedBy("test-user");

    assertEquals("test-user", result.getExecutedBy());
  }

  @Test
  @DisplayName("Should check isRunning correctly for pending status")
  void testIsRunningForPending() {
    CommandResult result = CommandResult.pending("test-command", "job-123");

    assertTrue(result.isRunning());
  }

  @Test
  @DisplayName("Should check isRunning correctly for running status")
  void testIsRunningForRunning() {
    CommandResult result = CommandResult.running("test-command", "job-123");

    assertTrue(result.isRunning());
  }

  @Test
  @DisplayName("Should check isRunning correctly for success status")
  void testIsRunningForSuccess() {
    CommandResult result = CommandResult.success("test-command", null);

    assertFalse(result.isRunning());
  }

  @Test
  @DisplayName("Should check isRunning correctly for failed status")
  void testIsRunningForFailed() {
    CommandResult result = CommandResult.error("test-command", "ERROR", "msg");

    assertFalse(result.isRunning());
  }

  @Test
  @DisplayName("Should create default constructor result")
  void testDefaultConstructor() {
    CommandResult result = new CommandResult("test-command");

    assertEquals("test-command", result.getCommandName());
    assertEquals(CommandResult.Status.PENDING, result.getStatus());
    assertNotNull(result.getExecutedAt());
    assertEquals(0, result.getExecutionTimeMs());
  }

  @Test
  @DisplayName("Should return correct toString representation")
  void testToString() {
    CommandResult result = CommandResult.success("test-command", null);
    result.setExecutedBy("test-user");
    result.setExecutionTime(100L);

    String str = result.toString();

    assertTrue(str.contains("test-command"));
    assertTrue(str.contains("test-user"));
    assertTrue(str.contains("SUCCESS"));
  }

  @Test
  @DisplayName("Should handle null data in success result")
  void testSuccessWithNullData() {
    CommandResult result = CommandResult.success("test-command", null);

    assertNull(result.getData());
    assertTrue(result.isSuccess());
  }

  @Test
  @DisplayName("Should handle different status values")
  void testStatusValues() {
    assertEquals(CommandResult.Status.SUCCESS, CommandResult.Status.valueOf("SUCCESS"));
    assertEquals(CommandResult.Status.PENDING, CommandResult.Status.valueOf("PENDING"));
    assertEquals(CommandResult.Status.RUNNING, CommandResult.Status.valueOf("RUNNING"));
    assertEquals(CommandResult.Status.FAILED, CommandResult.Status.valueOf("FAILED"));
    assertEquals(CommandResult.Status.TIMEOUT, CommandResult.Status.valueOf("TIMEOUT"));
    assertEquals(CommandResult.Status.CANCELLED, CommandResult.Status.valueOf("CANCELLED"));
  }
}
