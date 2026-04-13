# Service Consolidation Reference Guide

## Quick Lookup: What to Do With Each Duplicate

### Group 1: CRITICAL - Naming Issues

| From | To | Action | Priority |
|------|----|---------| --------|
| `HealthPingServiceService.java` | `HealthPingService.java` | Rename | P0 |
| `EnterpriseResilienceOrchestratorServiceService.java` | `EnterpriseResilienceOrchestratorService.java` | Rename | P0 |
| `ourService.java` | `OurService.java` | Rename or Delete | P0 |
| `suggestionService.java` | `SuggestionService.java` | Rename or Delete | P0 |
| `CICDService.java` | N/A | **DELETE** | P0 |

### Group 2: Firebase Layer (Data Persistence)

| File | Status | Merge Into | Reason | Priority |
|------|--------|-----------|---------|----------|
| `FirebaseService.java` | KEEP | - | Primary Firebase handler | P1 |
| `FirebaseServiceFixed.java` | DELETE | FirebaseService | Duplicate + logging | P1 |
| `OptimizedFirebaseSyncService.java` | KEEP | - | Specialized batch optimization | P1 |
| `ProjectAnalysisFirebaseService.java` | REVIEW | FirebaseService? | Check if project-specific or just wrapper | P2 |

**Merge Strategy for Firebase:**

```java
// FirebaseService (expand to include)
// ✅ Current: init(), getDatabase(), getAuth(), getSystemConfig()
// ✅ ADD from FirebaseServiceFixed: Logger, detailed error handling
// ✅ ADD: Retry logic, fallback handling
// ✅ KEEP SEPARATE: OptimizedFirebaseSyncService (optional optimization)
```

### Group 3: AI Provider Routing (Intelligent Request Direction)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `AIProviderRoutingService.java` | KEEP | - | Primary router (metrics-based) | P2 |
| `AIProviderDiscoveryService.java` | DELETE | AIProviderRoutingService | Add discovery methods | P2 |
| `AICapabilityRouter.java` | DELETE | AIProviderRoutingService | Merge capability matching | P2 |
| `CapabilityBasedAIRoutingService.java` | DELETE | AIProviderRoutingService | DUPLICATE of above | P2 |
| `PublicAIRouter.java` | REVIEW | ? | Understand first | P3 |

**Merge Strategy:**

```java
// AIProviderRoutingService (expand with)
// ✅ Current: routeByMetrics(), getProviderMetrics()
// ✅ ADD from AIProviderDiscoveryService: discoverProviders(), getConfiguredProviders()
// ✅ ADD from AICapabilityRouter: routeByCapability(), matchCapability()
// ✅ REMOVE: Duplicate implementations in other files
```

### Group 4: Quota Management (Resource Allocation)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `QuotaService.java` | KEEP | - | Primary quota tracker | P2 |
| `QuotaRotationService.java` | DELETE | QuotaService | Add rotateQuota() method | P2 |
| `UserQuotaService.java` | KEEP? | - | Only if per-user != global | P3 |
| `QuotaTracker.java` | REVIEW | QuotaService? | Check if wrapper or interface | P3 |

**Decision Tree:**

```
IF UserQuotaService != wrapping QuotaService:
  KEEP UserQuotaService (separate per-user tracking)
ELSE:
  DELETE UserQuotaService (use QuotaService instead)

IF QuotaTracker is interface:
  KEEP QuotaTracker (as contract)
  Make QuotaService implement it
ELSE:
  DELETE QuotaTracker (consolidate into QuotaService)
```

### Group 5: Git Integration (Version Control)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `GitService.java` | KEEP | - | Core git operations | P2 |
| `GitIntegrationService.java` | DELETE | GitService | Add integration methods | P2 |
| `GitHubAppService.java` | KEEP? | - | If GitHub-specific auth | P2 |
| `GitHubAppAuthService.java` | DELETE? | GitHubAppService | LIKELY DUPLICATE | P2 |
| `GitHubAPIService.java` | KEEP? | - | If low-level wrapper | P3 |

**Merge Strategy:**

```java
// GitService (expand with)
// ✅ Current: commitChanges(), pushToRemote(), getCurrentBranch()
// ✅ ADD from GitIntegrationService: integration-level methods
// ✅ CONSIDER: Move GitHub-specific to GitHubAppService if needed

// Verify:
// - Does GitHubAppAuthService do different auth than GitHubAppService?
// - If NO → DELETE GitHubAppAuthService
// - If YES → Keep both and document the difference
```

### Group 6: Signing & Security (Cryptographic Operations)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `SecureSigningService.java` | KEEP | - | Primary signing | P2 |
| `SigningAuditService.java` | DELETE | SecureSigningService | Add audit methods | P2 |
| `CloudKMSAPKSigningService.java` | EVALUATE | - | AWS/Cloud KMS specific? | P3 |
| `APKSigningAuditLogger.java` | DELETE | SigningAuditService | Merge audit logging | P3 |

**Merge Strategy:**

```java
// SecureSigningService (expand with)
// ✅ Current: signAPK(), validateSignature()
// ✅ ADD from SigningAuditService: auditSign(), getSigningHistory()
// ✅ ADD from APKSigningAuditLogger: logSigningOperation()

// Consider decorator pattern:
// SigningAuditDecorator wraps SecureSigningService for audit trail
```

### Group 7: Metrics Collection (Observability)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `MetricsService.java` | KEEP | - | Primary metrics collector | P2 |
| `MetricsCleanupService.java` | DELETE | MetricsService | Add cleanup() method | P3 |
| `ServerMetricsService.java` | EVALUATE | - | Server-specific or generic? | P3 |
| `WebSocketMetricsService.java` | EVALUATE | - | WebSocket-specific metrics | P3 |

**Decision Tree:**

```
IF ServerMetricsService != just MetricsService for servers:
  DELETE ServerMetricsService (consolidate)
ELSE:
  Check if distinct behavior (e.g., different interval, metrics)
  KEEP if significantly different

Same for WebSocketMetricsService
```

### Group 8: Cost Intelligence (Financial Analysis)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `CostIntelligenceService.java` | KEEP | - | Primary cost tracker | P3 |
| `RealCostIntelligenceService.java` | DELETE | CostIntelligenceService | Merge real-time logic | P3 |

**Why consolidate:**

- Both track costs
- "Real" probably means "with actual data"
- Keep one as canonical

### Group 9: Internet Research (Knowledge Acquisition)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `InternetSearchService.java` | REVIEW | - | No @Service annotation! | P3 |
| `InternetResearchService.java` | KEEP | - | Full research service | P3 |

**Decision Tree:**

```
IF InternetSearchService is standalone utility:
  KEEP BOTH but add @Service to InternetSearchService
ELSE:
  DELETE InternetSearchService (consolidate into InternetResearchService)
```

### Group 10: Auto-Fix Loop (Error Resolution)

| File | Status | Merge Into | Priority |
|------|--------|-----------|----------|
| `AutoFixLoopService.java` | KEEP | - | Orchestrates fix loop | P3 |
| `AutoFixingService.java` | KEEP | - | Executes individual fix | P3 |
| `AutoFixDecisionIntegrator.java` | DELETE | AutoFixLoopService | Add decision integration | P3 |

**Merge Strategy:**

```
Hierarchy should be:
1. AutoFixLoopService (orchestrates loop)
   └─ calls AutoFixingService for each iteration
        └─ AutoFixDecisionIntegrator logic merged in

Don't consolidate too much - separation here is good for:
- Loop control logic (AutoFixLoopService)
- Individual fix execution (AutoFixingService)
- Decision making (merged into loop)
```

### Group 11: Learning & Knowledge Seeding

| Files | Status | Note | Priority |
|-------|--------|------|----------|
| `KnowledgeSeedService.java` (agentorchestration/learning/) | KEEP? | Check if package-specific | P3 |
| `KnowledgeReseedService.java` (service/) | CONSOLIDATE? | Different from above? | P3 |
| `IncidentLearningIngestionService.java` | KEEP | Specific to incidents | P3 |
| `ActiveLearningHarvesterService.java` | KEEP? | Different from other learning? | P3 |

**Review needed** - Many learning services scattered

### Group 12: Consensus Voting (Multi-AI Agreement)

| File | Status | Note | Priority |
|------|--------|------|----------|
| `MultiAIConsensusService.java` | KEEP | Multi-AI voting | P3 |
| `SemanticConsensusVotingService.java` | REVIEW | Semantic voting (different?) | P3 |
| `DynamicAdaptiveConsensusService.java` | REVIEW | No @Service? | P3 |

**Question:** Do all 3 serve different purposes or are they alternatives?

---

## Consolidation Steps (By Service Group)

### Template: How to Consolidate Service A into Service B

```
1. CODE ANALYSIS PHASE
   [ ] Understand ServiceA purpose and public methods
   [ ] Understand ServiceB purpose and public methods
   [ ] List unique methods in ServiceA not in ServiceB
   [ ] Identify any state/fields that ServiceA has
   [ ] Check test coverage for both services

2. MERGE PHASE
   [ ] Copy unique methods from ServiceA into ServiceB
   [ ] Add ServiceA fields to ServiceB if needed
   [ ] Consolidate constructors if custom
   [ ] Merge @Autowired dependencies
   [ ] Update JavaDoc

3. UPDATE IMPORTS PHASE
   [ ] Find all: grep -r "ServiceA\|new ServiceA\|import.*ServiceA" src/
   [ ] Replace imports: ServiceA → ServiceB
   [ ] Update @Autowired fields: ServiceA → ServiceB
   [ ] Update new ServiceA() calls to new ServiceB()

4. TESTING PHASE
   [ ] Find tests for ServiceA
   [ ] Find tests for ServiceB
   [ ] Run both test classes
   [ ] Verify new merged methods work
   [ ] Check for any regression

5. CLEANUP PHASE
   [ ] Delete ServiceA.java
   [ ] Delete ServiceATest.java if merging into ServiceBTest
   [ ] Run full test suite
   [ ] Commit with message: "Consolidate ServiceA into ServiceB (Phase X)"

6. VERIFICATION PHASE
   [ ] Code review
   [ ] Full build
   [ ] Full test run
   [ ] Verify no remaining references to ServiceA
```

---

## Files by Priority

### 🔴 DO FIRST (2 hours)

```
1. HealthPingServiceService.java        → HealthPingService.java
2. EnterpriseResilienceOrchestratorServiceService.java → Rename
3. ourService.java                      → Delete/Rename
4. suggestionService.java               → Delete/Rename  
5. CICDService.java                     → DELETE
```

### 🟠 DO SECOND (4-6 hours)

```
6. FirebaseServiceFixed.java            → Merge into FirebaseService
7. AIProviderDiscoveryService.java      → Merge into AIProviderRoutingService
8. AICapabilityRouter.java,
   CapabilityBasedAIRoutingService.java → Consolidate to one
```

### 🟡 DO THIRD (8+ hours)

```
All other groups...
```

---

## Checklist: Am I Ready to Consolidate?

Before consolidating any service pair, ask:

- [ ] Do these services have overlapping responsibilities?
- [ ] Are the methods truly independent or do they share state?
- [ ] Can I consolidate without breaking existing code?
- [ ] Have I found all usages of the source service?
- [ ] Do I understand why there were two in the first place?
- [ ] Are there tests for both services?
- [ ] Will consolidation improve code clarity?
- [ ] Have I notified the team?
- [ ] Is there a clear reason to keep them separate?

If you answer "no" to ANY of these, **DO NOT CONSOLIDATE** yet.

---

**Status:** Draft
**Last Updated:** Phase 12 Analysis
**Next: Execute Phase 1**
