import 'package:flutter/material.dart';
import '../../config/environment.dart';
import '../../services/api_service.dart';

class VpnScreen extends StatefulWidget {
  const VpnScreen({Key? key}) : super(key: key);

  @override
  State<VpnScreen> createState() => _VpnScreenState();
}

class _VpnScreenState extends State<VpnScreen> {
  final ApiService _apiService = ApiService();
  List<dynamic> _vpnList = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadVpns();
  }

  Future<void> _loadVpns() async {
    setState(() { _isLoading = true; _error = null; });
    final response = await _apiService.get<List<dynamic>>(Environment.vpnList);
    if (!mounted) return;
    setState(() {
      _isLoading = false;
      if (response.success) {
        _vpnList = (response.data as List<dynamic>?) ?? [];
      } else {
        _error = response.error ?? 'VPN তথ্য লোড করা যায়নি';
      }
    });
  }

  Future<void> _connectVpn(String id) async {
    final response = await _apiService.post<Map<String, dynamic>>('/api/vpn/$id/connect');
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success ? 'VPN সংযুক্ত হয়েছে!' : 'ত্রুটি: ${response.error}'),
      backgroundColor: response.success ? Colors.green : Colors.red,
    ));
    if (response.success) _loadVpns();
  }

  Future<void> _disconnectVpn(String id) async {
    final response = await _apiService.post<Map<String, dynamic>>('/api/vpn/$id/disconnect');
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(response.success ? 'VPN বিচ্ছিন্ন হয়েছে' : 'ত্রুটি: ${response.error}'),
      backgroundColor: response.success ? Colors.green : Colors.red,
    ));
    if (response.success) _loadVpns();
  }

  Future<void> _addVpn() async {
    final nameController = TextEditingController();
    final hostController = TextEditingController();
    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('নতুন VPN যোগ করুন'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          const Text('(নতুন VPN সংযোগ তৈরি করুন)', style: TextStyle(fontSize: 12, color: Colors.grey)),
          const SizedBox(height: 12),
          TextField(
            controller: nameController,
            decoration: const InputDecoration(labelText: 'VPN নাম', helperText: '(সহজ নাম দিন, যেমন: Office VPN)', border: OutlineInputBorder()),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: hostController,
            decoration: const InputDecoration(labelText: 'হোস্ট ঠিকানা', helperText: '(সার্ভারের IP বা ডোমেইন)', border: OutlineInputBorder()),
          ),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('বাতিল')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('যোগ করুন')),
        ],
      ),
    );
    if (result == true && nameController.text.isNotEmpty) {
      final response = await _apiService.post<Map<String, dynamic>>(
        Environment.vpnAdd,
        data: {'name': nameController.text, 'host': hostController.text},
      );
      if (response.success) _loadVpns();
    }
    nameController.dispose();
    hostController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('VPN ব্যবস্থাপনা'),
        actions: [IconButton(icon: const Icon(Icons.refresh), tooltip: 'রিফ্রেশ', onPressed: _loadVpns)],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _addVpn,
        icon: const Icon(Icons.add),
        label: const Text('নতুন VPN'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  const Icon(Icons.vpn_lock, size: 48, color: Colors.grey),
                  Text(_error!),
                  ElevatedButton(onPressed: _loadVpns, child: const Text('আবার চেষ্টা করুন')),
                ]))
              : RefreshIndicator(
                  onRefresh: _loadVpns,
                  child: _vpnList.isEmpty
                      ? const Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                          Icon(Icons.vpn_lock, size: 64, color: Colors.grey),
                          SizedBox(height: 16),
                          Text('কোনো VPN কনফিগার করা নেই'),
                          Text('(+ বাটন দিয়ে নতুন VPN যোগ করুন)', style: TextStyle(fontSize: 12, color: Colors.grey)),
                        ]))
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _vpnList.length,
                          itemBuilder: (ctx, i) => _buildVpnCard(_vpnList[i]),
                        ),
                ),
    );
  }

  Widget _buildVpnCard(dynamic vpn) {
    final map = vpn is Map<String, dynamic> ? vpn : <String, dynamic>{};
    final id = '${map['id'] ?? ''}';
    final name = '${map['name'] ?? 'Unknown VPN'}';
    final host = '${map['host'] ?? ''}';
    final connected = map['connected'] == true || '${map['status']}'.toUpperCase() == 'CONNECTED';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: (connected ? Colors.green : Colors.grey).withOpacity(0.1),
          child: Icon(connected ? Icons.vpn_lock : Icons.vpn_key_off,
              color: connected ? Colors.green : Colors.grey),
        ),
        title: Text(name),
        subtitle: Text('$host\n${connected ? "সংযুক্ত আছে" : "বিচ্ছিন্ন"}', style: const TextStyle(fontSize: 12)),
        isThreeLine: true,
        trailing: ElevatedButton(
          onPressed: () => connected ? _disconnectVpn(id) : _connectVpn(id),
          style: ElevatedButton.styleFrom(
            backgroundColor: connected ? Colors.red : Colors.green,
            foregroundColor: Colors.white,
          ),
          child: Text(connected ? 'বিচ্ছিন্ন' : 'সংযুক্ত'),
        ),
      ),
    );
  }
}
