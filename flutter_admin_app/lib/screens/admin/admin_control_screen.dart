import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class AdminControlScreen extends StatefulWidget {
  const AdminControlScreen({Key? key}) : super(key: key);

  @override
  State<AdminControlScreen> createState() => _AdminControlScreenState();
}

class _AdminControlScreenState extends State<AdminControlScreen> {
  final ApiService _apiService = ApiService();
  Map<String, dynamic>? _controlStatus;
  List<dynamic> _pendingApprovals = [];
  List<dynamic> _history = [];
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
      _apiService.get<Map<String, dynamic>>(Environment.adminControl),
      _apiService.get<List<dynamic>>(Environment.adminControlPending),
      _apiService.get<List<dynamic>>(Environment.adminControlHistory),
    ]);

    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success)
        _controlStatus = results[0].data as Map<String, dynamic>?;
      if (results[1].success)
        _pendingApprovals = (results[1].data as List<dynamic>?) ?? [];
      if (results[2].success)
        _history = (results[2].data as List<dynamic>?) ?? [];
      if (!results[0].success)
        _error = results[0].error ?? 'তথ্য লোড করা যায়নি';
    });
  }

  Future<void> _changeMode(String mode) async {
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.adminControlMode,
      data: {'mode': mode},
    );
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'মোড পরিবর্তন হয়েছে: $mode'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: response.success
          ? const Color(AppConstants.successColor)
          : const Color(AppConstants.errorColor),
    ));
    if (response.success) _loadAll();
  }

  Future<void> _stopSystem() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('সিস্টেম বন্ধ করুন'),
        content: const Text(
            'সিস্টেম সম্পূর্ণ বন্ধ হবে। আপনি কি নিশ্চিত?\n(সব কাজ থেমে যাবে)'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('না, রাখুন')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('হ্যাঁ, বন্ধ করুন'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      await _apiService
          .post<Map<String, dynamic>>(Environment.adminControlStop);
      _loadAll();
    }
  }

  Future<void> _resumeSystem() async {
    await _apiService
        .post<Map<String, dynamic>>(Environment.adminControlResume);
    _loadAll();
  }

  Future<void> _handleApproval(String id, bool approve) async {
    final endpoint =
        '${Environment.adminControlPending}/$id/${approve ? 'approve' : 'reject'}';
    final response = await _apiService.post<Map<String, dynamic>>(endpoint);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content:
          Text(approve ? 'অনুমোদন দেওয়া হয়েছে' : 'প্রত্যাখ্যান করা হয়েছে'),
      backgroundColor:
          Color(approve ? AppConstants.successColor : AppConstants.errorColor),
    ));
    if (response.success) _loadAll();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('অ্যাডমিন কন্ট্রোল'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
            onPressed: _loadAll,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                      const Icon(Icons.error_outline,
                          size: 48, color: Colors.grey),
                      const SizedBox(height: 16),
                      Text(_error!),
                      const SizedBox(height: 16),
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
                        _buildModeSection(),
                        const SizedBox(height: 24),
                        _buildSystemActions(),
                        const SizedBox(height: 24),
                        _buildPendingApprovals(),
                        const SizedBox(height: 24),
                        _buildHistorySection(),
                      ],
                    ),
                  ),
                ),
    );
  }

  Widget _buildModeSection() {
    final currentMode = '${_controlStatus?['mode'] ?? 'UNKNOWN'}'.toUpperCase();
    final isRunning = _controlStatus?['running'] ?? false;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.admin_panel_settings,
                    color: Color(AppConstants.primaryColor)),
                SizedBox(width: 8),
                Text('সিস্টেম মোড',
                    style:
                        TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              ],
            ),
            const Text('(সিস্টেম কিভাবে চলবে তা এখানে ঠিক করুন)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            Row(
              children: [
                Chip(
                  avatar: Icon(
                      isRunning ? Icons.play_circle : Icons.stop_circle,
                      color: isRunning ? Colors.green : Colors.red,
                      size: 18),
                  label: Text(isRunning ? 'চালু আছে' : 'বন্ধ আছে'),
                  backgroundColor:
                      (isRunning ? Colors.green : Colors.red).withOpacity(0.1),
                ),
                const SizedBox(width: 8),
                Chip(
                    label: Text('মোড: $currentMode'),
                    backgroundColor: Colors.blue.withOpacity(0.1)),
              ],
            ),
            const SizedBox(height: 16),
            const Text('মোড পরিবর্তন করুন:',
                style: TextStyle(fontWeight: FontWeight.w600)),
            const Text(
                '(AUTO = নিজে নিজে চলবে, WAIT = অনুমতি নেবে, FORCE_STOP = সব বন্ধ)',
                style: TextStyle(fontSize: 11, color: Colors.grey)),
            const SizedBox(height: 8),
            Wrap(spacing: 8, children: [
              _buildModeButton(
                  'AUTO', Icons.auto_mode, Colors.green, currentMode),
              _buildModeButton('WAIT', Icons.pause_circle_outline,
                  Colors.orange, currentMode),
              _buildModeButton(
                  'FORCE_STOP', Icons.dangerous, Colors.red, currentMode),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _buildModeButton(
      String mode, IconData icon, Color color, String currentMode) {
    final isActive = currentMode == mode;
    return ElevatedButton.icon(
      onPressed: isActive ? null : () => _changeMode(mode),
      icon: Icon(icon, size: 18),
      label: Text(mode),
      style: ElevatedButton.styleFrom(
        backgroundColor: isActive ? color : null,
        foregroundColor: isActive ? Colors.white : color,
        side: BorderSide(color: color),
      ),
    );
  }

  Widget _buildSystemActions() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('সিস্টেম নিয়ন্ত্রণ',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Text('(সিস্টেম চালু/বন্ধ করুন)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            Row(children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _resumeSystem,
                  icon: const Icon(Icons.play_arrow),
                  label: const Text('চালু করুন'),
                  style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      foregroundColor: Colors.white),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _stopSystem,
                  icon: const Icon(Icons.stop),
                  label: const Text('বন্ধ করুন'),
                  style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white),
                ),
              ),
            ]),
          ],
        ),
      ),
    );
  }

  Widget _buildPendingApprovals() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              const Icon(Icons.pending_actions,
                  color: Color(AppConstants.warningColor)),
              const SizedBox(width: 8),
              Text('অপেক্ষমাণ অনুমোদন (${_pendingApprovals.length})',
                  style: const TextStyle(
                      fontSize: 18, fontWeight: FontWeight.bold)),
            ]),
            const Text(
                '(WAIT মোডে AI যা করতে চায় তা এখানে অনুমোদন/প্রত্যাখ্যান করুন)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            if (_pendingApprovals.isEmpty)
              const Center(
                  child: Padding(
                padding: EdgeInsets.all(24),
                child: Text('কোনো অপেক্ষমাণ কাজ নেই',
                    style: TextStyle(color: Colors.grey)),
              ))
            else
              ..._pendingApprovals.map((item) {
                final map =
                    item is Map<String, dynamic> ? item : <String, dynamic>{};
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.help_outline)),
                  title: Text('${map['action'] ?? 'Unknown action'}'),
                  subtitle: Text('${map['description'] ?? ''}'),
                  trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                    IconButton(
                      icon: const Icon(Icons.check_circle, color: Colors.green),
                      tooltip: 'অনুমোদন দিন',
                      onPressed: () => _handleApproval('${map['id']}', true),
                    ),
                    IconButton(
                      icon: const Icon(Icons.cancel, color: Colors.red),
                      tooltip: 'প্রত্যাখ্যান করুন',
                      onPressed: () => _handleApproval('${map['id']}', false),
                    ),
                  ]),
                );
              }),
          ],
        ),
      ),
    );
  }

  Widget _buildHistorySection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('সাম্প্রতিক কার্যকলাপ',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const Text('(আগে কি কি কাজ হয়েছে তার তালিকা)',
                style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 12),
            if (_history.isEmpty)
              const Center(
                  child: Padding(
                padding: EdgeInsets.all(24),
                child: Text('কোনো ইতিহাস নেই',
                    style: TextStyle(color: Colors.grey)),
              ))
            else
              ...(_history.take(20).map((item) {
                final map =
                    item is Map<String, dynamic> ? item : <String, dynamic>{};
                return ListTile(
                  dense: true,
                  leading: Icon(Icons.history,
                      color: Colors.grey.shade400, size: 20),
                  title: Text('${map['action'] ?? 'Unknown'}',
                      style: const TextStyle(fontSize: 13)),
                  subtitle: Text('${map['timestamp'] ?? ''}',
                      style: const TextStyle(fontSize: 11)),
                  trailing: Chip(
                    label: Text('${map['status'] ?? ''}',
                        style: const TextStyle(fontSize: 10)),
                    padding: EdgeInsets.zero,
                    visualDensity: VisualDensity.compact,
                  ),
                );
              })),
          ],
        ),
      ),
    );
  }
}
