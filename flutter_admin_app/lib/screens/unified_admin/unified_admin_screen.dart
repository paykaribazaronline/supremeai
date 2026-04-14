import 'package:flutter/material.dart';

import '../../config/environment.dart';
import '../../services/api_service.dart';
import '../system_learning_screen.dart';
import '../settings_screen.dart';
import '../chat/offline_chat_screen.dart';
import '../metrics_screen.dart';
import '../projects/projects_list_screen.dart';
import '../phases/phases_screen.dart';
import '../providers/ai_providers_screen.dart';
import '../analytics/analytics_screen.dart';
import '../consensus/consensus_screen.dart';
import '../vpn/vpn_screen.dart';
import '../teaching/teaching_screen.dart';

/// UNIFIED ADMIN SCREEN - Fully Functional Feature Integration
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

    final title = _contract?['title'] as String? ?? 'SupremeAI Control Panel';
    final navigation = (_contract?['navigation'] as List?)?.cast<Map<String, dynamic>>() ?? [];
    final components = (_contract?['components'] as List?)?.cast<Map<String, dynamic>>() ?? [];

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        elevation: 0,
        backgroundColor: const Color(0xFF722ED1),
        foregroundColor: Colors.white,
      ),
      body: CustomScrollView(
        slivers: [
          // Navigation - Dynamic Filter Chips
          SliverToBoxAdapter(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Wrap(
                spacing: 8,
                children: navigation
                    .map((item) => FilterChip(
                          avatar: Text(item['icon'] ?? '📦'),
                          label: Text(item['label'] as String? ?? ''),
                          selected: _selectedComponentKey == (item['key'] ?? ''),
                          onSelected: (item['enabled'] as bool? ?? true)
                              ? (selected) {
                                  if (selected) {
                                    setState(() {
                                      _selectedComponentKey = item['key'] as String? ?? '';
                                    });
                                  }
                                }
                              : null,
                          selectedColor: const Color(0xFF722ED1).withOpacity(0.2),
                          checkmarkColor: const Color(0xFF722ED1),
                        ))
                    .toList(),
              ),
            ),
          ),
          
          // Component Details & Real Features
          SliverFillRemaining(
            hasScrollBody: true,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: _buildComponentContent(components),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildComponentContent(List<Map<String, dynamic>> components) {
    // 1. Map Keys to Real Feature Widgets
    Widget featureWidget;
    bool showSuggestion = true;

    switch (_selectedComponentKey) {
      case 'overview':
      case 'metrics':
        featureWidget = const MetricsScreen();
        break;
      case 'chat':
        featureWidget = const OfflineChatScreen();
        break;
      case 'learning':
        featureWidget = const SystemLearningScreen();
        break;
      case 'settings':
        featureWidget = const SettingsScreen();
        break;
      case 'projects':
        featureWidget = const ProjectsListScreen();
        break;
      case 'phases':
        featureWidget = const PhasesScreen();
        break;
      case 'providers':
        featureWidget = const AIProvidersScreen();
        break;
      case 'analytics':
        featureWidget = const AnalyticsScreen();
        break;
      case 'consensus':
        featureWidget = const ConsensusScreen();
        break;
      case 'vpn':
        featureWidget = const VPNScreen();
        break;
      case 'teaching':
        featureWidget = const TeachingScreen();
        break;
      default:
        // Generic View for unmapped components
        final selected = components.firstWhere(
          (c) => c['key'] == _selectedComponentKey,
          orElse: () => {},
        );
        featureWidget = _buildGenericInfoCard(selected);
        showSuggestion = selected.isNotEmpty;
    }

    return Column(
      children: [
        Expanded(child: featureWidget),
        if (showSuggestion) ...[
          const SizedBox(height: 16),
          _buildSuggestionButton(),
        ],
      ],
    );
  }

  Widget _buildGenericInfoCard(Map<String, dynamic> selected) {
    if (selected.isEmpty) return const Center(child: Text('Feature coming soon...'));
    
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Text(selected['icon'] ?? '📦', style: const TextStyle(fontSize: 48)),
            const SizedBox(height: 12),
            Text(selected['label'] ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text(selected['description'] ?? '', textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }

  Widget _buildSuggestionButton() {
    return ElevatedButton.icon(
      onPressed: () => _showSuggestionDialog(context, _selectedComponentKey, _selectedComponentKey.toUpperCase()),
      icon: const Icon(Icons.lightbulb_outline),
      label: const Text('Suggest Changes'),
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF722ED1),
        foregroundColor: Colors.white,
        minimumSize: const Size(double.infinity, 50),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  void _showSuggestionDialog(BuildContext context, String tabKey, String tabLabel) {
    final controller = TextEditingController();
    bool isLoading = false;

    showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text('Suggest Changes for $tabLabel'),
          content: TextField(
            controller: controller,
            maxLines: 5,
            decoration: const InputDecoration(hintText: 'Describe your suggestion...'),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: isLoading ? null : () async {
                setDialogState(() => isLoading = true);
                await _submitSuggestion(ctx, tabKey, tabLabel, controller.text, false);
                if (ctx.mounted) Navigator.of(ctx).pop();
              },
              child: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submitSuggestion(BuildContext context, String tabKey, String tabLabel, String suggestion, bool applyNow) async {
    try {
      await _apiService.post('${Environment.apiBaseUrl}/api/admin/suggestions', data: {
        'tabKey': tabKey, 'tabLabel': tabLabel, 'suggestion': suggestion, 'applyNow': applyNow,
      });
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Suggestion submitted!')));
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    }
  }
}
