package com.supremeai.controller;

import com.supremeai.service.CostTransparencyReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/cost")
public class CostTransparencyController {
    public CostTransparencyController(CostTransparencyReportService costService) {
        this.costService = costService;
    }



    @GetMapping("/realtime")
    public Mono<Map<String, Object>> getRealtimeCostReport(@RequestParam(defaultValue = "user_001") String userId) {
        return costService.generateCostReport(userId);
    }
}
