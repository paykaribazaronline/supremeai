// Environment configuration for SupremeAI Admin App
//
// PRODUCTION DEPLOYMENT INSTRUCTIONS:
// 1. Deploy backend to Google Cloud Run (see PRODUCTION_DEPLOYMENT_GUIDE.md)
// 2. Get the service URL from: gcloud run services describe supremeai --format='value(status.url)'
// 3. Replace 'YOUR_GCP_PROJECT_ID' below with your actual GCP project ID
// 4. Rebuild Flutter web: flutter build web --base-href "/admin/" --release
// 5. The URL format will be: https://supremeai-YOUR_GCP_PROJECT_ID.us-central1.run.app

class Environment {
  // 🚀 PRODUCTION: Cloud Run Backend URL
  // GCP Project: supremeai-a
  // Deployed at: https://supremeai-565236080752.us-central1.run.app/
  static const String baseUrl = 'https://supremeai-565236080752.us-central1.run.app';
  
  // 🔧 LOCAL DEVELOPMENT: Uncomment to test against local backend
  // Requires: ./gradlew bootRun (running on http://localhost:8080)
  // static const String baseUrl = 'http://localhost:8080';
  
  // API Endpoints
  static const String authLogin = '/api/auth/login';
  static const String authRegister = '/api/auth/register';
  static const String authLogout = '/api/auth/logout';
  static const String authRefresh = '/api/auth/refresh';
  
  static const String projectsList = '/api/projects';
  static const String projectCreate = '/api/projects/create';
  static const String projectUpdate = '/api/projects/update';
  static const String projectDelete = '/api/projects/delete';
  
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
  
  // Tracing
  static const String tracingRecent = '/api/tracing/traces/recent';
  static const String tracingErrors = '/api/tracing/traces/errors';
  static const String tracingStats = '/api/tracing/stats';
  
  // Timeouts (in seconds)
  static const int connectionTimeout = 30;
  static const int receiveTimeout = 30;
  
  // Token storage key
  static const String tokenStorageKey = 'supremeai_token';
  static const String refreshTokenStorageKey = 'supremeai_refresh_token';
  static const String userStorageKey = 'supremeai_user';
}
