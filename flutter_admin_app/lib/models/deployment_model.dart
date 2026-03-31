// Models for Deployment Management
import 'package:flutter/foundation.dart';

class DeploymentRecord {
  final String deploymentId;
  final String applicationName;
  final String version;
  final String environment;
  final String status;
  final DateTime createdAt;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final String? failureReason;
  final String? rolledBackTo;

  DeploymentRecord({
    required this.deploymentId,
    required this.applicationName,
    required this.version,
    required this.environment,
    required this.status,
    required this.createdAt,
    this.startedAt,
    this.completedAt,
    this.failureReason,
    this.rolledBackTo,
  });

  factory DeploymentRecord.fromJson(Map<String, dynamic> json) {
    return DeploymentRecord(
      deploymentId: json['deploymentId'] as String,
      applicationName: json['applicationName'] as String,
      version: json['version'] as String,
      environment: json['environment'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      startedAt: json['startedAt'] != null ? DateTime.parse(json['startedAt'] as String) : null,
      completedAt: json['completedAt'] != null ? DateTime.parse(json['completedAt'] as String) : null,
      failureReason: json['failureReason'] as String?,
      rolledBackTo: json['rolledBackTo'] as String?,
    );
  }

  Duration? getDuration() {
    if (completedAt == null || startedAt == null) return null;
    return completedAt!.difference(startedAt!);
  }

  bool get isRunning => status == 'IN_PROGRESS';
  bool get isSuccess => status == 'SUCCESS';
  bool get isFailed => status == 'FAILED';
}

class ApplicationVersion {
  final String versionId;
  final String applicationName;
  final String version;
  final String artifactUrl;
  final String releaseNotes;
  final DateTime releasedAt;
  final int downloadCount;

  ApplicationVersion({
    required this.versionId,
    required this.applicationName,
    required this.version,
    required this.artifactUrl,
    required this.releaseNotes,
    required this.releasedAt,
    required this.downloadCount,
  });

  factory ApplicationVersion.fromJson(Map<String, dynamic> json) {
    return ApplicationVersion(
      versionId: json['versionId'] as String,
      applicationName: json['applicationName'] as String,
      version: json['version'] as String,
      artifactUrl: json['artifactUrl'] as String,
      releaseNotes: json['releaseNotes'] as String,
      releasedAt: DateTime.parse(json['releasedAt'] as String),
      downloadCount: json['downloadCount'] as int,
    );
  }
}

class DeploymentEvent {
  final String eventId;
  final String deploymentId;
  final String eventType;
  final String message;
  final DateTime timestamp;

  DeploymentEvent({
    required this.eventId,
    required this.deploymentId,
    required this.eventType,
    required this.message,
    required this.timestamp,
  });

  factory DeploymentEvent.fromJson(Map<String, dynamic> json) {
    return DeploymentEvent(
      eventId: json['eventId'] as String,
      deploymentId: json['deploymentId'] as String,
      eventType: json['eventType'] as String,
      message: json['message'] as String,
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }
}
