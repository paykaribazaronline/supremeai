// flutter_admin_app/lib/screens/admin/admin_unified_screen.dart
// UNIFIED ADMIN DASHBOARD - Consumes /api/admin/dashboard/contract
//
// Same contract as React Web → Identical UI everywhere
// Change backend contract = changes in React + Flutter Mobile + Flutter Web

import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class AdminUnifiedScreen extends StatefulWidget {
  const AdminUnifiedScreen({Key? key}) : super(key: key);

  @override
  State<AdminUnifiedScreen> createState() => _AdminUnifiedScreenState();
}

class _AdminUnifiedScreenState extends State<AdminUnifiedScreen> {
  final ApiService _apiService = ApiService();

  Map<String, dynamic>? _contract;
  bool _isLoading = true;
  String? _error;
  String _selectedComponentKey = 'overview';

  @override
  void initState() {
    super.initState();
    _loadContract();
  }

  Future<void> _loadContract() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    final response = await _apiService.get<Map<String, dynamic>>(
      '${Environment.apiBaseUrl}/api/admin/dashboard/contract',
    );

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (response.success) {
        _contract = response.data;
      } else {
        _error = response.error ?? 'চুক্তি লোড করা যায়নি';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_contract?['title'] ?? 'অ্যাডমিন ড্যাশবোর্ড'),
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Center(
              child: Text(
                'v${_contract?['contractVersion'] ?? 'unknown'}',
                style: const TextStyle(fontSize: 12, color: Colors.white70),
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadContract,
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
                      const Icon(Icons.error_outline,
                          size: 48, color: Colors.grey),
                      const SizedBox(height: 16),
                      Text(_error!),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadContract,
                        child: const Text('আবার চেষ্টা করুন'),
                      ),
                    ],
                  ),
                )
              : _contract == null
                  ? const SizedBox.shrink()
                  : _buildContent(),
    );
  }

  Widget _buildContent() {
    final navigation = List<Map<String, dynamic>>.from(
      _contract?['navigation'] ?? [],
    );
    final components = List<Map<String, dynamic>>.from(
      _contract?['components'] ?? [],
    );
    final stats = _contract?['stats'] as Map<String, dynamic>? ?? {};

    final selectedComponent = components.firstWhere(
      (c) => c['key'] == _selectedComponentKey,
      orElse: () => components.isNotEmpty ? components.first : {},
    );

    return SingleChildScrollView(
      child: Column(
        children: [
          // Stats Section
          Container(
            padding: const EdgeInsets.all(AppConstants.paddingLarge),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'সিস্টেম পরিসংখ্যান',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 12),
                GridView.count(
                  crossAxisCount: 2,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                  children: [
                    _buildStatCard(
                      'সক্রিয় AI',
                      '${stats['activeAIAgents'] ?? 0}',
                      Colors.blue,
                    ),
                    _buildStatCard(
                      'চলমান কাজ',
                      '${stats['runningTasks'] ?? 0}',
                      Colors.orange,
                    ),
                    _buildStatCard(
                      'সম্পন্ন কাজ',
                      '${stats['completedTasks'] ?? 0}',
                      Colors.green,
                    ),
                    _buildStatCard(
                      'সাফল্যের হার',
                      '${stats['successRate'] ?? 0}%',
                      Colors.purple,
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Navigation Menu
          Container(
            padding: const EdgeInsets.all(AppConstants.paddingLarge),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'উপলব্ধ বিভাগ',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: navigation.map<Widget>((item) {
                    final isSelected = item['key'] == _selectedComponentKey;
                    return FilterChip(
                      label: Text(item['label'] ?? 'Unknown'),
                      selected: isSelected,
                      onSelected: !item['enabled'] ?? true
                          ? null
                          : (selected) {
                              if (selected) {
                                setState(
                                    () => _selectedComponentKey = item['key']);
                              }
                            },
                    );
                  }).toList(),
                ),
              ],
            ),
          ),

          // Selected Component Details
          if (selectedComponent.isNotEmpty)
            Container(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Card(
                elevation: 2,
                child: Padding(
                  padding: const EdgeInsets.all(AppConstants.paddingLarge),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            selectedComponent['icon'] ?? '🔹',
                            style: const TextStyle(fontSize: 24),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  selectedComponent['label'] ?? 'অজানা',
                                  style: const TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Text(
                                  'বিভাগ: ${selectedComponent['category'] ?? 'N/A'}',
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: (selectedComponent['enabled'] ?? false)
                                  ? Colors.green
                                  : Colors.red,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              (selectedComponent['enabled'] ?? false)
                                  ? 'সক্রিয়'
                                  : 'নিষ্ক্রিয়',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'কনফিগারেশন',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: SingleChildScrollView(
                          scrollDirection: Axis.horizontal,
                          child: Text(
                            _formatJson(selectedComponent['config'] ?? {}),
                            style: const TextStyle(
                              fontSize: 11,
                              fontFamily: 'monospace',
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildStatCard(String label, String value, Color color) {
    return Card(
      elevation: 2,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(8),
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [color.withOpacity(0.1), color.withOpacity(0.05)],
          ),
        ),
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              value,
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }

  String _formatJson(dynamic obj) {
    try {
      return obj.toString();
    } catch (_) {
      return 'N/A';
    }
  }
}
