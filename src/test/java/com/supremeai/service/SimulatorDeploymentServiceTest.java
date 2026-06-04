package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.model.SimulatorDeploymentRecord;
import com.supremeai.repository.SimulatorDeploymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SimulatorDeploymentServiceTest {

  @Mock private SimulatorDeploymentRepository repository;

  @InjectMocks private SimulatorDeploymentService service;

  @Test
  void getStatus_WhenRecordExists_ReturnsStatus() {
    SimulatorDeploymentRecord record =
        new SimulatorDeploymentRecord("app1", "web", "url", "RUNNING");
    when(repository.findById("app1")).thenReturn(Mono.just(record));

    StepVerifier.create(service.getStatus("app1"))
        .expectNext(SimulatorDeploymentService.DeploymentStatus.RUNNING)
        .verifyComplete();
  }

  @Test
  void getStatus_WhenRecordDoesNotExist_ReturnsNotDeployed() {
    when(repository.findById("app2")).thenReturn(Mono.empty());

    StepVerifier.create(service.getStatus("app2"))
        .expectNext(SimulatorDeploymentService.DeploymentStatus.NOT_DEPLOYED)
        .verifyComplete();
  }

  @Test
  void getStatus_WhenStatusIsNull_ReturnsNotDeployed() {
    SimulatorDeploymentRecord record = new SimulatorDeploymentRecord("app3", "web", "url", null);
    when(repository.findById("app3")).thenReturn(Mono.just(record));

    StepVerifier.create(service.getStatus("app3"))
        .expectNext(SimulatorDeploymentService.DeploymentStatus.NOT_DEPLOYED)
        .verifyComplete();
  }

  @Test
  void getStatus_WhenStatusIsInvalid_ReturnsError() {
    SimulatorDeploymentRecord record =
        new SimulatorDeploymentRecord("app4", "web", "url", "INVALID_STATUS_123");
    when(repository.findById("app4")).thenReturn(Mono.just(record));

    StepVerifier.create(service.getStatus("app4"))
        .expectNext(SimulatorDeploymentService.DeploymentStatus.ERROR)
        .verifyComplete();
  }

  @Test
  void getAllDeployments_ReturnsFlux() {
    SimulatorDeploymentRecord record =
        new SimulatorDeploymentRecord("app1", "web", "url", "RUNNING");
    when(repository.findAll()).thenReturn(Flux.just(record));

    StepVerifier.create(service.getAllDeployments()).expectNext(record).verifyComplete();
  }

  @Test
  void isDeploymentHealthy_Localhost_ReturnsTrue() {
    // Localhost URL should bypass WebClient and return true immediately
    StepVerifier.create(service.isDeploymentHealthy("http://localhost:8080/health"))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void isDeploymentHealthy_NullOrEmptyUrl_ReturnsFalse() {
    StepVerifier.create(service.isDeploymentHealthy(null)).expectNext(false).verifyComplete();
  }
}
