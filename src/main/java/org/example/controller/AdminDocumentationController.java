package org.example.controller;

import org.example.model.DocumentationRules;
import org.example.service.DocumentationRulesService;
import org.example.service.DocumentationRulesService.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Admin Documentation Rules Controller
 * REST endpoints for admin dashboard to manage doc rules
 */
@RestController
@RequestMapping("/api/admin/doc-rules")
public class AdminDocumentationController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDocumentationController.class);
    
    @Autowired
    private DocumentationRulesService rulesService;
    
    /**
     * GET /api/admin/doc-rules/current
     * Get current documentation rules
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentRules() {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("ruleId", rules.getRuleId());
            response.put("ruleName", rules.getRuleName());
            response.put("enabled", rules.isEnabled());
            response.put("allowedInRoot", rules.getAllowedInRoot());
            response.put("docCategories", rules.getDocCategories());
            response.put("enforcementLevel", rules.getEnforcementLevel());
            response.put("autoCorrectPath", rules.isAutoCorrectPath());
            response.put("updatedAt", new Date(rules.getUpdatedAt()));
            response.put("updatedBy", rules.getUpdatedBy());
            
            logger.info("✅ Admin retrieved current doc rules");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving rules: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/doc-rules/update
     * Update documentation rules (admin only)
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateRules(
            @RequestBody DocumentationRules newRules,
            @RequestParam(defaultValue = "admin") String updatedBy) {
        try {
            newRules.setUpdatedBy(updatedBy);
            rulesService.updateRules("default", newRules);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Updated");
            response.put("ruleId", newRules.getRuleId());
            response.put("timestamp", new Date());
            response.put("updatedBy", updatedBy);
            
            logger.info("✅ Admin updated doc rules by: {}", updatedBy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error updating rules: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/doc-rules/add-category
     * Add new documentation category
     */
    @PostMapping("/add-category")
    public ResponseEntity<Map<String, Object>> addCategory(
            @RequestParam String categoryName,
            @RequestParam String rootPath,
            @RequestParam(defaultValue = "5000") int maxFileSizeKB,
            @RequestParam(defaultValue = "false") boolean requireApproval) {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            DocumentationRules.DocCategoryRule categoryRule = new DocumentationRules.DocCategoryRule(
                categoryName,
                rootPath
            );
            categoryRule.setMaxFileSizeKB(maxFileSizeKB);
            categoryRule.setRequireApproval(requireApproval);
            
            rules.getDocCategories().put(categoryName, categoryRule);
            rulesService.updateRules("default", rules);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Category Added");
            response.put("categoryName", categoryName);
            response.put("rootPath", rootPath);
            response.put("maxFileSizeKB", maxFileSizeKB);
            response.put("requireApproval", requireApproval);
            
            logger.info("✅ New documentation category added: {}", categoryName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error adding category: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/admin/doc-rules/remove-category
     * Remove documentation category
     */
    @DeleteMapping("/remove-category")
    public ResponseEntity<Map<String, Object>> removeCategory(@RequestParam String categoryName) {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            rules.getDocCategories().remove(categoryName);
            rulesService.updateRules("default", rules);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Category Removed");
            response.put("categoryName", categoryName);
            
            logger.info("✅ Documentation category removed: {}", categoryName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error removing category: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/doc-rules/set-enforcement-level
     * Set enforcement level (STRICT, WARNING, INFO)
     */
    @PostMapping("/set-enforcement-level")
    public ResponseEntity<Map<String, Object>> setEnforcementLevel(
            @RequestParam String level) {
        try {
            if (!Arrays.asList("STRICT", "WARNING", "INFO").contains(level)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "❌ Invalid", "message", "Level must be STRICT, WARNING, or INFO"));
            }
            
            DocumentationRules rules = rulesService.getRules("default");
            rules.setEnforcementLevel(level);
            rulesService.updateRules("default", rules);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Updated");
            response.put("enforcementLevel", level);
            response.put("meaning", getEnforcementMeaning(level));
            
            logger.info("✅ Enforcement level set to: {}", level);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error setting enforcement level: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/admin/doc-rules/validate-document
     * Validate a document against current rules
     */
    @PostMapping("/validate-document")
    public ResponseEntity<Map<String, Object>> validateDocument(
            @RequestParam String filepath,
            @RequestParam String category,
            @RequestParam(defaultValue = "100") long fileSizeKB) {
        try {
            ValidationResult result = rulesService.validateDocument(filepath, category, fileSizeKB);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", result.isValid() ? "✅ Valid" : "⚠️ Invalid");
            response.put("valid", result.isValid());
            response.put("filepath", filepath);
            response.put("category", category);
            response.put("fileSizeKB", fileSizeKB);
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("infos", result.getInfos());
            response.put("requiresApproval", result.isRequiresApproval());
            
            if (result.getCorrectedPath() != null) {
                response.put("correctedPath", result.getCorrectedPath());
            }
            
            logger.info("✅ Document validation completed for: {}", filepath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error validating document: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/admin/doc-rules/allowed-in-root
     * Get list of files allowed in root
     */
    @GetMapping("/allowed-in-root")
    public ResponseEntity<Map<String, Object>> getAllowedInRoot() {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("count", rules.getAllowedInRoot().size());
            response.put("files", rules.getAllowedInRoot());
            
            logger.info("✅ Retrieved allowed root files list");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving allowed files: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/admin/doc-rules/categories
     * Get all documentation categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("count", rules.getDocCategories().size());
            response.put("categories", rules.getDocCategories());
            
            logger.info("✅ Retrieved all doc categories: {}", rules.getDocCategories().keySet());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving categories: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/admin/doc-rules/status
     * Get overall rules status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRulesStatus() {
        try {
            DocumentationRules rules = rulesService.getRules("default");
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("enabled", rules.isEnabled());
            response.put("enforcementLevel", rules.getEnforcementLevel());
            response.put("autoCorrectPath", rules.isAutoCorrectPath());
            response.put("totalCategories", rules.getDocCategories().size());
            response.put("totalAllowedInRoot", rules.getAllowedInRoot().size());
            response.put("updatedAt", new Date(rules.getUpdatedAt()));
            response.put("updatedBy", rules.getUpdatedBy());
            
            logger.info("✅ Rules status retrieved");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving status: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    private String getEnforcementMeaning(String level) {
        return switch(level) {
            case "STRICT" -> "Block generation of non-compliant docs";
            case "WARNING" -> "Warn but allow non-compliant docs";
            case "INFO" -> "Only log non-compliant docs";
            default -> "Unknown";
        };
    }
}
