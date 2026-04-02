# SupremeAI Flutter Admin App - Configuration Guide

## 🔑 Base URL Configuration

**Current Production URL:**

```
https://supremeai-565236080752.us-central1.run.app
```

This URL is configured in [`lib/config/environment.dart`](lib/config/environment.dart)

## 🛠️ Environment Setup

### Development Environment

1. **Update Base URL for Local Development**

Edit `lib/config/environment.dart`:

```dart
// For local backend on port 8080
static const String baseUrl = 'http://localhost:8080';

// For production (Cloud Run)
// static const String baseUrl = 'https://supremeai-565236080752.us-central1.run.app';
```

2. **Ensure Backend is Running Locally**

```bash
cd c:\Users\Nazifa\supremeai
.\gradlew run
# Backend will be available at http://localhost:8080
```

### Production Environment

The app uses Cloud Run URL by default:

```dart
static const String baseUrl = 'https://supremeai-565236080752.us-central1.run.app';
```

No configuration needed for production builds.

## 📡 API Integration Points

### All API calls go through `ApiService`

```dart
// File: lib/services/api_service.dart

final apiService = ApiService();

// GET request
final response = await apiService.get<Map>('/api/projects');

// POST request
final response = await apiService.post<Map>(
  '/api/auth/login',
  data: {'email': 'admin@supremeai.com', 'password': 'password'},
);
```

### Authentication Flow

1. Login via `/api/auth/login` → Get JWT tokens
2. Store tokens in local storage
3. All subsequent requests auto-inject token via interceptor
4. Token auto-refresh when expired
5. Invalid tokens redirect to login

## 🔒 Token Management

### Token Storage

Tokens are stored in `SharedPreferences`:

- **Access Token:** `supremeai_token`
- **Refresh Token:** `supremeai_refresh_token`
- **User Data:** `supremeai_user`

### Automatic Token Injection

The `ApiService` interceptor automatically adds token to all requests:

```dart
// In ApiService._initializeDio()
onRequest: (options, handler) async {
  final token = await _storageService.getToken();
  if (token != null) {
    options.headers['Authorization'] = 'Bearer $token';
  }
  return handler.next(options);
}
```

## 🚀 Getting Started

### 1. Install Dependencies

```bash
cd flutter_admin_app
flutter pub get
```

### 2. Run App

```bash
# Choose your target device/emulator
flutter devices

# Run app
flutter run
```

### 3. Login

- Email: `admin@supremeai.com`
- Password: Set from backend registration

### 4. Test API Connectivity

- Login should work immediately
- Dashboard loads data from backend
- All API calls use configured Base URL

## 📋 Configuration Files

### `lib/config/environment.dart`

- Base URL for all API calls
- API endpoint paths
- Timeout settings
- Storage keys

### `lib/config/constants.dart`

- UI colors and theme
- Text sizes
- Padding/spacing values
- Error/success messages
- Validation rules

### `pubspec.yaml`

- Dependencies (Dio, Provider, etc.)
- Asset paths
- App metadata

## 🔍 Troubleshooting Configuration

### Issue: "Cannot connect to API"

**Check 1: Verify Base URL**

```dart
// In environment.dart, ensure correct URL
print(Environment.baseUrl); // Should print: https://supremeai-...
```

**Check 2: Verify Backend is Running**

```bash
# For Cloud Run - always available
# For localhost:8080 - run: .\gradlew run

# Test with curl
curl https://supremeai-565236080752.us-central1.run.app/api/metrics/health
```

**Check 3: Check Network**

```bash
# On device, verify internet connection
# On emulator, ensure internet access is enabled
flutter run -v  # Run with verbose logging
```

### Issue: "401 Unauthorized"

**Solution:**

1. Clear stored tokens:

```dart
final storageService = StorageService();
await storageService.clearAll();
```

2. Re-login with valid credentials
3. Check token expiration in JWT decoder

### Issue: "Timeout"

**Solution - Update timeout in environment.dart:**

```dart
static const int connectionTimeout = 60;  // Increase from 30
static const int receiveTimeout = 60;     // Increase from 30
```

## 📱 Platform-Specific Config

### Android

- Min API: 21
- Target API: 34
- Internet permission: Added in `AndroidManifest.xml`

### iOS

- Min iOS: 12.0
- HTTP config: Check `Info.plist` for ATS settings
- For local dev: Add localhost exception if needed

## 🔐 Security Configuration

### HTTPS Certificate Pinning (Optional)

Add to `ApiService` for production:

```dart
final httpClient = HttpClient();
httpClient.badCertificateCallback = (cert, host, port) => false;
// Implement proper pinning for production
```

### Token Encryption (Optional)

Override `StorageService` to use flutter_secure_storage:

```dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
```

## 🚢 Build Configuration

### Debug Build

```bash
flutter build apk --debug
```

### Release Build

```bash
flutter build apk --release
flutter build appbundle --release
flutter build ios --release
```

### Web Build (Future)

```bash
flutter build web --release
```

## 📊 Environment Variables

Currently no external `.env` file. To add .env support:

1. Add `flutter_dotenv` to pubspec.yaml
2. Create `.env` file with Base URL
3. Load in `main.dart` before running app

```dart
await dotenv.load(fileName: ".env");
String baseUrl = dotenv.env['BASE_URL']!;
```

## ✅ Configuration Checklist

Before deploying:

- [ ] Base URL is set correctly
- [ ] Backend is running and accessible
- [ ] All API endpoints are available
- [ ] JWT token endpoints work
- [ ] Storage service initialized before app start
- [ ] Error handling covers all failure scenarios
- [ ] Logging is configured for debugging
- [ ] HTTPS is used in production
- [ ] Token refresh mechanism works
- [ ] Authentication flow is tested

## 🎯 Next Steps

1. **Test all endpoints:** Use Postman to test each API endpoint
2. **Monitor logs:** Check Flutter console for API errors
3. **Test authentication:** Try login/logout flow
4. **Test data loading:** Verify dashboard data loads correctly
5. **Performance test:** Check latency and memory usage

## 📞 Support

For configuration issues:

1. Check the logs with `flutter run -v`
2. Review `SETUP_GUIDE.md`
3. Check backend is running and responding
4. Verify network connectivity

---

**Last Updated:** March 31, 2026  
**Version:** 1.0.0
