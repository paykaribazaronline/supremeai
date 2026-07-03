# 📋 Commit 5ccf4cad58ae83d32c963e215387aeba026fefb6

## Commit Stats
```
commit 5ccf4cad58ae83d32c963e215387aeba026fefb6
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:40:12 2026 +0600

    scripts: clean stale autogen docs and limit changelogs to 10

 scripts/generate_smart_docs.py | 20 ++++++++++++--------
 1 file changed, 12 insertions(+), 8 deletions(-)

```

## Diff Detail
```diff
commit 5ccf4cad58ae83d32c963e215387aeba026fefb6
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:40:12 2026 +0600

    scripts: clean stale autogen docs and limit changelogs to 10

diff --git a/scripts/generate_smart_docs.py b/scripts/generate_smart_docs.py
index 61b34a6f2..ebff6547e 100644
--- a/scripts/generate_smart_docs.py
+++ b/scripts/generate_smart_docs.py
@@ -51,6 +51,10 @@ def generate_docs():
     changes_dir = base_dir / "changes"
     full_dump_path = base_dir / "codebase_full.md"
 
+    # আগে পুরনো ফাইল ডিলিট করে নেওয়া (যেন ডিলিট করা ফাইলের পুরনো ডকস থেকে না যায়)
+    import shutil
+    if codebase_dir.exists():
+        shutil.rmtree(codebase_dir)
     codebase_dir.mkdir(parents=True, exist_ok=True)
     changes_dir.mkdir(parents=True, exist_ok=True)
 
@@ -97,12 +101,12 @@ def generate_docs():
     full_dump_path.write_text(full_dump_content, encoding='utf-8')
     print(f"Documented {file_count} files ({total_size:,} bytes)")
 
-    # ২. লাস্ট ১৫টি কমিটের চেঞ্জলগ তৈরি (বাংলা মন্তব্য: গিট থেকে শেষ ১৫টি কমিটের ডিটেইলস নিয়ে ফাইল তৈরি)
+    # ২. লাস্ট ১০টি কমিটের চেঞ্জলগ তৈরি (বাংলা মন্তব্য: গিট থেকে শেষ ১০টি কমিটের ডিটেইলস নিয়ে ফাইল তৈরি)
     # বাংলা মন্তব্য: ডিফ সাইজ ৫০০ কেবি-তে সীমাবদ্ধ রাখা হচ্ছে যাতে চেঞ্জলগ ফাইল খুব বড় না হয়ে পেজেস ডিপ্লয়মেন্ট ব্যর্থ না হয়
     MAX_DIFF_SIZE = 500 * 1024  # ৫০০ কেবি সর্বোচ্চ ডিফ সাইজ
-    print("Generating changelogs for the last 15 commits...")
+    print("Generating changelogs for the last 10 commits...")
     try:
-        commits = subprocess.check_output(["git", "log", "-n", "15", "--format=%H"]).decode().splitlines()
+        commits = subprocess.check_output(["git", "log", "-n", "10", "--format=%H"]).decode().splitlines()
         for commit in commits:
             try:
                 commit_info = subprocess.check_output(["git", "show", "--stat", commit]).decode('utf-8', errors='replace')
@@ -119,11 +123,11 @@ def generate_docs():
     except Exception as ge:
         print(f"Failed to get git history: {ge}")
 
-    # ৩. পুরনো চেঞ্জলগ ফাইলগুলো পরিষ্কার করা (সর্বশেষ ২০টি রাখা)
+    # ৩. পুরনো চেঞ্জলগ ফাইলগুলো পরিষ্কার করা (সর্বশেষ ১০টি রাখা)
     change_files = sorted(changes_dir.glob("change_*.md"), key=os.path.getmtime, reverse=True)
-    if len(change_files) > 20:
-        print(f"Pruning old changelogs (keeping max 20)...")
-        for f in change_files[20:]:
+    if len(change_files) > 10:
+        print(f"Pruning old changelogs (keeping max 10)...")
+        for f in change_files[10:]:
             try:
                 f.unlink()
             except Exception as e:
@@ -141,7 +145,7 @@ def generate_docs():
 - **কোডবেস ডাম্প:** [codebase_full.md](codebase_full.md) (পুরো কোডবেস একটি ফাইলে)
 
 ## চেঞ্জলগ
-সর্বশেষ ১৫-২০টি কমিটের বিস্তারিত পরিবর্তন এখানে সংরক্ষিত।
+সর্বশেষ ১০টি কমিটের বিস্তারিত পরিবর্তন এখানে সংরক্ষিত।
 - **ডিরেক্টরি:** [changes/](changes/)
 
 ---

```
