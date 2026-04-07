import 'dart:async';
import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../services/on_device_llm_service.dart';

/// Chat message model for the offline AI chat.
class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime timestamp;
  final bool isStreaming;

  ChatMessage({
    required this.text,
    required this.isUser,
    DateTime? timestamp,
    this.isStreaming = false,
  }) : timestamp = timestamp ?? DateTime.now();

  ChatMessage copyWith({String? text, bool? isStreaming}) {
    return ChatMessage(
      text: text ?? this.text,
      isUser: isUser,
      timestamp: timestamp,
      isStreaming: isStreaming ?? this.isStreaming,
    );
  }
}

/// Offline AI Chat screen — on-device LLM, no internet needed after download.
class OfflineChatScreen extends StatefulWidget {
  const OfflineChatScreen({Key? key}) : super(key: key);

  @override
  State<OfflineChatScreen> createState() => _OfflineChatScreenState();
}

class _OfflineChatScreenState extends State<OfflineChatScreen>
    with SingleTickerProviderStateMixin {
  final OnDeviceLlmService _llmService = OnDeviceLlmService();
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final FocusNode _focusNode = FocusNode();

  late TabController _tabController;

  final List<ChatMessage> _messages = [];
  bool _isGenerating = false;
  bool _isLoadingModel = false;
  String? _selectedModelId;

  // Dynamic model catalog (fetched from cloud, NOT hardcoded)
  List<Map<String, String>> _availableModels = [];
  bool _isLoadingModels = true;

  // Model management state
  final Map<String, bool> _downloadedModels = {};
  final Map<String, double> _downloadProgress = {};

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadModelCatalog();
  }

  Future<void> _loadModelCatalog() async {
    final models = await OnDeviceLlmService.getAvailableModels();
    if (mounted) {
      setState(() {
        _availableModels = models;
        _isLoadingModels = false;
      });
      _checkDownloadedModels();
    }
  }

  @override
  void dispose() {
    _inputController.dispose();
    _scrollController.dispose();
    _focusNode.dispose();
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _checkDownloadedModels() async {
    for (final model in _availableModels) {
      final id = model['id']!;
      final downloaded = await _llmService.isModelDownloaded(id);
      if (mounted) {
        setState(() => _downloadedModels[id] = downloaded);
      }
    }
  }

  Future<void> _downloadModel(String modelId, String url) async {
    setState(() => _downloadProgress[modelId] = 0.0);

    final success = await _llmService.downloadModel(
      modelId,
      url,
      onProgress: (progress) {
        if (mounted) {
          setState(() => _downloadProgress[modelId] = progress);
        }
      },
    );

    if (mounted) {
      setState(() {
        _downloadProgress.remove(modelId);
        _downloadedModels[modelId] = success;
      });

      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('মডেল ডাউনলোড সফল! ✅'),
            backgroundColor: Color(AppConstants.successColor),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('মডেল ডাউনলোড ব্যর্থ হয়েছে ❌'),
            backgroundColor: Color(AppConstants.errorColor),
          ),
        );
      }
    }
  }

  Future<void> _loadAndStartChat(String modelId) async {
    setState(() => _isLoadingModel = true);

    final loaded = await _llmService.loadModel(modelId);

    if (mounted) {
      setState(() {
        _isLoadingModel = false;
        if (loaded) {
          _selectedModelId = modelId;
          _tabController.animateTo(0); // Switch to chat tab
          _messages.add(ChatMessage(
            text: '🤖 মডেল "$modelId" লোড হয়েছে।\n\n'
                'আমি এখন সম্পূর্ণ অফলাইনে আপনার ফোনে চলছি — '
                'কোনো ইন্টারনেট বা API দরকার নেই!\n\n'
                'আমাকে যেকোনো প্রশ্ন করুন।',
            isUser: false,
          ));
        }
      });

      if (!loaded) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('মডেল লোড করা যায়নি। ডিভাইসে পর্যাপ্ত RAM আছে কি?'),
            backgroundColor: Color(AppConstants.errorColor),
          ),
        );
      }
    }
  }

  Future<void> _deleteModel(String modelId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('মডেল মুছে ফেলবেন?'),
        content: const Text('এটা ডাউনলোড করা ফাইল মুছে ফেলবে। আবার ব্যবহার করতে হলে নতুন করে ডাউনলোড করতে হবে।'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('না')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: Color(AppConstants.errorColor)),
            child: const Text('হ্যাঁ, মুছুন'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    final deleted = await _llmService.deleteModel(modelId);
    if (mounted) {
      setState(() {
        _downloadedModels[modelId] = !deleted;
        if (modelId == _selectedModelId) _selectedModelId = null;
      });
    }
  }

  Future<void> _sendMessage() async {
    final text = _inputController.text.trim();
    if (text.isEmpty || _isGenerating) return;

    if (!_llmService.isModelLoaded) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('আগে "মডেল" ট্যাব থেকে একটি মডেল লোড করুন।'),
          backgroundColor: Color(AppConstants.warningColor),
        ),
      );
      return;
    }

    _inputController.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true));
      _messages.add(ChatMessage(text: '', isUser: false, isStreaming: true));
      _isGenerating = true;
    });
    _scrollToBottom();

    try {
      final buffer = StringBuffer();
      await for (final token in _llmService.generateResponseStream(text)) {
        buffer.write(token);
        if (mounted) {
          setState(() {
            _messages.last = _messages.last.copyWith(text: buffer.toString());
          });
          _scrollToBottom();
        }
      }
    } catch (e) {
      // Fallback to non-streaming
      final response = await _llmService.generateResponse(text);
      if (mounted) {
        setState(() {
          _messages.last = _messages.last.copyWith(text: response);
        });
      }
    }

    if (mounted) {
      setState(() {
        _messages.last = _messages.last.copyWith(isStreaming: false);
        _isGenerating = false;
      });
      _scrollToBottom();
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _clearChat() {
    setState(() => _messages.clear());
  }

  // ─── BUILD ───────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('অফলাইন AI চ্যাট'),
            Text(
              _selectedModelId != null
                  ? '🟢 ${_selectedModelId!} — ইন্টারনেট ছাড়া'
                  : '⚪ কোনো মডেল লোড হয়নি',
              style: const TextStyle(fontSize: 11, fontWeight: FontWeight.normal),
            ),
          ],
        ),
        actions: [
          if (_messages.isNotEmpty)
            IconButton(
              icon: const Icon(Icons.delete_sweep),
              tooltip: 'চ্যাট মুছুন',
              onPressed: _clearChat,
            ),
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.chat_bubble_outline), text: 'চ্যাট'),
            Tab(icon: Icon(Icons.download_rounded), text: 'মডেল'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildChatTab(),
          _buildModelTab(),
        ],
      ),
    );
  }

  // ─── CHAT TAB ────────────────────────────────────────────────────────────

  Widget _buildChatTab() {
    if (!_llmService.isModelLoaded) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingXLarge),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.smart_toy_outlined, size: 80, color: Colors.grey.shade400),
              const SizedBox(height: AppConstants.paddingLarge),
              const Text(
                'অফলাইন AI চ্যাট',
                style: TextStyle(
                  fontSize: AppConstants.titleFontSize,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: AppConstants.paddingSmall),
              const Text(
                'এই AI মডেল আপনার ফোনে চলবে।\n'
                'একবার ডাউনলোড হলে ইন্টারনেট বা API কী দরকার নেই!\n\n'
                'প্রথমে "মডেল" ট্যাবে যান → মডেল ডাউনলোড করুন → লোড করুন।',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey, height: 1.5),
              ),
              const SizedBox(height: AppConstants.paddingXLarge),
              ElevatedButton.icon(
                icon: const Icon(Icons.download_rounded),
                label: const Text('মডেল ট্যাবে যান'),
                onPressed: () => _tabController.animateTo(1),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: [
        Expanded(
          child: _messages.isEmpty
              ? Center(
                  child: Text(
                    'আমাকে কিছু জিজ্ঞেস করুন! 🤖',
                    style: TextStyle(color: Colors.grey.shade500, fontSize: 16),
                  ),
                )
              : ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppConstants.paddingMedium,
                    vertical: AppConstants.paddingSmall,
                  ),
                  itemCount: _messages.length,
                  itemBuilder: (context, index) => _buildMessageBubble(_messages[index]),
                ),
        ),
        _buildInputBar(),
      ],
    );
  }

  Widget _buildMessageBubble(ChatMessage msg) {
    final isUser = msg.isUser;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        constraints: BoxConstraints(
          maxWidth: MediaQuery.of(context).size.width * 0.78,
        ),
        decoration: BoxDecoration(
          color: isUser
              ? Color(AppConstants.primaryColor)
              : Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(16),
            topRight: const Radius.circular(16),
            bottomLeft: Radius.circular(isUser ? 16 : 4),
            bottomRight: Radius.circular(isUser ? 4 : 16),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (msg.isStreaming && msg.text.isEmpty)
              _buildTypingIndicator()
            else
              SelectableText(
                msg.text,
                style: TextStyle(
                  color: isUser ? Colors.white : null,
                  fontSize: 15,
                  height: 1.4,
                ),
              ),
            const SizedBox(height: 4),
            Text(
              '${msg.timestamp.hour.toString().padLeft(2, '0')}:${msg.timestamp.minute.toString().padLeft(2, '0')}',
              style: TextStyle(
                fontSize: 10,
                color: isUser ? Colors.white60 : Colors.grey,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypingIndicator() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(3, (i) {
        return TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: Duration(milliseconds: 600 + (i * 200)),
          builder: (_, value, child) {
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 2),
              child: Opacity(
                opacity: 0.3 + (0.7 * value),
                child: child,
              ),
            );
          },
          child: Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: Colors.grey.shade500,
              shape: BoxShape.circle,
            ),
          ),
        );
      }),
    );
  }

  Widget _buildInputBar() {
    return Container(
      padding: EdgeInsets.only(
        left: AppConstants.paddingMedium,
        right: AppConstants.paddingSmall,
        top: AppConstants.paddingSmall,
        bottom: MediaQuery.of(context).padding.bottom + AppConstants.paddingSmall,
      ),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              controller: _inputController,
              focusNode: _focusNode,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _sendMessage(),
              maxLines: 4,
              minLines: 1,
              decoration: InputDecoration(
                hintText: 'মেসেজ লিখুন...',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: BorderSide.none,
                ),
                filled: true,
                fillColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Material(
            color: _isGenerating
                ? Colors.grey
                : Color(AppConstants.primaryColor),
            shape: const CircleBorder(),
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: _isGenerating ? null : _sendMessage,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Icon(
                  _isGenerating ? Icons.hourglass_top : Icons.send_rounded,
                  color: Colors.white,
                  size: 22,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ─── MODEL MANAGEMENT TAB ───────────────────────────────────────────────

  Widget _buildModelTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Info banner
          Container(
            padding: const EdgeInsets.all(AppConstants.paddingMedium),
            decoration: BoxDecoration(
              color: Color(AppConstants.infoColor).withOpacity(0.1),
              borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
              border: Border.all(color: Color(AppConstants.infoColor).withOpacity(0.3)),
            ),
            child: const Row(
              children: [
                Icon(Icons.info_outline, color: Color(AppConstants.infoColor)),
                SizedBox(width: AppConstants.paddingSmall),
                Expanded(
                  child: Text(
                    'মডেল একবার ডাউনলোড করলে সব সময় অফলাইনে চলবে।\n'
                    'প্রয়োজন: ≥4GB RAM, পর্যাপ্ত স্টোরেজ।',
                    style: TextStyle(fontSize: 13, height: 1.4),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppConstants.paddingXLarge),

          // Device info
          FutureBuilder<Map<String, dynamic>>(
            future: _llmService.getDeviceInfo(),
            builder: (context, snapshot) {
              if (!snapshot.hasData || snapshot.data!.containsKey('error')) {
                return const SizedBox.shrink();
              }
              final info = snapshot.data!;
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(AppConstants.paddingMedium),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('ডিভাইসের তথ্য',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                      const SizedBox(height: 8),
                      if (info['totalRamMb'] != null)
                        Text('RAM: ${info['totalRamMb']} MB'),
                      if (info['availableRamMb'] != null)
                        Text('ফাঁকা RAM: ${info['availableRamMb']} MB'),
                      if (info['gpuSupported'] != null)
                        Text('GPU সাপোর্ট: ${info['gpuSupported'] == true ? '✅ হ্যাঁ' : '❌ না'}'),
                    ],
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: AppConstants.paddingLarge),

          const Text(
            'উপলব্ধ মডেলসমূহ',
            style: TextStyle(
              fontSize: AppConstants.titleFontSize,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppConstants.paddingXSmall),
          const Text(
            '(মডেল ডাউনলোড → লোড → চ্যাট শুরু)',
            style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
          ),
          const SizedBox(height: AppConstants.paddingMedium),

          // Model cards (dynamically loaded from cloud)
          if (_isLoadingModels)
            const Center(child: Padding(
              padding: EdgeInsets.all(32),
              child: Column(children: [
                CircularProgressIndicator(),
                SizedBox(height: 16),
                Text('ক্লাউড থেকে মডেল খুঁজছি...', style: TextStyle(color: Colors.grey)),
              ]),
            )),
          if (!_isLoadingModels && _availableModels.isEmpty)
            Center(child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(children: [
                Icon(Icons.cloud_off, size: 48, color: Colors.grey.shade400),
                const SizedBox(height: 16),
                const Text('কোনো মডেল পাওয়া যায়নি', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                const Text('ইন্টারনেট সংযোগ চেক করুন ও আবার চেষ্টা করুন', style: TextStyle(color: Colors.grey)),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  icon: const Icon(Icons.refresh),
                  label: const Text('আবার খুঁজুন'),
                  onPressed: () {
                    setState(() => _isLoadingModels = true);
                    OnDeviceLlmService.clearModelCache();
                    _loadModelCatalog();
                  },
                ),
              ]),
            )),
          ..._availableModels.map((model) {
            final id = model['id']!;
            final isDownloaded = _downloadedModels[id] ?? false;
            final isDownloading = _downloadProgress.containsKey(id);
            final progress = _downloadProgress[id] ?? 0.0;
            final isLoaded = _selectedModelId == id && _llmService.isModelLoaded;

            return Card(
              margin: const EdgeInsets.only(bottom: AppConstants.paddingMedium),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
                side: isLoaded
                    ? BorderSide(color: Color(AppConstants.successColor), width: 2)
                    : BorderSide.none,
              ),
              child: Padding(
                padding: const EdgeInsets.all(AppConstants.paddingMedium),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Color(AppConstants.primaryColor).withOpacity(0.1),
                            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
                          ),
                          child: Icon(
                            id.contains('gpu') ? Icons.memory : Icons.developer_board,
                            color: Color(AppConstants.primaryColor),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(model['name']!,
                                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                              const SizedBox(height: 2),
                              Text(model['description']!,
                                  style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                            ],
                          ),
                        ),
                        Chip(
                          label: Text(model['size']!,
                              style: const TextStyle(fontSize: 11)),
                          backgroundColor: Colors.grey.shade100,
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    // Status badges
                    Row(
                      children: [
                        _buildStatusBadge(
                          isDownloaded ? 'ডাউনলোড হয়েছে' : 'ডাউনলোড হয়নি',
                          isDownloaded ? Colors.green : Colors.grey,
                        ),
                        const SizedBox(width: 8),
                        if (isLoaded)
                          _buildStatusBadge('🟢 সক্রিয়', Colors.green),
                      ],
                    ),

                    // Download progress
                    if (isDownloading) ...[
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: LinearProgressIndicator(
                                value: progress,
                                minHeight: 8,
                                backgroundColor: Colors.grey.shade200,
                                color: Color(AppConstants.primaryColor),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Text('${(progress * 100).toInt()}%',
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ],

                    const SizedBox(height: 12),

                    // Action buttons
                    Row(
                      children: [
                        if (!isDownloaded && !isDownloading)
                          Expanded(
                            child: ElevatedButton.icon(
                              icon: const Icon(Icons.download_rounded),
                              label: const Text('ডাউনলোড'),
                              onPressed: () => _downloadModel(id, model['url']!),
                            ),
                          ),
                        if (isDownloaded && !isLoaded) ...[
                          Expanded(
                            child: ElevatedButton.icon(
                              icon: _isLoadingModel
                                  ? const SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                    )
                                  : const Icon(Icons.play_arrow_rounded),
                              label: Text(_isLoadingModel ? 'লোড হচ্ছে...' : 'লোড ও চ্যাট শুরু'),
                              onPressed: _isLoadingModel ? null : () => _loadAndStartChat(id),
                            ),
                          ),
                          const SizedBox(width: 8),
                          IconButton(
                            icon: Icon(Icons.delete_outline, color: Color(AppConstants.errorColor)),
                            tooltip: 'মডেল মুছুন',
                            onPressed: () => _deleteModel(id),
                          ),
                        ],
                        if (isLoaded)
                          Expanded(
                            child: OutlinedButton.icon(
                              icon: const Icon(Icons.stop_rounded),
                              label: const Text('মডেল আনলোড'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Color(AppConstants.errorColor),
                              ),
                              onPressed: () async {
                                await _llmService.unloadModel();
                                setState(() => _selectedModelId = null);
                              },
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildStatusBadge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Text(text, style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w500)),
    );
  }
}
