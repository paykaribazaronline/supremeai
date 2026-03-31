// Models for Docker Management
class DockerImage {
  final String imageId;
  final String imageName;
  final String tag;
  final String dockerfilePath;
  final String? baseImageId;
  final String status;
  final String? registry;
  final String? registryUrl;
  final int size;
  final DateTime createdAt;
  final DateTime? pushedAt;
  final int validationCount;
  final String? buildJobId;

  DockerImage({
    required this.imageId,
    required this.imageName,
    required this.tag,
    required this.dockerfilePath,
    this.baseImageId,
    required this.status,
    this.registry,
    this.registryUrl,
    required this.size,
    required this.createdAt,
    this.pushedAt,
    required this.validationCount,
    this.buildJobId,
  });

  factory DockerImage.fromJson(Map<String, dynamic> json) {
    return DockerImage(
      imageId: json['imageId'] as String,
      imageName: json['imageName'] as String,
      tag: json['tag'] as String,
      dockerfilePath: json['dockerfilePath'] as String,
      baseImageId: json['baseImageId'] as String?,
      status: json['status'] as String,
      registry: json['registry'] as String?,
      registryUrl: json['registryUrl'] as String?,
      size: json['size'] as int,
      createdAt: DateTime.parse(json['createdAt'] as String),
      pushedAt: json['pushedAt'] != null ? DateTime.parse(json['pushedAt'] as String) : null,
      validationCount: json['validationCount'] as int,
      buildJobId: json['buildJobId'] as String?,
    );
  }

  String get sizeInMB => '${(size ~/ (1024 * 1024))} MB';
  bool get isReady => status == 'READY';
  bool get isPublished => status == 'PUBLISHED';
}

class BuildJob {
  final String buildJobId;
  final String imageId;
  final String imageRef;
  final String status;
  final DateTime startedAt;
  final DateTime? completedAt;
  final List<String> logs;

  BuildJob({
    required this.buildJobId,
    required this.imageId,
    required this.imageRef,
    required this.status,
    required this.startedAt,
    this.completedAt,
    required this.logs,
  });

  factory BuildJob.fromJson(Map<String, dynamic> json) {
    return BuildJob(
      buildJobId: json['buildJobId'] as String,
      imageId: json['imageId'] as String,
      imageRef: json['imageRef'] as String,
      status: json['status'] as String,
      startedAt: DateTime.parse(json['startedAt'] as String),
      completedAt: json['completedAt'] != null ? DateTime.parse(json['completedAt'] as String) : null,
      logs: List<String>.from(json['logs'] as List),
    );
  }

  Duration? getDuration() {
    if (completedAt == null) return null;
    return completedAt!.difference(startedAt);
  }

  bool get isBuilding => status == 'BUILDING';
  bool get isSuccess => status == 'SUCCESS';
  bool get isFailed => status == 'FAILED';
}

class ImageStats {
  final int totalImages;
  final int readyImages;
  final int publishedImages;
  final int totalSizeBytes;
  final int totalBuildJobs;
  final int averageImageSize;
  final DateTime generatedAt;

  ImageStats({
    required this.totalImages,
    required this.readyImages,
    required this.publishedImages,
    required this.totalSizeBytes,
    required this.totalBuildJobs,
    required this.averageImageSize,
    required this.generatedAt,
  });

  factory ImageStats.fromJson(Map<String, dynamic> json) {
    return ImageStats(
      totalImages: json['totalImages'] as int,
      readyImages: (json['readyImages'] as num).toInt(),
      publishedImages: (json['publishedImages'] as num).toInt(),
      totalSizeBytes: json['totalSizeBytes'] as int,
      totalBuildJobs: json['totalBuildJobs'] as int,
      averageImageSize: json['averageImageSize'] as int,
      generatedAt: DateTime.parse(json['generatedAt'] as String),
    );
  }

  String get totalSizeInGB => '${(totalSizeBytes ~/ (1024 * 1024 * 1024))} GB';
}
