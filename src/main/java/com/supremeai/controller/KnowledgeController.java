package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.KnowledgeService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

  @Autowired private KnowledgeService knowledgeService;

  @GetMapping("/recent-scraped")
  public Mono<List<SystemLearning>> getRecentScrapedLearnings(
      @RequestParam(defaultValue = "3") int limit) {
    return knowledgeService.getRecentScrapedLearnings(limit);
  }

  /** DELETE /api/knowledge/clear-scraped Purge all web-scraped knowledge entries (Admin only). */
  @DeleteMapping("/clear-scraped")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<Map<String, String>>> clearScrapedLearnings() {
    return knowledgeService
        .clearOldScrapedLearnings()
        .thenReturn(
            ResponseEntity.ok(
                Map.of(
                    "status", "success",
                    "message",
                        "All web-scraped knowledge entries have been purged from Firestore.")));
  }

  /** Gracefully handle timeout exceptions specifically for scraping operations. */
  @ExceptionHandler(TimeoutException.class)
  public ResponseEntity<Map<String, Object>> handleScrapingTimeout(TimeoutException ex) {
    Map<String, Object> errorDetails = new HashMap<>();
    errorDetails.put("status", "error");
    errorDetails.put("type", "TIMEOUT");
    errorDetails.put(
        "message",
        "The scraping operation timed out. The system may still be processing data in the background.");
    errorDetails.put("timestamp", java.time.LocalDateTime.now().toString());
    errorDetails.put(
        "action_required", "Please verify the 'Recent Learnings' feed in a few moments.");

    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(errorDetails);
  }
}
