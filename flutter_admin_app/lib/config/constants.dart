// App-wide constants

class AppConstants {
  // App Info
  static const String appName = 'SupremeAI Admin';
  static const String appVersion = '1.0.0';
  
  // Colors (Material Design 3)
  static const int primaryColor = 0xFF3B82F6;           // Blue
  static const int secondaryColor = 0xFF10B981;         // Green
  static const int backgroundColor = 0xFFF8FAFC;        // Light Gray
  static const int surfaceColor = 0xFFFFFFFF;           // White
  static const int errorColor = 0xFFEF4444;             // Red
  static const int warningColor = 0xFFFBBF24;           // Amber
  static const int successColor = 0xFF10B981;           // Green
  static const int infoColor = 0xFF3B82F6;              // Blue
  
  // Text Styles
  static const double headingFontSize = 28.0;
  static const double titleFontSize = 20.0;
  static const double subtitleFontSize = 16.0;
  static const double bodyFontSize = 14.0;
  static const double captionFontSize = 12.0;
  
  // Padding & Margins
  static const double paddingXXSmall = 4.0;
  static const double paddingXSmall = 8.0;
  static const double paddingSmall = 12.0;
  static const double paddingMedium = 16.0;
  static const double paddingLarge = 24.0;
  static const double paddingXLarge = 32.0;
  
  // Border Radius
  static const double radiusSmall = 4.0;
  static const double radiusMedium = 8.0;
  static const double radiusLarge = 12.0;
  static const double radiusXLarge = 16.0;
  static const double radiusFull = 999.0;
  
  // Durations
  static const Duration shortDuration = Duration(milliseconds: 200);
  static const Duration normalDuration = Duration(milliseconds: 300);
  static const Duration longDuration = Duration(milliseconds: 500);
  
  // Error Messages
  static const String errorNetwork = 'Network error. Please check your connection.';
  static const String errorServer = 'Server error. Please try again later.';
  static const String errorUnknown = 'An unknown error occurred.';
  static const String errorUnauthorized = 'Unauthorized. Please login again.';
  static const String errorInvalidCredentials = 'Invalid email or password.';
  static const String errorEmptyFields = 'Please fill in all fields.';
  
  // Success Messages
  static const String successLogin = 'Login successful!';
  static const String successLogout = 'Logged out successfully.';
  static const String successCreate = 'Created successfully.';
  static const String successUpdate = 'Updated successfully.';
  static const String successDelete = 'Deleted successfully.';
  
  // Validation
  static const int minPasswordLength = 8;
  static const int maxPasswordLength = 128;
  static const int minProjectNameLength = 3;
  static const int maxProjectNameLength = 100;
}
