package com.supremeai.controller;

import com.supremeai.service.CostTransparencyReportService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cost")
public class CostTransparencyController {

  @Autowired private CostTransparencyReportService costService;

  @GetMapping("/realtime")
  public Mono<Map<String, Object>> getRealtimeCostReport(
      @RequestParam(defaultValue = "user_001") String userId) {
    return costService.generateCostReport(userId);
  }
}
