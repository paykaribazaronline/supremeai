import 'package:flutter/material.dart';
import '../../config/app_constants.dart';
import '../../services/api_service.dart';

class ChatHistoryScreen extends StatefulWidget {
  const ChatHistoryScreen({Key? key}) : super(key: key);

  @override
  State<ChatHistoryScreen> createState() => _ChatHistoryScreenState();
}

class _ChatHistoryScreenState extends State<ChatHistoryScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;
  late ScrollController _scrollController;

  List<dynamic> _chatMessages = [];
  List<dynamic> _workProcesses = [];
  bool _isLoading = true;
  bool _autoRefresh = true;
  String _filterType = 'all'; // all, errors, success, warnings
  String _searchText = '';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _scrollController = ScrollController();
    _loadAll();

    // Auto-refresh every 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted && _autoRefresh) {
        _loadAll();
      }
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _loadAll() async {
    if (!mounted) return;

    setState(() => _isLoading = true);

    // Fetch only last 5 messages to reduce Firebase quota usage
    final chatResults =
        await _apiService.get<List<dynamic>>('/api/admin/chat-history?limit=5');
    final processResults =
        await _apiService.get<List<dynamic>>('/api/admin/work-process');

    if (mounted) {
      setState(() {
        if (chatResults.success && chatResults.data != null) {
          // Keep only last 5 messages
          _chatMessages = (chatResults.data ?? []).length > 5
              ? (chatResults.data!).sublist((chatResults.data!).length - 5)
              : (chatResults.data ?? []);
        }
        if (processResults.success && processResults.data != null) {
          _workProcesses = processResults.data!;
        }
        _isLoading = false;
      });

      // Schedule next refresh
      Future.delayed(const Duration(seconds: 5), () {
        if (mounted && _autoRefresh) {
          _loadAll();
        }
      });
    }
  }

  Color _getStatusColor(String status) {
    switch (status.toLowerCase()) {
      case 'success':
        return Colors.green[400] ?? Colors.green;
      case 'failed':
      case 'error':
        return Colors.red[400] ?? Colors.red;
      case 'warning':
        return Colors.orange[400] ?? Colors.orange;
      case 'running':
      case 'pending':
        return Colors.blue[400] ?? Colors.blue;
      default:
        return Colors.grey[400] ?? Colors.grey;
    }
  }

  Color _getSenderColor(String sender) {
    switch (sender.toLowerCase()) {
      case 'admin':
        return Colors.purple[400] ?? Colors.purple;
      case 'system':
        return Colors.cyan[400] ?? Colors.cyan;
      case 'user':
        return Colors.blue[400] ?? Colors.blue;
      default:
        return Colors.grey[400] ?? Colors.grey;
    }
  }

  List<dynamic> _getFilteredMessages() {
    return _chatMessages.where((msg) {
      final msgType = msg['type']?.toString().toLowerCase() ?? 'info';
      final msgContent = msg['message']?.toString().toLowerCase() ?? '';

      // Filter by type
      if (_filterType == 'errors' && msgType != 'error') return false;
      if (_filterType == 'success' && msgType != 'success') return false;
      if (_filterType == 'warnings' && msgType != 'warning') return false;

      // Filter by search
      if (_searchText.isNotEmpty &&
          !msgContent.contains(_searchText.toLowerCase())) {
        return false;
      }

      return true;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filteredMessages = _getFilteredMessages();

    return Scaffold(
      appBar: AppBar(
        title: const Text('💬 কাজের প্রক্রিয়া ও চ্যাট ইতিহাস'),
        bottom: TabBar(
          controller: _tabController,
          tabs: [
            Tab(text: 'চ্যাট (${filteredMessages.length}/5 - অপ্টিমাইজড)'),
            Tab(text: 'প্রক্রিয়া (${_workProcesses.length})'),
            const Tab(text: 'সময়সীমা'),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'রিফ্রেশ করুন',
            onPressed: _loadAll,
          ),
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Center(
              child: Text(
                _autoRefresh ? '🟢 লাইভ' : '⏸ ধরে রাখা',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  color: _autoRefresh ? Colors.green : Colors.orange,
                ),
              ),
            ),
          ),
        ],
      ),
      body: _isLoading && _chatMessages.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // Controls
                Padding(
                  padding: const EdgeInsets.all(AppConstants.paddingMedium),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        SizedBox(
                          width: 200,
                          child: TextField(
                            decoration: InputDecoration(
                              hintText: 'অনুসন্ধান করুন...',
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                              isDense: true,
                              contentPadding: const EdgeInsets.symmetric(
                                horizontal: AppConstants.paddingSmall,
                                vertical: AppConstants.paddingSmall,
                              ),
                            ),
                            onChanged: (val) {
                              setState(() => _searchText = val);
                            },
                          ),
                        ),
                        const SizedBox(width: 8),
                        DropdownButton<String>(
                          value: _filterType,
                          items: const [
                            DropdownMenuItem(value: 'all', child: Text('সব')),
                            DropdownMenuItem(
                                value: 'errors', child: Text('শুধু ত্রুটি')),
                            DropdownMenuItem(
                                value: 'success', child: Text('শুধু সফল')),
                            DropdownMenuItem(
                                value: 'warnings', child: Text('শুধু সতর্কতা')),
                          ],
                          onChanged: (val) {
                            if (val != null) {
                              setState(() => _filterType = val);
                            }
                          },
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed: () {
                            setState(() => _autoRefresh = !_autoRefresh);
                          },
                          style: ElevatedButton.styleFrom(
                            backgroundColor:
                                _autoRefresh ? Colors.green : Colors.grey,
                          ),
                          child: Text(
                            _autoRefresh ? '🔄 চলছে' : '⏸ বন্ধ',
                            style: const TextStyle(color: Colors.white),
                          ),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed: () {
                            setState(() {
                              _searchText = '';
                              _filterType = 'all';
                            });
                          },
                          child: const Text('পরিষ্কার'),
                        ),
                      ],
                    ),
                  ),
                ),
                const Divider(),
                // Tabs
                Expanded(
                  child: TabBarView(
                    controller: _tabController,
                    children: [
                      // Chat Tab
                      _buildChatTab(filteredMessages),
                      // Process Tab
                      _buildProcessTab(),
                      // Timeline Tab
                      _buildTimelineTab(),
                    ],
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildChatTab(List<dynamic> messages) {
    if (messages.isEmpty) {
      return const Center(
        child: Text('কোনো বার্তা নেই'),
      );
    }

    return Column(
      children: [
        Container(
          color: Colors.blue[50],
          padding: const EdgeInsets.all(AppConstants.paddingSmall),
          child: const Row(
            children: [
              Icon(Icons.info_outline, color: Colors.blue),
              SizedBox(width: 8),
              Expanded(
                child: Text(
                  'শেষ ৫টি বার্তা দেখানো হচ্ছে (Firebase কোটা সংরক্ষণ)',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView.builder(
            controller: _scrollController,
            itemCount: messages.length,
            itemBuilder: (context, index) {
              final msg = messages[index];
              final timestamp = msg['timestamp'] ?? '';
              final sender =
                  msg['sender']?.toString().toUpperCase() ?? 'UNKNOWN';
              final type = msg['type']?.toString().toUpperCase() ?? 'INFO';
              final message = msg['message'] ?? '';
              final projectId = msg['projectId'] as String?;

              return Card(
                margin: const EdgeInsets.symmetric(
                  horizontal: AppConstants.paddingSmall,
                  vertical: AppConstants.paddingSmall,
                ),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: _getSenderColor(sender),
                    child: Text(
                      sender[0],
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                  title: Row(
                    children: [
                      Chip(
                        label: Text(sender),
                        backgroundColor: _getSenderColor(sender),
                        labelStyle: const TextStyle(color: Colors.white),
                      ),
                      const SizedBox(width: 8),
                      Chip(
                        label: Text(type),
                        backgroundColor: _getStatusColor(type),
                        labelStyle: const TextStyle(color: Colors.white),
                      ),
                    ],
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 8),
                      Text(message),
                      if (projectId != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4.0),
                          child: Text(
                            'প্রকল্প: $projectId',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey[600],
                            ),
                          ),
                        ),
                      Padding(
                        padding: const EdgeInsets.only(top: 4.0),
                        child: Text(
                          timestamp,
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey[500],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildProcessTab() {
    if (_workProcesses.isEmpty) {
      return const Center(
        child: Text('কোনো প্রক্রিয়া নেই'),
      );
    }

    return ListView.builder(
      itemCount: _workProcesses.length,
      itemBuilder: (context, index) {
        final process = _workProcesses[index];
        final eventType = process['eventType'] ?? '';
        final status = process['status']?.toString().toUpperCase() ?? 'UNKNOWN';
        final duration = process['duration'] ?? 0;
        final details = process['details'] ?? '';
        final projectId = process['projectId'] ?? '';

        return Card(
          margin: const EdgeInsets.symmetric(
            horizontal: AppConstants.paddingSmall,
            vertical: AppConstants.paddingSmall,
          ),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: _getStatusColor(status),
              child: Icon(
                status == 'SUCCESS'
                    ? Icons.check_circle
                    : status == 'FAILED'
                        ? Icons.error
                        : Icons.hourglass_empty,
                color: Colors.white,
              ),
            ),
            title: Text(eventType),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text(details),
                Padding(
                  padding: const EdgeInsets.only(top: 4.0),
                  child: Row(
                    children: [
                      Chip(
                        label: Text(status),
                        backgroundColor: _getStatusColor(status),
                        labelStyle: const TextStyle(color: Colors.white),
                        visualDensity: VisualDensity.compact,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '⏱ ${duration}ms',
                        style: const TextStyle(fontSize: 12),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '📁 $projectId',
                        style: const TextStyle(fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildTimelineTab() {
    final allEvents = [
      ..._chatMessages.map((msg) => {
            'type': 'chat',
            'timestamp': msg['timestamp'],
            'title': msg['sender'],
            'content': msg['message'],
            'status': msg['type'],
          }),
      ..._workProcesses.map((proc) => {
            'type': 'process',
            'timestamp': proc['timestamp'],
            'title': proc['eventType'],
            'content': proc['details'],
            'status': proc['status'],
          }),
    ];

    // Sort by timestamp (most recent first)
    allEvents.sort((a, b) {
      final aTime = DateTime.parse(a['timestamp']?.toString() ?? '');
      final bTime = DateTime.parse(b['timestamp']?.toString() ?? '');
      return bTime.compareTo(aTime);
    });

    if (allEvents.isEmpty) {
      return const Center(
        child: Text('কোনো ইভেন্ট নেই'),
      );
    }

    return ListView.builder(
      itemCount: allEvents.length,
      itemBuilder: (context, index) {
        final event = allEvents[index];
        final isChat = event['type'] == 'chat';

        return Container(
          margin: const EdgeInsets.symmetric(
            horizontal: AppConstants.paddingMedium,
            vertical: AppConstants.paddingSmall,
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Column(
                children: [
                  CircleAvatar(
                    radius: 12,
                    backgroundColor:
                        _getStatusColor(event['status']?.toString() ?? ''),
                    child: Icon(
                      isChat ? Icons.chat : Icons.settings,
                      size: 16,
                      color: Colors.white,
                    ),
                  ),
                  if (index < allEvents.length - 1)
                    Container(
                      width: 2,
                      height: 40,
                      color: Colors.grey[300],
                    ),
                ],
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(AppConstants.paddingSmall),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          event['title']?.toString() ?? '',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Text(event['content']?.toString() ?? ''),
                        Padding(
                          padding: const EdgeInsets.only(top: 4.0),
                          child: Text(
                            event['timestamp']?.toString() ?? '',
                            style: TextStyle(
                              fontSize: 11,
                              color: Colors.grey[600],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
