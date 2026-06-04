package com.supremeai.service.analysis;

import com.supremeai.model.analysis.DependencyGraph;
import com.supremeai.repository.analysis.DependencyGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DependencyGraphService {

    private static final Logger log = LoggerFactory.getLogger(DependencyGraphService.class);


    private final DependencyGraphRepository dependencyGraphRepository;

    private static final Map<String, Pattern> IMPORT_PATTERNS = Map.of(
        "java", Pattern.compile("^import\\s+([\\w.]+);"),
        "javascript", Pattern.compile("^import\\s+.*from\\s+['\"]([^'\"]+)['\"]"),
        "typescript", Pattern.compile("^import\\s+.*from\\s+['\"]([^'\"]+)['\"]"),
        "python", Pattern.compile("^(?:import\\s+([\\w.]+)|from\\s+([\\w.]+))"),
        "go", Pattern.compile("^import\\s+[\"']([^\"']+)[\"']"),
        "ruby", Pattern.compile("^require(?:_relative)?\\s+['\"]([^'\"]+)['\"]"),
        "php", Pattern.compile("^(?:use|import)\\s+([\\w\\\\]+);"),
        "csharp", Pattern.compile("^using\\s+([\\w.]+);"),
        "kotlin", Pattern.compile("^import\\s+([\\w.]+)"),
        "scala", Pattern.compile("^import\\s+([\\w.]+)")
    );

    @Autowired
    public DependencyGraphService(DependencyGraphRepository dependencyGraphRepository) {
        this.dependencyGraphRepository = dependencyGraphRepository;
    }

    public Mono<Void> buildDependencyGraph(String projectId, List<File> files) {
        Map<String, List<String>> fileImports = new HashMap<>();
        Map<String, Set<String>> reverseDeps = new HashMap<>();

        for (File file : files) {
            String relativePath = file.getPath();
            String language = detectLanguage(file.getName());
            List<String> imports = extractImports(file, language);
            fileImports.put(relativePath, imports);

            for (String imp : imports) {
                reverseDeps.computeIfAbsent(imp, k -> new HashSet<>()).add(relativePath);
            }
        }

        List<DependencyGraph> graphs = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : fileImports.entrySet()) {
            String filePath = entry.getKey();
            List<String> imports = entry.getValue();

            List<String> importedBy = new ArrayList<>();
            for (String imp : imports) {
                Set<String> dependents = reverseDeps.getOrDefault(filePath, Collections.emptySet());
                importedBy.addAll(dependents);
            }

            graphs.add(DependencyGraph.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .file(filePath)
                .imports(imports)
                .importedBy(new ArrayList<>(new HashSet<>(importedBy)))
                .updatedAt(Instant.now().toString())
                .build());
        }

        return dependencyGraphRepository.deleteAll(
                dependencyGraphRepository.findByProjectId(projectId))
            .thenMany(Flux.fromIterable(graphs))
            .flatMap(dependencyGraphRepository::save)
            .then()
            .doOnSuccess(v -> log.info("Built dependency graph for project {}: {} files", projectId, graphs.size()))
            .doOnError(e -> log.error("Failed to build dependency graph for project {}: {}", projectId, e.getMessage()));
    }

    public List<String> findImpactedFiles(String projectId, List<String> changedFiles) {
        List<DependencyGraph> allGraphs;
        try {
            allGraphs = dependencyGraphRepository.findByProjectId(projectId).collectList().block();
        } catch (Exception e) {
            log.warn("Failed to fetch dependency graph for project {}: {}", projectId, e.getMessage());
            return new ArrayList<>(changedFiles);
        }

        if (allGraphs == null || allGraphs.isEmpty()) {
            return new ArrayList<>(changedFiles);
        }

        Set<String> impacted = new LinkedHashSet<>(changedFiles);
        Set<String> toProcess = new LinkedHashSet<>(changedFiles);
        Set<String> processed = new HashSet<>();

        while (!toProcess.isEmpty()) {
            String current = toProcess.iterator().next();
            toProcess.remove(current);
            processed.add(current);

            for (DependencyGraph graph : allGraphs) {
                if (graph.getFile().equals(current) && graph.getImportedBy() != null) {
                    for (String dependent : graph.getImportedBy()) {
                        if (!processed.contains(dependent)) {
                            impacted.add(dependent);
                            toProcess.add(dependent);
                        }
                    }
                }
            }
        }

        log.debug("Changed files: {}, Impacted files: {}", changedFiles.size(), impacted.size());
        return new ArrayList<>(impacted);
    }

    public Mono<Void> updateGraphForFile(String projectId, File file) {
        String relativePath = file.getPath();
        String language = detectLanguage(file.getName());
        List<String> imports = extractImports(file, language);

        return dependencyGraphRepository.findByProjectIdAndFile(projectId, relativePath)
            .flatMap(existing -> {
                existing.setImports(imports);
                existing.setUpdatedAt(Instant.now().toString());
                return dependencyGraphRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> dependencyGraphRepository.save(
                DependencyGraph.builder()
                    .id(UUID.randomUUID().toString())
                    .projectId(projectId)
                    .file(relativePath)
                    .imports(imports)
                    .importedBy(List.of())
                    .updatedAt(Instant.now().toString())
                    .build()
            )))
            .then();
    }

    public Mono<Void> clearProjectGraph(String projectId) {
        return dependencyGraphRepository.findByProjectId(projectId)
            .flatMap(dependencyGraphRepository::delete)
            .then()
            .doOnSuccess(v -> log.info("Cleared dependency graph for project {}", projectId));
    }

    private List<String> extractImports(File file, String language) {
        Pattern pattern = IMPORT_PATTERNS.get(language);
        if (pattern == null) {
            return List.of();
        }

        List<String> imports = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line.trim());
                if (matcher.find()) {
                    String imp = matcher.group(1);
                    if (imp != null) {
                        imports.add(imp);
                    } else if (matcher.groupCount() > 1 && matcher.group(2) != null) {
                        imports.add(matcher.group(2));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to extract imports from {}: {}", file.getPath(), e.getMessage());
        }
        return imports;
    }

    private String detectLanguage(String filename) {
        if (filename.endsWith(".java")) return "java";
        if (filename.endsWith(".js")) return "javascript";
        if (filename.endsWith(".ts") || filename.endsWith(".tsx")) return "typescript";
        if (filename.endsWith(".py")) return "python";
        if (filename.endsWith(".go")) return "go";
        if (filename.endsWith(".rb")) return "ruby";
        if (filename.endsWith(".php")) return "php";
        if (filename.endsWith(".cs")) return "csharp";
        if (filename.endsWith(".kt")) return "kotlin";
        if (filename.endsWith(".scala")) return "scala";
        return "unknown";
    }
}
