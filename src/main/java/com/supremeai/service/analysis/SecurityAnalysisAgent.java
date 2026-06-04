package com.supremeai.service.analysis;

import com.supremeai.model.analysis.AnalysisFinding;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Security Scanner Agent - Detects OWASP Top 10 vulnerabilities using regex patterns. */
@Component
public class SecurityAnalysisAgent implements AnalysisAgentInterface {

  private static final Logger log = LoggerFactory.getLogger(SecurityAnalysisAgent.class);
  private final PatternRepository patternRepository;
  private final AnalysisStats stats = new AnalysisStats();

  public SecurityAnalysisAgent(PatternRepository patternRepository) {
    this.patternRepository = patternRepository;
  }

  @Override
  public String getCategory() {
    return "SECURITY";
  }

  @Override
  public List<String> getSupportedExtensions() {
    return Arrays.asList(
        ".java", ".js", ".ts", ".tsx", ".jsx", ".py", ".go", ".rb", ".php", ".cs", ".c", ".cpp",
        ".h", ".hpp", ".scala", ".kt", ".swift", ".m", ".mm");
  }

  @Override
  public boolean isEnabled() {
    return true; // Always enabled for Phase 1
  }

  @Override
  public Flux<AnalysisFinding> scanFile(File file, String relativePath) {
    return Flux.<AnalysisFinding>create(
            emitter -> {
              try {
                String extension = getFileExtension(file.getName());
                if (!getSupportedExtensions().contains(extension)) {
                  emitter.complete();
                  return;
                }

                List<String> lines = readFileLines(file);
                Map<String, List<PatternRule>> rulesBySeverity =
                    patternRepository.getRulesBySeverity();

                AtomicInteger lineNum = new AtomicInteger(1);

                for (String line : lines) {
                  for (Map.Entry<String, List<PatternRule>> entry : rulesBySeverity.entrySet()) {
                    String severity = entry.getKey();
                    for (PatternRule rule : entry.getValue()) {
                      Matcher matcher = rule.getPattern().matcher(line);
                      if (matcher.find()) {
                        String matchedText = matcher.group();
                        String message = rule.getMessage().replace("{match}", matchedText);
                        String suggestion = rule.getSuggestion();

                        AnalysisFinding finding =
                            AnalysisFinding.builder()
                                .id(UUID.randomUUID().toString())
                                .jobId("") // Will be set by service
                                .severity(severity)
                                .category(rule.getCategory())
                                .file(relativePath)
                                .line(lineNum.get())
                                .message(message)
                                .suggestion(suggestion)
                                .pattern(rule.getPattern().toString())
                                .codeSnippet(truncateSnippet(line.trim()))
                                .build();

                        emitter.next(finding);
                        stats.incrementFinding(severity);
                      }
                    }
                  }
                  lineNum.incrementAndGet();
                }

                emitter.complete();
              } catch (IOException e) {
                log.error("Error scanning file {}: {}", relativePath, e.getMessage());
                emitter.error(e);
              }
            })
        .doOnComplete(() -> log.debug("Completed scanning file: {}", relativePath))
        .doOnError(e -> log.error("Error in scanFile for {}: {}", relativePath, e.getMessage()));
  }

  private String getFileExtension(String filename) {
    int lastDot = filename.lastIndexOf('.');
    return (lastDot == -1) ? "" : filename.substring(lastDot).toLowerCase();
  }

  private List<String> readFileLines(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return reader.lines().collect(Collectors.toList());
    }
  }

  private String truncateSnippet(String snippet) {
    if (snippet.length() > 200) {
      return snippet.substring(0, 200) + "...";
    }
    return snippet;
  }

  public AnalysisStats getStats() {
    return stats;
  }

  public void resetStats() {
    stats.reset();
  }

  /** Internal class for tracking statistics. */
  public static class AnalysisStats {
    private final Map<String, Integer> counts = new HashMap<>();

    public AnalysisStats() {
      counts.put("CRITICAL", 0);
      counts.put("HIGH", 0);
      counts.put("MEDIUM", 0);
      counts.put("LOW", 0);
      counts.put("INFO", 0);
    }

    public void incrementFinding(String severity) {
      counts.merge(severity, 1, Integer::sum);
    }

    public Map<String, Integer> getCounts() {
      return new HashMap<>(counts);
    }

    public void reset() {
      counts.replaceAll((k, v) -> 0);
    }
  }
}
