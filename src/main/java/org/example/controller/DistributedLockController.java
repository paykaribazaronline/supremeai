package org.example.controller;

import org.example.service.DistributedLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Distributed Lock Management REST API
 * Provides endpoints for monitoring and managing distributed locks
 */
@RestController
@RequestMapping("/api/locks")
public class DistributedLockController {

    @Autowired
    private DistributedLockService lockService;

    /**
     * Get information about a specific lock
     */
    @GetMapping("/{lockKey}")
    public Map<String, Object> getLockInfo(@PathVariable String lockKey) {
        return lockService.getLockInfo(lockKey);
    }

    /**
     * Check if a lock is currently held
     */
    @GetMapping("/{lockKey}/status")
    public Map<String, Object> checkLockStatus(@PathVariable String lockKey) {
        return Map.of(
            "lockKey", lockKey,
            "isLocked", lockService.isLocked(lockKey),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Get all active locks
     */
    @GetMapping("/active")
    public Map<String, Object> getAllActiveLocks() {
        return Map.of(
            "activeLocks", lockService.getAllActiveLocks(),
            "count", lockService.getAllActiveLocks().size(),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Acquire a lock
     */
    @PostMapping("/{lockKey}/acquire")
    public Map<String, Object> acquireLock(
            @PathVariable String lockKey,
            @RequestParam(defaultValue = "5000") long timeoutMs,
            @RequestParam String ownerName) {
        String lockToken = lockService.acquireLock(lockKey, timeoutMs, ownerName);
        return Map.of(
            "success", lockToken != null,
            "lockKey", lockKey,
            "lockToken", lockToken != null ? lockToken : "FAILED",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Release a lock
     */
    @PostMapping("/{lockKey}/release")
    public Map<String, Object> releaseLock(
            @PathVariable String lockKey,
            @RequestParam String lockToken) {
        boolean released = lockService.releaseLock(lockKey, lockToken);
        return Map.of(
            "success", released,
            "lockKey", lockKey,
            "message", released ? "Lock released successfully" : "Lock token mismatch",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Get lock statistics and monitoring dashboard
     */
    @GetMapping("/stats")
    public Map<String, Object> getLockStats() {
        List<Map<String, Object>> activeLocks = lockService.getAllActiveLocks();
        return Map.of(
            "totalActiveLocks", activeLocks.size(),
            "activeLocks", activeLocks,
            "lockMetrics", Map.of(
                "maxLocks", 1000,
                "currentLocks", activeLocks.size(),
                "utilizationPercent", (activeLocks.size() * 100.0) / 1000.0
            ),
            "timestamp", System.currentTimeMillis()
        );
    }
}
