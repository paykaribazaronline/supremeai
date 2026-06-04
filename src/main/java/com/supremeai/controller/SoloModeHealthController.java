package com.supremeai.controller;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/solo-mode")
public class SoloModeHealthController {

  private final ThirdOpinionOrchestrator thirdOpinionOrchestrator;

  public SoloModeHealthController(ThirdOpinionOrchestrator thirdOpinionOrchestrator) {
    this.thirdOpinionOrchestrator = thirdOpinionOrchestrator;
  }

  /**
   * GET /api/health/solo-mode Returns whether the system is in solo mode — i.e. no active AI
   * providers are configured in Firestore and the system is running entirely on core_knowledge.json
   * / autonomous_seed_knowledge.json.
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> getSoloModeHealth() {
    boolean inSoloMode = thirdOpinionOrchestrator.getSoloMode();
    Map<String, Object> health = new HashMap<>();
    health.put("status", inSoloMode ? "SOLO_MODE_ACTIVE" : "FULL_AI_MODE");
    health.put("soloMode", inSoloMode);
    health.put("timestamp", System.currentTimeMillis());
    if (inSoloMode) {
      health.put(
          "message",
          "System is operating in solo mode — zero active AI providers configured in Firestore. "
              + "All requests use local knowledge base (core_knowledge.json + autonomous_seed_knowledge.json). "
              + "Add provider entries to Firestore api_providers to restore full AI capability.");
    } else {
      health.put("message", "Active AI providers detected. System is operating in full AI mode.");
    }
    return ResponseEntity.ok(health);
  }
}
