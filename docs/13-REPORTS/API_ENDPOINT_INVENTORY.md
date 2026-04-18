# API Endpoint Inventory

Generated: 2026-04-05 22:50:08

| Controller | HTTP Method | Endpoint | Source File |
|------------|-------------|----------|-------------|
| ABTestController | POST | /analyze | src\main\java\org\example\controller\ABTestController.java |
| ABTestController |  | /api/v1/ab-test | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | POST | /compare | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | GET | /history | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | POST | /recommend | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | GET | /result/{testId} | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | POST | /run | src\main\java\org\example\controller\ABTestController.java |
| ABTestController | GET | /stats | src\main\java\org\example\controller\ABTestController.java |
| AdminControlController |  | /api/admin/control | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | GET | /history | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | POST | /mode | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | GET | /pending | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | POST | /pending/{id}/approve | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | POST | /pending/{id}/reject | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | POST | /resume | src\main\java\org\example\controller\AdminControlController.java |
| AdminControlController | POST | /stop | src\main\java\org\example\controller\AdminControlController.java |
| AdminDashboardController |  | /api/admin/dashboard | src\main\java\org\example\controller\AdminDashboardController.java |
| AdminDashboardController | GET | /contract | src\main\java\org\example\controller\AdminDashboardController.java |
| AdminDashboardController | GET | /health | src\main\java\org\example\controller\AdminDashboardController.java |
| AdminDashboardController | GET | /stats | src\main\java\org\example\controller\AdminDashboardController.java |
| AdminDocumentationController | POST | /add-category | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | GET | /allowed-in-root | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController |  | /api/admin/doc-rules | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | GET | /categories | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | GET | /current | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | DELETE | /remove-category | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | POST | /set-enforcement-level | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | GET | /status | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | POST | /update | src\main\java\org\example\controller\AdminDocumentationController.java |
| AdminDocumentationController | POST | /validate-document | src\main\java\org\example\controller\AdminDocumentationController.java |
| AgentLearningController |  | /api/agent-orchestration/learning | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /chains/{agentName} | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | POST | /chains/index | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /chains/stats | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | POST | /generate | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /generate/stats | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /profiles | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /profiles/{agentName} | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | POST | /profiles/rebuild | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /providers/status | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentLearningController | GET | /status | src\main\java\org\example\agentorchestration\learning\AgentLearningController.java |
| AgentOrchestrationController | GET | /active-loops | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController |  | /api/agent-orchestration | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /history | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /leaderboard | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /muonclip/{agentName} | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /routing | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /status | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | POST | /submit | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /task-types | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentOrchestrationController | GET | /tools | src\main\java\org\example\agentorchestration\AgentOrchestrationController.java |
| AgentPhasesController |  | /api/v1/agents | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | GET | /health | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase10/evolve-agents | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase10/evolve-consensus | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | GET | /phase10/knowledge-base | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase10/learn-patterns | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase7/generate-desktop | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase7/generate-ios | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase7/generate-web | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase7/publish-appstore | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase7/publish-playstore | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase8/analyze-privacy | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase8/scan-security | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase8/validate-compliance | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase9/optimize-resources | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase9/plan-budget | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | POST | /phase9/run-scenario | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | GET | /phase9/track-costs | src\main\java\org\example\controller\AgentPhasesController.java |
| AgentPhasesController | GET | /status | src\main\java\org\example\controller\AgentPhasesController.java |
| AIAgentsController | GET | /{id} | src\main\java\org\example\controller\AIAgentsController.java |
| AIAgentsController |  | /api/ai/agents | src\main\java\org\example\controller\AIAgentsController.java |
| AIModelsController | POST | /add | src\main\java\org\example\controller\AIModelsController.java |
| AIModelsController |  | /api/models | src\main\java\org\example\controller\AIModelsController.java |
| AIModelsController | GET | /search | src\main\java\org\example\controller\AIModelsController.java |
| AIOperationsController |  | /api/ai/ops | src\main\java\org\example\controller\AIOperationsController.java |
| AIOperationsController | GET | /cost-estimate | src\main\java\org\example\controller\AIOperationsController.java |
| AIOperationsController | GET | /metrics | src\main\java\org\example\controller\AIOperationsController.java |
| AIOperationsController | GET | /summary | src\main\java\org\example\controller\AIOperationsController.java |
| AIRankingController |  | /api/intelligence/ranking | src\main\java\org\example\controller\AIRankingController.java |
| AIRankingController | GET | /cost | src\main\java\org\example\controller\AIRankingController.java |
| AIRankingController | GET | /hybrid | src\main\java\org\example\controller\AIRankingController.java |
| AIRankingController | GET | /performance | src\main\java\org\example\controller\AIRankingController.java |
| AIRankingController | GET | /speed | src\main\java\org\example\controller\AIRankingController.java |
| AIRankingController | GET | /task/{taskType} | src\main\java\org\example\controller\AIRankingController.java |
| AlertingController | POST | /{alertId}/resolve | src\main\java\org\example\api\AlertingController.java |
| AlertingController | GET | /{severity} | src\main\java\org\example\api\AlertingController.java |
| AlertingController |  | /api/alerts | src\main\java\org\example\api\AlertingController.java |
| AlertingController | POST | /create | src\main\java\org\example\api\AlertingController.java |
| AlertingController | GET | /history/all | src\main\java\org\example\api\AlertingController.java |
| AlertingController | GET | /history/recent | src\main\java\org\example\api\AlertingController.java |
| AlertingController | GET | /stats | src\main\java\org\example\api\AlertingController.java |
| AssignmentsController |  | /api/assignments | src\main\java\org\example\controller\AssignmentsController.java |
| AssignmentsController | POST | /create | src\main\java\org\example\controller\AssignmentsController.java |
| AuthController |  | /api/auth | src\main\java\com\supremeai\teaching\controllers\AuthController.java |
| AuthController | POST | /login | src\main\java\com\supremeai\teaching\controllers\AuthController.java |
| AuthController | POST | /register | src\main\java\com\supremeai\teaching\controllers\AuthController.java |
| AuthController | POST | /validate | src\main\java\com\supremeai\teaching\controllers\AuthController.java |
| AuthenticationController |  | /api/auth | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /bootstrap | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /change-password | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /firebase-login | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /hash-password | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /login | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /logout | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | GET | /me | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /refresh | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /register | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /setup | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | GET | /users | src\main\java\org\example\controller\AuthenticationController.java |
| AuthenticationController | POST | /users/{userId}/disable | src\main\java\org\example\controller\AuthenticationController.java |
| AutoFixController |  | /api/v1/autofix | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /attempt/{attemptId} | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | POST | /fix-error | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | POST | /fix-with-decisions | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /health | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /integrated/{fixId} | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /integrated-stats | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /recent | src\main\java\org\example\controller\AutoFixController.java |
| AutoFixController | GET | /stats | src\main\java\org\example\controller\AutoFixController.java |
| ChatController |  | /api/chat | src\main\java\org\example\api\ChatController.java |
| ChatController | DELETE | /clear-history | src\main\java\org\example\api\ChatController.java |
| ChatController | POST | /feedback | src\main\java\org\example\api\ChatController.java |
| ChatController | GET | /history | src\main\java\org\example\api\ChatController.java |
| ChatController | POST | /send | src\main\java\org\example\api\ChatController.java |
| ChatController | GET | /stats | src\main\java\org\example\api\ChatController.java |
| ChatController | GET | /task-history | src\main\java\org\example\api\ChatController.java |
| CodeGenerationController | GET | /analytics | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController |  | /api/generation | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /batch | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | GET | /config | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | GET | /frameworks | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | GET | /health | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | GET | /history/{projectId} | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /model | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /node-service | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /react-component | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | GET | /stats | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /utility | src\main\java\org\example\api\CodeGenerationController.java |
| CodeGenerationController | POST | /validate-and-generate | src\main\java\org\example\api\CodeGenerationController.java |
| CodeValidationController |  | /api/validation | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | POST | /batch-validate | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /compare | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /config | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /framework-stats | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /health | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /readiness/{projectId} | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | GET | /report/{projectId} | src\main\java\org\example\api\CodeValidationController.java |
| CodeValidationController | POST | /validate | src\main\java\org\example\api\CodeValidationController.java |
| CostIntelligenceController | GET | /anomalies | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController |  | /api/v1/cost-intelligence | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /budget-status | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /forecast | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /health | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /multi-cloud | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /optimize | src\main\java\org\example\controller\CostIntelligenceController.java |
| CostIntelligenceController | GET | /trends | src\main\java\org\example\controller\CostIntelligenceController.java |
| DataController |  | /api/v1/data | src\main\java\org\example\controller\DataController.java |
| DataController | POST | /cache/clear | src\main\java\org\example\controller\DataController.java |
| DataController | GET | /firebase | src\main\java\org\example\controller\DataController.java |
| DataController | GET | /github/{owner}/{repo} | src\main\java\org\example\controller\DataController.java |
| DataController | GET | /health | src\main\java\org\example\controller\DataController.java |
| DataController | GET | /stats | src\main\java\org\example\controller\DataController.java |
| DataController | GET | /vercel/{projectId} | src\main\java\org\example\controller\DataController.java |
| DecisionsController |  | /api | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | POST | /decisions/create | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | GET | /decisions/list | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | POST | /v1/decisions/{decisionId}/apply | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | POST | /v1/decisions/{decisionId}/outcome | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | POST | /v1/decisions/{decisionId}/vote | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | GET | /v1/decisions/agent/{agentName} | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | POST | /v1/decisions/log | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | GET | /v1/decisions/project/{projectId} | src\main\java\org\example\controller\DecisionsController.java |
| DecisionsController | GET | /v1/decisions/stats | src\main\java\org\example\controller\DecisionsController.java |
| DeploymentController |  | /api/v1/deployment | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /cloudformation/deploy | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /configure | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /docker/build | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /docker/push | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | GET | /health/deployment | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /kubernetes/deploy | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /kubernetes/update | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | GET | /pipeline/{pipelineId} | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /pipeline/create | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /pipeline/execute | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | GET | /pipeline/history | src\main\java\org\example\controller\DeploymentController.java |
| DeploymentController | POST | /pipeline/webhook | src\main\java\org\example\controller\DeploymentController.java |
| ErrorFixingController |  | /api/fixing | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | POST | /apply | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | POST | /auto-heal/{projectId} | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | GET | /config | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | GET | /health | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | GET | /history/{projectId} | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | GET | /instructions/{issueCode} | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | GET | /stats | src\main\java\org\example\api\ErrorFixingController.java |
| ErrorFixingController | POST | /suggest | src\main\java\org\example\api\ErrorFixingController.java |
| ExecutionLogController |  | /api/execution-logs | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | POST | /cleanup/{daysToKeep} | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /daily/{date} | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /export | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /health | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /project/{projectId} | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /system | src\main\java\org\example\api\ExecutionLogController.java |
| ExecutionLogController | GET | /trends/{days} | src\main\java\org\example\api\ExecutionLogController.java |
| FailoverController |  | /api/resilience | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /circuit-breakers | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /circuit-breakers/{name} | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | POST | /circuit-breakers/{name}/reset | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | POST | /failover-chain | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /failover-chain/{serviceId} | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /failover-chain/{serviceId}/next-provider | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /health-checks | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /health-checks/{serviceId} | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | POST | /health-checks/trigger/{serviceId} | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | PUT | /providers/{providerId}/status | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /retry-stats | src\main\java\org\example\controller\FailoverController.java |
| FailoverController | GET | /summary | src\main\java\org\example\controller\FailoverController.java |
| GitController |  | /api/git | src\main\java\org\example\controller\GitController.java |
| GitController |  | /api/github | src\main\java\org\example\controller\GitController.java |
| GitController | POST | /commit | src\main\java\org\example\controller\GitController.java |
| GitController | POST | /issue | src\main\java\org\example\controller\GitController.java |
| GitController | GET | /logs | src\main\java\org\example\controller\GitController.java |
| GitController | POST | /push | src\main\java\org\example\controller\GitController.java |
| GitController | GET | /runs | src\main\java\org\example\controller\GitController.java |
| GitController | GET | /status | src\main\java\org\example\controller\GitController.java |
| GitController | GET | /workflow/{name} | src\main\java\org\example\controller\GitController.java |
| ImprovementsController |  | /api/improvements | src\main\java\org\example\controller\ImprovementsController.java |
| ImprovementsController | GET | /list | src\main\java\org\example\controller\ImprovementsController.java |
| LoadTestingController |  | /api/testing/load | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | POST | /quick-test | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | GET | /results | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | DELETE | /results | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | GET | /results/{testName} | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | POST | /spike-test | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | POST | /sustained-load-test | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | POST | /throughput-test | src\main\java\org\example\controller\LoadTestingController.java |
| LoadTestingController | POST | /websocket-stress-test | src\main\java\org\example\controller\LoadTestingController.java |
| MetricsController | GET | /alerts | src\main\java\org\example\api\MetricsController.java |
| MetricsController |  | /api/metrics | src\main\java\org\example\api\MetricsController.java |
| MetricsController | GET | /health | src\main\java\org\example\api\MetricsController.java |
| MetricsController | GET | /stats | src\main\java\org\example\api\MetricsController.java |
| MetricsController | GET | /status | src\main\java\org\example\api\MetricsController.java |
| MLIntelligenceController | GET | /anomaly-summary | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLIntelligenceController |  | /api/intelligence/ml | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLIntelligenceController | POST | /autoscale-suggestions | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLIntelligenceController | POST | /detect-anomalies | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLIntelligenceController | POST | /predict-failure | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLIntelligenceController | POST | /recommend-provider | src\main\java\org\example\controller\MLIntelligenceController.java |
| MLPredictionController |  | /api/v1/ml | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | GET | /insights/aggregate | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | GET | /model/stats | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | GET | /patterns/error-type/{errorType} | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | GET | /patterns/strategy/{strategy} | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | POST | /predict | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | POST | /rank-variants | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | GET | /recommendation/{errorType} | src\main\java\org\example\controller\MLPredictionController.java |
| MLPredictionController | POST | /train | src\main\java\org\example\controller\MLPredictionController.java |
| MultiAIConsensusController |  | /api/consensus | src\main\java\org\example\controller\MultiAIConsensusController.java |
| MultiAIConsensusController | POST | /ask | src\main\java\org\example\controller\MultiAIConsensusController.java |
| MultiAIConsensusController | GET | /history | src\main\java\org\example\controller\MultiAIConsensusController.java |
| MultiAIConsensusController | GET | /stats | src\main\java\org\example\controller\MultiAIConsensusController.java |
| NotificationController |  | /api/notifications | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | GET | /channels | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /discord | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /email | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /escalate | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | GET | /history | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /recipient | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /slack | src\main\java\org\example\controller\NotificationController.java |
| NotificationController | POST | /sms | src\main\java\org\example\controller\NotificationController.java |
| PerformanceAnalysisController |  | /api/intelligence/performance | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /best-framework | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /comparison | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /framework/{name} | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /insights | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /needs-improvement | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /recommendations | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | GET | /recommendations/critical | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PerformanceAnalysisController | POST | /record-execution | src\main\java\org\example\controller\PerformanceAnalysisController.java |
| PersistentAnalyticsController |  | /api/analytics | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /compare | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /daily | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /export/csv | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /export/json | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /historical | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /monthly | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | POST | /record | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| PersistentAnalyticsController | GET | /trend | src\main\java\org\example\controller\PersistentAnalyticsController.java |
| Phase6IntegrationController |  | /api/v1/phase6 | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase6IntegrationController | GET | /features | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase6IntegrationController | GET | /health | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase6IntegrationController | GET | /info | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase6IntegrationController | GET | /metrics | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase6IntegrationController | POST | /workflow | src\main\java\org\example\controller\Phase6IntegrationController.java |
| Phase7AgentController |  | /api/phase7/agents | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /capabilities | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | POST | /desktop/generate | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /desktop/status | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | POST | /ios/generate | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /ios/status | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | POST | /publish/prepare | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /publish/status | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /summary | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | POST | /web/generate | src\main\java\org\example\controller\Phase7AgentController.java |
| Phase7AgentController | GET | /web/status | src\main\java\org\example\controller\Phase7AgentController.java |
| ProjectGenerationController | DELETE | /{projectId} | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /{projectId}/file-content | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /{projectId}/files | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /{projectId}/logs | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /{projectId}/status | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | POST | /{projectId}/validate | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController |  | /api/projects | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | POST | /generate | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /stats/overview | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /templates/{templateType} | src\main\java\org\example\api\ProjectGenerationController.java |
| ProjectGenerationController | GET | /templates/list | src\main\java\org\example\api\ProjectGenerationController.java |
| ProvidersController | DELETE | /{id} | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | PUT | /{id} | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | POST | /add | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController |  | /api/providers | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | GET | /audit | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | GET | /available | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | GET | /configured | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | POST | /probe/{id} | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | POST | /remove | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | POST | /rotate/{id} | src\main\java\org\example\controller\ProvidersController.java |
| ProvidersController | POST | /test/{id} | src\main\java\org\example\controller\ProvidersController.java |
| QuotaController | GET | /all | src\main\java\org\example\controller\QuotaController.java |
| QuotaController |  | /api/quota | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | GET | /available | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | POST | /check/{providerId} | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | GET | /fallback-required | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | PUT | /limit/{providerId} | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | GET | /provider/{providerId} | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | POST | /record | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | POST | /reset | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | GET | /status | src\main\java\org\example\controller\QuotaController.java |
| QuotaController | GET | /summary | src\main\java\org\example\controller\QuotaController.java |
| QuotaPredictionController | GET | /analytics | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaPredictionController |  | /api/quota | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaPredictionController | GET | /prediction/{agentId} | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaPredictionController | GET | /reset-schedule | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaPredictionController | POST | /rotate | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaPredictionController | GET | /status | src\main\java\org\example\api\QuotaPredictionController.java |
| QuotaRotationController |  | /api/quotas | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /health | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /next-provider | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /optimal-provider | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /providers | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | POST | /record-failure | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | POST | /record-success | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /remaining | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | POST | /reset-monthly | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /status | src\main\java\org\example\controller\QuotaRotationController.java |
| QuotaRotationController | GET | /summary | src\main\java\org\example\controller\QuotaRotationController.java |
| RequirementController | POST | /{id}/approve | src\main\java\org\example\controller\RequirementController.java |
| RequirementController | POST | /{id}/reject | src\main\java\org\example\controller\RequirementController.java |
| RequirementController |  | /api/v1/requirements | src\main\java\org\example\controller\RequirementController.java |
| ResilienceController |  | /api/resilience | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /circuit-breaker/metrics | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /circuit-breaker/metrics/{breakerName} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | POST | /circuit-breaker/reset/{breakerName} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /circuit-breaker/state/{breakerName} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | POST | /failover/endpoint/healthy | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | POST | /failover/endpoint/unhealthy | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /failover/endpoints | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /failover/group/{groupName} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /failover/healthy/{groupName} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /rate-limit/remaining/{userId} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | POST | /rate-limit/reset/{userId} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /rate-limit/status/{userId} | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceController | GET | /status | src\main\java\org\example\controller\ResilienceController.java |
| ResilienceHealthController |  | /api/v1/resilience | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /circuit-breakers | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /circuit-breakers | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /circuit-breakers/{name} | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /circuit-breakers/{name}/reset | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /failover/clear-cache | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /failover/stats | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /failover-chain | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /failover-chain/{serviceKey} | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /health | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /health/check | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /health/events | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | GET | /report | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /test/failover/cache | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /test/failover/database | src\main\java\org\example\controller\ResilienceHealthController.java |
| ResilienceHealthController | POST | /test/failover/provider | src\main\java\org\example\controller\ResilienceHealthController.java |
| SafeZoneProtectionController |  | /api/safezone | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | POST | /bulk-protect | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | GET | /is-protected/{agentId} | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | POST | /protect/{agentId} | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | GET | /protected | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | GET | /stats | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SafeZoneProtectionController | DELETE | /unprotect/{agentId} | src\main\java\org\example\api\SafeZoneProtectionController.java |
| SelfExtensionController |  | /api/extend | src\main\java\org\example\controller\SelfExtensionController.java |
| SelfExtensionController | POST | /batch | src\main\java\org\example\controller\SelfExtensionController.java |
| SelfExtensionController | POST | /requirement | src\main\java\org\example\controller\SelfExtensionController.java |
| SelfExtensionController | GET | /status | src\main\java\org\example\controller\SelfExtensionController.java |
| SelfHealingController |  | /api/v1/self-healing | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | GET | /circuit-breaker/{serviceName} | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | GET | /health | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | POST | /recover/{serviceName} | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | GET | /service/{serviceName} | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | POST | /start | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | POST | /stop | src\main\java\org\example\controller\SelfHealingController.java |
| SelfHealingController | GET | /system-health | src\main\java\org\example\controller\SelfHealingController.java |
| ServerStatusController | GET | /api/status/health | src\main\java\org\example\controller\ServerStatusController.java |
| ServerStatusController | GET | /api/status/performance | src\main\java\org\example\controller\ServerStatusController.java |
| ServerStatusController | GET | /api/status/summary | src\main\java\org\example\controller\ServerStatusController.java |
| SystemLearningController |  | /api/learning | src\main\java\org\example\controller\SystemLearningController.java |
| SystemLearningController | GET | /critical | src\main\java\org\example\controller\SystemLearningController.java |
| SystemLearningController | GET | /providers/coverage | src\main\java\org\example\controller\SystemLearningController.java |
| SystemLearningController | POST | /reseed | src\main\java\org\example\controller\SystemLearningController.java |
| SystemLearningController | GET | /solutions/{category} | src\main\java\org\example\controller\SystemLearningController.java |
| SystemLearningController | GET | /stats | src\main\java\org\example\controller\SystemLearningController.java |
| SystemMetricsController |  | /api/system | src\main\java\org\example\controller\SystemMetricsController.java |
| SystemMetricsController | GET | /metrics | src\main\java\org\example\controller\SystemMetricsController.java |
| TeachingController |  | /api/teaching | src\main\java\org\example\controller\TeachingController.java |
| TeachingController | POST | /create-app | src\main\java\org\example\controller\TeachingController.java |
| TeachingController | POST | /seed-technique | src\main\java\org\example\controller\TeachingController.java |
| TeachingController | POST | /solve-error | src\main\java\org\example\controller\TeachingController.java |
| TimelineVisualizationController | GET | /agent/{agentName} | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController |  | /api/v1/timeline | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController | GET | /drill-down/{decisionId} | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController | GET | /export/{projectId} | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController | GET | /project/{projectId} | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController | GET | /range | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TimelineVisualizationController | GET | /stats/aggregate | src\main\java\org\example\controller\TimelineVisualizationController.java |
| TracingController |  | /api/tracing | src\main\java\org\example\controller\TracingController.java |
| TracingController | POST | /cleanup | src\main\java\org\example\controller\TracingController.java |
| TracingController | GET | /stats | src\main\java\org\example\controller\TracingController.java |
| TracingController | GET | /trace/{traceId} | src\main\java\org\example\controller\TracingController.java |
| TracingController | GET | /traces/errors | src\main\java\org\example\controller\TracingController.java |
| TracingController | GET | /traces/path | src\main\java\org\example\controller\TracingController.java |
| TracingController | GET | /traces/recent | src\main\java\org\example\controller\TracingController.java |
| UserTierController | GET | /all | src\main\java\org\example\controller\UserTierController.java |
| UserTierController |  | /api/tier | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | GET | /available-tiers | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | POST | /can-create-app | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | POST | /can-request | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | POST | /make-superadmin | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | GET | /my-quota | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | GET | /pricing | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | POST | /reset-monthly | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | POST | /set-user-tier | src\main\java\org\example\controller\UserTierController.java |
| UserTierController | GET | /user/{userId} | src\main\java\org\example\controller\UserTierController.java |
| VisualizationController |  | /api/v1/visualization | src\main\java\org\example\controller\VisualizationController.java |
| VisualizationController | GET | /config | src\main\java\org\example\controller\VisualizationController.java |
| VisualizationController | GET | /frame | src\main\java\org\example\controller\VisualizationController.java |
| VisualizationController | GET | /health | src\main\java\org\example\controller\VisualizationController.java |
| VisualizationController | GET | /stats | src\main\java\org\example\controller\VisualizationController.java |
| VPNController | PUT | /{id} | src\main\java\org\example\controller\VPNController.java |
| VPNController | POST | /{id}/connect | src\main\java\org\example\controller\VPNController.java |
| VPNController | POST | /{id}/disconnect | src\main\java\org\example\controller\VPNController.java |
| VPNController | POST | /add | src\main\java\org\example\controller\VPNController.java |
| VPNController |  | /api/vpn | src\main\java\org\example\controller\VPNController.java |
| VPNController | GET | /list | src\main\java\org\example\controller\VPNController.java |
| WebhookController | GET | /github/stats | src\main\java\org\example\controller\WebhookController.java |
| WebhookController |  | /webhook | src\main\java\org\example\controller\WebhookController.java |
| WorkHistoryController |  | /api/work-history | src\main\java\org\example\controller\WorkHistoryController.java |

---

**Total Endpoints:** 461
**Last Updated:** 2026-04-05 22:50:08
