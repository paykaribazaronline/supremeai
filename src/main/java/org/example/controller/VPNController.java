package org.example.controller;

import org.example.model.VPNConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * VPN Controller
 * Manages VPN configurations and connections
 */
@RestController
@RequestMapping("/api/vpn")
@CrossOrigin(origins = "*")
public class VPNController {

    private static final List<VPNConnection> vpnConnections = new ArrayList<>();

    static {
        VPNConnection vpn1 = new VPNConnection("Secure Europe", "WireGuard", "eu.vpn.example.com", 51820);
        vpn1.setEncryption("ChaCha20");
        vpn1.setStatus("disconnected");
        vpnConnections.add(vpn1);

        VPNConnection vpn2 = new VPNConnection("US Fast", "OpenVPN", "us.vpn.example.com", 1194);
        vpn2.setEncryption("AES-256");
        vpn2.setStatus("disconnected");
        vpnConnections.add(vpn2);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listVPNs() {
        try {
            return ResponseEntity.ok(vpnConnections);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addVPN(@RequestBody VPNConnection vpn) {
        try {
            if (vpn.getId() == null) {
                vpn.setId(UUID.randomUUID().toString());
            }
            vpnConnections.add(vpn);
            return ResponseEntity.ok(Map.of("success", true, "id", vpn.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVPN(@PathVariable String id, @RequestBody VPNConnection vpn) {
        try {
            vpnConnections.removeIf(v -> v.getId().equals(id));
            vpn.setId(id);
            vpnConnections.add(vpn);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/connect")
    public ResponseEntity<?> connectVPN(@PathVariable String id) {
        try {
            VPNConnection vpn = vpnConnections.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(null);

            if (vpn == null) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found"));
            }

            vpn.setStatus("connected");
            vpn.setLastConnected(LocalDateTime.now());
            return ResponseEntity.ok(Map.of("success", true, "status", "connected"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/disconnect")
    public ResponseEntity<?> disconnectVPN(@PathVariable String id) {
        try {
            VPNConnection vpn = vpnConnections.stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(null);

            if (vpn == null) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found"));
            }

            vpn.setStatus("disconnected");
            return ResponseEntity.ok(Map.of("success", true, "status", "disconnected"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
