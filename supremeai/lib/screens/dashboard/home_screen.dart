import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/orchestration_provider.dart';
import '../../providers/auth_provider.dart';
import '../settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  final TextEditingController _chatController = TextEditingController();
  final List<Map<String, dynamic>> _messages = [];

  void _sendMessage() {
    if (_chatController.text.trim().isEmpty) return;

    final userMessage = _chatController.text.trim();
    setState(() {
      _messages.add({'role': 'user', 'content': userMessage});
    });
    _chatController.clear();

    // Auto-detect orchestration need and process
    _processWithAI(userMessage);
  }

  Future<void> _processWithAI(String message) async {
    final orchestration = context.read<OrchestrationProvider>();
    final auth = context.read<AuthProvider>();

    if (auth.token == null) return;

    // AI automatically detects if orchestration is needed
    await orchestration.orchestrateRequirement(message, auth.token!);

    final result = orchestration.lastResult;
    if (result != null && mounted) {
      setState(() {
        if (result['status'] == 'DECIDED' || result['status'] == 'COMPLETED') {
          final mode = result['mode'] ?? 'code';
          _messages.add({
            'role': 'ai',
            'content': 'I\'ve analyzed your requirement using the **${mode.toString().toUpperCase()}** mode and created a project plan. Tap "Generate" to start building.',
            'action': 'generate',
            'mode': mode,
          });
        } else {
          _messages.add({
            'role': 'ai',
            'content': result.toString(),
          });
        }
      });
    }
  }

  void _generateProject() {
    final auth = context.read<AuthProvider>();
    final orchestration = context.read<OrchestrationProvider>();
    if (auth.token != null) {
      orchestration.generateProject(auth.token!);
    }
  }

  @override
  void dispose() {
    _chatController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final orchestration = context.watch<OrchestrationProvider>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('SupremeAI'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AuthProvider>().logout(),
            tooltip: 'Logout',
          ),
        ],
      ),
      body: _currentIndex == 0 ? _buildChatInterface(orchestration) : const SettingsScreen(),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) => setState(() => _currentIndex = index),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.chat),
            label: 'Chat',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }

  Widget _buildChatInterface(OrchestrationProvider orchestration) {
    return Column(
      children: [
        Expanded(
          child: _messages.isEmpty
              ? const Center(
                  child: Text(
                    'Describe what you want to build...\nAI will handle everything automatically.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 16, color: Colors.grey),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _messages.length,
                  itemBuilder: (context, index) {
                    final msg = _messages[index];
                    final isUser = msg['role'] == 'user';
                    return Align(
                      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                      child: Container(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: isUser ? Colors.blue[100] : Colors.grey[200],
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(msg['content']),
                            if (msg['action'] == 'generate') ...[
                              const SizedBox(height: 8),
                              ElevatedButton.icon(
                                onPressed: orchestration.isLoading ? null : _generateProject,
                                icon: const Icon(Icons.rocket_launch),
                                label: const Text('Generate Project'),
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: Colors.orange,
                                  foregroundColor: Colors.white,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
        if (orchestration.isLoading)
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                SizedBox(width: 8),
                Text('AI is processing...'),
              ],
            ),
          ),
        if (orchestration.error != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Text(
              'Error: ${orchestration.error}',
              style: const TextStyle(color: Colors.red),
            ),
          ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _chatController,
                  decoration: const InputDecoration(
                    hintText: 'Enter your requirement...',
                    border: OutlineInputBorder(),
                  ),
                  maxLines: null,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => _sendMessage(),
                ),
              ),
              const SizedBox(width: 8),
              IconButton(
                onPressed: orchestration.isLoading ? null : _sendMessage,
                icon: const Icon(Icons.send),
                style: IconButton.styleFrom(
                  backgroundColor: Colors.blue,
                  foregroundColor: Colors.white,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
