# SupremeAI Flutter Admin App

Professional Android & iOS admin application for SupremeAI backend management.

## Getting Started

```bash
flutter pub get
flutter run
```

## Base URL Configuration

**Production:** `https://supremeai-565236080752.us-central1.run.app`

The app is configured to use this Cloud Run endpoint for all API requests.

## Project Structure

```
lib/
├── main.dart                 # App entry point
├── config/
│   ├── constants.dart        # App constants
│   └── environment.dart      # Environment configuration
├── models/
│   ├── user_model.dart
│   ├── project_model.dart
│   └── api_provider_model.dart
├── services/
│   ├── api_service.dart      # HTTP client & API communication
│   ├── auth_service.dart     # Authentication logic
│   └── storage_service.dart  # Local storage
├── providers/
│   ├── auth_provider.dart
│   ├── project_provider.dart
│   └── ui_provider.dart
├── screens/
│   ├── auth/
│   │   ├── login_screen.dart
│   │   └── register_screen.dart
│   ├── dashboard/
│   │   ├── home_screen.dart
│   │   └── widgets/
│   └── projects/
│       ├── projects_screen.dart
│       └── project_detail_screen.dart
├── widgets/
│   ├── custom_app_bar.dart
│   ├── loading_dialog.dart
│   └── error_widget.dart
└── utils/
    ├── validators.dart
    └── formatters.dart
```

## Features

✅ JWT Authentication (Login/Register)
✅ Admin Dashboard with Real-time Data
✅ Project Management
✅ API Provider Configuration
✅ AI Agent Assignment
✅ Monitoring & Metrics
✅ Offline Support
✅ Dark Mode Support

## Target Devices

- Android 7.0+ (API 24)
- iOS 12.0+

## Environment

```dart
const String BASE_URL = 'https://supremeai-565236080752.us-central1.run.app';
```
