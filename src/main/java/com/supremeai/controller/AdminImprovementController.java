package com.supremeai.controller;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.model.ImprovementProposal;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AdminImprovementController - Handles system improvement proposals.
 */
@RestController
@RequestMapping("/api/admin/improvements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImprovementController {

    private static final Logger log = LoggerFactory.getLogger(AdminImprovementController.class);
    private final AdminDashboardService adminDashboardService;

    @Autowired
    public AdminImprovementController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/pending")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getPendingImprovements() {
        return adminDashboardService.getPendingApprovals()
                .collectList()
                .map(pending -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                        "pending", pending,
                        "count", pending.size()
                ))));
    }

    @PostMapping("/approve/{proposalId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> approveProposal(@PathVariable String proposalId) {
        return adminDashboardService.approveProposal(proposalId)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                            "status", "approved",
                            "proposalId", proposalId
                        )));
                    } else {
                        return ResponseEntity.status(404).body(ApiResponse.error("Proposal not found", Map.of(
                            "proposalId", proposalId
                        )));
                    }
                });
    }

    @PostMapping("/reject/{proposalId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> rejectProposal(@PathVariable String proposalId) {
        return adminDashboardService.rejectProposal(proposalId)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                            "status", "rejected",
                            "proposalId", proposalId
                        )));
                    } else {
                        return ResponseEntity.status(404).body(ApiResponse.error("Proposal not found", Map.of(
                            "proposalId", proposalId
                        )));
                    }
                });
    }
}
