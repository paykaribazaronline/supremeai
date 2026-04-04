import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';
import '../../services/api_service.dart';
import '../projects/projects_list_screen.dart';
import '../metrics_screen.dart';
import '../settings_screen.dart';

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
      Navigator.of(context).pushNamedAndRemoveUntil('/login', (route) => false);
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
            icon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.folder),
            label: 'Projects',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.show_chart),
            label: 'Metrics',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Settings',
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
                    'Dashboard',
                    style: TextStyle(
                      fontSize: AppConstants.titleFontSize,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildDashboardCards(),
                  const SizedBox(height: AppConstants.paddingXLarge),
                  const Text(
                    'Quick Actions',
                    style: TextStyle(
                      fontSize: AppConstants.titleFontSize,
                      fontWeight: FontWeight.bold,
                    ),
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
            'Welcome, $userName!',
            style: const TextStyle(
              color: Colors.white,
              fontSize: AppConstants.headingFontSize,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppConstants.paddingSmall),
          const Text(
            'SupremeAI Admin Management System',
            style: TextStyle(
              color: Colors.white70,
              fontSize: AppConstants.subtitleFontSize,
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
            child: Text(
              'System Status: $systemStatus',
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w500,
              ),
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
                'Dashboard stats are not available yet.',
                style: TextStyle(color: Colors.grey),
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
              child: const Text('Retry'),
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
          subtitle: 'Start a new project',
          icon: Icons.add_circle,
          color: Color(AppConstants.primaryColor),
          onTap: () {
            setState(() => _selectedIndex = 1);
          },
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'View Metrics',
          subtitle: 'System performance',
          icon: Icons.analytics,
          color: Color(AppConstants.secondaryColor),
          onTap: () {
            setState(() => _selectedIndex = 2);
          },
        ),
        const SizedBox(height: AppConstants.paddingMedium),
        _buildActionButton(
          title: 'System Settings',
          subtitle: 'Configure system',
          icon: Icons.tune,
          color: Color(AppConstants.warningColor),
          onTap: () {
            setState(() => _selectedIndex = 3);
          },
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
              padding: const EdgeInsets.only(bottom: AppConstants.paddingMedium),
              child: Text(
                userEmail,
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.person),
              title: const Text('Profile'),
              onTap: () {
                Navigator.pop(context);
                setState(() => _selectedIndex = 3);
              },
            ),
            ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: const Text('Logout', style: TextStyle(color: Colors.red)),
              onTap: () {
                Navigator.pop(context);
                authProvider.logout();
                Navigator.of(context).pushReplacementNamed('/login');
              },
            ),
          ],
        ),
      ),
    );
  }
}
