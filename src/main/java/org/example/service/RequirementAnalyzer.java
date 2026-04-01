package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

/**
 * Requirement Analyzer
 * Parses requirements → identifies what code needs to be generated
 */
@Service
public class RequirementAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(RequirementAnalyzer.class);
    
    public static class Requirement {
        public String name;
        public String description;
        public String type; // SERVICE, CONTROLLER, MODEL, UTILITY
        public List<String> dependencies = new ArrayList<>();
        public Map<String, String> methods = new HashMap<>(); // name -> signature
        public Map<String, Object> metadata = new HashMap<>();
        
        @Override
        public String toString() {
            return String.format("%s (%s): %s", name, type, description);
        }
    }
    
    /**
     * Analyze requirement string and extract components
     */
    public Requirement analyze(String requirementText) {
        Requirement req = new Requirement();
        
        // Extract requirement name
        req.name = extractName(requirementText);
        req.type = detectType(requirementText);
        req.description = requirementText;
        
        // Extract dependencies
        req.dependencies = extractDependencies(requirementText);
        
        // Extract methods needed
        req.methods = extractMethods(requirementText);
        
        logger.info("✅ Analyzed requirement: {} ({})", req.name, req.type);
        return req;
    }
    
    /**
     * Extract component name from requirement
     */
    private String extractName(String req) {
        // Pattern: "create XyzService" or "add XyzController" or "build XyzModel"
        Pattern p = Pattern.compile("(?:create|add|build|implement)\\s+([A-Za-z]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(req);
        if (m.find()) {
            return m.group(1);
        }
        
        // Fallback: first capitalized word
        String[] words = req.split("\\s+");
        for (String word : words) {
            if (word.length() > 2 && Character.isUpperCase(word.charAt(0))) {
                return word;
            }
        }
        
        return "Feature";
    }
    
    /**
     * Detect if requirement is for SERVICE, CONTROLLER, MODEL, or UTILITY
     */
    private String detectType(String req) {
        req = req.toLowerCase();
        
        if (req.contains("service") || req.contains("process") || req.contains("business logic")) {
            return "SERVICE";
        }
        if (req.contains("endpoint") || req.contains("api") || req.contains("controller") || req.contains("rest")) {
            return "CONTROLLER";
        }
        if (req.contains("model") || req.contains("data") || req.contains("entity") || req.contains("class")) {
            return "MODEL";
        }
        if (req.contains("utility") || req.contains("helper") || req.contains("tool")) {
            return "UTILITY";
        }
        
        return "SERVICE"; // Default
    }
    
    /**
     * Extract dependencies (other services needed)
     */
    private List<String> extractDependencies(String req) {
        List<String> deps = new ArrayList<>();
        
        // Pattern: "needs X" or "uses Y" or "depends on Z"
        Pattern p = Pattern.compile("(?:needs?|uses?|depends on|requires)\\s+([A-Za-z]+Service)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(req);
        
        while (m.find()) {
            deps.add(m.group(1));
        }
        
        return deps;
    }
    
    /**
     * Extract method signatures from requirement
     */
    private Map<String, String> extractMethods(String req) {
        Map<String, String> methods = new HashMap<>();
        
        // Look for patterns like "method: doX, doY"
        Pattern p = Pattern.compile("(?:method|methods|endpoint|endpoints):\\s*([^.]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(req);
        
        if (m.find()) {
            String[] methodNames = m.group(1).split(",");
            for (String method : methodNames) {
                String methodName = method.trim();
                methods.put(methodName, "void " + methodName + "()");
            }
        }
        
        return methods;
    }
    
    /**
     * Check if requirement is critical (admin requirement)
     */
    public boolean isCritical(String req) {
        String lower = req.toLowerCase();
        return lower.contains("critical") || 
               lower.contains("must") || 
               lower.contains("admin") ||
               lower.contains("security") ||
               lower.contains("required");
    }
}
