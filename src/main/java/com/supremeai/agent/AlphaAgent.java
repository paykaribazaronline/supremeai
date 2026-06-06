package com.supremeai.agent;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AlphaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(AlphaAgent.class);

  @Override
  public String getAgentId() {
    return "ALPHA";
  }

  @Override
  public String getAgentName() {
    return "Alpha-Security";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "security", "owasp", "vulnerability", "scan", "hack", "exploit", "penetration");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[AlphaAgent] Performing security analysis for task: {}", task);

    return Mono.fromCallable(
            () -> {
              Set<VulnerabilityReport> vulnerabilities = new HashSet<>();

              String codeContext = (String) context.getOrDefault("code", "");
              if (codeContext != null && !codeContext.isEmpty()) {
                vulnerabilities.addAll(scanCodeForVulnerabilities(codeContext));
              }

              String dependencyContext = (String) context.getOrDefault("dependencies", "");
              if (dependencyContext != null && !dependencyContext.isEmpty()) {
                vulnerabilities.addAll(scanDependencies(dependencyContext));
              }

              return generateReport(vulnerabilities);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public List<VulnerabilityReport> scanCodeForVulnerabilities(String code) {
    List<VulnerabilityReport> findings = new ArrayList<>();

    if (code.contains("SELECT * FROM") && code.contains("+")) {
      findings.add(
          new VulnerabilityReport(
              "SQL Injection Risk",
              "High",
              "String concatenation in SQL query detected. Use prepared statements.",
              "A03:2021",
              "sql"));
    }

    if (code.contains("eval(") || code.contains("exec(")) {
      findings.add(
          new VulnerabilityReport(
              "Code Injection Risk",
              "Critical",
              "Dynamic code execution detected. Validate and sanitize all inputs.",
              "A03:2021",
              "injection"));
    }

    if (code.contains("<script>") && code.contains("document.write")) {
      findings.add(
          new VulnerabilityReport(
              "XSS Risk",
              "High",
              "Unsanitized DOM manipulation detected. Sanitize user inputs.",
              "A03:2021",
              "xss"));
    }

    return findings;
  }

  public List<VulnerabilityReport> scanDependencies(String dependencyList) {
    List<VulnerabilityReport> findings = new ArrayList<>();

    String[] deps = dependencyList.split("[\\n,]");
    for (String dep : deps) {
      String trimmed = dep.trim().toLowerCase();
      if (trimmed.contains("lodash") && trimmed.contains("<4.17.20")) {
        findings.add(
            new VulnerabilityReport(
                "Outdated Vulnerable Dependency",
                "Medium",
                "lodash < 4.17.20 has known prototype pollution issues",
                "A06:2021",
                "dependencies"));
      }
      if (trimmed.contains("log4j") && (trimmed.contains("1.") || trimmed.contains("2.0"))) {
        findings.add(
            new VulnerabilityReport(
                "Log4Shell Vulnerable",
                "Critical",
                "log4j 1.x or 2.0 has Log4Shell vulnerability (CVE-2021-44228)",
                "A06:2021",
                "dependencies"));
      }
    }

    return findings;
  }

  private String generateReport(Set<VulnerabilityReport> vulnerabilities) {
    if (vulnerabilities.isEmpty()) {
      return "[AlphaAgent] No critical OWASP Top 10 vulnerabilities detected in analyzed code.";
    }

    Map<String, Long> severityCounts =
        vulnerabilities.stream()
            .collect(Collectors.groupingBy(VulnerabilityReport::severity, Collectors.counting()));

    StringBuilder report = new StringBuilder();
    report.append("[AlphaAgent] OWASP Security Scan Results:\n\n");
    report.append("Vulnerabilities Found: ").append(vulnerabilities.size()).append("\n");
    report
        .append("  - Critical: ")
        .append(severityCounts.getOrDefault("Critical", 0L))
        .append("\n");
    report.append("  - High: ").append(severityCounts.getOrDefault("High", 0L)).append("\n");
    report.append("  - Medium: ").append(severityCounts.getOrDefault("Medium", 0L)).append("\n");

    report.append("\nDetailed Findings:\n");
    for (VulnerabilityReport v : vulnerabilities) {
      report.append("  [").append(v.severity()).append("] ").append(v.title()).append("\n");
      report.append("    ").append(v.description()).append("\n");
      report.append("    OWASP: ").append(v.owaspCategory()).append("\n");
    }

    return report.toString();
  }

  public record VulnerabilityReport(
      String title, String severity, String description, String owaspCategory, String category) {}
}
