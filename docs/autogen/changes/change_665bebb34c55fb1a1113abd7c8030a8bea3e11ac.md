# 📋 Commit 665bebb34c55fb1a1113abd7c8030a8bea3e11ac

## Commit Stats
```
commit 665bebb34c55fb1a1113abd7c8030a8bea3e11ac
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 17:31:07 2026 +0600

    fix(docker): use --only main,tools to exclude ml group during poetry install

 backend/Dockerfile | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

```

## Diff Detail
```diff
commit 665bebb34c55fb1a1113abd7c8030a8bea3e11ac
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Wed Jul 8 17:31:07 2026 +0600

    fix(docker): use --only main,tools to exclude ml group during poetry install

diff --git a/backend/Dockerfile b/backend/Dockerfile
index b53ee2199c..431d7f5c5e 100644
--- a/backend/Dockerfile
+++ b/backend/Dockerfile
@@ -12,7 +12,7 @@ RUN poetry config virtualenvs.in-project true
 
 # ক্যাশ লেয়ার: শুধু ডিপেন্ডেন্সি ইন্সটল
 COPY backend/pyproject.toml backend/poetry.lock* ./
-RUN poetry install --no-interaction --no-ansi --no-root --with tools
+RUN poetry install --no-interaction --no-ansi --no-root --only main,tools
 
 # Stage 2: Runner
 FROM python:3.11-slim AS runner

```
