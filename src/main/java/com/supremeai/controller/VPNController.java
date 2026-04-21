package com.supremeai.controller;

import com.supremeai.model.VPNConnection;
import com.supremeai.repository.VPNRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/vpn")
public class VPNController extends BaseAdminController<VPNConnection, String> {

    private final VPNRepository vpnRepository;

    public VPNController(VPNRepository vpnRepository) {
        this.vpnRepository = vpnRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<Object>> getConnections() {
        return wrapList(vpnRepository.findAll(), "connections");
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createConnection(@RequestBody VPNConnection connection) {
        return wrapSave(vpnRepository.save(connection), "VPN connection created", connection);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteConnection(@PathVariable String id) {
        return wrapDelete(vpnRepository.deleteById(id), "VPN connection deleted");
    }
}
