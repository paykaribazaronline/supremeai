# 📋 Commit a9ef88bb7e34e03cc5b9e186cfe13b9d14c6edf3

## Commit Stats
```
commit a9ef88bb7e34e03cc5b9e186cfe13b9d14c6edf3
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Mon Jul 6 02:26:07 2026 +0600

    fix: increase admin api rate limit to prevent 429 on dashboard

 backend/api/routes/admin_dashboard.py | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

```

## Diff Detail
```diff
commit a9ef88bb7e34e03cc5b9e186cfe13b9d14c6edf3
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Mon Jul 6 02:26:07 2026 +0600

    fix: increase admin api rate limit to prevent 429 on dashboard

diff --git a/backend/api/routes/admin_dashboard.py b/backend/api/routes/admin_dashboard.py
index 7c64ea6f6..b565e14b1 100644
--- a/backend/api/routes/admin_dashboard.py
+++ b/backend/api/routes/admin_dashboard.py
@@ -78,7 +78,7 @@ def admin_rate_limit(request: Request):
 
     client_ip = request.client.host if request.client else "unknown"
     key = f"rate_limit:admin:{client_ip}"
-    limit = 20
+    limit = 600
     window = 60
 
     redis_queue = getattr(app_mod, "redis_queue", None)

```
