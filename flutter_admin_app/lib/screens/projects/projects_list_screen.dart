import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../models/models.dart';
import '../../providers/projects_provider.dart';
import 'project_detail_screen.dart';

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
      Provider.of<ProjectsProvider>(context, listen: false).fetchProjects();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Projects'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: () {
              Provider.of<ProjectsProvider>(context, listen: false).fetchProjects();
            },
            icon: const Icon(Icons.refresh),
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
                    child: const Text('Retry'),
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
                    'No projects yet',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
                  ),
                  const SizedBox(height: AppConstants.paddingSmall),
                  const Text(
                    'Create your first project to get started',
                    style: TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => provider.fetchProjects(),
            child: ListView.builder(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              itemCount: provider.projects.length,
              itemBuilder: (context, index) {
                final project = provider.projects[index];
                return _buildProjectCard(context, project);
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => const ProjectDetailScreen(),
            ),
          );
        },
        backgroundColor: Color(AppConstants.primaryColor),
        child: const Icon(Icons.add),
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
            color: statusColor.withOpacity(0.1),
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
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppConstants.paddingSmall,
                vertical: 2,
              ),
              decoration: BoxDecoration(
                color: statusColor.withOpacity(0.1),
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
                  Text('View'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'edit',
              child: Row(
                children: [
                  Icon(Icons.edit, size: 20),
                  SizedBox(width: 8),
                  Text('Edit'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete, size: 20, color: Colors.red),
                  SizedBox(width: 8),
                  Text('Delete', style: TextStyle(color: Colors.red)),
                ],
              ),
            ),
          ],
          onSelected: (value) {
            if (value == 'view' || value == 'edit') {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => ProjectDetailScreen(project: project),
                ),
              );
            } else if (value == 'delete') {
              _showDeleteConfirmation(context, project);
            }
          },
        ),
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute(
              builder: (context) => ProjectDetailScreen(project: project),
            ),
          );
        },
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status.toLowerCase()) {
      case 'active':
        return Colors.green;
      case 'inactive':
        return Colors.grey;
      case 'error':
        return Colors.red;
      case 'building':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  void _showDeleteConfirmation(BuildContext context, Project project) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Project'),
        content: Text('Are you sure you want to delete "${project.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Provider.of<ProjectsProvider>(context, listen: false)
                  .deleteProject(project.id);
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Project "${project.name}" deleted')),
              );
            },
            child: const Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
