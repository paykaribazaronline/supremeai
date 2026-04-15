// User model for authentication
class User {
  final String id;
  final String email;
  final String name;
  final String role;
  final DateTime createdAt;
  final DateTime updatedAt;

  User({
    required this.id,
    required this.email,
    required this.name,
    required this.role,
    required this.createdAt,
    required this.updatedAt,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? '',
      email: json['email'] ?? '',
      name: json['name'] ?? '',
      role: json['role'] ?? 'admin',
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
      updatedAt: json['updatedAt'] != null 
          ? DateTime.parse(json['updatedAt']) 
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      'role': role,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }
}

// Project model
class Project {
  final String id;
  final String name;
  final String description;
  final String status;
  final String templateType;
  final String repoUrl;
  final String repoBranch;
  final String repoToken;
  final int progress;
  final int fileCount;
  final bool pushed;
  final bool trackedForImprovement;
  final List<String> features;
  final String? aiAgentId;
  final Map<String, dynamic>? metadata;
  final DateTime createdAt;
  final DateTime? updatedAt;
  final DateTime? completedAt;

  Project({
    required this.id,
    required this.name,
    required this.description,
    required this.status,
    required this.templateType,
    required this.repoUrl,
    required this.repoBranch,
    this.repoToken = '',
    required this.progress,
    required this.fileCount,
    required this.pushed,
    this.trackedForImprovement = false,
    this.features = const [],
    this.aiAgentId,
    this.metadata,
    required this.createdAt,
    this.updatedAt,
    this.completedAt,
  });

  factory Project.fromJson(Map<String, dynamic> json) {
    final createdAtRaw = json['createdAt'];
    final updatedAtRaw = json['updatedAt'];
    final completedAtRaw = json['completedAt'];
    final projectId = (json['projectId'] ?? json['id'] ?? '').toString();
    final repoUrl = (json['repoUrl'] ?? '').toString();
    final featuresRaw = json['features'];
    return Project(
      id: projectId,
      name: (json['name'] ?? projectId).toString(),
      description: json['description'] ?? '',
      status: (json['status'] ?? 'GENERATING').toString(),
      templateType: (json['templateType'] ?? 'REACT').toString(),
      repoUrl: repoUrl,
      repoBranch: (json['repoBranch'] ?? 'main').toString(),
      repoToken: '',
      progress: json['progress'] is int ? json['progress'] as int : int.tryParse('${json['progress'] ?? 0}') ?? 0,
      fileCount: json['fileCount'] is int ? json['fileCount'] as int : int.tryParse('${json['fileCount'] ?? 0}') ?? 0,
      pushed: json['pushed'] == true,
      trackedForImprovement: json['trackedForImprovement'] == true,
      features: featuresRaw is List
          ? featuresRaw.map((item) => item.toString()).where((item) => item.trim().isNotEmpty).toList()
          : const [],
      aiAgentId: json['aiAgentId'],
      metadata: json['metadata'] is Map<String, dynamic>
          ? json['metadata'] as Map<String, dynamic>
          : null,
      createdAt: createdAtRaw != null
          ? DateTime.tryParse(createdAtRaw.toString()) ?? DateTime.now()
          : DateTime.now(),
      updatedAt: updatedAtRaw != null
          ? DateTime.tryParse(updatedAtRaw.toString())
          : null,
      completedAt: completedAtRaw != null
          ? DateTime.tryParse(completedAtRaw.toString())
          : null,
    );
  }

  Project copyWith({
    bool? trackedForImprovement,
  }) {
    return Project(
      id: id,
      name: name,
      description: description,
      status: status,
      templateType: templateType,
      repoUrl: repoUrl,
      repoBranch: repoBranch,
      repoToken: repoToken,
      progress: progress,
      fileCount: fileCount,
      pushed: pushed,
      trackedForImprovement: trackedForImprovement ?? this.trackedForImprovement,
      features: features,
      aiAgentId: aiAgentId,
      metadata: metadata,
      createdAt: createdAt,
      updatedAt: updatedAt,
      completedAt: completedAt,
    );
  }

  Map<String, dynamic> toCreateJson() {
    return {
      'projectId': id,
      'templateType': templateType,
      'description': description,
      'features': features,
      'repoUrl': repoUrl,
      'repoBranch': repoBranch,
      'repoToken': repoToken,
    };
  }
}

// API Provider model
class APIProvider {
  final String id;
  final String name;
  final String type;
  final String? description;
  final String status;
  final Map<String, dynamic>? config;
  final DateTime createdAt;

  APIProvider({
    required this.id,
    required this.name,
    required this.type,
    this.description,
    required this.status,
    this.config,
    required this.createdAt,
  });

  factory APIProvider.fromJson(Map<String, dynamic> json) {
    return APIProvider(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      type: json['type'] ?? '',
      description: json['description'],
      status: json['status'] ?? 'active',
      config: json['config'],
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'type': type,
      'description': description,
      'status': status,
      'config': config,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}

// AI Agent model
class AIAgent {
  final String id;
  final String name;
  final String role;
  final String description;
  final String status;
  final Map<String, dynamic>? config;

  AIAgent({
    required this.id,
    required this.name,
    required this.role,
    required this.description,
    required this.status,
    this.config,
  });

  factory AIAgent.fromJson(Map<String, dynamic> json) {
    return AIAgent(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      role: json['role'] ?? '',
      description: json['description'] ?? '',
      status: json['status'] ?? 'active',
      config: json['config'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'role': role,
      'description': description,
      'status': status,
      'config': config,
    };
  }
}

// Health metrics model
class HealthMetrics {
  final double memoryUsageMB;
  final double cpuUsagePercent;
  final int totalRequests;
  final int errorCount;
  final double averageLatencyMs;
  final String status;

  HealthMetrics({
    required this.memoryUsageMB,
    required this.cpuUsagePercent,
    required this.totalRequests,
    required this.errorCount,
    required this.averageLatencyMs,
    required this.status,
  });

  factory HealthMetrics.fromJson(Map<String, dynamic> json) {
    return HealthMetrics(
      memoryUsageMB: (json['memoryUsageMB'] ?? 0).toDouble(),
      cpuUsagePercent: (json['cpuUsagePercent'] ?? 0).toDouble(),
      totalRequests: json['totalRequests'] ?? 0,
      errorCount: json['errorCount'] ?? 0,
      averageLatencyMs: (json['averageLatencyMs'] ?? 0).toDouble(),
      status: json['status'] ?? 'unknown',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'memoryUsageMB': memoryUsageMB,
      'cpuUsagePercent': cpuUsagePercent,
      'totalRequests': totalRequests,
      'errorCount': errorCount,
      'averageLatencyMs': averageLatencyMs,
      'status': status,
    };
  }
}

// Alert model
class Alert {
  final String id;
  final String title;
  final String message;
  final String severity;
  final DateTime createdAt;
  final bool isResolved;

  Alert({
    required this.id,
    required this.title,
    required this.message,
    required this.severity,
    required this.createdAt,
    required this.isResolved,
  });

  factory Alert.fromJson(Map<String, dynamic> json) {
    return Alert(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      message: json['message'] ?? '',
      severity: json['severity'] ?? 'info',
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
      isResolved: json['isResolved'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'message': message,
      'severity': severity,
      'createdAt': createdAt.toIso8601String(),
      'isResolved': isResolved,
    };
  }
}

// External AI Knowledge Seed Model
class ExternalKnowledgeSeed {
  final String id;
  final String sourceAiModelName;
  final String prompt;
  final String aiResponse;
  final String category;
  final String seedStatus;
  final double confidence;
  final String? adminNotes;
  final String seededByUserId;
  final Map<String, dynamic>? metadata;
  final DateTime createdAt;
  final DateTime? processedAt;

  ExternalKnowledgeSeed({
    required this.id,
    required this.sourceAiModelName,
    required this.prompt,
    required this.aiResponse,
    required this.category,
    this.seedStatus = 'pending',
    this.confidence = 0.0,
    this.adminNotes,
    required this.seededByUserId,
    this.metadata,
    required this.createdAt,
    this.processedAt,
  });

  factory ExternalKnowledgeSeed.fromJson(Map<String, dynamic> json) {
    return ExternalKnowledgeSeed(
      id: json['id'] ?? '',
      sourceAiModelName: json['sourceAiModelName'] ?? '',
      prompt: json['prompt'] ?? '',
      aiResponse: json['aiResponse'] ?? '',
      category: json['category'] ?? 'uncategorized',
      seedStatus: json['seedStatus'] ?? 'pending',
      confidence: (json['confidence'] ?? 0.0).toDouble(),
      adminNotes: json['adminNotes'],
      seededByUserId: json['seededByUserId'] ?? '',
      metadata: json['metadata'] is Map<String, dynamic>
          ? json['metadata'] as Map<String, dynamic>
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'])
          : DateTime.now(),
      processedAt: json['processedAt'] != null
          ? DateTime.parse(json['processedAt'])
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'sourceAiModelName': sourceAiModelName,
      'prompt': prompt,
      'aiResponse': aiResponse,
      'category': category,
      'seedStatus': seedStatus,
      'confidence': confidence,
      'adminNotes': adminNotes,
      'seededByUserId': seededByUserId,
      'metadata': metadata,
      'createdAt': createdAt.toIso8601String(),
      'processedAt': processedAt?.toIso8601String(),
    };
  }
}

// Login response model
class LoginResponse {
  final String token;
  final String refreshToken;
  final User user;

  LoginResponse({
    required this.token,
    required this.refreshToken,
    required this.user,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      token: json['token'] ?? '',
      refreshToken: json['refreshToken'] ?? '',
      user: User.fromJson(json['user'] ?? {}),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'refreshToken': refreshToken,
      'user': user.toJson(),
    };
  }
}
