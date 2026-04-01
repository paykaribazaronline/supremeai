package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

/**
 * Documentation Generation Rules
 * Admin configurable rules for auto-doc generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationRules {
    private String ruleId;
    private String ruleName;
    private boolean enabled = true;
    
    // Root folder rules
    private List<String> allowedInRoot = Arrays.asList(
        "README.md",
        "LICENSE",
        "CODE_OF_CONDUCT.md",
        "ARCHITECTURE_AND_IMPLEMENTATION.md",
        "build.gradle.kts",
        "gradlew",
        "gradlew.bat"
    );
    
    // Documentation categories
    private Map<String, DocCategoryRule> docCategories = new HashMap<>();
    
    // Enforcement level
    private String enforcementLevel; // "STRICT", "WARNING", "INFO"
    
    // Auto-correction
    private boolean autoCorrectPath = true;
    
    // Timestamp
    private long createdAt;
    private long updatedAt;
    private String updatedBy;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocCategoryRule {
        public String categoryName;           // "architecture", "guides", etc
        public String rootPath;               // "docs/02-ARCHITECTURE/"
        public List<String> allowedExtensions = Arrays.asList(".md", ".txt");
        public int maxFileSizeKB = 5000;
        public boolean requireApproval = false;
        public String description;
        
        public DocCategoryRule(String categoryName, String rootPath) {
            this.categoryName = categoryName;
            this.rootPath = rootPath;
        }
    }
    
    public List<String> getAllowedInRoot() {
        return allowedInRoot;
    }
    
    public Map<String, DocCategoryRule> getDocCategories() {
        if (docCategories == null) {
            docCategories = new HashMap<>();
        }
        return docCategories;
    }
}
