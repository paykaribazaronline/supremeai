package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ServerStatusControllerTest {

  private final ServerStatusController controller = new ServerStatusController();

  @Test
  void getStatus_shouldReturnUpStatus() {
    Map<String, String> status = controller.getStatus();

    assertNotNull(status);
    assertEquals("UP", status.get("status"));
    assertEquals("SupremeAI Backend is running", status.get("message"));
    assertEquals("6.0.0", status.get("version"));
  }

  @Test
  void getStatus_shouldReturnAllRequiredFields() {
    Map<String, String> status = controller.getStatus();

    assertTrue(status.containsKey("status"));
    assertTrue(status.containsKey("message"));
    assertTrue(status.containsKey("version"));
    assertEquals(3, status.size());
  }
}
