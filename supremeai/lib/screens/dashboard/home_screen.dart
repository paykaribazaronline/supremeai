import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/orchestration_provider.dart';
import '../../providers/auth_provider.dart';
import '../providers/ai_providers_screen.dart';
import '../projects/projects_list_screen.dart';
import '../analytics/analytics_screen.dart';
import '../notifications/notifications_screen.dart';
import '../alerts/alerts_screen.dart';
import '../vpn/vpn_screen.dart';
import '../resilience/resilience_screen.dart';
import '../git/git_screen.dart';
import '../quota/quota_screen.dart';
import '../extension/extension_screen.dart';
import '../consensus/consensus_screen.dart';
import '../learning/learning_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final TextEditingController _requirementController = TextEditingController();

  void _submitRequirement() {
    final orchestration = context.read<OrchestrationProvider>();
    final auth = context.read<AuthProvider>();
    if (_requirementController.text.isNotEmpty && auth.token != null) {
      orchestration.orchestrateRequirement(_requirementController.text, auth.token!);
    }
  }

  @override
  Widget build(BuildContext context) {
    final orchestration = context.watch<OrchestrationProvider>();
    final result = orchestration.lastResult;

    return Scaffold(
      appBar: AppBar(
        title: const Text('SupremeAI Dashboard'),
      ),
      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: BoxDecoration(color: Theme.of(context).primaryColor),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(child: Icon(Icons.person)),
                  SizedBox(height: 10),
                  Text('Admin User', style: TextStyle(color: Colors.white, fontSize: 18)),
                ],
              ),
            ),
            ListTile(
              leading: const Icon(Icons.dashboard),
              title: const Text('Dashboard'),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.satellite_alt),
              title: const Text('AI Providers'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const AiProvidersScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.folder),
              title: const Text('Projects'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const ProjectsListScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.analytics),
              title: const Text('Analytics'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const AnalyticsScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.notifications),
              title: const Text('Notifications'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const NotificationsScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.notification_important),
              title: const Text('Alerts'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const AlertsScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.vpn_lock),
              title: const Text('VPN Management'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const VpnScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.health_and_safety),
              title: const Text('Self-Healing'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const ResilienceScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.commit),
              title: const Text('Git Ops'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const GitScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.label_important),
              title: const Text('Tiers & Quotas'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const QuotaScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.extension),
              title: const Text('Self-Extension'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const ExtensionScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.group_work),
              title: const Text('Multi-AI Consensus'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const ConsensusScreen()));
              },
            ),
            ListTile(
              leading: const Icon(Icons.school),
              title: const Text('System Learning'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(context, MaterialPageRoute(builder: (_) => const LearningScreen()));
              },
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.logout),
              title: const Text('Logout'),
              onTap: () => context.read<AuthProvider>().logout(),
            ),
          ],
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _requirementController,
              decoration: const InputDecoration(
                labelText: 'Enter App Requirement',
                hintText: 'e.g. Create a task manager with Firebase',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: orchestration.isLoading ? null : _submitRequirement,
              child: orchestration.isLoading
                  ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Orchestrate Requirement'),
            ),
            const SizedBox(height: 20),
            if (orchestration.error != null)
              Text('Error: ${orchestration.error}', style: const TextStyle(color: Colors.red)),
            if (result != null) ...[
              const Text('Orchestration Result:', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 10),
              if (result['status'] == 'DECIDED')
                ElevatedButton.icon(
                  onPressed: orchestration.isLoading ? null : () => orchestration.generateProject(auth.token!),
                  icon: const Icon(Icons.rocket_launch),
                  label: const Text('Generate Project'),
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.orange, foregroundColor: Colors.white),
                ),
              const SizedBox(height: 10),
              Expanded(
                child: ListView(
                  children: [
                    Text('Status: ${result['status']}'),
                    const Divider(),
                    const Text('Decisions:', style: TextStyle(fontWeight: FontWeight.bold)),
                    ...((result['context']?['decisions'] as List?)?.map((d) => ListTile(
                      title: Text(d['decisionKey'] ?? 'Unknown'),
                      subtitle: Text(d['aiConsensus'] ?? ''),
                      leading: const Icon(Icons.check_circle, color: Colors.green),
                    )).toList() ?? [const Text('No decisions found')]),
                  ],
                ),
              ),
            ]
          ],
        ),
      ),
    );
  }
}
