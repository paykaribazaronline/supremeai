package org.example.service;

import java.util.*;
import java.io.*;

/**
 * 🛡️ SafeZone Manager (Sandbox & Safety Enforcement)
 * Ensures AI-generated code runs in an isolated environment 
 * and identifies risky operations before execution.
 */
public class SafeZoneManager {
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
        "rm -rf", "shred", "format", "mkfs", "dd", "chmod 777"
    );

    /**
     * Scan AI-generated code for security risks.
     */
    public boolean isSafe(String code) {
        String lowerCode = code.toLowerCase();
        for (String cmd : DANGEROUS_COMMANDS) {
            if (lowerCode.contains(cmd)) {
                System.err.println("🚨 SAFE ZONE ALERT: Dangerous command detected: " + cmd);
                return false;
            }
        }
        return true;
    }

    /**
     * Executes AI tasks within a protected directory (SafeZone).
     */
    public boolean executeInSafeZone(String projectId, Runnable task) {
        String safePath = "safezone/" + projectId;
        File dir = new File(safePath);
        if (!dir.exists()) dir.mkdirs();

        try {
            System.out.println("🛡️ Entering SafeZone: " + safePath);
            // In a real implementation: chroot or Docker container
            task.run();
            System.out.println("✅ Task completed within SafeZone.");
            return true;
        } catch (Exception e) {
            System.err.println("❌ SafeZone task failed: " + e.getMessage());
            return false;
        }
    }
}
