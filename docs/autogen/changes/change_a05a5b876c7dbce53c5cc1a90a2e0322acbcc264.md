# 📋 Commit a05a5b876c7dbce53c5cc1a90a2e0322acbcc264

## Commit Stats
```
commit a05a5b876c7dbce53c5cc1a90a2e0322acbcc264
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 21:50:37 2026 +0600

    fix(backend): change TOTP from 7 digits to 6 digits to support Google Authenticator

 backend/core/admin_routes.py        | 12 ++++++------
 backend/tests/test_cloud_sandbox.py |  8 ++++----
 2 files changed, 10 insertions(+), 10 deletions(-)

```

## Diff Detail
```diff
commit a05a5b876c7dbce53c5cc1a90a2e0322acbcc264
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 21:50:37 2026 +0600

    fix(backend): change TOTP from 7 digits to 6 digits to support Google Authenticator

diff --git a/backend/core/admin_routes.py b/backend/core/admin_routes.py
index 303975591..170cedbdc 100644
--- a/backend/core/admin_routes.py
+++ b/backend/core/admin_routes.py
@@ -209,9 +209,9 @@ def admin_firebase_totp_setup(payload: AdminFirebaseTotpSetupRequest):
         except Exception as e:
             logger.error(f"Failed to store temp TOTP secret in Firestore: {e}")
 
-    # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি রিকোয়েস্ট করার জন্য digits=7 যোগ করা হলো
+    # বাংলা মন্তব্য: ৬ ডিজিটের ওটিপি রিকোয়েস্ট করা হলো
     provisioning_uri = (
-        f"otpauth://totp/SupremeAI:{email}?secret={secret}&issuer=SupremeAI&digits=7"
+        f"otpauth://totp/SupremeAI:{email}?secret={secret}&issuer=SupremeAI&digits=6"
     )
     return {"secret": secret, "provisioning_uri": provisioning_uri}
 
@@ -425,8 +425,8 @@ def verify_totp_code(user_otp: str, base32_secret: str) -> bool:
             h = hmac.new(key, msg, hashlib.sha1).digest()
             o = h[19] & 15
             h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
-            # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
-            code = f"{h_num % 10000000:07d}"
+            # বাংলা মন্তব্য: ৬ ডিজিটের ওটিপি জেনারেট করা হলো
+            code = f"{h_num % 1000000:06d}"
             # বাংলা মন্তব্য: টাইমিং অ্যাটাক প্রতিরোধে constant-time তুলনা ব্যবহার করা হলো
             if hmac.compare_digest(code, user_otp):
                 return True
@@ -448,8 +448,8 @@ def check_totp(user_otp: str, base32_secret: str) -> bool:
             h = hmac.new(key, msg, hashlib.sha1).digest()
             o = h[19] & 15
             h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
-            # বাংলা মন্তব্য: ৭ ডিজিটের ওটিপি জেনারেট করা হলো এন্টারপ্রাইজ গ্রেড সিকিউরিটির জন্য
-            code = f"{h_num % 10000000:07d}"
+            # বাংলা মন্তব্য: ৬ ডিজিটের ওটিপি জেনারেট করা হলো
+            code = f"{h_num % 1000000:06d}"
             # বাংলা মন্তব্য: টাইমিং অ্যাটাক প্রতিরোধে constant-time তুলনা ব্যবহার করা হলো
             if hmac.compare_digest(code, user_otp):
                 return True
diff --git a/backend/tests/test_cloud_sandbox.py b/backend/tests/test_cloud_sandbox.py
index 213809205..625895389 100644
--- a/backend/tests/test_cloud_sandbox.py
+++ b/backend/tests/test_cloud_sandbox.py
@@ -193,7 +193,7 @@ class TestTOTPVerification:
         h = hmac.new(key, msg, hashlib.sha1).digest()
         o = h[19] & 15
         h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
-        valid_code = f"{h_num % 10000000:07d}"
+        valid_code = f"{h_num % 1000000:06d}"
 
         assert verify_totp_code(valid_code, secret) is True
 
@@ -201,8 +201,8 @@ class TestTOTPVerification:
         """TOTP কোড প্রসੂসিং এ এক্সেপশন হলে False রিটার্ন করে।"""
         from core.admin_routes import verify_totp_code
 
-        assert verify_totp_code("1234567", "") is False
-        assert verify_totp_code("1234567", "invalid-secret!!!") is False
+        assert verify_totp_code("123456", "") is False
+        assert verify_totp_code("123456", "invalid-secret!!!") is False
 
     def test_check_totp_success(self):
         """check_totp ফাংশন সফল ভেরিফিকেশন রিটার্ন করে।"""
@@ -218,7 +218,7 @@ class TestTOTPVerification:
         h = hmac.new(key, msg, hashlib.sha1).digest()
         o = h[19] & 15
         h_num = struct.unpack(">I", h[o : o + 4])[0] & 0x7FFFFFFF
-        valid_code = f"{h_num % 10000000:07d}"
+        valid_code = f"{h_num % 1000000:06d}"
 
         assert check_totp(valid_code, secret) is True
 

```
