# SupremeAI — Database Schema & Linkage Map
# ডেটাবেস সংযোগ নকশা ও অবস্থা

**তারিখ:** ২০২৬-০৫-১৫  
**Database:** Google Cloud Firestore (Spring Data Reactive)  
**সংযোগ পদ্ধতি:** `FirestoreReactiveRepository` (Spring GCP)

---

## 🔑 কিভাবে একটি Entity ডেটাবেসে সংরক্ষিত হয়

```
Java Model Class
   └── @Document(collectionName = "xyz")   ← Firestore collection নাম নির্ধারণ
   └── @DocumentId                          ← Document ID field

Java Repository Interface
   └── extends FirestoreReactiveRepository<Model> ← Firestore CRUD
   └── Custom query methods (Flux<>/Mono<>)

Service Class
   └── repository.save() / findAll() / findById() ← ব্যবহার করে

Controller
   └── service.method()  ← API endpoint
```

---

## ✅ সম্পূর্ণ সংযুক্ত Entities (Model + Collection + Repository সবকিছু আছে)

| # | Model Class | Firestore Collection | Repository | অবস্থা |
|---|-------------|---------------------|------------|--------|
| 1 | `ActivityLog` | `activity_logs` | `ActivityLogRepository` | ✅ সম্পূর্ণ |
| 2 | `Agent` | `ai_agents` | `AgentRepository` | ✅ সম্পূর্ণ |
| 3 | `AnalysisBaseline` | `analysis_baselines` | `AnalysisBaselineRepository` | ✅ সম্পূর্ণ |
| 4 | `AnalysisFinding` | `analysis_findings` | `AnalysisFindingRepository` | ✅ সম্পূর্ণ |
| 5 | `AnalysisFix` | `analysis_fixes` | `AnalysisFixRepository` | ✅ সম্পূর্ণ |
| 6 | `AnalysisJob` | `analysis_jobs` | `AnalysisJobRepository` | ✅ সম্পূর্ণ |
| 7 | `APIHealthReport` | `api_health_reports` | `APIHealthReportRepository` | ✅ সম্পূর্ণ |
| 8 | `BrowserActivity` | `browser_activities` | `BrowserActivityRepository` | ✅ সম্পূর্ণ |
| 9 | `BrowserFinding` | `browser_findings` | `BrowserFindingRepository` | ✅ সম্পূর্ণ |
| 10 | `BrowserTask` | `browser_tasks` | `BrowserTaskRepository` | ✅ সম্পূর্ণ |
| 11 | `ChatAdminAction` | `chat_admin_actions` | `ChatAdminActionRepository` | ✅ সম্পূর্ণ |
| 12 | `ChatCommand` | `chat_commands` | `ChatCommandRepository` | ✅ সম্পূর্ণ |
| 13 | `ChatConfirmation` | `chat_confirmations` | `ChatConfirmationRepository` | ✅ সম্পূর্ণ |
| 14 | `ChatMessage` | `chat_messages` | `ChatHistoryRepository` | ✅ সম্পূর্ণ |
| 15 | `ChatPlan` | `chat_plans` | `ChatPlanRepository` | ✅ সম্পূর্ণ |
| 16 | `ChatRule` | `chat_rules` | `ChatRuleRepository` | ✅ সম্পূর্ণ |
| 17 | `ChatSession` | `chat_sessions` | `ChatSessionRepository` | ✅ সম্পূর্ণ |
| 18 | `CodeChunk` | `code_embeddings` | `CodeChunkRepository` | ✅ সম্পূর্ণ |
| 19 | `DependencyGraph` | `dependency_graphs` | `DependencyGraphRepository` | ✅ সম্পূর্ণ |
| 20 | `ExistingProject` | `projects` | `ProjectRepository` | ✅ সম্পূর্ণ |
| 21 | `GeneratedApp` | `generated_apps` | `GeneratedAppRepository` | ✅ সম্পূর্ণ |
| 22 | `ImprovementProposal` | `improvement_proposals` | `ImprovementProposalRepository` | ✅ সম্পূর্ণ |
| 23 | `KnowledgeDomain` | `knowledge_domains` | `KnowledgeDomainRepository` | ✅ সম্পূর্ণ |
| 24 | `KnowledgeRecommendation` | `knowledge_recommendations` | `KnowledgeRecommendationRepository` | ✅ সম্পূর্ণ |
| 25 | `Milestone` | `milestones` | `MilestoneRepository` | ✅ সম্পূর্ণ |
| 26 | `ModelEvolution` | `model_evolution` | `ModelEvolutionRepository` | ✅ সম্পূর্ণ |
| 27 | `ProtocolRule` | `protocol_rules` | `ProtocolRuleRepository` | ✅ সম্পূর্ণ |
| 28 | `ReverseEngineeringJob` | `reverse_engineering_jobs` | `ReverseEngineeringJobRepository` | ✅ সম্পূর্ণ |
| 29 | `SolutionMemory` | `solution_memories` | `SolutionMemoryRepository` | ✅ সম্পূর্ণ |
| 30 | `StorageMetadata` | `storage_metadata` | `StorageMetadataRepository` | ✅ সম্পূর্ণ |
| 31 | `StoredCredential` | `browser_credentials` | `StoredCredentialRepository` | ✅ সম্পূর্ণ |
| 32 | `SystemConfig` | `system_configs` | `SystemConfigRepository` | ✅ সম্পূর্ণ |
| 33 | `SystemInstruction` | `system_instructions` | `SystemInstructionRepository` | ✅ সম্পূর্ণ |
| 34 | `SystemLearning` | `system_learning` | `SystemLearningRepository` | ✅ সম্পূর্ণ |
| 35 | `TaskProviderAssignment` | `task_provider_assignments` | `TaskProviderAssignmentRepository` | ✅ সম্পূর্ণ |
| 36 | `UrlPermission` | `browser_url_permissions` | `UrlPermissionRepository` | ✅ সম্পূর্ণ |
| 37 | `UrlPermissionRequest` | `browser_url_requests` | `UrlPermissionRequestRepository` | ✅ সম্পূর্ণ |
| 38 | `User` | `users` | `UserRepository` | ✅ সম্পূর্ণ |
| 39 | `UserApiKey` | `user_api_keys` | `UserApiKeyRepository` | ✅ সম্পূর্ণ |
| 40 | `UserGuide` | `user_guides` | `UserGuideRepository` | ✅ সম্পূর্ণ |
| 41 | `UserLanguagePreference` | `user_preferences` | `UserLanguagePreferenceRepository` | ✅ সম্পূর্ণ |
| 42 | `UserSimulatorProfile` | `simulator_profiles` | `UserSimulatorProfileRepository` | ✅ সম্পূর্ণ |
| 43 | `VPNConnection` | `vpn_connections` | `VPNRepository` | ✅ সম্পূর্ণ |
| 44 | `WorkflowDefinition` | `workflow_definitions` | `WorkflowDefinitionRepository` | ✅ সম্পূর্ণ |
| 45 | `WorkflowExecution` | `workflow_executions` | `WorkflowExecutionRepository` | ✅ সম্পূর্ণ |

---

## ⚠️ আংশিক সংযুক্ত — Repository আছে কিন্তু সমস্যা আছে

| # | Model/Class | Collection | সমস্যা | করণীয় |
|---|-------------|------------|--------|--------|
| 1 | `APIProvider` | `api_providers` | ✅ `@Document` আছে কিন্তু `APIProviderRepository` নেই; `ProviderRepository` আছে | `ProviderRepository` → rename বা নতুন `APIProviderRepository` তৈরি |
| 2 | `SimulatorDeploymentRecord` | `simulator_deployments` | ✅ `@Document` আছে কিন্তু `SimulatorDeploymentRepository` ভিন্ন model ব্যবহার করতে পারে | Repository-Model binding যাচাই করুন |

---

## ❌ @Document আছে কিন্তু Repository নেই (DB-তে লেখা যাচ্ছে না)

| # | Model Class | Collection | সমস্যা | করণীয় |
|---|-------------|------------|--------|--------|
| 1 | `ProviderTaskPerformance` | `provider_task_performance` | Repository নেই | `ProviderTaskPerformanceRepository` তৈরি করুন |
| 2 | `HealingEvent` | ❓ collection নেই | `@Document` নেই — in-memory only | Collection ঠিক করুন + Repository তৈরি করুন |

---

## 🚫 Model আছে কিন্তু Database-এ সংরক্ষণের ব্যবস্থা নেই (DTO/Utility Classes)

এগুলো শুধু in-memory বা DTO হিসেবে ব্যবহার হয়, Firestore-এ save হয় না:

| # | Class | ব্যবহার | Database দরকার কি? |
|---|-------|---------|------------------|
| 1 | `AIBehaviorProfile` | AI behavior configuration | ✅ দরকার — `ai_behavior_profiles` collection তৈরি করুন |
| 2 | `ConsensusResult` | Voting result (in-memory) | ⚠️ অস্থায়ী — লগের জন্য save করা উচিত |
| 3 | `ConsensusVote` | Individual vote (in-memory) | ⚠️ অস্থায়ী — audit trail হিসেবে save করুন |
| 4 | `DataCategory` | Data classification | ❓ config হলে save দরকার |
| 5 | `EntityDefinition` | Schema definition | ✅ দরকার — dynamic schema এর জন্য |
| 6 | `ErrorResponse` | HTTP error wrapper | ❌ দরকার নেই — শুধু response DTO |
| 7 | `FieldDefinition` | Field schema | ✅ দরকার — EntityDefinition এর অংশ |
| 8 | `KnowledgeEntry` | Knowledge item | ✅ দরকার — `knowledge_entries` collection তৈরি করুন |
| 9 | `ProviderVote` | Vote tracking | ⚠️ `ConsensusVote` এর মতো |
| 10 | `ReasoningLog` | AI reasoning trace | ✅ দরকার — `reasoning_logs` collection তৈরি করুন |
| 11 | `UserTier` | Subscription tier info | ✅ দরকার — `User` model এ embed বা আলাদা collection |
| 12 | `WorkflowStep` | Single step of workflow | ✅ দরকার — `WorkflowExecution` এর sub-collection হিসেবে |
| 13 | `AnalysisRequest` | API request DTO | ❌ দরকার নেই |
| 14 | `AnalysisResponse` | API response DTO | ❌ দরকার নেই |

---

## 📊 Firestore Collections — সম্পূর্ণ তালিকা

```
Firestore Database
├── 👤 User & Auth
│   ├── users                        ✅ (User)
│   ├── user_api_keys                ✅ (UserApiKey)
│   ├── user_preferences             ✅ (UserLanguagePreference)
│   ├── simulator_profiles           ✅ (UserSimulatorProfile)
│   └── activity_logs               ✅ (ActivityLog)
│
├── 💬 Chat & Communication
│   ├── chat_messages                ✅ (ChatMessage → ChatHistoryRepository)
│   ├── chat_sessions                ✅ (ChatSession)
│   ├── chat_commands                ✅ (ChatCommand)
│   ├── chat_plans                   ✅ (ChatPlan)
│   ├── chat_rules                   ✅ (ChatRule)
│   ├── chat_confirmations           ✅ (ChatConfirmation)
│   └── chat_admin_actions           ✅ (ChatAdminAction)
│
├── 🤖 AI & Providers
│   ├── api_providers                ✅ (APIProvider → ProviderRepository)
│   ├── task_provider_assignments    ✅ (TaskProviderAssignment)
│   ├── provider_task_performance    ⚠️ (ProviderTaskPerformance — Repository নেই)
│   └── model_evolution              ✅ (ModelEvolution)
│
├── 🧠 Knowledge & Learning
│   ├── system_learning              ✅ (SystemLearning)
│   ├── knowledge_domains            ✅ (KnowledgeDomain)
│   ├── knowledge_recommendations    ✅ (KnowledgeRecommendation)
│   ├── solution_memories            ✅ (SolutionMemory)
│   ├── improvement_proposals        ✅ (ImprovementProposal)
│   ├── user_guides                  ✅ (UserGuide)
│   └── [knowledge_entries]          ❌ নেই (KnowledgeEntry model আছে)
│
├── 📦 Projects & Apps
│   ├── projects                     ✅ (ExistingProject → ProjectRepository)
│   ├── generated_apps               ✅ (GeneratedApp)
│   ├── milestones                   ✅ (Milestone)
│   ├── workflow_definitions         ✅ (WorkflowDefinition)
│   └── workflow_executions          ✅ (WorkflowExecution)
│
├── 🔍 Code Analysis
│   ├── analysis_jobs                ✅ (AnalysisJob)
│   ├── analysis_findings            ✅ (AnalysisFinding)
│   ├── analysis_fixes               ✅ (AnalysisFix)
│   ├── analysis_baselines           ✅ (AnalysisBaseline)
│   ├── code_embeddings              ✅ (CodeChunk)
│   └── dependency_graphs            ✅ (DependencyGraph)
│
├── 🔧 System & Config
│   ├── system_configs               ✅ (SystemConfig)
│   ├── system_instructions          ✅ (SystemInstruction)
│   ├── protocol_rules               ✅ (ProtocolRule)
│   ├── ai_agents                    ✅ (Agent)
│   └── api_health_reports           ✅ (APIHealthReport)
│
├── 🌐 Browser & Automation
│   ├── browser_tasks                ✅ (BrowserTask)
│   ├── browser_activities           ✅ (BrowserActivity)
│   ├── browser_findings             ✅ (BrowserFinding)
│   ├── browser_credentials          ✅ (StoredCredential)
│   ├── browser_url_permissions      ✅ (UrlPermission)
│   └── browser_url_requests         ✅ (UrlPermissionRequest)
│
├── 🖥️ Simulator
│   ├── simulator_profiles           ✅ (UserSimulatorProfile)
│   ├── simulator_deployments        ⚠️ (SimulatorDeploymentRecord — binding যাচাই দরকার)
│   └── reverse_engineering_jobs     ✅ (ReverseEngineeringJob)
│
├── 🔒 Security
│   ├── vpn_connections              ✅ (VPNConnection → VPNRepository)
│   └── storage_metadata             ✅ (StorageMetadata)
│
└── ❌ Missing Collections (তৈরি করা দরকার)
    ├── [ai_behavior_profiles]       ❌ (AIBehaviorProfile model আছে)
    ├── [reasoning_logs]             ❌ (ReasoningLog model আছে)
    ├── [knowledge_entries]          ❌ (KnowledgeEntry model আছে)
    ├── [healing_events]             ❌ (HealingEvent — @Document নেই)
    ├── [consensus_results]          ❌ (ConsensusResult — in-memory)
    └── [user_tiers]                 ❌ (UserTier — @Document নেই)
```

---

## 📈 সংখ্যাগত সারাংশ

| বিভাগ | সংখ্যা |
|------|--------|
| ✅ সম্পূর্ণ সংযুক্ত (Model + Collection + Repository) | **45** |
| ⚠️ আংশিক সংযুক্ত (কিছু সমস্যা আছে) | **2** |
| ❌ @Document আছে কিন্তু Repository নেই | **2** |
| 🚫 Model আছে কিন্তু DB সংযোগ নেই (DTO) | **13** |
| 🔴 DB-তে save হওয়া উচিত কিন্তু collection নেই | **6** |
| **মোট Collections (বর্তমান)** | **~44** |
| **মোট Models** | **~75** |

---

## 🛠️ করণীয় কাজ (Priority অনুযায়ী)

### 🔴 জরুরি (Critical)

```
1. HealingEvent — @Document annotation যোগ করুন:
   @Document(collectionName = "healing_events")

2. ProviderTaskPerformanceRepository তৈরি করুন:
   public interface ProviderTaskPerformanceRepository
       extends FirestoreReactiveRepository<ProviderTaskPerformance> {}

3. AIBehaviorProfile — @Document যোগ করুন + Repository তৈরি করুন:
   @Document(collectionName = "ai_behavior_profiles")
```

### 🟡 High Priority

```
4. KnowledgeEntry — @Document যোগ করুন:
   @Document(collectionName = "knowledge_entries")
   + KnowledgeEntryRepository তৈরি করুন

5. ReasoningLog — @Document যোগ করুন:
   @Document(collectionName = "reasoning_logs")
   + ReasoningLogRepository তৈরি করুন

6. ConsensusResult + ConsensusVote — audit trail এর জন্য:
   @Document(collectionName = "consensus_results")
   @Document(collectionName = "consensus_votes")
```

### 🟢 Medium Priority

```
7. UserTier — User model এ embed করুন বা আলাদা collection তৈরি করুন
8. WorkflowStep — WorkflowExecution এর sub-collection হিসেবে যোগ করুন
9. SimulatorDeploymentRepository binding যাচাই করুন
```

---

## 🔗 Flutter Admin App — Database Linkage

Flutter app (`supremeai/`) এ Firestore সংযোগ:

```
supremeai/lib/services/    ← Firestore service calls
supremeai/lib/providers/   ← State management (Riverpod/Provider)
supremeai/lib/screens/     ← UI screens
```

| Flutter Screen | Backend API | Firestore Collection |
|---------------|-------------|---------------------|
| `login_screen.dart` | `/api/auth/**` | `users` |
| `api_keys_screen.dart` | `/api/api-keys/**` | `user_api_keys` |
| `settings_screen.dart` | `/api/config/**` | `system_configs` |
| `analytics/` | `/api/analytics/**` | `activity_logs` |
| `providers/` | `/api/providers/**` | `api_providers` |
| `projects/` | `/api/projects/**` | `projects` |
| `learning/` | `/api/learning/**` | `system_learning` |
| `dashboard/` | `/api/admin/**` | Multiple collections |

---

*দস্তাবেজ তৈরি: ২০২৬-০৫-১৫ | Antigravity AI Assistant*  
*পরবর্তী আপডেট: নতুন Model/Repository যোগ হলে*
