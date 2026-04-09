import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

/// UNIFIED ADMIN SCREEN - Consumes Backend Contract
/// ================================================
/// This screen fetches the admin dashboard contract from the backend
/// and renders it natively using Flutter widgets (NOT WebView).
/// 
/// Benefits:
/// ✅ True native Flutter implementation (not WebView)
/// ✅ Consumes same /api/admin/dashboard/contract as React
/// ✅ Feature parity guaranteed (same backend = same UI)
/// ✅ Faster performance (native Flutter rendering)
/// ✅ Offline support (can cache contract)
/// ✅ Mobile-optimized (Material Design)
/// 
/// Backend: /api/admin/dashboard/contract
/// Returns: 23 components (admin controls + platform-specific screens)

class UnifiedAdminScreen extends StatefulWidget {
  const UnifiedAdminScreen({Key? key}) : super(key: key);

  @override
  State<UnifiedAdminScreen> createState() => _UnifiedAdminScreenState();
}

class _UnifiedAdminScreenState extends State<UnifiedAdminScreen> {
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

    try {
      final response = await _apiService.get<Map<String, dynamic>>(
        '${Environment.apiBaseUrl}/api/admin/dashboard/contract',
      );

      if (!mounted) return;
      
      if (response.success && response.data != null) {
        setState(() {
          _contract = response.data;
          _isLoading = false;
        });
      } else {
        setState(() {
          _error = response.error ?? 'Failed to load dashboard contract';
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Error: ${e.toString()}';
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (_error != null) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.grey),
              const SizedBox(height: 16),
              Text(_error!),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loadContract,
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    if (_contract == null) {
      return const Scaffold(
        body: Center(child: Text('No contract data')),
      );
    }

    final title = _contract!['title'] as String? ?? 'Admin Dashboard';
    final stats = _contract!['stats'] as Map<String, dynamic>? ?? {};
    final navigation = (_contract!['navigation'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    final components =
        (_contract!['components'] as List?)?.cast<Map<String, dynamic>>() ?? [];

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        elevation: 0,
      ),
      body: CustomScrollView(
        slivers: [
          // Stats Cards
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                children: [
                  _buildStatCard(
                    'সক্রিয় AI',
                    '${stats['activeAIAgents'] ?? 0}',
                    '🤖',
                    Colors.blue,
                  ),
                  _buildStatCard(
                    'চলমান কাজ',
                    '${stats['runningTasks'] ?? 0}',
                    '⚙️',
                    Colors.orange,
                  ),
                  _buildStatCard(
                    'সম্পন্ন',
                    '${stats['completedTasks'] ?? 0}',
                    '✅',
                    Colors.green,
                  ),
                  _buildStatCard(
                    'সাফল্য',
                    '${stats['successRate'] ?? 0}%',
                    '📊',
                    Colors.purple,
                  ),
                ],
              ),
            ),
          ),
          
          // Navigation
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Wrap(
                spacing: 8,
                children: navigation
                    .map((item) => FilterChip(
                          label: Text(
                            item['label'] as String? ?? '',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          selected:
                              _selectedComponentKey == (item['key'] ?? ''),
                          onSelected: (item['enabled'] as bool? ?? true)
                              ? (selected) {
                                  if (selected) {
                                    setState(() {
                                      _selectedComponentKey =
                                          item['key'] as String? ?? '';
                                    });
                                  }
                                }
                              : null,
                        ))
                    .toList(),
              ),
            ),
          ),
          
          // Component Details
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: _buildComponentDetails(components),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(
    String label,
    String value,
    String emoji,
    Color color,
  ) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [color.withOpacity(0.8), color.withOpacity(0.4)],
          ),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(emoji, style: const TextStyle(fontSize: 32)),
            const SizedBox(height: 8),
            Text(
              value,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.white70,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildComponentDetails(List<Map<String, dynamic>> components) {
    final selected = components.firstWhere(
      (c) => c['key'] == _selectedComponentKey,
      orElse: () => components.isNotEmpty ? components[0] : {},
    );

    if (selected.isEmpty) {
      return const Text('No component selected');
    }

    final label = selected['label'] as String? ?? 'Component';
    final icon = selected['icon'] as String? ?? '📦';
    final description = selected['description'] as String? ?? '';
    final category = selected['category'] as String? ?? '';
    final config = selected['config'] as Map<String, dynamic>? ?? {};

    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(icon, style: const TextStyle(fontSize: 28)),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        label,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (description.isNotEmpty)
                        Text(
                          description,
                          style: const TextStyle(
                            fontSize: 12,
                            color: Colors.grey,
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (category.isNotEmpty)
              Chip(
                label: Text(category),
                backgroundColor: Colors.blue.withOpacity(0.2),
              ),
            const SizedBox(height: 12),
            if (config.isNotEmpty) ...[
              const Text(
                'Configuration',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Text(
                    config.toString(),
                    style: const TextStyle(fontSize: 11, fontFamily: 'monospace'),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}