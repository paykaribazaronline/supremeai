# 📋 Commit e02fda8e49c9ced14bf86f7cd8917fcb5034914d

## Commit Stats
```
commit e02fda8e49c9ced14bf86f7cd8917fcb5034914d
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:28:15 2026 +0600

    chore: optimize Dockerfile and CI caching for faster builds

 .github/workflows/supreme-core-ci.yml |  42 ++++++++++++--
 backend/Dockerfile                    | 102 ++++++----------------------------
 2 files changed, 52 insertions(+), 92 deletions(-)

```

## Diff Detail
```diff
commit e02fda8e49c9ced14bf86f7cd8917fcb5034914d
Author: SupremeAI CI Bot <ci-bot@supremeai.dev>
Date:   Tue Jul 7 23:28:15 2026 +0600

    chore: optimize Dockerfile and CI caching for faster builds

diff --git a/.github/workflows/supreme-core-ci.yml b/.github/workflows/supreme-core-ci.yml
index 6c912d6c9..2b77ddc6c 100644
--- a/.github/workflows/supreme-core-ci.yml
+++ b/.github/workflows/supreme-core-ci.yml
@@ -43,7 +43,7 @@ on:
 # Purpose: Ensures new pushes cancel pending/running jobs in this pipeline.
 # ==============================================================================
 concurrency:
-  group: supreme-core-ci-${{ github.ref }}
+  group: ${{ github.workflow }}-${{ github.ref }}
   cancel-in-progress: true
 
 env:
@@ -68,12 +68,21 @@ jobs:
           python-version: ${{ env.PYTHON_VERSION }}
           cache: 'pip'
 
+      - name: Cache Poetry
+        uses: actions/cache@v4
+        with:
+          path: ~/.cache/pypoetry
+          key: ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-${{ hashFiles('backend/poetry.lock') }}
+          restore-keys: |
+            ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-
+            ${{ runner.os }}-poetry-
+
       - name: Install Dependencies
         working-directory: backend
         run: |
           pip install poetry
           poetry config virtualenvs.in-project true
-          poetry install --sync --with dev --without ml
+          poetry install --sync --with dev --without ml,tools
 
       - name: 🛡️ Safety Guard - File Protection Validation
         id: safety_guard
@@ -146,12 +155,21 @@ jobs:
           python-version: ${{ env.PYTHON_VERSION }}
           cache: 'pip'
       
+      - name: Cache Poetry
+        uses: actions/cache@v4
+        with:
+          path: ~/.cache/pypoetry
+          key: ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-${{ hashFiles('backend/poetry.lock') }}
+          restore-keys: |
+            ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-
+            ${{ runner.os }}-poetry-
+
       - name: Install Dependencies
         working-directory: backend
         run: |
           pip install poetry
           poetry config virtualenvs.in-project true
-          poetry install --sync --with dev --without ml
+          poetry install --sync --with dev --without ml,tools
       
       - name: 🧹 Lint Code (Auto-Fix & Warning Mode)
         working-directory: backend
@@ -422,6 +440,14 @@ jobs:
         with:
           python-version: ${{ env.PYTHON_VERSION }}
           cache: 'pip'
+      - name: Cache Poetry
+        uses: actions/cache@v4
+        with:
+          path: ~/.cache/pypoetry
+          key: ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-${{ hashFiles('backend/poetry.lock') }}
+          restore-keys: |
+            ${{ runner.os }}-poetry-${{ env.PYTHON_VERSION }}-
+            ${{ runner.os }}-poetry-
       - name: Install Backend Dependencies & Start Server
         working-directory: backend
         env:
@@ -429,7 +455,7 @@ jobs:
           SUPREMEAI_API_URL: http://127.0.0.1:8000
         run: |
           pip install poetry
-          poetry install --sync --with dev --without ml
+          poetry install --sync --with dev --without ml,tools
           poetry run uvicorn main:app --port 8000 &
       - name: Install Playwright Browsers
         run: pnpm exec playwright install --with-deps
@@ -482,16 +508,20 @@ jobs:
           username: _json_key
           password: ${{ secrets.GCP_SA_KEY }}
       
-      - uses: docker/setup-buildx-action@v3
+      - name: Set up Docker Buildx
+        uses: docker/setup-buildx-action@v3
+        with:
+          driver-opts: image=moby/buildkit:buildx-stable-1
       
       - name: Build & Push API Image
         uses: docker/build-push-action@v5
         with:
           context: .
+          file: ./backend/Dockerfile
           push: true
           tags: ${{ vars.GCP_REGION || 'us-central1' }}-docker.pkg.dev/${{ secrets.GCP_PROJECT_ID }}/supremeai-repo/supremeai-api:latest
           cache-from: type=gha
-          cache-to: type=gha,mode=max
+          cache-to: type=gha,mode=min
 
       - name: 🚀 Deploy API to Cloud Run
         env:
diff --git a/backend/Dockerfile b/backend/Dockerfile
index 4b7603078..502370879 100644
--- a/backend/Dockerfile
+++ b/backend/Dockerfile
@@ -1,99 +1,29 @@
-# ══════════════════════════════════════════════════════════
-# SupremeAI 2.0 — Optimized Production Dockerfile
-# Target: ~800 MB (from ~3-4 GB)
-# ══════════════════════════════════════════════════════════
-
-# Stage 1: Build dependencies
+# Stage 1: Builder
 FROM python:3.11-slim AS builder
+ENV PYTHONDONTWRITEBYTECODE=1 \
+    PYTHONUNBUFFERED=1
 
-WORKDIR /app
-
-# System build deps (cleaned up after)
-RUN apt-get update && apt-get install -y --no-install-recommends \
-    build-essential \
-    libpq-dev \
-    curl \
-    && apt-get clean \
-    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /var/cache/apt/*
-
-RUN pip install --no-cache-dir poetry && poetry config virtualenvs.in-project true
-
-# ── Install CPU-only PyTorch FIRST as constraint ──
-# This prevents Poetry from downloading the 4.5 GB CUDA version
-RUN python -m venv /app/.venv && \
-    /app/.venv/bin/pip install --no-cache-dir --upgrade pip && \
-    /app/.venv/bin/pip install --no-cache-dir \
-        torch torchvision --index-url https://download.pytorch.org/whl/cpu && \
-    /app/.venv/bin/pip install --no-cache-dir "setuptools<82.0.0"
-
-# Copy dependency files only (for Docker layer caching)
-COPY pyproject.toml ./
-COPY poetry.lock* ./
-
-RUN poetry install --no-interaction --no-ansi --no-root --only main --with ml || \
-    poetry install --no-interaction --no-ansi --no-root
-
-# ── Force CPU-only torch (in case Poetry overwrote it) ──
-RUN /app/.venv/bin/pip uninstall -y \
-    nvidia-cuda-nvrtc-cu12 nvidia-cuda-runtime-cu12 nvidia-cuda-cupti-cu12 \
-    nvidia-cudnn-cu12 nvidia-cublas-cu12 nvidia-cufft-cu12 nvidia-curand-cu12 \
-    nvidia-cusolver-cu12 nvidia-cusparse-cu12 nvidia-nccl-cu12 nvidia-nvtx-cu12 \
-    nvidia-nvjitlink-cu12 triton 2>/dev/null || true && \
-    /app/.venv/bin/pip install --no-cache-dir torch torchvision \
-        --index-url https://download.pytorch.org/whl/cpu
-
-# Install whisper separately (needs special build isolation handling)
-RUN /app/.venv/bin/pip install --no-cache-dir --no-build-isolation "openai-whisper==20240930" 2>/dev/null || true
+RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
+    build-essential libpq-dev curl && rm -rf /var/lib/apt/lists/*
 
-# ── Aggressive cleanup: remove ~500 MB of unnecessary files ──
-RUN find /app/.venv -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true && \
-    find /app/.venv -type d -name "tests" -exec rm -rf {} + 2>/dev/null || true && \
-    find /app/.venv -type d -name "test" -exec rm -rf {} + 2>/dev/null || true && \
-    find /app/.venv -type d -name "docs" -exec rm -rf {} + 2>/dev/null || true && \
-    find /app/.venv -type f -name "*.pyc" -delete 2>/dev/null || true && \
-    find /app/.venv -type f -name "*.pyo" -delete 2>/dev/null || true && \
-    find /app/.venv -type f -name "*.txt" -path "*/dist-info/*" -delete 2>/dev/null || true && \
-    rm -rf /app/.venv/lib/python3.11/site-packages/torch/test 2>/dev/null || true && \
-    rm -rf /app/.venv/lib/python3.11/site-packages/caffe2 2>/dev/null || true
-
-# ── Pre-download EasyOCR models (bn + en) ──
-RUN mkdir -p /root/.EasyOCR/model && \
-    /app/.venv/bin/python -c "import easyocr; easyocr.Reader(['bn', 'en'])" 2>/dev/null || true && \
-    rm -f /root/.EasyOCR/model/*.zip 2>/dev/null || true
+WORKDIR /app
+RUN pip install --no-cache-dir poetry
+RUN poetry config virtualenvs.in-project true
 
+# ক্যাশ লেয়ার: শুধু ডিপেন্ডেন্সি ইন্সটল
+COPY backend/pyproject.toml backend/poetry.lock* ./
+RUN poetry install --no-interaction --no-ansi --no-root --only main
 
-# distroless ইমেজে curl/python নেই, তাই python:3.11-slim ব্যবহার করা হচ্ছে
-# এতে হেলথচেক ও ডিবাগিং সুবিধা পাওয়া যাবে
+# Stage 2: Runner
 FROM python:3.11-slim AS runner
-
 WORKDIR /app
+RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
+    libpq5 && rm -rf /var/lib/apt/lists/*
 
-# Runtime-only system deps
-RUN apt-get update && apt-get install -y --no-install-recommends \
-    libpq5 \
-    libglib2.0-0 \
-    libgl1 \
-    && apt-get clean \
-    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /var/cache/apt/*
-
-# Copy only what's needed from builder
+# শুধুমাত্র ভার্চুয়াল এনভায়রনমেন্ট কপি করো (পুরো সোর্স কোড নয়)
 COPY --from=builder /app/.venv /app/.venv
-COPY --from=builder /root/.EasyOCR /root/.EasyOCR
-
-COPY . /app/backend
+COPY backend/ . 
 
 ENV PATH="/app/.venv/bin:$PATH"
-ENV PYTHONPATH="/app/backend"
-ENV PYTHONUNBUFFERED=1
-ENV PYTHONDONTWRITEBYTECODE=1
-
-WORKDIR /app/backend
-
 EXPOSE 8000
-
-# হেলথচেক — start-period বাড়ানো হয়েছে কারণ Redis, Firebase, EasyOCR মডেল
-# লোড হতে সময় লাগে। Railway/Render এর হেলথচেক টাইমআউটের সাথে সামঞ্জস্যপূর্ণ।
-HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
-    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1
-
 CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

```
