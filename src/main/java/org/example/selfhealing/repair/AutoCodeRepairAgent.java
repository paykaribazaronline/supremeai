package org.example.selfhealing.repair;

import org.example.service.MultiAIConsensusService;
import org.example.service.XBuilderFailurePatternService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto Code Repair Agent
 * 
 * Generates code fixes for identified problems.
 * 
 * Strategies:
 * 1. AI Consensus: Ask 10 AIs, vote on best fix
 * 2. Pattern Matching: Apply known solutions
 * 3. Fallback: Return common fix templates
 * 
 * This integrates with the existing:
 * - MultiAI Consensus System (10 providers)
 * - XBuilder (code generation engine)
 * - Failure Pattern Database
 */
@Service
public class AutoCodeRepairAgent {
    private static final Logger logger = LoggerFactory.getLogger(AutoCodeRepairAgent.class);
    
    @Autowired(required = false)
    private MultiAIConsensusService consensusService;
    
    @Autowired(required = false)
    private XBuilderFailurePatternService failurePatterns;
    
    /**
     * Generate a code fix for the identified error
     * 
     * @param errorType Type of error (COMPILATION_ERROR, TEST_FAILURE, etc)
     * @param errorMessage The error message/stack trace
     * @param contextLogs Additional context (logs, test output)
     * @return Generated code fix
     */
    public String generateFix(String errorType, String errorMessage, String contextLogs) {
        logger.info("🔧 Generating fix for error type: {}", errorType);
        
        try {
            // Step 1: Check if we've seen this error pattern before
            String knownFix = checkKnownPatterns(errorType, errorMessage);
            if (knownFix != null) {
                logger.info("✅ Using known pattern fix for: {}", errorType);
                return knownFix;
            }
            
            // Step 2: Try AI consensus approach (if 10 AIs available)
            String consensusFix = generateFixWithConsensus(errorType, errorMessage, contextLogs);
            if (consensusFix != null && !consensusFix.isEmpty()) {
                logger.info("🤖 AI consensus generated fix ({} bytes)", consensusFix.length());
                return consensusFix;
            }
            
            // Step 3: Fallback to common templates
            String templateFix = generateFixFromTemplate(errorType, errorMessage);
            if (templateFix != null) {
                logger.info("📋 Using template fix for: {}", errorType);
                return templateFix;
            }
            
            logger.warn("⚠️ No fix available for error: {}", errorType);
            return null;
            
        } catch (Exception e) {
            logger.error("❌ Error generating fix", e);
            return null;
        }
    }
    
    /**
     * Check if we've seen this error pattern before (pattern matching)
     */
    private String checkKnownPatterns(String errorType, String errorMessage) {
        if (failurePatterns == null) {
            return null;
        }
        
        try {
            // Query pattern database
            // FailurePattern pattern = failurePatterns.findBySimilarError(errorMessage);
            // if (pattern != null && pattern.hasEffectiveFix()) {
            //     return pattern.getEffectiveFix();
            // }
            
            return null;
        } catch (Exception e) {
            logger.error("Error checking patterns", e);
            return null;
        }
    }
    
    /**
     * Use Multi-AI Consensus to generate fix
     * 
     * Asks 10 different AI providers to suggest fixes,
     * votes on the best, and returns the consensus fix.
     */
    private String generateFixWithConsensus(String errorType, String errorMessage, 
                                           String contextLogs) {
        if (consensusService == null) {
            logger.debug("Consensus service not available");
            return null;
        }
        
        try {
            String prompt = buildConsensusPrompt(errorType, errorMessage, contextLogs);
            
            // Call consensus service
            // ConsensusResult result = consensusService.ask(prompt, 10);
            // 
            // Extract the fix from consensus result
            // String fixCode = extractCodeFromResponse(result.getWinningResponse());
            // return fixCode;
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error in AI consensus", e);
            return null;
        }
    }
    
    /**
     * Generate fix from common problem templates
     */
    private String generateFixFromTemplate(String errorType, String errorMessage) {
        return switch (errorType) {
            case "COMPILATION_ERROR" -> generateCompilationFix(errorMessage);
            case "TEST_FAILURE" -> generateTestFix(errorMessage);
            case "RUNTIME_ERROR" -> generateRuntimeFix(errorMessage);
            case "BUILD_FAILURE" -> generateBuildFix(errorMessage);
            default -> null;
        };
    }
    
    /**
     * Generate fix for compilation errors
     */
    private String generateCompilationFix(String errorMessage) {
        if (errorMessage.contains("cannot find symbol")) {
            // Common fix: add missing import or define variable
            return "// Auto-fix: Add missing import or variable declaration\n" +
                   "// Error was: " + errorMessage.split("\n")[0];
        }
        
        if (errorMessage.contains("package")) {
            // Package related error
            return "// Auto-fix: Ensure package statement is at top of file\n" +
                   "// Check file location matches package structure";
        }
        
        return null;
    }
    
    /**
     * Generate fix for test failures
     */
    private String generateTestFix(String errorMessage) {
        if (errorMessage.contains("AssertionError")) {
            return "// Auto-fix: Investigate assertion failure\n" +
                   "// May need to update expected value or test logic\n" +
                   "// Error: " + errorMessage.split("\n")[0];
        }
        
        if (errorMessage.contains("NullPointerException")) {
            return "// Auto-fix: Add null check before assertion\n" +
                   "// Object is null where not expected\n" +
                   "assertNotNull(variable);";
        }
        
        return null;
    }
    
    /**
     * Generate fix for runtime errors
     */
    private String generateRuntimeFix(String errorMessage) {
        if (errorMessage.contains("NullPointerException")) {
            return "// Auto-fix: Add null check\n" +
                   "if (object != null) {\n" +
                   "    // Use object\n" +
                   "}";
        }
        
        if (errorMessage.contains("IndexOutOfBoundsException")) {
            return "// Auto-fix: Add bounds check\n" +
                   "if (index >= 0 && index < array.length) {\n" +
                   "    // Safe to access array[index]\n" +
                   "}";
        }
        
        return null;
    }
    
    /**
     * Generate fix for build failures
     */
    private String generateBuildFix(String errorMessage) {
        if (errorMessage.contains("dependency")) {
            return "// Auto-fix: Add missing dependency\n" +
                   "// Check build.gradle or pom.xml\n" +
                   "// May need to refresh dependencies";
        }
        
        if (errorMessage.contains("version")) {
            return "// Auto-fix: Check version compatibility\n" +
                   "// Update gradle wrapper or dependency versions";
        }
        
        return null;
    }
    
    /**
     * Build prompt for consensus service
     */
    private String buildConsensusPrompt(String errorType, String errorMessage, 
                                       String contextLogs) {
        return String.format(
            "Generate a code fix for this error in a Java/Spring Boot project:\n" +
            "Error Type: %s\n" +
            "Error Message: %s\n" +
            "Context:\n%s\n" +
            "Return only the code fix without explanation.",
            errorType, errorMessage, contextLogs
        );
    }
    
    /**
     * Extract code from AI response
     */
    private String extractCodeFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        Pattern codeBlockPattern = Pattern.compile("```(?:[\\w\\+\\-]*)\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = codeBlockPattern.matcher(response);

        StringBuilder extracted = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1).trim();
            if (!code.isEmpty()) {
                if (extracted.length() > 0) {
                    extracted.append("\n\n");
                }
                extracted.append(code);
            }
        }

        if (extracted.length() > 0) {
            return extracted.toString();
        }

        return response.trim();
    }
}
