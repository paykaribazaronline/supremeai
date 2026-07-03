# 📋 Commit b3efc5ebae73421ad3be8b2273a59e9f16a3d816

## Commit Stats
```
commit b3efc5ebae73421ad3be8b2273a59e9f16a3d816
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Fri Jul 3 21:43:30 2026 +0600

    chore: replace k6 apt-get install with k6io/setup-k6 action

 .github/workflows/supreme-core-ci.yml | 8 ++------
 1 file changed, 2 insertions(+), 6 deletions(-)

```

## Diff Detail
```diff
commit b3efc5ebae73421ad3be8b2273a59e9f16a3d816
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Fri Jul 3 21:43:30 2026 +0600

    chore: replace k6 apt-get install with k6io/setup-k6 action

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index e70df30f0..30467ecf1 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -436,12 +436,8 @@ jobs:
     runs-on: ubuntu-latest
     steps:
       - uses: actions/checkout@v4
-      - name: Install k6
-        run: |
-          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keys.openpgp.org --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
-          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
-          sudo apt-get update
-          sudo apt-get install -y k6
+- name: Install k6
+  uses: k6io/setup-k6@v1
       - name: Run k6 load test
         env:
           SUPREMEAI_URL: ${{ env.SUPREMEAI_API_URL }}

```
