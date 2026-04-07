import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class PhasesScreen extends StatefulWidget {
  const PhasesScreen({Key? key}) : super(key: key);

  @override
  State<PhasesScreen> createState() => _PhasesScreenState();
}

class _PhasesScreenState extends State<PhasesScreen> with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _phase6Health;
  List<dynamic> _phase6Features = [];
  Map<String, dynamic>? _phase6Metrics;
  Map<String, dynamic>? _phase7Summary;
  List<dynamic> _phase7Capabilities = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() { _isLoading = true; _error = null; });
    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>(Environment.phase6Health),
      _apiService.get<List<dynamic>>(Environment.phase6Features),
      _apiService.get<Map<String, dynamic>>(Environment.phase6Metrics),
      _apiService.get<Map<String, dynamic>>(Environment.phase7Summary),
      _apiService.get<List<dynamic>>(Environment.phase7Capabilities),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) _phase6Health = results[0].data as Map<String, dynamic>?;
      if (results[1].success) _phase6Features = (results[1].data as List<dynamic>?) ?? [];
      if (results[2].success) _phase6Metrics = results[2].data as Map<String, dynamic>?;
      if (results[3].success) _phase7Summary = results[3].data as Map<String, dynamic>?;
      if (results[4].success) _phase7Capabilities = (results[4].data as List<dynamic>?) ?? [];
      if (!results[0].success && !results[3].success) _error = 'ফেজ তথ্য লোড করা যায়নি';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ফেজ ৬ ও ৭'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.integration_instructions), text: 'ফেজ ৬'),
            Tab(icon: Icon(Icons.devices), text: 'ফেজ ৭'),
          ],
        ),
        actions: [IconButton(icon: const Icon(Icons.refresh), tooltip: 'রিফ্রেশ', onPressed: _loadAll)],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  const Icon(Icons.layers, size: 48, color: Colors.grey),
                  Text(_error!),
                  ElevatedButton(onPressed: _loadAll, child: const Text('আবার চেষ্টা করুন')),
                ]))
              : TabBarView(
                  controller: _tabController,
                  children: [_buildPhase6Tab(), _buildPhase7Tab()],
                ),
    );
  }

  Widget _buildPhase6Tab() {
    final h = _phase6Health ?? {};
    final m = _phase6Metrics ?? {};
    final status = '${h['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final isHealthy = status == 'UP' || status == 'HEALTHY';

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
                    Row(children: [
                      Icon(isHealthy ? Icons.check_circle : Icons.warning,
                          color: isHealthy ? Colors.green : Colors.orange, size: 28),
                      const SizedBox(width: 8),
                      const Text('ফেজ ৬: ইন্টিগ্রেশন', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    const Text('(সব সিস্টেম একসাথে কাজ করার জন্য সংযুক্ত)', style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 12),
                    Chip(
                      label: Text(isHealthy ? 'সক্রিয়' : 'সমস্যা'),
                      backgroundColor: (isHealthy ? Colors.green : Colors.orange).withOpacity(0.1),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            if (m.isNotEmpty)
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 8,
                crossAxisSpacing: 8,
                childAspectRatio: 1.5,
                children: [
                  _buildStatCard('ওয়ার্কফ্লো', '${m['workflows'] ?? 0}', Icons.account_tree, AppConstants.primaryColor),
                  _buildStatCard('সফলতা', '${m['successRate'] ?? 0}%', Icons.check, AppConstants.successColor),
                  _buildStatCard('গড় সময়', '${m['avgTime'] ?? 0}s', Icons.timer, AppConstants.infoColor),
                  _buildStatCard('ত্রুটি', '${m['errors'] ?? 0}', Icons.error, AppConstants.errorColor),
                ],
              ),
            const SizedBox(height: 20),
            const Text('ফিচার তালিকা:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const Text('(ফেজ ৬ এ কোন কোন ফিচার আছে)', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 8),
            if (_phase6Features.isEmpty)
              const Text('কোনো ফিচার তথ্য নেই', style: TextStyle(color: Colors.grey))
            else
              ..._phase6Features.map((f) {
                final fm = f is Map<String, dynamic> ? f : <String, dynamic>{};
                final name = '${fm['name'] ?? f}';
                final enabled = fm['enabled'] == true;
                return ListTile(
                  dense: true,
                  leading: Icon(enabled ? Icons.check_circle : Icons.circle_outlined,
                      color: enabled ? Colors.green : Colors.grey, size: 20),
                  title: Text(name),
                  trailing: Text(enabled ? 'সক্রিয়' : 'নিষ্ক্রিয়',
                      style: TextStyle(fontSize: 11, color: enabled ? Colors.green : Colors.grey)),
                );
              }),
          ],
        ),
      ),
    );
  }

  Widget _buildPhase7Tab() {
    final s = _phase7Summary ?? {};
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
                    Row(children: [
                      Icon(Icons.devices, color: Color(AppConstants.primaryColor), size: 28),
                      const SizedBox(width: 8),
                      const Text('ফেজ ৭: ক্লায়েন্ট জেনারেশন', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    const Text('(iOS, Web, Desktop অ্যাপ তৈরি ও প্রকাশ)', style: TextStyle(fontSize: 12, color: Colors.grey)),
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
              childAspectRatio: 1.5,
              children: [
                _buildStatCard('iOS', '${s['iosStatus'] ?? 'N/A'}', Icons.phone_iphone, AppConstants.primaryColor),
                _buildStatCard('Web', '${s['webStatus'] ?? 'N/A'}', Icons.web, AppConstants.infoColor),
                _buildStatCard('Desktop', '${s['desktopStatus'] ?? 'N/A'}', Icons.computer, AppConstants.successColor),
                _buildStatCard('Publish', '${s['publishStatus'] ?? 'N/A'}', Icons.publish, AppConstants.warningColor),
              ],
            ),
            const SizedBox(height: 20),
            const Text('সক্ষমতা:', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const Text('(কি কি ক্লায়েন্ট তৈরি করতে পারে)', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 8),
            if (_phase7Capabilities.isEmpty)
              const Text('কোনো সক্ষমতা তথ্য নেই', style: TextStyle(color: Colors.grey))
            else
              Wrap(spacing: 8, runSpacing: 8, children: [
                ..._phase7Capabilities.map((c) {
                  final cm = c is Map<String, dynamic> ? c : <String, dynamic>{};
                  final name = '${cm['name'] ?? c}';
                  return Chip(
                    avatar: const Icon(Icons.star, size: 16),
                    label: Text(name, style: const TextStyle(fontSize: 12)),
                  );
                }),
              ]),
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
            Text(value, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            Text(title, style: const TextStyle(fontSize: 10, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
