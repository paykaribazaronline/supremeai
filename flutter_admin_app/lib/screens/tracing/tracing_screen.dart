import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class TracingScreen extends StatefulWidget {
  const TracingScreen({Key? key}) : super(key: key);

  @override
  State<TracingScreen> createState() => _TracingScreenState();
}

class _TracingScreenState extends State<TracingScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _stats;
  List<dynamic> _recentTraces = [];
  List<dynamic> _errorTraces = [];
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
      _apiService.get<Map<String, dynamic>>(Environment.tracingStats),
      _apiService.get<List<dynamic>>(Environment.tracingRecent),
      _apiService.get<List<dynamic>>(Environment.tracingErrors),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) _stats = results[0].data as Map<String, dynamic>?;
      if (results[1].success) {
        _recentTraces = (results[1].data as List<dynamic>?) ?? [];
      }
      if (results[2].success) {
        _errorTraces = (results[2].data as List<dynamic>?) ?? [];
      }
      if (!results[0].success) {
        _error = results[0].error ?? 'ট্রেসিং তথ্য লোড করা যায়নি';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('ট্রেসিং'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.bar_chart), text: 'পরিসংখ্যান'),
            Tab(icon: Icon(Icons.list_alt), text: 'সাম্প্রতিক'),
            Tab(icon: Icon(Icons.error_outline), text: 'ত্রুটি'),
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
                      const Icon(Icons.route, size: 48, color: Colors.grey),
                      Text(_error!),
                      ElevatedButton(
                          onPressed: _loadAll,
                          child: const Text('আবার চেষ্টা করুন')),
                    ]))
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildStatsTab(),
                    _buildRecentTab(),
                    _buildErrorsTab()
                  ],
                ),
    );
  }

  Widget _buildStatsTab() {
    final s = _stats ?? {};
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
                      Icon(Icons.route,
                          color: Color(AppConstants.primaryColor)),
                      SizedBox(width: 8),
                      Text('ট্রেসিং পরিসংখ্যান',
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    Text(
                        '(প্রতিটি API কল কতটা সময় নিচ্ছে ও কোথায় সমস্যা হচ্ছে)',
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
                _buildStatCard('মোট ট্রেস', '${s['totalTraces'] ?? 0}',
                    Icons.timeline, AppConstants.primaryColor),
                _buildStatCard('ত্রুটি ট্রেস', '${s['errorTraces'] ?? 0}',
                    Icons.error, AppConstants.errorColor),
                _buildStatCard('গড় সময়', '${s['avgDuration'] ?? 0}ms',
                    Icons.speed, AppConstants.infoColor),
                _buildStatCard('সক্রিয় পাথ', '${s['activePaths'] ?? 0}',
                    Icons.route, AppConstants.successColor),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _recentTraces.isEmpty
          ? const Center(
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                  Icon(Icons.list_alt, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('কোনো সাম্প্রতিক ট্রেস নেই'),
                  Text('(API কল হলে এখানে দেখা যাবে)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                ]))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _recentTraces.length,
              itemBuilder: (ctx, i) => _buildTraceCard(_recentTraces[i], false),
            ),
    );
  }

  Widget _buildErrorsTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _errorTraces.isEmpty
          ? const Center(
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                  Icon(Icons.check_circle, size: 64, color: Colors.green),
                  SizedBox(height: 16),
                  Text('কোনো ত্রুটি ট্রেস নেই!'),
                  Text('(ভালো খবর - কোনো সমস্যা নেই)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                ]))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _errorTraces.length,
              itemBuilder: (ctx, i) => _buildTraceCard(_errorTraces[i], true),
            ),
    );
  }

  Widget _buildTraceCard(dynamic trace, bool isError) {
    final map = trace is Map<String, dynamic> ? trace : <String, dynamic>{};
    final traceId = '${map['traceId'] ?? map['id'] ?? ''}'.length > 12
        ? '${map['traceId'] ?? map['id'] ?? ''}'.substring(0, 12)
        : '${map['traceId'] ?? map['id'] ?? ''}';
    final path = '${map['path'] ?? map['endpoint'] ?? 'Unknown'}';
    final duration = '${map['duration'] ?? 0}ms';
    final status = '${map['status'] ?? map['statusCode'] ?? ''}'.toUpperCase();
    final timestamp = '${map['timestamp'] ?? ''}';
    final method = '${map['method'] ?? 'GET'}'.toUpperCase();

    final isSuccess =
        status.startsWith('2') || status == 'SUCCESS' || status == 'OK';

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ExpansionTile(
        leading: CircleAvatar(
          backgroundColor: (isError || !isSuccess ? Colors.red : Colors.green)
              .withValues(alpha: 0.1),
          child: Icon(isError || !isSuccess ? Icons.error : Icons.check,
              color: isError || !isSuccess ? Colors.red : Colors.green,
              size: 18),
        ),
        title: Row(children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(method,
                style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Colors.blue)),
          ),
          const SizedBox(width: 8),
          Expanded(
              child: Text(path,
                  style: const TextStyle(fontSize: 13),
                  overflow: TextOverflow.ellipsis)),
        ]),
        subtitle:
            Text('$duration • $traceId', style: const TextStyle(fontSize: 11)),
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildDetailRow(
                    'ট্রেস ID', '${map['traceId'] ?? map['id'] ?? 'N/A'}'),
                _buildDetailRow('পাথ', path),
                _buildDetailRow('মেথড', method),
                _buildDetailRow('স্ট্যাটাস', status),
                _buildDetailRow('সময়', duration),
                _buildDetailRow('টাইমস্ট্যাম্প', timestamp),
                if (map['error'] != null)
                  _buildDetailRow('ত্রুটি', '${map['error']}'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
              width: 100,
              child: Text('$label:',
                  style: const TextStyle(
                      fontWeight: FontWeight.w600, fontSize: 12))),
          Expanded(
              child: Text(value,
                  style: TextStyle(
                      fontSize: 12,
                      fontFamily: label.contains('ID') ? 'monospace' : null))),
        ],
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
