# 📋 Commit d68cb7c5e9e1a7cf6b30beb0ad6dc144b465e2b4

## Commit Stats
```
commit d68cb7c5e9e1a7cf6b30beb0ad6dc144b465e2b4
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 20:18:13 2026 +0600

    fix: add proxy methods for asyncpg pool to PgBouncerConnectionPool

 backend/core/pgbouncer_pool.py | 26 ++++++++++++++++++++++++++
 1 file changed, 26 insertions(+)

```

## Diff Detail
```diff
commit d68cb7c5e9e1a7cf6b30beb0ad6dc144b465e2b4
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 20:18:13 2026 +0600

    fix: add proxy methods for asyncpg pool to PgBouncerConnectionPool

diff --git a/backend/core/pgbouncer_pool.py b/backend/core/pgbouncer_pool.py
index 3f42a81e8..ecad4be41 100644
--- a/backend/core/pgbouncer_pool.py
+++ b/backend/core/pgbouncer_pool.py
@@ -37,6 +37,32 @@ class PgBouncerConnectionPool:
         if self._pool:
             await self._pool.release(conn)
 
+    # asyncpg.Pool এর মেথডগুলোকে সরাসরি কল করার জন্য proxy মেথডগুলো যুক্ত করা হলো
+    # যাতে কোডবেসে pool.execute() বা pool.fetch() কল করলে কোনো Attribute Error না দেয়।
+    async def execute(self, query: str, *args, **kwargs):
+        """Executes a query using the pool."""
+        if not self._pool:
+            raise RuntimeError("Connection pool not initialized.")
+        return await self._pool.execute(query, *args, **kwargs)
+
+    async def fetch(self, query: str, *args, **kwargs):
+        """Fetches rows using the pool."""
+        if not self._pool:
+            raise RuntimeError("Connection pool not initialized.")
+        return await self._pool.fetch(query, *args, **kwargs)
+
+    async def fetchrow(self, query: str, *args, **kwargs):
+        """Fetches a single row using the pool."""
+        if not self._pool:
+            raise RuntimeError("Connection pool not initialized.")
+        return await self._pool.fetchrow(query, *args, **kwargs)
+
+    async def fetchval(self, query: str, *args, **kwargs):
+        """Fetches a single value using the pool."""
+        if not self._pool:
+            raise RuntimeError("Connection pool not initialized.")
+        return await self._pool.fetchval(query, *args, **kwargs)
+
     async def close(self):
         """Closes the connection pool."""
         if self._pool:

```
