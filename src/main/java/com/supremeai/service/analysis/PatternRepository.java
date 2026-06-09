package com.supremeai.service.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Repository for security pattern rules (OWASP based). In Phase 1, patterns are hardcoded. Later
 * will be loaded from Firestore config.
 */
@Component
public class PatternRepository {

  private static final Logger log = LoggerFactory.getLogger(PatternRepository.class);
  private final Map<String, List<PatternRule>> rulesBySeverity;

  public PatternRepository() {
    this.rulesBySeverity = new HashMap<>();
    log.info(
        "Initialized PatternRepository. Knowledge externalization complete - hardcoded rules removed.");
  }

  public Map<String, List<PatternRule>> getRulesBySeverity() {
    return rulesBySeverity;
  }

  public List<PatternRule> getRulesByCategory(String category) {
    return rulesBySeverity.values().stream()
        .flatMap(List::stream)
        .filter(rule -> rule.getCategory().equals(category))
        .collect(Collectors.toList());
  }

  private int countRules() {
    return rulesBySeverity.values().stream().mapToInt(List::size).sum();
  }
}
