import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class DecisionHistoryScreen extends StatefulWidget {
  const DecisionHistoryScreen({Key? key}) : super(key: key);

  @override
  State<DecisionHistoryScreen> createState() => _DecisionHistoryScreenState();
}

class _DecisionHistoryScreenState extends State<DecisionHistoryScreen> {
  final ApiService _apiService = ApiService();
  List<dynamic> _timeline = [];
  Map<String, dynamic>? _stats;
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
      _apiService.get<List<dynamic>>(Environment.timelineRange),
      _apiService.get<Map<String, dynamic>>(Environment.timelineStats),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) {
        _timeline = (results[0].data as List<dynamic>?) ?? [];
      }
      if (results[1].success) _stats = results[1].data as Map<String, dynamic>?;
      if (!results[0].success) {
        _error = results[0].error ?? 'টাইমলাইন লোড করা যায়নি';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('সিদ্ধান্তের ইতিহাস'),
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
                      const Icon(Icons.timeline, size: 48, color: Colors.grey),
                      Text(_error!),
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
                        _buildStatsSection(),
                        const SizedBox(height: 20),
                        _buildTimelineSection(),
                      ],
                    ),
                  ),
                ),
    );
  }

  Widget _buildStatsSection() {
    final s = _stats ?? {};
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(children: [
              Icon(Icons.analytics, color: Color(AppConstants.primaryColor)),
              SizedBox(width: 8),
              Text('সিদ্ধান্তের পরিসংখ্যান',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text('(AI কতটি সিদ্ধান্ত নিয়েছে এবং সেগুলোর ফলাফল)',
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
                _buildStatCard('মোট সিদ্ধান্ত', '${s['totalDecisions'] ?? 0}',
                    Icons.gavel, AppConstants.primaryColor),
                _buildStatCard('সফল', '${s['successful'] ?? 0}',
                    Icons.check_circle, AppConstants.successColor),
                _buildStatCard('ব্যর্থ', '${s['failed'] ?? 0}', Icons.cancel,
                    AppConstants.errorColor),
                _buildStatCard('অপেক্ষমাণ', '${s['pending'] ?? 0}',
                    Icons.hourglass_empty, AppConstants.warningColor),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimelineSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('টাইমলাইন',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const Text('(সময়ের ক্রমানুসারে সিদ্ধান্তের তালিকা)',
            style: TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 12),
        if (_timeline.isEmpty)
          const Center(
              child: Padding(
            padding: EdgeInsets.all(32),
            child: Text('কোনো সিদ্ধান্তের ইতিহাস নেই',
                style: TextStyle(color: Colors.grey)),
          ))
        else
          ..._timeline.map((item) {
            final map =
                item is Map<String, dynamic> ? item : <String, dynamic>{};
            final action = '${map['action'] ?? map['decision'] ?? 'Unknown'}';
            final agent = '${map['agent'] ?? map['agentName'] ?? ''}';
            final timestamp = '${map['timestamp'] ?? map['createdAt'] ?? ''}';
            final outcome =
                '${map['outcome'] ?? map['status'] ?? ''}'.toUpperCase();
            final details = '${map['details'] ?? map['description'] ?? ''}';

            Color outcomeColor;
            switch (outcome) {
              case 'SUCCESS':
              case 'APPROVED':
                outcomeColor = Colors.green;
                break;
              case 'FAILED':
              case 'REJECTED':
                outcomeColor = Colors.red;
                break;
              case 'PENDING':
                outcomeColor = Colors.orange;
                break;
              default:
                outcomeColor = Colors.grey;
            }

            return Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: IntrinsicHeight(
                child: Row(
                  children: [
                    Container(
                      width: 4,
                      decoration: BoxDecoration(
                        color: outcomeColor,
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(4),
                          bottomLeft: Radius.circular(4),
                        ),
                      ),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                    child: Text(action,
                                        style: const TextStyle(
                                            fontWeight: FontWeight.bold))),
                                Chip(
                                  label: Text(outcome.isEmpty ? 'N/A' : outcome,
                                      style: TextStyle(
                                          fontSize: 10, color: outcomeColor)),
                                  backgroundColor:
                                      outcomeColor.withValues(alpha: 0.1),
                                  padding: EdgeInsets.zero,
                                  visualDensity: VisualDensity.compact,
                                ),
                              ],
                            ),
                            if (agent.isNotEmpty)
                              Text('এজেন্ট: $agent',
                                  style: const TextStyle(
                                      fontSize: 12, color: Colors.grey)),
                            if (details.isNotEmpty)
                              Text(details,
                                  style: const TextStyle(fontSize: 12)),
                            Text(timestamp,
                                style: TextStyle(
                                    fontSize: 11, color: Colors.grey.shade500)),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          }),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, int color) {
    return Card(
      elevation: 0,
      color: Color(color).withValues(alpha: 0.08),
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
