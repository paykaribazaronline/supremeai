// apps/mobile/lib/widgets/agent_metrics_card.dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/orchestration_provider.dart';

class AgentMetricsCard extends StatelessWidget {
  const AgentMetricsCard({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // আমাদের সিঙ্ক করা OrchestrationProvider ওয়াচ করা হচ্ছে
    return Consumer<OrchestrationProvider>(
      builder: (context, provider, child) {
        final metrics = provider.activeAgentMetrics;
        final executionTier = metrics['execution_tier'] ?? 'Idle';
        final isZeroCost = executionTier.contains('Layer 2');

        return Card(
          elevation: 4,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          color: isZeroCost ? Colors.teal.shade900 : Colors.indigo.shade900,
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      "Active Agent State",
                      style: TextStyle(color: Colors.white70, fontSize: 14),
                    ),
                    Container(
                      padding: EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: isZeroCost ? Colors.greenAccent.shade400 : Colors.orangeAccent,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        isZeroCost ? "ZERO COST" : "API ACTIVE",
                        style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 12),
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 12),
                Text(
                  executionTier,
                  style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
                ),
                SizedBox(height: 16),
                Divider(color: Colors.white24),
                SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildStatNode("Extracted Nodes", "${(metrics['data'] is Map) ? (metrics['data'] as Map).length : 0}"),
                    _buildStatNode("Latency Guard", "${metrics['latency_ms'] ?? '0'} ms"),
                  ],
                )
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildStatNode(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: TextStyle(color: Colors.white60, fontSize: 12)),
        SizedBox(height: 4),
        Text(value, style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
      ],
    );
  }
}
