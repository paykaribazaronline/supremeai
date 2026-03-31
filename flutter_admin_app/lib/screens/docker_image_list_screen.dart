// DockerImageListScreen - Lists Docker images with build status
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/docker_model.dart';
import '../services/docker_service.dart';

class DockerImageListScreen extends StatefulWidget {
  const DockerImageListScreen({Key? key}) : super(key: key);

  @override
  State<DockerImageListScreen> createState() => _DockerImageListScreenState();
}

class _DockerImageListScreenState extends State<DockerImageListScreen> {
  late DockerService dockerService;
  List<DockerImage> images = [];
  bool isLoading = false;
  String searchQuery = '';

  @override
  void initState() {
    super.initState();
    dockerService = context.read<DockerService>();
    _loadImages();
    _setupAutoRefresh();
  }

  void _setupAutoRefresh() {
    Future.delayed(const Duration(seconds: 15), () {
      if (mounted) {
        _loadImages();
        _setupAutoRefresh();
      }
    });
  }

  Future<void> _loadImages() async {
    if (!mounted) return;
    setState(() => isLoading = true);

    try {
      final data = await dockerService.listImages();
      if (mounted) {
        setState(() {
          images = data;
          isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading images: $e')),
        );
      }
    }
  }

  List<DockerImage> _getFilteredImages() {
    if (searchQuery.isEmpty) return images;
    return images
        .where((img) =>
            img.imageName.toLowerCase().contains(searchQuery.toLowerCase()) ||
            img.tag.toLowerCase().contains(searchQuery.toLowerCase()))
        .toList();
  }

  Color _getStatusColor(DockerStatus status) {
    switch (status) {
      case DockerStatus.ready:
        return Colors.green;
      case DockerStatus.published:
        return Colors.blue;
      case DockerStatus.building:
        return Colors.orange;
      case DockerStatus.pending:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _getFilteredImages();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Docker Images'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadImages,
          ),
        ],
      ),
      body: Column(
        children: [
          // Search bar
          Padding(
            padding: const EdgeInsets.all(16),
            child: SearchBar(
              hintText: 'Search images...',
              onChanged: (value) => setState(() => searchQuery = value),
              leading: const Icon(Icons.search),
            ),
          ),

          // Images list
          Expanded(
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : filtered.isEmpty
                    ? Center(
                        child: Text(
                          searchQuery.isEmpty
                              ? 'No images found'
                              : 'No matching images',
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: (_) => _loadImages(),
                        child: ListView.builder(
                          itemCount: filtered.length,
                          itemBuilder: (context, index) {
                            final image = filtered[index];
                            return _buildImageCard(image);
                          },
                        ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Navigate to build image screen
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildImageCard(DockerImage image) {
    final statusColor = _getStatusColor(image.status);
    final sizeInMB = image.sizeInMB;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ExpansionTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: statusColor.withOpacity(0.2),
            shape: BoxShape.circle,
          ),
          child: Icon(
            image.status == DockerStatus.published
                ? Icons.cloud_done
                : image.status == DockerStatus.ready
                    ? Icons.check_circle
                    : Icons.image,
            color: statusColor,
          ),
        ),
        title: Text(image.imageName),
        subtitle: Text(
          '${image.tag} • ${sizeInMB.toStringAsFixed(1)} MB',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Chip(
          label: Text(image.status.name),
          backgroundColor: statusColor.withOpacity(0.2),
          labelStyle: TextStyle(color: statusColor),
        ),
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildInfoRow('Registry', image.registry),
                _buildInfoRow('Base Image', image.baseImageId ?? 'N/A'),
                _buildInfoRow('Created', image.createdAt?.toString() ?? 'N/A'),
                _buildInfoRow('Last Updated', image.updatedAt?.toString() ?? 'N/A'),
                _buildInfoRow('Validations', image.validationCount.toString()),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    ElevatedButton.icon(
                      onPressed: () async {
                        await dockerService.validateImage(image.imageId);
                        _loadImages();
                      },
                      icon: const Icon(Icons.check),
                      label: const Text('Validate'),
                    ),
                    ElevatedButton.icon(
                      onPressed: () {
                        // Push to registry
                      },
                      icon: const Icon(Icons.cloud_upload),
                      label: const Text('Push'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          Text(
            value,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
