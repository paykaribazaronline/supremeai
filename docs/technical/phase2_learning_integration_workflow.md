# Phase 2: Learning & Integration Workflow
## "The Soul" — SupremeAI Ecosystem Plan

**Status:** 🚧 In Progress  
**Started:** 2026-05-17  
**Target:** Connect SupremeAI to all hubs, test learning loop and system suggestion mechanism

---

## 1. Phase 2 Objectives

Per [`supremeai_ecosystem_plan.md`](../architecture/supremeai_ecosystem_plan.md), Phase 2 is titled **"Learning & Integration (The Soul)"** and has three primary goals:

1. **Connect SupremeAI to all hubs** — Wire `SupremeLearningOrchestrator` into every hub service so intent routing flows through the central brain.
2. **Learning loop testing** — Validate that user corrections flow: `ChatProcessingService → recordCorrection() → GlobalKnowledgeBase → Firestore`.
3. **System Suggestion Mechanism** — Implement and test the three proactive suggestion types:
   - Auto-Model Suggestion
   - Link-Based Evaluation
   - Gap Analysis

---

## 2. Current Architecture State (Pre-Phase 2)

### 2.1 What Already Exists

| Component | File | Status |
|-----------|------|--------|
| SupremeLearningOrchestrator | `learning/SupremeLearningOrchestrator.java` | ✅ Exists — keyword-based intent matching |
| EnhancedSelfLearningRouter | `learning/EnhancedSelfLearningRouter.java` | ✅ Exists — Q-learning router with Boltzmann exploration |
| GlobalKnowledgeBase | `learning/knowledge/GlobalKnowledgeBase.java` | ✅ Exists — Firestore-backed solution memory |
| SupremeAIBrain | `service/SupremeAIBrain.java` | ✅ Exists — central execution point |
| AIProviderDiscoveryService | `service/AIProviderDiscoveryService.java` | ✅ Exists — dynamic model discovery |
| LearningAdminController | `controller/LearningAdminController.java` | ✅ Exists — admin learning endpoints |
| SystemLearningController | `controller/SystemLearningController.java` | ✅ Exists — system learning CRUD |
| EnhancedLearningService | `service/EnhancedLearningService.java` | ✅ Exists — NLP/multimodal learning |
| core_knowledge.json | `src/main/resources/core_knowledge.json` | ✅ Exists — v5.0.0-SuperHub, intent taxonomy |

### 2.2 What Is Missing / Needs Phase 2 Work

| Gap | Description |
|-----|-------------|
| **Orchestrator not wired into Brain** | `SupremeAIBrain.think()` routes through `AIFallbackOrchestrator` but never calls `SupremeLearningOrchestrator.identifyBestHub()` for intent classification |
| **Keyword-based intent matching** | `SupremeLearningOrchestrator.identifyBestHub()` uses simple `String.contains()` — needs vector/semantic upgrade |
| **Learning loop not closed** | `ChatProcessingService` does not call `recordCorrection()` on user feedback |
| **System Suggestion Logic stub** | `proposeLogicRefinement()` only logs — needs real admin notification + dashboard integration |
| **No Phase 2 API endpoints** | No REST endpoint for learning loop health, correction stats, or suggestion queue |
| **core_knowledge.json incomplete** | Missing Phase 2 logic patterns (vector matching, suggestion criteria, learning loop schema) |

---

## 3. Phase 2 Implementation Plan

### Step 1: Wire Orchestrator into SupremeAIBrain

**File:** `service/SupremeAIBrain.java`

**Change:** Inject `SupremeLearningOrchestrator` and call `identifyBestHub()` before routing to `AIFallbackOrchestrator`. This ensures every request passes through the intent taxonomy.

```java
@Autowired
private SupremeLearningOrchestrator learningOrchestrator;

// In think(String taskCategory, String prompt, String errorSignature):
Map<String, String> hubInfo = learningOrchestrator.identifyBestHub(prompt);
String suggestedHub = hubInfo.get("hub");
String suggestedCluster = hubInfo.get("cluster");
log.info("[BRAIN] Intent → Hub: {} | Cluster: {}", suggestedHub, suggestedCluster);
// Pass hub info to fallbackOrchestrator for provider selection
```

**Why:** This is the critical integration point. Without it, `SupremeLearningOrchestrator` is a standalone utility that no code path actually uses.

---

### Step 2: Upgrade Intent Matching to Vector-Based

**File:** `learning/SupremeLearningOrchestrator.java`

**Change:** Replace `String.contains()` keyword matching with a lightweight embedding-based similarity score using `Multilingual-E5` (already in the provider registry) or a simple TF-IDF cosine similarity as a self-contained fallback.

**Approach:**
- Add `computeSimilarity(String query, String candidate)` using character n-gram overlap + keyword weight scoring
- Sort all taxonomy categories by similarity score, pick top match
- Fall back to keyword matching if embedding service unavailable (solo-capable)

```java
private String findBestHubBySimilarity(String query) {
    // Score each taxonomy category by keyword overlap + n-gram similarity
    // Return highest-scoring hub
}
```

**Why:** Keyword matching fails on paraphrased or multilingual (Bengali) queries. Vector/semantic matching is required for Phase 2 "Command Superiority."

---

### Step 3: Close the Learning Loop

**File:** `service/ChatProcessingService.java` (or wherever user feedback is handled)

**Change:** After a user provides feedback/correction on an AI response, call `SupremeLearningOrchestrator.recordCorrection()`.

```java
// When user corrects a response:
learningOrchestrator.recordCorrection(
    originalIntent,    // what the system thought the user wanted
    correctedHub,      // what the user actually wanted
    userFeedback       // user's correction text
);
```

**Also:** Ensure `GlobalKnowledgeBase.recordSuccessWithPermission()` is called when a fix is confirmed successful.

**Why:** This is the self-learning loop. Without it, corrections are lost and the system never improves.

---

### Step 4: Implement System Suggestion Logic

**File:** `learning/SupremeLearningOrchestrator.java` (extend existing stub)

**Three sub-features:**

#### 4a. Auto-Model Suggestion
When a provider fails repeatedly for a task type, suggest deploying a better model.

```java
public List<SystemSuggestion> checkForModelGaps() {
    // Scan providerStats for any task type where all providers have < 50% success rate
    // Return suggestions like: "Task 'code_generation' has 35% avg success. Consider adding DeepSeek-Coder-V2."
}
```

#### 4b. Link-Based Evaluation
When admin shares a model link (HuggingFace/GitHub), analyze it and compare against deployed models.

```java
public SystemSuggestion evaluateModelLink(String url) {
    // Fetch model metadata from URL
    // Compare parameter count, context window, task accuracy against current deployed models
    // Return recommendation: DEPLOY / EVALUATE / SKIP
}
```

#### 4c. Gap Analysis
When the system detects it cannot handle a task type with current providers.

```java
public SystemSuggestion detectIntelligenceGap(String failedTaskType) {
    // Check if failedTaskType has any provider with > 0 success
    // If none, suggest: "I need a [X] model for [task]. Current providers cannot handle this."
}
```

**Why:** These are the "proactive" features that make SupremeAI self-improving rather than reactive.

---

### Step 5: Add Phase 2 REST API Endpoints

**New file:** `controller/LearningLoopController.java`

```java
@RestController
@RequestMapping("/api/admin/learning-loop")
@PreAuthorize("hasRole('ADMIN')")
public class LearningLoopController {
    
    @GetMapping("/health")           // Learning loop health status
    @GetMapping("/corrections")      // Recent correction history
    @GetMapping("/suggestions")      // Pending system suggestions
    @PostMapping("/approve/{id}")    // Admin approves a system suggestion
    @GetMapping("/router-stats")     // Q-learning router statistics
}
```

**Why:** Admin needs visibility into the learning loop to verify Phase 2 is working correctly.

---

### Step 6: Update core_knowledge.json

**File:** `src/main/resources/core_knowledge.json`

**Additions:**
1. `vector_intent_matching` section under `meta_logic_patterns`
2. `learning_loop_schema` section describing the correction → knowledge base → Firestore flow
3. `system_suggestion_criteria` section with evaluation weights
4. New intent categories for Phase 2-specific tasks (model evaluation, gap analysis)

**Why:** The orchestrator reads from this file. New logic patterns must be declared here for the system to use them.

---

### Step 7: Update Ecosystem Plan

**File:** `docs/architecture/supremeai_ecosystem_plan.md`

**Change:** Mark Phase 2 as complete with a summary of what was built, and update Phase 3 prerequisites.

---

## 4. Data Flow Diagram (Phase 2)

```
User Query
    │
    ▼
SupremeAIBrain.think()
    │
    ├─► SupremeLearningOrchestrator.identifyBestHub(query)
    │       │
    │       ├─► [NEW] Vector similarity scoring against intent_taxonomy
    │       └─► Returns: hub + cluster
    │
    ├─► AIFallbackOrchestrator.executeWithSupremeIntelligence(task, hub, prompt)
    │       │
    │       └─► Routes to best provider for the identified hub
    │
    └─► [ON FEEDBACK] recordCorrection() → GlobalKnowledgeBase → Firestore
                    │
                    └─► [IF 3+ corrections] proposeLogicRefinement() → Admin Dashboard
```

---

## 5. Success Criteria for Phase 2

| Criterion | How to Verify |
|-----------|---------------|
| Orchestrator wired into Brain | Log shows `[BRAIN] Intent → Hub:` on every request |
| Vector intent matching | Bengali/paraphrased queries route to correct hub (not default) |
| Learning loop closed | `learning_reservoir.recent_corrections` grows in `core_knowledge.json` after user feedback |
| System suggestions appear | Admin dashboard shows suggestion cards for model gaps |
| Router stats available | `GET /api/admin/learning-loop/router-stats` returns Q-table metrics |
| Build passes | `./gradlew build -x test` succeeds with 0 errors |

---

## 6. Files to Modify (Summary)

| File | Action |
|------|--------|
| `service/SupremeAIBrain.java` | Inject + call `SupremeLearningOrchestrator` |
| `learning/SupremeLearningOrchestrator.java` | Add vector matching, system suggestion logic |
| `service/ChatProcessingService.java` | Call `recordCorrection()` on user feedback |
| `controller/LearningLoopController.java` | **NEW** — Phase 2 API endpoints |
| `src/main/resources/core_knowledge.json` | Add Phase 2 logic patterns |
| `docs/architecture/supremeai_ecosystem_plan.md` | Mark Phase 2 complete |

---

*Created by Kilo Code | Phase 2 Workflow | 2026-05-17*
