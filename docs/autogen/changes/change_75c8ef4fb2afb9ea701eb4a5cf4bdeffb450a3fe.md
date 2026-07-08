# 📋 Commit 75c8ef4fb2afb9ea701eb4a5cf4bdeffb450a3fe

## Commit Stats
```
commit 75c8ef4fb2afb9ea701eb4a5cf4bdeffb450a3fe
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 17:18:49 2026 +0600

    fix(docker): include tools group in poetry install to resolve missing discord module on startup

 backend/Dockerfile | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

```

## Diff Detail
```diff
commit 75c8ef4fb2afb9ea701eb4a5cf4bdeffb450a3fe
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 17:18:49 2026 +0600

    fix(docker): include tools group in poetry install to resolve missing discord module on startup

diff --git a/backend/Dockerfile b/backend/Dockerfile
index 05dd75e9d..b53ee2199 100644
--- a/backend/Dockerfile
+++ b/backend/Dockerfile
@@ -12,7 +12,7 @@ RUN poetry config virtualenvs.in-project true
 
 # ক্যাশ লেয়ার: শুধু ডিপেন্ডেন্সি ইন্সটল
 COPY backend/pyproject.toml backend/poetry.lock* ./
-RUN poetry install --no-interaction --no-ansi --no-root --only main
+RUN poetry install --no-interaction --no-ansi --no-root --with tools
 
 # Stage 2: Runner
 FROM python:3.11-slim AS runner

```
