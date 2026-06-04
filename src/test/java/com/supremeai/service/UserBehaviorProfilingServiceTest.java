package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserBehaviorProfilingServiceTest {
  private UserBehaviorProfilingService service;

  @BeforeEach
  void setUp() {
    service = new UserBehaviorProfilingService();
  }

  @Test
  void trackAction_shouldRecordActionForUser() {
    String userId = "user-1";
    service.trackAction(userId, "login", Map.of("ip", "1.2.3.4"));
    Map<String, Object> profile = service.getProfileStats(userId);
    assertNotNull(profile);
    assertTrue((Boolean) profile.getOrDefault("exists", false));
    assertEquals(1, profile.get("totalActions"));
  }

  @Test
  void getSuggestions_shouldReturnDefaultWhenNoProfile() {
    List<UserBehaviorProfilingService.SmartSuggestion> suggestions =
        service.getSuggestions("unknown-user", 5);
    assertNotNull(suggestions);
    assertFalse(suggestions.isEmpty());
  }

  @Test
  void getProfileStats_shouldReturnEmptyForUnknownUser() {
    Map<String, Object> stats = service.getProfileStats("nonexistent");
    assertNotNull(stats);
    assertEquals(false, stats.get("exists"));
  }
}
