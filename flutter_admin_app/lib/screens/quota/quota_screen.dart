import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class QuotaScreen extends StatefulWidget {
  const QuotaScreen({Key? key}) : super(key: key);

  @override
  State<QuotaScreen> createState() => _QuotaScreenState();
}

class _QuotaScreenState extends State<QuotaScreen> {
  final ApiService _apiService = ApiService();
  Map<String, dynamic>? _summary;
  Map<String, dynamic>? _health;
  List<dynamic> _providers = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadAll();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>(Environment.quotaSummary),
      _apiService.get<Map<String, dynamic>>(Environment.quotasHealth),
      _apiService.get<List<dynamic>>(Environment.quotasProviders),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) {
        _summary = results[0].data as Map<String, dynamic>?;
      }
      if (results[1].success) {
        _health = results[1].data as Map<String, dynamic>?;
      }
      if (results[2].success) {
        _providers = (results[2].data as List<dynamic>?) ?? [];
      }
      if (!results[0].success) {
        _error = results[0].error ?? 'কোটা তথ্য লোড করা যায়নি';
      }
    });
  }

  Future<void> _resetQuotas() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('কোটা রিসেট'),
        content: const Text(
            'সব কোটা রিসেট হবে। আপনি কি নিশ্চিত?\n(মাসিক ব্যবহার শূন্য থেকে শুরু হবে)'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('না')),
          ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('হ্যাঁ, রিসেট করুন')),
        ],
      ),
    );
    if (confirm == true) {
      final response =
          await _apiService.post<Map<String, dynamic>>(Environment.quotaReset);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(response.success ? 'কোটা রিসেট হয়েছে!' : 'ত্রুটি'),
        backgroundColor: Color(response.success
            ? AppConstants.successColor
            : AppConstants.errorColor),
      ));
      if (response.success) _loadAll();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('কোটা ব্যবস্থাপনা'),
        actions: [
          IconButton(
              icon: const Icon(Icons.restart_alt),
              tooltip: 'কোটা রিসেট',
              onPressed: _resetQuotas),
          IconButton(
              icon: const Icon(Icons.refresh),
              tooltip: 'রিফ্রেশ',
              onPressed: _loadAll),
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
                      const SizedBox(height: 16),
                      ElevatedButton(
                          onPressed: _loadAll,
                          child: const Text('আবার চেষ্টা করুন')),
                    ]))
              : RefreshIndicator(
                  onRefresh: _loadAll,
                  child: SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(AppConstants.paddingLarge),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildSummarySection(),
                        const SizedBox(height: 20),
                        _buildHealthSection(),
                        const SizedBox(height: 20),
                        _buildProvidersSection(),
                      ],
                    ),
                  ),
                ),
    );
  }

  Widget _buildSummarySection() {
    final s = _summary ?? {};
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(children: [
              Icon(Icons.pie_chart, color: Color(AppConstants.primaryColor)),
              SizedBox(width: 8),
              Text('কোটা সারসংক্ষেপ',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text(
                '(প্রতিটি AI প্রোভাইডার কতটুকু ব্যবহার হয়েছে / বাকি আছে)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: 1.5,
              children: [
                _buildStatCard('মোট অনুরোধ', '${s['totalRequests'] ?? 0}',
                    Icons.send, AppConstants.primaryColor),
                _buildStatCard('ব্যবহৃত', '${s['usedQuota'] ?? 0}',
                    Icons.data_usage, AppConstants.warningColor),
                _buildStatCard('বাকি আছে', '${s['remainingQuota'] ?? 0}',
                    Icons.battery_charging_full, AppConstants.successColor),
                _buildStatCard('সীমা', '${s['totalLimit'] ?? 0}', Icons.speed,
                    AppConstants.infoColor),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHealthSection() {
    final h = _health ?? {};
    final status = '${h['status'] ?? 'UNKNOWN'}'.toUpperCase();
    final isHealthy = status == 'HEALTHY' || status == 'UP';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Icon(isHealthy ? Icons.check_circle : Icons.warning,
                  color: isHealthy ? Colors.green : Colors.orange),
              const SizedBox(width: 8),
              const Text('কোটা স্বাস্থ্য',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text('(কোটা সিস্টেম ঠিকমতো কাজ করছে কিনা)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            Chip(
              avatar: Icon(isHealthy ? Icons.check : Icons.close,
                  color: isHealthy ? Colors.green : Colors.red, size: 18),
              label: Text(isHealthy ? 'স্বাভাবিক' : 'সমস্যা আছে'),
              backgroundColor:
                  (isHealthy ? Colors.green : Colors.red).withOpacity(0.1),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildProvidersSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('প্রোভাইডার কোটা',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Text('(কোন AI কতটুকু ব্যবহার হয়েছে)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            if (_providers.isEmpty)
              const Center(
                  child: Padding(
                      padding: EdgeInsets.all(24),
                      child: Text('কোনো প্রোভাইডার নেই')))
            else
              ..._providers.map((p) {
                final map = p is Map<String, dynamic> ? p : <String, dynamic>{};
                final name = '${map['name'] ?? map['providerId'] ?? 'Unknown'}';
                final used = map['used'] ?? map['requestCount'] ?? 0;
                final limit = map['limit'] ?? map['monthlyLimit'] ?? 100;
                final pct = limit > 0 ? (used / limit).clamp(0.0, 1.0) : 0.0;

                return Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.w600)),
                            Text('$used / $limit',
                                style: const TextStyle(
                                    fontSize: 12, color: Colors.grey)),
                          ]),
                      const SizedBox(height: 4),
                      LinearProgressIndicator(
                        value: pct.toDouble(),
                        backgroundColor: Colors.grey.shade200,
                        valueColor: AlwaysStoppedAnimation(pct > 0.9
                            ? Colors.red
                            : pct > 0.7
                                ? Colors.orange
                                : Colors.green),
                      ),
                    ],
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
            Text(value,
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            Text(title,
                style: const TextStyle(fontSize: 10, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
