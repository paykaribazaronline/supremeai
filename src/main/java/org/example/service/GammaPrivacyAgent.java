package org.supremeai.agents.phase8;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * PHASE 8: GAMMA-PRIVACY AGENT
 * 
 * Analyzes data flows and ensures encryption of sensitive data.
 * Tracks PII (Personally Identifiable Information) through application layers.
 * Validates encryption strength and data protection practices.
 * 
 * Target: All sensitive data encrypted, flows verified
 */
@Service
public class GammaPrivacyAgent {
    private static final Logger logger = LoggerFactory.getLogger(GammaPrivacyAgent.class);

    // Sensitive data patterns
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "ssn", "social_security", "passport", "credit_card", "card_number",
        "password", "api_key", "secret", "token", "private_key",
        "email", "phone", "dob", "birth_date", "address",
        "salary", "bank_account", "medical", "health_record",
        "biometric", "fingerprint", "face_id", "iris"
    );

    /**
     * Analyze data flows and privacy protection
     */
    public Map<String, Object> analyzePrivacy(String projectId, String sourceCode) {
        logger.info("🔐 GammaPrivacyAgent: Starting privacy analysis for project {}", projectId);
        
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("project_id", projectId);
        report.put("agent", "GammaPrivacyAgent");
        report.put("scan_timestamp", System.currentTimeMillis());
        report.put("phase", 8);
        
        // Identify sensitive data fields
        List<Map<String, Object>> sensitiveFields = identifySensitiveData(sourceCode);
        report.put("sensitive_data_fields", sensitiveFields);
        
        // Analyze encryption implementation
        Map<String, Object> encryptionAnalysis = analyzeEncryption(sourceCode);
        report.put("encryption", encryptionAnalysis);
        
        // Analyze data flows
        List<Map<String, Object>> dataFlows = analyzeDataFlows(sourceCode);
        report.put("data_flows", dataFlows);
        
        // Check PII exposure
        List<Map<String, Object>> exposures = checkPIIExposure(sourceCode);
        report.put("potential_exposures", exposures);
        
        // Calculate privacy score
        int score = calculatePrivacyScore(sensitiveFields, encryptionAnalysis, exposures);
        report.put("privacy_score", score);
        report.put("status", score >= 80 ? "PROTECTED" : "AT_RISK");
        report.put("recommendation", generateRecommendation(score));
        
        logger.info("✓ GammaPrivacyAgent analysis complete. Score: {}/100. Status: {}", 
            score, report.get("status"));
        
        return report;
    }

    /**
     * Lightweight analysis without source code
     */
    public Map<String, Object> analyzePrivacy(String projectId) {
        logger.info("🔐 GammaPrivacyAgent: Lightweight analysis for project {}", projectId);
        return analyzePrivacy(projectId, "");
    }

    /**
     * Identify sensitive data fields in code
     */
    private List<Map<String, Object>> identifySensitiveData(String sourceCode) {
        List<Map<String, Object>> fields = new ArrayList<>();
        
        if (sourceCode == null || sourceCode.isEmpty()) {
            return fields;
        }
        
        for (String sensitive : SENSITIVE_FIELDS) {
            Pattern pattern = Pattern.compile(
                "(?:String|int|long|byte\\[\\]|char\\[\\])\\s+" + sensitive + 
                "\\s*[=;]|" + sensitive + "\\s*:\\s*(?:String|number|password)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = pattern.matcher(sourceCode);
            while (matcher.find()) {
                Map<String, Object> field = new LinkedHashMap<>();
                field.put("field_name", sensitive);
                field.put("type", "SENSITIVE_PII");
                field.put("location", matcher.group());
                fields.add(field);
            }
        }
        
        return fields;
    }

    /**
     * Analyze encryption implementation
     */
    private Map<String, Object> analyzeEncryption(String sourceCode) {
        Map<String, Object> encryption = new LinkedHashMap<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        
        // Check for strong encryption algorithms
        if (sourceCode.contains("AES") || sourceCode.contains("AES-256")) {
            strengths.add("AES encryption detected");
        } else if (sourceCode.contains("DES") || sourceCode.contains("RC4")) {
            weaknesses.add("Weak encryption algorithm (DES/RC4)");
        }
        
        // Check for encryption in transit
        if (sourceCode.contains("HTTPS") || sourceCode.contains("TLS") || sourceCode.contains("SSL")) {
            strengths.add("HTTPS/TLS transport encryption");
        } else {
            weaknesses.add("No TLS/SSL encryption found");
        }
        
        // Check for encryption at rest
        if (sourceCode.contains("encryptData") || sourceCode.contains("encryptionKey")) {
            strengths.add("Data-at-rest encryption implemented");
        } else if (sourceCode.contains("password") || sourceCode.contains("secret")) {
            weaknesses.add("Sensitive data may not be encrypted at rest");
        }
        
        // Check for key management
        if (sourceCode.contains("keyManagement") || sourceCode.contains("KeyStore")) {
            strengths.add("Key management system detected");
        } else {
            weaknesses.add("No explicit key management found");
        }
        
        encryption.put("strengths", strengths);
        encryption.put("weaknesses", weaknesses);
        encryption.put("encrypted_fields_count", strengths.size());
        encryption.put("encryption_status", strengths.size() > weaknesses.size() ? "STRONG" : "WEAK");
        
        return encryption;
    }

    /**
     * Trace data flows through application
     */
    private List<Map<String, Object>> analyzeDataFlows(String sourceCode) {
        List<Map<String, Object>> flows = new ArrayList<>();
        
        if (sourceCode == null || sourceCode.isEmpty()) {
            return flows;
        }
        
        // Pattern: Data from input -> processing -> storage/output
        String[] flowPatterns = {
            "getUserInput|getRequest|getPOST",
            "processData|transform|analyze",
            "saveData|storeData|writeDB|sendAPI"
        };
        
        for (String pattern : flowPatterns) {
            if (sourceCode.contains(pattern)) {
                Map<String, Object> flow = new LinkedHashMap<>();
                flow.put("stage", pattern);
                flow.put("present", true);
                flows.add(flow);
            }
        }
        
        // Analyze flow: Input -> Process -> Output
        if (flows.size() >= 2) {
            Map<String, Object> fullFlow = new LinkedHashMap<>();
            fullFlow.put("data_flow_chain", "INPUT -> PROCESS -> OUTPUT");
            fullFlow.put("stages_identified", flows.size());
            flows.add(fullFlow);
        }
        
        return flows;
    }

    /**
     * Check for PII exposure vulnerabilities
     */
    private List<Map<String, Object>> checkPIIExposure(String sourceCode) {
        List<Map<String, Object>> exposures = new ArrayList<>();
        
        if (sourceCode == null || sourceCode.isEmpty()) {
            return exposures;
        }
        
        // Check for logging PII
        Pattern logPII = Pattern.compile(
            "(?:log|console\\.log|println)\\s*\\(.*(?:" + 
            String.join("|", SENSITIVE_FIELDS) + 
            ").*\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = logPII.matcher(sourceCode);
        if (matcher.find()) {
            Map<String, Object> exposure = new LinkedHashMap<>();
            exposure.put("type", "PII_LOGGING");
            exposure.put("severity", "HIGH");
            exposure.put("description", "Sensitive data found in logging statements");
            exposure.put("remediation", "Remove sensitive fields from logs or use masking");
            exposures.add(exposure);
        }
        
        // Check for PII in response
        if (sourceCode.contains("response.send") || sourceCode.contains("return")) {
            for (String sensitive : SENSITIVE_FIELDS) {
                if (sourceCode.contains("return " + sensitive)) {
                    Map<String, Object> exposure = new LinkedHashMap<>();
                    exposure.put("type", "PII_IN_RESPONSE");
                    exposure.put("field", sensitive);
                    exposure.put("severity", "MEDIUM");
                    exposures.add(exposure);
                    break; // Only add once per type
                }
            }
        }
        
        // Check for hardcoded PII
        Pattern hardcodedPII = Pattern.compile(
            "=\\s*['\"]([0-9-]{11,}|[a-z\\.+]+@[a-z\\.]+)['\"]",
            Pattern.CASE_INSENSITIVE
        );
        
        matcher = hardcodedPII.matcher(sourceCode);
        if (matcher.find()) {
            Map<String, Object> exposure = new LinkedHashMap<>();
            exposure.put("type", "HARDCODED_PII");
            exposure.put("severity", "CRITICAL");
            exposure.put("description", "Hardcoded PII values found in source code");
            exposures.add(exposure);
        }
        
        return exposures;
    }

    /**
     * Calculate overall privacy score
     */
    private int calculatePrivacyScore(List<Map<String, Object>> fields, 
                                      Map<String, Object> encryption, 
                                      List<Map<String, Object>> exposures) {
        int score = 100;
        
        // Deduct for sensitive fields without encryption
        score -= Math.min(20, fields.size() * 5);
        
        // Deduct for encryption weaknesses
        List<String> weaknesses = (List<String>) encryption.get("weaknesses");
        score -= Math.min(20, weaknesses.size() * 5);
        
        // Deduct for exposures
        int criticalExposures = (int) exposures.stream()
            .filter(e -> "CRITICAL".equals(e.get("severity")))
            .count();
        score -= criticalExposures * 25;
        
        int mediumExposures = (int) exposures.stream()
            .filter(e -> "MEDIUM".equals(e.get("severity")))
            .count();
        score -= mediumExposures * 10;
        
        return Math.max(0, score);
    }

    private String generateRecommendation(int score) {
        if (score >= 90) return "Privacy protections are excellent. Continue monitoring for compliance.";
        if (score >= 80) return "Good privacy protections. Minor improvements recommended.";
        if (score >= 70) return "Moderate privacy protections. Address identified weaknesses.";
        if (score >= 60) return "Weak privacy protections. Significant improvements needed.";
        return "CRITICAL: Serious privacy risks. Immediate action required before deployment.";
    }

    /**
     * Get privacy analysis for a project
     */
    public Map<String, Object> getPrivacyStatus(String projectId) {
        return analyzePrivacy(projectId);
    }
}
