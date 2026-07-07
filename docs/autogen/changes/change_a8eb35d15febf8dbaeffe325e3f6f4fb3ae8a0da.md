# 📋 Commit a8eb35d15febf8dbaeffe325e3f6f4fb3ae8a0da

## Commit Stats
```
commit a8eb35d15febf8dbaeffe325e3f6f4fb3ae8a0da
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 00:36:29 2026 +0600

    test: set REDIS_URL in test_prod_docs_security.py to fix test failure

 backend/tests/test_prod_docs_security.py | 1 +
 1 file changed, 1 insertion(+)

```

## Diff Detail
```diff
commit a8eb35d15febf8dbaeffe325e3f6f4fb3ae8a0da
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 00:36:29 2026 +0600

    test: set REDIS_URL in test_prod_docs_security.py to fix test failure

diff --git a/backend/tests/test_prod_docs_security.py b/backend/tests/test_prod_docs_security.py
index 624489f02..77501ca8e 100644
--- a/backend/tests/test_prod_docs_security.py
+++ b/backend/tests/test_prod_docs_security.py
@@ -91,6 +91,7 @@ def test_docs_disabled_in_production():
         os.environ["CI_WEBHOOK_SECRET"] = "secure-ci-webhook-secret-for-testing-2026"
         os.environ["SUPREMEAI_ADMIN_PASSWORD_HASH"] = "mock_hash_for_production_test"
         os.environ["docs_auth_enabled"] = "false"
+        os.environ["REDIS_URL"] = "redis://mock:6379"
         import core.app as app_mod
         import core.services as services
 

```
