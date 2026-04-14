import 'package:flutter/foundation.dart';

class Environment {
  // Single setup for Flutter web/app and backend:
  // - Web uses the same host it is served from.
  // - Mobile/desktop can override with --dart-define=SUPREMEAI_API_BASE_URL=...
  // - Otherwise fall back to the deployed cloud backend.
  static const String _apiBaseUrlOverride = String.fromEnvironment(
    'SUPREMEAI_API_BASE_URL',
    defaultValue: '',
  );
  static const String cloudBaseUrl =
      'https://supremeai-565236080752.us-central1.run.app';

  static String get baseUrl {
    if (_apiBaseUrlOverride.trim().isNotEmpty) {
      return _apiBaseUrlOverride.trim();
    }

    if (kIsWeb) {
      return Uri.base.origin;
    }

    return cloudBaseUrl;
  }

  // Alias for baseUrl (used by admin screens)
  static String get apiBaseUrl => baseUrl;

  static const String projectsList = '/api/projects';
  static const String projectGenerate = '/api/projects/generate';
  static const String projectRunning = '/api/projects/running';
  static const String projectFinished = '/api/projects/finished';
  static const String projectStorageStatus = '/api/projects/storage-status';
  static const String existingProjects = '/api/existing-projects';
  
  static const String providersAvailable = '/api/providers/available';
  static const String providersConfigured = '/api/providers/configured';
  static const String providersAdd = '/api/providers/add';
  static const String providersRemove = '/api/providers/remove';
  static const String providersTest = '/api/providers/test';
  
  static const String agentsAssign = '/api/agents/assign';
  static const String agentsList = '/api/agents';
  
  static const String metricsHealth = '/api/metrics/health';
  static const String metricsPerformance = '/api/metrics/performance';
  static const String alertsList = '/api/alerts';
  static const String alertsStats = '/api/alerts/stats';
  
  // Learning & Research
  static const String learningStats = '/api/learning/stats';
  static const String learningCritical = '/api/learning/critical';
  static const String learningSolutions = '/api/learning/solutions';
  static const String learningResearchStats = '/api/learning/research-stats';
  static const String learningResearchNow = '/api/learning/research-now';
  
  // Teaching
  static const String teachingCreateApp = '/api/teaching/create-app';
  static const String teachingSolveError = '/api/teaching/solve-error';
  static const String teachingSeedTechnique = '/api/teaching/seed-technique';
  
  // Self-Extension
  static const String extendRequirement = '/api/extend/requirement';
  static const String extendStatus = '/api/extend/status';
  static const String extendBatch = '/api/extend/batch';
  
  // Execution Logs
  static const String executionLogs = '/api/execution-logs/system';
  
  // Admin Control
  static const String adminControl = '/api/admin/control';
  static const String adminControlMode = '/api/admin/control/mode';
  static const String adminControlStop = '/api/admin/control/stop';
  static const String adminControlResume = '/api/admin/control/resume';
  static const String adminControlPending = '/api/admin/control/pending';
  static const String adminControlHistory = '/api/admin/control/history';
  
  // Multi-AI Consensus
  static const String consensusAsk = '/api/consensus/ask';
  static const String consensusHistory = '/api/consensus/history';
  static const String consensusStats = '/api/consensus/stats';
  
  // Git Operations
  static const String gitCommit = '/api/git/commit';
  static const String gitPush = '/api/git/push';
  static const String gitStatus = '/api/git/status';
  static const String gitLogs = '/api/git/logs';
  
  // Quota Management
  static const String quotaSummary = '/api/quota/summary';
  static const String quotaAll = '/api/quota/all';
  static const String quotaStatus = '/api/quota/status';
  static const String quotaReset = '/api/quota/reset';
  static const String quotasHealth = '/api/quotas/health';
  static const String quotasProviders = '/api/quotas/providers';
  
  // VPN Management
  static const String vpnList = '/api/vpn/list';
  static const String vpnAdd = '/api/vpn/add';
  
  // Resilience
  static const String resilienceHealth = '/api/v1/resilience/health';
  static const String resilienceCircuitBreakers = '/api/v1/resilience/circuit-breakers';
  static const String resilienceFailoverStats = '/api/v1/resilience/failover/stats';
  static const String resilienceReport = '/api/v1/resilience/report';
  static const String resilienceStatus = '/api/resilience/status';
  static const String selfHealingHealth = '/api/v1/self-healing/system-health';
  
  // ML Intelligence
  static const String mlAnomalySummary = '/api/intelligence/ml/anomaly-summary';
  static const String mlDetectAnomalies = '/api/intelligence/ml/detect-anomalies';
  static const String mlPredictFailure = '/api/intelligence/ml/predict-failure';
  static const String mlRecommendProvider = '/api/intelligence/ml/recommend-provider';
  
  // Notifications
  static const String notificationChannels = '/api/notifications/channels';
  static const String notificationHistory = '/api/notifications/history';
  static const String notificationEmail = '/api/notifications/email';
  static const String notificationSlack = '/api/notifications/slack';
  static const String notificationDiscord = '/api/notifications/discord';
  static const String notificationSms = '/api/notifications/sms';
  static const String notificationEscalate = '/api/notifications/escalate';
  
  // Analytics
  static const String analyticsHistorical = '/api/analytics/historical';
  static const String analyticsTrend = '/api/analytics/trend';
  static const String analyticsDaily = '/api/analytics/daily';
  static const String analyticsMonthly = '/api/analytics/monthly';
  static const String analyticsExportCsv = '/api/analytics/export/csv';
  
  // Decision History / Timeline
  static const String timelineRange = '/api/v1/timeline/range';
  static const String timelineStats = '/api/v1/timeline/stats/aggregate';
  
  // Phase 6/7
  static const String phase6Health = '/api/v1/phase6/health';
  static const String phase6Features = '/api/v1/phase6/features';
  static const String phase6Metrics = '/api/v1/phase6/metrics';
  static const String phase7Summary = '/api/phase7/agents/summary';
  static const String phase7Capabilities = '/api/phase7/agents/capabilities';

  // Phase 1 (Optimization)
  static const String phase1Health = '/api/v1/optimization/health';
  static const String phase1Metrics = '/api/v1/optimization/metrics';

  // Phase 8 (Security & Compliance)
  static const String phase8Summary = '/api/v1/agents/phase8/summary';

  // Phase 9 (Cost Intelligence)
  static const String phase9Summary = '/api/v1/agents/phase9/summary';

  // Phase 10 (Self-Improvement)
  static const String phase10Summary = '/api/v1/agents/phase10/summary';

  // All Phases Overview
  static const String allPhases = '/api/v1/agents/all-phases';
  
  // Tracing
  static const String tracingRecent = '/api/tracing/traces/recent';
  static const String tracingErrors = '/api/tracing/traces/errors';
  static const String tracingStats = '/api/tracing/stats';
  
  // Timeouts (in seconds)
  static const int connectionTimeout = 30;
  static const int receiveTimeout = 30;
}
