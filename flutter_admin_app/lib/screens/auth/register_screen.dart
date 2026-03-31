import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../config/constants.dart';
import '../../providers/auth_provider.dart';
import '../../utils/validators.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({Key? key}) : super(key: key);

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _handleRegister(BuildContext context) async {
    if (_formKey.currentState?.validate() ?? false) {
      final authProvider = context.read<AuthProvider>();
      
      final success = await authProvider.register(
        _emailController.text.trim(),
        _passwordController.text,
        _nameController.text.trim(),
      );

      if (!mounted) return;

      if (success) {
        Navigator.of(context).pushReplacementNamed('/home');
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(authProvider.error ?? 'Registration failed'),
            backgroundColor: Color(AppConstants.errorColor),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Color(AppConstants.backgroundColor),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppConstants.paddingLarge),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Logo
              Container(
                width: 80,
                height: 80,
                decoration: BoxDecoration(
                  color: Color(AppConstants.primaryColor),
                  borderRadius: BorderRadius.circular(AppConstants.radiusLarge),
                ),
                child: const Icon(
                  Icons.admin_panel_settings,
                  color: Colors.white,
                  size: 48,
                ),
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              // Title
              const Text(
                'Create Admin Account',
                style: TextStyle(
                  fontSize: AppConstants.titleFontSize,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: AppConstants.paddingSmall),
              // Subtitle
              const Text(
                'Register for SupremeAI Admin Portal',
                style: TextStyle(
                  fontSize: AppConstants.bodyFontSize,
                  color: Colors.grey,
                ),
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              // Form
              Form(
                key: _formKey,
                child: Column(
                  children: [
                    // Name Field
                    TextFormField(
                      controller: _nameController,
                      decoration: const InputDecoration(
                        labelText: 'Full Name',
                        hintText: 'John Doe',
                        prefixIcon: Icon(Icons.person_outline),
                      ),
                      validator: Validators.validateName,
                      textInputAction: TextInputAction.next,
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Email Field
                    TextFormField(
                      controller: _emailController,
                      decoration: const InputDecoration(
                        labelText: 'Email',
                        hintText: 'admin@supremeai.com',
                        prefixIcon: Icon(Icons.email_outlined),
                      ),
                      keyboardType: TextInputType.emailAddress,
                      validator: Validators.validateEmail,
                      textInputAction: TextInputAction.next,
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Password Field
                    TextFormField(
                      controller: _passwordController,
                      decoration: InputDecoration(
                        labelText: 'Password',
                        hintText: 'Enter password',
                        prefixIcon: const Icon(Icons.lock_outlined),
                        suffixIcon: IconButton(
                          onPressed: () {
                            setState(() {
                              _obscurePassword = !_obscurePassword;
                            });
                          },
                          icon: Icon(
                            _obscurePassword
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                        ),
                      ),
                      obscureText: _obscurePassword,
                      validator: Validators.validatePassword,
                      textInputAction: TextInputAction.next,
                    ),
                    const SizedBox(height: AppConstants.paddingMedium),
                    // Confirm Password Field
                    TextFormField(
                      controller: _confirmPasswordController,
                      decoration: InputDecoration(
                        labelText: 'Confirm Password',
                        hintText: 'Confirm password',
                        prefixIcon: const Icon(Icons.lock_outlined),
                        suffixIcon: IconButton(
                          onPressed: () {
                            setState(() {
                              _obscureConfirmPassword =
                                  !_obscureConfirmPassword;
                            });
                          },
                          icon: Icon(
                            _obscureConfirmPassword
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                        ),
                      ),
                      obscureText: _obscureConfirmPassword,
                      validator: (value) => Validators.validateConfirmPassword(
                        value,
                        _passwordController.text,
                      ),
                      textInputAction: TextInputAction.done,
                    ),
                    const SizedBox(height: AppConstants.paddingSmall),
                    // Password Helper Text
                    Container(
                      padding: const EdgeInsets.all(AppConstants.paddingSmall),
                      decoration: BoxDecoration(
                        color: Color(AppConstants.infoColor).withOpacity(0.1),
                        borderRadius:
                            BorderRadius.circular(AppConstants.radiusMedium),
                      ),
                      child: const Text(
                        'Password must be at least 8 characters long',
                        style: TextStyle(
                          fontSize: AppConstants.captionFontSize,
                          color: Colors.grey,
                        ),
                      ),
                    ),
                    const SizedBox(height: AppConstants.paddingLarge),
                    // Error Message
                    Consumer<AuthProvider>(
                      builder: (context, authProvider, _) {
                        if (authProvider.error != null) {
                          return Padding(
                            padding: const EdgeInsets.only(
                              bottom: AppConstants.paddingMedium,
                            ),
                            child: Container(
                              padding: const EdgeInsets.all(
                                AppConstants.paddingMedium,
                              ),
                              decoration: BoxDecoration(
                                color: Color(AppConstants.errorColor)
                                    .withOpacity(0.1),
                                borderRadius: BorderRadius.circular(
                                  AppConstants.radiusMedium,
                                ),
                                border: Border.all(
                                  color: Color(AppConstants.errorColor),
                                ),
                              ),
                              child: Text(
                                authProvider.error ?? '',
                                style: TextStyle(
                                  color: Color(AppConstants.errorColor),
                                ),
                              ),
                            ),
                          );
                        }
                        return SizedBox.shrink();
                      },
                    ),
                    // Register Button
                    Consumer<AuthProvider>(
                      builder: (context, authProvider, _) {
                        return SizedBox(
                          width: double.infinity,
                          height: 50,
                          child: ElevatedButton(
                            onPressed: authProvider.isLoading
                                ? null
                                : () => _handleRegister(context),
                            child: authProvider.isLoading
                                ? const SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      valueColor:
                                          AlwaysStoppedAnimation<Color>(
                                        Colors.white,
                                      ),
                                    ),
                                  )
                                : const Text(
                                    'Register',
                                    style: TextStyle(
                                      fontSize: AppConstants.titleFontSize,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppConstants.paddingLarge),
              // Footer
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text('Already have an account? '),
                  TextButton(
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                    child: const Text('Login here'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
