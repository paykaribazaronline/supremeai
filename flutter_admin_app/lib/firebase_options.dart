// firebase_options.dart — manually maintained Firebase configuration
// Generated from google-services.json (Android) and Firebase project settings.
// Run `flutterfire configure` to regenerate if project settings change.

import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      default:
        // Fallback to web options for desktop builds
        return web;
    }
  }

  // Web: values from dashboard Firebase config
  static const FirebaseOptions web = FirebaseOptions(
    apiKey: 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8',
    appId: '1:565236080752:web:572bb9313db9afb355d4b5',
    messagingSenderId: '565236080752',
    projectId: 'supremeai-a',
    authDomain: 'supremeai-a.firebaseapp.com',
    databaseURL:
        'https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app',
    storageBucket: 'supremeai-a.firebasestorage.app',
    measurementId: 'G-KR71G4S1BR',
  );

  // Android: values from google-services.json
  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyAuJllVURoY8ivtdop4QIQAE5x4Zd2XZgs',
    appId: '1:565236080752:android:6ba81082d7807a8455d4b5',
    messagingSenderId: '565236080752',
    projectId: 'supremeai-a',
    databaseURL:
        'https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app',
    storageBucket: 'supremeai-a.firebasestorage.app',
  );

  // iOS: use same values as Android for now (update when iOS bundle ID is known)
  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyAuJllVURoY8ivtdop4QIQAE5x4Zd2XZgs',
    appId: '1:565236080752:ios:000000000000000055d4b5',
    messagingSenderId: '565236080752',
    projectId: 'supremeai-a',
    databaseURL:
        'https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app',
    storageBucket: 'supremeai-a.firebasestorage.app',
  );
}
