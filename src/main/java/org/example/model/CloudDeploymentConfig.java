package org.example.model;

import java.util.*;

/**
 * Configuration model for cloud deployments
 * Supports multiple cloud providers and environments
 */
public class CloudDeploymentConfig {
    
    // Deployment target
    public enum CloudProvider { AWS, GCP, AZURE, KUBERNETES }
    public enum Environment { DEV, STAGING, PRODUCTION }
    public enum DeploymentStrategy { BLUE_GREEN, CANARY, ROLLING_UPDATE, SHADOW }
    
    private String deploymentId;
    private CloudProvider cloudProvider;
    private Environment environment;
    private DeploymentStrategy strategy;
    private long createdAt;
    
    // Cloud-specific configuration
    private AwsConfig awsConfig;
    private KubernetesConfig k8sConfig;
    private DockerConfig dockerConfig;
    private HealthCheckConfig healthCheckConfig;
    
    // Deployment parameters
    private int instanceCount = 1;
    private String instanceType = "t3.medium";
    private int minReplicas = 1;
    private int maxReplicas = 5;
    private double cpuLimit = 1.0;
    private double memoryLimit = 1024;
    private String imageTag = "latest";
    
    // Networking
    private String vpcId;
    private List<String> subnetIds;
    private List<String> securityGroupIds;
    private boolean publicIp = false;
    
    // Notifications & Logging
    private boolean enableLogging = true;
    private boolean enableMonitoring = true;
    private String logGroupName;
    private String metricsNamespace;
    
    public CloudDeploymentConfig() {
        this.deploymentId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.awsConfig = new AwsConfig();
        this.k8sConfig = new KubernetesConfig();
        this.dockerConfig = new DockerConfig();
        this.healthCheckConfig = new HealthCheckConfig();
        this.subnetIds = new ArrayList<>();
        this.securityGroupIds = new ArrayList<>();
    }
    
    /**
     * AWS-specific configuration
     */
    public static class AwsConfig {
        public String region = "us-east-1";
        public String accountId;
        public String roleArn;
        public String vpcCidr = "10.0.0.0/16";
        public String subnetCidr = "10.0.1.0/24";
        public boolean useNetworkLoadBalancer = false;
        public int healthCheckInterval = 30;
        public int healthCheckTimeout = 5;
        public int healthyThreshold = 2;
        public int unhealthyThreshold = 3;
        public Map<String, String> tags = new HashMap<>();
    }
    
    /**
     * Kubernetes-specific configuration
     */
    public static class KubernetesConfig {
        public String clusterName = "supremeai-cluster";
        public String namespace = "default";
        public String kubeConfigPath = "~/.kube/config";
        public boolean useHelm = true;
        public String helmChartPath = "./helm/supremeai";
        public int replicas = 3;
        public String imagePullPolicy = "IfNotPresent";
        public List<String> nodeSelectors = new ArrayList<>();
        public Map<String, String> labels = new HashMap<>();
        public Map<String, String> annotations = new HashMap<>();
    }
    
    /**
     * Docker image configuration
     */
    public static class DockerConfig {
        public String registry = "docker.io";
        public String imageName = "supremeai";
        public String imageTag = "latest";
        public String dockerfile = "Dockerfile";
        public boolean buildOnDeploy = true;
        public boolean pushToRegistry = true;
        public boolean scanForVulnerabilities = true;
        public Map<String, String> buildArgs = new HashMap<>();
    }
    
    /**
     * Health check configuration
     */
    public static class HealthCheckConfig {
        public boolean enabled = true;
        public String endpoint = "/health";
        public int intervalSeconds = 10;
        public int timeoutSeconds = 3;
        public int successThreshold = 1;
        public int failureThreshold = 3;
        public int initialDelaySeconds = 30;
        public Map<String, String> httpHeaders = new HashMap<>();
    }
    
    /**
     * Validate configuration completeness
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (cloudProvider == null) {
            errors.add("Cloud provider must be specified");
        }
        
        if (environment == null) {
            errors.add("Environment must be specified");
        }
        
        if (strategy == null) {
            errors.add("Deployment strategy must be specified");
        }
        
        if (instanceCount <= 0) {
            errors.add("Instance count must be > 0");
        }
        
        if (minReplicas < 1 || maxReplicas < minReplicas) {
            errors.add("Invalid replica configuration");
        }
        
        if (cpuLimit <= 0 || memoryLimit <= 0) {
            errors.add("CPU and memory limits must be positive");
        }
        
        if (dockerConfig == null || dockerConfig.imageName == null) {
            errors.add("Docker image name is required");
        }
        
        return errors;
    }
    
    /**
     * Convert to deployment manifest
     */
    public Map<String, Object> toManifestMap() {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("deploymentId", deploymentId);
        manifest.put("cloudProvider", cloudProvider.name());
        manifest.put("environment", environment.name());
        manifest.put("strategy", strategy.name());
        manifest.put("instanceCount", instanceCount);
        manifest.put("cpuLimit", cpuLimit);
        manifest.put("memoryLimit", memoryLimit);
        manifest.put("imageTag", imageTag);
        manifest.put("awsConfig", awsConfig);
        manifest.put("k8sConfig", k8sConfig);
        manifest.put("dockerConfig", dockerConfig);
        manifest.put("healthCheckConfig", healthCheckConfig);
        return manifest;
    }
    
    /**
     * Get environment-specific overrides
     */
    public Map<String, Object> getEnvironmentOverrides() {
        Map<String, Object> overrides = new HashMap<>();
        
        switch (environment) {
            case DEV:
                overrides.put("replicas", 1);
                overrides.put("cpuLimit", 0.5);
                overrides.put("memoryLimit", 512);
                overrides.put("imagePullPolicy", "Always");
                overrides.put("imageTag", "dev-latest");
                break;
            case STAGING:
                overrides.put("replicas", 2);
                overrides.put("cpuLimit", 1.0);
                overrides.put("memoryLimit", 1024);
                overrides.put("imagePullPolicy", "IfNotPresent");
                overrides.put("imageTag", "staging");
                break;
            case PRODUCTION:
                overrides.put("replicas", 3);
                overrides.put("cpuLimit", 2.0);
                overrides.put("memoryLimit", 2048);
                overrides.put("imagePullPolicy", "IfNotPresent");
                overrides.put("imageTag", "v" + System.currentTimeMillis());
                break;
        }
        
        return overrides;
    }
    
    // Getters and Setters
    public String getDeploymentId() { return deploymentId; }
    public CloudProvider getCloudProvider() { return cloudProvider; }
    public void setCloudProvider(CloudProvider cloudProvider) { this.cloudProvider = cloudProvider; }
    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }
    public DeploymentStrategy getStrategy() { return strategy; }
    public void setStrategy(DeploymentStrategy strategy) { this.strategy = strategy; }
    public String getImageTag() { return imageTag; }
    public void setImageTag(String imageTag) { this.imageTag = imageTag; }
    public int getInstanceCount() { return instanceCount; }
    public void setInstanceCount(int instanceCount) { this.instanceCount = instanceCount; }
    public String getInstanceType() { return instanceType; }
    public void setInstanceType(String instanceType) { this.instanceType = instanceType; }
    public int getMinReplicas() { return minReplicas; }
    public void setMinReplicas(int minReplicas) { this.minReplicas = minReplicas; }
    public int getMaxReplicas() { return maxReplicas; }
    public void setMaxReplicas(int maxReplicas) { this.maxReplicas = maxReplicas; }
    public double getCpuLimit() { return cpuLimit; }
    public void setCpuLimit(double cpuLimit) { this.cpuLimit = cpuLimit; }
    public double getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(double memoryLimit) { this.memoryLimit = memoryLimit; }
    public AwsConfig getAwsConfig() { return awsConfig; }
    public KubernetesConfig getK8sConfig() { return k8sConfig; }
    public DockerConfig getDockerConfig() { return dockerConfig; }
    public HealthCheckConfig getHealthCheckConfig() { return healthCheckConfig; }
    public boolean isEnableLogging() { return enableLogging; }
    public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
    public boolean isEnableMonitoring() { return enableMonitoring; }
    public void setEnableMonitoring(boolean enableMonitoring) { this.enableMonitoring = enableMonitoring; }
}
