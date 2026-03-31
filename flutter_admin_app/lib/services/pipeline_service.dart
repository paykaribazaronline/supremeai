// Service for Pipeline API calls
import 'package:dio/dio.dart';
import '../models/pipeline_model.dart';

class PipelineService {
  final Dio dio;
  final String baseUrl;

  PipelineService({required this.dio, required this.baseUrl});

  Future<Pipeline> createPipeline({
    required String name,
    required String description,
    required String sourceRepository,
    required List<String> stages,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/pipelines',
        data: {
          'name': name,
          'description': description,
          'sourceRepository': sourceRepository,
          'stages': stages,
        },
      );
      return Pipeline.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<Pipeline?> getPipeline(String pipelineId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/pipelines/$pipelineId');
      return Pipeline.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<Pipeline>> listPipelines() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/pipelines');
      return (response.data as List)
          .map((e) => Pipeline.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<Pipeline?> updatePipeline({
    required String pipelineId,
    required String name,
    required String description,
    required bool enabled,
  }) async {
    try {
      final response = await dio.put(
        '$baseUrl/api/deployment/pipelines/$pipelineId',
        data: {
          'name': name,
          'description': description,
          'enabled': enabled,
        },
      );
      return Pipeline.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<PipelineExecution> executePipeline({
    required String pipelineId,
    required String trigger,
    required String branch,
  }) async {
    try {
      final response = await dio.post(
        '$baseUrl/api/deployment/pipelines/$pipelineId/execute',
        data: {
          'trigger': trigger,
          'branch': branch,
        },
      );
      return PipelineExecution.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      rethrow;
    }
  }

  Future<PipelineExecution?> getExecution(String executionId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/executions/$executionId');
      return PipelineExecution.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<PipelineExecution>> listExecutions(String pipelineId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/pipelines/$pipelineId/executions');
      return (response.data as List)
          .map((e) => PipelineExecution.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<PipelineStageExecution?>> getStageExecutions(String executionId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/executions/$executionId/stages');
      return (response.data as List)
          .map((e) => PipelineStageExecution.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<String>> getExecutionLogs(String executionId) async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/executions/$executionId/logs');
      return List<String>.from(response.data as List);
    } catch (e) {
      return [];
    }
  }

  Future<void> retryStage(String executionId, String stageName) async {
    try {
      await dio.post(
        '$baseUrl/api/deployment/executions/$executionId/stages/$stageName/retry',
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<void> cancelExecution(String executionId) async {
    try {
      await dio.post('$baseUrl/api/deployment/executions/$executionId/cancel');
    } catch (e) {
      rethrow;
    }
  }

  Future<PipelineStats?> getPipelineStats() async {
    try {
      final response = await dio.get('$baseUrl/api/deployment/stats/pipelines');
      return PipelineStats.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }
}
