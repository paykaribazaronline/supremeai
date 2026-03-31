# Deployment Automation Guide

## Overview

The Deployment Automation system provides comprehensive support for managing application deployments across multiple environments, including Docker containerization, Kubernetes orchestration, and deployment lifecycle management. This system is designed for enterprise-grade deployment reliability, scalability, and auditability.

## Architecture

### Core Components

1. **DeploymentService** - Manages deployment lifecycle and versioning
2. **KubernetesService** - Orchestrates Kubernetes deployments and pod management
3. **DockerIntegrationService** - Handles Docker image building and registry operations
4. **CICDPipelineService** - Orchestrates multi-stage CI/CD pipeline execution

## DeploymentService

The DeploymentService manages the complete deployment lifecycle from creation through success or rollback.

### Key Features

- **Deployment Lifecycle Management**: PENDING → IN_PROGRESS → SUCCESS/FAILED/ROLLED_BACK
- **Version Management**: Application version registering and tracking
- **Event Tracking**: Comprehensive deployment event logging
- **Statistics**: Success rates and deployment metrics

### API Reference

#### Create Deployment
```java
DeploymentRecord createDeployment(String appName, String version, String environment)
```

**Parameters:**
- `appName`: Application name
- `version`: Application version (e.g., "1.0.0")
- `environment`: Target environment (e.g., "production", "staging")

**Returns:** `DeploymentRecord` with deployment ID and initial status PENDING

**Example:**
```java
DeploymentRecord deployment = deploymentService.createDeployment(
    "myapp",
    "2.0.0",
    "production"
);
```

#### Get Deployment
```java
DeploymentRecord getDeployment(String deploymentId)
```

**Returns:** Deployment record with current status and metadata

#### List Deployments
```java
Collection<DeploymentRecord> listDeployments()
```

**Returns:** All active deployments

#### List by Application
```java
List<DeploymentRecord> listDeploymentsByApplication(String appName)
```

**Returns:** All deployments for specific application

#### List by Environment
```java
List<DeploymentRecord> listDeploymentsByEnvironment(String environment)
```

**Returns:** All deployments in specific environment

#### Start Deployment
```java
void startDeployment(String deploymentId)
```

Transitions deployment from PENDING to IN_PROGRESS. Call this after creating deployment.

#### Complete Deployment
```java
void completeDeployment(String deploymentId)
```

Transitions deployment from IN_PROGRESS to SUCCESS.

#### Fail Deployment
```java
void failDeployment(String deploymentId, String reason)
```

Transitions deployment from IN_PROGRESS to FAILED with failure reason.

#### Rollback Deployment
```java
void rollbackDeployment(String deploymentId, String previousVersion)
```

Transitions to ROLLED_BACK status, restoring previous version.

### Deployment States

| Status | Description | Transitions |
|--------|-------------|-------------|
| PENDING | Deployment created, awaiting start | → IN_PROGRESS |
| IN_PROGRESS | Deployment actively running | → SUCCESS, FAILED |
| SUCCESS | Deployment completed successfully | (final) |
| FAILED | Deployment encountered error | None |
| ROLLED_BACK | Deployment rolled back to previous version | None |

### Version Management

#### Register Version
```java
ApplicationVersion registerVersion(String appName, String version, 
                                 String artifactUrl, String releaseNotes)
```

**Example:**
```java
ApplicationVersion v2 = deploymentService.registerVersion(
    "myapp",
    "2.0.0",
    "https://releases.example.com/myapp-2.0.0.jar",
    "Added new features X, Y, Z"
);
```

#### Get Latest Version
```java
ApplicationVersion getLatestVersion(String appName)
```

Returns most recent version registered for application.

#### List Application Versions
```java
List<ApplicationVersion> listVersionsForApplication(String appName)
```

Returns all versions sorted by release date (newest first).

### Event Tracking

#### Get Deployment Events
```java
List<DeploymentEvent> getDeploymentEvents(String deploymentId)
```

Returns all events for deployment in chronological order.

**Event Fields:**
- `eventType`: Type of event (CREATED, STARTED, COMPLETED, FAILED, ROLLED_BACK)
- `message`: Event description
- `timestamp`: When event occurred

### Statistics

#### Get Deployment Statistics
```java
Map<String, Object> getDeploymentStats()
```

**Returns:**
```json
{
  "totalDeployments": 150,
  "successfulDeployments": 142,
  "failedDeployments": 8,
  "successRate": "94.67%",
  "totalVersions": 25,
  "generatedAt": 1700000000000
}
```

## KubernetesService

Manages Kubernetes deployment, pod, and service orchestration.

### Key Features

- **Deployment Management**: Create, scale, and update K8s deployments
- **Pod Lifecycle**: Create pods, update status, track health
- **Service Management**: Create K8s services and manage endpoints
- **Cluster Health**: Real-time cluster health metrics

### API Reference

#### Create Deployment
```java
K8sDeployment createDeployment(String name, String namespace, 
                               String imageUrl, int replicas)
```

**Parameters:**
- `name`: Deployment name
- `namespace`: K8s namespace (e.g., "default", "production")
- `imageUrl`: Container image URL (e.g., "myapp:1.0.0")
- `replicas`: Desired replica count

**Example:**
```java
K8sDeployment deployment = kubernetesService.createDeployment(
    "myapp-deployment",
    "production",
    "docker.io/myapp:2.0.0",
    3
);
```

#### Scale Deployment
```java
void scaleDeployment(String deploymentId, int replicas)
```

Dynamically adjust replica count.

#### Update Deployment Image
```java
void updateDeploymentImage(String deploymentId, String newImageUrl)
```

Performs rolling update with new container image.

### Pod Management

#### Create Pod
```java
K8sPod createPod(String podName, String namespace, 
                 String deploymentId, String containerImage)
```

#### Update Pod Status
```java
void updatePodStatus(String podId, String status)
```

**Pod Status Lifecycle:** Pending → Running → Terminated

#### List Pods by Deployment
```java
List<K8sPod> listPodsByDeployment(String deploymentId)
```

### Service Management

#### Create Service
```java
K8sService createService(String serviceName, String namespace, 
                         int port, String selector)
```

**Parameters:**
- `serviceName`: Service name
- `namespace`: K8s namespace
- `port`: Service port
- `selector`: Label selector for pod routing (e.g., "app=myapp")

#### List Services
```java
Collection<K8sService> listServices()
```

### Cluster Health

#### Get Cluster Health
```java
Map<String, Object> getClusterHealth()
```

**Returns:**
```json
{
  "totalPods": 15,
  "runningPods": 13,
  "pendingPods": 2,
  "failedPods": 0,
  "healthPercent": "86.67%",
  "totalDeployments": 5,
  "totalServices": 3
}
```

## DockerIntegrationService

Manages Docker image building, tagging, and registry operations.

### Key Features

- **Image Building**: Build Docker images from Dockerfile
- **Image Tagging**: Create alternative tags for existing images
- **Registry Operations**: Push images to Docker registries
- **Image Validation**: Verify image integrity

### API Reference

#### Build Image
```java
DockerImage buildImage(String imageName, String tag, 
                      String dockerfilePath, String buildContext)
```

**Parameters:**
- `imageName`: Image name (e.g., "myapp")
- `tag`: Image tag (e.g., "1.0.0", "latest")
- `dockerfilePath`: Path to Dockerfile
- `buildContext`: Build context directory

**Example:**
```java
DockerImage image = dockerService.buildImage(
    "myapp",
    "2.0.0",
    "/src/Dockerfile",
    "/src"
);
```

#### Tag Image
```java
DockerImage tagImage(String sourceImageId, String newTag)
```

Creates new tag pointing to existing image (e.g., "latest" pointing to "2.0.0").

#### Push to Registry
```java
void pushImageToRegistry(String imageId, String registry, 
                        String username, String password)
```

**Parameters:**
- `registry`: Registry URL (e.g., "docker.io", "quay.io")
- `username`: Registry credentials username
- `password`: Registry credentials password

**Example:**
```java
dockerService.pushImageToRegistry(
    image.imageId,
    "docker.io",
    "username",
    "token"
);
```

#### Validate Image
```java
boolean validateImage(String imageId)
```

Checks image exists and is accessible.

### Build Job Management

#### Get Build Job
```java
BuildJob getBuildJob(String buildJobId)
```

Returns build job details including logs.

#### Get Build Logs
```java
List<String> getBuildLogs(String buildJobId)
```

Returns timestamped build logs.

**Example Log Output:**
```
[2024-01-15 10:30:45] Starting Docker build for myapp:2.0.0
[2024-01-15 10:30:46] Dockerfile: /src/Dockerfile
[2024-01-15 10:30:47] Build context: /src
[2024-01-15 10:30:48] Building image layers...
[2024-01-15 10:30:50] Layer 1/5: base image pulled
[2024-01-15 10:30:53] Layer 2/5: installing dependencies
[2024-01-15 10:30:58] Layer 3/5: copying application code
[2024-01-15 10:31:02] Layer 4/5: setting up entrypoint
[2024-01-15 10:31:05] Layer 5/5: finalizing image
[2024-01-15 10:31:10] Build completed successfully
```

### Image Cleanup

#### Cleanup Old Images
```java
void cleanupOldImages(String imageName, int keepCount)
```

Removes old versions, keeping only most recent N.

### Image Statistics

#### Get Image Stats
```java
Map<String, Object> getImageStats()
```

**Returns:**
```json
{
  "totalImages": 25,
  "readyImages": 20,
  "publishedImages": 18,
  "totalSizeBytes": 42949672960,
  "totalBuildJobs": 45,
  "averageImageSize": 1717986918,
  "generatedAt": 1700000000000
}
```

## CICDPipelineService

Manages multi-stage CI/CD pipeline orchestration.

### Architecture

The CI/CD Pipeline consists of 6 standard stages executed sequentially:

1. **Checkout** - Clone repository and check out branch
2. **Build** - Compile and package application
3. **Test** - Execute unit and integration tests
4. **Security Scan** - Run security vulnerability scanning
5. **Docker** - Build Docker image and push to registry
6. **Deployment** - Deploy to target environment

### API Reference

#### Create Pipeline
```java
PipelineExecution createPipeline(PipelineConfig config)
```

**PipelineConfig:**
```java
public static class PipelineConfig {
    public String pipelineName;
    public String gitRepo;
    public String gitBranch = "main";
    public String buildCommand = "./gradlew clean build";
    public String testCommand = "./gradlew test";
    public String deploymentTarget;
    public boolean failOnSecurityIssues = false;
    public Map<String, String> environment = new HashMap<>();
}
```

#### Execute Pipeline
```java
PipelineExecutionResult executePipeline(PipelineExecution execution, 
                                       PipelineConfig config)
```

Executes all pipeline stages sequentially. Returns result with per-stage details.

### Pipeline Execution Result

```json
{
  "pipelineId": "uuid-123",
  "status": "SUCCESS",
  "duration": 145000,
  "stages": [
    {
      "stage": "CHECKOUT",
      "success": true,
      "duration": 2000
    },
    {
      "stage": "BUILD",
      "success": true,
      "duration": 25000
    },
    {
      "stage": "TEST",
      "success": true,
      "duration": 15000,
      "tests": 142,
      "passed": 142,
      "failed": 0
    },
    {
      "stage": "SECURITY_SCAN",
      "success": true,
      "duration": 8000
    },
    {
      "stage": "DOCKER",
      "success": true,
      "duration": 18000
    },
    {
      "stage": "DEPLOYMENT",
      "success": true,
      "duration": 12000
    }
  ]
}
```

### Webhook Handling

#### Handle Git Webhook
```java
WebhookResult handleGitWebhook(WebhookPayload payload)
```

Auto-triggers pipeline on push events to monitored branch.

## Best Practices

### Deployment Strategy

1. **Pre-Deployment Validation**
   - Register version before deployment
   - Validate Docker image before deployment
   - Check deployment target availability

2. **Health Checks**
   - Monitor pod health during deployment
   - Check cluster health after scaling
   - Validate image accessibility before push

3. **Rollback Planning**
   - Always maintain deployment history
   - Register versions before deployment
   - Test rollback procedures

### Performance Optimization

1. **Image Size Management**
   - Cleanup old images regularly
   - Use multi-stage Docker builds
   - Monitor total image storage

2. **Resource Efficiency**
   - Use appropriate replica counts
   - Monitor pod health percentages
   - Scale based on cluster health

3. **Pipeline Efficiency**
   - Parallelize independent stages where possible
   - Cache Docker layers
   - Use incremental builds

## Configuration

### Environment Variables

```properties
# Deployment
deployment.max-concurrent=10
deployment.timeout-seconds=3600
deployment.retention-days=30

# Kubernetes
kubernetes.max-pod-retries=3
kubernetes.health-check-interval-ms=5000
kubernetes.scaling-cooldown-ms=30000

# Docker
docker.registry-timeout-seconds=300
docker.build-timeout-minutes=30
docker.cleanup-schedule=0 2 * * *

# CI/CD
cicd.concurrent-pipelines=5
cicd.stage-timeout-minutes=60
cicd.artifact-retention-days=7
```

## Monitoring & Observability

### Key Metrics

1. **Deployment Metrics**
   - Deployment success rate
   - Average deployment duration
   - Failed deployment count
   - Rollback frequency

2. **Kubernetes Metrics**
   - Pod health percentage
   - Pod restart count
   - Scaling latency
   - Pod age distribution

3. **Docker Metrics**
   - Image build success rate
   - Image size trends
   - Registry push latency
   - Image cleanup operations

4. **Pipeline Metrics**
   - Pipeline success rate
   - Stage duration distribution
   - Test pass rate
   - Security scan findings

### Alerting

Configure alerts for:
- Deployment failures
- Pod health below 80%
- Image build failures
- Pipeline execution timeouts
- Registry push errors

## Troubleshooting

### Deployment Issues

**Problem: Deployment stuck in IN_PROGRESS**
- Check pod health status
- Verify resource availability
- Review deployment logs
- Consider rollback

**Problem: Version not found**
- Verify version was registered
- Check application name spelling
- List versions for application

### Kubernetes Issues

**Problem: Pods not transitioning to Running**
- Check cluster health
- Verify image availability
- Check resource quotas
- Review pod logs

**Problem: Service endpoints not routing traffic**
- Verify selector matches pod labels
- Check pod health status
- Verify port configuration

### Docker Issues

**Problem: Image build failure**
- Check Dockerfile syntax
- Verify build context path
- Review build logs
- Verify dependency availability

**Problem: Push to registry fails**
- Verify registry credentials
- Check network connectivity
- Verify registry URL format
- Check image size limits

## REST API Endpoints

The DeploymentController exposes comprehensive REST APIs:

```
POST   /api/deployment/deployments
GET    /api/deployment/deployments
GET    /api/deployment/deployments/{deploymentId}
GET    /api/deployment/deployments/application/{appName}
GET    /api/deployment/deployments/environment/{environment}
PUT    /api/deployment/deployments/{deploymentId}/start
PUT    /api/deployment/deployments/{deploymentId}/complete
PUT    /api/deployment/deployments/{deploymentId}/fail
PUT    /api/deployment/deployments/{deploymentId}/rollback

POST   /api/deployment/versions
GET    /api/deployment/versions/{appName}/latest
GET    /api/deployment/versions/{appName}
GET    /api/deployment/deployments/{deploymentId}/events

GET    /api/deployment/kubernetes/deployments
POST   /api/deployment/kubernetes/deployments
GET    /api/deployment/kubernetes/deployments/{deploymentId}
PUT    /api/deployment/kubernetes/deployments/{deploymentId}/scale
PUT    /api/deployment/kubernetes/deployments/{deploymentId}/image

POST   /api/deployment/kubernetes/pods
GET    /api/deployment/kubernetes/pods/{podId}
GET    /api/deployment/kubernetes/pods/deployment/{deploymentId}
PUT    /api/deployment/kubernetes/pods/{podId}/status

POST   /api/deployment/kubernetes/services
GET    /api/deployment/kubernetes/services
GET    /api/deployment/kubernetes/health

POST   /api/deployment/docker/images/build
GET    /api/deployment/docker/images
GET    /api/deployment/docker/images/{imageId}
GET    /api/deployment/docker/images/name/{imageName}
POST   /api/deployment/docker/images/{imageId}/tag
POST   /api/deployment/docker/images/{imageId}/push
GET    /api/deployment/docker/images/{imageId}/validate
GET    /api/deployment/docker/builds/{buildJobId}/logs
POST   /api/deployment/docker/images/{imageName}/cleanup

GET    /api/deployment/stats/deployments
GET    /api/deployment/stats/images
```

## Example Workflows

### Complete Deployment Workflow

```java
// 1. Register new version
ApplicationVersion v2 = deploymentService.registerVersion(
    "myapp", "2.0.0", "https://releases.example.com/myapp-2.0.0.jar", "Latest features"
);

// 2. Build Docker image
DockerImage image = dockerService.buildImage(
    "myapp", "2.0.0", "/Dockerfile", "/src"
);

// 3. Push to registry
dockerService.pushImageToRegistry(
    image.imageId, "docker.io", "username", "token"
);

// 4. Create deployment
DeploymentRecord deployment = deploymentService.createDeployment(
    "myapp", "2.0.0", "production"
);

// 5. Start deployment
deploymentService.startDeployment(deployment.deploymentId);

// 6. Create K8s deployment
K8sDeployment k8s = kubernetesService.createDeployment(
    "myapp-prod", "production", "docker.io/myapp:2.0.0", 3
);

// 7. Monitor health
Map<String, Object> health = kubernetesService.getClusterHealth();

// 8. Complete deployment
deploymentService.completeDeployment(deployment.deploymentId);

// 9. Get stats
Map<String, Object> stats = deploymentService.getDeploymentStats();
```

### CI/CD Pipeline Workflow

```java
// 1. Configure pipeline
CICDPipelineService.PipelineConfig config = new CICDPipelineService.PipelineConfig();
config.pipelineName = "myapp-prod";
config.gitRepo = "https://github.com/example/myapp.git";
config.gitBranch = "main";
config.deploymentTarget = "production";
config.failOnSecurityIssues = false;

// 2. Create pipeline
CICDPipelineService.PipelineExecution execution = cicdService.createPipeline(config);

// 3. Execute pipeline
CICDPipelineService.PipelineExecutionResult result = 
    cicdService.executePipeline(execution, config);

// 4. Check results
System.out.println("Status: " + result.overallStatus);
System.out.println("Duration: " + result.duration + "ms");
for (CICDPipelineService.StageResult stage : result.stages) {
    System.out.println(stage.stageName + ": " + stage.success);
}
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-01-15 | Initial deployment automation implementation |

