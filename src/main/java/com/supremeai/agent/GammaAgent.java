package com.supremeai.agent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GammaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(GammaAgent.class);

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
  private static final Pattern PHONE_PATTERN =
      Pattern.compile("\\b(\\d{3}[-.]?\\d{3}[-.]?\\d{4}|\\+\\d{1,3}\\s?\\d{3,14})\\b");
  private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-?\\d{2}-?\\d{4}\\b");
  private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
  private static final Pattern IP_PATTERN =
      Pattern.compile(
          "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

  private static final Set<String> SENSITIVE_KEYWORDS =
      Set.of(
          "password",
          "secret",
          "token",
          "apikey",
          "api_key",
          "private_key",
          "encryption_key",
          "access_token");

  @Override
  public String getAgentId() {
    return "GAMMA";
  }

  @Override
  public String getAgentName() {
    return "Gamma-Privacy";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList("privacy", "pii", "personal", "sensitive", "data.flow", "anonymize");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[GammaAgent] Performing PII and data flow analysis for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String data = (String) context.getOrDefault("data", "");
              String logs = (String) context.getOrDefault("logs", "");
              String code = (String) context.getOrDefault("code", "");

              List<PIIFinding> piiFindings = new ArrayList<>();

              if (data != null && !data.isEmpty()) {
                piiFindings.addAll(detectPII(data));
              }

              if (logs != null && !logs.isEmpty()) {
                piiFindings.addAll(detectPIIInLogs(logs));
              }

              if (code != null && !code.isEmpty()) {
                piiFindings.addAll(detectSensitiveVariableNames(code));
              }

              return generatePrivacyReport(piiFindings);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public List<PIIFinding> detectPII(String text) {
    List<PIIFinding> findings = new ArrayList<>();

    EMAIL_PATTERN
        .matcher(text)
        .results()
        .forEach(
            m ->
                findings.add(
                    new PIIFinding(
                        "Email Address",
                        "HIGH",
                        m.group(),
                        "Consider hashing or redacting in logs")));

    PHONE_PATTERN
        .matcher(text)
        .results()
        .forEach(
            m ->
                findings.add(
                    new PIIFinding(
                        "Phone Number",
                        "HIGH",
                        m.group(),
                        "Phone numbers should be encrypted at rest")));

    SSN_PATTERN
        .matcher(text)
        .results()
        .forEach(
            m ->
                findings.add(
                    new PIIFinding(
                        "SSN/Social Security",
                        "CRITICAL",
                        m.group(),
                        "Never log or store SSNs in plain text")));

    CREDIT_CARD_PATTERN
        .matcher(text)
        .results()
        .forEach(
            m ->
                findings.add(
                    new PIIFinding(
                        "Credit Card",
                        "CRITICAL",
                        m.group(),
                        "PCI DSS violation - cards must be tokenized")));

    IP_PATTERN
        .matcher(text)
        .results()
        .forEach(
            m ->
                findings.add(
                    new PIIFinding(
                        "IP Address",
                        "MEDIUM",
                        m.group(),
                        "IP addresses may be considered personal data under GDPR")));

    return findings;
  }

  public List<PIIFinding> detectPIIInLogs(String logs) {
    List<PIIFinding> findings = new ArrayList<>();
    String[] lines = logs.split("\\n");

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      final int lineNum = i;
      if (line.toLowerCase().contains("error") || line.toLowerCase().contains("debug")) {
        detectPII(line).stream()
            .map(
                f ->
                    new PIIFinding(
                        f.type(),
                        f.severity(),
                        f.value(),
                        f.recommendation() + " [log line " + lineNum + "]"))
            .forEach(findings::add);
      }
    }

    return findings;
  }

  public List<PIIFinding> detectSensitiveVariableNames(String code) {
    List<PIIFinding> findings = new ArrayList<>();
    String lowerCode = code.toLowerCase();

    for (String keyword : SENSITIVE_KEYWORDS) {
      if (lowerCode.contains(keyword)) {
        int count = countOccurrences(lowerCode, keyword);
        findings.add(
            new PIIFinding(
                "Sensitive Variable Name",
                "MEDIUM",
                keyword,
                "Variable contains sensitive keyword '"
                    + keyword
                    + "' ("
                    + count
                    + " occurrences). Consider masking."));
      }
    }

    return findings;
  }

  private int countOccurrences(String text, String pattern) {
    return (int) text.chars().filter(ch -> text.indexOf(pattern) >= 0).count();
  }

  private String generatePrivacyReport(List<PIIFinding> findings) {
    if (findings.isEmpty()) {
      return "[GammaAgent] ✅ No PII or sensitive data detected in analyzed content.";
    }

    Map<String, Long> severityCounts =
        findings.stream()
            .collect(Collectors.groupingBy(PIIFinding::severity, Collectors.counting()));

    StringBuilder report = new StringBuilder();
    report.append("[GammaAgent] PII Detection Report:\n\n");

    report.append("Total PII Findings: ").append(findings.size()).append("\n");
    report
        .append("  - CRITICAL: ")
        .append(severityCounts.getOrDefault("CRITICAL", 0L))
        .append("\n");
    report.append("  - HIGH: ").append(severityCounts.getOrDefault("HIGH", 0L)).append("\n");
    report.append("  - MEDIUM: ").append(severityCounts.getOrDefault("MEDIUM", 0L)).append("\n\n");

    report.append("Detailed Findings:\n");
    for (PIIFinding f : findings) {
      String maskedValue = maskValue(f.value());
      report
          .append("  [")
          .append(f.severity())
          .append("] ")
          .append(f.type())
          .append(": ")
          .append(maskedValue)
          .append("\n");
      report.append("    Recommendation: ").append(f.recommendation()).append("\n");
    }

    report.append("\nPrivacy Remediation Steps:\n");
    report.append("  1. Implement field-level encryption for high-sensitivity data\n");
    report.append("  2. Add log sanitization middleware\n");
    report.append("  3. Enable automatic PII redaction in data flows\n");

    return report.toString();
  }

  private String maskValue(String value) {
    if (value == null || value.length() < 4) return "***";
    return "***" + value.substring(Math.max(0, value.length() - 4));
  }

  public record PIIFinding(String type, String severity, String value, String recommendation) {}
}
