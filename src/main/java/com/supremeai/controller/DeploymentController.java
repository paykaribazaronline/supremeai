package com.supremeai.controller;

import com.supremeai.deployment.AutoDeploymentOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deployment")
public class DeploymentController {

    private final AutoDeploymentOrchestrator deploymentOrchestrator;

    public DeploymentController(AutoDeploymentOrchestrator deploymentOrchestrator) {
        this.deploymentOrchestrator = deploymentOrchestrator;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> deploy(@RequestBody Map<String, String> artifacts) {
        Map<String, String> result = deploymentOrchestrator.deploy(artifacts);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ready",
                "message", "Deployment orchestrator is active"
        ));
    }
}
