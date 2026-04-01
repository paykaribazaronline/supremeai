# SupremeAI Flutter Admin App - Setup Guide

## 📱 Project Overview

A professional Flutter admin application for managing the SupremeAI backend system.

**Base URL:** `https://supremeai-565236080752.us-central1.run.app`

## 🚀 Quick Start

### Prerequisites

```bash
# Flutter (3.0+)
flutter --version

# Ensure both Android & iOS SDKs are installed
flutter doctor
```

### Installation

1. **Navigate to project directory**
```bash
cd flutter_admin_app
```

2. **Get dependencies**
```bash
flutter pub get
```

3. **Run the app**
```bash
# For development (uses Cloud Run backend)
flutter run

# For iOS
flutter run -d <ios-device-id>

# For Android
flutter run -d <android-device-id>
```

## 📁 Project Structure

```
lib/
├── main.dart                          # App entry point & splash screen
│
├── config/
│   ├── constants.dart                # App-wide constants (colors, sizes, messages)
│   └── environment.dart              # Environment configuration & Base URL
│
├── models/                           # Data models for API responses
│   ├── user_model.dart
│   ├── project_model.dart
│   └── api_provider_model.dart
│
├── services/                         # Business logic & API communication
│   ├── api_service.dart             # HTTP client with interceptors
│   ├── auth_service.dart            # Authentication logic
│   └── storage_service.dart         # Local data storage (SharedPreferences)
│
├── providers/                        # State management (Provider package)
│   ├── auth_provider.dart           # Authentication state
│   ├── project_provider.dart        # Projects state
│   └── ui_provider.dart             # UI state
│
├── screens/                          # UI screens
│   ├── auth/
│   │   ├── login_screen.dart
│   │   └── register_screen.dart
│   ├── dashboard/
│   │   ├── home_screen.dart
│   │   └── widgets/
│   └── projects/
│       ├── projects_screen.dart
│       └── project_detail_screen.dart
│
├── widgets/                          # Reusable UI components
│   ├── custom_app_bar.dart
│   ├── loading_dialog.dart
│   └── error_widget.dart
│
└── utils/                            # Utility functions
    ├── validators.dart              # Form validators
    └── formatters.dart              # Data formatters
```

## 🔐 Authentication Flow

### Login Process

1. User enters email & password on `LoginScreen`
2. `AuthProvider.login()` calls `ApiService.post()` to `/api/auth/login`
3. Backend returns JWT token & refresh token
4. Tokens stored in `StorageService` (SharedPreferences)
5. User data extracted from JWT & stored
6. User navigated to `HomeScreen`

### JWT Token Management

- **Access Token:** 24 hours expiration
- **Refresh Token:** 7 days expiration
- **Auto-refresh:** Implemented in `ApiService` interceptors
- **Token Storage:** Encrypted local storage

### Login Credentials (Development)

```
Email: admin@supremeai.com
Password: (Set by backend)
```

## 🔗 Base URL Configuration

### Update Backend URL

Edit `lib/config/environment.dart`:

```dart
// Production (Cloud Run)
static const String baseUrl = 'https://supremeai-565236080752.us-central1.run.app';

// Local Development
// static const String baseUrl = 'http://localhost:8080';
```

## 🛠️ Key Features

### ✅ Authentication

- [x] Login with JWT tokens
- [x] User registration (admin only)
- [x] Token refresh mechanism
- [x] Secure password handling
- [x] Session management

### ✅ Dashboard

- [x] Welcome screen with user info
- [x] Real-time stats cards
- [x] Quick action buttons
- [x] Profile menu
- [x] Logout functionality

### 📋 API Service Features

- [x] Automatic token injection
- [x] Request/response logging
- [x] Error handling with custom messages
- [x] Timeout configuration
- [x] Interceptor-based auth flow
- [x] Singleton pattern for efficiency

### 💾 Storage Service

- [x] Token management (get/save/clear)
- [x] User data persistence
- [x] Generic key-value storage
- [x] Login status checking

## 📡 API Endpoints Used

### Authentication
```
POST /api/auth/login          - Login with credentials
POST /api/auth/register       - Register new admin
POST /api/auth/logout         - Logout current user
POST /api/auth/refresh        - Refresh JWT token
```

### Projects
```
GET  /api/projects            - List all projects
POST /api/projects/create     - Create new project
PUT  /api/projects/update     - Update project
DEL  /api/projects/delete     - Delete project
```

### AI Providers
```
GET  /api/providers/available  - List available providers
GET  /api/providers/configured - List configured providers
POST /api/providers/add        - Add new provider
POST /api/providers/remove     - Remove provider
POST /api/providers/test       - Test provider connection
```

### Agents & Monitoring
```
POST /api/agents/assign       - Assign AI agent to project
GET  /api/agents              - List all agents
GET  /api/metrics/health      - System health metrics
GET  /api/metrics/performance - Performance metrics
GET  /api/alerts              - Alert list
```

## 🎨 Theme & Styling

### Color Scheme (Material Design 3)
- **Primary:** Blue (#3B82F6)
- **Secondary:** Green (#10B981)
- **Background:** Light Gray (#F8FAFC)
- **Error:** Red (#EF4444)
- **Success:** Green (#10B981)

### Spacing System
- XSmall: 4dp
- Small: 12dp
- Medium: 16dp
- Large: 24dp
- XLarge: 32dp

## 📦 Dependencies

### Core
- `flutter:sdk` - Flutter framework
- `provider: ^6.0.0` - State management
- `get: ^4.6.5` - Navigation & service locator

### Networking
- `dio: ^5.3.1` - HTTP client
- `http: ^1.1.0` - Basic HTTP
- `jwt_decoder: ^2.0.1` - JWT parsing

### Storage
- `shared_preferences: ^2.2.2` - Local storage
- `hive: ^2.2.3` - NoSQL database

### UI & Design
- `google_fonts: ^6.1.0` - Custom fonts
- `flutter_svg: ^2.0.7` - SVG support
- `cached_network_image: ^3.3.0` - Image caching
- `fl_chart: ^0.63.0` - Charts & graphs

### Validation
- `email_validator: ^2.1.17` - Email validation
- `intl: ^0.19.0` - Internationalization

### Development
- `logger: ^2.0.1` - Logging
- `flutter_lints: ^2.0.0` - Linting

## 🧪 Testing

```bash
# Run unit tests
flutter test

# Run with coverage
flutter test --coverage

# Run specific test file
flutter test test/services/auth_service_test.dart
```

## 🚢 Building for Production

### Android Build
```bash
# Clean build
flutter clean
flutter pub get

# Build APK
flutter build apk --release

# Build App Bundle
flutter build appbundle --release
```

### iOS Build
```bash
# Clean build
flutter clean
flutter pub get

# Build iOS
flutter build ios --release

# Build & create IPA
flutter build ipa --release
```

## 🐛 Troubleshooting

### "Cannot connect to backend"
- Verify internet connection
- Check Base URL in `environment.dart`
- Ensure backend is running (Cloud Run or localhost)
- Check firewall settings

### "Invalid token" error
- Clear app cache: `flutter clean`
- Delete stored tokens: `await storageService.clearAll()`
- Re-login with valid credentials

### "Build fails"
```bash
# Full clean rebuild
flutter clean
flutter pub get
flutter pub upgrade
flutter run
```

### "iOS Pod issues"
```bash
cd ios
rm -rf Pods Pod.lock
pod install --repo-update
cd ..
flutter run
```

## 📚 Additional Resources

- [Flutter Documentation](https://flutter.dev/docs)
- [Provider Package](https://pub.dev/packages/provider)
- [Dio HTTP Client](https://pub.dev/packages/dio)
- [Material Design 3](https://m3.material.io)

## 🔒 Security Best Practices

1. ✅ Never hardcode API keys
2. ✅ Use HTTPS for all connections
3. ✅ Store tokens securely in device keychain/keystore
4. ✅ Implement certificate pinning for production
5. ✅ Regular security audits
6. ✅ Keep dependencies updated

## 📞 Support

For issues or questions:
- Check logs: Run with `flutter run -v` for verbose output
- Check Backend: Test API endpoints with Postman
- Review Documentation: Check AUTHENTICATION_GUIDE.md

## 📝 Changelog

### Version 1.0.0 (Initial Release)
- ✅ Authentication system
- ✅ Admin dashboard
- ✅ API service layer
- ✅ State management with Provider
- ✅ Local storage integration
- ✅ Form validation
- ✅ Error handling
- ✅ Material Design 3 UI

---

**Last Updated:** March 31, 2026  
**Status:** Ready for Development
