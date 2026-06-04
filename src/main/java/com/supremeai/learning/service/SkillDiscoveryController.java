package com.supremeai.learning.controller;

import com.supremeai.learning.service.AutonomousSkillDiscoveryService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/admin/skills")
public class SkillDiscoveryController {

  @Autowired private AutonomousSkillDiscoveryService discoveryService;

  /**
   * On-Demand Learning Endpoint for Admin Dashboard. Admin can pass a URL, and the system will
   * scrape it, extract the skill, and push it to the MCP Marketplace Pending Queue.
   */
  @PostMapping("/learn-on-demand")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<?> triggerOnDemandLearning(@RequestBody Map<String, String> request) {
    String url = request.get("url");
    if (url == null || url.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
    }

    // Run asynchronously so it doesn't block the admin's UI/HTTP response
    Mono.fromRunnable(() -> discoveryService.learnSkillOnDemand(url))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

    return ResponseEntity.ok(
        Map.of(
            "message",
            "On-demand learning triggered successfully. The skill will appear in the 'Pending Approvals' queue once processed.",
            "target_url",
            url));
  }
}
