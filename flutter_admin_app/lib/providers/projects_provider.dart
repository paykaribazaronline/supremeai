import 'package:flutter/material.dart';
import '../config/environment.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class ProjectsProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  List<Project> _projects = [];
  List<Project> _runningProjects = [];
  List<Project> _finishedProjects = [];
  bool _isLoading = false;
  String? _error;
  bool _cloudStorageActive = false;
  final Set<String> _trackedRepoUrls = <String>{};

  List<Project> get projects => _projects;
  List<Project> get runningProjects => _runningProjects;
  List<Project> get finishedProjects => _finishedProjects;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get cloudStorageActive => _cloudStorageActive;
  int get runningCount => _runningProjects.length;
  int get finishedCount => _finishedProjects.length;
  int get trackedProjectsCount => _trackedRepoUrls.length;

  Future<void> fetchProjects() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final responses = await Future.wait([
        _apiService.get<Map<String, dynamic>>(Environment.projectsList),
        _apiService.get<Map<String, dynamic>>(Environment.projectRunning),
        _apiService.get<Map<String, dynamic>>(Environment.projectFinished),
        _apiService.get<Map<String, dynamic>>(Environment.projectStorageStatus),
        _apiService.get<Map<String, dynamic>>(Environment.existingProjects),
      ]);

      final allProjectsResponse = responses[0];
      final runningProjectsResponse = responses[1];
      final finishedProjectsResponse = responses[2];
      final storageStatusResponse = responses[3];
      final existingProjectsResponse = responses[4];

      if (allProjectsResponse.success && allProjectsResponse.data != null) {
        _trackedRepoUrls
          ..clear()
          ..addAll(_extractTrackedRepoUrls(existingProjectsResponse.data));
        _projects = _parseProjectsEnvelope(allProjectsResponse.data);
        _runningProjects = _parseProjectsEnvelope(runningProjectsResponse.data);
        _finishedProjects = _parseProjectsEnvelope(finishedProjectsResponse.data);
        _cloudStorageActive = storageStatusResponse.data?['cloudStorageActive'] == true;
        _projects = _markTrackedProjects(_projects);
        _runningProjects = _markTrackedProjects(_runningProjects);
        _finishedProjects = _markTrackedProjects(_finishedProjects);
        _error = null;
      } else {
        _error = allProjectsResponse.error ?? 'Failed to fetch projects';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> createProject(Project project) async {
    try {
      final response = await _apiService.post<Map<String, dynamic>>(
        Environment.projectGenerate,
        data: project.toCreateJson(),
      );

      if (response.success && response.data != null && response.data!['status'] == 'success') {
        await fetchProjects();
      } else {
        throw Exception(response.error ?? 'Failed to create project');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateProject(Project project) async {
    throw Exception('Editing generated projects is not supported here. Use the Existing App workflow for improvements.');
  }

  Future<void> deleteProject(String projectId) async {
    try {
      final response = await _apiService.delete<Map<String, dynamic>>('${Environment.projectsList}/$projectId');

      if (response.success) {
        _projects.removeWhere((p) => p.id == projectId);
        _runningProjects.removeWhere((p) => p.id == projectId);
        _finishedProjects.removeWhere((p) => p.id == projectId);
        notifyListeners();
      } else {
        throw Exception(response.error ?? 'Failed to delete project');
      }
    } catch (e) {
      rethrow;
    }
  }

  List<Project> _parseProjectsEnvelope(Map<String, dynamic>? envelope) {
    if (envelope == null) {
      return const [];
    }
    final rawProjects = envelope['projects'];
    if (rawProjects is! List) {
      return const [];
    }
    return rawProjects
        .whereType<Map>()
        .map((project) => Project.fromJson(Map<String, dynamic>.from(project)))
        .toList();
  }

  Set<String> _extractTrackedRepoUrls(Map<String, dynamic>? envelope) {
    if (envelope == null) {
      return <String>{};
    }
    final rawProjects = envelope['projects'];
    if (rawProjects is! List) {
      return <String>{};
    }
    return rawProjects
        .whereType<Map>()
        .map((project) => project['repoUrl']?.toString() ?? '')
        .where((repoUrl) => repoUrl.isNotEmpty)
        .toSet();
  }

  List<Project> _markTrackedProjects(List<Project> projects) {
    return projects
        .map((project) => project.copyWith(
              trackedForImprovement: project.repoUrl.isNotEmpty && _trackedRepoUrls.contains(project.repoUrl),
            ))
        .toList();
  }
}
