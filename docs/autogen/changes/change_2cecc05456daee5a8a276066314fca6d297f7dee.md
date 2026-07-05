# 📋 Commit 2cecc05456daee5a8a276066314fca6d297f7dee

## Commit Stats
```
commit 2cecc05456daee5a8a276066314fca6d297f7dee
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 22:32:18 2026 +0600

    fix(backend): keep task stream open to prevent client reconnect rate-limiting

 backend/api/routes/task.py | 8 +++++++-
 1 file changed, 7 insertions(+), 1 deletion(-)

```

## Diff Detail
```diff
commit 2cecc05456daee5a8a276066314fca6d297f7dee
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 22:32:18 2026 +0600

    fix(backend): keep task stream open to prevent client reconnect rate-limiting

diff --git a/backend/api/routes/task.py b/backend/api/routes/task.py
index 8333ce48e..f6f668580 100644
--- a/backend/api/routes/task.py
+++ b/backend/api/routes/task.py
@@ -400,7 +400,13 @@ async def execute_task(req: TaskRequest, background_tasks: BackgroundTasks):
 @router.get("/api/task/stream")
 async def task_stream():
     async def keepalive():
-        yield f"data: {json.dumps({'status': 'alive', 'timestamp': datetime.datetime.now(datetime.UTC).isoformat()})}\n\n"
+        import asyncio
+        try:
+            while True:
+                yield f"data: {json.dumps({'status': 'alive', 'timestamp': datetime.datetime.now(datetime.UTC).isoformat()})}\n\n"
+                await asyncio.sleep(15)
+        except asyncio.CancelledError:
+            pass
 
     return StreamingResponse(
         keepalive(),

```
