import 'package:flutter/material.dart';
import '../../config/constants.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class TeachingScreen extends StatefulWidget {
  const TeachingScreen({Key? key}) : super(key: key);

  @override
  State<TeachingScreen> createState() => _TeachingScreenState();
}

class _TeachingScreenState extends State<TeachingScreen>
    with SingleTickerProviderStateMixin {
  final ApiService _apiService = ApiService();
  late TabController _tabController;

  // Create App form
  final _appNameController = TextEditingController();
  final _appDescController = TextEditingController();
  final _appTypeController = TextEditingController();

  // Solve Error form
  final _errorMessageController = TextEditingController();
  final _errorContextController = TextEditingController();

  // Seed Technique form
  final _techNameController = TextEditingController();
  final _techCategoryController = TextEditingController();
  final _techDescController = TextEditingController();

  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _appNameController.dispose();
    _appDescController.dispose();
    _appTypeController.dispose();
    _errorMessageController.dispose();
    _errorContextController.dispose();
    _techNameController.dispose();
    _techCategoryController.dispose();
    _techDescController.dispose();
    super.dispose();
  }

  Future<void> _submitCreateApp() async {
    if (_appNameController.text.trim().isEmpty) {
      _showSnackBar('অ্যাপের নাম লিখুন', isError: true);
      return;
    }
    setState(() => _isSubmitting = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.teachingCreateApp,
      data: {
        'name': _appNameController.text.trim(),
        'description': _appDescController.text.trim(),
        'type': _appTypeController.text.trim(),
      },
    );
    if (!mounted) return;
    setState(() => _isSubmitting = false);
    if (response.success) {
      _showSnackBar('অ্যাপ তৈরির নির্দেশ সফলভাবে পাঠানো হয়েছে!');
      _appNameController.clear();
      _appDescController.clear();
      _appTypeController.clear();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "অ্যাপ তৈরি করা যায়নি"}',
          isError: true);
    }
  }

  Future<void> _submitSolveError() async {
    if (_errorMessageController.text.trim().isEmpty) {
      _showSnackBar('ত্রুটির বার্তা লিখুন', isError: true);
      return;
    }
    setState(() => _isSubmitting = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.teachingSolveError,
      data: {
        'errorMessage': _errorMessageController.text.trim(),
        'context': _errorContextController.text.trim(),
      },
    );
    if (!mounted) return;
    setState(() => _isSubmitting = false);
    if (response.success) {
      _showSnackBar('ত্রুটি সমাধানের নির্দেশ সফল!');
      _errorMessageController.clear();
      _errorContextController.clear();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "সমাধান করা যায়নি"}',
          isError: true);
    }
  }

  Future<void> _submitSeedTechnique() async {
    if (_techNameController.text.trim().isEmpty) {
      _showSnackBar('টেকনিকের নাম লিখুন', isError: true);
      return;
    }
    setState(() => _isSubmitting = true);
    final response = await _apiService.post<Map<String, dynamic>>(
      Environment.teachingSeedTechnique,
      data: {
        'name': _techNameController.text.trim(),
        'category': _techCategoryController.text.trim(),
        'description': _techDescController.text.trim(),
      },
    );
    if (!mounted) return;
    setState(() => _isSubmitting = false);
    if (response.success) {
      _showSnackBar('নতুন টেকনিক সফলভাবে শেখানো হয়েছে!');
      _techNameController.clear();
      _techCategoryController.clear();
      _techDescController.clear();
    } else {
      _showSnackBar('ত্রুটি: ${response.error ?? "টেকনিক শেখানো যায়নি"}',
          isError: true);
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError
            ? const Color(AppConstants.errorColor)
            : const Color(AppConstants.successColor),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Teaching'),
        elevation: 0,
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(icon: Icon(Icons.apps), text: 'অ্যাপ তৈরি'),
            Tab(icon: Icon(Icons.bug_report), text: 'ত্রুটি সমাধান'),
            Tab(icon: Icon(Icons.lightbulb), text: 'টেকনিক শেখাও'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _buildCreateAppTab(),
          _buildSolveErrorTab(),
          _buildSeedTechniqueTab(),
        ],
      ),
    );
  }

  Widget _buildCreateAppTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildTabHeader(
            icon: Icons.apps,
            title: 'অ্যাপ তৈরি করো',
            subtitle:
                'AI-কে একটি নতুন অ্যাপ বানাতে নির্দেশ দিন। নাম, বিবরণ ও ধরন দিলে AI নিজে কোড লিখবে।',
          ),
          const SizedBox(height: AppConstants.paddingLarge),
          _buildTextField(
            controller: _appNameController,
            label: 'অ্যাপের নাম',
            hint: 'যেমন: TodoApp, ChatBot, EcommerceApp',
            helperText: '(আপনি যে অ্যাপ বানাতে চান তার নাম)',
            icon: Icons.label,
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildTextField(
            controller: _appDescController,
            label: 'বিবরণ',
            hint: 'অ্যাপটি কী করবে তা বিস্তারিত লিখুন...',
            helperText: '(অ্যাপের কাজ ও ফিচারগুলো সংক্ষেপে লিখুন)',
            icon: Icons.description,
            maxLines: 3,
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildTextField(
            controller: _appTypeController,
            label: 'অ্যাপের ধরন',
            hint: 'যেমন: web, mobile, api, fullstack',
            helperText: '(কোন ধরনের অ্যাপ — ওয়েব, মোবাইল, API ইত্যাদি)',
            icon: Icons.category,
          ),
          const SizedBox(height: AppConstants.paddingXLarge),
          _buildSubmitButton('অ্যাপ তৈরির নির্দেশ দাও', _submitCreateApp),
        ],
      ),
    );
  }

  Widget _buildSolveErrorTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildTabHeader(
            icon: Icons.bug_report,
            title: 'ত্রুটি সমাধান করো',
            subtitle:
                'কোনো error বা সমস্যা AI-কে দিন, AI নিজে সমাধান খুঁজে বের করবে ও শিখবে।',
          ),
          const SizedBox(height: AppConstants.paddingLarge),
          _buildTextField(
            controller: _errorMessageController,
            label: 'ত্রুটির বার্তা',
            hint: 'যেমন: NullPointerException at line 42...',
            helperText: '(Error message বা exception যেটা দেখতে পাচ্ছেন)',
            icon: Icons.error,
            maxLines: 3,
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildTextField(
            controller: _errorContextController,
            label: 'প্রসঙ্গ (Context)',
            hint: 'কোন ফাইলে, কোন পরিস্থিতিতে error হয়েছে...',
            helperText: '(Error কোথায় হচ্ছে — ফাইলের নাম, ফাংশন ইত্যাদি)',
            icon: Icons.code,
            maxLines: 3,
          ),
          const SizedBox(height: AppConstants.paddingXLarge),
          _buildSubmitButton('সমাধান খুঁজে বের করো', _submitSolveError),
        ],
      ),
    );
  }

  Widget _buildSeedTechniqueTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildTabHeader(
            icon: Icons.lightbulb,
            title: 'নতুন টেকনিক শেখাও',
            subtitle:
                'AI-কে নতুন কোনো টেকনিক বা জ্ঞান শেখান। এটি সিস্টেমের জ্ঞানভাণ্ডারে যোগ হবে।',
          ),
          const SizedBox(height: AppConstants.paddingLarge),
          _buildTextField(
            controller: _techNameController,
            label: 'টেকনিকের নাম',
            hint: 'যেমন: Circuit Breaker Pattern, Docker Multi-stage Build',
            helperText: '(যে টেকনিক বা প্যাটার্ন শেখাতে চান তার নাম)',
            icon: Icons.lightbulb_outline,
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildTextField(
            controller: _techCategoryController,
            label: 'ক্যাটাগরি',
            hint: 'যেমন: design-pattern, devops, security, database',
            helperText: '(কোন বিভাগে পড়ে — ডিজাইন প্যাটার্ন, ডেভঅপস ইত্যাদি)',
            icon: Icons.folder,
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildTextField(
            controller: _techDescController,
            label: 'বিবরণ',
            hint: 'টেকনিকটি কীভাবে কাজ করে তা বিস্তারিত লিখুন...',
            helperText:
                '(টেকনিকের বিস্তারিত ব্যাখ্যা — কীভাবে কাজ করে, কেন দরকার)',
            icon: Icons.description,
            maxLines: 4,
          ),
          const SizedBox(height: AppConstants.paddingXLarge),
          _buildSubmitButton('টেকনিক শেখাও', _submitSeedTechnique),
        ],
      ),
    );
  }

  Widget _buildTabHeader(
      {required IconData icon,
      required String title,
      required String subtitle}) {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        color: const Color(AppConstants.primaryColor).withOpacity(0.08),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: Row(
        children: [
          Icon(icon, size: 40, color: const Color(AppConstants.primaryColor)),
          const SizedBox(width: AppConstants.paddingMedium),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontSize: AppConstants.titleFontSize,
                        fontWeight: FontWeight.bold)),
                const SizedBox(height: AppConstants.paddingXXSmall),
                Text(subtitle,
                    style: const TextStyle(
                        fontSize: AppConstants.captionFontSize,
                        color: Colors.grey)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required String helperText,
    required IconData icon,
    int maxLines = 1,
  }) {
    return TextField(
      controller: controller,
      maxLines: maxLines,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        helperText: helperText,
        helperMaxLines: 2,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
        ),
      ),
    );
  }

  Widget _buildSubmitButton(String text, VoidCallback onPressed) {
    return SizedBox(
      width: double.infinity,
      height: 50,
      child: ElevatedButton.icon(
        onPressed: _isSubmitting ? null : onPressed,
        icon: _isSubmitting
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                    strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.send),
        label: Text(text,
            style: const TextStyle(fontSize: AppConstants.subtitleFontSize)),
      ),
    );
  }
}
