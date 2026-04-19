package com.supremeai.controller;

import com.supremeai.model.VPNConnection;
import com.supremeai.repository.VPNRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vpn")
public class VPNController {

    @Autowired
    private VPNRepository vpnRepository;

    @GetMapping
    public Mono<ResponseEntity<Object>> getConnections() {
        return vpnRepository.findAll()
                .collectList()
                .map(connections -> ResponseEntity.ok((Object) Map.of("connections", connections)))
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Failed to fetch VPN connections: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createConnection(@RequestBody VPNConnection connection) {
        return vpnRepository.save(connection)
                .map(saved -> ResponseEntity.ok((Object) Map.of("message", "VPN connection created", "connection", saved)))
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Failed to create VPN connection: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteConnection(@PathVariable String id) {
        return vpnRepository.deleteById(id)
                .then(Mono.just(ResponseEntity.ok((Object) Map.of("message", "VPN connection deleted"))))
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Failed to delete VPN connection: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body((Object) errorBody));
                });
    }
}
