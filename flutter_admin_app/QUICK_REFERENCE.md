# ✅ Flutter Admin App - Quick Reference

## 🚀 Start the App

```bash
cd c:\Users\Nazifa\supremeai\flutter_admin_app
flutter pub get
flutter run
```

## 🔑 Base URL

**Production (Cloud Run):**
```
https://supremeai-565236080752.us-central1.run.app
```

**Configuration File:**
```
lib/config/environment.dart (line 1)
```

## 📱 App Screens

1. **Splash Screen** - Initial loading screen
2. **Login Screen** - Authentication
   - Email: `admin@supremeai.com`
   - Auto-password from backend
3. **Register Screen** - New admin registration
4. **Home/Dashboard** - Main admin interface
   - Welcome card
   - Metrics cards
   - Quick action buttons
   - Profile menu

## 🔐 Authentication Flow

```
Login Form → API POST /api/auth/login → JWT Tokens
    ↓
Store Tokens (SharedPreferences)
    ↓
Extract User Data from JWT
    ↓
Navigate to Dashboard
```

## 📡 API Calls

All API calls use `ApiService` singleton:

```dart
// In any screen/widget
final apiService = ApiService();

// GET
final response = await apiService.get<Map>('/api/projects');

// POST
final response = await apiService.post<Map>(
  '/api/auth/login',
  data: {'email': 'test@example.com', 'password': 'password'},
);
```

Auto-injects Bearer token in header:
```
Authorization: Bearer <JWT_TOKEN>
```

## 🎨 Colors & Theme

- Primary Blue: `#3B82F6`
- Secondary Green: `#10B981`
- Error Red: `#EF4444`
- Background: `#F8FAFC`

## 📊 File Organization

```
lib/
├── main.dart                 ← App entry point
├── config/environment.dart   ← BASE_URL HERE
├── services/api_service.dart ← All API calls
├── screens/auth/             ← Login/Register
└── screens/dashboard/        ← Home screen
```

## 🛠️ Key Classes

| File | Purpose |
|------|---------|
| `environment.dart` | Base URL & API paths |
| `api_service.dart` | HTTP client with auto-auth |
| `auth_service.dart` | Login/Register logic |
| `storage_service.dart` | Token & user data storage |
| `auth_provider.dart` | State management |
| `login_screen.dart` | Login UI |
| `home_screen.dart` | Dashboard UI |

## 🔄 Token Management

```dart
// Save tokens after login
await storageService.saveToken(jwtToken);
await storageService.saveRefreshToken(refreshToken);

// Get token
final token = await storageService.getToken();

// Clear on logout
await storageService.clearToken();
```

## 🧪 Test API Connectivity

```bash
# Check backend is running
curl https://supremeai-a.us-central1.run.app/api/metrics/health

# Or locally (if running on localhost)
curl http://localhost:8080/api/metrics/health
```

## 🐛 Enable Debug Logging

Run with verbose output:
```bash
flutter run -v
```

Logs show:
- 🔵 API requests
- 🟢 API responses
- 🔴 API errors

## 📋 Common Tasks

### Change Backend URL
Edit `lib/config/environment.dart`:
```dart
// Line 3
static const String baseUrl = 'YOUR_NEW_URL';
```

### Add New Screen
1. Create `lib/screens/new_screen.dart`
2. Add route in `main.dart` routes
3. Navigate with `Navigator.pushNamed('/new-screen')`

### Make API Call
1. Use `ApiService()` singleton
2. Call `.get()`, `.post()`, `.put()`, `.delete()`
3. Token auto-injected
4. Handle response

### Add State Management
1. Create new `ChangeNotifier` in `lib/providers/`
2. Register in `main.dart` with `ChangeNotifierProvider`
3. Use in widgets with `context.read<ProviderName>()`

## 🚢 Build for Release

```bash
# Android APK
flutter build apk --release

# iOS IPA
flutter build ipa --release
```

## 🔗 Related Backend

- **Backend Location:** `c:\Users\Nazifa\supremeai`
- **Backend Port:** 8080 (local) or Cloud Run
- **Auth Endpoints:** `/api/auth/*`
- **Admin Endpoints:** `/api/projects/*`, `/api/providers/*`

## 📚 Documentation

- `SETUP_GUIDE.md` - Detailed setup instructions
- `CONFIGURATION.md` - Configuration options
- `README.md` - Project overview

## ⚠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| "Cannot connect to API" | Check base URL in environment.dart |
| "401 Unauthorized" | Clear cache: `flutter clean` & re-login |
| "Socket exception" | Verify backend is running |
| "Build fails" | Run `flutter pub get` & `flutter pub upgrade` |

## ✅ Ready to Use!

The Flutter admin app is fully initialized and ready for:
1. ✅ Development
2. ✅ Testing
3. ✅ Deployment

Start developing new features now!

---

**Last Updated:** March 31, 2026  
**Backend URL:** https://supremeai-a.us-central1.run.app  
**Status:** Ready for Development
