import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../config/app_routes.dart';
import '../config/constants.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final ApiService _apiService = ApiService();
  late TextEditingController _nameController;
  late TextEditingController _emailController;
  bool _notificationsEnabled = true;
  bool _darkModeEnabled = false;
  String _autoRefreshInterval = '5min';
  bool _isUpdatingProfile = false;
  bool _isChangingPassword = false;

  @override
  void initState() {
    super.initState();
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    _nameController =
        TextEditingController(text: authProvider.currentUser?['name'] ?? '');
    _emailController =
        TextEditingController(text: authProvider.currentUser?['email'] ?? '');
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('সেটিংস'),
        elevation: 0,
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Profile Section
            _buildSection(
              'প্রোফাইল',
              '(আপনার নাম ও ইমেইল পরিবর্তন করুন)',
              [
                _buildTextField(
                  label: 'পুরো নাম',
                  controller: _nameController,
                  icon: Icons.person,
                  helperText: '(আপনার নাম)',
                ),
                _buildTextField(
                  label: 'ইমেইল',
                  controller: _emailController,
                  icon: Icons.email,
                  readOnly: true,
                  helperText: '(পরিবর্তন করা যাবে না)',
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppConstants.paddingLarge,
                    vertical: AppConstants.paddingMedium,
                  ),
                  child: ElevatedButton(
                    onPressed: _isUpdatingProfile ? null : _updateProfile,
                    child: _isUpdatingProfile
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Text('প্রোফাইল আপডেট করুন'),
                  ),
                ),
              ],
            ),

            // Preferences Section
            _buildSection(
              'পছন্দসমূহ',
              '(সিস্টেমের আচরণ পরিবর্তন করুন)',
              [
                _buildToggleTile(
                  title: 'নোটিফিকেশন চালু',
                  subtitle: 'সিস্টেমের খবর ও সতর্কতা পান (চালু রাখুন)',
                  value: _notificationsEnabled,
                  onChanged: (value) {
                    setState(() => _notificationsEnabled = value);
                  },
                ),
                _buildToggleTile(
                  title: 'ডার্ক মোড',
                  subtitle: 'অন্ধকার থিমে বদলান (চোখে আরাম হয়)',
                  value: _darkModeEnabled,
                  onChanged: (value) {
                    setState(() => _darkModeEnabled = value);
                  },
                ),
                _buildDropdownTile(
                  title: 'অটো-রিফ্রেশ সময়',
                  subtitle: 'কতক্ষণ পরপর ডেটা আপনাআপনি রিফ্রেশ হবে',
                  value: _autoRefreshInterval,
                  options: {
                    '1min': '১ মিনিট',
                    '5min': '৫ মিনিট',
                    '10min': '১০ মিনিট',
                    '30min': '৩০ মিনিট',
                    'manual': 'নিজে রিফ্রেশ করবো',
                  },
                  onChanged: (value) {
                    setState(() => _autoRefreshInterval = value);
                  },
                ),
              ],
            ),

            // Security Section
            _buildSection(
              'নিরাপত্তা',
              '(পাসওয়ার্ড ও টু-ফ্যাক্টর অথেন্টিকেশন)',
              [
                ListTile(
                  leading: const Icon(Icons.lock),
                  title: const Text('পাসওয়ার্ড পরিবর্তন'),
                  subtitle: const Text('আপনার গোপন পাসওয়ার্ড বদলান',
                      style: TextStyle(fontSize: 12)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                  onTap: _changePassword,
                ),
                ListTile(
                  leading: const Icon(Icons.security),
                  title: const Text('টু-ফ্যাক্টর অথেন্টিকেশন'),
                  subtitle: const Text(
                      'অতিরিক্ত নিরাপত্তা যোগ করুন (শীঘ্রই আসছে)',
                      style: TextStyle(fontSize: 12)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                  onTap: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('2FA শীঘ্রই আসছে'),
                      ),
                    );
                  },
                ),
              ],
            ),

            // About Section
            _buildSection(
              'তথ্য',
              '(অ্যাপের ভার্সন ও লাইসেন্স)',
              [
                _buildInfoTile(
                  title: 'অ্যাপ ভার্সন',
                  value: '1.0.0',
                ),
                _buildInfoTile(
                  title: 'বিল্ড নম্বর',
                  value: '2026.04.01',
                ),
                ListTile(
                  leading: const Icon(Icons.info),
                  title: const Text('লাইসেন্স'),
                  subtitle: const Text('ওপেন সোর্স লাইসেন্স দেখুন',
                      style: TextStyle(fontSize: 12)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                  onTap: () {
                    showLicensePage(context: context);
                  },
                ),
              ],
            ),

            // Danger Zone
            _buildSection(
              'সতর্কতা!',
              '(সাবধানে ব্যবহার করুন)',
              [
                ListTile(
                  leading: const Icon(Icons.logout, color: Colors.red),
                  title: const Text(
                    'লগআউট',
                    style: TextStyle(color: Colors.red),
                  ),
                  subtitle: const Text('অ্যাকাউন্ট থেকে বের হন',
                      style: TextStyle(fontSize: 12, color: Colors.red)),
                  onTap: _logout,
                ),
              ],
            ),

            const SizedBox(height: AppConstants.paddingXLarge),
          ],
        ),
      ),
    );
  }

  Widget _buildSection(String title, String hint, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppConstants.paddingLarge,
            AppConstants.paddingXLarge,
            AppConstants.paddingLarge,
            AppConstants.paddingXSmall,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                hint,
                style: const TextStyle(fontSize: 11, color: Colors.grey),
              ),
            ],
          ),
        ),
        Card(
          margin: const EdgeInsets.symmetric(
            horizontal: AppConstants.paddingMedium,
          ),
          child: Column(
            children: children,
          ),
        ),
      ],
    );
  }

  Widget _buildTextField({
    required String label,
    required TextEditingController controller,
    required IconData icon,
    bool readOnly = false,
    String? helperText,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppConstants.paddingLarge,
        vertical: AppConstants.paddingSmall,
      ),
      child: TextField(
        controller: controller,
        readOnly: readOnly,
        decoration: InputDecoration(
          labelText: label,
          helperText: helperText,
          prefixIcon: Icon(icon),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
          ),
        ),
      ),
    );
  }

  Widget _buildToggleTile({
    required String title,
    required String subtitle,
    required bool value,
    required Function(bool) onChanged,
  }) {
    return SwitchListTile(
      title: Text(title),
      subtitle: Text(subtitle),
      value: value,
      onChanged: onChanged,
    );
  }

  Widget _buildDropdownTile({
    required String title,
    required String subtitle,
    required String value,
    required Map<String, String> options,
    required Function(String) onChanged,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppConstants.paddingMedium,
        vertical: AppConstants.paddingSmall,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          Text(
            subtitle,
            style: const TextStyle(fontSize: 12, color: Colors.grey),
          ),
          const SizedBox(height: AppConstants.paddingSmall),
          DropdownButtonFormField<String>(
            initialValue: value,
            items: options.entries
                .map((e) => DropdownMenuItem(
                      value: e.key,
                      child: Text(e.value),
                    ))
                .toList(),
            onChanged: (v) => onChanged(v ?? value),
            decoration: InputDecoration(
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(AppConstants.radiusMedium),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoTile({required String title, required String value}) {
    return ListTile(
      title: Text(title),
      trailing: Text(
        value,
        style: const TextStyle(fontWeight: FontWeight.bold),
      ),
    );
  }

  void _updateProfile() async {
    final name = _nameController.text.trim();
    if (name.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('নাম খালি রাখা যাবে না'),
            backgroundColor: Color(AppConstants.errorColor)),
      );
      return;
    }

    setState(() => _isUpdatingProfile = true);

    try {
      final response = await _apiService.put<Map<String, dynamic>>(
        '/api/auth/profile',
        data: {'name': name},
      );

      if (!mounted) return;
      setState(() => _isUpdatingProfile = false);

      if (response.success) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('প্রোফাইল সফলভাবে আপডেট হয়েছে ✅'),
            backgroundColor: Color(AppConstants.successColor),
          ),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('আপডেট ব্যর্থ: ${response.error ?? "অজানা সমস্যা"}'),
            backgroundColor: const Color(AppConstants.errorColor),
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _isUpdatingProfile = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('ত্রুটি: $e'),
          backgroundColor: const Color(AppConstants.errorColor),
        ),
      );
    }
  }

  void _changePassword() {
    final oldPasswordController = TextEditingController();
    final newPasswordController = TextEditingController();
    final confirmPasswordController = TextEditingController();

    showDialog(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Text('পাসওয়ার্ড পরিবর্তন'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('(নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে)',
                  style: TextStyle(fontSize: 12, color: Colors.grey)),
              const SizedBox(height: AppConstants.paddingMedium),
              TextField(
                controller: oldPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'বর্তমান পাসওয়ার্ড',
                  helperText: '(এখন যে পাসওয়ার্ড ব্যবহার করছেন)',
                ),
              ),
              const SizedBox(height: AppConstants.paddingMedium),
              TextField(
                controller: newPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'নতুন পাসওয়ার্ড',
                  helperText: '(নতুন পাসওয়ার্ড দিন)',
                ),
              ),
              const SizedBox(height: AppConstants.paddingMedium),
              TextField(
                controller: confirmPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'নতুন পাসওয়ার্ড নিশ্চিত',
                  helperText: '(আবার লিখুন)',
                ),
              ),
              if (_isChangingPassword)
                const Padding(
                  padding: EdgeInsets.only(top: 16),
                  child: CircularProgressIndicator(),
                ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: _isChangingPassword
                  ? null
                  : () {
                      oldPasswordController.dispose();
                      newPasswordController.dispose();
                      confirmPasswordController.dispose();
                      Navigator.pop(dialogContext);
                    },
              child: const Text('বাতিল'),
            ),
            TextButton(
              onPressed: _isChangingPassword
                  ? null
                  : () async {
                      final oldPass = oldPasswordController.text;
                      final newPass = newPasswordController.text;
                      final confirmPass = confirmPasswordController.text;

                      if (oldPass.isEmpty ||
                          newPass.isEmpty ||
                          confirmPass.isEmpty) {
                        ScaffoldMessenger.of(this.context).showSnackBar(
                          const SnackBar(
                            content: Text('সব ফিল্ড পূরণ করুন'),
                            backgroundColor: Color(AppConstants.errorColor),
                          ),
                        );
                        return;
                      }

                      if (newPass != confirmPass) {
                        ScaffoldMessenger.of(this.context).showSnackBar(
                          const SnackBar(
                            content: Text('নতুন পাসওয়ার্ড মিলছে না'),
                            backgroundColor: Color(AppConstants.errorColor),
                          ),
                        );
                        return;
                      }

                      if (newPass.length < 6) {
                        ScaffoldMessenger.of(this.context).showSnackBar(
                          const SnackBar(
                            content:
                                Text('পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে'),
                            backgroundColor: Color(AppConstants.errorColor),
                          ),
                        );
                        return;
                      }

                      setState(() => _isChangingPassword = true);
                      setDialogState(() {});

                      try {
                        final response =
                            await _apiService.post<Map<String, dynamic>>(
                          '/api/auth/change-password',
                          data: {
                            'oldPassword': oldPass,
                            'newPassword': newPass,
                          },
                        );

                        setState(() => _isChangingPassword = false);

                        oldPasswordController.dispose();
                        newPasswordController.dispose();
                        confirmPasswordController.dispose();

                        if (!mounted) return;

                        Navigator.pop(dialogContext);

                        if (response.success) {
                          ScaffoldMessenger.of(this.context).showSnackBar(
                            const SnackBar(
                              content:
                                  Text('পাসওয়ার্ড সফলভাবে পরিবর্তন হয়েছে ✅'),
                              backgroundColor: Color(AppConstants.successColor),
                            ),
                          );
                        } else {
                          ScaffoldMessenger.of(this.context).showSnackBar(
                            SnackBar(
                              content: Text(
                                  'ব্যর্থ: ${response.error ?? "অজানা সমস্যা"}'),
                              backgroundColor:
                                  const Color(AppConstants.errorColor),
                            ),
                          );
                        }
                      } catch (e) {
                        setState(() => _isChangingPassword = false);
                        if (!mounted) return;
                        Navigator.pop(dialogContext);
                        ScaffoldMessenger.of(this.context).showSnackBar(
                          SnackBar(
                            content: Text('ত্রুটি: $e'),
                            backgroundColor:
                                const Color(AppConstants.errorColor),
                          ),
                        );
                      }
                    },
              child: const Text('পরিবর্তন করুন'),
            ),
          ],
        ),
      ),
    );
  }

  void _logout() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('লগআউট করবেন?'),
        content: const Text(
            'আপনি কি নিশ্চিত লগআউট করতে চান?\n(লগআউট করলে আবার লগইন করতে হবে)'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('না, থাকুন'),
          ),
          TextButton(
            onPressed: () {
              Provider.of<AuthProvider>(context, listen: false).logout();
              Navigator.pop(context);
              Navigator.of(context).pushReplacementNamed(AppRoutes.login);
            },
            child:
                const Text('হ্যাঁ, লগআউট', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
