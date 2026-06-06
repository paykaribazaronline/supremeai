package com.supremeai.controller;

import com.supremeai.service.MemoryService;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

  private final MemoryService memoryService;

  @PostMapping("/ingest")
  public ResponseEntity<Map<String, String>> ingestKnowledge(@RequestBody IngestRequest request) {
    memoryService.ingestKnowledge(
        request.getAgentId(), request.getContentId(), request.getContent());
    return ResponseEntity.ok(
        Map.of("status", "success", "message", "Knowledge ingested successfully."));
  }

  @PostMapping("/retrieve")
  public ResponseEntity<Map<String, String>> retrieveContext(@RequestBody RetrieveRequest request) {
    String context = memoryService.retrieveRelevantContext(request.getQuery(), request.getTopK());
    return ResponseEntity.ok(Map.of("context", context));
  }

  @Data
  public static class IngestRequest {
    private String agentId;
    private String contentId;
    private String content;
  }

  @Data
  public static class RetrieveRequest {
    private String query;
    private int topK = 3; // Default to top 3
  }
}
