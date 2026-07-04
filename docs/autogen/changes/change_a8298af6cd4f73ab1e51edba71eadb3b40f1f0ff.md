# 📋 Commit a8298af6cd4f73ab1e51edba71eadb3b40f1f0ff

## Commit Stats
```
commit a8298af6cd4f73ab1e51edba71eadb3b40f1f0ff
Author: devin-ai-integration[bot] <158243242+devin-ai-integration[bot]@users.noreply.github.com>
Date:   Sat Jul 4 09:00:35 2026 +0600

    security: enforce admin role on /admin & /gcp routes, block mock-token TOTP bypass in prod, constant-time TOTP compare (#159)
    
    Co-authored-by: niloy joy <niloyjoy7@gmail.com>
    Co-authored-by: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>

 backend/core/admin_routes.py          | 18 ++++++++++++++++--
 backend/middleware/auth_middleware.py |  8 ++++++--
 2 files changed, 22 insertions(+), 4 deletions(-)

```

## Diff Detail
```diff
commit a8298af6cd4f73ab1e51edba71eadb3b40f1f0ff
Author: devin-ai-integration[bot] <158243242+devin-ai-integration[bot]@users.noreply.github.com>
Date:   Sat Jul 4 09:00:35 2026 +0600

    security: enforce admin role on /admin & /gcp routes, block mock-token TOTP bypass in prod, constant-time TOTP compare (#159)
    
    Co-authored-by: niloy joy <niloyjoy7@gmail.com>
    Co-authored-by: Devin AI <158243242+devin-ai-integration[bot]@users.noreply.github.com>

diff --git a/backend/core/admin_routes.py b/backend/core/admin_routes.py
index c78caf161..303975591 100644
--- a/backend/core/admin_routes.py
+++ b/backend/core/admin_routes.py
@@ -174,9 +174,15 @@ def admin_firebase_login(payload: AdminFirebaseLoginRequest):
 @router.post("/api/admin/firebase-totp-setup")
 def admin_firebase_totp_setup(payload: AdminFirebaseTotpSetupRequest):
     id_token = payload.id_token
+    is_production = getattr(settings, "env", "local").lower() == "production"
 
     try:
         if id_token.startswith("mock-"):
+            # বাংলা মন্তব্য: প্রোডাকশনে mock টোকেন দিয়ে TOTP সেটআপ বাইপাস কঠোরভাবে নিষিদ্ধ
+            if is_production:
+                raise HTTPException(
+                    status_code=403, detail="Mock tokens are strictly forbidden in production."
+                )
             uid = "mock-admin-uid"
             email = settings.admin_emails[0] if settings.admin_emails else "admin@example.com"
         elif auth:
@@ -214,9 +220,15 @@ def admin_firebase_totp_setup(payload: AdminFirebaseTotpSetupRequest):
 def admin_firebase_totp_verify(payload: AdminFirebaseTotpVerifyRequest):
     id_token = payload.id_token
     otp = payload.otp
+    is_production = getattr(settings, "env", "local").lower() == "production"
 
     try:
         if id_token.startswith("mock-"):
+            # বাংলা মন্তব্য: প্রোডাকশনে mock টোকেন দিয়ে TOTP ভেরিফিকেশন বাইপাস কঠোরভাবে নিষিদ্ধ
+            if is_production:
+                raise HTTPException(
+                    status_code=403, detail="Mock tokens are strictly forbidden in production."
+                )
             uid = "mock-admin-uid"
         elif auth:
             decoded_token = auth.verify_id_token(id_token)
@@ -415,7 +427,8 @@ def verify_totp_code(user_otp: str, base32_secret: str) -> bool:
             h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
             # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
             code = f"{h_num % 10000000:07d}"
-            if code == user_otp:
+            # বাংলা মন্তব্য: টাইমিং অ্যাটাক প্রতিরোধে constant-time তুলনা ব্যবহার করা হলো
+            if hmac.compare_digest(code, user_otp):
                 return True
         return False
     except Exception:
@@ -437,7 +450,8 @@ def check_totp(user_otp: str, base32_secret: str) -> bool:
             h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
             # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
             code = f"{h_num % 10000000:07d}"
-            if code == user_otp:
+            # বাংলা মন্তব্য: টাইমিং অ্যাটাক প্রতিরোধে constant-time তুলনা ব্যবহার করা হলো
+            if hmac.compare_digest(code, user_otp):
                 return True
         return False
     except Exception:
diff --git a/backend/middleware/auth_middleware.py b/backend/middleware/auth_middleware.py
index ca5dd825e..9553e5ea4 100644
--- a/backend/middleware/auth_middleware.py
+++ b/backend/middleware/auth_middleware.py
@@ -64,9 +64,13 @@ class ZeroTrustAuthMiddleware(BaseHTTPMiddleware):
             request.state.user = payload
             request.state.tenant_id = payload.get("tenant_id") or payload.get("sub")
 
-            # অ্যাডমিন রাউটের জন্য স্ট্রিক্ট রোল চেক
+            # বাংলা মন্তব্য: শুধু "/api/admin" নয়, prefix ছাড়া রেজিস্টার হওয়া সব
+            # অ্যাডমিন-লেভেল রাউটেও (/admin/*, /admin-api/*, /gcp/*) স্ট্রিক্ট রোল চেক
+            # প্রয়োগ করা হলো — নয়তো সাধারণ ইউজার টোকেন দিয়ে admin_routes.py এর
+            # /admin/rules, /admin/free-tier-override ইত্যাদি অ্যাক্সেস করা যেত (privilege escalation)।
+            admin_prefixes = ("/api/admin", "/admin/", "/admin-api", "/gcp/")
             if (
-                request.url.path.startswith("/api/admin")
+                any(request.url.path.startswith(p) for p in admin_prefixes)
                 and payload.get("role") != "admin"
             ):
                 logger.critical(

```
