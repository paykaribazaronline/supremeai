package com.supremeai.api;

import com.supremeai.service.CodeGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/generate")
public class CodeGenerationController {

    @Autowired
    private CodeGenerationService codeGenerationService;

    /**
     * POST /api/generate/app
     * Generate a skeleton application from basic spec (legacy/simple).
     * Body: {"name": "MyApp", "description": "..."}
     */
    @PostMapping("/app")
    public Mono<ResponseEntity<Map<String, Object>>> generateApp(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = codeGenerationService.generate(request);
        return Mono.just(ResponseEntity.ok(result));
    }

    /**
     * POST /api/generate/from-orchestration
     * Generate app using orchestration context (decisions from consensus voting).
     * Body: {"decisions": {"database":"PostgreSQL","architecture":"monolith",...}}
     */
    @PostMapping("/from-orchestration")
    public Mono<ResponseEntity<Map<String, Object>>> generateFromOrchestration(@RequestBody Map<String, Object> request) {
        Map<String, String> decisions = (Map<String, String>) request.get("decisions");
        if (decisions == null) {
            decisions = Map.of();
        }
        Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
        return Mono.just(ResponseEntity.ok(result));
    }

    /**
     * GET /api/generate/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "status", "UP",
            "service", "CodeGenerationService"
        )));
    }
}
