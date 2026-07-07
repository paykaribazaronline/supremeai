# 📋 Commit fdf160fa09d0a88dbe3cf3e9b7eec1b286a6be7d

## Commit Stats
```
commit fdf160fa09d0a88dbe3cf3e9b7eec1b286a6be7d
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 17:33:53 2026 +0600

    fix: restore download-artifact path to 'apps' due to common ancestor stripping

 .github/workflows/supreme-core-ci.yml | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

```

## Diff Detail
```diff
commit fdf160fa09d0a88dbe3cf3e9b7eec1b286a6be7d
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 17:33:53 2026 +0600

    fix: restore download-artifact path to 'apps' due to common ancestor stripping

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 2b0d998b9..44bbd573d 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -405,7 +405,7 @@ jobs:
         uses: actions/download-artifact@v4
         with:
           name: frontend-dist
-          path: .
+          path: apps
         continue-on-error: true
       # বাংলা মন্তব্য: আর্টিফ্যাক্ট ডাউনলোড ব্যর্থ হলে বা dist না থাকলে ফলব্যাক হিসেবে বিল্ড চালানো হবে
       - name: Fallback Build Frontend (if dist missing)
@@ -616,7 +616,7 @@ jobs:
         uses: actions/download-artifact@v4
         with:
           name: frontend-dist
-          path: .
+          path: apps
 
       - name: 🌐 Deploy to Firebase
         run: |

```
