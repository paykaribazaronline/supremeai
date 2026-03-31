package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DeploymentService
 */
public class DeploymentServiceTest {
    
    private DeploymentService service;
    
    @BeforeEach
    public void setUp() {
        service = new DeploymentService();
    }
    
    @Test
    public void testCreateDeployment() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment(
                "myapp",
                "1.0.0",
                "production"
        );
        
        assertNotNull(deployment);
        assertEquals("myapp", deployment.applicationName);
        assertEquals("1.0.0", deployment.version);
        assertEquals("production", deployment.environment);
    }
    
    @Test
    public void testGetDeployment() {
        DeploymentService.DeploymentRecord created = service.createDeployment("app", "1.0.0", "prod");
        DeploymentService.DeploymentRecord retrieved = service.getDeployment(created.deploymentId);
        
        assertNotNull(retrieved);
        assertEquals(created.deploymentId, retrieved.deploymentId);
    }
    
    @Test
    public void testListDeployments() {
        service.createDeployment("app1", "1.0.0", "prod");
        service.createDeployment("app2", "2.0.0", "prod");
        service.createDeployment("app3", "1.5.0", "staging");
        
        Collection<DeploymentService.DeploymentRecord> deployments = service.listDeployments();
        assertEquals(3, deployments.size());
    }
    
    @Test
    public void testListDeploymentsByApplication() {
        service.createDeployment("app1", "1.0.0", "prod");
        service.createDeployment("app1", "2.0.0", "staging");
        service.createDeployment("app2", "1.0.0", "prod");
        
        List<DeploymentService.DeploymentRecord> deployments = service.listDeploymentsByApplication("app1");
        assertEquals(2, deployments.size());
    }
    
    @Test
    public void testListDeploymentsByEnvironment() {
        service.createDeployment("app1", "1.0.0", "prod");
        service.createDeployment("app2", "1.5.0", "prod");
        service.createDeployment("app3", "2.0.0", "staging");
        
        List<DeploymentService.DeploymentRecord> deployments = service.listDeploymentsByEnvironment("prod");
        assertEquals(2, deployments.size());
    }
    
    @Test
    public void testStartDeployment() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "1.0.0", "prod");
        
        service.startDeployment(deployment.deploymentId);
        
        DeploymentService.DeploymentRecord updated = service.getDeployment(deployment.deploymentId);
        assertEquals(DeploymentService.DeploymentStatus.IN_PROGRESS, updated.status);
    }
    
    @Test
    public void testCompleteDeployment() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "1.0.0", "prod");
        service.startDeployment(deployment.deploymentId);
        
        service.completeDeployment(deployment.deploymentId);
        
        DeploymentService.DeploymentRecord updated = service.getDeployment(deployment.deploymentId);
        assertEquals(DeploymentService.DeploymentStatus.SUCCESS, updated.status);
    }
    
    @Test
    public void testFailDeployment() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "1.0.0", "prod");
        
        service.failDeployment(deployment.deploymentId, "Connection timeout");
        
        DeploymentService.DeploymentRecord updated = service.getDeployment(deployment.deploymentId);
        assertEquals(DeploymentService.DeploymentStatus.FAILED, updated.status);
        assertEquals("Connection timeout", updated.failureReason);
    }
    
    @Test
    public void testRollbackDeployment() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "2.0.0", "prod");
        
        service.rollbackDeployment(deployment.deploymentId, "1.9.5");
        
        DeploymentService.DeploymentRecord updated = service.getDeployment(deployment.deploymentId);
        assertEquals(DeploymentService.DeploymentStatus.ROLLED_BACK, updated.status);
        assertEquals("1.9.5", updated.rolledBackTo);
    }
    
    @Test
    public void testRegisterVersion() {
        DeploymentService.ApplicationVersion version = service.registerVersion(
                "myapp",
                "1.0.0",
                "https://artifacts.example.com/app-1.0.0.jar",
                "Initial release"
        );
        
        assertNotNull(version);
        assertEquals("myapp", version.applicationName);
        assertEquals("1.0.0", version.version);
    }
    
    @Test
    public void testGetLatestVersion() {
        service.registerVersion("app", "1.0.0", "url1", "v1");
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.registerVersion("app", "2.0.0", "url2", "v2");
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.registerVersion("app", "1.5.0", "url3", "v1.5");
        
        DeploymentService.ApplicationVersion latest = service.getLatestVersion("app");
        
        assertNotNull(latest);
        assertEquals("1.5.0", latest.version);
    }
    
    @Test
    public void testListVersionsForApplication() {
        service.registerVersion("app1", "1.0.0", "url1", "v1");
        service.registerVersion("app1", "2.0.0", "url2", "v2");
        service.registerVersion("app2", "1.0.0", "url3", "v1");
        
        List<DeploymentService.ApplicationVersion> versions = service.listVersionsForApplication("app1");
        assertEquals(2, versions.size());
    }
    
    @Test
    public void testGetDeploymentEvents() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "1.0.0", "prod");
        service.startDeployment(deployment.deploymentId);
        service.completeDeployment(deployment.deploymentId);
        
        List<DeploymentService.DeploymentEvent> events = service.getDeploymentEvents(deployment.deploymentId);
        assertTrue(events.size() >= 3); // Created, Started, Completed
    }
    
    @Test
    public void testGetDeploymentStats() {
        service.createDeployment("app1", "1.0.0", "prod");
        service.createDeployment("app2", "2.0.0", "prod");
        service.createDeployment("app3", "1.5.0", "staging");
        
        Map<String, Object> stats = service.getDeploymentStats();
        
        assertNotNull(stats);
        assertEquals(3L, stats.get("totalDeployments"));
        assertNotNull(stats.get("successRate"));
    }
    
    @Test
    public void testDeploymentDuration() {
        DeploymentService.DeploymentRecord deployment = service.createDeployment("app", "1.0.0", "prod");
        service.startDeployment(deployment.deploymentId);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.completeDeployment(deployment.deploymentId);
        
        DeploymentService.DeploymentRecord updated = service.getDeployment(deployment.deploymentId);
        assertTrue(updated.getDuration() >= 0);
    }
    
    @Test
    public void testMultipleDeployments() {
        DeploymentService.DeploymentRecord dep1 = service.createDeployment("app1", "1.0.0", "prod");
        DeploymentService.DeploymentRecord dep2 = service.createDeployment("app2", "2.0.0", "staging");
        
        assertNotEquals(dep1.deploymentId, dep2.deploymentId);
        
        service.completeDeployment(dep1.deploymentId);
        service.failDeployment(dep2.deploymentId, "Test failure");
        
        DeploymentService.DeploymentRecord updated1 = service.getDeployment(dep1.deploymentId);
        DeploymentService.DeploymentRecord updated2 = service.getDeployment(dep2.deploymentId);
        
        assertEquals(DeploymentService.DeploymentStatus.SUCCESS, updated1.status);
        assertEquals(DeploymentService.DeploymentStatus.FAILED, updated2.status);
    }
}
