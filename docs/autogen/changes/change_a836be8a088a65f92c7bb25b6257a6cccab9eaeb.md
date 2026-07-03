# 📋 Commit a836be8a088a65f92c7bb25b6257a6cccab9eaeb

## Commit Stats
```
commit a836be8a088a65f92c7bb25b6257a6cccab9eaeb
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Fri Jul 3 21:02:24 2026 +0600

    fix(ci): enable github pages enablement for configure-pages step

 .github/workflows/supreme-core-ci.yml | 2 ++
 1 file changed, 2 insertions(+)

```

## Diff Detail
```diff
commit a836be8a088a65f92c7bb25b6257a6cccab9eaeb
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Fri Jul 3 21:02:24 2026 +0600

    fix(ci): enable github pages enablement for configure-pages step

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index b1654a744..1010bc297 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -532,6 +532,8 @@ jobs:
       - name: Setup GitHub Pages Environment
         if: github.ref == 'refs/heads/main'
         uses: actions/configure-pages@v5
+        with:
+          enablement: true # বাংলা মন্তব্য: রিপোজিটরিতে যদি পেজেস কনফিগার করা না থাকে, তবে এটি স্বয়ংক্রিয়ভাবে অ্যাকশনস সোর্স দিয়ে চালু করবে।
       - name: Upload Artifact to Pages
         if: github.ref == 'refs/heads/main'
         uses: actions/upload-pages-artifact@v3

```
