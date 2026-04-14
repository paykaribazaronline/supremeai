import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../models/models.dart';
import '../../providers/projects_provider.dart';

class ProjectDetailScreen extends StatefulWidget {
  final Project? project;

  const ProjectDetailScreen({Key? key, this.project}) : super(key: key);

  @override
  State<ProjectDetailScreen> createState() => _ProjectDetailScreenState();
}

class _ProjectDetailScreenState extends State<ProjectDetailScreen> {
  late TextEditingController _projectIdController;
  late TextEditingController _descriptionController;
  late TextEditingController _repoUrlController;
  late TextEditingController _repoBranchController;
  late TextEditingController _repoTokenController;
  late TextEditingController _featuresController;
  String _selectedTemplate = 'REACT';
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _projectIdController =
        TextEditingController(text: widget.project?.id ?? '');
    _descriptionController =
        TextEditingController(text: widget.project?.description ?? '');
    _repoUrlController =
        TextEditingController(text: widget.project?.repoUrl ?? '');
    _repoBranchController =
        TextEditingController(text: widget.project?.repoBranch ?? 'main');
    _repoTokenController = TextEditingController();
    _featuresController =
        TextEditingController(text: widget.project?.features.join('\n') ?? '');
    _selectedTemplate = widget.project?.templateType ?? 'REACT';
  }

  @override
  void dispose() {
    _projectIdController.dispose();
    _descriptionController.dispose();
    _repoUrlController.dispose();
    _repoBranchController.dispose();
    _repoTokenController.dispose();
    _featuresController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEditMode = widget.project != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditMode ? 'প্রজেক্ট সম্পাদনা' : 'নতুন প্রজেক্ট'),
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppConstants.paddingLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (isEditMode) ...[
              _buildProjectInfo(),
            ] else ...[
              _buildFormField(
                label: 'Project ID',
                controller: _projectIdController,
                hint: 'যেমন: dolilbook',
                helperText: '(শুধু letters, numbers, dots, hyphen, underscore)',
                required: true,
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildTemplateDropdown(),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildFormField(
                label: 'Description',
                controller: _descriptionController,
                hint: 'প্রজেক্টটি কী করবে তা লিখুন...',
                helperText:
                    '(এই বিবরণ Existing App workflow তেও improvement goal হিসেবে যাবে)',
                maxLines: 4,
                required: true,
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildFormField(
                label: 'Features',
                controller: _featuresController,
                hint: 'One feature per line',
                helperText: '(প্রতি লাইনে একটি feature লিখুন)',
                maxLines: 5,
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildFormField(
                label: 'GitHub Repo URL',
                controller: _repoUrlController,
                hint: 'https://github.com/owner/repo',
                helperText: '(empty repo URL required)',
                required: true,
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildFormField(
                label: 'Branch',
                controller: _repoBranchController,
                hint: 'main',
                helperText: '(default main)',
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              _buildFormField(
                label: 'GitHub Token',
                controller: _repoTokenController,
                hint: 'Private repo হলে দিন',
                helperText: '(private repo এর জন্য optional token)',
              ),
            ],
            const SizedBox(height: AppConstants.paddingXLarge),
            _buildActionButtons(),
          ],
        ),
      ),
    );
  }

  Widget _buildFormField({
    required String label,
    required TextEditingController controller,
    required String hint,
    bool required = false,
    int maxLines = 1,
    String? helperText,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              label,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
            if (required)
              const Text(
                ' *',
                style: TextStyle(color: Colors.red),
              ),
          ],
        ),
        if (helperText != null)
          Text(helperText,
              style: const TextStyle(fontSize: 11, color: Colors.grey)),
        const SizedBox(height: AppConstants.paddingSmall),
        TextFormField(
          controller: controller,
          maxLines: maxLines,
          decoration: InputDecoration(
            hintText: hint,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppConstants.paddingMedium,
              vertical: AppConstants.paddingMedium,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTemplateDropdown() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Template Type',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
        const Text('(কোন ধরনের project generate হবে)',
            style: TextStyle(fontSize: 11, color: Colors.grey)),
        const SizedBox(height: AppConstants.paddingSmall),
        DropdownButtonFormField<String>(
          initialValue: _selectedTemplate,
          items: ['REACT', 'SPRING_BOOT', 'FLUTTER', 'FULL_STACK', 'REST_API', 'PYTHON_FASTAPI', 'NODE_EXPRESS', 'ANDROID_KOTLIN', 'NEXT_JS']
              .map((template) => DropdownMenuItem(
                    value: template,
                    child: Text(template),
                  ))
              .toList(),
          onChanged: (value) {
            setState(() => _selectedTemplate = value ?? 'REACT');
          },
          decoration: InputDecoration(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppConstants.paddingMedium,
              vertical: AppConstants.paddingMedium,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildProjectInfo() {
    final features = widget.project!.features;
    return Column(
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          decoration: BoxDecoration(
            color: Colors.grey[100],
            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
            border: Border.all(color: Colors.grey[300]!),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.info_outline, size: 18, color: Colors.blue),
                  SizedBox(width: 8),
                  Text(
                    'প্রজেক্টের তথ্য',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                  ),
                ],
              ),
              const Text('(সিস্টেম থেকে স্বয়ংক্রিয় তথ্য)',
                  style: TextStyle(fontSize: 11, color: Colors.grey)),
              const Divider(height: 24),
              _buildInfoRow('Project ID', widget.project!.id),
              _buildInfoRow('Template', widget.project!.templateType),
              _buildStatusRow('Status', widget.project!.status),
              _buildInfoRow('Repo',
                  widget.project!.repoUrl.isEmpty ? '-' : widget.project!.repoUrl),
              _buildInfoRow('Branch', widget.project!.repoBranch),
              _buildProgressRow('Progress', widget.project!.progress),
              _buildInfoRow('Files', widget.project!.fileCount.toString()),
              _buildInfoRow('Tracked For Improvement',
                  widget.project!.trackedForImprovement ? 'Yes' : 'No'),
              _buildInfoRow(
                'Created',
                widget.project!.createdAt.toString().split('.')[0],
              ),
              if (widget.project!.completedAt != null)
                _buildInfoRow(
                  'Completed',
                  widget.project!.completedAt.toString().split('.')[0],
                ),
            ],
          ),
        ),
        if (features.isNotEmpty) ...[
          const SizedBox(height: AppConstants.paddingLarge),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(AppConstants.paddingMedium),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.05),
              borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
              border: Border.all(color: Colors.blue.withValues(alpha: 0.2)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.list, size: 18, color: Colors.blue),
                    SizedBox(width: 8),
                    Text(
                      'নির্ধারিত ফিচারসমূহ',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                ...features.map((f) => Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(Icons.check_circle, size: 14, color: Colors.green),
                          const SizedBox(width: 8),
                          Expanded(child: Text(f, style: const TextStyle(fontSize: 13))),
                        ],
                      ),
                    )),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildStatusRow(String label, String status) {
    Color statusColor = Colors.grey;
    if (status == 'COMPLETED') statusColor = Colors.green;
    if (status == 'GENERATING' || status == 'IN_PROGRESS') statusColor = Colors.blue;
    if (status == 'FAILED') statusColor = Colors.red;

    return Padding(
      padding: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.w500, color: Colors.grey)),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
              color: statusColor.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: statusColor.withValues(alpha: 0.5)),
            ),
            child: Text(
              status,
              style: TextStyle(fontWeight: FontWeight.bold, color: statusColor, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressRow(String label, int progress) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.w500, color: Colors.grey)),
          Row(
            children: [
              SizedBox(
                width: 60,
                child: LinearProgressIndicator(
                  value: progress / 100,
                  backgroundColor: Colors.grey[300],
                  valueColor: AlwaysStoppedAnimation<Color>(
                    progress == 100 ? Colors.green : Colors.blue,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                '$progress%',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppConstants.paddingSmall),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontWeight: FontWeight.w500,
              color: Colors.grey,
            ),
          ),
          Text(
            value,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      children: [
        Expanded(
          child: OutlinedButton(
            onPressed: _isSaving ? null : () => Navigator.pop(context),
            child: const Text('বাতিল'),
          ),
        ),
        const SizedBox(width: AppConstants.paddingMedium),
        Expanded(
          child: ElevatedButton(
            onPressed: _isSaving ? null : _saveProject,
            child: _isSaving
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(widget.project == null ? 'তৈরি করুন' : 'ফিরে যান'),
          ),
        ),
      ],
    );
  }

  void _saveProject() async {
    if (widget.project != null) {
      Navigator.pop(context);
      return;
    }

    if (_projectIdController.text.isEmpty ||
        _descriptionController.text.isEmpty ||
        _repoUrlController.text.isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('সব ফিল্ড পূরণ করুন')),
      );
      return;
    }

    setState(() => _isSaving = true);

    try {
      final features = _featuresController.text
          .split('\n')
          .map((line) => line.trim())
          .where((line) => line.isNotEmpty)
          .toList();

      final project = Project(
        id: _projectIdController.text.trim(),
        name: _projectIdController.text.trim(),
        description: _descriptionController.text,
        status: 'GENERATING',
        templateType: _selectedTemplate,
        repoUrl: _repoUrlController.text.trim(),
        repoBranch: _repoBranchController.text.trim().isEmpty
            ? 'main'
            : _repoBranchController.text.trim(),
        repoToken: _repoTokenController.text.trim(),
        progress: 0,
        fileCount: 0,
        pushed: false,
        features: features,
        createdAt: DateTime.now(),
      );

      await Provider.of<ProjectsProvider>(context, listen: false)
          .createProject(project);

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
                'প্রজেক্ট সফলভাবে তৈরি হয়েছে এবং Existing App workflow-এ sync হবে।'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e')),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }
}
