# 📋 Commit fa7c6a4cd9eb971fd6babeed2364170bb3a5f3ef

## Commit Stats
```
commit fa7c6a4cd9eb971fd6babeed2364170bb3a5f3ef
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 00:38:43 2026 +0600

    test: mock fetch_secret in test_prod_docs_security to fix HF_API_KEY error

 backend/tests/test_prod_docs_security.py | 5 +++++
 1 file changed, 5 insertions(+)

```

## Diff Detail
```diff
commit fa7c6a4cd9eb971fd6babeed2364170bb3a5f3ef
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 00:38:43 2026 +0600

    test: mock fetch_secret in test_prod_docs_security to fix HF_API_KEY error

diff --git a/backend/tests/test_prod_docs_security.py b/backend/tests/test_prod_docs_security.py
index 77501ca8e..1c50a1c76 100644
--- a/backend/tests/test_prod_docs_security.py
+++ b/backend/tests/test_prod_docs_security.py
@@ -92,6 +92,11 @@ def test_docs_disabled_in_production():
         os.environ["SUPREMEAI_ADMIN_PASSWORD_HASH"] = "mock_hash_for_production_test"
         os.environ["docs_auth_enabled"] = "false"
         os.environ["REDIS_URL"] = "redis://mock:6379"
+        
+        # Mock secret fetching to prevent errors for missing production secrets
+        import core.secret_vault as sv
+        sv.ProductionSecretVault.fetch_secret = lambda self, name: "mock"
+        
         import core.app as app_mod
         import core.services as services
 

```
