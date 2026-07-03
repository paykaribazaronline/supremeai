# 📋 Commit b8f2fd361bd85e50e5dc5a80b76ddc8d4136af77

## Commit Stats
```
commit b8f2fd361bd85e50e5dc5a80b76ddc8d4136af77
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:58:28 2026 +0600

    fix: hardcoded path, middleware pattern, AbortController in ThemeContext, DB pool init, React monorepo overrides

 apps/desktop/src-ui/package.json                 | 10 +++++-----
 apps/studio-client/src/contexts/ThemeContext.tsx | 22 +++++++++++++++++-----
 backend/core/lifespan.py                         | 22 +++++++---------------
 backend/core/origin_validator.py                 | 10 +++++-----
 package.json                                     |  4 ++--
 scripts/generate_md.py                           |  6 ++++--
 6 files changed, 40 insertions(+), 34 deletions(-)

```

## Diff Detail
```diff
commit b8f2fd361bd85e50e5dc5a80b76ddc8d4136af77
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:58:28 2026 +0600

    fix: hardcoded path, middleware pattern, AbortController in ThemeContext, DB pool init, React monorepo overrides

diff --git a/apps/desktop/src-ui/package.json b/apps/desktop/src-ui/package.json
index 8d5e16bdf..825f6450e 100644
--- a/apps/desktop/src-ui/package.json
+++ b/apps/desktop/src-ui/package.json
@@ -9,12 +9,12 @@
     "@testing-library/user-event": "^13.5.0",
     "@types/jest": "^29.0.0",
     "@types/node": "^16.18.0",
-    "@types/react": "^18.0.0",
-    "@types/react-dom": "^18.0.0",
-    "react": "^18.2.0",
-    "react-dom": "^18.2.0",
+    "@types/react": "^19.0.0",
+    "@types/react-dom": "^19.0.0",
+    "react": "^19.2.7",
+    "react-dom": "^19.2.7",
     "react-router-dom": "^6.4.0",
-    "typescript": "^4.9.0",
+    "typescript": "^5.4.0",
     "zustand": "^4.3.9"
   },
   "scripts": {
diff --git a/apps/studio-client/src/contexts/ThemeContext.tsx b/apps/studio-client/src/contexts/ThemeContext.tsx
index 66ed75463..caf21ce7a 100644
--- a/apps/studio-client/src/contexts/ThemeContext.tsx
+++ b/apps/studio-client/src/contexts/ThemeContext.tsx
@@ -18,6 +18,12 @@ export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ childre
   const [theme, setTheme] = useState<Theme>('dark'); // ডিফল্ট Deep Space (dark)
 
   useEffect(() => {
+    // বাংলা মন্তব্য: Race Condition এড়াতে AbortController ব্যবহার করা হয়েছে
+    const controller = new AbortController();
+    const token = getAdminToken();
+
+    if (!token) return;
+
     // 1. লোকাল স্টোরেজ থেকে থিম পড়া (Optimistic Load)
     const localTheme = localStorage.getItem('supremeai_theme') as Theme | null;
     if (localTheme && THEME_ORDER.includes(localTheme)) {
@@ -26,20 +32,26 @@ export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ childre
     
     // 2. ব্যাকএন্ড থেকে ফেচ করা (Cross-device sync)
     const API_BASE = getApiBaseUrl();
-    const token = getAdminToken();
     fetch(`${API_BASE}/api/v1/preferences`, {
       headers: {
         'Authorization': `Bearer ${token}`
-      }
+      },
+      signal: controller.signal
     })
-      .then(res => res.json())
+      .then(res => res.ok ? res.json() : Promise.reject())
       .then(data => {
-        if (data && data.theme && data.theme !== localTheme) {
+        if (data?.theme) {
           setTheme(data.theme);
           localStorage.setItem('supremeai_theme', data.theme);
         }
       })
-      .catch(err => console.log('Background theme sync skipped or failed:', err));
+      .catch(err => {
+        if (err.name !== 'AbortError') {
+          console.error('Theme sync failed:', err);
+        }
+      });
+
+    return () => controller.abort(); // কম্পোনেন্ট আনমাউন্ট হলে রিকোয়েস্ট বাতিল
   }, []);
 
   useEffect(() => {
diff --git a/backend/core/lifespan.py b/backend/core/lifespan.py
index ebb9cb7a2..932ce3d80 100644
--- a/backend/core/lifespan.py
+++ b/backend/core/lifespan.py
@@ -10,6 +10,7 @@ from core.config import settings
 from core.discord_bot import SupremeDiscordBot
 from core.orchestrator import Orchestrator
 from core.pgbouncer_pool import get_db_pool
+from core.pgbouncer_pool import init_db_pool
 from core.redis_manager import redis_manager
 
 
@@ -85,22 +86,13 @@ async def app_lifespan(app):
     logger.info("✅ Global HTTP Connection Pool initialized [Max Cons: 200].")
 
     try:
-        await get_db_pool()
-        logger.info("PgBouncer connection pool accessed on startup")
+        db_url = settings.supabase_database_url
+        await init_db_pool(db_url)
+        logger.info("⚡ PgBouncer connection pool successfully initialized at startup.")
         await _ensure_api_key_tables()
-    except RuntimeError:
-        try:
-            from core.pgbouncer_pool import init_db_pool
-
-            db_url = settings.supabase_database_url
-            if isinstance(db_url, str) and db_url.startswith(("postgresql://", "postgres://")):
-                await init_db_pool(db_url)
-                logger.info("PgBouncer connection pool initialized on startup")
-                await _ensure_api_key_tables()
-            else:
-                logger.warning("PgBouncer pool initialization deferred: non-PostgreSQL DSN")
-        except RuntimeError as exc:
-            logger.warning(f"PgBouncer pool initialization deferred: {exc}")
+    except Exception as exc:
+        logger.error(f"❌ Failed to initialize DB Pool: {exc}")
+        raise exc
 
     try:
         await redis_manager.initialize()
diff --git a/backend/core/origin_validator.py b/backend/core/origin_validator.py
index a0b8033f9..a76daa551 100644
--- a/backend/core/origin_validator.py
+++ b/backend/core/origin_validator.py
@@ -1,9 +1,9 @@
 # বাংলা কমেন্ট: সুপ্রিম-এআই এর ট্রাস্টেড অরিজিন ভ্যালিডেশন মিডলওয়্যার।
 # এটি ওয়াইল্ডকার্ড CORS বাইপাস রোধ করে এবং শুধুমাত্র অনুমোদিত ডোমেইন থেকে এপিআই অ্যাক্সেস নিশ্চিত করে।
 
-from fastapi import HTTPException
 from fastapi import Request
 from fastapi import status
+from fastapi.responses import JSONResponse
 from starlette.middleware.base import BaseHTTPMiddleware
 
 from core.config import settings
@@ -23,9 +23,9 @@ class TrustedOriginMiddleware(BaseHTTPMiddleware):
         if origin and origin not in self.allowed_origins:
                 client_ip = request.client.host if request.client else "unknown"
                 logger.critical(f"🔥 CSRF ALERT: Unauthorized Origin Access Blocked! Malicious Origin: {origin} from IP: {client_ip}")
-                raise HTTPException(
+                return JSONResponse(
                     status_code=status.HTTP_403_FORBIDDEN,
-                    detail="Cross-Origin Request Blocked. Device identity unauthorized."
+                    content={"detail": "Cross-Origin Request Blocked. Device identity unauthorized."}
                 )
                 
         # বাংলা মন্তব্য: হোস্ট হেডার ভ্যালিডেশন - WHOLE DOMAIN ম্যাচিং, substring vulnerability removed
@@ -33,9 +33,9 @@ class TrustedOriginMiddleware(BaseHTTPMiddleware):
         is_allowed = host in set(settings.allowed_hosts) if host else True
         if host and not is_allowed:
             logger.critical(f"🚨 Security Intrusion: Host Header Tampering Detected -> {host}")
-            raise HTTPException(
+            return JSONResponse(
                 status_code=status.HTTP_403_FORBIDDEN,
-                detail="Host verification failure."
+                content={"detail": "Host verification failure."}
             )
 
         # বাংলা কমেন্ট: ভ্যালিডেশন সাকসেসফুল হলে রিকোয়েস্ট পরবর্তী প্রসেসে পাস হবে
diff --git a/package.json b/package.json
index 32306b25f..7535b794a 100644
--- a/package.json
+++ b/package.json
@@ -34,8 +34,8 @@
   "overrides": {
     "typescript": "5.4.5",
     "vite": "7.3.5",
-    "react": "18.2.0",
-    "react-dom": "18.2.0"
+    "react": "19.2.7",
+    "react-dom": "19.2.7"
   },
   "engines": {
     "node": ">=20.0.0",
diff --git a/scripts/generate_md.py b/scripts/generate_md.py
index afbf46f19..64263c961 100644
--- a/scripts/generate_md.py
+++ b/scripts/generate_md.py
@@ -1,7 +1,9 @@
+from pathlib import Path
 import os
 
-root_dir = r"c:\Users\n\supremeai\supremeai_2.0"
-output_file = os.path.join(root_dir, "project_code.md")
+# বাংলা মন্তব্য: স্ক্রিপ্টের সাপেক্ষে প্রজেক্ট রুট ডিরেক্টরি ডাইনামিকভাবে নির্ধারণ করা
+root_dir = Path(__file__).resolve().parents[1]
+output_file = root_dir / "project_code.md"
 
 exclude_dirs = {'.git', '.venv', 'node_modules', '__pycache__', 'build', 'dist', '.dart_tool', '.idea', '.vscode', 'coverage', '.mypy_cache', '.pytest_cache', 'android', 'ios', 'web', 'windows', 'macos', 'linux'}
 exclude_exts = {'.pyc', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.ico', '.pdf', '.zip', '.tar', '.gz', '.db', '.sqlite3', '.lock', '.ttf'}

```
