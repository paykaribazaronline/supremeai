import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import '../config/environment.dart';

/// On-device LLM inference service using MediaPipe GenAI.
/// Model catalog is fetched DYNAMICALLY — nothing is hardcoded.
/// Cloud backend → HuggingFace API → local cache (fallback order).
class OnDeviceLlmService {
  static const _channel = MethodChannel('com.supremeai.admin/on_device_llm');

  bool _isModelLoaded = false;
  bool _isDownloading = false;
  double _downloadProgress = 0.0;
  String? _loadedModelName;

  bool get isModelLoaded => _isModelLoaded;
  bool get isDownloading => _isDownloading;
  double get downloadProgress => _downloadProgress;
  String? get loadedModelName => _loadedModelName;

  // ── Dynamic model catalog (fetched at runtime, NEVER hardcoded) ──

  static List<Map<String, String>> _cachedModels = [];
  static bool _modelsLoaded = false;

  /// Fetch available on-device models. Tries in order:
  /// 1. Backend cloud config (single source of truth)
  /// 2. HuggingFace API search for MediaPipe/GGUF compatible models
  /// 3. Cached results from last successful fetch
  static Future<List<Map<String, String>>> getAvailableModels() async {
    if (_modelsLoaded && _cachedModels.isNotEmpty) return _cachedModels;

    // 1. Cloud backend — always try first (CLOUD-FIRST rule)
    try {
      final resp = await http.get(
        Uri.parse('${Environment.baseUrl}/api/config/on-device-models'),
      ).timeout(const Duration(seconds: 10));
      if (resp.statusCode == 200) {
        final List<dynamic> data = jsonDecode(resp.body);
        _cachedModels = data.map((m) => Map<String, String>.from(m)).toList();
        if (_cachedModels.isNotEmpty) {
          _modelsLoaded = true;
          return _cachedModels;
        }
      }
    } catch (_) {}

    // 2. HuggingFace API — discover MediaPipe-compatible on-device models
    try {
      final resp = await http.get(
        Uri.parse(
          'https://huggingface.co/api/models'
          '?filter=mediapipe&sort=downloads&direction=-1&limit=20',
        ),
      ).timeout(const Duration(seconds: 10));
      if (resp.statusCode == 200) {
        final List<dynamic> data = jsonDecode(resp.body);
        final models = <Map<String, String>>[];
        for (final m in data) {
          final modelId = m['modelId']?.toString() ?? '';
          final siblings = List<Map<String, dynamic>>.from(
            (m['siblings'] as List?)?.map((s) => Map<String, dynamic>.from(s)) ?? [],
          );
          // Find .bin or .task files (MediaPipe model format)
          final binFile = siblings.firstWhere(
            (s) => s['rfilename']?.toString().endsWith('.bin') == true
                || s['rfilename']?.toString().endsWith('.task') == true,
            orElse: () => <String, dynamic>{},
          );
          final fileName = binFile['rfilename']?.toString();
          if (modelId.isNotEmpty) {
            models.add({
              'id': modelId.replaceAll('/', '--'),
              'name': modelId.split('/').last,
              'size': _formatBytes(binFile['size']),
              'description': '${m['pipeline_tag'] ?? 'LLM'} — $modelId',
              'url': fileName != null
                  ? 'https://huggingface.co/$modelId/resolve/main/$fileName'
                  : '',
            });
          }
        }
        if (models.isNotEmpty) {
          _cachedModels = models.where((m) => m['url']!.isNotEmpty).toList();
          _modelsLoaded = true;
          return _cachedModels;
        }
      }
    } catch (_) {}

    // 3. Google MediaPipe model index (last network attempt)
    try {
      final resp = await http.get(
        Uri.parse(
          'https://huggingface.co/api/models'
          '?search=mediapipe+llm+inference&sort=downloads&direction=-1&limit=10',
        ),
      ).timeout(const Duration(seconds: 10));
      if (resp.statusCode == 200) {
        final List<dynamic> data = jsonDecode(resp.body);
        final models = <Map<String, String>>[];
        for (final m in data) {
          final modelId = m['modelId']?.toString() ?? '';
          if (modelId.isNotEmpty) {
            models.add({
              'id': modelId.replaceAll('/', '--'),
              'name': modelId.split('/').last,
              'size': 'Unknown',
              'description': '${m['pipeline_tag'] ?? 'LLM'} — $modelId',
              'url': 'https://huggingface.co/$modelId/resolve/main/model.bin',
            });
          }
        }
        if (models.isNotEmpty) {
          _cachedModels = models;
          _modelsLoaded = true;
          return _cachedModels;
        }
      }
    } catch (_) {}

    return _cachedModels;
  }

  /// Clear cached models to force re-fetch
  static void clearModelCache() {
    _cachedModels = [];
    _modelsLoaded = false;
  }

  static String _formatBytes(dynamic bytes) {
    if (bytes == null) return 'Unknown';
    final b = bytes is int ? bytes : int.tryParse(bytes.toString()) ?? 0;
    if (b <= 0) return 'Unknown';
    if (b > 1e9) return '~${(b / 1e9).toStringAsFixed(1)} GB';
    if (b > 1e6) return '~${(b / 1e6).toStringAsFixed(0)} MB';
    return '~${(b / 1e3).toStringAsFixed(0)} KB';
  }

  /// Check if a model file exists on device.
  Future<bool> isModelDownloaded(String modelId) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        'isModelDownloaded',
        {'modelId': modelId},
      );
      return result ?? false;
    } on PlatformException {
      return false;
    }
  }

  /// Download model to local storage. Reports progress via [onProgress].
  Future<bool> downloadModel(
    String modelId,
    String url, {
    void Function(double progress)? onProgress,
  }) async {
    _isDownloading = true;
    _downloadProgress = 0.0;

    try {
      // Listen to download progress events
      _channel.setMethodCallHandler((call) async {
        if (call.method == 'downloadProgress') {
          _downloadProgress = (call.arguments as double?) ?? 0.0;
          onProgress?.call(_downloadProgress);
        }
      });

      final result = await _channel.invokeMethod<bool>(
        'downloadModel',
        {'modelId': modelId, 'url': url},
      );

      _isDownloading = false;
      _downloadProgress = 1.0;
      return result ?? false;
    } on PlatformException {
      _isDownloading = false;
      return false;
    }
  }

  /// Delete a downloaded model to free space.
  Future<bool> deleteModel(String modelId) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        'deleteModel',
        {'modelId': modelId},
      );
      if (modelId == _loadedModelName) {
        _isModelLoaded = false;
        _loadedModelName = null;
      }
      return result ?? false;
    } on PlatformException {
      return false;
    }
  }

  /// Load model into memory for inference. Must be downloaded first.
  Future<bool> loadModel(String modelId) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        'loadModel',
        {'modelId': modelId},
      );
      _isModelLoaded = result ?? false;
      if (_isModelLoaded) _loadedModelName = modelId;
      return _isModelLoaded;
    } on PlatformException {
      _isModelLoaded = false;
      return false;
    }
  }

  /// Unload model from memory.
  Future<void> unloadModel() async {
    try {
      await _channel.invokeMethod<void>('unloadModel');
    } on PlatformException {
      // ignore
    }
    _isModelLoaded = false;
    _loadedModelName = null;
  }

  /// Generate a response from the on-device model.
  /// Returns a stream of partial tokens for streaming effect.
  Stream<String> generateResponseStream(String prompt) {
    final controller = StreamController<String>();

    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onToken':
          controller.add(call.arguments as String);
          break;
        case 'onComplete':
          await controller.close();
          break;
        case 'onError':
          controller.addError(call.arguments as String);
          await controller.close();
          break;
      }
    });

    _channel
        .invokeMethod<void>('generateResponse', {'prompt': prompt})
        .catchError((e) {
      controller.addError(e.toString());
      controller.close();
    });

    return controller.stream;
  }

  /// Generate a full response (non-streaming).
  Future<String> generateResponse(String prompt) async {
    try {
      final result = await _channel.invokeMethod<String>(
        'generateResponseFull',
        {'prompt': prompt},
      );
      return result ?? 'মডেল থেকে উত্তর পাওয়া যায়নি।';
    } on PlatformException catch (e) {
      return 'ত্রুটি: ${e.message}';
    }
  }

  /// Get device capability info (RAM, GPU support, etc.)
  Future<Map<String, dynamic>> getDeviceInfo() async {
    try {
      final result = await _channel.invokeMethod<Map>('getDeviceInfo');
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException {
      return {'error': 'Device info unavailable'};
    }
  }

  /// Get storage usage for downloaded models.
  Future<Map<String, dynamic>> getStorageInfo() async {
    try {
      final result = await _channel.invokeMethod<Map>('getStorageInfo');
      return Map<String, dynamic>.from(result ?? {});
    } on PlatformException {
      return {'totalBytes': 0, 'models': <String, int>{}};
    }
  }
}
