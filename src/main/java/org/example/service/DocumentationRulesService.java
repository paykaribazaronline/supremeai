package org.example.service;

import org.example.model.DocumentationRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Documentation Rules Management Service
 * Stores and enforces admin-configured doc rules
 */
@Service
public class DocumentationRulesService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentationRulesService.class);
    
    private static final Map<String, DocumentationRules> rulesStore = new ConcurrentHashMap<>();
    
    /**
     * Initialize default rules
     */
    public DocumentationRulesService() {
        initializeDefaultRules();
    }
    
    private void initializeDefaultRules() {
        DocumentationRules defaultRules = new DocumentationRules();
        defaultRules.setRuleId("default");
        defaultRules.setRuleName("Default Documentation Rules");
        defaultRules.setEnforcementLevel("WARNING");
        defaultRules.setAutoCorrectPath(true);
        defaultRules.setCreatedAt(System.currentTimeMillis());
        
        // Setup categories
        Map<String, DocumentationRules.DocCategoryRule> categories = defaultRules.getDocCategories();
        categories.put("architecture", new DocumentationRules.DocCategoryRule("architecture", "docs/02-ARCHITECTURE/"));
        categories.put("setup", new DocumentationRules.DocCategoryRule("setup", "docs/01-SETUP-DEPLOYMENT/"));
        categories.put("guides", new DocumentationRules.DocCategoryRule("guides", "docs/12-GUIDES/"));
        categories.put("implementation", new DocumentationRules.DocCategoryRule("implementation", "docs/10-IMPLEMENTATION/"));
        categories.put("features", new DocumentationRules.DocCategoryRule("features", "docs/06-FEATURES/"));
        categories.put("troubleshooting", new DocumentationRules.DocCategoryRule("troubleshooting", "docs/09-TROUBLESHOOTING/"));
        categories.put("ci-cd", new DocumentationRules.DocCategoryRule("ci-cd", "docs/08-CI-CD/"));
        categories.put("reports", new DocumentationRules.DocCategoryRule("reports", "docs/13-REPORTS/"));
        
        rulesStore.put("default", defaultRules);
        logger.info("✅ Default documentation rules initialized");
    }
    
    /**
     * Get current rules
     */
    public DocumentationRules getRules(String ruleId) {
        return rulesStore.getOrDefault(ruleId, rulesStore.get("default"));
    }
    
    /**
     * Update rules (admin action)
     */
    public void updateRules(String ruleId, DocumentationRules newRules) {
        newRules.setUpdatedAt(System.currentTimeMillis());
        rulesStore.put(ruleId, newRules);
        logger.info("✅ Documentation rules updated: {}", ruleId);
    }
    
    /**
     * Validate if file can be in root
     */
    public boolean isAllowedInRoot(String filename) {
        DocumentationRules rules = getRules("default");
        return rules.getAllowedInRoot().contains(filename);
    }
    
    /**
     * Get correct path for document
     */
    public String getCorrectPath(String filename, String category) {
        DocumentationRules rules = getRules("default");
        
        // If allowed in root, return as-is
        if (rules.getAllowedInRoot().contains(filename)) {
            return filename;
        }
        
        // Otherwise, get path from category
        DocumentationRules.DocCategoryRule categoryRule = rules.getDocCategories().get(category);
        if (categoryRule != null) {
            return categoryRule.rootPath + filename;
        }
        
        // Default fallback
        return "docs/12-GUIDES/" + filename;
    }
    
    /**
     * Validate document before generation
     */
    public ValidationResult validateDocument(String filepath, String category, long fileSizeKB) {
        DocumentationRules rules = getRules("default");
        ValidationResult result = new ValidationResult();
        
        // Check if root is allowed
        if (filepath.startsWith("./") || filepath.startsWith("/")) {
            String filename = filepath.substring(Math.max(filepath.lastIndexOf("/"), filepath.lastIndexOf("\\")) + 1);
            if (!rules.getAllowedInRoot(filename)) {
                result.addError("File not allowed in root folder: " + filename);
                
                if (rules.isAutoCorrectPath()) {
                    String correctPath = getCorrectPath(filename, category);
                    result.setCorrectedPath(correctPath);
                    result.addWarning("Auto-corrected path to: " + correctPath);
                }
            }
        }
        
        // Check category rules
        DocumentationRules.DocCategoryRule categoryRule = rules.getDocCategories().get(category);
        if (categoryRule != null) {
            if (fileSizeKB > categoryRule.getMaxFileSizeKB()) {
                result.addError("File exceeds max size: " + fileSizeKB + "KB > " + categoryRule.getMaxFileSizeKB() + "KB");
            }
            
            if (categoryRule.isRequireApproval()) {
                result.setRequiresApproval(true);
                result.addInfo("Document requires admin approval before publishing");
            }
        }
        
        return result;
    }
    
    /**
     * Get all rules
     */
    public Collection<DocumentationRules> getAllRules() {
        return rulesStore.values();
    }
    
    /**
     * Delete rule
     */
    public void deleteRule(String ruleId) {
        if (!"default".equals(ruleId)) {
            rulesStore.remove(ruleId);
            logger.info("✅ Documentation rule deleted: {}", ruleId);
        }
    }
    
    // Inner class for validation results
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> infos = new ArrayList<>();
        private String correctedPath;
        private boolean requiresApproval = false;
        
        public void addError(String msg) {
            errors.add(msg);
        }
        
        public void addWarning(String msg) {
            warnings.add(msg);
        }
        
        public void addInfo(String msg) {
            infos.add(msg);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public List<String> getInfos() {
            return infos;
        }
        
        public String getCorrectedPath() {
            return correctedPath;
        }
        
        public void setCorrectedPath(String path) {
            this.correctedPath = path;
        }
        
        public boolean isRequiresApproval() {
            return requiresApproval;
        }
        
        public void setRequiresApproval(boolean requiresApproval) {
            this.requiresApproval = requiresApproval;
        }
    }
}
