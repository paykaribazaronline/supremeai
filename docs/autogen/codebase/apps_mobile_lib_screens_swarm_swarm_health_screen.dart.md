# 📄 ফাইল: apps/mobile/lib/screens/swarm/swarm_health_screen.dart

**প্রকার:** .dart  
**সাইজ:** 8,106 বাইট  
**আপডেট:** 2026-07-11T19:51:42.324960

---

## কোড

```dart
import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../../theme/tokens.dart'; // Adjust path
import '../../widgets/supreme_ui/supreme_card.dart'; // From Phase 4
import 'hold_to_kill_button.dart';

class SwarmHealthScreen extends StatefulWidget {
  const SwarmHealthScreen({Key? key}) : super(key: key);

  @override
  State<SwarmHealthScreen> createState() => _SwarmHealthScreenState();
}

class _SwarmHealthScreenState extends State<SwarmHealthScreen> {
  Timer? _mockStreamTimer;
  
  // Mock State
  String _circuitState = 'CLOSED';
  double _cpuUsage = 12.0;
  double _memoryUsage = 256.0;
  int _activeAgents = 3;
  double _errorRate = 0.0;
  List<Map<String, dynamic>> _logs = [];

  final Random _rnd = Random();

  @override
  void initState() {
    super.initState();
    _startMockStream();
  }

  void _startMockStream() {
    _mockStreamTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_circuitState == 'OPEN') return;

      setState(() {
        _cpuUsage = min(100, max(5, _cpuUsage + (_rnd.nextDouble() * 10 - 5)));
        _memoryUsage = max(100, _memoryUsage + (_rnd.nextDouble() * 50 - 20));
        _activeAgents = _rnd.nextInt(3) + 3;
        _errorRate = max(0, _errorRate + (_rnd.nextDouble() * 2 - 1));

        if (_rnd.nextDouble() > 0.6) {
          final agents = ['Architect', 'Coder', 'QA', 'Deployer'];
          final messages = ['Analyzing AST...', 'Resolving dependencies...', 'Running test suite...', 'Optimizing loop...'];
          _logs.insert(0, {
            'agent': agents[_rnd.nextInt(agents.length)],
            'message': messages[_rnd.nextInt(messages.length)],
            'level': _rnd.nextDouble() > 0.9 ? 'warn' : 'info',
            'time': DateTime.now(),
          });
          if (_logs.length > 50) _logs.removeLast();
        }
      });
    });
  }

  @override
  void dispose() {
    _mockStreamTimer?.cancel();
    super.dispose();
  }

  void _triggerCircuitBreaker() {
    setState(() {
      _circuitState = 'OPEN';
      _logs.insert(0, {
        'agent': 'SYSTEM',
        'message': 'CIRCUIT BREAKER TRIGGERED. Swarm execution halted.',
        'level': 'error',
        'time': DateTime.now(),
      });
    });

    // Auto-recover after 4 seconds for testing purposes
    Future.delayed(const Duration(seconds: 4), () {
      if (mounted) {
        setState(() => _circuitState = 'CLOSED');
      }
    });
  }

  Widget _buildMetricCard(String title, String value, String unit) {
    return SupremeCard(
      child: Padding(
        padding: EdgeInsets.all(DesignTokens.space4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              title,
              style: TextStyle(
                color: DesignTokens.textSecondaryDark,
                fontSize: DesignTokens.fontSizeSm,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  value,
                  style: TextStyle(
                    color: DesignTokens.textPrimaryDark,
                    fontSize: DesignTokens.fontSize2xl,
                    fontWeight: FontWeight.bold,
                    fontFamily: DesignTokens.fontFamilyMono,
                  ),
                ),
                const SizedBox(width: 4),
                Text(
                  unit,
                  style: TextStyle(
                    color: DesignTokens.brandPrimaryDark,
                    fontSize: DesignTokens.fontSizeXs,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: DesignTokens.bgVoidDark,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          'Swarm Health',
          style: TextStyle(
            fontFamily: DesignTokens.fontFamilyDisplay,
            color: DesignTokens.textPrimaryDark,
          ),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(DesignTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. Metrics Grid
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                mainAxisSpacing: DesignTokens.space3,
                crossAxisSpacing: DesignTokens.space3,
                childAspectRatio: 1.5,
                physics: const NeverScrollableScrollPhysics(),
                children: [
                  _buildMetricCard('CPU Load', _cpuUsage.toStringAsFixed(1), '%'),
                  _buildMetricCard('Memory', _memoryUsage.toStringAsFixed(0), 'MB'),
                  _buildMetricCard('Active Agents', '0$_activeAgents', 'NODES'),
                  _buildMetricCard('Error Rate', _errorRate.toStringAsFixed(2), '%'),
                ],
              ),
              SizedBox(height: DesignTokens.space6),

              // 2. Live Log Feed
              Text(
                'LIVE EXECUTION FEED',
                style: TextStyle(
                  color: DesignTokens.textSecondaryDark,
                  fontSize: DesignTokens.fontSizeSm,
                  letterSpacing: 1.2,
                ),
              ),
              SizedBox(height: DesignTokens.space2),
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    color: DesignTokens.bgElevatedDark,
                    borderRadius: BorderRadius.circular(DesignTokens.radiusMd),
                    border: Border.all(color: DesignTokens.borderDefaultDark),
                  ),
                  child: ListView.builder(
                    padding: EdgeInsets.all(DesignTokens.space3),
                    itemCount: _logs.length,
                    itemBuilder: (context, index) {
                      final log = _logs[index];
                      final isError = log['level'] == 'error';
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8.0),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '[${log['agent']}]',
                              style: TextStyle(
                                color: isError ? DesignTokens.brandDangerDark : DesignTokens.brandSecondaryDark,
                                fontFamily: DesignTokens.fontFamilyMono,
                                fontSize: DesignTokens.fontSizeXs,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                log['message'],
                                style: TextStyle(
                                  color: isError ? DesignTokens.brandDangerDark : DesignTokens.textPrimaryDark,
                                  fontFamily: DesignTokens.fontFamilyMono,
                                  fontSize: DesignTokens.fontSizeXs,
                                ),
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
              ),
              SizedBox(height: DesignTokens.space6),

              // 3. Circuit Breaker Global Kill Switch
              HoldToKillButton(
                onTrigger: _triggerCircuitBreaker,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

```