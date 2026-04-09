import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class MlIntelligenceScreen extends StatefulWidget {
  const MlIntelligenceScreen({Key? key}) : super(key: key);

  @override
  State<MlIntelligenceScreen> createState() => _MlIntelligenceScreenState();
}

class _MlIntelligenceScreenState extends State<MlIntelligenceScreen> {
  final ApiService _apiService = ApiService();
  Map<String, dynamic>? _anomalySummary;
  Map<String, dynamic>? _prediction;
  Map<String, dynamic>? _recommendation;
  bool _isLoading = true;
  bool _isDetecting = false;
  bool _isPredicting = false;
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
    final response = await _apiService
        .get<Map<String, dynamic>>(Environment.mlAnomalySummary);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (response.success) {
        _anomalySummary = response.data;
      } else {
        _error = response.error ?? 'ML তথ্য লোড করা যায়নি';
      }
    });
  }

  Future<void> _detectAnomalies() async {
    setState(() => _isDetecting = true);
    final response = await _apiService
        .post<Map<String, dynamic>>(Environment.mlDetectAnomalies);
    if (!mounted) return;
    setState(() {
      _isDetecting = false;
      if (response.success) _anomalySummary = response.data;
    });
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'অ্যানোমালি ডিটেকশন সম্পন্ন!'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
  }

  Future<void> _predictFailure() async {
    setState(() => _isPredicting = true);
    final response = await _apiService
        .post<Map<String, dynamic>>(Environment.mlPredictFailure);
    if (!mounted) return;
    setState(() {
      _isPredicting = false;
      if (response.success) _prediction = response.data;
    });
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'ভবিষ্যদ্বাণী সম্পন্ন!'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
  }

  Future<void> _getRecommendation() async {
    final response = await _apiService
        .post<Map<String, dynamic>>(Environment.mlRecommendProvider);
    if (!mounted) return;
    setState(() {
      if (response.success) _recommendation = response.data;
    });
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'সুপারিশ পাওয়া গেছে!'
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
        title: const Text('ML বুদ্ধিমত্তা'),
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
                      const Icon(Icons.psychology,
                          size: 48, color: Colors.grey),
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
                        _buildAnomalySection(),
                        const SizedBox(height: 20),
                        _buildActionsSection(),
                        const SizedBox(height: 20),
                        if (_prediction != null) _buildPredictionSection(),
                        if (_recommendation != null) ...[
                          const SizedBox(height: 20),
                          _buildRecommendationSection(),
                        ],
                      ],
                    ),
                  ),
                ),
    );
  }

  Widget _buildAnomalySection() {
    final a = _anomalySummary ?? {};
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(children: [
              Icon(Icons.troubleshoot, color: Color(AppConstants.warningColor)),
              SizedBox(width: 8),
              Text('অ্যানোমালি সারসংক্ষেপ',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text('(সিস্টেমে অস্বাভাবিক কিছু হলে ML তা ধরে)',
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
                _buildStatCard('মোট অ্যানোমালি', '${a['totalAnomalies'] ?? 0}',
                    Icons.warning_amber, AppConstants.warningColor),
                _buildStatCard('জরুরি', '${a['criticalAnomalies'] ?? 0}',
                    Icons.error, AppConstants.errorColor),
                _buildStatCard('সমাধিত', '${a['resolvedAnomalies'] ?? 0}',
                    Icons.check_circle, AppConstants.successColor),
                _buildStatCard('নতুন', '${a['newAnomalies'] ?? 0}',
                    Icons.new_releases, AppConstants.infoColor),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('ML কার্যক্রম',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Text('(মেশিন লার্নিং দিয়ে সিস্টেম বিশ্লেষণ করুন)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            _buildActionButton(
              'অ্যানোমালি ডিটেকশন',
              '(অস্বাভাবিক কিছু খুঁজে বের করো)',
              Icons.search,
              Colors.orange,
              _isDetecting ? null : _detectAnomalies,
              _isDetecting,
            ),
            const SizedBox(height: 8),
            _buildActionButton(
              'ব্যর্থতা পূর্বাভাস',
              '(ভবিষ্যতে কি সমস্যা হতে পারে তা বলো)',
              Icons.timeline,
              Colors.blue,
              _isPredicting ? null : _predictFailure,
              _isPredicting,
            ),
            const SizedBox(height: 8),
            _buildActionButton(
              'প্রোভাইডার সুপারিশ',
              '(কোন AI প্রোভাইডার সবচেয়ে ভালো সেটা বলো)',
              Icons.recommend,
              Colors.green,
              _getRecommendation,
              false,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButton(String title, String hint, IconData icon,
      Color color, VoidCallback? onPressed, bool loading) {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.all(16),
          side: BorderSide(color: color.withOpacity(0.5)),
        ),
        child: Row(children: [
          loading
              ? SizedBox(
                  width: 24,
                  height: 24,
                  child:
                      CircularProgressIndicator(strokeWidth: 2, color: color))
              : Icon(icon, color: color),
          const SizedBox(width: 12),
          Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                Text(title,
                    style:
                        TextStyle(fontWeight: FontWeight.bold, color: color)),
                Text(hint,
                    style: const TextStyle(fontSize: 11, color: Colors.grey)),
              ])),
        ]),
      ),
    );
  }

  Widget _buildPredictionSection() {
    final p = _prediction ?? {};
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(children: [
              Icon(Icons.timeline, color: Color(AppConstants.infoColor)),
              SizedBox(width: 8),
              Text('ভবিষ্যদ্বাণী ফলাফল',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text('(ML কি ভবিষ্যদ্বাণী করেছে)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            Text('ঝুঁকির মাত্রা: ${p['riskLevel'] ?? 'N/A'}',
                style: const TextStyle(fontWeight: FontWeight.w600)),
            if (p['predictions'] is List)
              ...(p['predictions'] as List).map((pred) => ListTile(
                    dense: true,
                    leading: const Icon(Icons.arrow_right, size: 18),
                    title: Text('${pred['message'] ?? pred}',
                        style: const TextStyle(fontSize: 13)),
                  )),
          ],
        ),
      ),
    );
  }

  Widget _buildRecommendationSection() {
    final r = _recommendation ?? {};
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(children: [
              Icon(Icons.recommend, color: Color(AppConstants.successColor)),
              SizedBox(width: 8),
              Text('AI প্রোভাইডার সুপারিশ',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const SizedBox(height: 12),
            Text(
                'সেরা প্রোভাইডার: ${r['recommended'] ?? r['provider'] ?? 'N/A'}',
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            if (r['reason'] != null)
              Text('কারণ: ${r['reason']}',
                  style: const TextStyle(fontSize: 13, color: Colors.grey)),
            if (r['score'] != null)
              Text('স্কোর: ${r['score']}',
                  style: const TextStyle(fontSize: 13)),
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
