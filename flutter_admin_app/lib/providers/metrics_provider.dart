import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../models/models.dart';
import '../services/api_service.dart';

class MetricsProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  HealthMetrics? _metrics;
  bool _isLoading = false;
  String? _error;
  
  List<FlSpot> _memoryHistory = [];
  List<BarChartGroupData> _requestHistory = [];

  HealthMetrics? get metrics => _metrics;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> fetchMetrics() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.get<Map<String, dynamic>>('/api/metrics/health');
      
      if (response.success && response.data != null) {
        _metrics = HealthMetrics.fromJson(response.data as Map<String, dynamic>);
        _generateHistoryData();
        _error = null;
      } else {
        _error = response.error ?? 'Failed to fetch metrics';
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void _generateHistoryData() {
    // Generate mock historical data for charts
    _memoryHistory = List.generate(24, (index) {
      return FlSpot(
        index.toDouble(),
        50 + (index % 5) * 10 + (index % 3) * 5.0,
      );
    });

    _requestHistory = List.generate(24, (index) {
      return BarChartGroupData(
        x: index,
        barRods: [
          BarChartRodData(
            toY: 100 + (index % 7) * 30.0,
            color: Colors.blue,
          ),
        ],
      );
    });
  }

  List<FlSpot> getMemorySpots() {
    return _memoryHistory.isNotEmpty
        ? _memoryHistory
        : [const FlSpot(0, 0), const FlSpot(1, 0)];
  }

  List<BarChartGroupData> getRequestBarGroups() {
    return _requestHistory.isNotEmpty
        ? _requestHistory
        : [
            BarChartGroupData(
              x: 0,
              barRods: [BarChartRodData(toY: 0)],
            ),
          ];
  }

  List<PieChartSectionData> getErrorSections() {
    const colors = [
      Colors.red,
      Colors.orange,
      Colors.yellow,
      Colors.blue,
    ];
    const errors = ['500 Error', '404 Error', '403 Error', 'Timeout'];
    final values = [30.0, 25.0, 20.0, 25.0];

    return List.generate(errors.length, (index) {
      return PieChartSectionData(
        value: values[index],
        title: errors[index],
        color: colors[index],
        radius: 50,
      );
    });
  }
}
