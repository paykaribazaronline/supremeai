import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

/// PhasesScreen — shows the health and status of ALL operational phases
/// (Phase 1, 6, 7, 8, 9, 10) in a unified tab view.
class PhasesScreen extends StatefulWidget {
  const PhasesScreen({Key? key}) : super(key: key);

  @override
  State<PhasesScreen> createState() => _PhasesScreenState();
}

class _PhasesScreenState extends State<PhasesScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  // All-phases overview
  Map<String, dynamic>? _allPhases;

  // Phase 1 — Optimization
  Map<String, dynamic>? _phase1Health;
  Map<String, dynamic>? _phase1Metrics;

  // Phase 6 — Integration
  Map<String, dynamic>? _phase6Health;
  List<dynamic> _phase6Features = [];
  Map<String, dynamic>? _phase6Metrics;

  // Phase 7 — Platform Generation
  Map<String, dynamic>? _phase7Summary;
  Map<String, dynamic>? _phase7Capabilities;

  // Phase 8 — Security
  Map<String, dynamic>? _phase8Summary;

  // Phase 9 — Cost Intelligence
  Map<String, dynamic>? _phase9Summary;

  // Phase 10 — Self-Improvement
  Map<String, dynamic>? _phase10Summary;

  bool _isLoading = true;
  String? _error;

  static const _tabs = [
    Tab(icon: Icon(Icons.tune), text: 'Phase 1'),
    Tab(icon: Icon(Icons.integration_instructions), text: 'Phase 6'),
    Tab(icon: Icon(Icons.devices), text: 'Phase 7'),
    Tab(icon: Icon(Icons.security), text: 'Phase 8'),
    Tab(icon: Icon(Icons.attach_money), text: 'Phase 9'),
    Tab(icon: Icon(Icons.auto_awesome), text: 'Phase 10'),
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: _tabs.length, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>(Environment.allPhases),        // 0
      _apiService.get<Map<String, dynamic>>(Environment.phase1Health),     // 1
      _apiService.get<Map<String, dynamic>>(Environment.phase1Metrics),    // 2
      _apiService.get<Map<String, dynamic>>(Environment.phase6Health),     // 3
      _apiService.get<List<dynamic>>(Environment.phase6Features),          // 4
      _apiService.get<Map<String, dynamic>>(Environment.phase6Metrics),    // 5
      _apiService.get<Map<String, dynamic>>(Environment.phase7Summary),    // 6
      _apiService.get<Map<String, dynamic>>(Environment.phase7Capabilities), // 7
      _apiService.get<Map<String, dynamic>>(Environment.phase8Summary),    // 8
      _apiService.get<Map<String, dynamic>>(Environment.phase9Summary),    // 9
      _apiService.get<Map<String, dynamic>>(Environment.phase10Summary),   // 10
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) _allPhases = results[0].data as Map<String, dynamic>?;
      if (results[1].success) _phase1Health = results[1].data as Map<String, dynamic>?;
      if (results[2].success) _phase1Metrics = results[2].data as Map<String, dynamic>?;
      if (results[3].success) _phase6Health = results[3].data as Map<String, dynamic>?;
      if (results[4].success) _phase6Features = (results[4].data as List<dynamic>?) ?? [];
      if (results[5].success) _phase6Metrics = results[5].data as Map<String, dynamic>?;
      if (results[6].success) _phase7Summary = results[6].data as Map<String, dynamic>?;
      if (results[7].success) _phase7Capabilities = results[7].data as Map<String, dynamic>?;
      if (results[8].success) _phase8Summary = results[8].data as Map<String, dynamic>?;
      if (results[9].success) _phase9Summary = results[9].data as Map<String, dynamic>?;
      if (results[10].success) _phase10Summary = results[10].data as Map<String, dynamic>?;
      if (!results[1].success && !results[3].success && !results[6].success) {
        _error = 'Phase data could not be loaded';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('All Phases'),
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          tabs: _tabs,
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
            onPressed: _loadAll,
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
                      const Icon(Icons.layers, size: 48, color: Colors.grey),
                      const SizedBox(height: 12),
                      Text(_error!),
                      const SizedBox(height: 12),
                      ElevatedButton(
                        onPressed: _loadAll,
                        child: const Text('Retry'),
                      ),
                    ],
                  ),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildPhase1Tab(),
                    _buildPhase6Tab(),
                    _buildPhase7Tab(),
                    _buildPhase8Tab(),
                    _buildPhase9Tab(),
                    _buildPhase10Tab(),
                  ],
                ),
    );
  }

  // ─────────────────────── Phase 1: Optimization ───────────────────────────

  Widget _buildPhase1Tab() {
    final h = _phase1Health ?? {};
    final m = _phase1Metrics ?? {};
    final statusStr = '${h['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final isOk = statusStr.contains('OK') || statusStr.contains('UP') || statusStr.contains('PHASE 1');

    return _buildPhaseScrollView(
      header: _PhaseHeader(
        phaseNumber: 1,
        title: 'Optimization',
        subtitle: 'LRU Cache · Smart Weighting · Firebase Sync · Error DLQ',
        icon: Icons.tune,
        color: const Color(AppConstants.primaryColor),
        isHealthy: isOk,
        statusLabel: isOk ? 'Operational' : 'Degraded',
      ),
      statsWidgets: [
        _buildStatCard(
          'Cache',
          _nestedStr(m, ['cache', 'status'], 'N/A'),
          Icons.memory,
          AppConstants.primaryColor,
        ),
        _buildStatCard(
          'Weighting',
          _nestedStr(m, ['weighting', 'status'], 'N/A'),
          Icons.balance,
          AppConstants.infoColor,
        ),
        _buildStatCard(
          'Sync',
          _nestedStr(m, ['firebaseSync', 'status'], 'N/A'),
          Icons.sync,
          AppConstants.successColor,
        ),
        _buildStatCard(
          'Error DLQ',
          _nestedStr(m, ['errorDLQ', 'status'], 'N/A'),
          Icons.error_outline,
          AppConstants.warningColor,
        ),
      ],
      features: [
        'LRU Cache (40% fewer Firebase reads)',
        'Smart Provider Weighting (success-rate-based)',
        'Optimized Firebase Sync (5-min batch)',
        'Error Dead-Letter Queue (10% sample)',
      ],
      featuresEnabled: [true, true, true, true],
    );
  }

  // ─────────────────────── Phase 6: Integration ────────────────────────────

  Widget _buildPhase6Tab() {
    final h = _phase6Health ?? {};
    final m = _phase6Metrics ?? {};
    final status = '${h['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final isHealthy = status == 'UP' || status == 'HEALTHY';

    final featureNames = _phase6Features.map((f) {
      if (f is Map) return '${f['name'] ?? f}';
      return '$f'.replaceFirst(RegExp(r'^✅\s*'), '');
    }).toList();

    final featureEnabled = _phase6Features.map((f) {
      if (f is Map) return f['enabled'] != false;
      return true;
    }).toList();

    return _buildPhaseScrollView(
      header: _PhaseHeader(
        phaseNumber: 6,
        title: 'Integration',
        subtitle: 'Decision Logging · Auto-Fix Loop · A/B Testing · Timeline',
        icon: Icons.integration_instructions,
        color: Colors.indigo,
        isHealthy: isHealthy,
        statusLabel: isHealthy ? 'Operational' : 'Issues Detected',
      ),
      statsWidgets: [
        _buildStatCard('Workflows', '${m['workflows'] ?? 0}', Icons.account_tree, AppConstants.primaryColor),
        _buildStatCard('Success', '${m['successRate'] ?? 0}%', Icons.check_circle, AppConstants.successColor),
        _buildStatCard('Avg Time', '${m['avgTime'] ?? 0}s', Icons.timer, AppConstants.infoColor),
        _buildStatCard('Errors', '${m['errors'] ?? 0}', Icons.error, AppConstants.errorColor),
      ],
      features: featureNames.isEmpty
          ? ['Decision logging', 'Auto-fix loop', 'A/B testing', 'Timeline visualization']
          : featureNames,
      featuresEnabled: featureEnabled.isEmpty ? [true, true, true, true] : featureEnabled,
    );
  }

  // ─────────────────────── Phase 7: Platform Generation ────────────────────

  Widget _buildPhase7Tab() {
    final s = _phase7Summary ?? {};
    final caps = _phase7Capabilities ?? {};

    return _buildPhaseScrollView(
      header: const _PhaseHeader(
        phaseNumber: 7,
        title: 'Multi-Platform Generation',
        subtitle: 'iOS · Web · Desktop · Play Store · App Store',
        icon: Icons.devices,
        color: Colors.teal,
        isHealthy: true,
        statusLabel: 'Operational',
      ),
      statsWidgets: [
        _buildStatCard('Total Agents', '${s['totalAgents'] ?? 4}', Icons.smart_toy, AppConstants.primaryColor),
        _buildStatCard('iOS', _agentStatusLabel(s, 'iosStatus'), Icons.phone_iphone, AppConstants.infoColor),
        _buildStatCard('Web', _agentStatusLabel(s, 'webStatus'), Icons.web, AppConstants.successColor),
        _buildStatCard('Desktop', _agentStatusLabel(s, 'desktopStatus'), Icons.computer, AppConstants.warningColor),
      ],
      features: [
        'Agent D — SwiftUI iOS App Generator',
        'Agent E — React PWA Web Generator',
        'Agent F — Desktop App Generator (Tauri/Electron)',
        'Agent G — Play Store & App Store Publisher',
      ],
      featuresEnabled: [
        caps['ios'] != null,
        caps['web'] != null,
        caps['desktop'] != null,
        caps['publishing'] != null,
      ].map((v) => v as bool? ?? true).toList(),
    );
  }

  // ─────────────────────── Phase 8: Security ───────────────────────────────

  Widget _buildPhase8Tab() {
    final s = _phase8Summary ?? {};
    final status = '${s['status'] ?? 'unknown'}';
    final isOk = status == 'operational';

    return _buildPhaseScrollView(
      header: _PhaseHeader(
        phaseNumber: 8,
        title: 'Security & Compliance',
        subtitle: 'OWASP Scanning · GDPR Compliance · Privacy Analysis',
        icon: Icons.security,
        color: Colors.red,
        isHealthy: isOk,
        statusLabel: isOk ? 'Operational' : 'Partial',
      ),
      statsWidgets: [
        _buildStatCard('Total Agents', '${s['agentCount'] ?? 3}', Icons.smart_toy, AppConstants.primaryColor),
        _buildStatCard('Alpha', _boolStatus(s['alphaAvailable']), Icons.shield, Colors.red.value),
        _buildStatCard('Beta', _boolStatus(s['betaAvailable']), Icons.rule, Colors.orange.value),
        _buildStatCard('Gamma', _boolStatus(s['gammaAvailable']), Icons.privacy_tip, Colors.deepOrange.value),
      ],
      features: (s['capabilities'] as List<dynamic>?)?.cast<String>() ?? [
        'OWASP Top 10 vulnerability scanning',
        'GDPR / CCPA compliance validation',
        'Privacy data flow analysis',
        'Static code security analysis',
      ],
      featuresEnabled: List.filled(4, isOk),
    );
  }

  // ─────────────────────── Phase 9: Cost Intelligence ──────────────────────

  Widget _buildPhase9Tab() {
    final s = _phase9Summary ?? {};
    final status = '${s['status'] ?? 'unknown'}';
    final isOk = status == 'operational';

    return _buildPhaseScrollView(
      header: _PhaseHeader(
        phaseNumber: 9,
        title: 'Cost Intelligence',
        subtitle: 'Cost Tracking · Optimization · Budget Forecasting',
        icon: Icons.attach_money,
        color: Colors.green,
        isHealthy: isOk,
        statusLabel: isOk ? 'Operational' : 'Partial',
      ),
      statsWidgets: [
        _buildStatCard('Total Agents', '${s['agentCount'] ?? 3}', Icons.smart_toy, AppConstants.primaryColor),
        _buildStatCard('Delta', _boolStatus(s['deltaAvailable']), Icons.bar_chart, Colors.green.value),
        _buildStatCard('Epsilon', _boolStatus(s['epsilonAvailable']), Icons.speed, Colors.teal.value),
        _buildStatCard('Zeta', _boolStatus(s['zetaAvailable']), Icons.account_balance, Colors.cyan.value),
      ],
      features: (s['capabilities'] as List<dynamic>?)?.cast<String>() ?? [
        'Real-time cost tracking across cloud providers',
        'AI usage and token cost analysis',
        'Resource utilization optimization',
        'Budget forecasting and scenario planning',
      ],
      featuresEnabled: List.filled(4, isOk),
    );
  }

  // ─────────────────────── Phase 10: Self-Improvement ──────────────────────

  Widget _buildPhase10Tab() {
    final s = _phase10Summary ?? {};
    final status = '${s['status'] ?? 'unknown'}';
    final isOk = status == 'operational';

    return _buildPhaseScrollView(
      header: _PhaseHeader(
        phaseNumber: 10,
        title: 'Self-Improvement & Evolution',
        subtitle: 'Agent Evolution · Pattern Learning · Knowledge Management',
        icon: Icons.auto_awesome,
        color: Colors.purple,
        isHealthy: isOk,
        statusLabel: isOk ? 'Operational' : 'Partial',
      ),
      statsWidgets: [
        _buildStatCard('Total Agents', '${s['agentCount'] ?? 4}', Icons.smart_toy, AppConstants.primaryColor),
        _buildStatCard('Eta', _boolStatus(s['etaAvailable']), Icons.psychology, Colors.purple.value),
        _buildStatCard('Theta', _boolStatus(s['thetaAvailable']), Icons.school, Colors.deepPurple.value),
        _buildStatCard('Iota+Kappa', _boolStatus(s['iotaAvailable'] == true && s['kappaAvailable'] == true), Icons.hub, Colors.indigo.value),
      ],
      features: (s['capabilities'] as List<dynamic>?)?.cast<String>() ?? [
        'Agent performance evolution and self-tuning',
        'Pattern learning from historical decisions',
        'Knowledge base management and consolidation',
        'Consensus mechanism evolution via A/B testing',
      ],
      featuresEnabled: List.filled(4, isOk),
    );
  }

  // ─────────────────────── Shared Widgets ──────────────────────────────────

  Widget _buildPhaseScrollView({
    required _PhaseHeader header,
    required List<Widget> statsWidgets,
    required List<String> features,
    required List<bool> featuresEnabled,
  }) {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            header,
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1.5,
              children: statsWidgets,
            ),
            const SizedBox(height: 20),
            const Text(
              'Features',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            ...List.generate(features.length, (i) {
              final enabled = i < featuresEnabled.length ? featuresEnabled[i] : true;
              return ListTile(
                dense: true,
                leading: Icon(
                  enabled ? Icons.check_circle : Icons.circle_outlined,
                  color: enabled ? Colors.green : Colors.grey,
                  size: 20,
                ),
                title: Text(features[i]),
                trailing: Text(
                  enabled ? 'Active' : 'Inactive',
                  style: TextStyle(
                    fontSize: 11,
                    color: enabled ? Colors.green : Colors.grey,
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, int color) {
    return Card(
      elevation: 0,
      color: Color(color).withOpacity(0.08),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: Color(color), size: 20),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            Text(title, style: const TextStyle(fontSize: 10, color: Colors.grey)),
          ],
        ),
      ),
    );
  }

  // ─────────────────────── Helper Utilities ─────────────────────────────────

  String _nestedStr(Map<String, dynamic> map, List<String> keys, String fallback) {
    dynamic cur = map;
    for (final k in keys) {
      if (cur is Map) {
        cur = cur[k];
      } else {
        return fallback;
      }
    }
    return cur?.toString() ?? fallback;
  }

  String _boolStatus(dynamic available) {
    if (available == true) return 'Ready';
    if (available == false) return 'N/A';
    return '—';
  }

  String _agentStatusLabel(Map<String, dynamic> summary, String key) {
    final v = summary[key];
    if (v == null) return 'N/A';
    return v.toString();
  }
}

// ─────────────────────── Phase Header Widget ──────────────────────────────

class _PhaseHeader extends StatelessWidget {
  final int phaseNumber;
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final bool isHealthy;
  final String statusLabel;

  const _PhaseHeader({
    required this.phaseNumber,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.isHealthy,
    required this.statusLabel,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  isHealthy ? Icons.check_circle : Icons.warning,
                  color: isHealthy ? Colors.green : Colors.orange,
                  size: 28,
                ),
                const SizedBox(width: 8),
                Icon(icon, color: color, size: 24),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Phase $phaseNumber: $title',
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        subtitle,
                        style: const TextStyle(fontSize: 11, color: Colors.grey),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Chip(
              label: Text(statusLabel),
              backgroundColor: (isHealthy ? Colors.green : Colors.orange).withOpacity(0.12),
              labelStyle: TextStyle(color: isHealthy ? Colors.green : Colors.orange),
            ),
          ],
        ),
      ),
    );
  }
}
