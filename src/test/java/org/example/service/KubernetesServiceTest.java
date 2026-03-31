package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KubernetesService
 */
public class KubernetesServiceTest {
    
    private KubernetesService service;
    
    @BeforeEach
    public void setUp() {
        service = new KubernetesService();
    }
    
    @Test
    public void testCreateDeployment() {
        KubernetesService.K8sDeployment deployment = service.createDeployment(
                "nginx-deployment",
                "production",
                "nginx:latest",
                3
        );
        
        assertNotNull(deployment);
        assertEquals("nginx-deployment", deployment.name);
        assertEquals("production", deployment.namespace);
        assertEquals(3, deployment.desiredReplicas);
        assertEquals(0, deployment.readyReplicas);
    }
    
    @Test
    public void testGetDeployment() {
        KubernetesService.K8sDeployment created = service.createDeployment("app", "default", "app:1.0", 2);
        KubernetesService.K8sDeployment retrieved = service.getDeployment(created.deploymentId);
        
        assertNotNull(retrieved);
        assertEquals(created.deploymentId, retrieved.deploymentId);
    }
    
    @Test
    public void testListDeployments() {
        service.createDeployment("app1", "default", "app1:1.0", 1);
        service.createDeployment("app2", "default", "app2:1.0", 2);
        service.createDeployment("app3", "production", "app3:1.0", 3);
        
        java.util.Collection<KubernetesService.K8sDeployment> deployments = service.listDeployments();
        assertEquals(3, deployments.size());
    }
    
    @Test
    public void testScaleDeployment() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 2);
        
        service.scaleDeployment(deployment.deploymentId, 5);
        
        KubernetesService.K8sDeployment updated = service.getDeployment(deployment.deploymentId);
        assertEquals(5, updated.desiredReplicas);
    }
    
    @Test
    public void testUpdateDeploymentImage() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 2);
        
        service.updateDeploymentImage(deployment.deploymentId, "app:2.0");
        
        KubernetesService.K8sDeployment updated = service.getDeployment(deployment.deploymentId);
        assertEquals("app:2.0", updated.imageUrl);
    }
    
    @Test
    public void testCreatePod() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 1);
        
        KubernetesService.K8sPod pod = service.createPod(
                "app-pod-001",
                "default",
                deployment.deploymentId,
                "app:1.0"
        );
        
        assertNotNull(pod);
        assertEquals("app-pod-001", pod.podName);
        assertEquals("Pending", pod.status);
    }
    
    @Test
    public void testGetPod() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 1);
        KubernetesService.K8sPod created = service.createPod("pod", "default", deployment.deploymentId, "app:1.0");
        
        KubernetesService.K8sPod retrieved = service.getPod(created.podId);
        
        assertNotNull(retrieved);
        assertEquals(created.podId, retrieved.podId);
    }
    
    @Test
    public void testUpdatePodStatus() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 1);
        KubernetesService.K8sPod pod = service.createPod("pod", "default", deployment.deploymentId, "app:1.0");
        
        service.updatePodStatus(pod.podId, "Running");
        
        KubernetesService.K8sPod updated = service.getPod(pod.podId);
        assertEquals("Running", updated.status);
    }
    
    @Test
    public void testPodLifecycle() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 1);
        KubernetesService.K8sPod pod = service.createPod("pod", "default", deployment.deploymentId, "app:1.0");
        
        // Pending -> Running -> Terminated
        service.updatePodStatus(pod.podId, "Running");
        KubernetesService.K8sPod running = service.getPod(pod.podId);
        assertEquals("Running", running.status);
        
        service.updatePodStatus(pod.podId, "Terminated");
        KubernetesService.K8sPod terminated = service.getPod(pod.podId);
        assertEquals("Terminated", terminated.status);
    }
    
    @Test
    public void testListPodsByDeployment() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 3);
        
        service.createPod("pod1", "default", deployment.deploymentId, "app:1.0");
        service.createPod("pod2", "default", deployment.deploymentId, "app:1.0");
        service.createPod("pod3", "default", deployment.deploymentId, "app:1.0");
        
        List<KubernetesService.K8sPod> pods = service.listPodsByDeployment(deployment.deploymentId);
        assertEquals(3, pods.size());
    }
    
    @Test
    public void testCreateService() {
        KubernetesService.K8sService service_obj = service.createService(
                "app-service",
                "default",
                8080,
                "app=myapp"
        );
        
        assertNotNull(service_obj);
        assertEquals("app-service", service_obj.serviceName);
        assertEquals(8080, service_obj.port);
        assertEquals("app=myapp", service_obj.selector);
    }
    
    @Test
    public void testGetService() {
        KubernetesService.K8sService created = service.createService("svc", "default", 8080, "app=test");
        KubernetesService.K8sService retrieved = service.getService(created.serviceId);
        
        assertNotNull(retrieved);
        assertEquals(created.serviceId, retrieved.serviceId);
    }
    
    @Test
    public void testListServices() {
        service.createService("svc1", "default", 8080, "app=test1");
        service.createService("svc2", "default", 8081, "app=test2");
        service.createService("svc3", "production", 443, "app=test3");
        
        java.util.Collection<KubernetesService.K8sService> services = service.listServices();
        assertEquals(3, services.size());
    }
    
    @Test
    public void testGetClusterHealth() {
        // Create deployment with pods
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 3);
        service.createPod("pod1", "default", deployment.deploymentId, "app:1.0");
        service.createPod("pod2", "default", deployment.deploymentId, "app:1.0");
        service.createPod("pod3", "default", deployment.deploymentId, "app:1.0");
        
        Map<String, Object> health = service.getClusterHealth();
        
        assertNotNull(health);
        assertNotNull(health.get("totalPods"));
        assertNotNull(health.get("runningPods"));
        assertNotNull(health.get("pendingPods"));
        assertNotNull(health.get("failedPods"));
        assertNotNull(health.get("healthPercent"));
    }
    
    @Test
    public void testClusterHealthWithFailedPods() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 3);
        KubernetesService.K8sPod pod1 = service.createPod("pod1", "default", deployment.deploymentId, "app:1.0");
        KubernetesService.K8sPod pod2 = service.createPod("pod2", "default", deployment.deploymentId, "app:1.0");
        KubernetesService.K8sPod pod3 = service.createPod("pod3", "default", deployment.deploymentId, "app:1.0");
        
        // Set some to running, one to failed
        service.updatePodStatus(pod1.podId, "Running");
        service.updatePodStatus(pod2.podId, "Running");
        service.updatePodStatus(pod3.podId, "Failed");
        
        Map<String, Object> health = service.getClusterHealth();
        
        assertEquals(3, health.get("totalPods"));
        assertEquals(2L, health.get("runningPods"));
        assertEquals(1L, health.get("failedPods"));
    }
    
    @Test
    public void testMultiplePods() {
        KubernetesService.K8sDeployment deployment = service.createDeployment("app", "default", "app:1.0", 2);
        KubernetesService.K8sPod pod1 = service.createPod("pod1", "default", deployment.deploymentId, "app:1.0");
        KubernetesService.K8sPod pod2 = service.createPod("pod2", "default", deployment.deploymentId, "app:1.0");
        
        assertNotEquals(pod1.podId, pod2.podId);
        
        service.updatePodStatus(pod1.podId, "Running");
        service.updatePodStatus(pod2.podId, "Running");
        
        List<KubernetesService.K8sPod> pods = service.listPodsByDeployment(deployment.deploymentId);
        assertEquals(2, pods.size());
    }
}
