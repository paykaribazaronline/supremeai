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
  // Deployed at: https://supremeai-lhlwyikwlq-uc.a.run.app/
  static const String baseUrl = 'https://supremeai-lhlwyikwlq-uc.a.run.app';
  
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
  
  // Timeouts (in seconds)
  static const int connectionTimeout = 30;
  static const int receiveTimeout = 30;
  
  // Token storage key
  static const String tokenStorageKey = 'supremeai_token';
  static const String refreshTokenStorageKey = 'supremeai_refresh_token';
  static const String userStorageKey = 'supremeai_user';
}
