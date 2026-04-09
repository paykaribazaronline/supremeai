import 'package:dio/dio.dart';
import 'package:logger/logger.dart';
import '../config/environment.dart';

class ApiResponse<T> {
  final bool success;
  final T? data;
  final String? error;
  final int? statusCode;

  ApiResponse({
    required this.success,
    this.data,
    this.error,
    this.statusCode,
  });
}

class ApiService {
  static final ApiService _instance = ApiService._internal();
  late Dio _dio;
  final Logger _logger = Logger();

  factory ApiService() {
    return _instance;
  }

  ApiService._internal() {
    _initializeDio();
  }

  void _initializeDio() {
    _dio = Dio(
      BaseOptions(
        baseUrl: Environment.baseUrl,
        connectTimeout: const Duration(seconds: Environment.connectionTimeout),
        receiveTimeout: const Duration(seconds: Environment.receiveTimeout),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    // Add interceptors
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          _logger.i('🔵 REQUEST: ${options.method} ${options.path}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          _logger.i('🟢 RESPONSE: ${response.statusCode} ${response.requestOptions.path}');
          return handler.next(response);
        },
        onError: (error, handler) {
          _logger.e('🔴 ERROR: ${error.message}');
          if (error.response?.statusCode == 401) {
            // Handle token refresh or redirect to login
            _handleUnauthorized();
          }
          return handler.next(error);
        },
      ),
    );
  }

  Future<ApiResponse<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ApiResponse<T>(
          success: true,
          data: response.data as T,
          statusCode: response.statusCode,
        );
      }

      return ApiResponse<T>(
        success: false,
        error: _extractErrorMessage(response.data),
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      return _handleError<T>(e);
    }
  }

  Future<ApiResponse<T>> post<T>(
    String path, {
    Map<String, dynamic>? data,
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ApiResponse<T>(
          success: true,
          data: response.data as T,
          statusCode: response.statusCode,
        );
      }

      return ApiResponse<T>(
        success: false,
        error: _extractErrorMessage(response.data),
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      return _handleError<T>(e);
    }
  }

  Future<ApiResponse<T>> put<T>(
    String path, {
    Map<String, dynamic>? data,
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      final response = await _dio.put<Map<String, dynamic>>(
        path,
        data: data,
        queryParameters: queryParameters,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return ApiResponse<T>(
          success: true,
          data: response.data as T,
          statusCode: response.statusCode,
        );
      }

      return ApiResponse<T>(
        success: false,
        error: _extractErrorMessage(response.data),
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      return _handleError<T>(e);
    }
  }

  Future<ApiResponse<T>> delete<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      final response = await _dio.delete<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
      );

      if (response.statusCode == 200 || response.statusCode == 204) {
        return ApiResponse<T>(
          success: true,
          data: response.data as T,
          statusCode: response.statusCode,
        );
      }

      return ApiResponse<T>(
        success: false,
        error: _extractErrorMessage(response.data),
        statusCode: response.statusCode,
      );
    } on DioException catch (e) {
      return _handleError<T>(e);
    }
  }

  ApiResponse<T> _handleError<T>(DioException e) {
    String errorMessage;

    if (e.type == DioExceptionType.connectionTimeout) {
      errorMessage = 'Connection timeout. Please check your network.';
    } else if (e.type == DioExceptionType.receiveTimeout) {
      errorMessage = 'Request timeout. Server is not responding.';
    } else if (e.type == DioExceptionType.badResponse) {
      final statusCode = e.response?.statusCode;
      final backendMessage = _extractErrorMessage(e.response?.data);

      if (statusCode == 401 || statusCode == 403) {
        errorMessage = backendMessage ?? 'Session expired or unauthorized. Please log in again.';
      } else {
        errorMessage = backendMessage ?? 'Server error occurred.';
      }
    } else if (e.type == DioExceptionType.unknown) {
      errorMessage = 'Network error. Please check your connection.';
    } else {
      errorMessage = e.message ?? 'Unknown error occurred.';
    }

    _logger.e('API Error: $errorMessage (${e.type})');

    return ApiResponse<T>(
      success: false,
      error: errorMessage,
      statusCode: e.response?.statusCode,
    );
  }

  String? _extractErrorMessage(dynamic responseData) {
    if (responseData == null) {
      return null;
    }
    
    // If it's a Map, try to extract 'message' field
    if (responseData is Map<String, dynamic>) {
      final message = responseData['message'] as String?;
      if (message != null && message.trim().isNotEmpty) {
        return message;
      }

      final error = responseData['error'] as String?;
      if (error != null && error.trim().isNotEmpty) {
        return error;
      }

      final detail = responseData['detail'] as String?;
      if (detail != null && detail.trim().isNotEmpty) {
        return detail;
      }
    }
    
    // If it's a String, return it directly
    if (responseData is String) {
      return responseData;
    }
    
    // Otherwise try to convert to string
    return responseData.toString();
  }

  void _handleUnauthorized() {
    _logger.w('Unauthorized access from API request');
  }

  // Update base URL if needed
  void updateBaseUrl(String newUrl) {
    _dio.options.baseUrl = newUrl;
  }

  // Get Dio instance for advanced usage
  Dio get dioInstance => _dio;
}
