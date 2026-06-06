package com.supremeai.agent;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BetaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(BetaAgent.class);

  private static final Set<String> PERSONAL_DATA_FIELDS =
      Set.of(
          "email",
          "phone",
          "address",
          "name",
          "ssn",
          "credit.card",
          "location",
          "ip.address",
          "cookie.id",
          "biometric",
          "health");

  private static final Map<String, String> GDPR_RIGHTS =
      Map.of(
          "right.to.be.informed",
          "Clear privacy notices must be provided",
          "right.to.access",
          "Users can request their personal data",
          "right.to.rectification",
          "Users can correct inaccurate data",
          "right.to.erasure",
          "Users can request data deletion ('right to be forgotten')",
          "right.to.restrict.processing",
          "Users can limit data processing",
          "right.to.data.portability",
          "Users can export their data");

  @Override
  public String getAgentId() {
    return "BETA";
  }

  @Override
  public String getAgentName() {
    return "Beta-Compliance";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "compliance", "gdpr", "privacy", "legal", "right", "consent", "data.protection");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[BetaAgent] Performing GDPR compliance check for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String code = (String) context.getOrDefault("code", "");
              String dataFlow = (String) context.getOrDefault("dataFlow", "");
              String privacyPolicy = (String) context.getOrDefault("privacyPolicy", "");

              List<ComplianceIssue> issues = new ArrayList<>();

              if (code != null && !code.isEmpty()) {
                issues.addAll(analyzeCodeForCompliance(code));
              }

              if (dataFlow != null && !dataFlow.isEmpty()) {
                issues.addAll(checkDataFlowCompliance(dataFlow));
              }

              return generateComplianceReport(issues, privacyPolicy);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public List<ComplianceIssue> analyzeCodeForCompliance(String code) {
    List<ComplianceIssue> issues = new ArrayList<>();
    String lowerCode = code.toLowerCase();

    for (String field : PERSONAL_DATA_FIELDS) {
      if (lowerCode.contains(field)
          && !lowerCode.contains("consent")
          && !lowerCode.contains("encrypted")) {
        issues.add(
            new ComplianceIssue(
                "Missing Consent/Encryption",
                "HIGH",
                "Personal data field '"
                    + field
                    + "' detected without explicit consent or encryption"));
      }
    }

    if (lowerCode.contains("cookie") && !lowerCode.contains("cookie.consent")) {
      issues.add(
          new ComplianceIssue(
              "Missing Cookie Consent", "MEDIUM", "Cookie usage detected without consent banner"));
    }

    if (lowerCode.contains("third.party") && !lowerCode.contains("dpa")) {
      issues.add(
          new ComplianceIssue(
              "Missing Data Processing Agreement",
              "HIGH",
              "Third-party data sharing detected without DPA reference"));
    }

    return issues;
  }

  public List<ComplianceIssue> checkDataFlowCompliance(String dataFlow) {
    List<ComplianceIssue> issues = new ArrayList<>();

    if (dataFlow.contains("eu")
        && dataFlow.contains("us")
        && !dataFlow.contains("adequacy.decision")) {
      issues.add(
          new ComplianceIssue(
              "Cross-Border Transfer",
              "HIGH",
              "EU to non-EU data transfer needs adequacy decision or safeguards"));
    }

    if (dataFlow.contains("store") && !dataFlow.contains("retention.policy")) {
      issues.add(
          new ComplianceIssue(
              "Missing Retention Policy",
              "MEDIUM",
              "Data storage without defined retention period"));
    }

    return issues;
  }

  private String generateComplianceReport(List<ComplianceIssue> issues, String privacyPolicy) {
    StringBuilder report = new StringBuilder();
    report.append("[BetaAgent] GDPR Compliance Check Results:\n\n");

    if (issues.isEmpty()) {
      report.append("? No GDPR compliance issues detected.\n");
    } else {
      Map<String, Long> severityCounts =
          issues.stream()
              .collect(Collectors.groupingBy(ComplianceIssue::severity, Collectors.counting()));

      report.append("Compliance Issues: ").append(issues.size()).append("\n");
      report.append("  - HIGH: ").append(severityCounts.getOrDefault("HIGH", 0L)).append("\n");
      report.append("  - MEDIUM: ").append(severityCounts.getOrDefault("MEDIUM", 0L)).append("\n");
      report.append("  - LOW: ").append(severityCounts.getOrDefault("LOW", 0L)).append("\n\n");

      report.append("Issues Found:\n");
      for (ComplianceIssue issue : issues) {
        report
            .append("  [")
            .append(issue.severity())
            .append("] ")
            .append(issue.title())
            .append("\n");
        report.append("    ").append(issue.description()).append("\n");
      }
    }

    if (privacyPolicy != null && !privacyPolicy.isEmpty()) {
      report.append("\nPrivacy Policy Analysis:\n");
      report.append(analyzePrivacyPolicy(privacyPolicy));
    }

    report.append("\nRequired GDPR Rights Checklist:\n");
    for (Map.Entry<String, String> right : GDPR_RIGHTS.entrySet()) {
      report.append("  [ ] ").append(right.getValue()).append("\n");
    }

    return report.toString();
  }

  private String analyzePrivacyPolicy(String policy) {
    StringBuilder analysis = new StringBuilder();
    String lowerPolicy = policy.toLowerCase();

    if (!lowerPolicy.contains("data.controller")) {
      analysis.append("  ?? Missing data controller identification\n");
    }
    if (!lowerPolicy.contains("purpose")) {
      analysis.append("  ?? Missing purpose limitation clause\n");
    }
    if (!lowerPolicy.contains("retention") && !lowerPolicy.contains("how.long")) {
      analysis.append("  ?? Missing data retention information\n");
    }
    if (lowerPolicy.contains("sales") && !lowerPolicy.contains("california")) {
      analysis.append("  ?? CCPA considerations needed for US sales\n");
    }

    return analysis.length() > 0
        ? analysis.toString()
        : "  ? Privacy policy covers essential elements\n";
  }

  public record ComplianceIssue(String title, String severity, String description) {}
}
