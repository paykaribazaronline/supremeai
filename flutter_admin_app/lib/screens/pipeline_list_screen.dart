// PipelineListScreen - Lists CI/CD pipelines and their executions
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/pipeline_model.dart';
import '../services/pipeline_service.dart';

class PipelineListScreen extends StatefulWidget {
  const PipelineListScreen({Key? key}) : super(key: key);

  @override
  State<PipelineListScreen> createState() => _PipelineListScreenState();
}

class _PipelineListScreenState extends State<PipelineListScreen> {
  late PipelineService pipelineService;
  List<Pipeline> pipelines = [];
  Map<String, List<PipelineExecution>> executionsMap = {};
  bool isLoading = false;
  String selectedFilter = 'all';

  @override
  void initState() {
    super.initState();
    pipelineService = context.read<PipelineService>();
    _loadPipelines();
    _setupAutoRefresh();
  }

  void _setupAutoRefresh() {
    Future.delayed(const Duration(seconds: 20), () {
      if (mounted) {
        _loadPipelines();
        _setupAutoRefresh();
      }
    });
  }

  Future<void> _loadPipelines() async {
    if (!mounted) return;
    setState(() => isLoading = true);

    try {
      final data = await pipelineService.listPipelines();
      final execMap = <String, List<PipelineExecution>>{};

      // Load executions for each pipeline
      for (final pipeline in data) {
        try {
          final execs = await pipelineService.listExecutions(pipeline.pipelineId);
          execMap[pipeline.pipelineId] = execs;
        } catch (e) {
          execMap[pipeline.pipelineId] = [];
        }
      }

      if (mounted) {
        setState(() {
          pipelines = data;
          executionsMap = execMap;
          isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading pipelines: $e')),
        );
      }
    }
  }

  List<Pipeline> _getFilteredPipelines() {
    if (selectedFilter == 'all') return pipelines;
    return pipelines.where((p) => p.enabled == (selectedFilter == 'enabled')).toList();
  }

  Color _getStatusColor(PipelineExecutionStatus status) {
    switch (status) {
      case PipelineExecutionStatus.success:
        return Colors.green;
      case PipelineExecutionStatus.failed:
        return Colors.red;
      case PipelineExecutionStatus.running:
        return Colors.blue;
      case PipelineExecutionStatus.pending:
        return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _getFilteredPipelines();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Pipelines'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadPipelines,
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
              children: ['all', 'enabled', 'disabled']
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

          // Pipelines list
          Expanded(
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : filtered.isEmpty
                    ? Center(
                        child: Text(
                          'No pipelines found',
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: (_) => _loadPipelines(),
                        child: ListView.builder(
                          itemCount: filtered.length,
                          itemBuilder: (context, index) {
                            final pipeline = filtered[index];
                            final executions = executionsMap[pipeline.pipelineId] ?? [];
                            return _buildPipelineCard(pipeline, executions);
                          },
                        ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Navigate to create pipeline screen
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildPipelineCard(Pipeline pipeline, List<PipelineExecution> executions) {
    final latestExecution = executions.isNotEmpty ? executions.first : null;
    final statusColor = latestExecution != null
        ? _getStatusColor(latestExecution.status)
        : Colors.grey;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ExpansionTile(
        leading: Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: pipeline.enabled ? Colors.blue.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
            shape: BoxShape.circle,
          ),
          child: Icon(
            pipeline.enabled ? Icons.play_circle : Icons.pause_circle,
            color: pipeline.enabled ? Colors.blue : Colors.grey,
          ),
        ),
        title: Text(pipeline.name),
        subtitle: Text(
          pipeline.description,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Chip(
          label: Text(latestExecution?.status.name ?? 'No runs'),
          backgroundColor: statusColor.withOpacity(0.2),
          labelStyle: TextStyle(color: statusColor, fontSize: 10),
        ),
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildInfoRow('Repository', pipeline.sourceRepository),
                _buildInfoRow('Status', pipeline.enabled ? 'Enabled' : 'Disabled'),
                _buildInfoRow('Stages', pipeline.stages.length.toString()),
                const SizedBox(height: 16),
                Text(
                  'Recent Executions (${executions.length})',
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 12),
                _buildExecutionsList(executions),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton.icon(
                    onPressed: pipeline.enabled
                        ? () {
                            pipelineService.executePipeline(
                              pipelineId: pipeline.pipelineId,
                              trigger: 'manual',
                              branch: 'main',
                            );
                            _loadPipelines();
                          }
                        : null,
                    icon: const Icon(Icons.play_arrow),
                    label: const Text('Run Pipeline'),
                  ),
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
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          Text(
            value,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
          ),
        ],
      ),
    );
  }

  Widget _buildExecutionsList(List<PipelineExecution> executions) {
    if (executions.isEmpty) {
      return Text(
        'No executions yet',
        style: Theme.of(context).textTheme.bodySmall,
      );
    }

    return Column(
      children: executions.take(3).map((exec) {
        final statusColor = _getStatusColor(exec.status);
        return Container(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: statusColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: statusColor.withOpacity(0.3)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '#${exec.executionId.substring(0, 8)}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  Text(
                    exec.branch,
                    style: Theme.of(context)
                        .textTheme
                        .bodySmall
                        ?.copyWith(fontSize: 10),
                  ),
                ],
              ),
              Chip(
                label: Text(exec.status.name),
                backgroundColor: statusColor.withOpacity(0.3),
                labelStyle: TextStyle(color: statusColor, fontSize: 10),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}
