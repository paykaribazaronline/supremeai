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
  late TextEditingController _nameController;
  late TextEditingController _descriptionController;
  String _selectedStatus = 'active';
  String? _selectedAgent;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.project?.name ?? '');
    _descriptionController =
        TextEditingController(text: widget.project?.description ?? '');
    _selectedStatus = widget.project?.status ?? 'active';
    _selectedAgent = widget.project?.aiAgentId;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
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
            _buildFormField(
              label: 'প্রজেক্টের নাম',
              controller: _nameController,
              hint: 'যেমন: MyAwesomeApp',
              helperText: '(প্রজেক্টের একটা নাম দিন)',
              required: true,
            ),
            const SizedBox(height: AppConstants.paddingLarge),
            _buildFormField(
              label: 'বিবরণ',
              controller: _descriptionController,
              hint: 'প্রজেক্টটি কী করবে তা লিখুন...',
              helperText: '(প্রজেক্টের কাজ ও লক্ষ্য সংক্ষেপে লিখুন)',
              maxLines: 4,
              required: true,
            ),
            const SizedBox(height: AppConstants.paddingLarge),
            _buildStatusDropdown(),
            const SizedBox(height: AppConstants.paddingLarge),
            _buildAgentSelector(),
            const SizedBox(height: AppConstants.paddingXLarge),
            if (isEditMode) _buildProjectInfo(),
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
          Text(helperText, style: const TextStyle(fontSize: 11, color: Colors.grey)),
        const SizedBox(height: AppConstants.paddingSmall),
        TextField(
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

  Widget _buildStatusDropdown() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'অবস্থা',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
        const Text('(প্রজেক্ট এখন কোন পর্যায়ে আছে)', style: TextStyle(fontSize: 11, color: Colors.grey)),
        const SizedBox(height: AppConstants.paddingSmall),
        DropdownButtonFormField<String>(
          value: _selectedStatus,
          items: ['active', 'inactive', 'building', 'error']
              .map((status) => DropdownMenuItem(
                    value: status,
                    child: Text(status.toUpperCase()),
                  ))
              .toList(),
          onChanged: (value) {
            setState(() => _selectedStatus = value ?? 'active');
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

  Widget _buildAgentSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'AI এজেন্ট নির্বাচন (ঐচ্ছিক)',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
        const Text('(কোন AI এজেন্ট এই প্রজেক্টে কাজ করবে তা বেছে নিন)', style: TextStyle(fontSize: 11, color: Colors.grey)),
        const SizedBox(height: AppConstants.paddingSmall),
        Container(
          padding: const EdgeInsets.all(AppConstants.paddingMedium),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
          ),
          child: Column(
            children: [
              ListTile(
                title: const Text('Architect Agent'),
                subtitle: const Text('ডিজাইন ও স্থাপত্য তৈরি করে'),
                selected: _selectedAgent == 'architect',
                onTap: () => setState(() => _selectedAgent = 'architect'),
              ),
              Divider(color: Colors.grey[300]),
              ListTile(
                title: const Text('Builder Agent'),
                subtitle: const Text('কোড লেখে ও তৈরি করে'),
                selected: _selectedAgent == 'builder',
                onTap: () => setState(() => _selectedAgent = 'builder'),
              ),
              Divider(color: Colors.grey[300]),
              ListTile(
                title: const Text('Reviewer Agent'),
                subtitle: const Text('মান যাচাই ও টেস্ট করে'),
                selected: _selectedAgent == 'reviewer',
                onTap: () => setState(() => _selectedAgent = 'reviewer'),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildProjectInfo() {
    return Container(
      padding: const EdgeInsets.all(AppConstants.paddingMedium),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'প্রজেক্টের তথ্য',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const Text('(সিস্টেম থেকে স্বয়ংক্রিয় তথ্য)', style: TextStyle(fontSize: 11, color: Colors.grey)),
          const SizedBox(height: AppConstants.paddingMedium),
          _buildInfoRow('Project ID', widget.project!.id),
          _buildInfoRow(
            'Created',
            widget.project!.createdAt.toString().split('.')[0],
          ),
          _buildInfoRow(
            'Last Updated',
            widget.project!.updatedAt.toString().split('.')[0],
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
                : Text(widget.project == null ? 'তৈরি করুন' : 'আপডেট করুন'),
          ),
        ),
      ],
    );
  }

  void _saveProject() async {
    if (_nameController.text.isEmpty || _descriptionController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('সব ফিল্ড পূরণ করুন')),
      );
      return;
    }

    setState(() => _isSaving = true);

    try {
      final project = Project(
        id: widget.project?.id ?? 'project_${DateTime.now().millisecondsSinceEpoch}',
        name: _nameController.text,
        description: _descriptionController.text,
        status: _selectedStatus,
        aiAgentId: _selectedAgent,
        createdAt: widget.project?.createdAt ?? DateTime.now(),
        updatedAt: DateTime.now(),
      );

      if (widget.project == null) {
        await Provider.of<ProjectsProvider>(context, listen: false)
            .createProject(project);
      } else {
        await Provider.of<ProjectsProvider>(context, listen: false)
            .updateProject(project);
      }

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              widget.project == null
                  ? 'প্রজেক্ট সফলভাবে তৈরি হয়েছে!'
                  : 'প্রজেক্ট আপডেট হয়েছে!',
            ),
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
