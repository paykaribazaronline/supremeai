import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({Key? key}) : super(key: key);

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  List<dynamic> _channels = [];
  List<dynamic> _history = [];
  bool _isLoading = true;
  bool _isSending = false;
  bool _isBudgetLoading = false;

  Map<String, dynamic> _smsBudget = {};
  Map<String, dynamic> _smsStats = {};

  // Send notification form
  final _recipientController = TextEditingController();
  final _subjectController = TextEditingController();
  final _messageController = TextEditingController();
  String _selectedChannel = 'email';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _loadAll();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _recipientController.dispose();
    _subjectController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
    });
    final results = await Future.wait([
      _apiService.get<List<dynamic>>(Environment.notificationChannels),
      _apiService.get<List<dynamic>>(Environment.notificationHistory),
      _apiService.get<Map<String, dynamic>>(Environment.notificationSmsBudget),
      _apiService.get<Map<String, dynamic>>(Environment.notificationSmsStats),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) {
        _channels = (results[0].data as List<dynamic>?) ?? [];
      }
      if (results[1].success) {
        _history = (results[1].data as List<dynamic>?) ?? [];
      }
      if (results[2].success) {
        _smsBudget = (results[2].data as Map<String, dynamic>?) ?? {};
      }
      if (results[3].success) {
        _smsStats = (results[3].data as Map<String, dynamic>?) ?? {};
      }
      if (!results[0].success && !results[1].success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('নোটিফিকেশন তথ্য লোড করা যায়নি'), backgroundColor: Colors.red),
        );
      }
    });
  }

  Future<void> _sendNotification() async {
    if (_messageController.text.trim().isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('মেসেজ লিখুন'), backgroundColor: Colors.orange),
      );
      return;
    }

    setState(() => _isSending = true);

    String endpoint;
    switch (_selectedChannel) {
      case 'slack':
        endpoint = Environment.notificationSlack;
        break;
      case 'discord':
        endpoint = Environment.notificationDiscord;
        break;
      case 'sms':
        endpoint = Environment.notificationSms;
        break;
      case 'email':
      default:
        endpoint = Environment.notificationEmail;
        break;
    }

    final response = await _apiService.post<Map<String, dynamic>>(
      endpoint,
      data: {
        'to': _recipientController.text.trim(),
        'subject': _subjectController.text.trim(),
        'message': _messageController.text.trim(),
      },
    );
    if (!mounted) return;
    setState(() => _isSending = false);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'নোটিফিকেশন পাঠানো হয়েছে!'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));
    if (response.success) {
      _recipientController.clear();
      _subjectController.clear();
      _messageController.clear();
      _loadAll();
    }
  }

  Future<void> _escalateNotification() async {
    if (_messageController.text.trim().isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('এসক্যালেশনের জন্য মেসেজ লিখুন'),
            backgroundColor: Colors.orange),
      );
      return;
    }

    setState(() => _isSending = true);

    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.notificationEscalate,
      data: {
        'message': _messageController.text.trim(),
        'level': 'CRITICAL',
      },
    );

    if (!mounted) return;
    setState(() => _isSending = false);

    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? 'ক্রিটিক্যাল অ্যালার্ট এসক্যালেট করা হয়েছে!'
          : 'ত্রুটি: ${response.error}'),
      backgroundColor: Color(response.success
          ? AppConstants.successColor
          : AppConstants.errorColor),
    ));

    if (response.success) {
      _messageController.clear();
      _loadAll();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('নোটিফিকেশন'),
        bottom: TabBar(
          controller: _tabController,
          isScrollable: true,
          tabs: const [
            Tab(icon: Icon(Icons.send), text: 'পাঠান'),
            Tab(icon: Icon(Icons.history), text: 'ইতিহাস'),
            Tab(icon: Icon(Icons.settings_input_component), text: 'চ্যানেল'),
            Tab(icon: Icon(Icons.account_balance_wallet), text: 'বাজেট (SMS)'),
          ],
        ),
        actions: [
          IconButton(
              icon: const Icon(Icons.refresh),
              tooltip: 'রিফ্ রেশ',
              onPressed: _loadAll)
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : TabBarView(
              controller: _tabController,
              children: [
                _buildSendTab(),
                _buildHistoryTab(),
                _buildChannelsTab(),
                _buildSmsBudgetTab(),
              ],
            ),
    );
  }

  Widget _buildSendTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingLarge),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('নোটিফিকেশন পাঠান',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const Text('(ইমেইল, স্ল্যাক বা ডিসকর্ডে মেসেজ পাঠান)',
                  style: TextStyle(fontSize: 12, color: Colors.grey)),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                initialValue: _selectedChannel,
                decoration: const InputDecoration(
                  labelText: 'চ্যানেল বেছে নিন',
                  helperText: '(কোন মাধ্যমে পাঠাবেন)',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(value: 'email', child: Text('ইমেইল')),
                  DropdownMenuItem(value: 'slack', child: Text('স্ল্যাক')),
                  DropdownMenuItem(value: 'discord', child: Text('ডিসকর্ড')),
                  DropdownMenuItem(value: 'sms', child: Text('এসএমএস (SMS)')),
                  DropdownMenuItem(value: 'escalate', child: Text('🚨 এসক্যালেট (Critical)')),
                ],
                onChanged: (v) =>
                    setState(() => _selectedChannel = v ?? 'email'),
              ),
              const SizedBox(height: 12),
              if (_selectedChannel != 'escalate')
                TextFormField(
                  controller: _recipientController,
                  decoration: const InputDecoration(
                    labelText: 'প্রাপক',
                    hintText: 'email@example.com / #channel-id',
                    helperText: '(যাকে পাঠাবেন তার ইমেইল বা আইডি)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.person),
                  ),
                ),
              if (_selectedChannel != 'escalate' && _selectedChannel != 'sms') ...[
                const SizedBox(height: 12),
                TextFormField(
                  controller: _subjectController,
                  decoration: const InputDecoration(
                    labelText: 'বিষয়',
                    helperText: '(মেসেজের শিরোনাম)',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.subject),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              TextFormField(
                controller: _messageController,
                maxLines: 4,
                decoration: InputDecoration(
                  labelText: _selectedChannel == 'escalate' ? 'অ্যালার্ট মেসেজ' : 'মেসেজ',
                  helperText: _selectedChannel == 'escalate' 
                      ? '(সব চ্যানেলে জরুরি এলার্ট যাবে)' 
                      : '(যা পাঠাতে চান তা লিখুন)',
                  border: const OutlineInputBorder(),
                  prefixIcon: const Icon(Icons.message),
                ),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _isSending 
                      ? null 
                      : (_selectedChannel == 'escalate' ? _escalateNotification : _sendNotification),
                  icon: _isSending
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : Icon(_selectedChannel == 'escalate' ? Icons.warning : Icons.send),
                  style: _selectedChannel == 'escalate' 
                      ? ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white)
                      : null,
                  label: Text(_isSending 
                      ? 'পাঠানো হচ্ছে...' 
                      : (_selectedChannel == 'escalate' ? 'জরুরি এসক্যালেট করুন' : 'পাঠান')),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHistoryTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _history.isEmpty
          ? const Center(
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                  Icon(Icons.notifications_none, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('কোনো নোটিফিকেশন ইতিহাস নেই'),
                ]))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _history.length,
              itemBuilder: (ctx, i) {
                final n = _history[i] is Map<String, dynamic>
                    ? _history[i] as Map<String, dynamic>
                    : <String, dynamic>{};
                final channel = '${n['channel'] ?? 'email'}';
                final to = '${n['to'] ?? n['recipient'] ?? ''}';
                final subject = '${n['subject'] ?? ''}';
                final status = '${n['status'] ?? 'SENT'}'.toUpperCase();
                final time = '${n['timestamp'] ?? n['sentAt'] ?? ''}';

                IconData channelIcon;
                switch (channel.toLowerCase()) {
                  case 'slack':
                    channelIcon = Icons.tag;
                    break;
                  case 'discord':
                    channelIcon = Icons.discord;
                    break;
                  case 'sms':
                    channelIcon = Icons.sms;
                    break;
                  default:
                    channelIcon = Icons.email;
                }

                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ListTile(
                    leading: CircleAvatar(child: Icon(channelIcon, size: 20)),
                    title: Text(subject.isEmpty ? to : subject,
                        maxLines: 1, overflow: TextOverflow.ellipsis),
                    subtitle: Text('$channel → $to\n$time',
                        style: const TextStyle(fontSize: 11)),
                    isThreeLine: true,
                    trailing: Chip(
                      label: Text(status, style: const TextStyle(fontSize: 10)),
                      backgroundColor:
                          (status == 'SENT' || status == 'DELIVERED'
                                  ? Colors.green
                                  : Colors.red)
                              .withValues(alpha: 0.1),
                      padding: EdgeInsets.zero,
                      visualDensity: VisualDensity.compact,
                    ),
                  ),
                );
              },
            ),
    );
  }

  Widget _buildSmsBudgetTab() {
    final dailyBudget = _smsBudget['dailyBudget'] ?? 0.0;
    final spentToday = _smsStats['spentToday'] ?? 0.0;
    final remaining = _smsStats['remainingBudget'] ?? 0.0;
    final percentUsed = _smsStats['percentUsed'] ?? 0.0;
    final messagesRemaining = _smsStats['messagesRemaining'] ?? 0;

    return RefreshIndicator(
      onRefresh: _loadAll,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          children: [
            _buildSmsStatsCard(spentToday, dailyBudget, percentUsed,
                remaining, messagesRemaining),
            const SizedBox(height: 16),
            _buildSmsBudgetForm(),
          ],
        ),
      ),
    );
  }

  Widget _buildSmsStatsCard(double spent, double budget, double percent,
      double remaining, int msgLeft) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('SMS বাজেট স্ট্যাটাস',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('আজকের খরচ:'),
                Text('\$${spent.toStringAsFixed(2)}',
                    style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('দৈনিক বাজেট:'),
                Text('\$${budget.toStringAsFixed(2)}',
                    style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: (percent / 100).clamp(0.0, 1.0),
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(
                  percent > 90 ? Colors.red : Colors.blue),
              minHeight: 10,
            ),
            const SizedBox(height: 8),
            Text('${percent.toStringAsFixed(1)}% ব্যবহৃত হয়েছে',
                style: const TextStyle(fontSize: 12, color: Colors.grey)),
            const Divider(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('অবশিষ্ট বাজেট:'),
                Text('\$${remaining.toStringAsFixed(2)}',
                    style: const TextStyle(
                        fontWeight: FontWeight.bold, color: Colors.green)),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('অবশিষ্ট মেসেজ (প্রায়):'),
                Text('$msgLeft টি',
                    style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSmsBudgetForm() {
    final budgetController = TextEditingController(
        text: _smsBudget['dailyBudget']?.toString() ?? '50.00');

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('বাজেট পরিবর্তন করুন',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            const Text('(অ্যাডমিন কেবল)',
                style: TextStyle(fontSize: 11, color: Colors.grey)),
            const SizedBox(height: 16),
            TextFormField(
              controller: budgetController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'নতুন দৈনিক বাজেট (\$)',
                hintText: '50.00',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.attach_money),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isBudgetLoading
                        ? null
                        : () async {
                            final newBudget =
                                double.tryParse(budgetController.text);
                            if (newBudget == null) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('সঠিক সংখ্যা লিখুন')));
                              return;
                            }
                            setState(() => _isBudgetLoading = true);
                            final resp = await _apiService.post(
                                Environment.notificationSmsBudgetSet,
                                data: {'dailyBudget': newBudget});
                            if (!mounted) return;
                            setState(() => _isBudgetLoading = false);
                            if (resp.success) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('বাজেট আপডেট হয়েছে')));
                              _loadAll();
                            }
                          },
                    child: const Text('বাজেট সেট করুন'),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: const Icon(Icons.refresh, color: Colors.orange),
                  tooltip: 'খরচ রিসেট করুন',
                  onPressed: () async {
                    final confirm = await showDialog<bool>(
                        context: context,
                        builder: (ctx) => AlertDialog(
                              title: const Text('রিসেট করবেন?'),
                              content: const Text(
                                  'আজকের SMS খরচ কি ০ করতে চান? এটি বাজেট ট্র্যাকার রিসেট করবে।'),
                              actions: [
                                TextButton(
                                    onPressed: () => Navigator.pop(ctx, false),
                                    child: const Text('না')),
                                TextButton(
                                    onPressed: () => Navigator.pop(ctx, true),
                                    child: const Text('হ্যাঁ')),
                              ],
                            ));
                    if (confirm == true) {
                      await _apiService
                          .post(Environment.notificationSmsBudgetReset);
                      _loadAll();
                    }
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildChannelsTab() {
    return RefreshIndicator(
      onRefresh: _loadAll,
      child: _channels.isEmpty
          ? const Center(
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                  Icon(Icons.settings_input_component,
                      size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('কোনো চ্যানেল কনফিগার করা নেই'),
                  Text('(ইমেইল, স্ল্যাক, ডিসকর্ড সেটআপ করুন)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                ]))
          : ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: _channels.length,
              itemBuilder: (ctx, i) {
                final ch = _channels[i] is Map<String, dynamic>
                    ? _channels[i] as Map<String, dynamic>
                    : <String, dynamic>{};
                final name = '${ch['name'] ?? ch['type'] ?? 'Unknown'}';
                final enabled = ch['enabled'] == true;

                return Card(
                  child: SwitchListTile(
                    secondary: CircleAvatar(child: Text(name[0].toUpperCase())),
                    title: Text(name),
                    subtitle: Text(enabled ? 'সক্রিয়' : 'নিষ্ক্রিয়',
                        style: TextStyle(
                            color: enabled ? Colors.green : Colors.grey)),
                    value: enabled,
                    onChanged: null, // Read-only for now
                  ),
                );
              },
            ),
    );
  }
}
