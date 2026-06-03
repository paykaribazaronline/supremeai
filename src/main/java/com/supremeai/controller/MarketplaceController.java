package com.supremeai.controller;

import com.supremeai.service.MCPMarketplaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Marketplace Controller - Plan 24 Phase 4
 * Exposes MCP server/skill marketplace endpoints.
 */
@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    public MarketplaceController(MCPMarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }


    private static final Logger logger = LoggerFactory.getLogger(MarketplaceController.class);


    /**
     * GET /api/marketplace/servers
     * List all available MCP servers.
     */
    @GetMapping("/servers")
    public ResponseEntity<List<Map<String, Object>>> listServers() {
        List<MCPMarketplaceService.MCPServerInfo> servers = marketplaceService.getAllServers();

        List<Map<String, Object>> result = servers.stream()
            .map(s -> Map.of(
                "id", s.id,
                "name", s.name,
                "description", s.description,
                "version", s.version,
                "provider", s.provider,
                "installs", s.installCount,
                "rating", s.rating,
                "capabilities", s.capabilities,
                "tags", s.tags,
                "publishedAt", s.publishedAt
            ))
            .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/marketplace/servers/{serverId}
     * Get detailed info about a specific MCP server.
     */
    @GetMapping("/servers/{serverId}")
    public ResponseEntity<Map<String, Object>> getServer(@PathVariable String serverId) {
        MCPMarketplaceService.MCPServerInfo server = marketplaceService.getServer(serverId);

        if (server == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", server.id);
        response.put("name", server.name);
        response.put("description", server.description);
        response.put("version", server.version);
        response.put("provider", server.provider);
        response.put("capabilities", server.capabilities);
        response.put("tools", server.tools);
        response.put("installationCount", server.installCount);
        response.put("rating", server.rating);
        response.put("tags", server.tags);
        response.put("documentationUrl", server.documentationUrl);
        response.put("publishedAt", server.publishedAt);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/marketplace/servers/{serverId}/install
     * Install (enable) an MCP server.
     */
    @PostMapping("/servers/{serverId}/install")
    public ResponseEntity<Map<String, Object>> installServer(@PathVariable String serverId) {
        boolean success = marketplaceService.installServer(serverId);

        if (!success) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to install server"));
        }

        return ResponseEntity.ok(Map.of(
            "status", "INSTALLED",
            "serverId", serverId,
            "message", "Server installed successfully"
        ));
    }

    /**
     * DELETE /api/marketplace/servers/{serverId}/uninstall
     * Uninstall (disable) an MCP server.
     */
    @DeleteMapping("/servers/{serverId}/uninstall")
    public ResponseEntity<Map<String, Object>> uninstallServer(@PathVariable String serverId) {
        boolean success = marketplaceService.uninstallServer(serverId);

        if (!success) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to uninstall server"));
        }

        return ResponseEntity.ok(Map.of(
            "status", "UNINSTALLED",
            "serverId", serverId,
            "message", "Server uninstalled successfully"
        ));
    }

    /**
     * GET /api/marketplace/servers/{serverId}/usage-stats
     * Get usage statistics for a server.
     */
    @GetMapping("/servers/{serverId}/usage-stats")
    public ResponseEntity<Map<String, Object>> getUsageStats(@PathVariable String serverId) {
        Map<String, Object> stats = marketplaceService.getServerUsageStats(serverId);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/marketplace/servers/{serverId}/revenue-stats
     * Get revenue/earnings statistics for a server (if monetized).
     */
    @GetMapping("/servers/{serverId}/revenue-stats")
    public ResponseEntity<Map<String, Object>> getRevenueStats(@PathVariable String serverId) {
        Map<String, Object> stats = marketplaceService.getServerRevenueStats(serverId);

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/marketplace/trending
     * Get trending MCP servers (most installed this week).
     */
    @GetMapping("/trending")
    public ResponseEntity<List<Map<String, Object>>> getTrending() {
        return ResponseEntity.ok(marketplaceService.getTrendingServers());
    }

    /**
     * GET /api/marketplace/top-rated
     * Get top-rated MCP servers.
     */
    @GetMapping("/top-rated")
    public ResponseEntity<List<Map<String, Object>>> getTopRated() {
        return ResponseEntity.ok(marketplaceService.getTopRatedServers());
    }
}
