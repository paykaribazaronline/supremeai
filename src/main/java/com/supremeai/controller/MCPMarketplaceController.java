package com.supremeai.controller;

import com.supremeai.service.MCPMarketplaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing MCP Marketplace and Autonomous Skill Discovery (King Mode)
 */
@RestController
@RequestMapping("/api/admin/skills")
public class MCPMarketplaceController {
    public MCPMarketplaceController(MCPMarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }



    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingSkills() {
        return ResponseEntity.ok(marketplaceService.getPendingSkills());
    }

    @PostMapping("/{skillId}/approve")
    public ResponseEntity<Map<String, Object>> approveSkill(@PathVariable String skillId) {
        boolean success = marketplaceService.approveDiscoveredSkill(skillId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "স্কিলটি সফলভাবে অনুমোদিত হয়েছে (Approved)।"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "স্কিল অনুমোদন করতে ব্যর্থ হয়েছে।"));
    }

    @PostMapping("/{skillId}/reject")
    public ResponseEntity<Map<String, Object>> rejectSkill(@PathVariable String skillId) {
        boolean success = marketplaceService.rejectDiscoveredSkill(skillId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "স্কিলটি বাতিল করা হয়েছে (Rejected)।"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "স্কিল বাতিল করতে ব্যর্থ হয়েছে।"));
    }
}