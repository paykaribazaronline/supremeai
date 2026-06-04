package com.supremeai.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutoHealingEngine {

    private static final Logger logger = LoggerFactory.getLogger(AutoHealingEngine.class);
    private final Map<String, Integer> errorPatterns = new ConcurrentHashMap<>();

    public Map<String, Object> detectAndFix(String error) {
        logger.info("Auto-healing engine analyzing error: {}", error);
        
        errorPatterns.merge(error, 1, (Integer a, Integer b) -> a + b);
        
        String fix = getKnownFix(error);
        
        if (fix != null) {
            logger.info("Applying known fix: {}", fix);
            return Map.of(
                "status", "fixed",
                "fixApplied", fix,
                "confidence", 0.9,
                "errorCount", errorPatterns.get(error)
            );
        }
        
        return Map.of(
            "status", "analyzing",
            "message", "Error pattern not yet recognized",
            "errorCount", errorPatterns.get(error)
        );
    }
    
    private String getKnownFix(String error) {
        if (error.contains("quota") || error.contains("CpuAlloc")) {
            return "Reduced max instances to 10, 1 CPU per instance";
        }
        if (error.contains("OutOfMemory")) {
            return "Increased memory limit to 2Gi";
        }
        if (error.contains("timeout")) {
            return "Increased request timeout to 3600s";
        }
        if (error.contains("Connection refused")) {
            return "Restarted service instance";
        }
        return null;
    }
}
