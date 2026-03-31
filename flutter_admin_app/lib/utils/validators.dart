import 'package:email_validator/email_validator.dart';

class Validators {
  // Email validation
  static String? validateEmail(String? value) {
    if (value == null || value.isEmpty) {
      return 'Email is required';
    }

    if (!EmailValidator.validate(value)) {
      return 'Please enter a valid email';
    }

    return null;
  }

  // Password validation
  static String? validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Password is required';
    }

    if (value.length < 8) {
      return 'Password must be at least 8 characters';
    }

    return null;
  }

  // Confirm password validation
  static String? validateConfirmPassword(String? value, String password) {
    if (value == null || value.isEmpty) {
      return 'Please confirm your password';
    }

    if (value != password) {
      return 'Passwords do not match';
    }

    return null;
  }

  // Name validation
  static String? validateName(String? value) {
    if (value == null || value.isEmpty) {
      return 'Name is required';
    }

    if (value.length < 2) {
      return 'Name must be at least 2 characters';
    }

    return null;
  }

  // Project name validation
  static String? validateProjectName(String? value) {
    if (value == null || value.isEmpty) {
      return 'Project name is required';
    }

    if (value.length < 3) {
      return 'Project name must be at least 3 characters';
    }

    if (value.length > 100) {
      return 'Project name must not exceed 100 characters';
    }

    return null;
  }

  // Description validation
  static String? validateDescription(String? value) {
    if (value == null || value.isEmpty) {
      return 'Description is required';
    }

    if (value.length < 10) {
      return 'Description must be at least 10 characters';
    }

    return null;
  }

  // API Key validation
  static String? validateApiKey(String? value) {
    if (value == null || value.isEmpty) {
      return 'API Key is required';
    }

    if (value.length < 10) {
      return 'API Key must be at least 10 characters';
    }

    return null;
  }

  // Generic field validation
  static String? validateRequired(String? value, String fieldName) {
    if (value == null || value.isEmpty) {
      return '$fieldName is required';
    }

    return null;
  }
}
