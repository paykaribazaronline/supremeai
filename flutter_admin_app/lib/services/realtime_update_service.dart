// WebSocket service for real-time deployment updates
import 'dart:async';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../models/deployment_model.dart';
import '../models/kubernetes_model.dart';
import '../models/docker_model.dart';
import '../models/pipeline_model.dart';

class RealtimeUpdateService {
  late WebSocketChannel? _channel;
  final String wsUrl;
  
  final StreamController<DeploymentRecord> _deploymentStream =
      StreamController<DeploymentRecord>.broadcast();
  final StreamController<K8sPod> _podStream = StreamController<K8sPod>.broadcast();
  final StreamController<BuildJob> _buildStream = StreamController<BuildJob>.broadcast();
  final StreamController<PipelineExecution> _executionStream =
      StreamController<PipelineExecution>.broadcast();

  Stream<DeploymentRecord> get deploymentUpdates => _deploymentStream.stream;
  Stream<K8sPod> get podUpdates => _podStream.stream;
  Stream<BuildJob> get buildUpdates => _buildStream.stream;
  Stream<PipelineExecution> get executionUpdates => _executionStream.stream;

  RealtimeUpdateService({required this.wsUrl});

  Future<void> connect() async {
    try {
      _channel = WebSocketChannel.connect(Uri.parse(wsUrl));
      
      _channel!.stream.listen(
        (message) => _handleMessage(message),
        onError: (error) => print('WebSocket error: $error'),
        onDone: () => _reconnect(),
      );
    } catch (e) {
      print('Failed to connect WebSocket: $e');
      _reconnect();
    }
  }

  void _handleMessage(dynamic message) {
    try {
      final data = message as Map<String, dynamic>;
      final type = data['type'] as String?;

      switch (type) {
        case 'deployment_update':
          final deployment = DeploymentRecord.fromJson(data['data']);
          _deploymentStream.add(deployment);
          break;
        case 'pod_update':
          final pod = K8sPod.fromJson(data['data']);
          _podStream.add(pod);
          break;
        case 'build_update':
          final build = BuildJob.fromJson(data['data']);
          _buildStream.add(build);
          break;
        case 'execution_update':
          final execution = PipelineExecution.fromJson(data['data']);
          _executionStream.add(execution);
          break;
      }
    } catch (e) {
      print('Error parsing WebSocket message: $e');
    }
  }

  void _reconnect() {
    Future.delayed(const Duration(seconds: 5), () {
      if (_channel?.closeCode == null) {
        connect();
      }
    });
  }

  void subscribe(String deploymentId) {
    _channel?.sink.add({
      'action': 'subscribe',
      'deploymentId': deploymentId,
    });
  }

  void unsubscribe(String deploymentId) {
    _channel?.sink.add({
      'action': 'unsubscribe',
      'deploymentId': deploymentId,
    });
  }

  void close() {
    _channel?.sink.close();
    _deploymentStream.close();
    _podStream.close();
    _buildStream.close();
    _executionStream.close();
  }
}
