package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DockerIntegrationService
 */
public class DockerIntegrationServiceTest {
    
    private DockerIntegrationService service;
    
    @BeforeEach
    public void setUp() {
        service = new DockerIntegrationService();
    }
    
    @Test
    public void testBuildImage() {
        DockerIntegrationService.DockerImage image = service.buildImage(
                "myapp",
                "1.0.0",
                "/path/to/Dockerfile",
                "/path/to/context"
        );
        
        assertNotNull(image);
        assertEquals("myapp", image.imageName);
        assertEquals("1.0.0", image.tag);
        assertEquals("READY", image.status);
    }
    
    @Test
    public void testGetImage() {
        DockerIntegrationService.DockerImage created = service.buildImage("app", "1.0", "/Dockerfile", "/");
        DockerIntegrationService.DockerImage retrieved = service.getImage(created.imageId);
        
        assertNotNull(retrieved);
        assertEquals(created.imageId, retrieved.imageId);
    }
    
    @Test
    public void testListImages() {
        service.buildImage("app1", "1.0", "/Dockerfile", "/");
        service.buildImage("app2", "1.0", "/Dockerfile", "/");
        service.buildImage("app3", "2.0", "/Dockerfile", "/");
        
        Collection<DockerIntegrationService.DockerImage> images = service.listImages();
        assertEquals(3, images.size());
    }
    
    @Test
    public void testListImagesByName() {
        service.buildImage("app", "1.0", "/Dockerfile", "/");
        service.buildImage("app", "2.0", "/Dockerfile", "/");
        service.buildImage("other", "1.0", "/Dockerfile", "/");
        
        List<DockerIntegrationService.DockerImage> images = service.listImagesByName("app");
        assertEquals(2, images.size());
    }
    
    @Test
    public void testTagImage() {
        DockerIntegrationService.DockerImage source = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        DockerIntegrationService.DockerImage tagged = service.tagImage(source.imageId, "latest");
        
        assertNotNull(tagged);
        assertEquals("app", tagged.imageName);
        assertEquals("latest", tagged.tag);
        assertEquals(source.imageId, tagged.baseImageId);
    }
    
    @Test
    public void testPushImageToRegistry() {
        DockerIntegrationService.DockerImage image = service.buildImage("myapp", "1.0", "/Dockerfile", "/");
        
        service.pushImageToRegistry(image.imageId, "docker.io", "username", "password");
        
        DockerIntegrationService.DockerImage updated = service.getImage(image.imageId);
        assertEquals("PUBLISHED", updated.status);
        assertEquals("docker.io", updated.registry);
        assertNotNull(updated.registryUrl);
    }
    
    @Test
    public void testValidateImage() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        boolean valid = service.validateImage(image.imageId);
        
        assertTrue(valid);
        DockerIntegrationService.DockerImage updated = service.getImage(image.imageId);
        assertEquals(1, updated.validationCount);
    }
    
    @Test
    public void testGetBuildJob() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        DockerIntegrationService.BuildJob job = service.getBuildJob(image.buildJobId);
        
        assertNotNull(job);
        assertEquals("SUCCESS", job.status);
        assertEquals(image.imageId, job.imageId);
    }
    
    @Test
    public void testListBuildJobsForImage() {
        DockerIntegrationService.DockerImage image1 = service.buildImage("app", "1.0", "/Dockerfile", "/");
        DockerIntegrationService.DockerImage image2 = service.buildImage("app", "2.0", "/Dockerfile", "/");
        
        service.pushImageToRegistry(image1.imageId, "docker.io", "user", "pass");
        
        List<DockerIntegrationService.BuildJob> jobs = service.listBuildJobsForImage(image1.imageId);
        assertEquals(2, jobs.size()); // Build and push
    }
    
    @Test
    public void testGetBuildLogs() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        List<String> logs = service.getBuildLogs(image.buildJobId);
        
        assertNotNull(logs);
        assertTrue(logs.size() > 0);
        assertTrue(logs.get(0).contains("Starting Docker build"));
    }
    
    @Test
    public void testCleanupOldImages() {
        service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.buildImage("app", "2.0", "/Dockerfile", "/");
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.buildImage("app", "3.0", "/Dockerfile", "/");
        
        List<DockerIntegrationService.DockerImage> before = service.listImagesByName("app");
        assertEquals(3, before.size());
        
        // Keep only 2 versions
        service.cleanupOldImages("app", 2);
        
        List<DockerIntegrationService.DockerImage> after = service.listImagesByName("app");
        assertEquals(2, after.size());
    }
    
    @Test
    public void testGetImageStats() {
        service.buildImage("app1", "1.0", "/Dockerfile", "/");
        service.buildImage("app2", "1.0", "/Dockerfile", "/");
        service.buildImage("app3", "2.0", "/Dockerfile", "/");
        
        Map<String, Object> stats = service.getImageStats();
        
        assertNotNull(stats);
        assertEquals(3, stats.get("totalImages"));
        assertEquals(3L, stats.get("readyImages"));
        assertEquals(0L, stats.get("publishedImages"));
        assertNotNull(stats.get("totalSizeBytes"));
    }
    
    @Test
    public void testImageSize() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        assertTrue(image.size > 0);
        assertEquals(256 * 1024 * 1024, image.size);
    }
    
    @Test
    public void testBuildJobLogs() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        DockerIntegrationService.BuildJob job = service.getBuildJob(image.buildJobId);
        assertTrue(job.logs.size() > 0);
        assertTrue(job.logs.get(job.logs.size() - 1).contains("Build completed"));
    }
    
    @Test
    public void testMultipleRegistryPushes() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        service.pushImageToRegistry(image.imageId, "docker.io", "user1", "pass1");
        
        DockerIntegrationService.DockerImage updated = service.getImage(image.imageId);
        assertEquals("docker.io", updated.registry);
        
        // Simulate second push
        service.pushImageToRegistry(image.imageId, "quay.io", "user2", "pass2");
        
        DockerIntegrationService.DockerImage updated2 = service.getImage(image.imageId);
        assertEquals("quay.io", updated2.registry);
    }
    
    @Test
    public void testImageAge() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        long age = image.getAge();
        assertTrue(age >= 0);
    }
    
    @Test
    public void testBuildJobDuration() {
        DockerIntegrationService.DockerImage image = service.buildImage("app", "1.0", "/Dockerfile", "/");
        
        DockerIntegrationService.BuildJob job = service.getBuildJob(image.buildJobId);
        long duration = job.getDuration();
        
        assertTrue(duration >= 0);
    }
}
