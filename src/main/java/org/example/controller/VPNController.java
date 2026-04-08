package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.VPNConnection;
import org.example.service.LocalJsonStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * VPN Controller — manages VPN connection configurations.
 *
 * NOTE: "Connect" / "Disconnect" update the configuration record in SupremeAI's
 * database. Actual OS-level tunnel management (openvpn, wg-quick, etc.) depends
 * on the host system and is outside the scope of this controller.
 *
 * All state is persisted to disk via LocalJsonStoreService so it survives restarts.
 */
@RestController
@RequestMapping("/api/vpn")
@CrossOrigin(origins = "*")
public class VPNController {
    private static final Logger logger = LoggerFactory.getLogger(VPNController.class);

    private static final String STORE_PATH = "vpn/connections.json";

    @Autowired
    private LocalJsonStoreService jsonStore;

    /** In-memory map: id → VPNConnection, loaded from disk on startup. */
    private final Map<String, VPNConnection> connections = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<VPNConnection> saved = jsonStore.read(
                STORE_PATH,
                new TypeReference<List<VPNConnection>>() {},
                List.of());
        for (VPNConnection v : saved) {
            if (v.getId() != null) {
                connections.put(v.getId(), v);
            }
        }
        logger.info("✅ VPNController ready — loaded {} connection(s) from disk", connections.size());
    }

    private void persist() {
        jsonStore.write(STORE_PATH, new ArrayList<>(connections.values()));
    }

    // ── List ─────────────────────────────────────────────────────────────────

    @GetMapping("/list")
    public ResponseEntity<?> listVPNs() {
        try {
            List<VPNConnection> sorted = connections.values().stream()
                    .sorted(Comparator.comparing(VPNConnection::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sorted);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Add ──────────────────────────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<?> addVPN(@RequestBody VPNConnection vpn) {
        try {
            if (vpn.getId() == null || vpn.getId().isBlank()) {
                vpn.setId(UUID.randomUUID().toString());
            }
            vpn.setStatus("disconnected");
            vpn.setLastConnectedAt(0);
            connections.put(vpn.getId(), vpn);
            persist();
            logger.info("➕ VPN added: {} ({})", vpn.getName(), vpn.getId());
            return ResponseEntity.ok(Map.of("success", true, "id", vpn.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVPN(@PathVariable String id, @RequestBody VPNConnection vpn) {
        try {
            if (!connections.containsKey(id)) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found: " + id));
            }
            vpn.setId(id);
            connections.put(id, vpn);
            persist();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVPN(@PathVariable String id) {
        try {
            VPNConnection removed = connections.remove(id);
            if (removed == null) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found: " + id));
            }
            persist();
            logger.info("🗑️ VPN removed: {} ({})", removed.getName(), id);
            return ResponseEntity.ok(Map.of("success", true, "id", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Connect ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/connect")
    public ResponseEntity<?> connectVPN(@PathVariable String id) {
        try {
            VPNConnection vpn = connections.get(id);
            if (vpn == null) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found: " + id));
            }
            vpn.setStatus("connected");
            vpn.setLastConnectedAt(System.currentTimeMillis());
            persist();
            logger.info("🟢 VPN marked connected: {}", vpn.getName());
            return ResponseEntity.ok(Map.of("success", true, "status", "connected",
                    "lastConnectedAt", vpn.getLastConnectedAt()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Disconnect ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/disconnect")
    public ResponseEntity<?> disconnectVPN(@PathVariable String id) {
        try {
            VPNConnection vpn = connections.get(id);
            if (vpn == null) {
                return ResponseEntity.status(404).body(Map.of("error", "VPN not found: " + id));
            }
            vpn.setStatus("disconnected");
            persist();
            logger.info("⚪ VPN marked disconnected: {}", vpn.getName());
            return ResponseEntity.ok(Map.of("success", true, "status", "disconnected"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
