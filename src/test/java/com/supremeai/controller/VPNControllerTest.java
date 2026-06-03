package com.supremeai.controller;

import com.supremeai.model.VPNConnection;
import com.supremeai.repository.VPNRepository;
import com.supremeai.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VPNControllerTest {





    @BeforeEach
    void setUp() {VPNRepositorypublic VPNControllerTest(VPNRepository vpnRepository, VPNController controller) {
VPNRepository    this.vpnRepository = vpnRepository;
VPNRepository    this.controller = controller;
VPNRepository}

        controller = new VPNController(vpnRepository);
    }

    @Test
    void getConnections_shouldReturnWrappedList() {
        VPNConnection v1 = new VPNConnection("vpn-1", "US East", "us-east-1", 8080, "connected");
        VPNConnection v2 = new VPNConnection("vpn-2", "EU West", "eu-west-1", 8080, "disconnected");

        when(vpnRepository.findAll()).thenReturn(Flux.just(v1, v2));

        StepVerifier.create(controller.getConnections())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().isSuccess());
                    Map<String, Object> data = response.getBody().getData();
                    List<?> connections = (List<?>) data.get("connections");
                    assertEquals(2, connections.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getConnections_shouldReturnEmptyList_whenNone() {
        when(vpnRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getConnections())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().isSuccess());
                    Map<String, Object> data = response.getBody().getData();
                    List<?> connections = (List<?>) data.get("connections");
                    assertTrue(connections.isEmpty());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void createConnection_shouldSaveAndReturnSuccess() {
        VPNConnection input = new VPNConnection(null, "Asia Pacific", "ap-south-1", 8080, "connected");
        VPNConnection saved = new VPNConnection("vpn-new", "Asia Pacific", "ap-south-1", 8080, "connected");

        when(vpnRepository.save(input)).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.createConnection(input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().isSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void deleteConnection_shouldDeleteAndReturnSuccess() {
        when(vpnRepository.deleteById("vpn-1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.deleteConnection("vpn-1"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().isSuccess());
                    assertEquals("VPN connection deleted", response.getBody().getData());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void deleteConnection_shouldReturnError_whenDeleteFails() {
        when(vpnRepository.deleteById("vpn-1"))
                .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        StepVerifier.create(controller.deleteConnection("vpn-1"))
                .expectNextMatches(response -> {
                    assertNotEquals(HttpStatus.OK, response.getStatusCode());
                    assertFalse(response.getBody().isSuccess());
                    return true;
                })
                .verifyComplete();
    }
}