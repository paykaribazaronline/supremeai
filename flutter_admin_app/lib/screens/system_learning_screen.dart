import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

/// SystemLearningScreen - Flutter Mobile Version
///
/// Shows learned patterns and allows executing work autonomously
class SystemLearningScreen extends StatefulWidget {
  const SystemLearningScreen({Key? key}) : super(key: key);

  @override
  State<SystemLearningScreen> createState() => _SystemLearningScreenState();
}

class _SystemLearningScreenState extends State<SystemLearningScreen> {
  List<Map<String, dynamic>> patterns = [];
  Map<String, dynamic>? stats;
  bool loading = true;
  int selectedPatternIndex = -1;
  final TextEditingController requirementController = TextEditingController();
  final TextEditingController frameworkController = TextEditingController();
  final TextEditingController branchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadPatterns();
    _loadStats();
    // Auto-refresh every 5 seconds
    Future.delayed(const Duration(seconds: 5), _refreshData);
  }

  Future<void> _loadPatterns() async {
    try {
      final response = await http
          .get(
            Uri.parse(
                'https://supremeai-565236080752.us-central1.run.app/api/teach/patterns'),
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          patterns = List<Map<String, dynamic>>.from(data['patterns'] ?? []);
          loading = false;
        });
      }
    } catch (e) {
      print('Failed to load patterns: $e');
      setState(() => loading = false);
    }
  }

  Future<void> _loadStats() async {
    try {
      final response = await http
          .get(
            Uri.parse(
                'https://supremeai-565236080752.us-central1.run.app/api/teach/stats'),
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          stats = data;
        });
      }
    } catch (e) {
      print('Failed to load stats: $e');
    }
  }

  Future<void> _refreshData() async {
    await _loadPatterns();
    await _loadStats();
    Future.delayed(const Duration(seconds: 5), _refreshData);
  }

  Future<void> _executePattern(Map<String, dynamic> pattern) async {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Execute Pattern: ${pattern['name']}'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Action Sequence:',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 8),
              ...(pattern['actions'] as List).asMap().entries.map((e) => Text(
                    '${e.key + 1}. ${e.value}',
                  )),
              const SizedBox(height: 16),
              TextField(
                controller: requirementController,
                decoration: const InputDecoration(
                  labelText: 'Requirement',
                  hintText: 'What do you want?',
                  border: OutlineInputBorder(),
                ),
                maxLines: 2,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: frameworkController,
                decoration: const InputDecoration(
                  labelText: 'Framework',
                  hintText: 'React, Flutter, Spring Boot',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: branchController,
                decoration: const InputDecoration(
                  labelText: 'Target Branch',
                  hintText: 'main',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(ctx);
              await _doExecutePattern(pattern);
            },
            child: const Text('🚀 Execute'),
          ),
        ],
      ),
    );
  }

  Future<void> _doExecutePattern(Map<String, dynamic> pattern) async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('⚡ Executing pattern: ${pattern['name']}...')),
      );

      final response = await http
          .post(
            Uri.parse(
                'https://supremeai-565236080752.us-central1.run.app/api/teach/execute'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'pattern': pattern['name'],
              'inputs': {
                'requirement': requirementController.text,
                'framework': frameworkController.text,
                'targetBranch': branchController.text,
              }
            }),
          )
          .timeout(const Duration(seconds: 30));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('✓ Pattern executed: ${pattern['name']}'),
              backgroundColor: Colors.green,
            ),
          );
          requirementController.clear();
          frameworkController.clear();
          branchController.clear();
          _refreshData();
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Failed: ${jsonDecode(response.body)['error']}'),
              backgroundColor: Colors.red,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.psychology, color: Colors.amber),
            SizedBox(width: 8),
            Text('System Learning'),
          ],
        ),
        backgroundColor: Colors.deepPurple,
        elevation: 2,
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _refreshData,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Stats Cards
                      if (stats != null) ...[
                        Card(
                          elevation: 2,
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Learning Progress',
                                  style: Theme.of(context)
                                      .textTheme
                                      .titleMedium
                                      ?.copyWith(
                                        fontWeight: FontWeight.bold,
                                      ),
                                ),
                                const SizedBox(height: 12),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceAround,
                                  children: [
                                    Column(
                                      children: [
                                        Text(
                                          '${stats!['patternsLearned'] ?? 0}',
                                          style: const TextStyle(
                                            fontSize: 24,
                                            fontWeight: FontWeight.bold,
                                            color: Colors.deepPurple,
                                          ),
                                        ),
                                        const Text(
                                          'Patterns',
                                          style: TextStyle(color: Colors.grey),
                                        ),
                                      ],
                                    ),
                                    Column(
                                      children: [
                                        Text(
                                          '${((stats!['patternsLearned'] ?? 0) * 20).toInt()}%',
                                          style: const TextStyle(
                                            fontSize: 24,
                                            fontWeight: FontWeight.bold,
                                            color: Colors.green,
                                          ),
                                        ),
                                        const Text(
                                          'Autonomy',
                                          style: TextStyle(color: Colors.grey),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: Colors.blue[50],
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    'শেষ ৫টি প্যাটার্ন দেখানো হচ্ছে (Firebase কোটা সংরক্ষণ)',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: Colors.blue[900],
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(height: 20),
                      ],

                      // Patterns List
                      Text(
                        'Learned Patterns',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      const SizedBox(height: 12),
                      if (patterns.isEmpty)
                        const Center(
                          child: Padding(
                            padding: EdgeInsets.all(32),
                            child: Column(
                              children: [
                                Icon(Icons.school,
                                    size: 48, color: Colors.grey),
                                SizedBox(height: 12),
                                Text('No patterns learned yet'),
                                Text(
                                  'Keep working, I\'m learning your patterns! 🧠',
                                  style: TextStyle(color: Colors.grey),
                                ),
                              ],
                            ),
                          ),
                        )
                      else
                        ...patterns
                            .map((pattern) => Card(
                                  margin: const EdgeInsets.only(bottom: 12),
                                  child: ListTile(
                                    title: Text(
                                      pattern['name'] ?? 'Unknown',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold),
                                    ),
                                    subtitle: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        const SizedBox(height: 8),
                                        Wrap(
                                          spacing: 4,
                                          children:
                                              (pattern['actions'] as List?)
                                                      ?.map((action) => Chip(
                                                            label: Text(
                                                              action,
                                                              style:
                                                                  const TextStyle(
                                                                      fontSize:
                                                                          11),
                                                            ),
                                                            visualDensity:
                                                                VisualDensity
                                                                    .compact,
                                                          ))
                                                      .toList() ??
                                                  [],
                                        ),
                                        const SizedBox(height: 8),
                                        Row(
                                          children: [
                                            Chip(
                                              label: Text(
                                                  'Used: ${pattern['frequency']}'),
                                              avatar: const Icon(Icons.repeat,
                                                  size: 16),
                                            ),
                                            const SizedBox(width: 8),
                                            Chip(
                                              label: Text(
                                                  '${pattern['confidence']}'),
                                              avatar: const Icon(
                                                  Icons.trending_up,
                                                  size: 16),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                    trailing: ElevatedButton.icon(
                                      onPressed: () => _executePattern(pattern),
                                      icon: const Icon(Icons.rocket),
                                      label: const Text('Run'),
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: Colors.deepOrange,
                                      ),
                                    ),
                                  ),
                                ))
                            .toList(),

                      const SizedBox(height: 40),

                      // How It Works
                      Card(
                        elevation: 2,
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '🚀 How It Works',
                                style: Theme.of(context)
                                    .textTheme
                                    .titleMedium
                                    ?.copyWith(
                                      fontWeight: FontWeight.bold,
                                    ),
                              ),
                              const SizedBox(height: 12),
                              const Text(
                                '1. Recording: System records every action you take\n'
                                '2. Pattern Recognition: Extracts reusable work patterns\n'
                                '3. Learning: Stores patterns with confidence scores\n'
                                '4. Execution: Run entire patterns with one tap! ⚡',
                                style: TextStyle(height: 1.6),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
    );
  }

  @override
  void dispose() {
    requirementController.dispose();
    frameworkController.dispose();
    branchController.dispose();
    super.dispose();
  }
}
