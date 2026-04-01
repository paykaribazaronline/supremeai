import 'package:flutter/material.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class ProjectsProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  List<Project> _projects = [];
  bool _isLoading = false;
  String? _error;

  List<Project> get projects => _projects;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> fetchProjects() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.get<List<dynamic>>('/api/projects');
      
      if (response.success && response.data != null) {
        final data = response.data as List;
        _projects = data.map((p) => Project.fromJson(p as Map<String, dynamic>)).toList();
        _error = null;
      } else {
        _error = response.error ?? 'Failed to fetch projects';
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
        '/api/projects',
        data: project.toJson(),
      );

      if (response.success) {
        _projects.add(project);
        notifyListeners();
      } else {
        throw Exception(response.error ?? 'Failed to create project');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<void> updateProject(Project project) async {
    try {
      final response = await _apiService.put<Map<String, dynamic>>(
        '/api/projects/${project.id}',
        data: project.toJson(),
      );

      if (response.success) {
        final index = _projects.indexWhere((p) => p.id == project.id);
        if (index >= 0) {
          _projects[index] = project;
          notifyListeners();
        }
      } else {
        throw Exception(response.error ?? 'Failed to update project');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<void> deleteProject(String projectId) async {
    try {
      final response = await _apiService.delete<Map<String, dynamic>>('/api/projects/$projectId');

      if (response.success) {
        _projects.removeWhere((p) => p.id == projectId);
        notifyListeners();
      } else {
        throw Exception(response.error ?? 'Failed to delete project');
      }
    } catch (e) {
      rethrow;
    }
  }
}
