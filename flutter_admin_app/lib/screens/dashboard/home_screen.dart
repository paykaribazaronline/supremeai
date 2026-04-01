import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';
import '../projects/projects_list_screen.dart';
import '../metrics_screen.dart';
import '../settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

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
    return Scaffold(
      appBar: AppBar(
        title: const Text(AppConstants.appName),
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
          return SingleChildScrollView(
            padding: const EdgeInsets.all(AppConstants.paddingLarge),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Welcome Section
                _buildWelcomeSection(authProvider),
                const SizedBox(height: AppConstants.paddingXLarge),

                // Dashboard Cards
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

                // Quick Actions
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
          );
        },
      ),
    );
  }

  Widget _buildWelcomeSection(AuthProvider authProvider) {
    final userName = authProvider.currentUser?['name'] ?? 'Admin';

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
            child: const Text(
              'System Status: Online',
              style: TextStyle(
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
    final cards = [
      {
        'title': 'Projects',
        'count': '12',
        'icon': Icons.folder_outlined,
        'color': AppConstants.primaryColor,
      },
      {
        'title': 'AI Providers',
        'count': '8',
        'icon': Icons.api,
        'color': AppConstants.secondaryColor,
      },
      {
        'title': 'Active Agents',
        'count': '25',
        'icon': Icons.smart_toy_outlined,
        'color': AppConstants.warningColor,
      },
      {
        'title': 'System Health',
        'count': '98%',
        'icon': Icons.favorite,
        'color': AppConstants.successColor,
      },
    ];

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
          title: card['title'] as String,
          count: card['count'] as String,
          icon: card['icon'] as IconData,
          color: card['color'] as int,
        );
      },
    );
  }

  Widget _buildDashboardCard({
    required String title,
    required String count,
    required IconData icon,
    required int color,
  }) {
    return  Card(
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
