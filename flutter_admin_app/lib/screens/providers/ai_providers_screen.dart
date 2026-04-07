import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class AIProvidersScreen extends StatefulWidget {
  const AIProvidersScreen({Key? key}) : super(key: key);

  @override
  State<AIProvidersScreen> createState() => _AIProvidersScreenState();
}

class _AIProvidersScreenState extends State<AIProvidersScreen> {
  final ApiService _apiService = ApiService();
  List<dynamic> _availableProviders = [];
  List<dynamic> _configuredProviders = [];
  bool _isLoading = true;
  String? _error;

  final _apiKeyController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadProviders();
  }

  @override
  void dispose() {
    _apiKeyController.dispose();
    super.dispose();
  }

  Future<void> _loadProviders() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    final results = await Future.wait([
      _apiService.get<List<dynamic>>(Environment.providersAvailable),
      _apiService.get<List<dynamic>>(Environment.providersConfigured),
    ]);

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (results[0].success) _availableProviders = (results[0].data as List<dynamic>?) ?? [];
      if (results[1].success) _configuredProviders = (results[1].data as List<dynamic>?) ?? [];
      if (!results[0].success && !results[1].success) {
        _error = results[0].error ?? 'প্রোভাইডার লোড করা যায়নি';
      }
    });
  }

  Future<void> _testProvider(String providerId) async {
    _showSnackBar('$providerId পরীক্ষা করা হচ্ছে...', isInfo: true);

    final response = await _apiService.post<Map<String, dynamic>>(
      '${Environment.providersTest}/$providerId',
    );

    if (!mounted) return;

    if (response.success) {
      _showSnackBar('$providerId সফলভাবে কাজ করছে!');
    } else {
      _showSnackBar('$providerId পরীক্ষায় ব্যর্থ: ${response.error}', isError: true);
    }
  }

  Future<void> _addProvider(String providerName) async {
    _apiKeyController.clear();
    final result = await showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('$providerName যোগ করুন'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'এই AI প্রোভাইডারের API Key দিন।\n(প্রোভাইডারের ওয়েবসাইট থেকে API Key সংগ্রহ করুন)',
              style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey.shade600),
            ),
            const SizedBox(height: AppConstants.paddingMedium),
            TextField(
              controller: _apiKeyController,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'API Key',
                hintText: 'sk-... বা আপনার API key',
                helperText: '(প্রোভাইডারের গোপন API কী)',
                prefixIcon: const Icon(Icons.key),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('বাতিল')),
          ElevatedButton(
            onPressed: () {
              if (_apiKeyController.text.trim().isNotEmpty) {
                Navigator.pop(ctx, {'name': providerName, 'apiKey': _apiKeyController.text.trim()});
              }
            },
            child: const Text('যোগ করো'),
          ),
        ],
      ),
    );

    if (result == null) return;

    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.providersAdd,
      data: result,
    );

    if (!mounted) return;

    if (response.success) {
      _showSnackBar('$providerName সফলভাবে যোগ হয়েছে!');
      _loadProviders();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "যোগ করা যায়নি"}', isError: true);
    }
  }

  Future<void> _removeProvider(String providerId) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('প্রোভাইডার সরান?'),
        content: Text(
          '$providerId সরিয়ে ফেলতে চান?\n'
          '(এটি সরালে এই AI প্রোভাইডার আর কাজ করবে না)',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('না')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Color(AppConstants.errorColor)),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('হ্যাঁ, সরাও'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    final response = await _apiService.post<Map<String, dynamic>>(
      '${Environment.providersRemove}/$providerId',
    );

    if (!mounted) return;

    if (response.success) {
      _showSnackBar('$providerId সরানো হয়েছে');
      _loadProviders();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "সরানো যায়নি"}', isError: true);
    }
  }

  void _showSnackBar(String message, {bool isError = false, bool isInfo = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError
            ? Color(AppConstants.errorColor)
            : isInfo
                ? Color(AppConstants.infoColor)
                : Color(AppConstants.successColor),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Providers'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: _isLoading ? null : _loadProviders,
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildErrorView()
              : RefreshIndicator(
                  onRefresh: _loadProviders,
                  child: SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(AppConstants.paddingLarge),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildInfoBanner(),
                        const SizedBox(height: AppConstants.paddingLarge),
                        _buildConfiguredSection(),
                        const SizedBox(height: AppConstants.paddingLarge),
                        _buildAvailableSection(),
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
          ElevatedButton(onPressed: _loadProviders, child: const Text('আবার চেষ্টা করুন')),
        ],
      ),
    );
  }

  Widget _buildInfoBanner() {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(AppConstants.secondaryColor), Color(AppConstants.secondaryColor).withOpacity(0.7)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.hub, color: Colors.white, size: 36),
          const SizedBox(height: AppConstants.paddingSmall),
          const Text(
            'AI প্রোভাইডার ব্যবস্থাপনা',
            style: TextStyle(color: Colors.white, fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: AppConstants.paddingXSmall),
          const Text(
            '১০টি AI প্রোভাইডার সংযুক্ত করুন। যত বেশি AI যুক্ত, তত ভালো Consensus ভোটিং।\n'
            'প্রতিটি সিদ্ধান্ত ১০টি AI-এর মতামতের ভিত্তিতে নেওয়া হয়।',
            style: TextStyle(color: Colors.white70, fontSize: AppConstants.bodyFontSize),
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: AppConstants.paddingMedium, vertical: AppConstants.paddingSmall),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
            child: Text(
              'সক্রিয়: ${_configuredProviders.length} / ${_availableProviders.length + _configuredProviders.length} প্রোভাইডার',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConfiguredSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'সক্রিয় প্রোভাইডার',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(এই AI গুলো এখন কাজ করছে ও ভোট দিচ্ছে)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        if (_configuredProviders.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Row(
                children: const [
                  Icon(Icons.info_outline, color: Colors.orange),
                  SizedBox(width: AppConstants.paddingMedium),
                  Expanded(child: Text('কোনো প্রোভাইডার সক্রিয় নেই। নিচে থেকে যোগ করুন।')),
                ],
              ),
            ),
          )
        else
          ..._configuredProviders.map((p) {
            final map = p is Map<String, dynamic> ? p : <String, dynamic>{};
            final name = '${map['name'] ?? map['id'] ?? 'Unknown'}';
            final status = '${map['status'] ?? 'active'}';
            return Card(
              margin: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: Color(AppConstants.successColor).withOpacity(0.1),
                  child: Icon(Icons.check_circle, color: Color(AppConstants.successColor)),
                ),
                title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text('অবস্থা: $status (সক্রিয় — ভোটে অংশ নিচ্ছে)'),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      onPressed: () => _testProvider(name),
                      icon: const Icon(Icons.speed),
                      tooltip: 'পরীক্ষা করো (কাজ করছে কিনা দেখো)',
                    ),
                    IconButton(
                      onPressed: () => _removeProvider(name),
                      icon: Icon(Icons.delete_outline, color: Color(AppConstants.errorColor)),
                      tooltip: 'সরাও (এই প্রোভাইডার বাদ দাও)',
                    ),
                  ],
                ),
              ),
            );
          }),
      ],
    );
  }

  Widget _buildAvailableSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'উপলব্ধ প্রোভাইডার',
          style: TextStyle(fontSize: AppConstants.titleFontSize, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(এই AI গুলো যোগ করা যাবে — API Key দিয়ে সক্রিয় করুন)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        if (_availableProviders.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Row(
                children: const [
                  Icon(Icons.celebration, color: Colors.green),
                  SizedBox(width: AppConstants.paddingMedium),
                  Expanded(child: Text('সব প্রোভাইডার ইতিমধ্যে সক্রিয়! 🎉')),
                ],
              ),
            ),
          )
        else
          ..._availableProviders.map((p) {
            final map = p is Map<String, dynamic> ? p : <String, dynamic>{};
            final name = '${map['name'] ?? map['id'] ?? 'Unknown'}';
            final desc = '${map['description'] ?? ''}';
            return Card(
              margin: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: Color(AppConstants.warningColor).withOpacity(0.1),
                  child: Icon(Icons.add_circle_outline, color: Color(AppConstants.warningColor)),
                ),
                title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text(desc.isNotEmpty ? desc : 'API Key দিয়ে সক্রিয় করুন'),
                trailing: ElevatedButton.icon(
                  onPressed: () => _addProvider(name),
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('যোগ করো'),
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: AppConstants.paddingMedium),
                  ),
                ),
              ),
            );
          }),
      ],
    );
  }
}
