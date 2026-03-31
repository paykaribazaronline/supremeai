package org.example.controller;

import org.example.model.CloudDeploymentConfig;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for deployment management
 * Provides endpoints for cloud deployments and CI/CD pipelines
 */
@RestController
@RequestMapping("/api/v1/deployment")
public class DeploymentController {
    
    @Autowired
    private DockerImageBuilder dockerImageBuilder;
    
    @Autowired
    private KubernetesOrchestrator k8sOrchestrator;
    
    @Autowired
    private AWSCloudFormationTemplate cfnService;
    
    @Autowired
    private CICDPipelineService cicdService;
    
    /**
     * POST /configure
     * Create a deployment configuration
     */
    @PostMapping("/configure")
    public ResponseEntity<Map<String, Object>> createDeploymentConfig(
            @RequestBody CloudDeploymentConfig config) {
        
        // Validate configuration
        List<String> validationErrors = config.validate();
        if (!validationErrors.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "VALIDATION_FAILED");
            errorResponse.put("errors", validationErrors);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "CONFIGURED");
        response.put("deploymentId", config.getDeploymentId());
        response.put("cloudProvider", config.getCloudProvider().name());
        response.put("environment", config.getEnvironment().name());
        response.put("strategy", config.getStrategy().name());
        response.put("instanceCount", config.getInstanceCount());
        response.put("minReplicas", config.getMinReplicas());
        response.put("maxReplicas", config.getMaxReplicas());
        response.put("environmentOverrides", config.getEnvironmentOverrides());
        response.put("manifest", config.toManifestMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /docker/build
     * Build Docker image
     */
    @PostMapping("/docker/build")
    public ResponseEntity<Map<String, Object>> buildDockerImage(
            @RequestBody CloudDeploymentConfig config) {
        
        DockerImageBuilder.BuildResult result = dockerImageBuilder.buildImage(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildResult", result.toMap());
        response.put("dockerfile", result.dockerfile);
        if (result.securityScan != null) {
            response.put("securityScan", result.securityScan.toMap());
        }
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /docker/push
     * Push Docker image to registry
     */
    @PostMapping("/docker/push")
    public ResponseEntity<Map<String, Object>> pushDockerImage(
            @RequestBody DockerPushRequest request) {
        
        DockerImageBuilder.PushResult result = dockerImageBuilder.pushImageToRegistry(
            request.imageName, request.registry, request.tag);
        
        Map<String, Object> response = new HashMap<>();
        response.put("pushResult", result.toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /kubernetes/deploy
     * Deploy to Kubernetes
     */
    @PostMapping("/kubernetes/deploy")
    public ResponseEntity<Map<String, Object>> deployToKubernetes(
            @RequestBody CloudDeploymentConfig config) {
        
        KubernetesOrchestrator.DeploymentResult result = k8sOrchestrator.deploy(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("deployment", result.toMap());
        response.put("manifests", new HashMap<String, Object>() {{
            put("deployment", result.deployment);
            put("service", result.service);
            put("hpa", result.hpa);
            put("serviceAccount", result.serviceAccount);
        }});
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /kubernetes/update
     * Perform rolling update on Kubernetes
     */
    @PostMapping("/kubernetes/update")
    public ResponseEntity<Map<String, Object>> rollingUpdate(
            @RequestBody RollingUpdateRequest request) {
        
        KubernetesOrchestrator.RollingUpdateResult result = 
            k8sOrchestrator.performRollingUpdate(request.deploymentName, request.newImageTag);
        
        Map<String, Object> response = new HashMap<>();
        response.put("updateResult", result.toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /cloudformation/deploy
     * Deploy using CloudFormation
     */
    @PostMapping("/cloudformation/deploy")
    public ResponseEntity<Map<String, Object>> deployWithCloudFormation(
            @RequestBody CloudFormationDeployRequest request,
            @RequestParam(required = false) String templateType) {
        
        String template;
        if ("ec2".equals(templateType)) {
            template = cfnService.generateEC2Template(request.config);
        } else if ("rds".equals(templateType)) {
            template = cfnService.generateRDSTemplate(request.config);
        } else {
            template = cfnService.generateECSTemplate(request.config);
        }
        
        AWSCloudFormationTemplate.CloudFormationDeploymentResult result =
            cfnService.deployTemplate(request.stackName, template, request.config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("deploymentResult", result.toMap());
        response.put("template", template);
        response.put("templateType", templateType != null ? templateType : "ecs");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /pipeline/create
     * Create a CI/CD pipeline
     */
    @PostMapping("/pipeline/create")
    public ResponseEntity<Map<String, Object>> createPipeline(
            @RequestBody CICDPipelineService.PipelineConfig config) {
        
        CICDPipelineService.PipelineExecution execution = cicdService.createPipeline(config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("pipelineId", execution.pipelineId);
        response.put("pipelineName", execution.pipelineName);
        response.put("status", execution.status);
        response.put("gitBranch", execution.gitBranch);
        response.put("createdAt", execution.startTime);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /pipeline/execute
     * Execute a CI/CD pipeline
     */
    @PostMapping("/pipeline/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(
            @RequestBody ExecutePipelineRequest request) {
        
        CICDPipelineService.PipelineConfig config = new CICDPipelineService.PipelineConfig();
        config.pipelineName = request.pipelineName;
        config.gitRepo = request.gitRepo;
        config.gitBranch = request.gitBranch;
        config.deploymentTarget = request.deploymentTarget;
        config.failOnSecurityIssues = request.failOnSecurityIssues;
        
        CICDPipelineService.PipelineExecution execution = cicdService.createPipeline(config);
        CICDPipelineService.PipelineExecutionResult result = cicdService.executePipeline(execution, config);
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionResult", result.toMap());
        response.put("overallStatus", result.overallStatus);
        response.put("stageCount", result.stages.size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /pipeline/webhook
     * Handle Git webhook for auto-triggered pipelines
     */
    @PostMapping("/pipeline/webhook")
    public ResponseEntity<Map<String, Object>> handleGitWebhook(
            @RequestBody CICDPipelineService.WebhookPayload payload) {
        
        CICDPipelineService.WebhookResult result = cicdService.handleGitWebhook(payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("webhookResult", result.toMap());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /pipeline/{pipelineId}
     * Get pipeline execution status
     */
    @GetMapping("/pipeline/{pipelineId}")
    public ResponseEntity<Map<String, Object>> getPipelineStatus(
            @PathVariable String pipelineId) {
        
        CICDPipelineService.PipelineExecution execution = cicdService.getPipelineStatus(pipelineId);
        
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("pipelineId", execution.pipelineId);
        response.put("pipelineName", execution.pipelineName);
        response.put("status", execution.status);
        response.put("gitBranch", execution.gitBranch);
        response.put("startTime", execution.startTime);
        response.put("endTime", execution.endTime);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /pipeline/history
     * Get all pipeline executions
     */
    @GetMapping("/pipeline/history")
    public ResponseEntity<Map<String, Object>> getPipelineHistory() {
        
        List<CICDPipelineService.PipelineExecution> executions = 
            cicdService.getAllPipelineExecutions();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalPipelines", executions.size());
        response.put("pipelines", executions.stream()
            .map(e -> new HashMap<String, Object>() {{
                put("pipelineId", e.pipelineId);
                put("pipelineName", e.pipelineName);
                put("status", e.status);
                put("gitBranch", e.gitBranch);
                put("startTime", e.startTime);
            }})
            .toList());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /health/deployment
     * Health check for deployment system
     */
    @GetMapping("/health/deployment")
    public ResponseEntity<Map<String, Object>> deploymentHealthCheck() {
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "HEALTHY");
        response.put("components", new HashMap<String, Object>() {{
            put("dockerBuilder", "READY");
            put("kubernetesOrchestrator", "READY");
            put("cloudFormation", "READY");
            put("cicdPipeline", "READY");
        }});
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    
    public static class DockerPushRequest {
        public String imageName;
        public String registry;
        public String tag;
    }
    
    public static class RollingUpdateRequest {
        public String deploymentName;
        public String newImageTag;
    }
    
    public static class CloudFormationDeployRequest {
        public String stackName;
        public CloudDeploymentConfig config;
    }
    
    public static class ExecutePipelineRequest {
        public String pipelineName;
        public String gitRepo;
        public String gitBranch = "main";
        public String deploymentTarget;
        public boolean failOnSecurityIssues = false;
    }
}
