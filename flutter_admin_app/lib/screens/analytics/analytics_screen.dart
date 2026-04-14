import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class AnalyticsScreen extends StatefulWidget {
  const AnalyticsScreen({Key? key}) : super(key: key);

  @override
  State<AnalyticsScreen> createState() => _AnalyticsScreenState();
}

class _AnalyticsScreenState extends State<AnalyticsScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _trend;
  List<dynamic> _daily = [];
  List<dynamic> _monthly = [];
  bool _isLoading = true;
  bool _isExporting = false;
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
      _apiService.get<Map<String, dynamic>>(Environment.analyticsTrend),
      _apiService.get<List<dynamic>>(Environment.analyticsDaily),
      _apiService.get<List<dynamic>>(Environment.analyticsMonthly),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) _trend = results[0].data as Map<String, dynamic>?;
      if (results[1].success) {
        _daily = (results[1].data as List<dynamic>?) ?? [];
      }
      if (results[2].success) {
        _monthly = (results[2].data as List<dynamic>?) ?? [];
      }
      if (!results[0].success) {
        _error = results[0].error ?? 'অ্যানালিটিক্স লোড করা যায়নি';
      }
    });
  }

  Future<void> _exportCsv() async {
    setState(() => _isExporting = true);
    final response =
        await _apiService.get<dynamic>(Environment.analyticsExportCsv);
    if (!mounted) return;
    setState(() => _isExporting = false);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'CSV এক্সপোর্ট সম্পন্ন!'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('অ্যানালিটিক্স'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.trending_up), text: 'ট্রেন্ড'),
            Tab(icon: Icon(Icons.today), text: 'দৈনিক'),
            Tab(icon: Icon(Icons.calendar_month), text: 'মাসিক'),
          ],
        ),
        actions: [
          IconButton(
            icon: _isExporting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.download),
            tooltip: 'CSV এক্সপোর্ট',
            onPressed: _isExporting ? null : _exportCsv,
          ),
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
                      const Icon(Icons.analytics, size: 48, color: Colors.grey),
                      Text(_error!),
                      ElevatedButton(
                          onPressed: _loadAll,
                          child: const Text('আবার চেষ্টা করুন')),
                    ]))
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildTrendTab(),
                    _buildDailyTab(),
                    _buildMonthlyTab()
                  ],
                ),
    );
  }

  Widget _buildTrendTab() {
    final t = _trend ?? {};
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          children: [
            const Card(
              child: Padding(
                padding: EdgeInsets.all(AppConstants.paddingLarge),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      Icon(Icons.trending_up,
                          color: Color(AppConstants.primaryColor)),
                      SizedBox(width: 8),
                      Text('সিস্টেম ট্রেন্ড',
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    Text('(সিস্টেমের কর্মক্ষমতা কিভাবে পরিবর্তন হচ্ছে)',
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
                _buildStatCard('মোট অনুরোধ', '${t['totalRequests'] ?? 0}',
                    Icons.send, AppConstants.primaryColor),
                _buildStatCard('সফলতার হার', '${t['successRate'] ?? 0}%',
                    Icons.check_circle, AppConstants.successColor),
                _buildStatCard('গড় সময়', '${t['avgResponseTime'] ?? 0}ms',
                    Icons.speed, AppConstants.infoColor),
                _buildStatCard('ত্রুটির হার', '${t['errorRate'] ?? 0}%',
                    Icons.error, AppConstants.errorColor),
              ],
            ),
            if (t['trends'] is List) ...[
              const SizedBox(height: 20),
              const Text('ট্রেন্ড বিবরণ:',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ...(t['trends'] as List).map((tr) {
                final m = tr is Map<String, dynamic> ? tr : <String, dynamic>{};
                return ListTile(
                  dense: true,
                  leading: Icon(
                    '${m['direction']}'.toLowerCase() == 'up'
                        ? Icons.arrow_upward
                        : Icons.arrow_downward,
                    color: '${m['direction']}'.toLowerCase() == 'up'
                        ? Colors.green
                        : Colors.red,
                    size: 18,
                  ),
                  title: Text('${m['metric'] ?? ''}',
                      style: const TextStyle(fontSize: 13)),
                  trailing: Text('${m['change'] ?? ''}',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                );
              }),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDailyTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _daily.isEmpty
          ? const Center(child: Text('কোনো দৈনিক ডাটা নেই'))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _daily.length,
              itemBuilder: (ctx, i) => _buildDataCard(_daily[i], 'দৈনিক'),
            ),
    );
  }

  Widget _buildMonthlyTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _monthly.isEmpty
          ? const Center(child: Text('কোনো মাসিক ডাটা নেই'))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _monthly.length,
              itemBuilder: (ctx, i) => _buildDataCard(_monthly[i], 'মাসিক'),
            ),
    );
  }

  Widget _buildDataCard(dynamic item, String type) {
    final map = item is Map<String, dynamic> ? item : <String, dynamic>{};
    final date = '${map['date'] ?? map['period'] ?? ''}';
    final requests = map['requests'] ?? map['totalRequests'] ?? 0;
    final errors = map['errors'] ?? map['totalErrors'] ?? 0;
    final successRate = map['successRate'] ?? 0;

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor:
              const Color(AppConstants.primaryColor).withOpacity(0.1),
          child: const Icon(Icons.calendar_today,
              color: Color(AppConstants.primaryColor), size: 18),
        ),
        title: Text(date),
        subtitle: Text(
            'অনুরোধ: $requests | ত্রুটি: $errors | সফলতা: $successRate%',
            style: const TextStyle(fontSize: 12)),
        trailing: CircularProgressIndicator(
          value: (successRate is num ? successRate / 100 : 0)
              .toDouble()
              .clamp(0.0, 1.0),
          strokeWidth: 3,
          backgroundColor: Colors.grey.shade200,
          valueColor:
              const AlwaysStoppedAnimation(Color(AppConstants.successColor)),
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, int color) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [
            Color(color).withOpacity(0.1),
            Color(color).withOpacity(0.05)
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
