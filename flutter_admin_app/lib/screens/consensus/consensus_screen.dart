import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class ConsensusScreen extends StatefulWidget {
  const ConsensusScreen({Key? key}) : super(key: key);

  @override
  State<ConsensusScreen> createState() => _ConsensusScreenState();
}

class _ConsensusScreenState extends State<ConsensusScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;
  final TextEditingController _questionController = TextEditingController();

  Map<String, dynamic>? _stats;
  List<dynamic> _history = [];
  Map<String, dynamic>? _lastResult;
  bool _isLoading = true;
  bool _isAsking = false;
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
    _questionController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    final results = await Future.wait([
      _apiService.get<Map<String, dynamic>>(Environment.consensusStats),
      _apiService.get<List<dynamic>>(Environment.consensusHistory),
    ]);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (results[0].success) _stats = results[0].data as Map<String, dynamic>?;
      if (results[1].success)
        _history = (results[1].data as List<dynamic>?) ?? [];
      if (!results[0].success && !results[1].success)
        _error = 'তথ্য লোড করা যায়নি';
    });
  }

  Future<void> _askConsensus() async {
    final question = _questionController.text.trim();
    if (question.isEmpty) return;

    setState(() => _isAsking = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.consensusAsk,
      data: {'question': question},
    );
    if (!mounted) return;
    setState(() {
      _isAsking = false;
      if (response.success) {
        _lastResult = response.data;
        _questionController.clear();
        _tabController.animateTo(1);
      }
    });
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success
          ? '১০টি AI থেকে উত্তর পাওয়া গেছে!'
          : 'ত্রুটি: ${response.error}'),
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
        title: const Text('মাল্টি-AI সিদ্ধান্ত'),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.question_answer), text: 'প্রশ্ন করুন'),
            Tab(icon: Icon(Icons.how_to_vote), text: 'ভোটের ফলাফল'),
            Tab(icon: Icon(Icons.bar_chart), text: 'পরিসংখ্যান'),
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
              children: [_buildAskTab(), _buildResultsTab(), _buildStatsTab()],
            ),
    );
  }

  Widget _buildAskTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(children: [
                    Icon(Icons.groups, color: Color(AppConstants.primaryColor)),
                    SizedBox(width: 8),
                    Text('১০টি AI-কে প্রশ্ন করুন',
                        style: TextStyle(
                            fontSize: 18, fontWeight: FontWeight.bold)),
                  ]),
                  const Text(
                      '(একসাথে ১০টি AI থেকে উত্তর নিয়ে সেরা সিদ্ধান্ত বের করে)',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _questionController,
                    maxLines: 4,
                    decoration: const InputDecoration(
                      labelText: 'আপনার প্রশ্ন লিখুন',
                      helperText:
                          '(যেকোনো কোডিং, আর্কিটেকচার বা প্রযুক্তি সম্পর্কিত প্রশ্ন করুন)',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.edit),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: _isAsking ? null : _askConsensus,
                      icon: _isAsking
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.send),
                      label: Text(_isAsking
                          ? '১০টি AI ভাবছে...'
                          : 'সব AI-কে জিজ্ঞেস করো'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          const Text('AI প্রোভাইডার তালিকা:',
              style: TextStyle(fontWeight: FontWeight.w600)),
          const Text('(এই ১০টি AI একসাথে কাজ করে)',
              style: TextStyle(fontSize: 11, color: Colors.grey)),
          const SizedBox(height: 8),
          Wrap(spacing: 6, runSpacing: 6, children: [
            for (final name in [
              'OpenAI',
              'Anthropic',
              'Google',
              'Meta',
              'Mistral',
              'Cohere',
              'HuggingFace',
              'xAI',
              'DeepSeek',
              'Perplexity'
            ])
              Chip(
                  label: Text(name, style: const TextStyle(fontSize: 12)),
                  avatar: const Icon(Icons.smart_toy, size: 16)),
          ]),
        ],
      ),
    );
  }

  Widget _buildResultsTab() {
    if (_lastResult == null && _history.isEmpty) {
      return const Center(
          child: Text('এখনো কোনো ভোটের ফলাফল নেই\n(প্রথমে একটি প্রশ্ন করুন)',
              textAlign: TextAlign.center));
    }

    final items = _lastResult != null ? [_lastResult!, ..._history] : _history;

    return RefreshIndicator(
      onRefresh: _loadAll,
      child: ListView.builder(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        itemCount: items.length,
        itemBuilder: (ctx, i) {
          final map = items[i] is Map<String, dynamic>
              ? items[i] as Map<String, dynamic>
              : <String, dynamic>{};
          final question = '${map['question'] ?? 'Unknown'}';
          final consensus = '${map['consensus'] ?? map['result'] ?? 'N/A'}';
          final confidence =
              (map['confidence'] ?? map['approvalRate'] ?? 0).toString();
          final votes = map['votes'];

          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ExpansionTile(
              leading: CircleAvatar(
                backgroundColor:
                    const Color(AppConstants.primaryColor).withOpacity(0.1),
                child: const Icon(Icons.how_to_vote,
                    color: Color(AppConstants.primaryColor)),
              ),
              title:
                  Text(question, maxLines: 2, overflow: TextOverflow.ellipsis),
              subtitle: Text('আত্মবিশ্বাস: $confidence%'),
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('সম্মিলিত উত্তর:',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text(consensus),
                      if (votes is List) ...[
                        const SizedBox(height: 12),
                        const Text('AI ভোট:',
                            style: TextStyle(fontWeight: FontWeight.bold)),
                        ...votes.map((v) {
                          final vm = v is Map<String, dynamic>
                              ? v
                              : <String, dynamic>{};
                          return ListTile(
                            dense: true,
                            leading: Icon(Icons.smart_toy,
                                size: 18, color: Colors.grey.shade600),
                            title: Text(
                                '${vm['agentName'] ?? vm['provider'] ?? ''}',
                                style: const TextStyle(fontSize: 13)),
                            subtitle: Text(
                                '${vm['reasoning'] ?? vm['response'] ?? ''}',
                                style: const TextStyle(fontSize: 11)),
                            trailing: Text('${vm['confidence'] ?? ''}%',
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold)),
                          );
                        }),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildStatsTab() {
    final stats = _stats ?? {};
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        children: [
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 8,
            crossAxisSpacing: 8,
            children: [
              _buildStatCard('মোট প্রশ্ন', '${stats['totalQuestions'] ?? 0}',
                  Icons.question_answer, AppConstants.primaryColor),
              _buildStatCard(
                  'গড় আত্মবিশ্বাস',
                  '${stats['averageConfidence'] ?? 0}%',
                  Icons.trending_up,
                  AppConstants.successColor),
              _buildStatCard('সক্রিয় AI', '${stats['activeProviders'] ?? 10}',
                  Icons.smart_toy, AppConstants.infoColor),
              _buildStatCard('সফল ভোট', '${stats['successfulVotes'] ?? 0}',
                  Icons.check_circle, AppConstants.successColor),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, int color) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [
            Color(color).withOpacity(0.1),
            Color(color).withOpacity(0.05)
          ]),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: Color(color), size: 22),
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(value,
                    style: const TextStyle(
                        fontSize: 22, fontWeight: FontWeight.bold)),
                Text(title,
                    style: const TextStyle(fontSize: 11, color: Colors.grey)),
              ]),
            ],
          ),
        ),
      ),
    );
  }
}
