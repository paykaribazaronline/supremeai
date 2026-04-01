import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../config/constants.dart';
import '../../providers/metrics_provider.dart';

class MetricsScreen extends StatefulWidget {
  const MetricsScreen({Key? key}) : super(key: key);

  @override
  State<MetricsScreen> createState() => _MetricsScreenState();
}

class _MetricsScreenState extends State<MetricsScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      Provider.of<MetricsProvider>(context, listen: false).fetchMetrics();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Metrics & Monitoring'),
        elevation: 0,
        actions: [
          IconButton(
            onPressed: () {
              Provider.of<MetricsProvider>(context, listen: false).fetchMetrics();
            },
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Consumer<MetricsProvider>(
        builder: (context, provider, _) {
          if (provider.isLoading) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (provider.error != null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.error_outline,
                    size: 48,
                    color: Colors.red,
                  ),
                  const SizedBox(height: AppConstants.paddingMedium),
                  Text('Error: ${provider.error}'),
                  const SizedBox(height: AppConstants.paddingMedium),
                  ElevatedButton(
                    onPressed: () => provider.fetchMetrics(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          return RefreshIndicator(
            onRefresh: () => provider.fetchMetrics(),
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(AppConstants.paddingMedium),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // System Health
                  _buildSectionTitle('System Health'),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildHealthCards(provider),
                  const SizedBox(height: AppConstants.paddingXLarge),

                  // Memory Usage Chart
                  _buildSectionTitle('Memory Usage (Last 24h)'),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildMemoryChart(provider),
                  const SizedBox(height: AppConstants.paddingXLarge),

                  // Request Rate Chart
                  _buildSectionTitle('Request Rate (Last 24h)'),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildRequestChart(provider),
                  const SizedBox(height: AppConstants.paddingXLarge),

                  // Error Distribution
                  _buildSectionTitle('Error Distribution'),
                  const SizedBox(height: AppConstants.paddingMedium),
                  _buildErrorDistribution(provider),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.bold,
      ),
    );
  }

  Widget _buildHealthCards(MetricsProvider provider) {
    final metrics = provider.metrics;

    return GridView.count(
      crossAxisCount: 2,
      crossAxisSpacing: AppConstants.paddingMedium,
      mainAxisSpacing: AppConstants.paddingMedium,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      children: [
        _buildHealthCard(
          'Memory',
          '${metrics?.memoryUsageMB.toStringAsFixed(1)} MB',
          metrics?.memoryUsageMB ?? 0,
          100,
          Icons.storage,
        ),
        _buildHealthCard(
          'CPU',
          '${metrics?.cpuUsagePercent.toStringAsFixed(1)}%',
          metrics?.cpuUsagePercent ?? 0,
          100,
          Icons.speed,
        ),
        _buildHealthCard(
          'Requests',
          metrics?.totalRequests.toString() ?? '0',
          (metrics?.totalRequests ?? 0).toDouble(),
          1000,
          Icons.cloud_upload,
        ),
        _buildHealthCard(
          'Errors',
          metrics?.errorCount.toString() ?? '0',
          (metrics?.errorCount ?? 0).toDouble(),
          50,
          Icons.error_outline,
        ),
      ],
    );
  }

  Widget _buildHealthCard(
    String label,
    String value,
    double current,
    double max,
    IconData icon,
  ) {
    final percentage = (current / max * 100).clamp(0, 100);
    final color = percentage > 80
        ? Colors.red
        : percentage > 50
            ? Colors.orange
            : Colors.green;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
                Icon(icon, size: 16, color: color),
              ],
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            Text(
              value,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: percentage / 100,
                minHeight: 6,
                backgroundColor: Colors.grey[300],
                valueColor: AlwaysStoppedAnimation<Color>(color),
              ),
            ),
            const SizedBox(height: AppConstants.paddingSmall),
            Text(
              '${percentage.toStringAsFixed(1)}%',
              style: const TextStyle(fontSize: 10, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMemoryChart(MetricsProvider provider) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: SizedBox(
          height: 200,
          child: LineChart(
            LineChartData(
              gridData: FlGridData(show: false),
              titlesData: FlTitlesData(
                bottomTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    getTitlesWidget: (value, meta) {
                      return Text('${value.toInt()}h');
                    },
                  ),
                ),
                leftTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    getTitlesWidget: (value, meta) {
                      return Text('${value.toInt()}MB');
                    },
                  ),
                ),
              ),
              lineBarsData: [
                LineChartBarData(
                  spots: provider.getMemorySpots(),
                  isCurved: true,
                  color: Color(AppConstants.primaryColor),
                  barWidth: 2,
                  dotData: FlDotData(show: false),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRequestChart(MetricsProvider provider) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: SizedBox(
          height: 200,
          child: BarChart(
            BarChartData(
              gridData: FlGridData(show: false),
              titlesData: FlTitlesData(
                bottomTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    getTitlesWidget: (value, meta) {
                      return Text('${value.toInt()}h');
                    },
                  ),
                ),
              ),
              barGroups: provider.getRequestBarGroups(),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorDistribution(MetricsProvider provider) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppConstants.paddingMedium),
        child: SizedBox(
          height: 200,
          child: PieChart(
            PieChartData(
              sections: provider.getErrorSections(),
              centerSpaceRadius: 40,
            ),
          ),
        ),
      ),
    );
  }
}
