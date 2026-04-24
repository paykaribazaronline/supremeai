import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Semantics(
        label: 'Login screen for SupremeAI',
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Semantics(
                header: true,
                child: const Text(
                  'SupremeAI',
                  style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
                ),
              ),
              const SizedBox(height: 48),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: Semantics(
                  button: true,
                  label: 'Sign in to your account',
                  child: ElevatedButton(
                    onPressed: () async {
                      // Simple login implementation
                      try {
                        // Show a simple dialog for email/password (or use guest mode)
                        final provider = context.read<AuthProvider>();
                        await provider
                            .signInAnonymously(); // Temporary: use anonymous auth
                        if (context.mounted) {
                          Navigator.of(context).pushReplacementNamed('/home');
                        }
                      } catch (e) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Login failed: $e')),
                          );
                        }
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(double.infinity, 50),
                    ),
                    child: const Text('লগইন করুন'),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Semantics(
                label: 'Build Version: 1.0.1+fix',
                child: const Text(
                  'Build Version: 1.0.1+fix',
                  style: TextStyle(fontSize: 10, color: Colors.grey),
                  semanticsLabel: 'Build Version 1.0.1 fix',
                ),
              ),
              const SizedBox(height: 16),
              Semantics(
                button: true,
                label: 'Continue as guest with limited quota',
                child: TextButton(
                  onPressed: () {
                    context.read<AuthProvider>().continueAsGuest();
                  },
                  child: const Text(
                    'গেস্ট হিসেবে ব্যবহার করুন (Guest Mode)',
                    style: TextStyle(color: Colors.blue),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Semantics(
                label: 'Guest mode has limited quota',
                child: const Text(
                  '(গেস্ট মোডে সীমিত কোটা প্রযোজ্য)',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                  semanticsLabel: 'Guest mode has limited quota',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
