package com.supremeai.codeflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("codeRepository")
public class CodeRepository {
    
    @Id
    private String id;
    private String name;
    private String fullName;
    private String description;
    private String cloneUrl;
    private String sourceType;
    private String sourceId;
    private String defaultBranch;
    private String ownerId;
    private String ownerType;
    private String namespace;
    private AnalysisStatus analysisStatus;
    private Instant lastAnalyzedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long analysisDurationMs;
    private Integer healthScore;
    private String healthGrade;
    private List<HealthIssue> healthIssues;
    private Integer totalFiles;
    private Integer totalLinesOfCode;
    private Integer totalFunctions;
    private Integer totalClasses;
    private Map<String, Integer> languageStats;
    private List<CodeFile> files;
    private DependencyGraph dependencyGraph;
    private List<PatternDetection> detectedPatterns;
    private List<SecurityIssue> securityIssues;
    private List<DeadCode> deadCode;
    private List<CircularDependency> circularDependencies;
    private List<AISuggestion> aiSuggestions;
    private List<ErrorAnalysis> errorAnalyses;
    private String lastAnalysisProvider;
    private Boolean cached;
    private Instant cacheExpiresAt;
    private String cacheVersion;
    private List<String> authorizedUserIds;
    private List<String> authorizedTeamIds;
    private Boolean isPublic;
    private GitHubMetadata gitHubMetadata;
    private List<PullRequestAnalysis> pullRequestAnalyses;
    private Integer version;
    private String previousVersionId;

    public CodeRepository() {}

    public CodeRepository(String id, String name, String fullName, String description, String cloneUrl, String sourceType, String sourceId, String defaultBranch, String ownerId, String ownerType, String namespace, AnalysisStatus analysisStatus, Instant lastAnalyzedAt, Instant createdAt, Instant updatedAt, Long analysisDurationMs, Integer healthScore, String healthGrade, List<HealthIssue> healthIssues, Integer totalFiles, Integer totalLinesOfCode, Integer totalFunctions, Integer totalClasses, Map<String, Integer> languageStats, List<CodeFile> files, DependencyGraph dependencyGraph, List<PatternDetection> detectedPatterns, List<SecurityIssue> securityIssues, List<DeadCode> deadCode, List<CircularDependency> circularDependencies, List<AISuggestion> aiSuggestions, List<ErrorAnalysis> errorAnalyses, String lastAnalysisProvider, Boolean cached, Instant cacheExpiresAt, String cacheVersion, List<String> authorizedUserIds, List<String> authorizedTeamIds, Boolean isPublic, GitHubMetadata gitHubMetadata, List<PullRequestAnalysis> pullRequestAnalyses, Integer version, String previousVersionId) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.cloneUrl = cloneUrl;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.defaultBranch = defaultBranch;
        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.namespace = namespace;
        this.analysisStatus = analysisStatus;
        this.lastAnalyzedAt = lastAnalyzedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.analysisDurationMs = analysisDurationMs;
        this.healthScore = healthScore;
        this.healthGrade = healthGrade;
        this.healthIssues = healthIssues;
        this.totalFiles = totalFiles;
        this.totalLinesOfCode = totalLinesOfCode;
        this.totalFunctions = totalFunctions;
        this.totalClasses = totalClasses;
        this.languageStats = languageStats;
        this.files = files;
        this.dependencyGraph = dependencyGraph;
        this.detectedPatterns = detectedPatterns;
        this.securityIssues = securityIssues;
        this.deadCode = deadCode;
        this.circularDependencies = circularDependencies;
        this.aiSuggestions = aiSuggestions;
        this.errorAnalyses = errorAnalyses;
        this.lastAnalysisProvider = lastAnalysisProvider;
        this.cached = cached;
        this.cacheExpiresAt = cacheExpiresAt;
        this.cacheVersion = cacheVersion;
        this.authorizedUserIds = authorizedUserIds;
        this.authorizedTeamIds = authorizedTeamIds;
        this.isPublic = isPublic;
        this.gitHubMetadata = gitHubMetadata;
        this.pullRequestAnalyses = pullRequestAnalyses;
        this.version = version;
        this.previousVersionId = previousVersionId;
    }

    public static CodeRepositoryBuilder builder() { return new CodeRepositoryBuilder(); }

    public static class CodeRepositoryBuilder {
        private String id; private String name; private String fullName; private String description;
        private String cloneUrl; private String sourceType; private String sourceId;
        private String defaultBranch; private String ownerId; private String ownerType;
        private String namespace; private AnalysisStatus analysisStatus;
        private Instant lastAnalyzedAt; private Instant createdAt; private Instant updatedAt;
        private Long analysisDurationMs; private Integer healthScore; private String healthGrade;
        private List<HealthIssue> healthIssues; private Integer totalFiles; private Integer totalLinesOfCode;
        private Integer totalFunctions; private Integer totalClasses; private Map<String, Integer> languageStats;
        private List<CodeFile> files; private DependencyGraph dependencyGraph; private List<PatternDetection> detectedPatterns;
        private List<SecurityIssue> securityIssues; private List<DeadCode> deadCode; private List<CircularDependency> circularDependencies;
        private List<AISuggestion> aiSuggestions; private List<ErrorAnalysis> errorAnalyses;
        private String lastAnalysisProvider; private Boolean cached; private Instant cacheExpiresAt;
        private String cacheVersion; private List<String> authorizedUserIds; private List<String> authorizedTeamIds;
        private Boolean isPublic; private GitHubMetadata gitHubMetadata; private List<PullRequestAnalysis> pullRequestAnalyses;
        private Integer version; private String previousVersionId;

        public CodeRepositoryBuilder id(String id) { this.id = id; return this; }
        public CodeRepositoryBuilder name(String name) { this.name = name; return this; }
        public CodeRepositoryBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public CodeRepositoryBuilder description(String description) { this.description = description; return this; }
        public CodeRepositoryBuilder cloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; return this; }
        public CodeRepositoryBuilder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public CodeRepositoryBuilder sourceId(String sourceId) { this.sourceId = sourceId; return this; }
        public CodeRepositoryBuilder defaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; return this; }
        public CodeRepositoryBuilder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public CodeRepositoryBuilder ownerType(String ownerType) { this.ownerType = ownerType; return this; }
        public CodeRepositoryBuilder namespace(String namespace) { this.namespace = namespace; return this; }
        public CodeRepositoryBuilder analysisStatus(AnalysisStatus analysisStatus) { this.analysisStatus = analysisStatus; return this; }
        public CodeRepositoryBuilder lastAnalyzedAt(Instant lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; return this; }
        public CodeRepositoryBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public CodeRepositoryBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public CodeRepositoryBuilder analysisDurationMs(Long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; return this; }
        public CodeRepositoryBuilder healthScore(Integer healthScore) { this.healthScore = healthScore; return this; }
        public CodeRepositoryBuilder healthGrade(String healthGrade) { this.healthGrade = healthGrade; return this; }
        public CodeRepositoryBuilder healthIssues(List<HealthIssue> healthIssues) { this.healthIssues = healthIssues; return this; }
        public CodeRepositoryBuilder totalFiles(Integer totalFiles) { this.totalFiles = totalFiles; return this; }
        public CodeRepositoryBuilder totalLinesOfCode(Integer totalLinesOfCode) { this.totalLinesOfCode = totalLinesOfCode; return this; }
        public CodeRepositoryBuilder totalFunctions(Integer totalFunctions) { this.totalFunctions = totalFunctions; return this; }
        public CodeRepositoryBuilder totalClasses(Integer totalClasses) { this.totalClasses = totalClasses; return this; }
        public CodeRepositoryBuilder languageStats(Map<String, Integer> languageStats) { this.languageStats = languageStats; return this; }
        public CodeRepositoryBuilder files(List<CodeFile> files) { this.files = files; return this; }
        public CodeRepositoryBuilder dependencyGraph(DependencyGraph dependencyGraph) { this.dependencyGraph = dependencyGraph; return this; }
        public CodeRepositoryBuilder detectedPatterns(List<PatternDetection> detectedPatterns) { this.detectedPatterns = detectedPatterns; return this; }
        public CodeRepositoryBuilder securityIssues(List<SecurityIssue> securityIssues) { this.securityIssues = securityIssues; return this; }
        public CodeRepositoryBuilder deadCode(List<DeadCode> deadCode) { this.deadCode = deadCode; return this; }
        public CodeRepositoryBuilder circularDependencies(List<CircularDependency> circularDependencies) { this.circularDependencies = circularDependencies; return this; }
        public CodeRepositoryBuilder aiSuggestions(List<AISuggestion> aiSuggestions) { this.aiSuggestions = aiSuggestions; return this; }
        public CodeRepositoryBuilder errorAnalyses(List<ErrorAnalysis> errorAnalyses) { this.errorAnalyses = errorAnalyses; return this; }
        public CodeRepositoryBuilder lastAnalysisProvider(String lastAnalysisProvider) { this.lastAnalysisProvider = lastAnalysisProvider; return this; }
        public CodeRepositoryBuilder cached(Boolean cached) { this.cached = cached; return this; }
        public CodeRepositoryBuilder cacheExpiresAt(Instant cacheExpiresAt) { this.cacheExpiresAt = cacheExpiresAt; return this; }
        public CodeRepositoryBuilder cacheVersion(String cacheVersion) { this.cacheVersion = cacheVersion; return this; }
        public CodeRepositoryBuilder authorizedUserIds(List<String> authorizedUserIds) { this.authorizedUserIds = authorizedUserIds; return this; }
        public CodeRepositoryBuilder authorizedTeamIds(List<String> authorizedTeamIds) { this.authorizedTeamIds = authorizedTeamIds; return this; }
        public CodeRepositoryBuilder isPublic(Boolean isPublic) { this.isPublic = isPublic; return this; }
        public CodeRepositoryBuilder gitHubMetadata(GitHubMetadata gitHubMetadata) { this.gitHubMetadata = gitHubMetadata; return this; }
        public CodeRepositoryBuilder pullRequestAnalyses(List<PullRequestAnalysis> pullRequestAnalyses) { this.pullRequestAnalyses = pullRequestAnalyses; return this; }
        public CodeRepositoryBuilder version(Integer version) { this.version = version; return this; }
        public CodeRepositoryBuilder previousVersionId(String previousVersionId) { this.previousVersionId = previousVersionId; return this; }

        public CodeRepository build() {
            return new CodeRepository(id, name, fullName, description, cloneUrl, sourceType, sourceId, defaultBranch, ownerId, ownerType, namespace, analysisStatus, lastAnalyzedAt, createdAt, updatedAt, analysisDurationMs, healthScore, healthGrade, healthIssues, totalFiles, totalLinesOfCode, totalFunctions, totalClasses, languageStats, files, dependencyGraph, detectedPatterns, securityIssues, deadCode, circularDependencies, aiSuggestions, errorAnalyses, lastAnalysisProvider, cached, cacheExpiresAt, cacheVersion, authorizedUserIds, authorizedTeamIds, isPublic, gitHubMetadata, pullRequestAnalyses, version, previousVersionId);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public AnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(AnalysisStatus analysisStatus) { this.analysisStatus = analysisStatus; }
    public Instant getLastAnalyzedAt() { return lastAnalyzedAt; }
    public void setLastAnalyzedAt(Instant lastAnalyzedAt) { this.lastAnalyzedAt = lastAnalyzedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(Long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    public String getHealthGrade() { return healthGrade; }
    public void setHealthGrade(String healthGrade) { this.healthGrade = healthGrade; }
    public List<HealthIssue> getHealthIssues() { return healthIssues; }
    public void setHealthIssues(List<HealthIssue> healthIssues) { this.healthIssues = healthIssues; }
    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
    public Integer getTotalLinesOfCode() { return totalLinesOfCode; }
    public void setTotalLinesOfCode(Integer totalLinesOfCode) { this.totalLinesOfCode = totalLinesOfCode; }
    public Integer getTotalFunctions() { return totalFunctions; }
    public void setTotalFunctions(Integer totalFunctions) { this.totalFunctions = totalFunctions; }
    public Integer getTotalClasses() { return totalClasses; }
    public void setTotalClasses(Integer totalClasses) { this.totalClasses = totalClasses; }
    public Map<String, Integer> getLanguageStats() { return languageStats; }
    public void setLanguageStats(Map<String, Integer> languageStats) { this.languageStats = languageStats; }
    public List<CodeFile> getFiles() { return files; }
    public void setFiles(List<CodeFile> files) { this.files = files; }
    public DependencyGraph getDependencyGraph() { return dependencyGraph; }
    public void setDependencyGraph(DependencyGraph dependencyGraph) { this.dependencyGraph = dependencyGraph; return; }
    public List<PatternDetection> getDetectedPatterns() { return detectedPatterns; }
    public void setDetectedPatterns(List<PatternDetection> detectedPatterns) { this.detectedPatterns = detectedPatterns; }
    public List<SecurityIssue> getSecurityIssues() { return securityIssues; }
    public void setSecurityIssues(List<SecurityIssue> securityIssues) { this.securityIssues = securityIssues; }
    public List<DeadCode> getDeadCode() { return deadCode; }
    public void setDeadCode(List<DeadCode> deadCode) { this.deadCode = deadCode; }
    public List<CircularDependency> getCircularDependencies() { return circularDependencies; }
    public void setCircularDependencies(List<CircularDependency> circularDependencies) { this.circularDependencies = circularDependencies; }
    public List<AISuggestion> getAiSuggestions() { return aiSuggestions; }
    public void setAiSuggestions(List<AISuggestion> aiSuggestions) { this.aiSuggestions = aiSuggestions; }
    public List<ErrorAnalysis> getErrorAnalyses() { return errorAnalyses; }
    public void setErrorAnalyses(List<ErrorAnalysis> errorAnalyses) { this.errorAnalyses = errorAnalyses; }
    public String getLastAnalysisProvider() { return lastAnalysisProvider; }
    public void setLastAnalysisProvider(String lastAnalysisProvider) { this.lastAnalysisProvider = lastAnalysisProvider; }
    public Boolean getCached() { return cached; }
    public void setCached(Boolean cached) { this.cached = cached; }
    public Instant getCacheExpiresAt() { return cacheExpiresAt; }
    public void setCacheExpiresAt(Instant cacheExpiresAt) { this.cacheExpiresAt = cacheExpiresAt; }
    public String getCacheVersion() { return cacheVersion; }
    public void setCacheVersion(String cacheVersion) { this.cacheVersion = cacheVersion; }
    public List<String> getAuthorizedUserIds() { return authorizedUserIds; }
    public void setAuthorizedUserIds(List<String> authorizedUserIds) { this.authorizedUserIds = authorizedUserIds; }
    public List<String> getAuthorizedTeamIds() { return authorizedTeamIds; }
    public void setAuthorizedTeamIds(List<String> authorizedTeamIds) { this.authorizedTeamIds = authorizedTeamIds; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    public GitHubMetadata getGitHubMetadata() { return gitHubMetadata; }
    public void setGitHubMetadata(GitHubMetadata gitHubMetadata) { this.gitHubMetadata = gitHubMetadata; }
    public List<PullRequestAnalysis> getPullRequestAnalyses() { return pullRequestAnalyses; }
    public void setPullRequestAnalyses(List<PullRequestAnalysis> pullRequestAnalyses) { this.pullRequestAnalyses = pullRequestAnalyses; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getPreviousVersionId() { return previousVersionId; }
    public void setPreviousVersionId(String previousVersionId) { this.previousVersionId = previousVersionId; }
    
    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED, PARTIAL
    }
    
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
        private String content;

        public CodeFile() {}
        public String getPath() { return path; }
        public void setPath(String p) { this.path = p; }
        public String getLanguage() { return language; }
        public void setLanguage(String l) { this.language = l; }
        public List<ClassInfo> getClasses() { return classes; }
        public void setClasses(List<ClassInfo> c) { this.classes = c; }
        public List<FunctionInfo> getFunctions() { return functions; }
        public void setFunctions(List<FunctionInfo> f) { this.functions = f; }
        public List<ImportInfo> getImports() { return imports; }
        public void setImports(List<ImportInfo> i) { this.imports = i; }
        public String getContent() { return content; }
        public void setContent(String c) { this.content = c; }
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public void setExtension(String e) { this.extension = e; }
        public void setSize(Integer s) { this.size = s; }
        public void setLinesOfCode(Integer l) { this.linesOfCode = l; }
        public void setComplexity(Integer c) { this.complexity = c; }
        public void setCallReferences(List<CallReference> cr) { this.callReferences = cr; }
        public void setSecurityIssues(List<SecurityIssue> si) { this.securityIssues = si; }
        public void setHasEmbeddedScript(Boolean hes) { this.hasEmbeddedScript = hes; }
        public void setContentHash(String ch) { this.contentHash = ch; }
        public List<CallReference> getCallReferences() { return callReferences; }
        public Integer getLinesOfCode() { return linesOfCode; }
        public Integer getComplexity() { return complexity; }
    }
    
    public static class FunctionInfo {
        public String name;
        public Integer line;
        public Integer endLine;
        public List<String> parameters;
        public String returnType;
        public Integer complexity;
        public Integer cyclomaticComplexity;
        public Integer cognitiveComplexity;
        public List<String> calledFunctions;
        public Boolean isPublic;
        public Boolean isPrivate;
        public Boolean isStatic;
        public Boolean isAsync;
        public List<String> modifiers;

        public FunctionInfo() {}
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public Boolean isPrivate() { return isPrivate; }
        public void setIsPrivate(Boolean ip) { this.isPrivate = ip; }
        public Integer getLine() { return line; }
        public void setLine(Integer l) { this.line = l; }
        public void setEndLine(Integer el) { this.endLine = el; }
        public void setParameters(List<String> p) { this.parameters = p; }
        public void setReturnType(String rt) { this.returnType = rt; }
        public void setComplexity(Integer c) { this.complexity = c; }
        public void setCyclomaticComplexity(Integer cc) { this.cyclomaticComplexity = cc; }
        public void setCognitiveComplexity(Integer cc) { this.cognitiveComplexity = cc; }
        public void setCalledFunctions(List<String> cf) { this.calledFunctions = cf; }
        public void setIsPublic(Boolean ip) { this.isPublic = ip; }
        public void setIsStatic(Boolean is) { this.isStatic = is; }
        public void setIsAsync(Boolean ia) { this.isAsync = ia; }
        public void setModifiers(List<String> m) { this.modifiers = m; }
        public List<String> getCalledFunctions() { return calledFunctions; }
        public String getReturnType() { return returnType; }
    }
    
    public static class ClassInfo {
        public String name;
        public Integer line;
        public String type;
        public List<String> extendsClasses;
        public List<String> implementsInterfaces;
        public List<FunctionInfo> methods;
        public List<String> fields;
        public Integer complexity;
        public Boolean isAbstract;
        public Boolean isFinal;

        public ClassInfo() {}
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public Integer getLine() { return line; }
        public void setLine(Integer l) { this.line = l; }
        public List<FunctionInfo> getMethods() { return methods; }
        public void setMethods(List<FunctionInfo> m) { this.methods = m; }
        public List<String> getImplementsInterfaces() { return implementsInterfaces; }
        public void setImplementsInterfaces(List<String> ii) { this.implementsInterfaces = ii; }
        public List<String> getFields() { return fields; }
        public void setFields(List<String> f) { this.fields = f; }
        public Integer getComplexity() { return complexity; }
        public void setComplexity(Integer c) { this.complexity = c; }
        public void setType(String t) { this.type = t; }
        public void setExtendsClasses(List<String> ec) { this.extendsClasses = ec; }
        public void setIsAbstract(Boolean ia) { this.isAbstract = ia; }
        public void setIsFinal(Boolean ifn) { this.isFinal = ifn; }

        public static ClassInfoBuilder builder() { return new ClassInfoBuilder(); }
        public static class ClassInfoBuilder {
            private String name; private Integer line;
            public ClassInfoBuilder name(String n) { this.name = n; return this; }
            public ClassInfoBuilder line(Integer l) { this.line = l; return this; }
            public ClassInfo build() {
                ClassInfo ci = new ClassInfo(); ci.name = this.name; ci.line = this.line; return ci;
            }
        }
    }

    public static class DeadCode {
        private String type;
        private String file;
        private Integer line;
        private String name;
        private Integer lastUsedTimestamp;
        private Boolean isExported;

        public DeadCode() {}
        public void setType(String t) { this.type = t; }
        public void setFile(String f) { this.file = f; }
        public void setLine(Integer l) { this.line = l; }
        public void setName(String n) { this.name = n; }
        public void setIsExported(Boolean ie) { this.isExported = ie; }
        public String getType() { return type; }

        public static DeadCodeBuilder builder() { return new DeadCodeBuilder(); }
        public static class DeadCodeBuilder {
            private String type; private String file; private Integer line; private String name; private boolean isExported;
            public DeadCodeBuilder type(String t) { this.type = t; return this; }
            public DeadCodeBuilder file(String f) { this.file = f; return this; }
            public DeadCodeBuilder line(Integer l) { this.line = l; return this; }
            public DeadCodeBuilder name(String n) { this.name = n; return this; }
            public DeadCodeBuilder isExported(boolean ie) { this.isExported = ie; return this; }
            public DeadCode build() {
                DeadCode dc = new DeadCode();
                dc.type = this.type; dc.file = this.file; dc.line = this.line; dc.name = this.name; dc.isExported = this.isExported;
                return dc;
            }
        }
    }

    public static class ImportInfo {
        public String module;
        public String alias;
        public Boolean isUsed;
        public Integer line;
        public ImportInfo() {}
        public String getModule() { return module; }
        public void setModule(String m) { this.module = m; }
        public void setAlias(String a) { this.alias = a; }
        public void setIsUsed(Boolean iu) { this.isUsed = iu; }
        public void setLine(Integer l) { this.line = l; }
        public Boolean getIsUsed() { return isUsed; }
        public Integer getLine() { return line; }
    }

    public static class PatternDetection {
        private String patternType;
        private String description;
        private String file;
        private Integer line;
        private Integer confidence;
        private Map<String, Object> details;
        public PatternDetection() {}
        public void setPatternType(String pt) { this.patternType = pt; }
        public void setDescription(String d) { this.description = d; }
        public void setFile(String f) { this.file = f; }
        public void setLine(Integer l) { this.line = l; }
        public void setConfidence(Integer c) { this.confidence = c; }
        public void setDetails(Map<String, Object> d) { this.details = d; }
        public String getPatternType() { return patternType; }

        public static PatternDetectionBuilder builder() { return new PatternDetectionBuilder(); }
        public static class PatternDetectionBuilder {
            private String patternType; private String description; private String file; private Integer line; private Integer confidence;
            public PatternDetectionBuilder patternType(String pt) { this.patternType = pt; return this; }
            public PatternDetectionBuilder description(String d) { this.description = d; return this; }
            public PatternDetectionBuilder file(String f) { this.file = f; return this; }
            public PatternDetectionBuilder line(Integer l) { this.line = l; return this; }
            public PatternDetectionBuilder confidence(Integer c) { this.confidence = c; return this; }
            public PatternDetection build() {
                PatternDetection pd = new PatternDetection();
                pd.patternType = this.patternType; pd.description = this.description; pd.file = this.file; pd.line = this.line; pd.confidence = this.confidence;
                return pd;
            }
        }
    }

    public static class HealthIssue {
        private String type;
        private String severity;
        private String description;
        private String file;
        private Integer line;
        private String suggestion;
        public HealthIssue() {}
        public String getFile() { return file; }
        public Integer getLine() { return line; }
        public static HealthIssueBuilder builder() { return new HealthIssueBuilder(); }
        public static class HealthIssueBuilder {
            private String type; private String severity; private String description; private String file; private Integer line; private String suggestion;
            public HealthIssueBuilder type(String t) { this.type = t; return this; }
            public HealthIssueBuilder severity(String s) { this.severity = s; return this; }
            public HealthIssueBuilder description(String d) { this.description = d; return this; }
            public HealthIssueBuilder file(String f) { this.file = f; return this; }
            public HealthIssueBuilder line(Integer l) { this.line = l; return this; }
            public HealthIssueBuilder suggestion(String s) { this.suggestion = s; return this; }
            public HealthIssue build() {
                HealthIssue hi = new HealthIssue();
                hi.type = type; hi.severity = severity; hi.description = description; hi.file = file; hi.line = line; hi.suggestion = suggestion;
                return hi;
            }
        }
    }

    public static class CallReference {
        private String fromFunction;
        private String toFunction;
        private String toFile;
        private Integer line;
        private String type;
        public CallReference() {}
        public void setFromFunction(String f) { this.fromFunction = f; }
        public void setToFunction(String t) { this.toFunction = t; }
        public void setLine(Integer l) { this.line = l; }
        public void setType(String t) { this.type = t; }
        public String getFromFunction() { return fromFunction; }
        public String getToFunction() { return toFunction; }
        public Integer getLine() { return line; }
    }

    public static class DependencyGraph {
        private List<Node> nodes;
        private List<Edge> edges;
        private Map<String, Double> centralityScores;
        private List<String> criticalPath;
        private Integer blastRadius;
        
        public DependencyGraph() {}
        public List<Node> getNodes() { return nodes; }
        public void setNodes(List<Node> nodes) { this.nodes = nodes; }
        public List<Edge> getEdges() { return edges; }
        public void setEdges(List<Edge> edges) { this.edges = edges; }
        public List<String> getCriticalPath() { return criticalPath; }
        public Map<String, Double> getCentralityScores() { return centralityScores; }
        public Integer getBlastRadius() { return blastRadius; }

        public static DependencyGraphBuilder builder() { return new DependencyGraphBuilder(); }
        public static class DependencyGraphBuilder {
            private List<Node> nodes; private List<Edge> edges; private Map<String, Double> centralityScores;
            private List<String> criticalPath; private Integer blastRadius;
            public DependencyGraphBuilder nodes(List<Node> n) { this.nodes = n; return this; }
            public DependencyGraphBuilder edges(List<Edge> e) { this.edges = e; return this; }
            public DependencyGraphBuilder centralityScores(Map<String, Double> cs) { this.centralityScores = cs; return this; }
            public DependencyGraphBuilder criticalPath(List<String> cp) { this.criticalPath = cp; return this; }
            public DependencyGraphBuilder blastRadius(Integer br) { this.blastRadius = br; return this; }
            public DependencyGraph build() {
                DependencyGraph dg = new DependencyGraph();
                dg.nodes = nodes; dg.edges = edges; dg.centralityScores = centralityScores;
                dg.criticalPath = criticalPath; dg.blastRadius = blastRadius;
                return dg;
            }
        }

        public static class Node {
            private String id; private String file; private String type;
            private Integer linesOfCode; private Integer complexity;
            private Integer fanIn; private Integer fanOut;
            private Double centralityScore; private Map<String, Object> metadata;
            public Node() {}
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public Double getCentralityScore() { return centralityScore; }
            public void setCentralityScore(Double cs) { this.centralityScore = cs; }
            public Integer getFanOut() { return fanOut; }

            public static NodeBuilder builder() { return new NodeBuilder(); }
            public static class NodeBuilder {
                private String id; private String file; private String type;
                private Integer linesOfCode; private Integer complexity;
                private Integer fanIn; private Integer fanOut;
                private Double centralityScore; private Map<String, Object> metadata;
                public NodeBuilder id(String id) { this.id = id; return this; }
                public NodeBuilder file(String file) { this.file = file; return this; }
                public NodeBuilder type(String type) { this.type = type; return this; }
                public NodeBuilder linesOfCode(Integer loc) { this.linesOfCode = loc; return this; }
                public NodeBuilder complexity(Integer c) { this.complexity = c; return this; }
                public NodeBuilder fanIn(Integer fi) { this.fanIn = fi; return this; }
                public NodeBuilder fanOut(Integer fo) { this.fanOut = fo; return this; }
                public NodeBuilder centralityScore(Double cs) { this.centralityScore = cs; return this; }
                public NodeBuilder metadata(Map<String, Object> m) { this.metadata = m; return this; }
                public Node build() {
                    Node n = new Node();
                    n.id = id; n.file = file; n.type = type; n.linesOfCode = linesOfCode;
                    n.complexity = complexity; n.fanIn = fanIn; n.fanOut = fanOut;
                    n.centralityScore = centralityScore; n.metadata = metadata;
                    return n;
                }
            }
        }
        
        public static class Edge {
            private String source; private String target; private String type;
            private Integer weight; private Integer line;
            public Edge() {}
            public String getSource() { return source; }
            public void setSource(String s) { this.source = s; }
            public String getTarget() { return target; }
            public void setTarget(String t) { this.target = t; }

            public static EdgeBuilder builder() { return new EdgeBuilder(); }
            public static class EdgeBuilder {
                private String source; private String target; private String type;
                private Integer weight; private Integer line;
                public EdgeBuilder source(String s) { this.source = s; return this; }
                public EdgeBuilder target(String t) { this.target = t; return this; }
                public EdgeBuilder type(String ty) { this.type = ty; return this; }
                public EdgeBuilder weight(Integer w) { this.weight = w; return this; }
                public EdgeBuilder line(Integer l) { this.line = l; return this; }
                public Edge build() {
                    Edge e = new Edge();
                    e.source = source; e.target = target; e.type = type; e.weight = weight; e.line = line;
                    return e;
                }
            }
        }
    }

    public static class SecurityIssue {
        private String type; private String severity; private String description;
        private String file; private Integer line; private String codeSnippet;
        private String remediation; private String cweId; private String owaspCategory;
        private Boolean isFalsePositive;
        public SecurityIssue() {}
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getFile() { return file; }
        public Integer getLine() { return line; }
        public String getRemediation() { return remediation; }
        public String getType() { return type; }
        public String getCodeSnippet() { return codeSnippet; }

        public void setType(String t) { this.type = t; }
        public void setSeverity(String s) { this.severity = s; }
        public void setDescription(String d) { this.description = d; }
        public void setFile(String f) { this.file = f; }
        public void setLine(Integer l) { this.line = l; }
        public void setCodeSnippet(String cs) { this.codeSnippet = cs; }
        public void setRemediation(String r) { this.remediation = r; }
        public void setCweId(String c) { this.cweId = c; }
        public void setOwaspCategory(String o) { this.owaspCategory = o; }
        public void setIsFalsePositive(Boolean ifp) { this.isFalsePositive = ifp; }

        public static SecurityIssueBuilder builder() { return new SecurityIssueBuilder(); }
        public static class SecurityIssueBuilder {
            private String type; private String severity; private String description; private String file; private Integer line;
            private String codeSnippet; private String remediation; private String cweId; private String owaspCategory; private Boolean isFalsePositive;
            public SecurityIssueBuilder type(String t) { this.type = t; return this; }
            public SecurityIssueBuilder severity(String s) { this.severity = s; return this; }
            public SecurityIssueBuilder description(String d) { this.description = d; return this; }
            public SecurityIssueBuilder file(String f) { this.file = f; return this; }
            public SecurityIssueBuilder line(Integer l) { this.line = l; return this; }
            public SecurityIssueBuilder codeSnippet(String cs) { this.codeSnippet = cs; return this; }
            public SecurityIssueBuilder remediation(String r) { this.remediation = r; return this; }
            public SecurityIssueBuilder cweId(String c) { this.cweId = c; return this; }
            public SecurityIssueBuilder owaspCategory(String o) { this.owaspCategory = o; return this; }
            public SecurityIssueBuilder isFalsePositive(Boolean ifp) { this.isFalsePositive = ifp; return this; }
            public SecurityIssue build() {
                SecurityIssue si = new SecurityIssue();
                si.type = type; si.severity = severity; si.description = description; si.file = file; si.line = line;
                si.codeSnippet = codeSnippet; si.remediation = remediation; si.cweId = cweId; si.owaspCategory = owaspCategory; si.isFalsePositive = isFalsePositive;
                return si;
            }
        }
    }

    public static class CircularDependency {
        private List<String> files; private String description; private Integer severity;
        private String suggestion;
        public CircularDependency() {}
        public void setFiles(List<String> f) { this.files = f; }
        public void setDescription(String d) { this.description = d; }
        public void setSeverity(Integer s) { this.severity = s; }
        public Integer getSeverity() { return severity; }

        public static CircularDependencyBuilder builder() { return new CircularDependencyBuilder(); }
        public static class CircularDependencyBuilder {
            private List<String> files; private String description; private Integer severity;
            private String suggestion;
            public CircularDependencyBuilder files(List<String> f) { this.files = f; return this; }
            public CircularDependencyBuilder description(String d) { this.description = d; return this; }
            public CircularDependencyBuilder severity(Integer s) { this.severity = s; return this; }
            public CircularDependencyBuilder suggestion(String su) { this.suggestion = su; return this; }
            public CircularDependency build() {
                CircularDependency cd = new CircularDependency();
                cd.files = files; cd.description = description; cd.severity = severity; cd.suggestion = suggestion;
                return cd;
            }
        }
    }

    public static class AISuggestion {
        private String type; private String description; private String file;
        private Integer line; private String suggestion; private String codeBefore;
        private String codeAfter; private String provider; private Integer confidence;
        private Instant generatedAt;
        public AISuggestion() {}
        public void setType(String t) { this.type = t; }
        public void setDescription(String d) { this.description = d; }
        public void setFile(String f) { this.file = f; }
        public void setLine(Integer l) { this.line = l; }
        public void setSuggestion(String s) { this.suggestion = s; }
        public void setProvider(String p) { this.provider = p; }
        public void setConfidence(Integer c) { this.confidence = c; }
        public void setGeneratedAt(Instant ga) { this.generatedAt = ga; }

        public static AISuggestionBuilder builder() { return new AISuggestionBuilder(); }
        public static class AISuggestionBuilder {
            private String type; private String description; private String file; private Integer line;
            private String suggestion; private String provider; private Integer confidence; private Instant generatedAt;
            public AISuggestionBuilder type(String t) { this.type = t; return this; }
            public AISuggestionBuilder description(String d) { this.description = d; return this; }
            public AISuggestionBuilder file(String f) { this.file = f; return this; }
            public AISuggestionBuilder line(Integer l) { this.line = l; return this; }
            public AISuggestionBuilder suggestion(String s) { this.suggestion = s; return this; }
            public AISuggestionBuilder provider(String p) { this.provider = p; return this; }
            public AISuggestionBuilder confidence(Integer c) { this.confidence = c; return this; }
            public AISuggestionBuilder generatedAt(Instant ga) { this.generatedAt = ga; return this; }
            public AISuggestion build() {
                AISuggestion as = new AISuggestion();
                as.type = type; as.description = description; as.file = file; as.line = line; as.suggestion = suggestion;
                as.provider = provider; as.confidence = confidence; as.generatedAt = generatedAt;
                return as;
            }
        }
    }

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
         public ErrorAnalysis() {}
         public void setErrorType(String et) { this.errorType = et; }
         public void setStackTrace(String st) { this.stackTrace = st; }
         public void setFile(String f) { this.file = f; }
         public void setLine(Integer l) { this.line = l; }
         public void setRootCause(String rc) { this.rootCause = rc; }
         public void setSuggestedFix(String sf) { this.suggestedFix = sf; }
         public void setAffectedNodes(List<String> an) { this.affectedNodes = an; }
         public void setProvider(String p) { this.provider = p; }
         public void setConfidence(Integer c) { this.confidence = c; }

        public static ErrorAnalysisBuilder builder() { return new ErrorAnalysisBuilder(); }
         public static class ErrorAnalysisBuilder {
             private String errorType;
             private String stackTrace;
             private String rootCause;
             private String file;
             private Integer line;
             private List<String> affectedNodes;
             private String suggestedFix;
             private String provider;
             private Integer confidence;
             public ErrorAnalysisBuilder errorType(String et) { this.errorType = et; return this; }
             public ErrorAnalysisBuilder stackTrace(String st) { this.stackTrace = st; return this; }
             public ErrorAnalysisBuilder file(String f) { this.file = f; return this; }
             public ErrorAnalysisBuilder line(Integer l) { this.line = l; return this; }
             public ErrorAnalysisBuilder rootCause(String rc) { this.rootCause = rc; return this; }
             public ErrorAnalysisBuilder suggestedFix(String sf) { this.suggestedFix = sf; return this; }
             public ErrorAnalysisBuilder affectedNodes(List<String> an) { this.affectedNodes = an; return this; }
             public ErrorAnalysisBuilder provider(String p) { this.provider = p; return this; }
             public ErrorAnalysisBuilder confidence(Integer c) { this.confidence = c; return this; }
             public ErrorAnalysis build() {
                 ErrorAnalysis ea = new ErrorAnalysis();
                 ea.errorType = errorType; ea.stackTrace = stackTrace; ea.file = file; ea.line = line; ea.rootCause = rootCause;
                 ea.suggestedFix = suggestedFix; ea.affectedNodes = affectedNodes; ea.provider = provider; ea.confidence = confidence;
                 return ea;
             }
         }
    }

    public static class GitHubMetadata {
        private String repoId; private String owner; private String defaultBranch;
        private Boolean isPrivate; private Integer stars; private Integer forks;
        private Instant lastPushAt; private String language; private List<String> topics;
        public GitHubMetadata() {}
    }

    public static class PullRequestAnalysis {
        private String prId; private String title; private String author;
        private Instant createdAt; private Integer changedFiles; private Integer additions;
        private Integer deletions; private Integer riskScore;
        private List<String> suggestedReviewers; private List<String> affectedComponents;
        private String analysisSummary;
        public PullRequestAnalysis() {}
        public void setRiskScore(Integer rs) { this.riskScore = rs; }
        public void setAffectedComponents(List<String> ac) { this.affectedComponents = ac; }
        public void setSuggestedReviewers(List<String> sr) { this.suggestedReviewers = sr; }
        public void setAnalysisSummary(String as) { this.analysisSummary = as; }

        public static PullRequestAnalysisBuilder builder() { return new PullRequestAnalysisBuilder(); }
        public static class PullRequestAnalysisBuilder {
            private String prId; private String title; private String author; private Instant createdAt; private Integer changedFiles;
            public PullRequestAnalysisBuilder prId(String id) { this.prId = id; return this; }
            public PullRequestAnalysisBuilder title(String t) { this.title = t; return this; }
            public PullRequestAnalysisBuilder author(String a) { this.author = a; return this; }
            public PullRequestAnalysisBuilder createdAt(Instant ca) { this.createdAt = ca; return this; }
            public PullRequestAnalysisBuilder changedFiles(Integer cf) { this.changedFiles = cf; return this; }
            public PullRequestAnalysis build() {
                PullRequestAnalysis pra = new PullRequestAnalysis();
                pra.prId = prId; pra.title = title; pra.author = author; pra.createdAt = createdAt; pra.changedFiles = changedFiles;
                return pra;
            }
        }
    }
}