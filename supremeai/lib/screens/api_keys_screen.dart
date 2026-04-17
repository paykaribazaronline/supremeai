import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class ApiKeysScreen extends StatefulWidget {
  const ApiKeysScreen({super.key});

  @override
  State<ApiKeysScreen> createState() => _ApiKeysScreenState();
}

class _ApiKeysScreenState extends State<ApiKeysScreen> {
  List<Map<String, dynamic>> _apiKeys = [];
  bool _isLoading = true;
  String? _apiKey;

  @override
  void initState() {
    super.initState();
    _loadApiKeys();
  }

  Future<void> _loadApiKeys() async {
    final auth = context.read<AuthProvider>();
    if (auth.token == null) return;

    try {
      final response = await http.get(
        Uri.parse('https://supremeai-565236080752.us-central1.run.app/api/user/apis'),
        headers: {
          'Authorization': 'Bearer ${auth.token}',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        setState(() {
          _apiKeys = List<Map<String, dynamic>>.from(data['apis'] ?? []);
          _isLoading = false;
        });
      }
    } catch (e) {
      // Handle error
      setState(() => _isLoading = false);
    }
  }

  Future<void> _createApiKey(String name, String description) async {
    final auth = context.read<AuthProvider>();
    if (auth.token == null) return;

    try {
      final response = await http.post(
        Uri.parse('https://supremeai-565236080752.us-central1.run.app/api/user/apis'),
        headers: {
          'Authorization': 'Bearer ${auth.token}',
          'Content-Type': 'application/json',
        },
        body: json.encode({
          'apiName': name,
          'description': description,
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        setState(() {
          _apiKey = data['apiKey'];
        });
        _loadApiKeys();
      }
    } catch (e) {
      // Handle error
    }
  }

  Future<void> _deleteApiKey(int id) async {
    final auth = context.read<AuthProvider>();
    if (auth.token == null) return;

    try {
      await http.delete(
        Uri.parse('https://supremeai-565236080752.us-central1.run.app/api/user/apis/$id'),
        headers: {
          'Authorization': 'Bearer ${auth.token}',
        },
      );
      _loadApiKeys();
    } catch (e) {
      // Handle error
    }
  }

  void _showCreateApiKeyDialog() {
    final nameController = TextEditingController();
    final descController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Create API Key'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(labelText: 'API Name'),
            ),
            TextField(
              controller: descController,
              decoration: const InputDecoration(labelText: 'Description'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              if (nameController.text.isNotEmpty && descController.text.isNotEmpty) {
                _createApiKey(nameController.text, descController.text);
                Navigator.of(context).pop();
              }
            },
            child: const Text('Create'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('API Keys'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _showCreateApiKeyDialog,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _apiKeys.isEmpty
              ? const Center(child: Text('No API keys found. Create one to get started.'))
              : ListView.builder(
                  itemCount: _apiKeys.length,
                  itemBuilder: (context, index) {
                    final apiKey = _apiKeys[index];
                    return ListTile(
                      title: Text(apiKey['apiName'] ?? ''),
                      subtitle: Text(apiKey['description'] ?? ''),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete),
                        onPressed: () => _deleteApiKey(apiKey['id']),
                      ),
                    );
                  },
                ),
    );
  }
}