// DeploymentListScreen - Lists all deployments with real-time status
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/deployment_model.dart';
import '../services/deployment_service.dart';

class DeploymentListScreen extends StatefulWidget {
  const DeploymentListScreen({Key? key}) : super(key: key);

  @override
  State<DeploymentListScreen> createState() => _DeploymentListScreenState();
}

class _DeploymentListScreenState extends State<DeploymentListScreen> {
  late DeploymentService deploymentService;
  List<DeploymentRecord> deployments = [];
  bool isLoading = false;
  String selectedFilter = 'all';

  @override
  void initState() {
    super.initState();
    deploymentService = context.read<DeploymentService>();
    _loadDeployments();
    _setupAutoRefresh();
  }

  void _setupAutoRefresh() {
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted) {
        _loadDeployments();
        _setupAutoRefresh();
      }
    });
  }

  Future<void> _loadDeployments() async {
    if (!mounted) return;
    setState(() => isLoading = true);
    
    try {
      final data = await deploymentService.listDeployments();
      if (mounted) {
        setState(() {
          deployments = data;
          isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading deployments: $e')),
        );
      }
    }
  }

  List<DeploymentRecord> _getFilteredDeployments() {
    if (selectedFilter == 'all') return deployments;
    return deployments.where((d) => d.status.name == selectedFilter).toList();
  }

  Color _getStatusColor(DeploymentStatus status) {
    switch (status) {
      case DeploymentStatus.success:
        return Colors.green;
      case DeploymentStatus.failed:
        return Colors.red;
      case DeploymentStatus.inProgress:
        return Colors.blue;
      case DeploymentStatus.pending:
        return Colors.orange;
      case DeploymentStatus.rolledBack:
        return Colors.purple;
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _getFilteredDeployments();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Deployments'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadDeployments,
          ),
        ],
      ),
      body: Column(
        children: [
          // Filter chips
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.all(16),
            child: Row(
              children: ['all', 'PENDING', 'IN_PROGRESS', 'SUCCESS', 'FAILED', 'ROLLED_BACK']
                  .map((status) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(status),
                      selected: selectedFilter == status,
                      onSelected: (selected) {
                        setState(() => selectedFilter = selected ? status : 'all');
                      },
                    ),
                  ))
                  .toList(),
            ),
          ),
          // Deployments list
          Expanded(
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : filtered.isEmpty
                    ? Center(
                        child: Text(
                          'No deployments found',
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: (_) => _loadDeployments(),
                        child: ListView.builder(
                          itemCount: filtered.length,
                          itemBuilder: (context, index) {
                            final deployment = filtered[index];
                            return _buildDeploymentCard(deployment);
                          },
                        ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Navigate to create deployment screen
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildDeploymentCard(DeploymentRecord deployment) {
    final statusColor = _getStatusColor(deployment.status);
    final duration = deployment.getDuration();

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: statusColor,
            shape: BoxShape.circle,
          ),
          child: Icon(
            deployment.isSuccess
                ? Icons.check_circle
                : deployment.isFailed
                    ? Icons.error
                    : deployment.isRunning
                        ? Icons.timelapse
                        : Icons.schedule,
            color: Colors.white,
          ),
        ),
        title: Text(deployment.applicationName),
        subtitle: Text(
          '${deployment.version} • ${deployment.environment}',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              deployment.status.name.toUpperCase(),
              style: TextStyle(
                color: statusColor,
                fontWeight: FontWeight.bold,
                fontSize: 12,
              ),
            ),
            Text(
              duration,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
        onTap: () {
          // Navigate to deployment details
        },
      ),
    );
  }
}
