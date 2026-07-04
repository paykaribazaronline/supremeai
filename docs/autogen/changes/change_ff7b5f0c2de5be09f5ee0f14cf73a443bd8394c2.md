# 📋 Commit ff7b5f0c2de5be09f5ee0f14cf73a443bd8394c2

## Commit Stats
```
commit ff7b5f0c2de5be09f5ee0f14cf73a443bd8394c2
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 09:15:58 2026 +0600

    Fix GitHub Actions YAML job placement for Flutter integration tests

 .github/workflows/supreme-core-ci.yml | 58 +++++++++++++++++------------------
 1 file changed, 29 insertions(+), 29 deletions(-)

```

## Diff Detail
```diff
commit ff7b5f0c2de5be09f5ee0f14cf73a443bd8394c2
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 09:15:58 2026 +0600

    Fix GitHub Actions YAML job placement for Flutter integration tests

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 021bad89d..c5bc07aeb 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -567,35 +567,35 @@ jobs:
           name: k6-load-test
           path: load-test-output.json
 
-flutter-integration-tests:
-  name: 📱 Flutter Integration Test
-  needs: frontend-core
-  if: github.event_name == 'pull_request' # শুধুমাত্র PR-এর জন্য চালানো যেতে পারে
-  runs-on: macos-latest # iOS সিমুলেটরের জন্য macOS প্রয়োজন
-  strategy:
-    matrix:
-      api-level: [30] # Android API level
-      target: [ios, android]
-  steps:
-    - uses: actions/checkout@v4
-    - uses: subosito/flutter-action@v2
-      with:
-        channel: 'stable'
-    - name: Install Dependencies
-      run: |
-        cd apps/mobile
-        flutter pub get
-    - name: Run Flutter Integration Tests (Android)
-      if: matrix.target == 'android'
-      uses: reactivecircus/android-emulator-runner@v2
-      with:
-        api-level: ${{ matrix.api-level }}
-        script: cd apps/mobile && flutter test integration_test
-    - name: Run Flutter Integration Tests (iOS)
-      if: matrix.target == 'ios'
-      run: |
-        cd apps/mobile
-        flutter test integration_test
+  flutter-integration-tests:
+    name: 📱 Flutter Integration Test
+    needs: frontend-core
+    if: github.event_name == 'pull_request' # শুধুমাত্র PR-এর জন্য চালানো যেতে পারে
+    runs-on: macos-latest # iOS সিমুলেটরের জন্য macOS প্রয়োজন
+    strategy:
+      matrix:
+        api-level: [30] # Android API level
+        target: [ios, android]
+    steps:
+      - uses: actions/checkout@v4
+      - uses: subosito/flutter-action@v2
+        with:
+          channel: 'stable'
+      - name: Install Dependencies
+        run: |
+          cd apps/mobile
+          flutter pub get
+      - name: Run Flutter Integration Tests (Android)
+        if: matrix.target == 'android'
+        uses: reactivecircus/android-emulator-runner@v2
+        with:
+          api-level: ${{ matrix.api-level }}
+          script: cd apps/mobile && flutter test integration_test
+      - name: Run Flutter Integration Tests (iOS)
+        if: matrix.target == 'ios'
+        run: |
+          cd apps/mobile
+          flutter test integration_test
 
   build-and-release-desktop:
     name: 🖥️ Build & Release Desktop App

```
