# 📋 Commit a166e835c1f1b96e1df9100ef2ac2eef553cdbcf

## Commit Stats
```
commit a166e835c1f1b96e1df9100ef2ac2eef553cdbcf
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:28:58 2026 +0600

    ci: start python backend before running k6 load tests

 .github/workflows/supreme-core-ci.yml | 14 +++++++++++++-
 1 file changed, 13 insertions(+), 1 deletion(-)

```

## Diff Detail
```diff
commit a166e835c1f1b96e1df9100ef2ac2eef553cdbcf
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Sat Jul 4 02:28:58 2026 +0600

    ci: start python backend before running k6 load tests

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 79f272bd8..2ba0722bd 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -429,12 +429,24 @@ jobs:
     runs-on: ubuntu-latest
     steps:
       - uses: actions/checkout@v4
+      - name: Set up Python
+        uses: actions/setup-python@v5
+        with:
+          python-version: '3.11'
+      - name: Start Backend for Testing
+        working-directory: backend
+        run: |
+          pip install poetry
+          poetry config virtualenvs.in-project true
+          poetry install --sync --without ml
+          poetry run python main.py &
+          sleep 10
       # বাংলা মন্তব্য: k6io/setup-k6 রিপোজিটরি অপসারিত হওয়ায় grafana/setup-k6-action@v1 ব্যবহার করা হলো
       - name: Install k6
         uses: grafana/setup-k6-action@v1
       - name: Run k6 load test
         env:
-          SUPREMEAI_URL: ${{ env.SUPREMEAI_API_URL }}
+          SUPREMEAI_URL: "http://127.0.0.1:8000"
         run: |
           echo "Running k6 load test against ${SUPREMEAI_URL}"
           k6 run --out json=load-test-output.json scripts/k6/load_test.js

```
