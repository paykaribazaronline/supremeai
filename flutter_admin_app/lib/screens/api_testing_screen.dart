import 'dart:convert';
import 'package:flutter/material.dart';
import '../config/app_constants.dart';
import '../services/api_service.dart';

class ApiTestingScreen extends StatefulWidget {
  const ApiTestingScreen({Key? key}) : super(key: key);

  @override
  State<ApiTestingScreen> createState() => _ApiTestingScreenState();
}

class _ApiTestingScreenState extends State<ApiTestingScreen> {
  final ApiService _apiService = ApiService();
  final TextEditingController _urlController = TextEditingController();
  final TextEditingController _headersController = TextEditingController();
  final TextEditingController _bodyController = TextEditingController();
  final TextEditingController _responseController = TextEditingController();

  String _selectedMethod = 'GET';
  bool _isLoading = false;
  String? _error;

  final List<String> _methods = ['GET', 'POST', 'PUT', 'DELETE'];

  @override
  void dispose() {
    _urlController.dispose();
    _headersController.dispose();
    _bodyController.dispose();
    _responseController.dispose();
    super.dispose();
  }

  Future<void> _sendRequest() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      setState(() => _error = 'Please enter a URL');
      return;
    }

    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      // For testing console, use full URL, not relative
      // But ApiService uses baseUrl, so for external URLs, we need to handle differently
      // For now, assume it's relative to baseUrl

      ApiResponse response;
      switch (_selectedMethod) {
        case 'GET':
          response = await _apiService.get(url);
          break;
        case 'POST':
          dynamic data;
          if (_bodyController.text.isNotEmpty) {
            try {
              data = jsonDecode(_bodyController.text);
            } catch (e) {
              data = _bodyController.text;
            }
          }
          response = await _apiService.post(url, data: data);
          break;
        case 'PUT':
          dynamic data;
          if (_bodyController.text.isNotEmpty) {
            try {
              data = jsonDecode(_bodyController.text);
            } catch (e) {
              data = _bodyController.text;
            }
          }
          response = await _apiService.put(url, data: data);
          break;
        case 'DELETE':
          response = await _apiService.delete(url);
          break;
        default:
          throw UnsupportedError('Method $_selectedMethod not supported');
      }

      setState(() {
        _responseController.text = const JsonEncoder.withIndent('  ').convert({
          'success': response.success,
          'statusCode': response.statusCode,
          'data': response.data,
          'error': response.error,
        });
      });
    } catch (e) {
      setState(() {
        _responseController.text = 'Error: $e';
      });
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('API Testing Console'),
        backgroundColor: const Color(AppConstants.primaryColor),
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          children: [
            // Request Section
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppConstants.paddingMedium),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Request',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Method and URL
                    Row(
                      children: [
                        DropdownButton<String>(
                          value: _selectedMethod,
                          items: _methods.map((method) {
                            return DropdownMenuItem(
                              value: method,
                              child: Text(method),
                            );
                          }).toList(),
                          onChanged: (value) {
                            setState(() => _selectedMethod = value!);
                          },
                        ),
                        const SizedBox(width: AppConstants.paddingMedium),
                        Expanded(
                          child: TextField(
                            controller: _urlController,
                            decoration: const InputDecoration(
                              labelText: 'URL',
                              hintText: 'https://api.example.com/endpoint',
                              border: OutlineInputBorder(),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Headers
                    TextField(
                      controller: _headersController,
                      decoration: const InputDecoration(
                        labelText: 'Headers (one per line, key:value)',
                        hintText: 'Content-Type: application/json\nAuthorization: Bearer token',
                        border: OutlineInputBorder(),
                      ),
                      maxLines: 3,
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Body
                    TextField(
                      controller: _bodyController,
                      decoration: const InputDecoration(
                        labelText: 'Request Body (JSON)',
                        hintText: '{"key": "value"}',
                        border: OutlineInputBorder(),
                      ),
                      maxLines: 5,
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Send Button
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: _isLoading ? null : _sendRequest,
                        child: _isLoading
                            ? const CircularProgressIndicator(color: Colors.white)
                            : const Text('Send Request'),
                      ),
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: AppConstants.paddingSmall),
                      Text(
                        _error!,
                        style: const TextStyle(color: Colors.red),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppConstants.paddingLarge),
            // Response Section
            Expanded(
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(AppConstants.paddingMedium),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Response',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: AppConstants.paddingMedium),
                      Expanded(
                        child: TextField(
                          controller: _responseController,
                          decoration: const InputDecoration(
                            border: OutlineInputBorder(),
                            hintText: 'Response will appear here...',
                          ),
                          maxLines: null,
                          readOnly: true,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}