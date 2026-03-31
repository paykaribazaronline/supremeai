// Service for Deployment API calls
import 'package:dio/dio.dart';
import '../models/deployment_model.dart';

class DeploymentService {
  final Dio dio;
  final String baseUrl;

  DeploymentService({required this.dio, required this.baseUrl});

  Future<DeploymentRecord> createDeployment({
    required String appName,
    required String version,
    required String environment,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/deployments',
        queryParameters: {
          'appName': appName,
          'version': version,
          'environment': environment,
        },
      );
      return DeploymentRecord.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<DeploymentRecord?> getDeployment(String deploymentId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/deployments/$deploymentId');
      return DeploymentRecord.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<DeploymentRecord>> listDeployments() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/deployments');
      return (response.data as List)
          .map((e) => DeploymentRecord.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<DeploymentRecord>> listDeploymentsByApplication(String appName) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/deployments/application/$appName');
      return (response.data as List)
          .map((e) => DeploymentRecord.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<DeploymentRecord>> listDeploymentsByEnvironment(String environment) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/deployments/environment/$environment');
      return (response.data as List)
          .map((e) => DeploymentRecord.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> startDeployment(String deploymentId) async {
    try {
      await dio.put('$baseUrl/api/deployment/deployments/$deploymentId/start');
    } catch (e) {
      rethrow;
    }
  }

  Future<void> completeDeployment(String deploymentId) async {
    try {
      await dio.put('$baseUrl/api/deployment/deployments/$deploymentId/complete');
    } catch (e) {
      rethrow;
    }
  }

  Future<void> failDeployment(String deploymentId, String reason) async {
    try {
      await dio.put(
        '$baseUrl/api/deployment/deployments/$deploymentId/fail',
        queryParameters: {'reason': reason},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> rollbackDeployment(String deploymentId, String previousVersion) async {
    try {
      await dio.put(
        '$baseUrl/api/deployment/deployments/$deploymentId/rollback',
        queryParameters: {'previousVersion': previousVersion},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<ApplicationVersion> registerVersion({
    required String appName,
    required String version,
    required String artifactUrl,
    required String releaseNotes,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/versions',
        queryParameters: {
          'appName': appName,
          'version': version,
          'artifactUrl': artifactUrl,
          'releaseNotes': releaseNotes,
        },
      );
      return ApplicationVersion.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<ApplicationVersion?> getLatestVersion(String appName) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/versions/$appName/latest');
      return ApplicationVersion.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<ApplicationVersion>> listVersions(String appName) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/versions/$appName');
      return (response.data as List)
          .map((e) => ApplicationVersion.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<DeploymentEvent>> getDeploymentEvents(String deploymentId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/deployments/$deploymentId/events');
      return (response.data as List)
          .map((e) => DeploymentEvent.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<Map<String, dynamic>> getDeploymentStats() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/stats/deployments');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      return {};
    }
  }
}
