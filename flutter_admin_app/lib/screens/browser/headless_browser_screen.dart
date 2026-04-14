import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../services/api_service.dart';

class HeadlessBrowserScreen extends StatefulWidget {
  const HeadlessBrowserScreen({Key? key}) : super(key: key);

  @override
  State<HeadlessBrowserScreen> createState() => _HeadlessBrowserScreenState();
}

class _HeadlessBrowserScreenState extends State<HeadlessBrowserScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;
  final TextEditingController _urlController = TextEditingController();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  Map<String, dynamic>? _browserStats;
  List<dynamic> _auditLogs = [];
  final List<dynamic> _scrapeHistory = [];
  bool _isLoading = true;
  bool _isScraping = false;
  bool _useAuth = false;
  bool _includeScreenshot = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _urlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>('/api/browser/stats'),
      _apiService.get<List<dynamic>>('/api/browser/audit'),
    ]);

    if (!mounted) return;

    setState(() {
      _isLoading = false;
      if (results[0].success) {
        final data = results[0].data;
        if (data is Map<String, dynamic> && data.containsKey('stats')) {
          _browserStats = data['stats'] as Map<String, dynamic>?;
        } else if (data is Map<String, dynamic>) {
          _browserStats = data;
        }
      }
      if (results[1].success) {
        _auditLogs = (results[1].data as List<dynamic>?) ?? [];
      }
      if (!results[0].success && _error == null) {
        _error = results[0].error ?? 'ব্রাউজার তথ্য লোড করা যায়নি';
      }
    });
  }

  Future<void> _scrapeURL() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      _showSnackBar('URL লিখুন', Colors.orange[400]!);
      return;
    }

    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      _showSnackBar(
          'URL must start with http:// or https://', Colors.orange[400]!);
      return;
    }

    setState(() => _isScraping = true);

    final endpoint =
        _useAuth ? '/api/browser/scrape-auth' : '/api/browser/scrape';
    final data = {
      'url': url,
      'includeScreenshot': _includeScreenshot,
      'timeout': 30,
      if (_useAuth) ..._getAuthData(),
    };

    final response = await _apiService.post<Map<String, dynamic>>(
      endpoint,
      data: data,
    );

    if (!mounted) return;

    setState(() => _isScraping = false);

    if (response.success) {
      _showSnackBar('স্ক্র্যাপিং সফল হয়েছে!', Colors.green[400]!);
      _urlController.clear();
      _usernameController.clear();
      _passwordController.clear();
      setState(() => _useAuth = false);
      _loadAll();
    } else {
      _showSnackBar('ত্রুটি: ${response.error}', Colors.red[400]!);
    }
  }

  Future<void> _takeScreenshot() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      _showSnackBar('URL লিখুন', Colors.orange[400]!);
      return;
    }

    setState(() => _isScraping = true);

    final response = await _apiService.post<Map<String, dynamic>>(
      '/api/browser/screenshot',
      data: {'url': url, 'timeout': 30},
    );

    if (!mounted) return;

    setState(() => _isScraping = false);

    if (response.success) {
      _showSnackBar('স্ক্রিনশট সফল হয়েছে!', Colors.green[400]!);
    } else {
      _showSnackBar('ত্রুটি: ${response.error}', Colors.red[400]!);
    }
  }

  Map<String, dynamic> _getAuthData() {
    return {
      'username': _usernameController.text,
      'password': _passwordController.text,
    };
  }

  void _showSnackBar(String message, Color bgColor) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: bgColor,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🌐 হেডলেস ব্রাউজার'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.info_outline), text: 'স্ট্যাটাস'),
            Tab(icon: Icon(Icons.language), text: 'স্ক্র্যাপ'),
            Tab(icon: Icon(Icons.history), text: 'লগ'),
            Tab(icon: Icon(Icons.rule), text: 'অডিট'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ',
            onPressed: _loadAll,
          )
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : TabBarView(
              controller: _tabController,
              children: [
                _buildStatsTab(),
                _buildScrapeTab(),
                _buildHistoryTab(),
                _buildAuditTab(),
              ],
            ),
    );
  }

  Widget _buildStatsTab() {
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Colors.red.shade400),
            const SizedBox(height: 16),
            Text(_error ?? 'অজানা ত্রুটি',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 16)),
          ],
        ),
      );
    }

    final stats = _browserStats ?? {};
    final available = stats['puppeteerAvailable'] ?? false;
    final quotaRemaining = stats['quotaRemaining'] ?? 0;
    final quotaLimit = stats['quotaLimit'] ?? 10;
    final dailyUsage = stats['dailyUsage'] ?? 0;
    final healthStatus = stats['healthStatus'] ?? 'unknown';

    final usagePercent =
        ((quotaLimit - quotaRemaining) / quotaLimit * 100).toInt();

    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(AppConstants.paddingLarge),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [
                      const Icon(Icons.chrome_reader_mode,
                          color: Color(AppConstants.primaryColor)),
                      const SizedBox(width: 8),
                      const Expanded(
                        child: Text(
                          'ব্রাউজার স্ট্যাটাস',
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: available ? Colors.green : Colors.red,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          available ? 'সক্রিয়' : 'নিষ্ক্রিয়',
                          style: const TextStyle(
                              color: Colors.white, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ]),
                    const SizedBox(height: 16),
                    _buildStatRow('স্বাস্থ্য অবস্থা', healthStatus.toString(),
                        Icons.favorite),
                    _buildStatRow(
                        'আজকের ব্যবহার', '$dailyUsage', Icons.assessment),
                    _buildStatRow('অবশিষ্ট কোটা',
                        '$quotaRemaining / $quotaLimit', Icons.storage),
                    const SizedBox(height: 16),
                    _buildProgressIndicator(usagePercent),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScrapeTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'ওয়েব পেজ স্ক্র্যাপ করুন',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const Text(
                    '(Puppeteer ব্যবহার করে ওয়েব তথ্য সংগ্রহ করুন)',
                    style: TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _urlController,
                    decoration: const InputDecoration(
                      labelText: 'URL',
                      hintText: 'https://example.com',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.language),
                    ),
                  ),
                  const SizedBox(height: 12),
                  CheckboxListTile(
                    title: const Text('স্ক্রিনশট অন্তর্ভুক্ত করুন'),
                    value: _includeScreenshot,
                    onChanged: (v) =>
                        setState(() => _includeScreenshot = v ?? false),
                  ),
                  CheckboxListTile(
                    title: const Text('লগইন করুন (যদি প্রয়োজন হয়)'),
                    value: _useAuth,
                    onChanged: (v) => setState(() => _useAuth = v ?? false),
                  ),
                  if (_useAuth) ...[
                    const SizedBox(height: 12),
                    TextField(
                      controller: _usernameController,
                      decoration: const InputDecoration(
                        labelText: 'ব্যবহারকারী নাম',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.person),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _passwordController,
                      obscureText: true,
                      decoration: const InputDecoration(
                        labelText: 'পাসওয়ার্ড',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.lock),
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _isScraping ? null : _scrapeURL,
                      icon: const Icon(Icons.language),
                      label: _isScraping
                          ? const Text('স্ক্র্যাপ করা হচ্ছে...')
                          : const Text('স্ক্র্যাপ করুন'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _isScraping ? null : _takeScreenshot,
                      icon: const Icon(Icons.screenshot),
                      label: _isScraping
                          ? const Text('ক্যাপচার করা হচ্ছে...')
                          : const Text('স্ক্রিনশট নিন'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.orange,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_scrapeHistory.isEmpty) {
      return const Center(
        child: Text('কোনো স্ক্র্যাপ ইতিহাস নেই'),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        children: _scrapeHistory
            .map(
              (item) => Card(
                child: ListTile(
                  leading: const Icon(
                    Icons.check_circle,
                    color: Colors.green,
                  ),
                  title: Text(item['url'] ?? 'Unknown'),
                  subtitle: Text(
                    item['timestamp'] ?? '',
                    style: const TextStyle(fontSize: 12),
                  ),
                  trailing: Text(
                    '${item['duration'] ?? 0}ms',
                    style: const TextStyle(fontSize: 12),
                  ),
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  Widget _buildAuditTab() {
    if (_auditLogs.isEmpty) {
      return const Center(
        child: Text('কোনো অডিট লগ নেই'),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        children: _auditLogs
            .map(
              (log) => Card(
                child: ListTile(
                  leading: Icon(
                    log['status'] == 'success'
                        ? Icons.check_circle
                        : Icons.error,
                    color:
                        log['status'] == 'success' ? Colors.green : Colors.red,
                  ),
                  title: Text(
                      log['action']?.toString().toUpperCase() ?? 'UNKNOWN'),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        log['url'] ?? '',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      Text(
                        log['timestamp'] ?? '',
                        style: const TextStyle(fontSize: 11),
                      ),
                    ],
                  ),
                  trailing: Text(
                    '${log['duration']}ms',
                    style: const TextStyle(fontSize: 12),
                  ),
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  Widget _buildStatRow(String label, String value, IconData icon) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Colors.grey.shade600),
          const SizedBox(width: 12),
          Expanded(child: Text(label)),
          Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressIndicator(int percent) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('কোটা ব্যবহার'),
            Text('$percent%',
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: percent / 100,
            minHeight: 8,
            backgroundColor: Colors.grey.shade300,
            valueColor: AlwaysStoppedAnimation<Color>(
              percent > 80
                  ? Colors.red
                  : percent > 50
                      ? Colors.orange
                      : Colors.green,
            ),
          ),
        ),
      ],
    );
  }
}
