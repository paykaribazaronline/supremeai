package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProjectContextService {
    private static final Logger log = LoggerFactory.getLogger(ProjectContextService.class);

    @Value("${supremeai.project.root:c:/Users/n/supremeai}")
    private String projectRoot;

    /**
     * Reads a specific file within the project root.
     */
    public Mono<String> readFile(String relativePath) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(projectRoot, relativePath).normalize();
            if (!path.startsWith(Paths.get(projectRoot))) {
                throw new SecurityException("Access denied: Path is outside project root.");
            }
            log.info("[Context] Reading file: {}", path);
            return Files.readString(path);
        });
    }

    /**
     * Lists all important source files for the AI to see the project structure.
     */
    public Mono<String> getProjectStructure() {
        return Mono.fromCallable(() -> {
            Path root = Paths.get(projectRoot);
            try (Stream<Path> stream = Files.walk(root, 3)) {
                return stream
                    .filter(p -> !p.toString().contains(".git"))
                    .filter(p -> !p.toString().contains("node_modules"))
                    .filter(p -> !p.toString().contains("build/"))
                    .map(p -> root.relativize(p).toString())
                    .collect(Collectors.joining("\n"));
            }
        });
    }
}