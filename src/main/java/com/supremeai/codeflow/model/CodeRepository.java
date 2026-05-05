package com.supremeai.codeflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

/**
 * Firestore document for storing code repository analysis data
 * Schema: codeflow/repositories/{repositoryId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("codeRepository")
public class CodeRepository {
    
    @Id
    private String id;
    
    // Repository identification
    private String name;
    private String fullName;
    private String description;
    private String cloneUrl;
    private String sourceType; // GITHUB, GITLAB, BITBUCKET, LOCAL
    private String sourceId;
    private String defaultBranch;
    
    // Owner information
    private String ownerId;
    private String ownerType; // USER, ORGANIZATION, CUSTOMER
    private String namespace;
    
    // Analysis metadata
    private AnalysisStatus analysisStatus;
    private Instant lastAnalyzedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long analysisDurationMs;
    
    // Health scoring
    private Integer healthScore; // 0-100
    private String healthGrade; // A, B, C, D, F
    private List<HealthIssue> healthIssues;
    
    // File statistics
    private Integer totalFiles;
    private Integer totalLinesOfCode;
    private Integer totalFunctions;
    private Integer totalClasses;
    private Map<String, Integer> languageStats;
    
    // Analysis results
    private List<CodeFile> files;
    private DependencyGraph dependencyGraph;
    private List<PatternDetection> detectedPatterns;
    private List<SecurityIssue> securityIssues;
    private List<DeadCode> deadCode;
    private List<CircularDependency> circularDependencies;
    
    // AI analysis
    private List<AISuggestion> aiSuggestions;
    private List<ErrorAnalysis> errorAnalyses;
    private String lastAnalysisProvider;
    
    // Caching
    private Boolean cached;
    private Instant cacheExpiresAt;
    private String cacheVersion;
    
    // Access control
    private List<String> authorizedUserIds;
    private List<String> authorizedTeamIds;
    private Boolean isPublic;
    
    // GitHub integration
    private GitHubMetadata gitHubMetadata;
    private List<PullRequestAnalysis> pullRequestAnalyses;
    
    // Versioning
    private Integer version;
    private String previousVersionId;
    
    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED, PARTIAL
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HealthIssue {
        private String type;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String description;
        private String file;
        private Integer line;
        private String suggestion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeFile {
        private String path;
        private String name;
        private String extension;
        private String language;
        private Integer size;
        private Integer linesOfCode;
        private Integer complexity;
        private List<FunctionInfo> functions;
        private List<ClassInfo> classes;
        private List<ImportInfo> imports;
        private List<CallReference> callReferences;
        private List<SecurityIssue> securityIssues;
        private Boolean hasEmbeddedScript;
        private String contentHash;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionInfo {
        private String name;
        private Integer line;
        private Integer endLine;
        private List<String> parameters;
        private String returnType;
        private Integer complexity;
        private Integer cyclomaticComplexity;
        private Integer cognitiveComplexity;
        private List<String> calledFunctions;
        private Boolean isPublic;
        private Boolean isStatic;
        private Boolean isAsync;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassInfo {
        private String name;
        private Integer line;
        private String type; // CLASS, INTERFACE, ENUM, ABSTRACT_CLASS
        private List<String> extendsClasses;
        private List<String> implementsInterfaces;
        private List<FunctionInfo> methods;
        private List<String> fields;
        private Integer complexity;
        private Boolean isAbstract;
        private Boolean isFinal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportInfo {
        private String module;
        private String alias;
        private Boolean isUsed;
        private Integer line;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CallReference {
        private String fromFunction;
        private String toFunction;
        private String toFile;
        private Integer line;
        private String type; // DIRECT, DYNAMIC, INHERITED
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DependencyGraph {
        private List<Node> nodes;
        private List<Edge> edges;
        private Map<String, Double> centralityScores;
        private List<String> criticalPath;
        private Integer blastRadius;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Node {
            private String id;
            private String file;
            private String type; // FILE, FUNCTION, CLASS, MODULE
            private Integer linesOfCode;
            private Integer complexity;
            private Integer fanIn;
            private Integer fanOut;
            private Double centralityScore;
            private Map<String, Object> metadata;
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Edge {
            private String source;
            private String target;
            private String type; // CALLS, IMPORTS, EXTENDS, IMPLEMENTS
            private Integer weight;
            private Integer line;
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatternDetection {
        private String patternType; // SINGLETON, FACTORY, OBSERVER, DECORATOR, etc.
        private String description;
        private String file;
        private Integer line;
        private Integer confidence;
        private Map<String, Object> details;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecurityIssue {
        private String type; // HARDCODED_SECRET, SQL_INJECTION, XSS, CSRF, etc.
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String description;
        private String file;
        private Integer line;
        private String codeSnippet;
        private String remediation;
        private String cweId;
        private String owaspCategory;
        private Boolean isFalsePositive;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeadCode {
        private String type; // UNUSED_FUNCTION, UNUSED_IMPORT, UNREACHABLE_CODE
        private String file;
        private Integer line;
        private String name;
        private Integer lastUsedTimestamp;
        private Boolean isExported;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CircularDependency {
        private List<String> files;
        private String description;
        private Integer severity; // 1-10
        private String suggestion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AISuggestion {
        private String type; // REFACTOR, OPTIMIZE, FIX, DOCUMENT
        private String description;
        private String file;
        private Integer line;
        private String suggestion;
        private String codeBefore;
        private String codeAfter;
        private String provider;
        private Integer confidence;
        private Instant generatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorAnalysis {
        private String errorType;
        private String stackTrace;
        private String rootCause;
        private String file;
        private Integer line;
        private List<String> affectedNodes;
        private String suggestedFix;
        private String provider;
        private Integer confidence;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GitHubMetadata {
        private String repoId;
        private String owner;
        private String defaultBranch;
        private Boolean isPrivate;
        private Integer stars;
        private Integer forks;
        private Instant lastPushAt;
        private String language;
        private List<String> topics;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PullRequestAnalysis {
        private String prId;
        private String title;
        private String author;
        private Instant createdAt;
        private Integer changedFiles;
        private Integer additions;
        private Integer deletions;
        private Integer riskScore;
        private List<String> suggestedReviewers;
        private List<String> affectedComponents;
        private String analysisSummary;
    }
}