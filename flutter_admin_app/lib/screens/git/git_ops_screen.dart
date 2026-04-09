import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class GitOpsScreen extends StatefulWidget {
  const GitOpsScreen({Key? key}) : super(key: key);

  @override
  State<GitOpsScreen> createState() => _GitOpsScreenState();
}

class _GitOpsScreenState extends State<GitOpsScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;
  final TextEditingController _commitMsgController = TextEditingController();
  final TextEditingController _branchController =
      TextEditingController(text: 'main');

  Map<String, dynamic>? _gitStatus;
  List<dynamic> _gitLogs = [];
  bool _isLoading = true;
  bool _isCommitting = false;
  bool _isPushing = false;
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
    _commitMsgController.dispose();
    _branchController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>(Environment.gitStatus),
      _apiService.get<List<dynamic>>(Environment.gitLogs),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success)
        _gitStatus = results[0].data as Map<String, dynamic>?;
      if (results[1].success)
        _gitLogs = (results[1].data as List<dynamic>?) ?? [];
      if (!results[0].success)
        _error = results[0].error ?? 'Git তথ্য লোড করা যায়নি';
    });
  }

  Future<void> _commitChanges() async {
    final msg = _commitMsgController.text.trim();
    if (msg.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('কমিট মেসেজ লিখুন'), backgroundColor: Colors.orange),
      );
      return;
    }
    setState(() => _isCommitting = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.gitCommit,
      data: {'message': msg},
    );
    if (!mounted) return;
    setState(() => _isCommitting = false);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(
          response.success ? 'কমিট সফল হয়েছে!' : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
    if (response.success) {
      _commitMsgController.clear();
      _loadAll();
    }
  }

  Future<void> _pushChanges() async {
    final branch = _branchController.text.trim();
    if (branch.isEmpty) return;
    setState(() => _isPushing = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.gitPush,
      data: {'branch': branch},
    );
    if (!mounted) return;
    setState(() => _isPushing = false);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(
          response.success ? 'পুশ সফল হয়েছে!' : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
    if (response.success) _loadAll();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('গিট অপারেশন'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.info_outline), text: 'স্ট্যাটাস'),
            Tab(icon: Icon(Icons.commit), text: 'কমিট ও পুশ'),
            Tab(icon: Icon(Icons.history), text: 'লগ'),
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
          : TabBarView(
              controller: _tabController,
              children: [_buildStatusTab(), _buildCommitTab(), _buildLogsTab()],
            ),
    );
  }

  Widget _buildStatusTab() {
    final status = _gitStatus ?? {};
    final branch = '${status['branch'] ?? 'unknown'}';
    final modified = status['modified'] ?? 0;
    final untracked = status['untracked'] ?? 0;
    final staged = status['staged'] ?? 0;

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
                    const Row(children: [
                      Icon(Icons.source,
                          color: Color(AppConstants.primaryColor)),
                      SizedBox(width: 8),
                      Text('রিপোজিটরি অবস্থা',
                          style: TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold)),
                    ]),
                    const Text('(কোডের বর্তমান অবস্থা)',
                        style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 16),
                    _buildStatusRow(
                        'শাখা (Branch)', branch, Icons.account_tree),
                    _buildStatusRow(
                        'পরিবর্তিত ফাইল', '$modified', Icons.edit_note),
                    _buildStatusRow(
                        'নতুন ফাইল', '$untracked', Icons.add_circle_outline),
                    _buildStatusRow('প্রস্তুত (Staged)', '$staged',
                        Icons.check_circle_outline),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String label, String value, IconData icon) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Colors.grey.shade600),
          const SizedBox(width: 12),
          Expanded(child: Text(label)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  Widget _buildCommitTab() {
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
                  const Text('কমিট করুন',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const Text('(পরিবর্তনগুলো সংরক্ষণ করুন)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _commitMsgController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: 'কমিট মেসেজ',
                      helperText: '(কি পরিবর্তন করেছেন তা সংক্ষেপে লিখুন)',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.message),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _isCommitting ? null : _commitChanges,
                      icon: _isCommitting
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.save),
                      label:
                          Text(_isCommitting ? 'কমিট হচ্ছে...' : 'কমিট করুন'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('পুশ করুন',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const Text('(কোড GitHub-এ পাঠান)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _branchController,
                    decoration: const InputDecoration(
                      labelText: 'শাখার নাম (Branch)',
                      helperText: '(সাধারণত main অথবা master)',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.account_tree),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _isPushing ? null : _pushChanges,
                      icon: _isPushing
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.cloud_upload),
                      label: Text(_isPushing ? 'পুশ হচ্ছে...' : 'পুশ করুন'),
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

  Widget _buildLogsTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _gitLogs.isEmpty
          ? const Center(child: Text('কোনো কমিট লগ নেই'))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _gitLogs.length,
              itemBuilder: (ctx, i) {
                final log = _gitLogs[i] is Map<String, dynamic>
                    ? _gitLogs[i] as Map<String, dynamic>
                    : <String, dynamic>{};
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: const Color(AppConstants.primaryColor)
                          .withOpacity(0.1),
                      child: const Icon(Icons.commit, size: 18),
                    ),
                    title: Text('${log['message'] ?? 'No message'}',
                        maxLines: 2, overflow: TextOverflow.ellipsis),
                    subtitle: Text(
                        '${log['author'] ?? ''} • ${log['date'] ?? log['timestamp'] ?? ''}',
                        style: const TextStyle(fontSize: 11)),
                    trailing: Text(
                        '${log['hash'] ?? ''}'.length > 7
                            ? '${log['hash']}'.substring(0, 7)
                            : '${log['hash'] ?? ''}',
                        style: TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 11,
                            color: Colors.grey.shade600)),
                  ),
                );
              },
            ),
    );
  }
}
