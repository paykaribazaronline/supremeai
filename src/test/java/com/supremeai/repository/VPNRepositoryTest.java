package com.supremeai.repository;

import com.supremeai.model.VPNConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VPNRepositoryTest {

    @Mock
    private VPNRepository repository;

    @Test
    void save_shouldPersistConnection() {
        VPNConnection vpn = new VPNConnection("vpn-1", "US East", "us-east-1", 8080, "connected");
        when(repository.save(vpn)).thenReturn(Mono.just(vpn));

        StepVerifier.create(repository.save(vpn))
                .expectNextMatches(v -> "vpn-1".equals(v.getId()) && "connected".equals(v.getStatus()))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnConnection_whenExists() {
        VPNConnection vpn = new VPNConnection("vpn-2", "EU West", "eu-west-1", 8080, "disconnected");
        when(repository.findById("vpn-2")).thenReturn(Mono.just(vpn));

        StepVerifier.create(repository.findById("vpn-2"))
                .expectNextMatches(v -> "EU West".equals(v.getName()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllConnections() {
        VPNConnection v1 = new VPNConnection("vpn-3", "Asia", "ap-south-1", 8080, "connected");
        VPNConnection v2 = new VPNConnection("vpn-4", "Europe", "eu-central-1", 8080, "connected");

        when(repository.findAll()).thenReturn(Flux.fromIterable(List.of(v1, v2)));

        StepVerifier.create(repository.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deleteById_shouldRemoveConnection() {
        when(repository.deleteById("vpn-delete")).thenReturn(Mono.empty());

        StepVerifier.create(repository.deleteById("vpn-delete"))
                .verifyComplete();
    }
}
