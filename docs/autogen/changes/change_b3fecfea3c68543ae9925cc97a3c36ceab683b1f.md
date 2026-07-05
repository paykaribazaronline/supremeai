# 📋 Commit b3fecfea3c68543ae9925cc97a3c36ceab683b1f

## Commit Stats
```
commit b3fecfea3c68543ae9925cc97a3c36ceab683b1f
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 06:51:05 2026 +0600

    fix: remove x-access-token from mirror push url

 .github/workflows/supreme-core-ci.yml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

```

## Diff Detail
```diff
commit b3fecfea3c68543ae9925cc97a3c36ceab683b1f
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sun Jul 5 06:51:05 2026 +0600

    fix: remove x-access-token from mirror push url

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 04176abbe..4ccc60abe 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -620,7 +620,7 @@ jobs:
         env:
           MIRROR_REPO_TOKEN: ${{ secrets.MIRROR_REPO_TOKEN }}
         run: |
-          git remote add mirror https://x-access-token:${MIRROR_REPO_TOKEN}@github.com/SaifulHaqueNiloy/supremeai.git
+          git remote add mirror https://${MIRROR_REPO_TOKEN}@github.com/SaifulHaqueNiloy/supremeai.git
           git push mirror main:refs/heads/main
 
   generate-codebase-docs:

```
