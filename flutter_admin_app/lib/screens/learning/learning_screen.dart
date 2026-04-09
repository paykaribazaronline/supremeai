import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class LearningScreen extends StatefulWidget {
  const LearningScreen({Key? key}) : super(key: key);

  @override
  State<LearningScreen> createState() => _LearningScreenState();
}

class _LearningScreenState extends State<LearningScreen> {
  final ApiService _apiService = ApiService();
  Map<String, dynamic>? _learningStats;
  Map<String, dynamic>? _researchStats;
  List<dynamic>? _criticalItems;
  bool _isLoading = true;
  bool _isResearching = false;
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
      _apiService.get<Map<String, dynamic>>(Environment.learningStats),
      _apiService.get<Map<String, dynamic>>(Environment.learningResearchStats),
      _apiService.get<List<dynamic>>(Environment.learningCritical),
    ]);

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (results[0].success)
        _learningStats = results[0].data as Map<String, dynamic>?;
      if (results[1].success)
        _researchStats = results[1].data as Map<String, dynamic>?;
      if (results[2].success)
        _criticalItems = results[2].data as List<dynamic>?;
      if (!results[0].success && !results[1].success) {
        _error = results[0].error ?? 'Failed to load learning data';
      }
    });
  }

  Future<void> _triggerResearch() async {
    setState(() => _isResearching = true);

    final response = await _apiService
        .post<Map<String, dynamic>>(Environment.learningResearchNow);

    if (!mounted) return;

    setState(() => _isResearching = false);

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          response.success
              ? 'রিসার্চ সাইকেল শুরু হয়েছে!'
              : 'ত্রুটি: ${response.error ?? "রিসার্চ শুরু করা যায়নি"}',
        ),
        backgroundColor: response.success
            ? const Color(AppConstants.successColor)
            : const Color(AppConstants.errorColor),
      ),
    );

    if (response.success) {
      await Future.delayed(const Duration(seconds: 3));
      _loadAll();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Learning & Research'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: _isLoading ? null : _loadAll,
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isResearching ? null : _triggerResearch,
        icon: _isResearching
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                    strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.rocket_launch),
        label: Text(_isResearching ? 'গবেষণা চলছে...' : 'এখনই গবেষণা করো'),
        backgroundColor: const Color(AppConstants.primaryColor),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildErrorView()
              : RefreshIndicator(
                  onRefresh: _loadAll,
                  child: SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(AppConstants.paddingLarge),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildInfoBanner(),
                        const SizedBox(height: AppConstants.paddingLarge),
                        _buildLearningStatsSection(),
                        const SizedBox(height: AppConstants.paddingLarge),
                        _buildResearchStatsSection(),
                        const SizedBox(height: AppConstants.paddingLarge),
                        _buildCriticalItemsSection(),
                      ],
                    ),
                  ),
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

  Widget _buildInfoBanner() {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            const Color(AppConstants.primaryColor),
            const Color(AppConstants.primaryColor).withOpacity(0.7)
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: const Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.psychology, color: Colors.white, size: 36),
          SizedBox(height: AppConstants.paddingSmall),
          Text(
            'শেখা ও গবেষণা',
            style: TextStyle(
                color: Colors.white,
                fontSize: AppConstants.titleFontSize,
                fontWeight: FontWeight.bold),
          ),
          SizedBox(height: AppConstants.paddingXSmall),
          Text(
            'সিস্টেম স্বয়ংক্রিয়ভাবে ইন্টারনেট থেকে নতুন প্রযুক্তি ও সমাধান শেখে। '
            'এখানে শেখার অগ্রগতি ও গবেষণার ফলাফল দেখুন।',
            style: TextStyle(
                color: Colors.white70, fontSize: AppConstants.bodyFontSize),
          ),
        ],
      ),
    );
  }

  Widget _buildLearningStatsSection() {
    final stats = _learningStats;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'শেখার পরিসংখ্যান',
          style: TextStyle(
              fontSize: AppConstants.titleFontSize,
              fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingSmall),
        const Text(
          '(সিস্টেম কতটুকু শিখেছে তার সারসংক্ষেপ)',
          style: TextStyle(
              fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        GridView.count(
          crossAxisCount: 2,
          crossAxisSpacing: AppConstants.paddingMedium,
          mainAxisSpacing: AppConstants.paddingMedium,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          childAspectRatio: 1.4,
          children: [
            _buildStatCard(
              'মোট শেখা',
              '${stats?['totalLearnings'] ?? 0}',
              Icons.school,
              AppConstants.primaryColor,
              'সর্বমোট শেখা বিষয়',
            ),
            _buildStatCard(
              'ত্রুটি সমাধান',
              '${stats?['errorsResolved'] ?? 0}',
              Icons.bug_report,
              AppConstants.errorColor,
              'ত্রুটি থেকে শেখা সমাধান',
            ),
            _buildStatCard(
              'প্যাটার্ন',
              '${stats?['patternsLearned'] ?? 0}',
              Icons.pattern,
              AppConstants.warningColor,
              'চেনা কোডিং প্যাটার্ন',
            ),
            _buildStatCard(
              'আত্মবিশ্বাস',
              '${stats?['confidenceScore'] ?? 0}%',
              Icons.trending_up,
              AppConstants.successColor,
              'সিস্টেমের আত্মবিশ্বাসের হার',
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildResearchStatsSection() {
    final stats = _researchStats;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'ইন্টারনেট গবেষণা',
          style: TextStyle(
              fontSize: AppConstants.titleFontSize,
              fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingSmall),
        const Text(
          '(GitHub, StackOverflow, HackerNews থেকে স্বয়ংক্রিয় শেখা)',
          style: TextStyle(
              fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(AppConstants.paddingLarge),
            child: Column(
              children: [
                _buildResearchRow(
                    Icons.access_time,
                    'শেষ গবেষণা',
                    '${stats?['lastResearchTime'] ?? 'এখনো হয়নি'}',
                    'সর্বশেষ কবে গবেষণা হয়েছে'),
                const Divider(),
                _buildResearchRow(
                    Icons.repeat,
                    'মোট সাইকেল',
                    '${stats?['totalCycles'] ?? 0}',
                    'কতবার গবেষণা চালানো হয়েছে'),
                const Divider(),
                _buildResearchRow(Icons.library_books, 'শেখা বিষয়',
                    '${stats?['itemsLearned'] ?? 0}', 'গবেষণা থেকে শেখা আইটেম'),
                const Divider(),
                _buildResearchRow(
                    Icons.source,
                    'সোর্সগুলো',
                    '${stats?['sources'] ?? 'GitHub, SO, HN, DEV'}',
                    'কোন কোন উৎস থেকে শেখে'),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildResearchRow(
      IconData icon, String label, String value, String hint) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppConstants.paddingXSmall),
      child: Row(
        children: [
          Icon(icon, size: 20, color: const Color(AppConstants.primaryColor)),
          const SizedBox(width: AppConstants.paddingMedium),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label,
                    style: const TextStyle(fontWeight: FontWeight.w600)),
                Text(hint,
                    style: const TextStyle(
                        fontSize: AppConstants.captionFontSize,
                        color: Colors.grey)),
              ],
            ),
          ),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  Widget _buildCriticalItemsSection() {
    final items = _criticalItems ?? [];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'গুরুত্বপূর্ণ প্রয়োজনীয়তা',
          style: TextStyle(
              fontSize: AppConstants.titleFontSize,
              fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingSmall),
        const Text(
          '(সিস্টেম যেসব বিষয় অবশ্যই মনে রাখবে)',
          style: TextStyle(
              fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        if (items.isEmpty)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(AppConstants.paddingLarge),
              child: Row(
                children: [
                  Icon(Icons.check_circle_outline, color: Colors.green),
                  SizedBox(width: AppConstants.paddingMedium),
                  Expanded(
                      child: Text('কোনো গুরুত্বপূর্ণ আইটেম নেই — সব ঠিক আছে!')),
                ],
              ),
            ),
          )
        else
          ...items.map((item) {
            final map =
                item is Map<String, dynamic> ? item : <String, dynamic>{};
            return Card(
              margin: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
              child: ListTile(
                leading: const Icon(
                  Icons.warning_amber_rounded,
                  color: Color(AppConstants.warningColor),
                ),
                title: Text(
                    '${map['description'] ?? map['category'] ?? 'Unknown'}'),
                subtitle: Text(
                    'গুরুত্ব: ${map['severity'] ?? 'N/A'} | আত্মবিশ্বাস: ${map['confidence'] ?? 'N/A'}'),
              ),
            );
          }),
      ],
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
              Color(color).withOpacity(0.1),
              Color(color).withOpacity(0.05)
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
}
