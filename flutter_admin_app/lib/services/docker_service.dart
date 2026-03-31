// Service for Docker API calls
import 'package:dio/dio.dart';
import '../models/docker_model.dart';

class DockerService {
  final Dio dio;
  final String baseUrl;

  DockerService({required this.dio, required this.baseUrl});

  Future<DockerImage> buildImage({
    required String imageName,
    required String tag,
    required String dockerfilePath,
    required String buildContext,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/docker/images/build',
        queryParameters: {
          'imageName': imageName,
          'tag': tag,
          'dockerfilePath': dockerfilePath,
          'buildContext': buildContext,
        },
      );
      return DockerImage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<DockerImage?> getImage(String imageId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/images/$imageId');
      return DockerImage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<DockerImage>> listImages() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/images');
      return (response.data as List)
          .map((e) => DockerImage.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<DockerImage>> listImagesByName(String imageName) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/images/name/$imageName');
      return (response.data as List)
          .map((e) => DockerImage.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<DockerImage?> tagImage(String imageId, String newTag) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/docker/images/$imageId/tag',
        queryParameters: {'newTag': newTag},
      );
      return DockerImage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<void> pushImage({
    required String imageId,
    required String registry,
    required String username,
    required String password,
  }) async {
    try {
      await dio.post(
        '$baseUrl/api/deployment/docker/images/$imageId/push',
        queryParameters: {
          'registry': registry,
          'username': username,
          'password': password,
        },
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<bool> validateImage(String imageId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/images/$imageId/validate');
      return response.data['valid'] as bool;
    } catch (e) {
      return false;
    }
  }

  Future<BuildJob?> getBuildJob(String buildJobId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/builds/$buildJobId');
      return BuildJob.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<String>> getBuildLogs(String buildJobId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/docker/builds/$buildJobId/logs');
      return List<String>.from(response.data as List);
    } catch (e) {
      return [];
    }
  }

  Future<void> cleanupOldImages(String imageName, {int keepCount = 3}) async {
    try {
      await dio.post(
        '$baseUrl/api/deployment/docker/images/$imageName/cleanup',
        queryParameters: {'keepCount': keepCount},
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<ImageStats?> getImageStats() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/stats/images');
      return ImageStats.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }
}
