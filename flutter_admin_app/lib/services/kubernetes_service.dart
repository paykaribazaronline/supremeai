// Service for Kubernetes API calls
import 'package:dio/dio.dart';
import '../models/kubernetes_model.dart';

class KubernetesService {
  final Dio dio;
  final String baseUrl;

  KubernetesService({required this.dio, required this.baseUrl});

  Future<K8sDeployment> createDeployment({
    required String name,
    required String namespace,
    required String imageUrl,
    required int replicas,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/kubernetes/deployments',
        queryParameters: {
          'name': name,
          'namespace': namespace,
          'imageUrl': imageUrl,
          'replicas': replicas,
        },
      );
      return K8sDeployment.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<K8sDeployment?> getDeployment(String deploymentId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/deployments/$deploymentId');
      return K8sDeployment.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<K8sDeployment>> listDeployments() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/deployments');
      return (response.data as List)
          .map((e) => K8sDeployment.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> scaleDeployment(String deploymentId, int replicas) async {
    try {
      await dio.put(
        '$baseUrl/api/deployment/kubernetes/deployments/$deploymentId/scale',
        queryParameters: {'replicas': replicas},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateDeploymentImage(String deploymentId, String imageUrl) async {
    try {
      await dio.put(
        '$baseUrl/api/deployment/kubernetes/deployments/$deploymentId/image',
        queryParameters: {'imageUrl': imageUrl},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<K8sPod> createPod({
    required String podName,
    required String namespace,
    required String deploymentId,
    required String containerImage,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/kubernetes/pods',
        queryParameters: {
          'podName': podName,
          'namespace': namespace,
          'deploymentId': deploymentId,
          'containerImage': containerImage,
        },
      );
      return K8sPod.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<K8sPod?> getPod(String podId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/pods/$podId');
      return K8sPod.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<K8sPod>> listPodsByDeployment(String deploymentId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/pods/deployment/$deploymentId');
      return (response.data as List)
          .map((e) => K8sPod.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> updatePodStatus(String podId, String status) async {
    try {
      await dio.put(
        '$baseUrl/api/deployment/kubernetes/pods/$podId/status',
        queryParameters: {'status': status},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<K8sService> createService({
    required String serviceName,
    required String namespace,
    required int port,
    required String selector,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/kubernetes/services',
        queryParameters: {
          'serviceName': serviceName,
          'namespace': namespace,
          'port': port,
          'selector': selector,
        },
      );
      return K8sService.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<K8sService?> getService(String serviceId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/services/$serviceId');
      return K8sService.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<K8sService>> listServices() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/services');
      return (response.data as List)
          .map((e) => K8sService.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<ClusterHealth?> getClusterHealth() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/kubernetes/health');
      return ClusterHealth.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }
}
