// Environment configuration for SupremeAI Admin App

class Environment {
  // Production Cloud Run URL - connects to the backend service
  static const String baseUrl = 'https://supremeai-565236080752.us-central1.run.app';
  
  // Local Development URL (uncomment to use local backend)
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
