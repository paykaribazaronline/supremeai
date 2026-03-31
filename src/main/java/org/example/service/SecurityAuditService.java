package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * PHASE 8: SECURITY AUDIT SERVICE
 * Orchestrates security scans, compliance validation, and privacy analysis.
 */
@Service
public class SecurityAuditService {
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);

    @Autowired
    private AlphaSecurityAgent securityAgent;
    @Autowired
    private BetaComplianceAgent complianceAgent;
    @Autowired
    private GammaPrivacyAgent privacyAgent;
    @Autowired
    private FirebaseService firebaseService;

    public Map<String, Object> runFullAudit(String projectId) {
        logger.info("🛡️ Initiating Full Security & Compliance Audit for project: {}", projectId);
        
        Map<String, Object> auditReport = new LinkedHashMap<>();
        auditReport.put("projectId", projectId);
        auditReport.put("timestamp", System.currentTimeMillis());

        // 1. Security Scan
        Map<String, Object> securityResults = securityAgent.scanForVulnerabilities(projectId);
        auditReport.put("security", securityResults);

        // 2. Compliance Check
        Map<String, Object> complianceResults = complianceAgent.validateCompliance(projectId);
        auditReport.put("compliance", complianceResults);

        // 3. Privacy Analysis
        Map<String, Object> privacyResults = privacyAgent.analyzePrivacy(projectId);
        auditReport.put("privacy", privacyResults);

        // Calculate Overall Security Health
        int score = (int) securityResults.getOrDefault("security_score", 0);
        auditReport.put("overall_health_score", score);
        
        // Persistence
        saveAuditToFirebase(auditReport);

        logger.info("✓ Full Audit complete for {}. Health Score: {}", projectId, score);
        return auditReport;
    }

    private void saveAuditToFirebase(Map<String, Object> report) {
        // Logic to save to a "security_audits" node in Firebase
        // firebaseService doesn't have a specific method for this yet, so we'll add one or use a generic one
        firebaseService.saveSystemConfig("last_security_audit", report);
    }
}
