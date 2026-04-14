import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class ResilienceScreen extends StatefulWidget {
  const ResilienceScreen({Key? key}) : super(key: key);

  @override
  State<ResilienceScreen> createState() => _ResilienceScreenState();
}

class _ResilienceScreenState extends State<ResilienceScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _health;
  List<dynamic> _circuitBreakers = [];
  Map<String, dynamic>? _failoverStats;
  Map<String, dynamic>? _selfHealing;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
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
      _apiService.get<Map<String, dynamic>>(Environment.resilienceHealth),
      _apiService.get<List<dynamic>>(Environment.resilienceCircuitBreakers),
      _apiService
          .get<Map<String, dynamic>>(Environment.resilienceFailoverStats),
      _apiService.get<Map<String, dynamic>>(Environment.selfHealingHealth),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) {
        _health = results[0].data as Map<String, dynamic>?;
      }
      if (results[1].success) {
        _circuitBreakers = (results[1].data as List<dynamic>?) ?? [];
      }
      if (results[2].success) {
        _failoverStats = results[2].data as Map<String, dynamic>?;
      }
      if (results[3].success) {
        _selfHealing = results[3].data as Map<String, dynamic>?;
      }
      if (!results[0].success) {
        _error = results[0].error ?? 'রেজিলিয়েন্স তথ্য লোড করা যায়নি';
      }
    });
  }

  Future<void> _resetCircuitBreaker(String name) async {
    final response = await _apiService.post<Map<String, dynamic>>(
      '${Environment.resilienceCircuitBreakers}/$name/reset',
    );
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content:
          Text(response.success ? 'সার্কিট ব্রেকার রিসেট হয়েছে' : 'ত্রুটি'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
    if (response.success) _loadAll();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('রেজিলিয়েন্স ও আত্মরক্ষা'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.health_and_safety), text: 'স্বাস্থ্য'),
            Tab(icon: Icon(Icons.electric_bolt), text: 'সার্কিট ব্রেকার'),
            Tab(icon: Icon(Icons.healing), text: 'আত্মমেরামত'),
          ],
        ),
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh),
              tooltip: 'রিফ্রেশ',
              onPressed: _loadAll)
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
                      Text(_error!),
                      ElevatedButton(
                          onPressed: _loadAll,
                          child: const Text('আবার চেষ্টা করুন')),
                    ]))
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildHealthTab(),
                    _buildCircuitBreakersTab(),
                    _buildSelfHealingTab()
                  ],
                ),
    );
  }

  Widget _buildHealthTab() {
    final h = _health ?? {};
    final status = '${h['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final isHealthy = status == 'UP' || status == 'HEALTHY';
    final fs = _failoverStats ?? {};

    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppConstants.paddingLarge),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      Icon(isHealthy ? Icons.check_circle : Icons.warning,
                          color: isHealthy ? Colors.green : Colors.orange,
                          size: 32),
                      const SizedBox(width: 12),
                      Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('সিস্টেম রেজিলিয়েন্স',
                                style: TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.bold)),
                            Text(
                                isHealthy
                                    ? 'সব ঠিক আছে'
                                    : 'সমস্যা সনাক্ত হয়েছে',
                                style: TextStyle(
                                    color: isHealthy
                                        ? Colors.green
                                        : Colors.orange)),
                          ]),
                    ]),
                    const Text(
                        '(সিস্টেম কোনো সমস্যায় পড়লে নিজে নিজে সামলানোর ক্ষমতা)',
                        style: TextStyle(fontSize: 12, color: Colors.grey)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              children: [
                _buildStatCard('সক্রিয় ব্রেকার', '${_circuitBreakers.length}',
                    Icons.electric_bolt, AppConstants.warningColor),
                _buildStatCard('ফেইলওভার', '${fs['totalFailovers'] ?? 0}',
                    Icons.swap_horiz, AppConstants.infoColor),
                _buildStatCard(
                    'সফল পুনরুদ্ধার',
                    '${fs['successfulRecoveries'] ?? 0}',
                    Icons.restore,
                    AppConstants.successColor),
                _buildStatCard(
                    'স্ট্যাটাস',
                    status,
                    Icons.monitor_heart,
                    isHealthy
                        ? AppConstants.successColor
                        : AppConstants.errorColor),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCircuitBreakersTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _circuitBreakers.isEmpty
          ? const Center(
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                  Icon(Icons.electric_bolt, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('কোনো সার্কিট ব্রেকার নেই'),
                  Text('(সার্কিট ব্রেকার সিস্টেমকে ওভারলোড থেকে রক্ষা করে)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                ]))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _circuitBreakers.length,
              itemBuilder: (ctx, i) {
                final cb = _circuitBreakers[i] is Map<String, dynamic>
                    ? _circuitBreakers[i] as Map<String, dynamic>
                    : <String, dynamic>{};
                final name = '${cb['name'] ?? 'Unknown'}';
                final state = '${cb['state'] ?? 'CLOSED'}'.toUpperCase();
                final failureCount = cb['failureCount'] ?? 0;

                Color stateColor;
                IconData stateIcon;
                String stateText;
                switch (state) {
                  case 'OPEN':
                    stateColor = Colors.red;
                    stateIcon = Icons.error;
                    stateText = 'খোলা (সমস্যা!)';
                    break;
                  case 'HALF_OPEN':
                    stateColor = Colors.orange;
                    stateIcon = Icons.warning;
                    stateText = 'আংশিক (পরীক্ষা চলছে)';
                    break;
                  default:
                    stateColor = Colors.green;
                    stateIcon = Icons.check_circle;
                    stateText = 'বন্ধ (স্বাভাবিক)';
                }

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: stateColor.withValues(alpha: 0.1),
                      child: Icon(stateIcon, color: stateColor),
                    ),
                    title: Text(name),
                    subtitle: Text(
                        'অবস্থা: $stateText\nব্যর্থতা: $failureCount',
                        style: const TextStyle(fontSize: 12)),
                    isThreeLine: true,
                    trailing: state != 'CLOSED'
                        ? IconButton(
                            icon: const Icon(Icons.restart_alt,
                                color: Colors.blue),
                            tooltip: 'রিসেট করুন',
                            onPressed: () => _resetCircuitBreaker(name),
                          )
                        : null,
                  ),
                );
              },
            ),
    );
  }

  Widget _buildSelfHealingTab() {
    final sh = _selfHealing ?? {};
    final status = '${sh['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final services = sh['services'] is List ? sh['services'] as List : [];

    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppConstants.paddingLarge),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(children: [
                      Icon(Icons.healing,
                          color: Color(AppConstants.primaryColor)),
                      SizedBox(width: 8),
                      Text('আত্মমেরামত ব্যবস্থা',
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    const Text('(সিস্টেম নিজে নিজে সমস্যা ঠিক করার চেষ্টা করে)',
                        style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    Chip(
                      label: Text('অবস্থা: $status'),
                      backgroundColor: (status == 'ACTIVE' || status == 'UP'
                              ? Colors.green
                              : Colors.orange)
                          .withValues(alpha: 0.1),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            if (services.isNotEmpty) ...[
              const Text('সার্ভিস অবস্থা:',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const Text('(প্রতিটি সার্ভিস ঠিক আছে কিনা)',
                  style: TextStyle(fontSize: 12, color: Colors.grey)),
              const SizedBox(height: 8),
              ...services.map((s) {
                final svc = s is Map<String, dynamic> ? s : <String, dynamic>{};
                final sName = '${svc['name'] ?? 'Unknown'}';
                final sStatus = '${svc['status'] ?? ''}'.toUpperCase();
                final isUp = sStatus == 'UP' || sStatus == 'HEALTHY';
                return ListTile(
                  leading: Icon(isUp ? Icons.check_circle : Icons.error,
                      color: isUp ? Colors.green : Colors.red),
                  title: Text(sName),
                  trailing: Chip(
                      label: Text(isUp ? 'চালু' : 'সমস্যা',
                          style: const TextStyle(fontSize: 11))),
                );
              }),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, int color) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [
            Color(color).withValues(alpha: 0.1),
            Color(color).withValues(alpha: 0.05)
          ]),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: Color(color), size: 22),
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(value,
                    style: const TextStyle(
                        fontSize: 20, fontWeight: FontWeight.bold)),
                Text(title,
                    style: const TextStyle(fontSize: 11, color: Colors.grey)),
              ]),
            ],
          ),
        ),
      ),
    );
  }
}
