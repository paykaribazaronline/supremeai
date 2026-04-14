import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/app_routes.dart';
import '../../config/constants.dart';
import '../../models/models.dart';
import '../../providers/projects_provider.dart';

class ProjectsListScreen extends StatefulWidget {
  const ProjectsListScreen({Key? key}) : super(key: key);

  @override
  State<ProjectsListScreen> createState() => _ProjectsListScreenState();
}

class _ProjectsListScreenState extends State<ProjectsListScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      if (mounted) {
        Provider.of<ProjectsProvider>(context, listen: false).fetchProjects();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('প্রজেক্টসমূহ'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: () {
              Provider.of<ProjectsProvider>(context, listen: false)
                  .fetchProjects();
            },
            icon: const Icon(Icons.refresh),
            tooltip: 'আবার লোড করুন',
          ),
        ],
      ),
      body: Consumer<ProjectsProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (provider.error != null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.error_outline,
                    size: 48,
                    color: Colors.red,
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  Text(
                    'Error: ${provider.error}',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  ElevatedButton(
                    onPressed: () {
                      provider.fetchProjects();
                    },
                    child: const Text('আবার চেষ্টা করুন'),
                  ),
                ],
              ),
            );
          }

          if (provider.projects.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.folder_open,
                    size: 64,
                    color: Colors.grey[400],
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  const Text(
                    'এখনো কোনো প্রজেক্ট নেই',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
                  ),
                  const SizedBox(height: AppConstants.paddingSmall),
                  const Text(
                    'নতুন প্রজেক্ট তৈরি করতে + বাটনে চাপুন',
                    style: TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => provider.fetchProjects(),
            child: ListView(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              children: [
                _buildSummaryCard(provider),
                const SizedBox(height: AppConstants.paddingMedium),
                if (provider.runningProjects.isNotEmpty) ...[
                  _buildSectionHeader('Running Projects',
                      provider.runningProjects.length, Colors.blue),
                  const SizedBox(height: AppConstants.paddingSmall),
                  ...provider.runningProjects
                      .map((project) => _buildProjectCard(context, project)),
                ],
                if (provider.finishedProjects.isNotEmpty) ...[
                  _buildSectionHeader('Finished Projects',
                      provider.finishedProjects.length, Colors.green),
                  const SizedBox(height: AppConstants.paddingSmall),
                  ...provider.finishedProjects
                      .map((project) => _buildProjectCard(context, project)),
                ],
              ],
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.of(context).pushNamed(AppRoutes.projectNew);
        },
        backgroundColor: const Color(AppConstants.primaryColor),
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildSummaryCard(ProjectsProvider provider) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Project Storage Status',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildInfoChip(
                    provider.cloudStorageActive
                        ? 'Cloud Active'
                        : 'Local Cache Active',
                    provider.cloudStorageActive ? Colors.green : Colors.orange),
                _buildInfoChip('Running ${provider.runningCount}', Colors.blue),
                _buildInfoChip(
                    'Finished ${provider.finishedCount}', Colors.green),
                _buildInfoChip(
                    'Tracked ${provider.trackedProjectsCount}', Colors.purple),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, int count, Color color) {
    return Row(
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(999),
          ),
          child: Text(
            '$count',
            style: TextStyle(color: color, fontWeight: FontWeight.bold),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoChip(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontWeight: FontWeight.w600),
      ),
    );
  }

  Widget _buildProjectCard(BuildContext context, Project project) {
    final statusColor = _getStatusColor(project.status);

    return Card(
      margin: const EdgeInsets.only(bottom: AppConstants.paddingMedium),
      child: ListTile(
        contentPadding: const EdgeInsets.all(AppConstants.paddingMedium),
        leading: Container(
          padding: const EdgeInsets.all(AppConstants.paddingSmall),
          decoration: BoxDecoration(
            color: statusColor.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
          ),
          child: Icon(
            Icons.folder,
            color: statusColor,
          ),
        ),
        title: Text(
          project.name,
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 16,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: AppConstants.paddingSmall),
            Text(
              project.description.length > 50
                  ? '${project.description.substring(0, 50)}...'
                  : project.description,
              style: const TextStyle(fontSize: 12),
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            if (project.repoUrl.isNotEmpty)
              Text(
                project.repoUrl,
                style: const TextStyle(fontSize: 11, color: Colors.blueGrey),
              ),
            const SizedBox(height: AppConstants.paddingSmall),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                _buildTag(project.templateType, Colors.indigo),
                _buildTag('Branch ${project.repoBranch}', Colors.teal),
                _buildTag('Progress ${project.progress}%', Colors.blue),
                if (project.pushed) _buildTag('Pushed', Colors.green),
                if (project.trackedForImprovement)
                  _buildTag('Tracked for Improvement', Colors.purple),
              ],
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppConstants.paddingSmall,
                vertical: 2,
              ),
              decoration: BoxDecoration(
                color: statusColor.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(AppConstants.radiusSmall),
              ),
              child: Text(
                project.status.toUpperCase(),
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  color: statusColor,
                ),
              ),
            ),
          ],
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'view',
              child: Row(
                children: [
                  Icon(Icons.info_outline, size: 20),
                  SizedBox(width: 8),
                  Text('বিস্তারিত দেখুন'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete, size: 20, color: Colors.red),
                  SizedBox(width: 8),
                  Text('মুছে ফেলুন', style: TextStyle(color: Colors.red)),
                ],
              ),
            ),
          ],
          onSelected: (value) {
            if (value == 'view') {
              Navigator.of(context).pushNamed(
                AppRoutes.projectDetail,
                arguments: project,
              );
            } else if (value == 'delete') {
              _showDeleteConfirmation(context, project);
            }
          },
        ),
        onTap: () {
          Navigator.of(context).pushNamed(
            AppRoutes.projectDetail,
            arguments: project,
          );
        },
      ),
    );
  }

  Widget _buildTag(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppConstants.radiusSmall),
      ),
      child: Text(
        label,
        style:
            TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w600),
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status.toLowerCase()) {
      case 'pushed_to_repo':
        return Colors.green;
      case 'push_failed':
      case 'failed':
        return Colors.red;
      case 'generating':
      case 'template_initialized':
      case 'running':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  void _showDeleteConfirmation(BuildContext context, Project project) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('প্রজেক্ট মুছে ফেলবেন?'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('"${project.name}" মুছে ফেলতে চান?'),
            const SizedBox(height: 8),
            const Text(
              '(এটা মুছলে আর ফেরত আনা যাবে না)',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('না, রাখুন'),
          ),
          TextButton(
            onPressed: () async {
              final projectsProvider =
                  Provider.of<ProjectsProvider>(context, listen: false);
              await projectsProvider.deleteProject(project.id);
              if (context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('"${project.name}" মুছে ফেলা হয়েছে')),
                );
              }
            },
            child:
                const Text('হ্যাঁ, মুছুন', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
