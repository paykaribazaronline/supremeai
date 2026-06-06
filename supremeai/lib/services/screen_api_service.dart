import 'package:http/http.dart' as http;
import 'dart:convert';

class ScreenApiService {
  static const String _baseUrl = 'https://supremeai-a.web.app/api';

  Future<Map<String, dynamic>> ping(String screen) async {
    try {
      final response = await http
          .get(Uri.parse('$_baseUrl/screens/$screen'))
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        return json.decode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to load screen data: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error fetching $screen data: $e');
    }
  }
}
