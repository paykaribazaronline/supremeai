package com.supremeai.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.supremeai.model.SystemWorkRule;
import com.supremeai.repository.SystemWorkRuleRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SystemWorkRuleService — Manages the authoritative "System Work Rules" collection.
 *
 * <p>Core concept ──────────── The admin creates/updates a SystemWorkRule that specifies: • WHAT
 * kind of work parameter it controls (ruleKey, e.g. "LEARNING_AUTO_INTERVAL") • WHAT the correct
 * value should be (value / structuredValue) • WHERE in Firestore it must be written (targetDoc +
 * targetField)
 *
 * <p>On every save, resolveConflicts() reads the target Firestore document, compares the current
 * value with the rule's value, and if they differ it silently propagates the rule value into the
 * document — eliminating the conflict and recording the decision in an append-only decision log
 * stored alongside the rule document.
 */
@Service
public class SystemWorkRuleService {

  private static final Logger log = LoggerFactory.getLogger(SystemWorkRuleService.class);

  @Autowired private SystemWorkRuleRepository ruleRepository;

  @Autowired(required = false)
  private Firestore firestore;

  // ─── READ ────────────────────────────────────────────────────────────────

  /** All rules sorted by category then descending priority. */
  public Flux<SystemWorkRule> getAllRules() {
    return ruleRepository
        .findAll()
        .sort(
            (a, b) -> {
              int cat =
                  (a.getCategory() != null && b.getCategory() != null)
                      ? a.getCategory().compareTo(b.getCategory())
                      : (a.getCategory() != null && b.getCategory() == null) ? 1 : -1;
              return cat != 0 ? cat : Integer.compare(b.getPriority(), a.getPriority());
            });
  }

  /** Only rules whose enabled==true, sorted by priority descending. */
  public Flux<SystemWorkRule> getActiveRules() {
    return ruleRepository
        .findByEnabled(true)
        .sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
  }

  public Flux<SystemWorkRule> getRulesByCategory(String category) {
    return ruleRepository.findByCategory(category.toUpperCase());
  }

  public Mono<SystemWorkRule> getRuleByKey(String ruleKey) {
    return ruleRepository
        .findById(ruleKey)
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("SystemWorkRule not found: " + ruleKey)));
  }

  // ─── WRITE ───────────────────────────────────────────────────────────────

  /**
   * Create or update a rule and fire conflict-resolution immediately so the related Firestore
   * documents are synchronised before the call returns.
   */
  public Mono<SystemWorkRule> saveRule(SystemWorkRule rule) {
    String key = rule.getRuleKey();
    if (key == null || key.isBlank()) {
      return Mono.error(new IllegalArgumentException("ruleKey must not be blank"));
    }
    // Natural-id = ruleKey (same as Firestore doc ID)
    rule.setId(key);

    LocalDateTime now = LocalDateTime.now();
    if (rule.getCreatedAt() == null) {
      rule.setCreatedAt(now);
    }
    rule.setUpdatedAt(now);

    return ruleRepository
        .save(rule)
        .flatMap(this::resolveConflicts)
        .doOnSuccess(
            saved ->
                log.info(
                    "[SystemWorkRule] Saved '{}': value={}, category={}, targetDoc={}, lastSync={}",
                    saved.getRuleKey(),
                    saved.getValue(),
                    saved.getCategory(),
                    saved.getTargetDoc(),
                    saved.getLastSyncStatus()));
  }

  /**
   * Delete a rule. Does not revert the already-propagated changes — the admin should be aware and
   * set a replacement rule if needed.
   */
  public Mono<Void> deleteRule(String ruleKey) {
    return ruleRepository
        .deleteById(ruleKey)
        .then(
            Mono.<Void>fromRunnable(() -> log.info("[SystemWorkRule] Deleted rule '{}'", ruleKey)))
        .onErrorResume(
            e -> {
              log.error("[SystemWorkRule] Failed to delete '{}': {}", ruleKey, e.getMessage());
              return Mono.empty();
            });
  }

  // ─── CONFLICT DETECTION & PROPAGATION ───────────────────────────────────

  /**
   * Reads the target document described by rule.targetDoc and compares the current value of
   * rule.targetField with rule.value.
   *
   * <p>Conflict outcomes ───────────────── NO_CONFLICT → target value already matches, no write
   * needed. CONFLICT_RESOLVED → write succeeded; decisionAudit entry appended.
   * CONFLICT_RESOLUTION_ACK → decisionAudit noted but Firestore write was deferred (no bean).
   * PROPAGATION_FAILED → Firestore write error after conflict detected. INVALID_TARGET → targetDoc
   * not in "collection/docId" format. NO_TARGET → rule has no targetDoc. DISABLED →
   * rule.isEnabled() is false. FIRESTORE_UNAVAILABLE → Firestore bean missing.
   */
  private Mono<SystemWorkRule> resolveConflicts(SystemWorkRule rule) {
    if (!rule.isEnabled()) {
      rule.setLastSyncStatus("DISABLED");
      return Mono.just(rule);
    }
    String targetDoc = rule.getTargetDoc();
    if (targetDoc == null || targetDoc.isBlank()) {
      rule.setLastSyncStatus("NO_TARGET");
      return Mono.just(rule);
    }
    String[] parts = targetDoc.split("/", 2);
    if (parts.length < 2) {
      log.warn("[SystemWorkRule] Invalid targetDoc '{}'. Expected 'collection/docId'.", targetDoc);
      rule.setLastSyncStatus("INVALID_TARGET");
      return Mono.just(rule);
    }
    String collection = parts[0];
    String docId = parts[1];
    String targetField =
        (rule.getTargetField() != null && !rule.getTargetField().isBlank())
            ? rule.getTargetField()
            : rule.getRuleKey();

    if (firestore == null) {
      rule.setLastSyncStatus("FIRESTORE_UNAVAILABLE");
      return Mono.just(rule);
    }

    return Mono.fromCallable(
            () -> {
              DocumentReference ref = firestore.collection(collection).document(docId);
              DocumentSnapshot snap = ref.get().get();

              String previousValue;
              boolean hasConflict;

              if (snap.exists()) {
                Object cur = snap.get(targetField);
                previousValue = cur != null ? String.valueOf(cur) : "(null)";
                hasConflict = !rule.getValue().equalsIgnoreCase(String.valueOf(cur));
              } else {
                previousValue = "(document does not exist)";
                hasConflict = true;
              }

              log.debug(
                  "[SystemWorkRule] Check '{}' @ {}/{}[{}]: current={}",
                  rule.getRuleKey(),
                  collection,
                  docId,
                  targetField,
                  previousValue);

              if (!hasConflict) {
                rule.setLastSyncStatus("NO_CONFLICT");
                rule.setLastSyncedAt(LocalDateTime.now());
                return rule;
              }

              // ── CONFLICT — propagate the rule value ─────────────────────
              log.warn(
                  "[SystemWorkRule] CONFLICT in {}/{}[{}]: '{}' → '{}'. Propagating rule '{}'.",
                  collection,
                  docId,
                  targetField,
                  previousValue,
                  rule.getValue(),
                  rule.getRuleKey());
              return hasConflict;
            })
        .flatMap(
            hasConflictResult -> {
              if (!(hasConflictResult instanceof Boolean bool && bool)) {
                return Mono.just(rule);
              }

              // Second Firestore call: write the correct value
              return Mono.fromCallable(
                      () -> {
                        DocumentReference ref = firestore.collection(collection).document(docId);
                        String field = targetField;

                        Object writeValue;
                        if ("OBJECT".equalsIgnoreCase(rule.getValueType())
                            && rule.getStructuredValue() != null) {
                          writeValue = Map.copyOf(rule.getStructuredValue());
                        } else {
                          writeValue = rule.getValue();
                        }
                        Map<String, Object> update = Map.of(field, writeValue);
                        ref.set(update).get();

                        // Build decision-log entry (plain map, no custom types)
                        Map<String, Object> decision = new HashMap<>();
                        decision.put("timestamp", LocalDateTime.now().toString());
                        decision.put("conflict", true);
                        decision.put("ruleKey", rule.getRuleKey());
                        decision.put("category", rule.getCategory());
                        decision.put("previousValue", writeValue);
                        decision.put("appliedValue", rule.getValue());
                        decision.put("targetCollection", collection);
                        decision.put("targetDocId", docId);
                        decision.put("targetField", field);
                        decision.put("result", "PROPAGATED");

                        // Append to the rule's decisionAudit sub-field atomically
                        String ts = LocalDateTime.now().toString();
                        Map<String, Object> auditUpdate =
                            Map.of(
                                "decisionAudit." + ts,
                                decision,
                                "lastSyncStatus",
                                "CONFLICT_RESOLVED",
                                "lastSyncedAt",
                                LocalDateTime.now());
                        firestore
                            .collection("system_work_rules")
                            .document(rule.getRuleKey())
                            .update(auditUpdate)
                            .get();
                        return decision;
                      })
                  .doOnSuccess(
                      decision ->
                          log.info(
                              "[SystemWorkRule] CONFLICT_RESOLVED for '{}': {}",
                              rule.getRuleKey(),
                              decision))
                  .thenReturn(rule)
                  .doOnSuccess(
                      r -> {
                        r.setLastSyncStatus("CONFLICT_RESOLVED");
                        r.setLastSyncedAt(LocalDateTime.now());
                      })
                  .onErrorResume(
                      e -> {
                        log.error(
                            "[SystemWorkRule] Propagation FAILED for '{}': {}",
                            rule.getRuleKey(),
                            e.getMessage(),
                            e);
                        rule.setLastSyncStatus("PROPAGATION_FAILED");
                        return Mono.just(rule);
                      });
            })
        .doOnSuccess(r -> ruleRepository.save(r).subscribe())
        .onErrorResume(
            e -> {
              log.error(
                  "[SystemWorkRule] resolveConflicts failed for '{}': {}",
                  rule.getRuleKey(),
                  e.getMessage(),
                  e);
              rule.setLastSyncStatus("PROPAGATION_FAILED");
              return Mono.just(rule);
            });
  }

  // ─── SEED ────────────────────────────────────────────────────────────────

  public Mono<Map<String, Object>> ensureDefaults() {
    SystemWorkRule r1 =
        new SystemWorkRule(
            "LEARNING_AUTO_INTERVAL",
            "LEARNING",
            "Auto-learning trigger interval (minutes or cron expression).",
            "30min");
    r1.setValueType("CRON");
    r1.setTargetDoc("system_configs/global_settings");
    r1.setTargetField("timeouts.auto_learning_interval_min");

    SystemWorkRule r2 =
        new SystemWorkRule(
            "LEARNING_SCRAPING_MODE",
            "LEARNING",
            "Internet scraping intensity / permission level.",
            "BALANCED");
    r2.setValueType("STRING");
    r2.setTargetDoc("system_configs/global_settings");
    r2.setTargetField("settings.learning_scraping_mode");

    SystemWorkRule r3 =
        new SystemWorkRule(
            "LOGGING_ACTIVITY_ENABLED",
            "LOGGING",
            "Whether learning and admin activity logging is enabled.",
            "true");
    r3.setValueType("BOOLEAN");
    r3.setTargetDoc("system_configs/global_settings");
    r3.setTargetField("settings.logging_activity_enabled");

    SystemWorkRule r4 =
        new SystemWorkRule(
            "EXECUTION_AUTO_APPROVE",
            "APPROVAL",
            "Auto-approve improvement proposals when AutoPilot is enabled.",
            "false");
    r4.setValueType("BOOLEAN");
    r4.setTargetDoc("system_configs/global_settings");
    r4.setTargetField("autoExecApprovalRequired");

    SystemWorkRule r5 =
        new SystemWorkRule(
            "APPROVAL_QUEUE_STRATEGY",
            "APPROVAL",
            "How pending proposals are handled: allow | lock | manual.",
            "manual");
    r5.setValueType("STRING");
    r5.setTargetDoc("system_configs/global_settings");
    r5.setTargetField("settings.approval_queue_strategy");

    SystemWorkRule r6 =
        new SystemWorkRule(
            "PROTOCOL_SEVERITY_THRESHOLD",
            "PROTOCOL",
            "Minimum protocol severity before auto-escalation is triggered.",
            "high");
    r6.setValueType("STRING");
    r6.setTargetDoc("system_configs/global_settings");
    r6.setTargetField("settings.protocol_severity_threshold");

    SystemWorkRule r7 =
        new SystemWorkRule(
            "BROWSER_AUTO_MODE",
            "AUTOMATION",
            "Enables autonomous browser navigation and task execution.",
            "false");
    r7.setValueType("BOOLEAN");
    r7.setTargetDoc("system_configs/global_settings");
    r7.setTargetField("settings.browser_auto_mode");

    SystemWorkRule r8 =
        new SystemWorkRule(
            "BROWSER_RESTRICTED_URLS",
            "SECURITY",
            "Comma-separated list of prohibited domains for the bot.",
            "banking.com,crypto.com,login.live.com");
    r8.setValueType("LIST");
    r8.setTargetDoc("system_configs/global_settings");
    r8.setTargetField("settings.browser_restricted_urls");

    SystemWorkRule r9 =
        new SystemWorkRule(
            "BROWSER_MAX_STEPS",
            "AUTOMATION",
            "Maximum number of autonomous steps allowed per session.",
            "50");
    r9.setValueType("INTEGER");
    r9.setTargetDoc("system_configs/global_settings");
    r9.setTargetField("settings.browser_max_steps");

    SystemWorkRule r10 =
        new SystemWorkRule(
            "GITHUB_AUTO_PUSH",
            "AUTOMATION",
            "Automatically push generated code and updates to linked GitHub repository.",
            "false");
    r10.setValueType("BOOLEAN");
    r10.setTargetDoc("system_configs/global_settings");
    r10.setTargetField("settings.github_auto_push");

    SystemWorkRule r11 =
        new SystemWorkRule(
            "GITHUB_ORG_NAME",
            "AUTOMATION",
            "Default GitHub organization or username for new app repositories.",
            "supremeai-apps");
    r11.setValueType("STRING");
    r11.setTargetDoc("system_configs/global_settings");
    r11.setTargetField("settings.github_org_name");

    return Flux.just(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11)
        .flatMap(this::saveRule)
        .collectList()
        .<Map<String, Object>>map(
            list -> {
              Map<String, Object> result = new HashMap<>();
              for (SystemWorkRule r : list) {
                result.put(r.getRuleKey(), r.getCreatedAt() == null ? "created" : "exists");
              }
              return result;
            })
        .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())));
  }
}
