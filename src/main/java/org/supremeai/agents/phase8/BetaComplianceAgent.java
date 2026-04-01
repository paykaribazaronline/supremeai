package org.supremeai.agents.phase8;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * PHASE 8: BETA-COMPLIANCE AGENT
 * 
 * Validates GDPR, CCPA, and SOC2 compliance requirements.
 * Analyzes data handling, retention, user rights, and audit capabilities.
 * 
 * Target: All standards auto-checked, compliance report generated
 */
@Service
public class BetaComplianceAgent {
    private static final Logger logger = LoggerFactory.getLogger(BetaComplianceAgent.class);

    /**
     * Validate compliance against GDPR, CCPA, SOC2
     */
    public Map<String, Object> validateCompliance(String projectId, String sourceCode) {
        logger.info("📋 BetaComplianceAgent: Starting compliance validation for project {}", projectId);
        
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("project_id", projectId);
        report.put("agent", "BetaComplianceAgent");
        report.put("scan_timestamp", System.currentTimeMillis());
        report.put("phase", 8);
        
        // GDPR Validation
        Map<String, Object> gdprReport = validateGDPR(sourceCode);
        report.put("gdpr", gdprReport);
        
        // CCPA Validation
        Map<String, Object> ccpaReport = validateCCPA(sourceCode);
        report.put("ccpa", ccpaReport);
        
        // SOC2 Validation
        Map<String, Object> soc2Report = validateSOC2(sourceCode);
        report.put("soc2", soc2Report);
        
        // Overall compliance score
        int gdprScore = (int) gdprReport.getOrDefault("score", 0);
        int ccpaScore = (int) ccpaReport.getOrDefault("score", 0);
        int soc2Score = (int) soc2Report.getOrDefault("score", 0);
        int overallScore = (gdprScore + ccpaScore + soc2Score) / 3;
        
        report.put("overall_score", overallScore);
        report.put("compliance_status", overallScore >= 80 ? "COMPLIANT" : "NON_COMPLIANT");
        report.put("issues", collectIssues(gdprReport, ccpaReport, soc2Report));
        
        logger.info("✓ BetaComplianceAgent validation complete. Score: {}/100. Status: {}", 
            overallScore, report.get("compliance_status"));
        
        return report;
    }

    /**
     * Lightweight validation without source code
     */
    public Map<String, Object> validateCompliance(String projectId) {
        logger.info("📋 BetaComplianceAgent: Lightweight validation for project {}", projectId);
        return validateCompliance(projectId, "");
    }

    /**
     * GDPR Compliance Check
     * Articles: 5 (principles), 6 (lawfulness), 21 (right to object), 32 (security)
     */
    private Map<String, Object> validateGDPR(String sourceCode) {
        Map<String, Object> gdpr = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        int passed = 0;
        int total = 8;
        
        // Article 5: Data Protection Principles
        boolean hasDataMinimization = sourceCode.contains("collectMinimalData") || sourceCode.contains("dataMinimization");
        if (hasDataMinimization) passed++; else issues.add("No data minimization found");
        
        // Article 6: Lawful Basis
        boolean hasConsentMechanism = sourceCode.contains("consent") || sourceCode.contains("getUserConsent");
        if (hasConsentMechanism) passed++; else issues.add("No consent mechanism found");
        
        // Article 13: Transparency
        boolean hasPrivacyPolicy = sourceCode.contains("privacyPolicy") || sourceCode.contains("PRIVACY_URL");
        if (hasPrivacyPolicy) passed++; else issues.add("No privacy policy reference found");
        
        // Article 17: Right to Erasure
        boolean hasDeleteMechanism = sourceCode.contains("deleteUser") || sourceCode.contains("deleteData");
        if (hasDeleteMechanism) passed++; else issues.add("No data deletion mechanism found");
        
        // Article 21: Right to Object
        boolean hasObjectionMechanism = sourceCode.contains("objectToProcessing") || sourceCode.contains("optOut");
        if (hasObjectionMechanism) passed++; else issues.add("No opt-out/objection mechanism found");
        
        // Article 32: Security Measures
        boolean hasEncryption = sourceCode.contains("encrypt") || sourceCode.contains("AES");
        if (hasEncryption) passed++; else issues.add("No encryption implementation found");
        
        // Article 33: Data Breach Notification
        boolean hasBreachNotification = sourceCode.contains("notifyBreach") || sourceCode.contains("breachAlert");
        if (hasBreachNotification) passed++; else issues.add("No breach notification mechanism found");
        
        // Article 35: DPIA
        boolean hasDPIA = sourceCode.contains("dataProtectionImpactAssessment") || sourceCode.contains("DPIA");
        if (hasDPIA) passed++; else issues.add("No DPIA documentation found");
        
        gdpr.put("checks_passed", passed);
        gdpr.put("checks_total", total);
        gdpr.put("score", (passed * 100) / total);
        gdpr.put("status", passed >= 6 ? "COMPLIANT" : "NON_COMPLIANT");
        gdpr.put("issues", issues);
        
        return gdpr;
    }

    /**
     * CCPA Compliance Check
     * Key rights: Access, Deletion, Opt-out, Non-discrimination
     */
    private Map<String, Object> validateCCPA(String sourceCode) {
        Map<String, Object> ccpa = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        int passed = 0;
        int total = 6;
        
        // Right to Know
        boolean hasAccessRight = sourceCode.contains("getPersonalData") || sourceCode.contains("dataAccess");
        if (hasAccessRight) passed++; else issues.add("Right to know not implemented");
        
        // Right to Delete
        boolean hasDeleteRight = sourceCode.contains("deletePersonalData") || sourceCode.contains("rightToDelete");
        if (hasDeleteRight) passed++; else issues.add("Right to delete not implemented");
        
        // Right to Opt-Out
        boolean hasOptOut = sourceCode.contains("optOutOfSale") || sourceCode.contains("doNotSell");
        if (hasOptOut) passed++; else issues.add("Right to opt-out not implemented");
        
        // Non-Discrimination
        boolean hasNonDiscrimination = sourceCode.contains("noDiscrimination") || sourceCode.contains("equalPrice");
        if (hasNonDiscrimination) passed++; else issues.add("Non-discrimination clause not found");
        
        // Privacy Notice
        boolean hasNotice = sourceCode.contains("privacyNotice") || sourceCode.contains("CCPA_NOTICE");
        if (hasNotice) passed++; else issues.add("CCPA privacy notice not found");
        
        // Vendor Management
        boolean hasVendorControl = sourceCode.contains("vendorAgreement") || sourceCode.contains("serviceProvider");
        if (hasVendorControl) passed++; else issues.add("Vendor agreements not documented");
        
        ccpa.put("checks_passed", passed);
        ccpa.put("checks_total", total);
        ccpa.put("score", (passed * 100) / total);
        ccpa.put("status", passed >= 5 ? "COMPLIANT" : "NON_COMPLIANT");
        ccpa.put("issues", issues);
        
        return ccpa;
    }

    /**
     * SOC2 Compliance Check
     * Trust Service Criteria: Security, Availability, Processing Integrity, Confidentiality, Privacy
     */
    private Map<String, Object> validateSOC2(String sourceCode) {
        Map<String, Object> soc2 = new LinkedHashMap<>();
        List<String> issues = new ArrayList<>();
        int passed = 0;
        int total = 7;
        
        // Security: Access Controls
        boolean hasAccessControl = sourceCode.contains("accessControl") || sourceCode.contains("authorization");
        if (hasAccessControl) passed++; else issues.add("Access controls not implemented");
        
        // Security: Encryption
        boolean hasEncryption = sourceCode.contains("encrypt") || sourceCode.contains("TLS");
        if (hasEncryption) passed++; else issues.add("Encryption not implemented");
        
        // Availability: Monitoring
        boolean hasMonitoring = sourceCode.contains("monitoring") || sourceCode.contains("alerting");
        if (hasMonitoring) passed++; else issues.add("System monitoring not found");
        
        // Availability: Backup
        boolean hasBackup = sourceCode.contains("backup") || sourceCode.contains("replication");
        if (hasBackup) passed++; else issues.add("Backup strategy not defined");
        
        // Processing Integrity: Change Management
        boolean hasChangeControl = sourceCode.contains("changeManagement") || sourceCode.contains("versionControl");
        if (hasChangeControl) passed++; else issues.add("Change control not documented");
        
        // Confidentiality: Data Classification
        boolean hasDataClassification = sourceCode.contains("dataClassification") || sourceCode.contains("sensitivity");
        if (hasDataClassification) passed++; else issues.add("Data classification not defined");
        
        // Privacy: Data Handling
        boolean hasPrivacyPolicy = sourceCode.contains("privacyPolicy") || sourceCode.contains("dataHandling");
        if (hasPrivacyPolicy) passed++; else issues.add("Privacy policy not documented");
        
        soc2.put("checks_passed", passed);
        soc2.put("checks_total", total);
        soc2.put("score", (passed * 100) / total);
        soc2.put("status", passed >= 6 ? "COMPLIANT" : "NON_COMPLIANT");
        soc2.put("issues", issues);
        
        return soc2;
    }

    private List<Map<String, Object>> collectIssues(Map<String, Object> gdpr, Map<String, Object> ccpa, Map<String, Object> soc2) {
        List<Map<String, Object>> allIssues = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<String> gdprIssues = (List<String>) gdpr.get("issues");
        addIssuesByRegulation(allIssues, "GDPR", gdprIssues);
        
        @SuppressWarnings("unchecked")
        List<String> ccpaIssues = (List<String>) ccpa.get("issues");
        addIssuesByRegulation(allIssues, "CCPA", ccpaIssues);
        
        @SuppressWarnings("unchecked")
        List<String> soc2Issues = (List<String>) soc2.get("issues");
        addIssuesByRegulation(allIssues, "SOC2", soc2Issues);
        
        return allIssues;
    }

    private void addIssuesByRegulation(List<Map<String, Object>> allIssues, String regulation, List<String> issues) {
        for (String issue : issues) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("regulation", regulation);
            item.put("issue", issue);
            allIssues.add(item);
        }
    }

    /**
     * Get compliance status for a project
     */
    public Map<String, Object> getComplianceStatus(String projectId) {
        return validateCompliance(projectId);
    }
}
