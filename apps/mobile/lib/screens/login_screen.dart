import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/localization_service.dart';
import '../theme/app_theme.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> with SingleTickerProviderStateMixin {
  final emailCtrl = TextEditingController();
  final passCtrl = TextEditingController();
  late AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _pulseController.dispose();
    emailCtrl.dispose();
    passCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: SupremeColors.bgVoid,
      body: Stack(
        children: [
          // Background Glow Effects
          Positioned(
            top: -100,
            left: -100,
            child: AnimatedBuilder(
              animation: _pulseController,
              builder: (context, child) {
                return Container(
                  width: 300,
                  height: 300,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: SupremeColors.brandPrimary.withOpacity(0.1 + (_pulseController.value * 0.1)),
                    boxShadow: [
                      BoxShadow(
                        color: SupremeColors.brandPrimary.withOpacity(0.2),
                        blurRadius: 100,
                        spreadRadius: 50,
                      )
                    ],
                  ),
                );
              }
            ),
          ),
          Positioned(
            bottom: -100,
            right: -100,
            child: AnimatedBuilder(
              animation: _pulseController,
              builder: (context, child) {
                return Container(
                  width: 300,
                  height: 300,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: SupremeColors.brandSecondary.withOpacity(0.1 + ((1 - _pulseController.value) * 0.1)),
                    boxShadow: [
                      BoxShadow(
                        color: SupremeColors.brandSecondary.withOpacity(0.2),
                        blurRadius: 100,
                        spreadRadius: 50,
                      )
                    ],
                  ),
                );
              }
            ),
          ),
          
          // Glassmorphism Content
          Center(
            child: SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(24),
                  child: BackdropFilter(
                    filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                    child: Container(
                      padding: const EdgeInsets.all(32),
                      decoration: BoxDecoration(
                        color: SupremeColors.bgCard,
                        borderRadius: BorderRadius.circular(24),
                        border: Border.all(
                          color: SupremeColors.brandPrimary.withOpacity(0.2),
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.2),
                            blurRadius: 20,
                          )
                        ],
                      ),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // App Title with Gradient
                          ShaderMask(
                            shaderCallback: (bounds) => const LinearGradient(
                              colors: [SupremeColors.brandPrimary, SupremeColors.brandSecondary],
                            ).createShader(bounds),
                            child: Text(
                              '⚡ SUPREME AI',
                              style: const TextStyle(
                                fontSize: 36,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                                letterSpacing: 2,
                              ),
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '2.0',
                            style: TextStyle(
                              fontSize: 16,
                              color: SupremeColors.brandPrimary,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 4,
                            ),
                          ),
                          const SizedBox(height: 48),

                          // Input Fields
                          _buildGlowingTextField(
                            controller: emailCtrl,
                            label: 'Email',
                            icon: Icons.email_outlined,
                            keyboardType: TextInputType.emailAddress,
                          ),
                          const SizedBox(height: 20),
                          _buildGlowingTextField(
                            controller: passCtrl,
                            label: 'Password',
                            icon: Icons.lock_outline,
                            obscureText: true,
                          ),
                          const SizedBox(height: 32),

                          // Login Button
                          Container(
                            width: double.infinity,
                            height: 56,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(16),
                              gradient: const LinearGradient(
                                colors: [SupremeColors.brandPrimary, SupremeColors.brandSecondary],
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: SupremeColors.brandPrimary.withOpacity(0.4),
                                  blurRadius: 16,
                                  offset: const Offset(0, 4),
                                )
                              ],
                            ),
                            child: ElevatedButton(
                              onPressed: _handleLogin,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.transparent,
                                shadowColor: Colors.transparent,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(16),
                                ),
                              ),
                              child: Text(
                                'btn.login'.tr(),
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: SupremeColors.bgVoid,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 24),

                          // Google Sign-In
                          OutlinedButton.icon(
                            onPressed: _handleGoogleLogin,
                            style: OutlinedButton.styleFrom(
                              minimumSize: const Size(double.infinity, 56),
                              side: BorderSide(color: SupremeColors.brandPrimary.withOpacity(0.5)),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(16),
                              ),
                            ),
                            icon: const Icon(Icons.login, color: SupremeColors.textPrimary),
                            label: const Text(
                              'Google Sign-In',
                              style: TextStyle(color: SupremeColors.textPrimary, fontSize: 16),
                            ),
                          ),
                          const SizedBox(height: 24),

                          // Guest Mode
                          TextButton(
                            onPressed: () {
                              context.read<AuthProvider>().continueAsGuest();
                            },
                            child: Column(
                              children: [
                                Text(
                                  '${'nav.dashboard'.tr()} (Guest Mode)',
                                  style: TextStyle(color: SupremeColors.textMuted),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '(${'onboarding.rate_limiting_desc'.tr()})',
                                  style: TextStyle(fontSize: 12, color: SupremeColors.textMuted.withOpacity(0.6)),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 16),
                          Text(
                            'Build Version: 1.0.1+fix',
                            style: TextStyle(fontSize: 10, color: SupremeColors.textMuted.withOpacity(0.4)),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGlowingTextField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    bool obscureText = false,
    TextInputType? keyboardType,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: SupremeColors.bgVoid.withOpacity(0.5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: SupremeColors.brandPrimary.withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: SupremeColors.brandPrimary.withOpacity(0.05),
            blurRadius: 10,
            spreadRadius: 1,
          )
        ],
      ),
      child: TextField(
        controller: controller,
        obscureText: obscureText,
        keyboardType: keyboardType,
        style: const TextStyle(color: SupremeColors.textPrimary),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: TextStyle(color: SupremeColors.textMuted),
          prefixIcon: Icon(icon, color: SupremeColors.brandPrimary.withOpacity(0.7)),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        ),
      ),
    );
  }

  Future<void> _handleLogin() async {
    if (emailCtrl.text.trim().isEmpty || passCtrl.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter email and password')),
      );
      return;
    }
    final provider = context.read<AuthProvider>();
    final success = await provider.login(
      emailCtrl.text.trim(),
      passCtrl.text,
    );
    if (mounted && !success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.errorMessage ?? 'error.server'.tr())),
      );
    }
  }

  Future<void> _handleGoogleLogin() async {
    final success = await context.read<AuthProvider>().loginWithGoogle();
    if (mounted && !success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(context.read<AuthProvider>().errorMessage ?? 'error.network'.tr())),
      );
    }
  }
}
