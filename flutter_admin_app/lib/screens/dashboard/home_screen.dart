import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/app_routes.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';
import '../../services/api_service.dart';
import '../projects/projects_list_screen.dart';
import '../metrics_screen.dart';
import '../settings_screen.dart';
import '../learning/learning_screen.dart';
import '../teaching/teaching_screen.dart';
import '../providers/ai_providers_screen.dart';
import '../extension/self_extension_screen.dart';
import '../alerts/alerts_logs_screen.dart';

class DashboardStat {
  final String title;
  final String count;
  final IconData icon;
  final int color;

  const DashboardStat({
    required this.title,
    required this.count,
    required this.icon,
    required this.color,
  });
}

class DashboardContract {
  final String title;
  final String systemHealthStatus;
  final List<DashboardStat> stats;

  const DashboardContract({
    required this.title,
    required this.systemHealthStatus,
    required this.stats,
  });

  factory DashboardContract.fromJson(Map<String, dynamic> json) {
    final stats = json['stats'] as Map<String, dynamic>? ?? <String, dynamic>{};

    return DashboardContract(
      title: (json['title'] as String?)?.trim().isNotEmpty == true
          ? json['title'] as String
          : AppConstants.appName,
      systemHealthStatus:
          (stats['systemHealthStatus'] as String?) ?? 'UNKNOWN',
      stats: [
        DashboardStat(
          title: 'Total Users',
          count: _formatWholeNumber(stats['totalUsers']),
          icon: Icons.people_alt_outlined,
          color: AppConstants.primaryColor,
        ),
        DashboardStat(
          title: 'Active Projects',
          count: _formatWholeNumber(stats['activeProjects']),
          icon: Icons.folder_outlined,
          color: AppConstants.secondaryColor,
        ),
        DashboardStat(
          title: 'AI Providers',
          count: _formatWholeNumber(stats['aiProviders']),
          icon: Icons.api,
          color: AppConstants.warningColor,
        ),
        DashboardStat(
          title: 'System Health',
          count: _formatPercent(stats['systemHealthScore']),
          icon: Icons.favorite,
          color: AppConstants.successColor,
        ),
      ],
    );
  }

  static String _formatWholeNumber(dynamic value) {
    if (value is int) {
      return value.toString();
    }

    if (value is double) {
      return value.round().toString();
    }

    return '0';
  }

  static String _formatPercent(dynamic value) {
    if (value is int) {
      return '$value%';
    }

    if (value is double) {
      final normalizedValue = value % 1 == 0 ? value.toInt() : value;
      return '$normalizedValue%';
    }

    return '0%';
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;
  final ApiService _apiService = ApiService();
  DashboardContract? _dashboardContract;
  bool _isLoadingContract = true;
  String? _dashboardError;

  @override
  void initState() {
    super.initState();
    _loadDashboardContract();
  }

  Future<void> _loadDashboardContract() async {
    setState(() {
      _isLoadingContract = true;
      _dashboardError = null;
    });

    final response =
        await _apiService.get<Map<String, dynamic>>('/api/admin/dashboard/contract');

    if (!mounted) {
      return;
    }

    if (response.success && response.data != null) {
      setState(() {
        _dashboardContract = DashboardContract.fromJson(response.data!);
        _isLoadingContract = false;
      });
      return;
    }

    final statusCode = response.statusCode ?? 0;
    if (statusCode == 401 || statusCode == 403) {
      await context.read<AuthProvider>().logout();
      if (!mounted) {
        return;
      }
      Navigator.of(context).pushNamedAndRemoveUntil(AppRoutes.login, (route) => false);
      return;
    }

    setState(() {
      _isLoadingContract = false;
      _dashboardError = response.error ?? 'Unable to load dashboard contract.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _buildBody(),
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        currentIndex: _selectedIndex,
        onTap: (index) {
          setState(() => _selectedIndex = index);
        },
        items: const [
          BottomNavigationBarItem(
            icon: Tooltip(message: 'হোম ড্যাশবোর্ড — সবকিছুর সারসংক্ষেপ দেখুন', child: Icon(Icons.dashboard)),
            label: 'Dashboard',
            tooltip: 'হোম ড্যাশবোর্ড',
          ),
          BottomNavigationBarItem(
            icon: Tooltip(message: 'প্রজেক্ট — আপনার সব প্রজেক্ট দেখুন ও পরিচালনা করুন', child: Icon(Icons.folder)),
            label: 'Projects',
            tooltip: 'প্রজেক্টসমূহ',
          ),
          BottomNavigationBarItem(
            icon: Tooltip(message: 'মেট্রিক্স — সিস্টেমের স্বাস্থ্য ও কর্মক্ষমতা দেখুন', child: Icon(Icons.show_chart)),
            label: 'Metrics',
            tooltip: 'কর্মক্ষমতা',
          ),
          BottomNavigationBarItem(
            icon: Tooltip(message: 'সেটিংস — প্রোফাইল, থিম, নোটিফিকেশন ইত্যাদি পরিবর্তন করুন', child: Icon(Icons.settings)),
            label: 'Settings',
            tooltip: 'সেটিংস',
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    switch (_selectedIndex) {
      case 0:
        return _buildDashboardTab();
      case 1:
        return const ProjectsListScreen();
      case 2:
        return const MetricsScreen();
      case 3:
        return const SettingsScreen();
      default:
        return _buildDashboardTab();
    }
  }

  Widget _buildDashboardTab() {
    final dashboardTitle = _dashboardContract?.title ?? AppConstants.appName;

    return Scaffold(
      appBar: AppBar(
        title: Text(dashboardTitle),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: () {
              _showProfileMenu(context);
            },
            icon: const Icon(Icons.account_circle),
          ),
        ],
      ),
      body: Consumer<AuthProvider>(
        builder: (context, authProvider, _) {
          return RefreshIndicator(
            onRefresh: _loadDashboardContract,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildWelcomeSection(authProvider),
                  const SizedBox(height: AppConstants.paddingXLarge),
                  _buildContractStateBanner(),
                  if (_isLoadingContract || _dashboardError != null)
                    const SizedBox(height: AppConstants.paddingLarge),
                  const Text(
                    'ড্যাশবোর্ড',
                    style: TextStyle(
                      fontSize: AppConstants.titleFontSize,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: AppConstants.paddingXXSmall),
                  const Text(
                    '(সিস্টেমের গুরুত্বপূর্ণ তথ্যের সংক্ষিপ্ত কার্ড)',
                    style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildDashboardCards(),
                  const SizedBox(height: AppConstants.paddingXLarge),
                  const Text(
                    'দ্রুত কার্যক্রম',
                    style: TextStyle(
                      fontSize: AppConstants.titleFontSize,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: AppConstants.paddingXXSmall),
                  const Text(
                    '(এক ক্লিকে যেকোনো ফিচারে যান)',
                    style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildQuickActions(context),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildWelcomeSection(AuthProvider authProvider) {
    final userName = authProvider.currentUser?['name'] ?? 'Admin';
    final systemStatus = _dashboardContract?.systemHealthStatus ?? 'ONLINE';

    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingLarge),
      decoration: BoxDecoration(
        color: Color(AppConstants.primaryColor),
        borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'স্বাগতম, $userName!',
            style: const TextStyle(
              color: Colors.white,
              fontSize: AppConstants.headingFontSize,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppConstants.paddingSmall),
          const Text(
            'SupremeAI অ্যাডমিন ম্যানেজমেন্ট সিস্টেম',
            style: TextStyle(
              color: Colors.white70,
              fontSize: AppConstants.subtitleFontSize,
            ),
          ),
          const SizedBox(height: AppConstants.paddingXXSmall),
          const Text(
            '(এখান থেকে পুরো সিস্টেম নিয়ন্ত্রণ করুন)',
            style: TextStyle(
              color: Colors.white54,
              fontSize: AppConstants.captionFontSize,
            ),
          ),
          const SizedBox(height: AppConstants.paddingMedium),
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppConstants.paddingMedium,
              vertical: AppConstants.paddingSmall,
            ),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.2),
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'সিস্টেম অবস্থা: $systemStatus',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const Text(
                  '(সবুজ = চালু আছে, লাল = সমস্যা আছে)',
                  style: TextStyle(color: Colors.white54, fontSize: 10),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDashboardCards() {
    final cards = _dashboardContract?.stats ?? const <DashboardStat>[];

    if (cards.isEmpty && !_isLoadingContract) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingLarge),
          child: Column(
            children: const [
              Icon(Icons.dashboard_outlined, size: 32, color: Colors.grey),
              SizedBox(height: AppConstants.paddingMedium),
              Text(
                'ড্যাশবোর্ডের তথ্য এখনো পাওয়া যায়নি।',
                style: TextStyle(color: Colors.grey),
              ),
              SizedBox(height: AppConstants.paddingXSmall),
              Text(
                '(সার্ভার চালু থাকলে এখানে তথ্য আসবে)',
                style: TextStyle(color: Colors.grey, fontSize: AppConstants.captionFontSize),
              ),
            ],
          ),
        ),
      );
    }

    return GridView.builder(
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: AppConstants.paddingMedium,
        mainAxisSpacing: AppConstants.paddingMedium,
        childAspectRatio: 1.2,
      ),
      itemCount: cards.length,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemBuilder: (context, index) {
        final card = cards[index];
        return _buildDashboardCard(
            title: card.title,
            count: card.count,
            icon: card.icon,
            color: card.color,
        );
      },
    );
  }

  Widget _buildContractStateBanner() {
    if (_isLoadingContract) {
      return const LinearProgressIndicator();
    }

    if (_dashboardError == null) {
      return const SizedBox.shrink();
    }

    return Card(
      color: const Color(0xFFFFF3CD),
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: Row(
          children: [
            const Icon(Icons.warning_amber_rounded, color: Colors.orange),
            const SizedBox(width: AppConstants.paddingMedium),
            Expanded(
              child: Text(
                _dashboardError!,
                style: const TextStyle(
                  color: Color(0xFF7A4E00),
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            TextButton(
              onPressed: _loadDashboardContract,
              child: const Text('আবার চেষ্টা করুন'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboardCard({
    required String title,
    required String count,
    required IconData icon,
    required int color,
  }) {
    return Card(
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(color).withOpacity(0.1),
              Color(color).withOpacity(0.05),
            ],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(AppConstants.paddingSmall),
                decoration: BoxDecoration(
                  color: Color(color).withOpacity(0.2),
                  borderRadius:
                      BorderRadius.circular(AppConstants.radiusMedium),
                ),
                child: Icon(
                  icon,
                  color: Color(color),
                  size: 24,
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    count,
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 12,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    return Column(
      children: [
        _buildActionButton(
          title: 'Create Project',
          subtitle: 'নতুন প্রজেক্ট তৈরি করুন',
          icon: Icons.add_circle,
          color: Color(AppConstants.primaryColor),
          onTap: () {
            setState(() => _selectedIndex = 1);
          },
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'View Metrics',
          subtitle: 'সিস্টেমের কর্মক্ষমতা দেখুন',
          icon: Icons.analytics,
          color: Color(AppConstants.secondaryColor),
          onTap: () {
            setState(() => _selectedIndex = 2);
          },
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'System Settings',
          subtitle: 'সিস্টেম সেটিংস পরিবর্তন করুন',
          icon: Icons.tune,
          color: Color(AppConstants.warningColor),
          onTap: () {
            setState(() => _selectedIndex = 3);
          },
        ),
        const SizedBox(height: AppConstants.paddingXLarge),
        const Text(
          'AI ফিচারসমূহ',
          style: TextStyle(
            fontSize: AppConstants.titleFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(SupremeAI-এর বিশেষ ক্ষমতা — শেখা, শেখানো, বড় হওয়া)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Learning & Research',
          subtitle: 'শেখা ও গবেষণা — AI কী শিখেছে ও ইন্টারনেট থেকে কী পড়ছে',
          icon: Icons.psychology,
          color: Colors.indigo,
          onTap: () => Navigator.pushNamed(context, AppRoutes.learning),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Teaching',
          subtitle: 'AI-কে শেখান — নতুন অ্যাপ বানানো, ত্রুটি সমাধান, টেকনিক শেখানো',
          icon: Icons.school,
          color: Colors.teal,
          onTap: () => Navigator.pushNamed(context, AppRoutes.teaching),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'AI Providers',
          subtitle: 'AI প্রোভাইডার — ১০টি AI যোগ/পরীক্ষা/সরানো (Consensus ভোটিং)',
          icon: Icons.hub,
          color: Color(AppConstants.secondaryColor),
          onTap: () => Navigator.pushNamed(context, AppRoutes.aiProviders),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Self-Extension',
          subtitle: 'নিজে বড় হওয়া — AI-কে বলুন নতুন সার্ভিস বানাতে, নিজেই কোড লিখবে',
          icon: Icons.auto_fix_high,
          color: Colors.deepPurple,
          onTap: () => Navigator.pushNamed(context, AppRoutes.selfExtension),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Alerts & Logs',
          subtitle: 'সতর্কতা ও লগ — সিস্টেমের সমস্যা ও কার্যক্রমের রেকর্ড',
          icon: Icons.notifications_active,
          color: Color(AppConstants.warningColor),
          onTap: () => Navigator.pushNamed(context, AppRoutes.alertsLogs),
        ),
        const SizedBox(height: AppConstants.paddingXLarge),
        const Text(
          'সিস্টেম ব্যবস্থাপনা',
          style: TextStyle(
            fontSize: AppConstants.titleFontSize,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: AppConstants.paddingXSmall),
        const Text(
          '(সিস্টেম নিয়ন্ত্রণ, গিট, কোটা, VPN, নিরাপত্তা ইত্যাদি)',
          style: TextStyle(fontSize: AppConstants.captionFontSize, color: Colors.grey),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Admin Control',
          subtitle: 'অ্যাডমিন কন্ট্রোল — AUTO/WAIT/STOP মোড, অনুমোদন দিন',
          icon: Icons.admin_panel_settings,
          color: Colors.red.shade700,
          onTap: () => Navigator.pushNamed(context, AppRoutes.adminControl),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Consensus Voting',
          subtitle: 'মাল্টি-AI সিদ্ধান্ত — ১০টি AI-কে একসাথে প্রশ্ন করুন',
          icon: Icons.how_to_vote,
          color: Colors.purple,
          onTap: () => Navigator.pushNamed(context, AppRoutes.consensus),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Git Operations',
          subtitle: 'গিট — কমিট, পুশ, স্ট্যাটাস ও লগ দেখুন',
          icon: Icons.commit,
          color: Colors.orange.shade800,
          onTap: () => Navigator.pushNamed(context, AppRoutes.gitOps),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Quota Management',
          subtitle: 'কোটা — AI প্রোভাইডারদের ব্যবহার সীমা ও বাকি কোটা',
          icon: Icons.pie_chart,
          color: Colors.cyan.shade700,
          onTap: () => Navigator.pushNamed(context, AppRoutes.quota),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'VPN Management',
          subtitle: 'VPN — সংযোগ যোগ, সংযুক্ত ও বিচ্ছিন্ন করুন',
          icon: Icons.vpn_lock,
          color: Colors.blueGrey,
          onTap: () => Navigator.pushNamed(context, AppRoutes.vpn),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Resilience & Self-Healing',
          subtitle: 'রেজিলিয়েন্স — সার্কিট ব্রেকার, ফেইলওভার, আত্মমেরামত',
          icon: Icons.health_and_safety,
          color: Colors.green.shade800,
          onTap: () => Navigator.pushNamed(context, AppRoutes.resilience),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'ML Intelligence',
          subtitle: 'ML বুদ্ধিমত্তা — অ্যানোমালি ডিটেকশন, ব্যর্থতা পূর্বাভাস',
          icon: Icons.psychology,
          color: Colors.deepOrange,
          onTap: () => Navigator.pushNamed(context, AppRoutes.mlIntelligence),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Notifications',
          subtitle: 'নোটিফিকেশন — ইমেইল, স্ল্যাক, ডিসকর্ড মেসেজ পাঠান',
          icon: Icons.notifications,
          color: Colors.amber.shade800,
          onTap: () => Navigator.pushNamed(context, AppRoutes.notifications),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Analytics',
          subtitle: 'অ্যানালিটিক্স — ট্রেন্ড, দৈনিক/মাসিক রিপোর্ট, CSV এক্সপোর্ট',
          icon: Icons.analytics,
          color: Colors.blue.shade800,
          onTap: () => Navigator.pushNamed(context, AppRoutes.analytics),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Decision History',
          subtitle: 'সিদ্ধান্তের ইতিহাস — AI কবে কি সিদ্ধান্ত নিয়েছে',
          icon: Icons.timeline,
          color: Colors.brown,
          onTap: () => Navigator.pushNamed(context, AppRoutes.decisionHistory),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Phase 6 & 7',
          subtitle: 'ফেজ — ইন্টিগ্রেশন ও ক্লায়েন্ট জেনারেশন (iOS/Web/Desktop)',
          icon: Icons.layers,
          color: Colors.pink.shade700,
          onTap: () => Navigator.pushNamed(context, AppRoutes.phases),
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'Tracing',
          subtitle: 'ট্রেসিং — API কল ট্র্যাকিং, ত্রুটি ও কর্মক্ষমতা বিশ্লেষণ',
          icon: Icons.route,
          color: Colors.grey.shade700,
          onTap: () => Navigator.pushNamed(context, AppRoutes.tracing),
        ),
      ],
    );
  }

  Widget _buildActionButton({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Card(
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(AppConstants.paddingSmall),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
          ),
          child: Icon(
            icon,
            color: color,
          ),
        ),
        title: Text(
          title,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.arrow_forward_ios, size: 16),
        onTap: onTap,
      ),
    );
  }

  void _showProfileMenu(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final userName = authProvider.currentUser?['name'] ?? 'Admin';
    final userEmail = authProvider.currentUser?['email'] ?? 'admin@supremeai.com';

    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppConstants.radiusLarge),
        ),
      ),
      builder: (context) => SizedBox(
        height: 250,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(AppConstants.paddingLarge),
              child: Text(
                userName,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(bottom: AppConstants.paddingXSmall),
              child: Text(
                userEmail,
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ),
            const Padding(
              padding: EdgeInsets.only(bottom: AppConstants.paddingMedium),
              child: Text(
                '(আপনার অ্যাকাউন্টের তথ্য)',
                style: TextStyle(fontSize: 10, color: Colors.grey),
              ),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.person),
              title: const Text('Profile'),
              subtitle: const Text('প্রোফাইল ও সেটিংস দেখুন', style: TextStyle(fontSize: 11)),
              onTap: () {
                Navigator.pop(context);
                setState(() => _selectedIndex = 3);
              },
            ),
            ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: const Text('Logout', style: TextStyle(color: Colors.red)),
              subtitle: const Text('অ্যাকাউন্ট থেকে বের হন', style: TextStyle(fontSize: 11, color: Colors.red)),
              onTap: () {
                Navigator.pop(context);
                authProvider.logout();
                Navigator.of(context).pushReplacementNamed(AppRoutes.login);
              },
            ),
          ],
        ),
      ),
    );
  }
}
