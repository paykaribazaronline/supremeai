import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class SelfExtensionScreen extends StatefulWidget {
  const SelfExtensionScreen({Key? key}) : super(key: key);

  @override
  State<SelfExtensionScreen> createState() => _SelfExtensionScreenState();
}

class _SelfExtensionScreenState extends State<SelfExtensionScreen> {
  final ApiService _apiService = ApiService();
  final _requirementController = TextEditingController();
  final _batchController = TextEditingController();

  Map<String, dynamic>? _extensionStatus;
  bool _isLoading = true;
  bool _isSubmitting = false;
  String? _error;
  Map<String, dynamic>? _lastResult;

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  @override
  void dispose() {
    _requirementController.dispose();
    _batchController.dispose();
    super.dispose();
  }

  Future<void> _loadStatus() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    final response = await _apiService.get<Map<String, dynamic>>(Environment.extendStatus);

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (response.success) {
        _extensionStatus = response.data;
      } else {
        _error = response.error ?? 'স্ট্যাটাস লোড করা যায়নি';
      }
    });
  }

  Future<void> _submitRequirement() async {
    final text = _requirementController.text.trim();
    if (text.isEmpty) {
      _showSnackBar('প্রয়োজনীয়তা লিখুন', isError: true);
      return;
    }

    setState(() {
      _isSubmitting = true;
      _lastResult = null;
    });

    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.extendRequirement,
      data: {'requirement': text},
    );

    if (!mounted) return;

    setState(() {
      _isSubmitting = false;
      if (response.success) {
        _lastResult = response.data;
        _requirementController.clear();
      }
    });

    if (response.success) {
      _showSnackBar('নতুন সার্ভিস তৈরি হচ্ছে!');
      _loadStatus();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "তৈরি করা যায়নি"}', isError: true);
    }
  }

  Future<void> _submitBatch() async {
    final text = _batchController.text.trim();
    if (text.isEmpty) {
      _showSnackBar('অন্তত একটি প্রয়োজনীয়তা লিখুন', isError: true);
      return;
    }

    final requirements = text.split('\n').where((l) => l.trim().isNotEmpty).toList();
    if (requirements.isEmpty) {
      _showSnackBar('কোনো বৈধ প্রয়োজনীয়তা নেই', isError: true);
      return;
    }

    setState(() {
      _isSubmitting = true;
      _lastResult = null;
    });

    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.extendBatch,
      data: {'requirements': requirements},
    );

    if (!mounted) return;

    setState(() {
      _isSubmitting = false;
      if (response.success) {
        _lastResult = response.data;
        _batchController.clear();
      }
    });

    if (response.success) {
      _showSnackBar('${requirements.length}টি সার্ভিস তৈরি হচ্ছে!');
      _loadStatus();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "ব্যাচ তৈরি ব্যর্থ"}', isError: true);
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Color(AppConstants.errorColor) : Color(AppConstants.successColor),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Self-Extension'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: _isLoading ? null : _loadStatus,
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadStatus,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(AppConstants.paddingLarge),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildInfoBanner(),
                    const SizedBox(height: AppConstants.paddingLarge),
                    _buildStatusSection(),
                    const SizedBox(height: AppConstants.paddingLarge),
                    _buildSingleRequirementSection(),
                    const SizedBox(height: AppConstants.paddingLarge),
                    _buildBatchSection(),
                    if (_lastResult != null) ...[
                      const SizedBox(height: AppConstants.paddingLarge),
                      _buildResultSection(),
                    ],
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildInfoBanner() {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.deepPurple, Colors.deepPurple.withOpacity(0.7)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: const [
          Icon(Icons.auto_fix_high, color: Colors.white, size: 36),
          SizedBox(height: AppConstants.paddingSmall),
          Text(
            'নিজে নিজে বড় হওয়া',
            style: TextStyle(color: Colors.white, fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
          ),
          SizedBox(height: AppConstants.paddingXSmall),
          Text(
            'AI নিজেই নতুন সার্ভিস ও কন্ট্রোলার তৈরি করতে পারে!\n'
            'আপনি শুধু বলুন কী দরকার — AI কোড লিখবে, কম্পাইল করবে, লোড করবে।',
            style: TextStyle(color: Colors.white70, fontSize: AppConstants.bodyFontSize),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusSection() {
    final status = _extensionStatus;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'এক্সটেনশন স্ট্যাটাস',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(সিস্টেম এখন নতুন কোড তৈরি করতে প্রস্তুত কিনা)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        if (_error != null)
          Card(
            color: const Color(0xFFFFF3CD),
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              child: Row(
                children: [
                  const Icon(Icons.warning_amber_rounded, color: Colors.orange),
                  const SizedBox(width: AppConstants.paddingMedium),
                  Expanded(child: Text(_error!)),
                ],
              ),
            ),
          )
        else
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Column(
                children: [
                  _buildStatusRow(Icons.power, 'প্রস্তুত', '${status?['ready'] ?? 'unknown'}', 'সিস্টেম কোড তৈরি করতে পারবে কিনা'),
                  const Divider(),
                  _buildStatusRow(Icons.history, 'তৈরি হয়েছে', '${status?['totalGenerated'] ?? 0}টি', 'এ পর্যন্ত কতটি সার্ভিস তৈরি হয়েছে'),
                  const Divider(),
                  _buildStatusRow(Icons.memory, 'লোড হয়েছে', '${status?['totalLoaded'] ?? 0}টি', 'কতটি সার্ভিস সফলভাবে চালু আছে'),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildStatusRow(IconData icon, String label, String value, String hint) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppConstants.paddingXSmall),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Colors.deepPurple),
          const SizedBox(width: AppConstants.paddingMedium),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
                Text(hint, style: const TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey)),
              ],
            ),
          ),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  Widget _buildSingleRequirementSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'নতুন সার্ভিস তৈরি করো',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(একটি প্রয়োজনীয়তা লিখুন, AI নিজে সার্ভিস বানাবে)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        TextField(
          controller: _requirementController,
          maxLines: 3,
          decoration: InputDecoration(
            labelText: 'প্রয়োজনীয়তা',
            hintText: 'যেমন: create UserAuditService with methods: audit, log, export',
            helperText: '(কী ধরনের সার্ভিস, কোন মেথড থাকবে — ইংরেজিতে বা বাংলায় লিখুন)',
            helperMaxLines: 2,
            prefixIcon: const Icon(Icons.auto_fix_high),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
          ),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        SizedBox(
          width: double.infinity,
          height: 50,
          child: ElevatedButton.icon(
            onPressed: _isSubmitting ? null : _submitRequirement,
            icon: _isSubmitting
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.rocket_launch),
            label: const Text('তৈরি করো', style: TextStyle(fontSize: AppConstants.subtitleFontSize)),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.deepPurple),
          ),
        ),
      ],
    );
  }

  Widget _buildBatchSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'ব্যাচ তৈরি (একসাথে অনেকগুলো)',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(প্রতি লাইনে একটি করে প্রয়োজনীয়তা লিখুন — সব একসাথে তৈরি হবে)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        TextField(
          controller: _batchController,
          maxLines: 5,
          decoration: InputDecoration(
            labelText: 'একাধিক প্রয়োজনীয়তা',
            hintText: 'create PaymentService with methods: pay, refund\ncreate NotificationService with methods: send, schedule',
            helperText: '(প্রতিটি লাইনে একটি সার্ভিসের বিবরণ লিখুন)',
            helperMaxLines: 2,
            prefixIcon: const Icon(Icons.list_alt),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
          ),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        SizedBox(
          width: double.infinity,
          height: 50,
          child: ElevatedButton.icon(
            onPressed: _isSubmitting ? null : _submitBatch,
            icon: _isSubmitting
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.batch_prediction),
            label: const Text('সব তৈরি করো', style: TextStyle(fontSize: AppConstants.subtitleFontSize)),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.deepPurple.shade700),
          ),
        ),
      ],
    );
  }

  Widget _buildResultSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'ফলাফল',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(শেষ অনুরোধের ফলাফল)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        Card(
          color: Color(AppConstants.successColor).withOpacity(0.05),
          child: Padding(
            padding: const EdgeInsets.all(AppConstants.paddingLarge),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.check_circle, color: Color(AppConstants.successColor)),
                    const SizedBox(width: AppConstants.paddingSmall),
                    const Text('সফল!', style: TextStyle(fontWeight: FontWeight.bold, fontSize: AppConstants.subtitleFontSize)),
                  ],
                ),
                const SizedBox(height: AppConstants.paddingMedium),
                Text('${_lastResult?['message'] ?? _lastResult.toString()}'),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
