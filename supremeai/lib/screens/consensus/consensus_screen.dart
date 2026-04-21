import 'package:flutter/material.dart';

class ConsensusScreen extends StatelessWidget {
  const ConsensusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Multi-AI Consensus')),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: const [
          Text('Ongoing Voting Sessions', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          SizedBox(height: 10),
          Card(
            child: ListTile(
              leading: Icon(Icons.psychology, color: Colors.blue),
              title: Text('Session #142: Database Choice'),
              subtitle: Text('Consensus: MongoDB (80% Agreement)'),
              trailing: Chip(label: Text('FINALIZED'), backgroundColor: Colors.greenAccent),
            ),
          ),
          SizedBox(height: 10),
          Card(
            child: ListTile(
              leading: Icon(Icons.psychology, color: Colors.orange),
              title: Text('Session #143: Auth Mechanism'),
              subtitle: Text('Voting in Progress... (3/5 Agents voted)'),
              trailing: CircularProgressIndicator(),
            ),
          ),
        ],
      ),
    );
  }
}
