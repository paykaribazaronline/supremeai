package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Docker Integration Service
 * Manages Docker image building, registry operations, and container validation
 */
public class DockerIntegrationService {
    
    private final Map<String, DockerImage> images = new ConcurrentHashMap<>();
    private final Map<String, BuildJob> buildJobs = new ConcurrentHashMap<>();
    private final Map<String, String> registryCredentials = new ConcurrentHashMap<>();
    
    /**
     * Build Docker image from Dockerfile
     */
    public DockerImage buildImage(String imageName, String tag, String dockerfilePath, String buildContext) {
        DockerImage image = new DockerImage(
                UUID.randomUUID().toString(),
                imageName,
                tag,
                dockerfilePath,
                System.currentTimeMillis()
        );
        
        // Record build job
        BuildJob job = new BuildJob(
                UUID.randomUUID().toString(),
                image.imageId,
                imageName + ":" + tag,
                "BUILDING",
                System.currentTimeMillis()
        );
        buildJobs.put(job.buildJobId, job);
        
        // Simulate build process
        job.addLog("Starting Docker build for " + imageName + ":" + tag);
        job.addLog("Dockerfile: " + dockerfilePath);
        job.addLog("Build context: " + buildContext);
        job.addLog("Building image layers...");
        job.addLog("Layer 1/5: base image pulled");
        job.addLog("Layer 2/5: installing dependencies");
        job.addLog("Layer 3/5: copying application code");
        job.addLog("Layer 4/5: setting up entrypoint");
        job.addLog("Layer 5/5: finalizing image");
        job.addLog("Build completed successfully");
        
        job.status = "SUCCESS";
        job.completedAt = System.currentTimeMillis();
        image.buildJobId = job.buildJobId;
        image.size = 256 * 1024 * 1024; // 256 MB
        image.status = "READY";
        
        images.put(image.imageId, image);
        return image;
    }
    
    /**
     * Get Docker image
     */
    public DockerImage getImage(String imageId) {
        return images.get(imageId);
    }
    
    /**
     * List all Docker images
     */
    public Collection<DockerImage> listImages() {
        return new ArrayList<>(images.values());
    }
    
    /**
     * List images by name
     */
    public List<DockerImage> listImagesByName(String imageName) {
        return images.values().stream()
                .filter(img -> img.imageName.equals(imageName))
                .toList();
    }
    
    /**
     * Tag existing image with new tag
     */
    public DockerImage tagImage(String sourceImageId, String newTag) {
        DockerImage source = images.get(sourceImageId);
        if (source == null) return null;
        
        DockerImage newImage = new DockerImage(
                UUID.randomUUID().toString(),
                source.imageName,
                newTag,
                source.dockerfilePath,
                System.currentTimeMillis()
        );
        newImage.baseImageId = sourceImageId;
        newImage.size = source.size;
        newImage.status = "READY";
        
        images.put(newImage.imageId, newImage);
        return newImage;
    }
    
    /**
     * Push image to registry
     */
    public void pushImageToRegistry(String imageId, String registry, String username, String password) {
        DockerImage image = images.get(imageId);
        if (image == null) return;
        
        // Store registry credentials (in real implementation, use secure storage)
        String registryKey = registry + ":" + username;
        registryCredentials.put(registryKey, password);
        
        // Record build job for push operation
        BuildJob pushJob = new BuildJob(
                UUID.randomUUID().toString(),
                imageId,
                image.imageName + ":" + image.tag,
                "PUSHING",
                System.currentTimeMillis()
        );
        buildJobs.put(pushJob.buildJobId, pushJob);
        
        // Simulate push process
        pushJob.addLog("Logging into registry: " + registry);
        pushJob.addLog("Authenticating with credentials");
        pushJob.addLog("Pushing image " + image.imageName + ":" + image.tag);
        pushJob.addLog("Layer 1/5: Uploading 64MB");
        pushJob.addLog("Layer 2/5: Uploading 32MB");
        pushJob.addLog("Layer 3/5: Uploading 48MB");
        pushJob.addLog("Layer 4/5: Uploading 16MB");
        pushJob.addLog("Layer 5/5: Uploading 96MB");
        pushJob.addLog("Image pushed successfully");
        pushJob.addLog("Image URL: " + registry + "/" + image.imageName + ":" + image.tag);
        
        pushJob.status = "SUCCESS";
        pushJob.completedAt = System.currentTimeMillis();
        
        image.registry = registry;
        image.registryUrl = registry + "/" + image.imageName + ":" + image.tag;
        image.pushedAt = System.currentTimeMillis();
        image.status = "PUBLISHED";
    }
    
    /**
     * Validate image exists and is accessible
     */
    public boolean validateImage(String imageId) {
        DockerImage image = images.get(imageId);
        if (image == null) return false;
        
        image.lastValidatedAt = System.currentTimeMillis();
        image.validationCount++;
        return true;
    }
    
    /**
     * Get build job details
     */
    public BuildJob getBuildJob(String buildJobId) {
        return buildJobs.get(buildJobId);
    }
    
    /**
     * List build jobs for image
     */
    public List<BuildJob> listBuildJobsForImage(String imageId) {
        return buildJobs.values().stream()
                .filter(job -> job.imageId.equals(imageId))
                .toList();
    }
    
    /**
     * Get build logs for job
     */
    public List<String> getBuildLogs(String buildJobId) {
        BuildJob job = buildJobs.get(buildJobId);
        return job != null ? new ArrayList<>(job.logs) : new ArrayList<>();
    }
    
    /**
     * Clean up old images (keep last N versions)
     */
    public void cleanupOldImages(String imageName, int keepCount) {
        List<DockerImage> appImages = listImagesByName(imageName);
        
        if (appImages.size() > keepCount) {
            // Sort by creation time descending
            List<DockerImage> sortedImages = appImages.stream()
                    .sorted((a, b) -> Long.compare(b.createdAt, a.createdAt))
                    .toList();
            
            // Remove old ones
            for (int i = keepCount; i < sortedImages.size(); i++) {
                images.remove(sortedImages.get(i).imageId);
            }
        }
    }
    
    /**
     * Get image statistics
     */
    public Map<String, Object> getImageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalImages = images.size();
        long readyImages = images.values().stream()
                .filter(img -> "READY".equals(img.status))
                .count();
        long publishedImages = images.values().stream()
                .filter(img -> "PUBLISHED".equals(img.status))
                .count();
        long totalSize = images.values().stream()
                .mapToLong(img -> img.size)
                .sum();
        
        stats.put("totalImages", totalImages);
        stats.put("readyImages", readyImages);
        stats.put("publishedImages", publishedImages);
        stats.put("totalSizeBytes", totalSize);
        stats.put("totalBuildJobs", buildJobs.size());
        stats.put("averageImageSize", totalImages > 0 ? totalSize / totalImages : 0);
        stats.put("generatedAt", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * Docker Image
     */
    public static class DockerImage {
        public String imageId;
        public String imageName;
        public String tag;
        public String dockerfilePath;
        public String baseImageId = null;
        public String status = "PENDING"; // PENDING, BUILDING, READY, PUBLISHED
        public String registry = null;
        public String registryUrl = null;
        public long size = 0;
        public long createdAt;
        public long pushedAt = 0;
        public long lastValidatedAt = 0;
        public int validationCount = 0;
        public String buildJobId = null;
        
        public DockerImage(String imageId, String imageName, String tag, String dockerfilePath, long createdAt) {
            this.imageId = imageId;
            this.imageName = imageName;
            this.tag = tag;
            this.dockerfilePath = dockerfilePath;
            this.createdAt = createdAt;
        }
        
        public long getAge() {
            return System.currentTimeMillis() - createdAt;
        }
    }
    
    /**
     * Build Job
     */
    public static class BuildJob {
        public String buildJobId;
        public String imageId;
        public String imageRef;
        public String status; // BUILDING, PUSHING, SUCCESS, FAILED
        public long startedAt;
        public long completedAt = 0;
        public List<String> logs = Collections.synchronizedList(new ArrayList<>());
        
        public BuildJob(String buildJobId, String imageId, String imageRef, String status, long startedAt) {
            this.buildJobId = buildJobId;
            this.imageId = imageId;
            this.imageRef = imageRef;
            this.status = status;
            this.startedAt = startedAt;
        }
        
        public void addLog(String message) {
            logs.add("[" + new Date(System.currentTimeMillis()).toString() + "] " + message);
        }
        
        public long getDuration() {
            if (completedAt == 0) return 0;
            return completedAt - startedAt;
        }
    }
}
