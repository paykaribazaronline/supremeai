# ══════════════════════════════════════════════════════════
# SupremeAI 2.0 — Optimized Production Dockerfile
# Target: ~800 MB (from ~3-4 GB)
# ══════════════════════════════════════════════════════════

# Stage 1: Build dependencies
FROM python:3.11-slim AS builder

WORKDIR /app

# System build deps (cleaned up after)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /var/cache/apt/*

RUN pip install --no-cache-dir poetry && poetry config virtualenvs.in-project true

# ── Install CPU-only PyTorch FIRST as constraint ──
# This prevents Poetry from downloading the 4.5 GB CUDA version
RUN python -m venv /app/.venv && \
    /app/.venv/bin/pip install --no-cache-dir --upgrade pip && \
    /app/.venv/bin/pip install --no-cache-dir \
        torch torchvision --index-url https://download.pytorch.org/whl/cpu && \
    /app/.venv/bin/pip install --no-cache-dir "setuptools<82.0.0"

# Copy dependency files only (for Docker layer caching)
COPY pyproject.toml ./
COPY poetry.lock* ./

RUN poetry install --no-interaction --no-ansi --no-root --only main --with ml || \
    poetry install --no-interaction --no-ansi --no-root

# ── Force CPU-only torch (in case Poetry overwrote it) ──
RUN /app/.venv/bin/pip uninstall -y \
    nvidia-cuda-nvrtc-cu12 nvidia-cuda-runtime-cu12 nvidia-cuda-cupti-cu12 \
    nvidia-cudnn-cu12 nvidia-cublas-cu12 nvidia-cufft-cu12 nvidia-curand-cu12 \
    nvidia-cusolver-cu12 nvidia-cusparse-cu12 nvidia-nccl-cu12 nvidia-nvtx-cu12 \
    nvidia-nvjitlink-cu12 triton 2>/dev/null || true && \
    /app/.venv/bin/pip install --no-cache-dir torch torchvision \
        --index-url https://download.pytorch.org/whl/cpu

# Install whisper separately (needs special build isolation handling)
RUN /app/.venv/bin/pip install --no-cache-dir --no-build-isolation "openai-whisper==20240930" 2>/dev/null || true

# ── Aggressive cleanup: remove ~500 MB of unnecessary files ──
RUN find /app/.venv -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/.venv -type d -name "tests" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/.venv -type d -name "test" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/.venv -type d -name "docs" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/.venv -type f -name "*.pyc" -delete 2>/dev/null || true && \
    find /app/.venv -type f -name "*.pyo" -delete 2>/dev/null || true && \
    find /app/.venv -type f -name "*.txt" -path "*/dist-info/*" -delete 2>/dev/null || true && \
    rm -rf /app/.venv/lib/python3.11/site-packages/torch/test 2>/dev/null || true && \
    rm -rf /app/.venv/lib/python3.11/site-packages/caffe2 2>/dev/null || true

# ── Pre-download EasyOCR models (bn + en) ──
RUN mkdir -p /root/.EasyOCR/model && \
    /app/.venv/bin/python -c "import easyocr; easyocr.Reader(['bn', 'en'])" 2>/dev/null || true && \
    rm -f /root/.EasyOCR/model/*.zip 2>/dev/null || true


# distroless ইমেজে curl/python নেই, তাই python:3.11-slim ব্যবহার করা হচ্ছে
# এতে হেলথচেক ও ডিবাগিং সুবিধা পাওয়া যাবে
FROM python:3.11-slim AS runner

WORKDIR /app

# Runtime-only system deps
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    libglib2.0-0 \
    libgl1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /var/cache/apt/*

# Copy only what's needed from builder
COPY --from=builder /app/.venv /app/.venv
COPY --from=builder /root/.EasyOCR /root/.EasyOCR

COPY . /app/backend

ENV PATH="/app/.venv/bin:$PATH"
ENV PYTHONPATH="/app/backend"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

WORKDIR /app/backend

EXPOSE 8000

# হেলথচেক — start-period বাড়ানো হয়েছে কারণ Redis, Firebase, EasyOCR মডেল
# লোড হতে সময় লাগে। Railway/Render এর হেলথচেক টাইমআউটের সাথে সামঞ্জস্যপূর্ণ।
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
