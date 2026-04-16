import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/models.dart';
import '../../services/api_service.dart';
import '../../providers/auth_provider.dart';
import '../../utils/validators.dart';

class KnowledgeSeedScreen extends StatefulWidget {
  const KnowledgeSeedScreen({super.key});

  @override
  State<KnowledgeSeedScreen> createState() => _KnowledgeSeedScreenState();
}

class _KnowledgeSeedScreenState extends State<KnowledgeSeedScreen> {
  final _formKey = GlobalKey<FormState>();
  final _sourceAiController = TextEditingController();
  final _promptController = TextEditingController();
  final _responseController = TextEditingController();
  final _categoryController = TextEditingController();
  final _notesController = TextEditingController();
  double _confidence = 0.5;
  bool _isSubmitting = false;
  String _selectedCategory = 'uncategorized';

  final List<String> _categories = const [
    'uncategorized',
    'APP_CREATION',
    'ERROR_SOLVING',
    'ARCHITECTURE',
    'SECURITY',
    'CI_CD',
    'PERFORMANCE',
    'QUOTA_POLICY',
    'INCIDENT_LEARNING',
    'OPERATIONS',
    'BACKEND_SERVICES',
  ];

  @override
  void dispose() {
    _sourceAiController.dispose();
    _promptController.dispose();
    _responseController.dispose();
    _categoryController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  Future<void> _submitSeed() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);

    try {
      final auth = Provider.of<AuthProvider>(context, listen: false);
      final seed = ExternalKnowledgeSeed(
        id: '',
        sourceAiModelName: _sourceAiController.text.trim(),
        prompt: _promptController.text.trim(),
        aiResponse: _responseController.text.trim(),
        category: _selectedCategory,
        confidence: _confidence,
        adminNotes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
        seededByUserId: auth.user?.id ?? '',
        createdAt: DateTime.now(),
      );

      await ApiService.instance.createKnowledgeSeed(seed);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Knowledge seed stored successfully'), backgroundColor: Colors.green),
        );
        _clearForm();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to store seed: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  void _clearForm() {
    _sourceAiController.clear();
    _promptController.clear();
    _responseController.clear();
    _notesController.clear();
    _confidence = 0.5;
    _selectedCategory = 'uncategorized';
    _formKey.currentState?.reset();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Seed External AI Knowledge'), elevation: 0),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Card(elevation: 2, child: Padding(padding: const EdgeInsets.all(16.0), child: Column(children: [
              TextFormField(
                controller: _sourceAiController,
                decoration: const InputDecoration(
                  labelText: 'AI Model Name',
                  hintText: 'Enter name of external AI model',
                  border: OutlineInputBorder(),
                ),
                validator: Validators.required,
                enabled: !_isSubmitting,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                value: _selectedCategory,
                decoration: const InputDecoration(labelText: 'Knowledge Category', border: OutlineInputBorder()),
                items: _categories.map((cat) => DropdownMenuItem(value: cat, child: Text(cat))).toList(),
                onChanged: _isSubmitting ? null : (val) => setState(() => _selectedCategory = val ?? 'uncategorized'),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _promptController,
                maxLines: 4,
                decoration: const InputDecoration(labelText: 'Prompt Sent to AI', border: OutlineInputBorder()),
                validator: Validators.required,
                enabled: !_isSubmitting,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _responseController,
                maxLines: 8,
                decoration: const InputDecoration(labelText: 'AI Response Output', border: OutlineInputBorder()),
                validator: Validators.required,
                enabled: !_isSubmitting,
              ),
              const SizedBox(height: 16),
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('Confidence Score: ${(_confidence * 100).toStringAsFixed(0)}%'),
                Slider(value: _confidence, onChanged: _isSubmitting ? null : (val) => setState(() => _confidence = val), divisions: 20, min: 0, max: 1.0),
              ]),
              const SizedBox(height: 16),
              TextFormField(
                controller: _notesController,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Admin Notes (Optional)', border: OutlineInputBorder()),
                enabled: !_isSubmitting,
              ),
            ]))),
            const SizedBox(height: 24),
            SizedBox(width: double.infinity, child: ElevatedButton(
              onPressed: _isSubmitting ? null : _submitSeed,
              style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
              child: _isSubmitting ? const CircularProgressIndicator() : const Text('Store Knowledge Seed', style: TextStyle(fontSize: 16)),
            )),
          ]),
        ),
      ),
    );
  }
}
