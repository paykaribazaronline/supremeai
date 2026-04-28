import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/settings_provider.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _modelController = TextEditingController();
  final _smallModelController = TextEditingController();
  bool _fullAuthority = false;
  bool _externalDirectory = false;
  String _shareMode = 'manual';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final auth = context.read<AuthProvider>();
      final settingsProvider = context.read<SettingsProvider>();
      await settingsProvider.loadFromBackend(authToken: auth.token);
      _bind(settingsProvider.settings);
    });
  }

  void _bind(SupremeAISettings settings) {
    _modelController.text = settings.model;
    _smallModelController.text = settings.smallModel;
    _fullAuthority = settings.fullAuthority;
    _externalDirectory = settings.enableExternalDirectory;
    _shareMode = settings.shareMode;
    setState(() {});
  }

  @override
  void dispose() {
    _modelController.dispose();
    _smallModelController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final auth = context.read<AuthProvider>();
    final provider = context.read<SettingsProvider>();
    final current = provider.settings;

    provider.update(
      current.copyWith(
        model: _modelController.text.trim(),
        smallModel: _smallModelController.text.trim(),
        fullAuthority: _fullAuthority,
        shareMode: _shareMode,
        enableExternalDirectory: _externalDirectory,
      ),
    );

    if (_fullAuthority) {
      provider.setFullAuthority(true);
    }

    final ok = await provider.saveToBackend(authToken: auth.token);
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(ok
            ? 'Settings saved successfully'
            : (provider.error ?? 'Save failed')),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('SupremeAI Settings')),
      body: Consumer<SettingsProvider>(
        builder: (context, settingsProvider, _) {
          if (settingsProvider.isLoading &&
              settingsProvider.settings.model.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              TextField(
                controller: _modelController,
                decoration: const InputDecoration(
                  labelText: 'Primary model',
                  hintText: 'google/gemini-1.5-pro',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _smallModelController,
                decoration: const InputDecoration(
                  labelText: 'Small model',
                  hintText: 'google/gemini-1.5-flash',
                ),
              ),
              const SizedBox(height: 12),
               DropdownButtonFormField<String>(
                 initialValue: _shareMode,
                items: const [
                  DropdownMenuItem(
                      value: 'manual', child: Text('Manual share')),
                  DropdownMenuItem(value: 'auto', child: Text('Auto share')),
                  DropdownMenuItem(
                      value: 'disabled', child: Text('Sharing disabled')),
                ],
                onChanged: (value) {
                  if (value == null) return;
                  setState(() => _shareMode = value);
                },
                decoration: const InputDecoration(labelText: 'Share mode'),
              ),
              const SizedBox(height: 12),
              SwitchListTile(
                title: const Text('Full authority mode'),
                subtitle: const Text(
                    'Allows all core tools without extra confirmation'),
                value: _fullAuthority,
                onChanged: (value) => setState(() => _fullAuthority = value),
              ),
              SwitchListTile(
                title: const Text('Allow external directory access'),
                subtitle:
                    const Text('Can access directories outside workspace'),
                value: _externalDirectory,
                onChanged: (value) =>
                    setState(() => _externalDirectory = value),
              ),
              const SizedBox(height: 12),
              if (settingsProvider.error != null)
                Text(
                  settingsProvider.error!,
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              const SizedBox(height: 12),
              ElevatedButton.icon(
                onPressed: settingsProvider.isLoading ? null : _save,
                icon: const Icon(Icons.save),
                label: Text(
                    settingsProvider.isLoading ? 'Saving...' : 'Save Settings'),
              ),
            ],
          );
        },
      ),
    );
  }
}
