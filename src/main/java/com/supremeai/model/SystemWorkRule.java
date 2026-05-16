package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * SystemWorkRule — the authoritative rule set for how the SupremeAI system
 * carries out work in the real world.
 *
 * Rules saved here take precedence over any per-module settings found in
 * system_configs, system_instructions, protocol_rules, or system_learning.
 *
 * When a conflict is detected, SystemWorkRuleService automatically propagates
 * (copies) the correct value into the conflicting Firestore document and records
 * the event in the decision_log.
 *
 * Collection: "system_work_rules"
 */
@Document(collectionName = "system_work_rules")
public class SystemWorkRule {

    @DocumentId
    private String id;

    /**
     * Unique machine-readable key used for conflict detection and lookup.
     * Convention: SECTION_ACTION (e.g. LEARNING_AUTO_INTERVAL, EXECUTION_SYSTEM_INSTRUCTION,
     * PROTOCOL_SEVERITY_THRESHOLD, etc.)
     */
    private String ruleKey;

    /**
     * Human-readable category. Used to group rules and determine the target
     * Firestore collection that may need to be synced during a conflict.
     */
    private String category; // LEARNING, EXECUTION, PROTOCOL, SCHEDULING, APPROVAL, GENERAL

    /**
     * Brief description shown in the admin UI.
     */
    private String description;

    /**
     * The effective value the system should honour. The interpretation of this
     * field depends on the rule type.
     */
    private String value;

    /**
     * Optional structured value for complex rules (e.g. a cron expression,
     * JSON config, list of instruction IDs).
     */
    private Map<String, Object> structuredValue;

    /**
     * The data type of the value field.
     */
    private String valueType; // BOOLEAN, INTEGER, STRING, CRON, OBJECT, LIST

    /**
     * Target Firestore document that this rule must be propagated into
     * when a conflict is detected.
     * Format: "collection/docId"
     * e.g. "system_configs/global_settings", "system_instructions/my_instruction"
     */
    private String targetDoc;

    /**
     * Target Firestore field inside the document that must be written to
     * during propagation. Leave blank if the whole document should be
     * merged from structuredValue.
     */
    private String targetField;

    /**
     * Whether this rule is currently active.
     */
    private boolean enabled = true;

    /**
     * Priority when multiple rules exist for the same target.
     * Higher number wins.
     */
    private int priority = 1;

    /**
     * When this rule was last modified.
     */
    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;

    /**
     * Last sync result recorded by the service when a propagation was performed.
     * "synced" | "conflict_resolved" | "propagation_failed"
     */
    private String lastSyncStatus;

    private LocalDateTime lastSyncedAt;

    /**
     * Sequential decision-audit log that stores the output of each conflict-resolution
     * cycle.  New entries are appended by SystemWorkRuleService; old entries are
     * retained for the admin to audit.  Stored directly on the SystemWorkRule doc
     * as a free-form map.  Maps are deserialized by Spring Data Firestore.
     */
    private Map<String, Map<String, Object>> decisionAudit = new HashMap<>();

    /**
     * Free-form note explaining why the rule was created / last updated.
     */
    private String changeNote;

    public SystemWorkRule() {
        this.enabled = true;
        this.priority = 1;
        this.structuredValue = new HashMap<>();
    }

    public SystemWorkRule(String ruleKey, String category, String description, String value) {
        this();
        this.ruleKey = ruleKey;
        this.category = category;
        this.description = description;
        this.value = value;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Map<String, Object> getStructuredValue() { return structuredValue; }
    public void setStructuredValue(Map<String, Object> structuredValue) { this.structuredValue = structuredValue; }

    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }

    public String getTargetDoc() { return targetDoc; }
    public void setTargetDoc(String targetDoc) { this.targetDoc = targetDoc; }

    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getChangeNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }

    public Map<String, Map<String, Object>> getDecisionAudit() { return decisionAudit; }
    public void setDecisionAudit(Map<String, Map<String, Object>> decisionAudit) {
        this.decisionAudit = decisionAudit;
    }
}
