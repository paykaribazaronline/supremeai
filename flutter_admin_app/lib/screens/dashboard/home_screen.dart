import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';

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
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Navigate to create project
        },
        backgroundColor: Color(AppConstants.primaryColor),
        child: const Icon(Icons.add),
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
        'title': 'API Calls Today',
        'count': '1,234',
        'icon': Icons.trending_up_outlined,
        'color': AppConstants.infoColor,
      },
    ];

    return GridView.builder(
      physics: const NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: AppConstants.paddingMedium,
        mainAxisSpacing: AppConstants.paddingMedium,
        childAspectRatio: 1.2,
      ),
      itemCount: cards.length,
      itemBuilder: (context, index) {
        final card = cards[index];
        return Container(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          decoration: BoxDecoration(
            color: Color(card['color'] as int).withOpacity(0.1),
            borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
            border: Border.all(
              color: Color(card['color'] as int).withOpacity(0.3),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Icon(
                card['icon'] as IconData,
                color: Color(card['color'] as int),
                size: 28,
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    card['count'].toString(),
                    style: TextStyle(
                      fontSize: AppConstants.headingFontSize,
                      fontWeight: FontWeight.bold,
                      color: Color(card['color'] as int),
                    ),
                  ),
                  const SizedBox(height: AppConstants.paddingXSmall),
                  Text(
                    card['title'].toString(),
                    style: const TextStyle(
                      fontSize: AppConstants.bodyFontSize,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    final actions = [
      {
        'label': 'Create Project',
        'icon': Icons.create_outlined,
        'onTap': () {},
      },
      {
        'label': 'Add AI Provider',
        'icon': Icons.add_circle_outline,
        'onTap': () {},
      },
      {
        'label': 'View Metrics',
        'icon': Icons.bar_chart_outlined,
        'onTap': () {},
      },
      {
        'label': 'Settings',
        'icon': Icons.settings_outlined,
        'onTap': () {},
      },
    ];

    return GridView.builder(
      physics: const NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: AppConstants.paddingMedium,
        mainAxisSpacing: AppConstants.paddingMedium,
        childAspectRatio: 1.5,
      ),
      itemCount: actions.length,
      itemBuilder: (context, index) {
        final action = actions[index];
        return InkWell(
          onTap: action['onTap'] as VoidCallback,
          borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
          child: Container(
            decoration: BoxDecoration(
              color: Color(AppConstants.primaryColor).withOpacity(0.05),
              borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
              border: Border.all(
                color: Color(AppConstants.primaryColor).withOpacity(0.2),
              ),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  action['icon'] as IconData,
                  color: Color(AppConstants.primaryColor),
                  size: 32,
                ),
                const SizedBox(height: AppConstants.paddingSmall),
                Text(
                  action['label'].toString(),
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: AppConstants.bodyFontSize,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showProfileMenu(BuildContext context) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppConstants.radiusLarge),
        ),
      ),
      builder: (BuildContext context) {
        return Consumer<AuthProvider>(
          builder: (context, authProvider, _) {
            return SizedBox(
              height: 250,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.all(AppConstants.paddingLarge),
                    child: Text(
                      authProvider.currentUser?['email'] ?? 'Admin',
                      style: const TextStyle(
                        fontSize: AppConstants.subtitleFontSize,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const Divider(),
                  ListTile(
                    leading: const Icon(Icons.person_outline),
                    title: const Text('Profile'),
                    onTap: () {
                      Navigator.pop(context);
                    },
                  ),
                  ListTile(
                    leading: const Icon(Icons.settings_outline),
                    title: const Text('Settings'),
                    onTap: () {
                      Navigator.pop(context);
                    },
                  ),
                  ListTile(
                    leading: const Icon(Icons.help_outline),
                    title: const Text('Help'),
                    onTap: () {
                      Navigator.pop(context);
                    },
                  ),
                  const Divider(),
                  ListTile(
                    leading: const Icon(Icons.logout, color: Colors.red),
                    title: const Text(
                      'Logout',
                      style: TextStyle(color: Colors.red),
                    ),
                    onTap: () {
                      Navigator.pop(context);
                      _handleLogout(context, authProvider);
                    },
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _handleLogout(BuildContext context, AuthProvider authProvider) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Logout'),
          content: const Text('Are you sure you want to logout?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () async {
                Navigator.pop(context);
                await authProvider.logout();
                if (mounted) {
                  Navigator.of(context).pushReplacementNamed('/login');
                }
              },
              child: const Text('Logout'),
            ),
          ],
        );
      },
    );
  }
}
