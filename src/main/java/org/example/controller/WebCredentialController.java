package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.WebCredential;
import org.example.service.LocalJsonStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebCredentialController — stores login credentials for web sites used by
 * the Browser Automation workspace.
 *
 * Credentials are persisted to disk so they survive restarts.
 * Passwords are never sent to any AI provider.
 *
 * Endpoints:
 *   POST   /api/web-credentials              — Save a new credential
 *   GET    /api/web-credentials              — List all (passwords masked)
 *   GET    /api/web-credentials/lookup?url=  — Find credential for a domain (returns password)
 *   DELETE /api/web-credentials/{id}         — Remove a saved credential
 */
@RestController
@RequestMapping("/api/web-credentials")
@CrossOrigin(origins = "*")
public class WebCredentialController {
    private static final Logger logger = LoggerFactory.getLogger(WebCredentialController.class);

    private static final String STORE_PATH = "browser/web-credentials.json";

    @Autowired
    private LocalJsonStoreService jsonStore;

    private final Map<String, WebCredential> credentials = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<WebCredential> saved = jsonStore.read(
                STORE_PATH,
                new TypeReference<List<WebCredential>>() {},
                List.of());
        for (WebCredential c : saved) {
            if (c.getId() != null) {
                credentials.put(c.getId(), c);
            }
        }
        logger.info("✅ WebCredentialController ready — loaded {} credential(s) from disk",
                credentials.size());
    }

    private void persist() {
        jsonStore.write(STORE_PATH, new ArrayList<>(credentials.values()));
    }

    // ── List (passwords masked) ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<Map<String, Object>> masked = credentials.values().stream()
                    .sorted(Comparator.comparingLong(WebCredential::getCreatedAt).reversed())
                    .map(WebCredential::toMaskedMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("credentials", masked, "total", masked.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, String> body) {
        try {
            String siteUrl  = (body.getOrDefault("siteUrl", "")).trim();
            String username = (body.getOrDefault("username", "")).trim();
            String password = body.getOrDefault("password", "");

            if (siteUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "siteUrl is required"));
            }
            if (username.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
            }

            // Normalise to bare hostname to avoid duplicate entries for the same site
            String normalised = normaliseUrl(siteUrl);

            // Check if a credential for this domain already exists — update it
            Optional<WebCredential> existing = credentials.values().stream()
                    .filter(c -> normaliseUrl(c.getSiteUrl()).equals(normalised))
                    .findFirst();

            WebCredential cred;
            boolean updated;
            if (existing.isPresent()) {
                cred = existing.get();
                updated = true;
            } else {
                cred = new WebCredential();
                updated = false;
            }

            cred.setSiteUrl(siteUrl.isBlank() ? siteUrl : normalised);
            cred.setSiteName(body.getOrDefault("siteName", extractSiteName(siteUrl)));
            cred.setUsername(username);
            cred.setPassword(password);
            if (!updated) cred.setCreatedAt(System.currentTimeMillis());

            credentials.put(cred.getId(), cred);
            persist();

            logger.info("{} web credential for: {}", updated ? "Updated" : "Saved", normalised);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id", cred.getId(),
                    "updated", updated,
                    "siteName", cred.getSiteName()
            ));
        } catch (Exception e) {
            logger.error("❌ Failed to save web credential: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Lookup by URL (returns password for use by browser engine) ───────────

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam String url) {
        try {
            if (url == null || url.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "url param is required"));
            }
            String targetHost = extractHost(url);
            Optional<WebCredential> match = credentials.values().stream()
                    .filter(c -> {
                        String credHost = extractHost(c.getSiteUrl());
                        // Match if the credential host equals or is a suffix of the target host
                        return credHost != null
                                && (targetHost.equals(credHost) || targetHost.endsWith("." + credHost));
                    })
                    .findFirst();

            if (match.isEmpty()) {
                return ResponseEntity.ok(Map.of("found", false, "url", url));
            }

            WebCredential c = match.get();
            return ResponseEntity.ok(Map.of(
                    "found", true,
                    "id", c.getId(),
                    "siteName", c.getSiteName() != null ? c.getSiteName() : "",
                    "siteUrl", c.getSiteUrl(),
                    "username", c.getUsername(),
                    "password", c.getPassword() != null ? c.getPassword() : "",
                    "hasPassword", c.getPassword() != null && !c.getPassword().isBlank()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            WebCredential removed = credentials.remove(id);
            if (removed == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Credential not found: " + id));
            }
            persist();
            logger.info("🗑️ Removed web credential for: {}", removed.getSiteUrl());
            return ResponseEntity.ok(Map.of("success", true, "id", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractHost(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String u = url.trim();
            if (!u.contains("://")) u = "https://" + u;
            return new URI(u).getHost().toLowerCase();
        } catch (Exception e) {
            return url.toLowerCase().replaceAll("https?://", "").split("/")[0];
        }
    }

    private String normaliseUrl(String url) {
        return extractHost(url);
    }

    private String extractSiteName(String url) {
        String host = extractHost(url);
        if (host.isBlank()) return url;
        // e.g. "github.com" → "GitHub.com"
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            String domain = parts[parts.length - 2];
            return Character.toUpperCase(domain.charAt(0)) + domain.substring(1)
                    + "." + parts[parts.length - 1];
        }
        return host;
    }
}
