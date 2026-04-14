import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class AlertsLogsScreen extends StatefulWidget {
  const AlertsLogsScreen({Key? key}) : super(key: key);

  @override
  State<AlertsLogsScreen> createState() => _AlertsLogsScreenState();
}

class _AlertsLogsScreenState extends State<AlertsLogsScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  List<dynamic> _alerts = [];
  List<dynamic> _logs = [];
  Map<String, dynamic>? _alertStats;
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
      _apiService.get<List<dynamic>>(Environment.alertsList),
      _apiService.get<List<dynamic>>(Environment.executionLogs),
      _apiService.get<Map<String, dynamic>>(Environment.alertsStats),
    ]);

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (results[0].success) {
        _alerts = (results[0].data as List<dynamic>?) ?? [];
      }
      if (results[1].success) _logs = (results[1].data as List<dynamic>?) ?? [];
      if (results[2].success) {
        _alertStats = results[2].data as Map<String, dynamic>?;
      }
      if (!results[0].success && !results[1].success && !results[2].success) {
        _error = results[0].error ?? 'ডেটা লোড করা যায়নি';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Alerts & Logs'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: _isLoading ? null : _loadAll,
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.notifications_active), text: 'সতর্কতা'),
            Tab(icon: Icon(Icons.receipt_long), text: 'লগ'),
            Tab(icon: Icon(Icons.bar_chart), text: 'পরিসংখ্যান'),
          ],
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildErrorView()
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildAlertsTab(),
                    _buildLogsTab(),
                    _buildStatsTab(),
                  ],
                ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Colors.red),
          const SizedBox(height: AppConstants.paddingMedium),
          Text(_error!, textAlign: TextAlign.center),
          const SizedBox(height: AppConstants.paddingMedium),
          ElevatedButton(
              onPressed: _loadAll, child: const Text('আবার চেষ্টা করুন')),
        ],
      ),
    );
  }

  // ─── Alerts Tab ──────────────────────────────────────────

  Widget _buildAlertsTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _alerts.isEmpty
          ? _buildEmptyState(Icons.notifications_none, 'কোনো সতর্কতা নেই',
              'সব কিছু ঠিকঠাক চলছে!')
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _alerts.length + 1,
              itemBuilder: (context, index) {
                if (index == 0) {
                  return Padding(
                    padding: const EdgeInsets.only(
                        bottom: AppConstants.paddingMedium),
                    child: Text(
                      '(সিস্টেমে কোনো সমস্যা হলে এখানে দেখাবে)',
                      style: TextStyle(
                          fontSize: AppConstants.captionFontSize,
                          color: Colors.grey.shade600),
                    ),
                  );
                }
                return _buildAlertCard(_alerts[index - 1]);
              },
            ),
    );
  }

  Widget _buildAlertCard(dynamic alert) {
    final map = alert is Map<String, dynamic> ? alert : <String, dynamic>{};
    final severity =
        '${map['severity'] ?? map['level'] ?? 'INFO'}'.toUpperCase();
    final message =
        '${map['message'] ?? map['description'] ?? 'Unknown alert'}';
    final timestamp = '${map['timestamp'] ?? map['time'] ?? ''}';
    final category = '${map['category'] ?? map['type'] ?? ''}';

    Color severityColor;
    IconData severityIcon;
    String severityHint;
    switch (severity) {
      case 'CRITICAL':
      case 'ERROR':
        severityColor = const Color(AppConstants.errorColor);
        severityIcon = Icons.error;
        severityHint = 'জরুরি — এখনই সমাধান করুন';
        break;
      case 'WARNING':
      case 'WARN':
        severityColor = const Color(AppConstants.warningColor);
        severityIcon = Icons.warning;
        severityHint = 'সতর্কতা — শীঘ্রই দেখুন';
        break;
      default:
        severityColor = const Color(AppConstants.infoColor);
        severityIcon = Icons.info;
        severityHint = 'তথ্য — জানার জন্য';
    }

    return Card(
      margin: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
      child: ListTile(
        leading: Tooltip(
          message: severityHint,
          child: CircleAvatar(
            backgroundColor: severityColor.withValues(alpha: 0.1),
            child: Icon(severityIcon, color: severityColor),
          ),
        ),
        title:
            Text(message, style: const TextStyle(fontWeight: FontWeight.w500)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (category.isNotEmpty)
              Text('বিভাগ: $category (ক্যাটাগরি)',
                  style:
                      const TextStyle(fontSize: AppConstants.captionFontSize)),
            if (timestamp.isNotEmpty)
              Text('সময়: $timestamp',
                  style: TextStyle(
                      fontSize: AppConstants.captionFontSize,
                      color: Colors.grey.shade500)),
          ],
        ),
        trailing: Chip(
          label: Text(severity,
              style: TextStyle(
                  color: severityColor,
                  fontSize: 10,
                  fontWeight: FontWeight.bold)),
          backgroundColor: severityColor.withValues(alpha: 0.1),
          side: BorderSide.none,
        ),
      ),
    );
  }

  // ─── Logs Tab ──────────────────────────────────────────

  Widget _buildLogsTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _logs.isEmpty
          ? _buildEmptyState(Icons.receipt_long, 'কোনো লগ নেই',
              'এখনো কোনো কার্যক্রম রেকর্ড হয়নি।')
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _logs.length + 1,
              itemBuilder: (context, index) {
                if (index == 0) {
                  return Padding(
                    padding: const EdgeInsets.only(
                        bottom: AppConstants.paddingMedium),
                    child: Text(
                      '(সিস্টেমের প্রতিটি কাজের বিস্তারিত রেকর্ড)',
                      style: TextStyle(
                          fontSize: AppConstants.captionFontSize,
                          color: Colors.grey.shade600),
                    ),
                  );
                }
                return _buildLogCard(_logs[index - 1]);
              },
            ),
    );
  }

  Widget _buildLogCard(dynamic log) {
    final map = log is Map<String, dynamic> ? log : <String, dynamic>{};
    final action = '${map['action'] ?? map['type'] ?? 'Unknown'}';
    final details = '${map['details'] ?? map['message'] ?? ''}';
    final timestamp = '${map['timestamp'] ?? map['time'] ?? ''}';
    final status = '${map['status'] ?? 'completed'}'.toUpperCase();

    final isSuccess = status == 'SUCCESS' || status == 'COMPLETED';

    return Card(
      margin: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
      child: ExpansionTile(
        leading: Icon(
          isSuccess ? Icons.check_circle_outline : Icons.cancel_outlined,
          color: isSuccess
              ? const Color(AppConstants.successColor)
              : const Color(AppConstants.errorColor),
          size: 20,
        ),
        title: Text(action,
            style: const TextStyle(
                fontWeight: FontWeight.w500,
                fontSize: AppConstants.bodyFontSize)),
        subtitle: Text(
          timestamp.isNotEmpty ? timestamp : 'সময় অজানা',
          style: TextStyle(
              fontSize: AppConstants.captionFontSize,
              color: Colors.grey.shade500),
        ),
        trailing: Chip(
          label: Text(
            status,
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.bold,
              color: isSuccess
                  ? const Color(AppConstants.successColor)
                  : const Color(AppConstants.errorColor),
            ),
          ),
          backgroundColor: (isSuccess
                  ? const Color(AppConstants.successColor)
                  : const Color(AppConstants.errorColor))
              .withValues(alpha: 0.1),
          side: BorderSide.none,
        ),
        children: [
          if (details.isNotEmpty)
            Padding(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('বিস্তারিত (Details):',
                      style: TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: AppConstants.captionFontSize)),
                  const SizedBox(height: AppConstants.paddingXSmall),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(AppConstants.paddingMedium),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      borderRadius:
                          BorderRadius.circular(AppConstants.radiusMedium),
                    ),
                    child: Text(details,
                        style: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: AppConstants.captionFontSize)),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  // ─── Stats Tab ──────────────────────────────────────────

  Widget _buildStatsTab() {
    final stats = _alertStats;
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '(সতর্কতা ও লগের সামগ্রিক চিত্র)',
              style: TextStyle(
                  fontSize: AppConstants.captionFontSize, color: Colors.grey),
            ),
            const SizedBox(height: AppConstants.paddingLarge),
            _buildInfoBanner(),
            const SizedBox(height: AppConstants.paddingLarge),
            GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: AppConstants.paddingMedium,
              mainAxisSpacing: AppConstants.paddingMedium,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              childAspectRatio: 1.4,
              children: [
                _buildStatCard(
                    'মোট সতর্কতা',
                    '${stats?['totalAlerts'] ?? _alerts.length}',
                    Icons.notifications,
                    AppConstants.warningColor,
                    'সর্বমোট সতর্কতার সংখ্যা'),
                _buildStatCard(
                    'জরুরি',
                    '${stats?['criticalCount'] ?? 0}',
                    Icons.error,
                    AppConstants.errorColor,
                    'জরুরি সমস্যার সংখ্যা'),
                _buildStatCard(
                    'সমাধান হয়েছে',
                    '${stats?['resolvedCount'] ?? 0}',
                    Icons.check_circle,
                    AppConstants.successColor,
                    'সমাধান করা সমস্যা'),
                _buildStatCard(
                    'মোট লগ',
                    '${stats?['totalLogs'] ?? _logs.length}',
                    Icons.receipt_long,
                    AppConstants.infoColor,
                    'সর্বমোট কার্যক্রম রেকর্ড'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoBanner() {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            const Color(AppConstants.warningColor),
            const Color(AppConstants.warningColor).withValues(alpha: 0.7)
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.monitor_heart, color: Colors.white, size: 36),
          SizedBox(height: AppConstants.paddingSmall),
          Text(
            'সতর্কতা ও কার্যক্রম লগ',
            style: TextStyle(
                color: Colors.white,
                fontSize: AppConstants.titleFontSize,
                fontWeight: FontWeight.bold),
          ),
          SizedBox(height: AppConstants.paddingXSmall),
          Text(
            'সিস্টেমের সকল সতর্কতা, ত্রুটি ও কার্যক্রমের বিস্তারিত রেকর্ড।\n'
            'সমস্যা দ্রুত খুঁজে বের করুন ও সমাধান করুন।',
            style: TextStyle(
                color: Colors.white70, fontSize: AppConstants.bodyFontSize),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(
      String title, String value, IconData icon, int color, String hint) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
          gradient: LinearGradient(
            colors: [
              Color(color).withValues(alpha: 0.1),
              Color(color).withValues(alpha: 0.05)
            ],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(icon, color: Color(color), size: 22),
                  const Spacer(),
                  Tooltip(
                    message: hint,
                    child: Icon(Icons.info_outline,
                        size: 16, color: Colors.grey.shade400),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(value,
                      style: const TextStyle(
                          fontSize: 22, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 2),
                  Text(title,
                      style: const TextStyle(fontSize: 11, color: Colors.grey)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState(IconData icon, String title, String subtitle) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 64, color: Colors.grey.shade300),
          const SizedBox(height: AppConstants.paddingMedium),
          Text(title,
              style: const TextStyle(
                  fontSize: AppConstants.titleFontSize,
                  fontWeight: FontWeight.bold)),
          const SizedBox(height: AppConstants.paddingSmall),
          Text(subtitle, style: const TextStyle(color: Colors.grey)),
        ],
      ),
    );
  }
}
