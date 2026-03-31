// Models for Kubernetes Management
class K8sDeployment {
  final String deploymentId;
  final String name;
  final String namespace;
  final String imageUrl;
  final int desiredReplicas;
  final int readyReplicas;
  final DateTime createdAt;
  final DateTime? lastUpdatedAt;

  K8sDeployment({
    required this.deploymentId,
    required this.name,
    required this.namespace,
    required this.imageUrl,
    required this.desiredReplicas,
    required this.readyReplicas,
    required this.createdAt,
    this.lastUpdatedAt,
  });

  factory K8sDeployment.fromJson(Map<String, dynamic> json) {
    return K8sDeployment(
      deploymentId: json['deploymentId'] as String,
      name: json['name'] as String,
      namespace: json['namespace'] as String,
      imageUrl: json['imageUrl'] as String,
      desiredReplicas: json['desiredReplicas'] as int,
      readyReplicas: json['readyReplicas'] as int,
      createdAt: DateTime.parse(json['createdAt'] as String),
      lastUpdatedAt: json['lastUpdatedAt'] != null ? DateTime.parse(json['lastUpdatedAt'] as String) : null,
    );
  }

  double get readinessPercent => desiredReplicas > 0 ? (readyReplicas / desiredReplicas) * 100 : 0;
  bool get isHealthy => readyReplicas == desiredReplicas && readyReplicas > 0;
}

class K8sPod {
  final String podId;
  final String podName;
  final String namespace;
  final String deploymentId;
  final String containerImage;
  final String status;
  final DateTime createdAt;
  final DateTime? startedAt;
  final int restartCount;
  final List<String> logs;

  K8sPod({
    required this.podId,
    required this.podName,
    required this.namespace,
    required this.deploymentId,
    required this.containerImage,
    required this.status,
    required this.createdAt,
    this.startedAt,
    required this.restartCount,
    required this.logs,
  });

  factory K8sPod.fromJson(Map<String, dynamic> json) {
    return K8sPod(
      podId: json['podId'] as String,
      podName: json['podName'] as String,
      namespace: json['namespace'] as String,
      deploymentId: json['deploymentId'] as String,
      containerImage: json['containerImage'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      startedAt: json['startedAt'] != null ? DateTime.parse(json['startedAt'] as String) : null,
      restartCount: json['restartCount'] as int,
      logs: List<String>.from(json['logs'] as List),
    );
  }

  bool get isRunning => status == 'Running';
  bool get isPending => status == 'Pending';
  bool get isFailed => status == 'Failed';
}

class K8sService {
  final String serviceId;
  final String serviceName;
  final String namespace;
  final int port;
  final String selector;
  final DateTime createdAt;
  final List<String> endpoints;

  K8sService({
    required this.serviceId,
    required this.serviceName,
    required this.namespace,
    required this.port,
    required this.selector,
    required this.createdAt,
    required this.endpoints,
  });

  factory K8sService.fromJson(Map<String, dynamic> json) {
    return K8sService(
      serviceId: json['serviceId'] as String,
      serviceName: json['serviceName'] as String,
      namespace: json['namespace'] as String,
      port: json['port'] as int,
      selector: json['selector'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      endpoints: List<String>.from(json['endpoints'] as List),
    );
  }
}

class ClusterHealth {
  final int totalPods;
  final int runningPods;
  final int pendingPods;
  final int failedPods;
  final double healthPercent;
  final int totalDeployments;
  final int totalServices;

  ClusterHealth({
    required this.totalPods,
    required this.runningPods,
    required this.pendingPods,
    required this.failedPods,
    required this.healthPercent,
    required this.totalDeployments,
    required this.totalServices,
  });

  factory ClusterHealth.fromJson(Map<String, dynamic> json) {
    return ClusterHealth(
      totalPods: json['totalPods'] as int,
      runningPods: (json['runningPods'] as num).toInt(),
      pendingPods: (json['pendingPods'] as num).toInt(),
      failedPods: (json['failedPods'] as num).toInt(),
      healthPercent: json['healthPercent'] is String 
        ? double.parse((json['healthPercent'] as String).replaceAll('%', ''))
        : (json['healthPercent'] as num).toDouble(),
      totalDeployments: json['totalDeployments'] as int,
      totalServices: json['totalServices'] as int,
    );
  }

  bool get isHealthy => healthPercent >= 80;
}
