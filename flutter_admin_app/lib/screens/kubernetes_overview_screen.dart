// KubernetesOverviewScreen - Displays cluster health and pod status
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/kubernetes_model.dart';
import '../services/kubernetes_service.dart';

class KubernetesOverviewScreen extends StatefulWidget {
  const KubernetesOverviewScreen({Key? key}) : super(key: key);

  @override
  State<KubernetesOverviewScreen> createState() => _KubernetesOverviewScreenState();
}

class _KubernetesOverviewScreenState extends State<KubernetesOverviewScreen> {
  late KubernetesService kubernetesService;
  ClusterHealth? clusterHealth;
  List<K8sPod> pods = [];
  bool isLoading = false;

  @override
  void initState() {
    super.initState();
    kubernetesService = context.read<KubernetesService>();
    _loadClusterData();
    _setupAutoRefresh();
  }

  void _setupAutoRefresh() {
    Future.delayed(const Duration(seconds: 10), () {
      if (mounted) {
        _loadClusterData();
        _setupAutoRefresh();
      }
    });
  }

  Future<void> _loadClusterData() async {
    if (!mounted) return;
    setState(() => isLoading = true);

    try {
      final health = await kubernetesService.getClusterHealth();
      if (mounted && health != null) {
        setState(() {
          clusterHealth = health;
          isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading cluster health: $e')),
        );
      }
    }
  }

  Color _getHealthColor(double healthPercent) {
    if (healthPercent >= 80) return Colors.green;
    if (healthPercent >= 50) return Colors.orange;
    return Colors.red;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Kubernetes Cluster'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadClusterData,
          ),
        ],
      ),
      body: isLoading && clusterHealth == null
          ? const Center(child: CircularProgressIndicator())
          : clusterHealth == null
              ? Center(
                  child: Text(
                    'Unable to load cluster data',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                )
              : SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Health overview
                      _buildHealthCard(),
                      const SizedBox(height: 24),

                      // Pod statistics
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Text(
                          'Pod Status',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ),
                      const SizedBox(height: 16),
                      _buildStatRow('Running', clusterHealth!.runningPods, Colors.green),
                      _buildStatRow('Pending', clusterHealth!.pendingPods, Colors.orange),
                      _buildStatRow('Failed', clusterHealth!.failedPods, Colors.red),
                      const SizedBox(height: 24),

                      // Deployments section
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Text(
                          'Active Deployments (${clusterHealth!.deployments.length})',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ),
                      const SizedBox(height: 12),
                      _buildDeploymentsList(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildHealthCard() {
    final health = clusterHealth!;
    final healthColor = _getHealthColor(health.healthPercent);

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Cluster Health',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Column(
                children: [
                  Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox(
                        width: 100,
                        height: 100,
                        child: CircularProgressIndicator(
                          value: health.healthPercent / 100,
                          strokeWidth: 8,
                          valueColor: AlwaysStoppedAnimation<Color>(healthColor),
                        ),
                      ),
                      Text(
                        '${health.healthPercent.toStringAsFixed(0)}%',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    health.isHealthy ? 'Healthy' : 'Degraded',
                    style: TextStyle(
                      color: healthColor,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Total Pods: ${health.totalPods}'),
                  const SizedBox(height: 8),
                  Text('Services: ${health.services.length}'),
                  const SizedBox(height: 8),
                  Text('Nodes: ${health.deployments.length}'),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatRow(String label, int count, Color color) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodyLarge),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: color.withOpacity(0.2),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              '$count',
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDeploymentsList() {
    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: clusterHealth!.deployments.length,
      itemBuilder: (context, index) {
        final deployment = clusterHealth!.deployments[index];
        final readiness = deployment.readinessPercent;
        final readinessColor = readiness >= 80 ? Colors.green : Colors.orange;

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          child: ListTile(
            title: Text(deployment.name),
            subtitle: Text(deployment.namespace),
            trailing: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text('${deployment.readyReplicas}/${deployment.desiredReplicas}'),
                Text(
                  '${readiness.toStringAsFixed(0)}%',
                  style: TextStyle(color: readinessColor, fontSize: 12),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
