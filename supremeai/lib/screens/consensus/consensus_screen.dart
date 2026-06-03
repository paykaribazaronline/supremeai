import 'package:flutter/material.dart';
import 'dart:ui';
import '../../services/localization_service.dart';

class ConsensusScreen extends StatelessWidget {
  const ConsensusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: Text('consensus.title'.tr(),
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, letterSpacing: 1.5, color: Colors.white)
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          Text('consensus.ongoing_sessions'.tr().toUpperCase(), 
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w900, letterSpacing: 2, color: Colors.white54)
          ),
          const SizedBox(height: 16),
          _buildSessionCard(
            context,
            'Session #142: Database Choice',
            'Consensus: MongoDB (80% Agreement)',
            'consensus.finalized'.tr().toUpperCase(),
            Colors.greenAccent,
            Icons.psychology,
            isFinalized: true,
          ),
          _buildSessionCard(
            context,
            'Session #143: Auth Mechanism',
            'consensus.voting_in_progress'.tr(),
            'PENDING',
            Colors.orangeAccent,
            Icons.how_to_vote,
            isFinalized: false,
          ),
        ],
      ),
    );
  }

  Widget _buildSessionCard(BuildContext context, String title, String subtitle, String status, Color color, IconData icon, {required bool isFinalized}) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.03),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white10),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: color.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(icon, color: color, size: 28),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 6),
                      Text(subtitle, style: TextStyle(color: isFinalized ? Colors.white70 : color, fontSize: 13, fontWeight: isFinalized ? FontWeight.normal : FontWeight.w600)),
                    ],
                  ),
                ),
                if (!isFinalized)
                  const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.orangeAccent),
                  )
                else
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: color.withValues(alpha: 0.3)),
                    ),
                    child: Text(status, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold)),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
