package com.supremeai.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.analysis.AnalysisFinding;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;

@Component
public class DependencyAnalysisAgent implements AnalysisAgentInterface {

  private static final Logger log = LoggerFactory.getLogger(DependencyAnalysisAgent.class);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DocumentBuilder documentBuilder;
  private final AnalysisStats stats = new AnalysisStats();

  // Known vulnerable packages (simplified - in production use NIST NVD API)
  private final Map<String, Set<String>> vulnerablePackages =
      Map.of(
          "lodash", Set.of("<4.17.11"),
          "express", Set.of("<4.17.1"),
          "jquery", Set.of("<3.5.0"),
          "moment", Set.of("all"), // Deprecated
          "left-pad", Set.of("all"), // Classic example
          "spring-boot-starter-web", Set.of("<2.7.0"),
          "log4j-core", Set.of("<2.17.0") // Log4Shell
          );

  public DependencyAnalysisAgent() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    this.documentBuilder = factory.newDocumentBuilder();
  }

  @Override
  public String getCategory() {
    return "DEPENDENCIES";
  }

  @Override
  public List<String> getSupportedExtensions() {
    return Arrays.asList(
        "package.json",
        "package-lock.json",
        "pom.xml",
        "build.gradle",
        "build.gradle.kts",
        "requirements.txt",
        "Pipfile",
        "Pipfile.lock",
        "Cargo.toml",
        "composer.json",
        "Gemfile");
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public Flux<AnalysisFinding> scanFile(File file, String relativePath) {
    return Flux.<AnalysisFinding>create(
            emitter -> {
              try {
                String filename = file.getName().toLowerCase();

                if (filename.equals("package.json")) {
                  analyzePackageJson(file, relativePath, emitter);
                } else if (filename.equals("pom.xml")) {
                  analyzePomXml(file, relativePath, emitter);
                } else if (filename.equals("requirements.txt")
                    || filename.equals("pipfile")
                    || filename.equals("pipfile.lock")) {
                  analyzePythonDependencies(file, relativePath, emitter);
                } else if (filename.equals("cargo.toml")) {
                  analyzeCargoToml(file, relativePath, emitter);
                } else if (filename.equals("composer.json")) {
                  analyzeComposerJson(file, relativePath, emitter);
                } else if (filename.equals("gemfile")) {
                  analyzeGemfile(file, relativePath, emitter);
                } else if (filename.contains("build.gradle")) {
                  analyzeGradleBuild(file, relativePath, emitter);
                }

                emitter.complete();
              } catch (Exception e) {
                log.error("Error scanning dependency file {}: {}", relativePath, e.getMessage());
                emitter.error(e);
              }
            })
        .doOnComplete(() -> log.debug("Completed dependency analysis for file: {}", relativePath))
        .doOnError(
            e ->
                log.error("Error in dependency analysis for {}: {}", relativePath, e.getMessage()));
  }

  private void analyzePackageJson(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    JsonNode root = objectMapper.readTree(file);
    JsonNode dependencies = root.get("dependencies");
    JsonNode devDependencies = root.get("devDependencies");

    if (dependencies != null) {
      analyzeNpmDependencies(dependencies, false, relativePath, emitter);
    }
    if (devDependencies != null) {
      analyzeNpmDependencies(devDependencies, true, relativePath, emitter);
    }

    // Check for scripts that might be dangerous
    JsonNode scripts = root.get("scripts");
    if (scripts != null) {
      analyzeNpmScripts(scripts, relativePath, emitter);
    }
  }

  private void analyzeNpmDependencies(
      JsonNode deps,
      boolean isDev,
      String relativePath,
      reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
    deps.fields()
        .forEachRemaining(
            entry -> {
              String packageName = entry.getKey();
              String version = entry.getValue().asText();

              // Check for vulnerable packages
              if (vulnerablePackages.containsKey(packageName)) {
                Set<String> vulnerableVersions = vulnerablePackages.get(packageName);
                if (vulnerableVersions.contains("all")
                    || isVersionVulnerable(version, vulnerableVersions)) {
                  emitter.next(
                      createFinding(
                          "HIGH",
                          "VULNERABILITY",
                          "Vulnerable package: " + packageName + "@" + version,
                          "Update to latest secure version or use alternative package.",
                          relativePath,
                          1));
                  stats.incrementFinding("HIGH");
                }
              }

              // Check for outdated major versions (simplified)
              if (isOutdatedVersion(version)) {
                emitter.next(
                    createFinding(
                        "MEDIUM",
                        "OUTDATED",
                        "Potentially outdated package: " + packageName + "@" + version,
                        "Check for updates and update to latest stable version.",
                        relativePath,
                        1));
                stats.incrementFinding("MEDIUM");
              }
            });
  }

  private void analyzeNpmScripts(
      JsonNode scripts,
      String relativePath,
      reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
    scripts
        .fields()
        .forEachRemaining(
            entry -> {
              String scriptName = entry.getKey();
              String scriptCommand = entry.getValue().asText();

              // Check for dangerous scripts
              if (scriptCommand.contains("rm -rf")
                  || scriptCommand.contains("sudo")
                  || scriptCommand.contains("curl | bash")) {
                emitter.next(
                    createFinding(
                        "HIGH",
                        "SECURITY",
                        "Potentially dangerous npm script: " + scriptName,
                        "Review script for security implications. Avoid destructive commands.",
                        relativePath,
                        1));
                stats.incrementFinding("HIGH");
              }

              // Check for hardcoded secrets in scripts
              if (scriptCommand.matches(".*(password|secret|token|key).*")) {
                emitter.next(
                    createFinding(
                        "MEDIUM",
                        "SECURITY",
                        "Script may contain sensitive information: " + scriptName,
                        "Move secrets to environment variables or .env files.",
                        relativePath,
                        1));
                stats.incrementFinding("MEDIUM");
              }
            });
  }

  private void analyzePomXml(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws Exception {
    Document doc = documentBuilder.parse(file);
    NodeList dependencies = doc.getElementsByTagName("dependency");

    for (int i = 0; i < dependencies.getLength(); i++) {
      Element dep = (Element) dependencies.item(i);
      String groupId = getElementText(dep, "groupId");
      String artifactId = getElementText(dep, "artifactId");
      String version = getElementText(dep, "version");

      String fullName = groupId + ":" + artifactId;

      // Check for vulnerable Maven packages
      if (vulnerablePackages.containsKey(artifactId) || vulnerablePackages.containsKey(fullName)) {
        Set<String> vulnerableVersions =
            vulnerablePackages.getOrDefault(artifactId, vulnerablePackages.get(fullName));
        if (vulnerableVersions != null
            && (vulnerableVersions.contains("all")
                || isVersionVulnerable(version, vulnerableVersions))) {
          emitter.next(
              createFinding(
                  "HIGH",
                  "VULNERABILITY",
                  "Vulnerable Maven dependency: " + fullName + ":" + version,
                  "Update to latest secure version.",
                  relativePath,
                  1));
          stats.incrementFinding("HIGH");
        }
      }

      // Check for Spring Boot version issues
      if ("org.springframework.boot".equals(groupId)
          && "spring-boot-starter-parent".equals(artifactId)) {
        if (isOldSpringBootVersion(version)) {
          emitter.next(
              createFinding(
                  "MEDIUM",
                  "OUTDATED",
                  "Outdated Spring Boot version: " + version,
                  "Update to Spring Boot 3.x for latest security fixes.",
                  relativePath,
                  1));
          stats.incrementFinding("MEDIUM");
        }
      }
    }
  }

  private void analyzePythonDependencies(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    List<String> lines = Files.readAllLines(file.toPath());

    for (String line : lines) {
      if (line.trim().startsWith("#") || line.trim().isEmpty()) continue;

      // Parse package==version format
      Pattern pattern =
          Pattern.compile("([a-zA-Z0-9_-]+)(?:[=<>~!]+)([0-9]+(?:\\.[0-9]+)*(?:[a-zA-Z0-9._-]*)?)");
      Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        String packageName = matcher.group(1);
        String version = matcher.group(2);

        if (vulnerablePackages.containsKey(packageName)) {
          emitter.next(
              createFinding(
                  "HIGH",
                  "VULNERABILITY",
                  "Vulnerable Python package: " + packageName + "==" + version,
                  "Update to latest secure version.",
                  relativePath,
                  1));
          stats.incrementFinding("HIGH");
        }
      }
    }
  }

  private void analyzeCargoToml(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    List<String> lines = Files.readAllLines(file.toPath());
    boolean inDependencies = false;

    for (String line : lines) {
      if (line.trim().startsWith("[dependencies]")) {
        inDependencies = true;
        continue;
      }
      if (line.trim().startsWith("[") && inDependencies) {
        inDependencies = false;
      }

      if (inDependencies && line.contains("=")) {
        String[] parts = line.split("=");
        if (parts.length >= 2) {
          String packageName = parts[0].trim().replace("\"", "");
          String versionSpec = parts[1].trim();

          if (vulnerablePackages.containsKey(packageName)) {
            emitter.next(
                createFinding(
                    "HIGH",
                    "VULNERABILITY",
                    "Vulnerable Rust crate: " + packageName + versionSpec,
                    "Update to latest secure version.",
                    relativePath,
                    1));
            stats.incrementFinding("HIGH");
          }
        }
      }
    }
  }

  private void analyzeComposerJson(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    JsonNode root = objectMapper.readTree(file);
    JsonNode require = root.get("require");
    JsonNode requireDev = root.get("require-dev");

    if (require != null) {
      analyzeComposerDependencies(require, false, relativePath, emitter);
    }
    if (requireDev != null) {
      analyzeComposerDependencies(requireDev, true, relativePath, emitter);
    }
  }

  private void analyzeComposerDependencies(
      JsonNode deps,
      boolean isDev,
      String relativePath,
      reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
    deps.fields()
        .forEachRemaining(
            entry -> {
              String packageName = entry.getKey();
              String versionSpec = entry.getValue().asText();

              if (vulnerablePackages.containsKey(packageName)) {
                emitter.next(
                    createFinding(
                        "HIGH",
                        "VULNERABILITY",
                        "Vulnerable PHP package: " + packageName + ":" + versionSpec,
                        "Update to latest secure version.",
                        relativePath,
                        1));
                stats.incrementFinding("HIGH");
              }
            });
  }

  private void analyzeGemfile(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    List<String> lines = Files.readAllLines(file.toPath());

    for (String line : lines) {
      if (line.trim().startsWith("#") || line.trim().isEmpty()) continue;

      Pattern pattern =
          Pattern.compile("gem\\s+['\"]([a-zA-Z0-9_-]+)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]");
      Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        String gemName = matcher.group(1);
        String versionSpec = matcher.group(2);

        if (vulnerablePackages.containsKey(gemName)) {
          emitter.next(
              createFinding(
                  "HIGH",
                  "VULNERABILITY",
                  "Vulnerable Ruby gem: " + gemName + " " + versionSpec,
                  "Update to latest secure version.",
                  relativePath,
                  1));
          stats.incrementFinding("HIGH");
        }
      }
    }
  }

  private void analyzeGradleBuild(
      File file, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter)
      throws IOException {
    List<String> lines = Files.readAllLines(file.toPath());

    for (String line : lines) {
      // Look for dependency declarations
      if (line.contains("implementation") || line.contains("api") || line.contains("compileOnly")) {
        Pattern pattern = Pattern.compile("['\"]([^:]+):([^:]+):([^'\"]*)['\"]");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          String groupId = matcher.group(1);
          String artifactId = matcher.group(2);
          String version = matcher.group(3);

          String fullName = groupId + ":" + artifactId;

          if (vulnerablePackages.containsKey(artifactId)
              || vulnerablePackages.containsKey(fullName)) {
            Set<String> vulnerableVersions =
                vulnerablePackages.getOrDefault(artifactId, vulnerablePackages.get(fullName));
            if (vulnerableVersions != null
                && (vulnerableVersions.contains("all")
                    || isVersionVulnerable(version, vulnerableVersions))) {
              emitter.next(
                  createFinding(
                      "HIGH",
                      "VULNERABILITY",
                      "Vulnerable Gradle dependency: " + fullName + ":" + version,
                      "Update to latest secure version.",
                      relativePath,
                      1));
              stats.incrementFinding("HIGH");
            }
          }
        }
      }
    }
  }

  // Helper methods
  private boolean isVersionVulnerable(String version, Set<String> vulnerableVersions) {
    if (vulnerableVersions.contains("all")) return true;
    // Simplified version comparison - in production use proper semver library
    return vulnerableVersions.stream()
        .anyMatch(v -> version.startsWith(v.replace("<", "").replace(">", "").replace("=", "")));
  }

  private boolean isOutdatedVersion(String version) {
    // Simplified check - consider versions without dots as potentially outdated
    return !version.contains(".") || version.startsWith("0.") || version.startsWith("1.");
  }

  private boolean isOldSpringBootVersion(String version) {
    return version.startsWith("1.")
        || version.startsWith("2.0.")
        || version.startsWith("2.1.")
        || version.startsWith("2.2.")
        || version.startsWith("2.3.")
        || version.startsWith("2.4.")
        || version.startsWith("2.5.")
        || version.startsWith("2.6.");
  }

  private String getElementText(Element element, String tagName) {
    NodeList nodes = element.getElementsByTagName(tagName);
    if (nodes.getLength() > 0) {
      return nodes.item(0).getTextContent().trim();
    }
    return "";
  }

  private AnalysisFinding createFinding(
      String severity, String category, String message, String suggestion, String file, int line) {
    return AnalysisFinding.builder()
        .id(UUID.randomUUID().toString())
        .jobId("")
        .severity(severity)
        .category(category)
        .file(file)
        .line(line)
        .message(message)
        .suggestion(suggestion)
        .pattern("")
        .codeSnippet("")
        .build();
  }

  public AnalysisStats getStats() {
    return stats;
  }

  public void resetStats() {
    stats.reset();
  }

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
