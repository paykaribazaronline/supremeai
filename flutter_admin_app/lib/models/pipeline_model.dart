// Models for CI/CD Pipeline Management
class Pipeline {
  final String pipelineId;
  final String name;
  final String description;
  final String sourceRepository;
  final String status;
  final DateTime createdAt;
  final DateTime? lastExecutionTime;
  final String? lastExecutionStatus;
  final String? lastExecutionId;
  final List<String> stages;
  final bool enabled;

  Pipeline({
    required this.pipelineId,
    required this.name,
    required this.description,
    required this.sourceRepository,
    required this.status,
    required this.createdAt,
    this.lastExecutionTime,
    this.lastExecutionStatus,
    this.lastExecutionId,
    required this.stages,
    required this.enabled,
  });

  factory Pipeline.fromJson(Map<String, dynamic> json) {
    return Pipeline(
      pipelineId: json['pipelineId'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      sourceRepository: json['sourceRepository'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      lastExecutionTime: json['lastExecutionTime'] != null ? DateTime.parse(json['lastExecutionTime'] as String) : null,
      lastExecutionStatus: json['lastExecutionStatus'] as String?,
      lastExecutionId: json['lastExecutionId'] as String?,
      stages: List<String>.from(json['stages'] as List),
      enabled: json['enabled'] as bool,
    );
  }

  bool get isRunning => status == 'RUNNING';
  bool get isIdle => status == 'IDLE';
  bool get isFailed => status == 'FAILED';
}

class PipelineExecution {
  final String executionId;
  final String pipelineId;
  final String trigger;
  final String branch;
  final String status;
  final String? failureReason;
  final DateTime startedAt;
  final DateTime? completedAt;
  final List<PipelineStageExecution> stages;
  final List<String> logs;

  PipelineExecution({
    required this.executionId,
    required this.pipelineId,
    required this.trigger,
    required this.branch,
    required this.status,
    this.failureReason,
    required this.startedAt,
    this.completedAt,
    required this.stages,
    required this.logs,
  });

  factory PipelineExecution.fromJson(Map<String, dynamic> json) {
    return PipelineExecution(
      executionId: json['executionId'] as String,
      pipelineId: json['pipelineId'] as String,
      trigger: json['trigger'] as String,
      branch: json['branch'] as String,
      status: json['status'] as String,
      failureReason: json['failureReason'] as String?,
      startedAt: DateTime.parse(json['startedAt'] as String),
      completedAt: json['completedAt'] != null ? DateTime.parse(json['completedAt'] as String) : null,
      stages: (json['stages'] as List?)?.map((e) => PipelineStageExecution.fromJson(e as Map<String, dynamic>)).toList() ?? [],
      logs: List<String>.from(json['logs'] as List? ?? []),
    );
  }

  Duration? getDuration() {
    if (completedAt == null) return null;
    return completedAt!.difference(startedAt);
  }

  bool get isRunning => status == 'RUNNING';
  bool get isSuccess => status == 'SUCCESS';
  bool get isFailed => status == 'FAILED';
}

class PipelineStageExecution {
  final String stageId;
  final String stageName;
  final String stageType;
  final String status;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final List<String> output;

  PipelineStageExecution({
    required this.stageId,
    required this.stageName,
    required this.stageType,
    required this.status,
    this.startedAt,
    this.completedAt,
    required this.output,
  });

  factory PipelineStageExecution.fromJson(Map<String, dynamic> json) {
    return PipelineStageExecution(
      stageId: json['stageId'] as String,
      stageName: json['stageName'] as String,
      stageType: json['stageType'] as String,
      status: json['status'] as String,
      startedAt: json['startedAt'] != null ? DateTime.parse(json['startedAt'] as String) : null,
      completedAt: json['completedAt'] != null ? DateTime.parse(json['completedAt'] as String) : null,
      output: List<String>.from(json['output'] as List? ?? []),
    );
  }

  Duration? getDuration() {
    if (completedAt == null || startedAt == null) return null;
    return completedAt!.difference(startedAt!);
  }

  bool get isRunning => status == 'RUNNING';
  bool get isSuccess => status == 'SUCCESS';
  bool get isFailed => status == 'FAILED';
  bool get isPending => status == 'PENDING';
}

class PipelineStats {
  final int totalPipelines;
  final int activePipelines;
  final int totalExecutions;
  final int successfulExecutions;
  final int failedExecutions;
  final double successRate;
  final int averageExecutionTime;
  final DateTime generatedAt;

  PipelineStats({
    required this.totalPipelines,
    required this.activePipelines,
    required this.totalExecutions,
    required this.successfulExecutions,
    required this.failedExecutions,
    required this.successRate,
    required this.averageExecutionTime,
    required this.generatedAt,
  });

  factory PipelineStats.fromJson(Map<String, dynamic> json) {
    return PipelineStats(
      totalPipelines: json['totalPipelines'] as int,
      activePipelines: (json['activePipelines'] as num).toInt(),
      totalExecutions: (json['totalExecutions'] as num).toInt(),
      successfulExecutions: (json['successfulExecutions'] as num).toInt(),
      failedExecutions: (json['failedExecutions'] as num).toInt(),
      successRate: json['successRate'] is String 
        ? double.parse((json['successRate'] as String).replaceAll('%', ''))
        : (json['successRate'] as num).toDouble(),
      averageExecutionTime: json['averageExecutionTime'] as int,
      generatedAt: DateTime.parse(json['generatedAt'] as String),
    );
  }

  String get averageTimeMinutes => '${(averageExecutionTime ~/ 60)} min';
}
