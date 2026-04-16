import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class UserManagementScreen extends StatefulWidget {
  const UserManagementScreen({Key? key}) : super(key: key);

  @override
  State<UserManagementScreen> createState() => _UserManagementScreenState();
}

class _UserManagementScreenState extends State<UserManagementScreen> {
  final ApiService _apiService = ApiService();
  List<dynamic> _users = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final response = await _apiService.get<List<dynamic>>(Environment.usersList);
      if (!mounted) return;

      setState(() {
        _isLoading = false;
        if (response.success) {
          _users = response.data ?? [];
        } else {
          _error = response.error ?? 'ব্যবহারকারী তালিকা লোড করা যায়নি';
        }
      });
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = 'Error: $e';
        });
      }
    }
  }

  Future<void> _updateUserSetting(String userId, String type, dynamic value) async {
    final endpoint = type == 'quota' ? Environment.userUpdateQuota : Environment.userUpdateCost;
    final response = await _apiService.post<Map<String, dynamic>>(
      endpoint,
      data: {
        'userId': userId,
        type: value,
      },
    );

    if (!mounted) return;

    if (response.success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('সফলভাবে আপডেট করা হয়েছে')),
      );
      _loadUsers();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('ত্রুটি: ${response.error}')),
      );
    }
  }

  void _showEditDialog(Map<String, dynamic> user, String type) {
    final controller = TextEditingController(
      text: (type == 'quota' ? user['quota'] : user['apiCost'])?.toString() ?? '0',
    );

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(type == 'quota' ? 'কোটা সেট করুন' : 'API খরচ সেট করুন'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            labelText: type == 'quota' ? 'অনুরোধ সীমা' : 'টাকা/ডলার খরচ সীমা',
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('বাতিল')),
          ElevatedButton(
            onPressed: () {
              final value = double.tryParse(controller.text) ?? 0.0;
              _updateUserSetting(user['id'], type, value);
              Navigator.pop(ctx);
            },
            child: const Text('সংরক্ষণ'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ব্যবহারকারী ব্যবস্থাপনা'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadUsers,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, size: 48, color: Colors.grey),
                      const SizedBox(height: 16),
                      Text(_error!),
                      const SizedBox(height: 16),
                      ElevatedButton(onPressed: _loadUsers, child: const Text('আবার চেষ্টা করুন')),
                    ],
                  ),
                )
              : _users.isEmpty
                  ? const Center(child: Text('কোনো ব্যবহারকারী পাওয়া যায়নি'))
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _users.length,
                      itemBuilder: (ctx, index) {
                        final user = _users[index] as Map<String, dynamic>;
                        return Card(
                          margin: const EdgeInsets.bottom(12),
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      child: Text(user['name']?[0] ?? 'U'),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            user['name'] ?? 'অজানা ব্যবহারকারী',
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                          ),
                                          Text(user['email'] ?? '', style: const TextStyle(color: Colors.grey)),
                                        ],
                                      ),
                                    ),
                                    Chip(
                                      label: Text(user['role'] ?? 'user'),
                                      backgroundColor: (user['role'] == 'admin' ? Colors.purple : Colors.blue).withOpacity(0.1),
                                    ),
                                  ],
                                ),
                                const Divider(height: 24),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    _buildInfoItem('ব্যবহৃত কোটা', '${user['usedQuota'] ?? 0}/${user['quota'] ?? 0}'),
                                    _buildInfoItem('API খরচ', '\$${user['apiCost'] ?? 0}'),
                                  ],
                                ),
                                const SizedBox(height: 16),
                                Row(
                                  children: [
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        icon: const Icon(Icons.speed, size: 18),
                                        label: const Text('কোটা সেট'),
                                        onPressed: () => _showEditDialog(user, 'quota'),
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        icon: const Icon(Icons.attach_money, size: 18),
                                        label: const Text('খরচ সীমা'),
                                        onPressed: () => _showEditDialog(user, 'apiCost'),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
    );
  }

  Widget _buildInfoItem(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
      ],
    );
  }
}
