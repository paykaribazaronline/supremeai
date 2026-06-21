# Stage 1: Build dependencies
FROM python:3.11-slim AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir poetry && poetry config virtualenvs.in-project true

# Pre-create virtualenv and install CPU-only PyTorch to save space
RUN python -m venv /app/.venv && \
    /app/.venv/bin/pip install --no-cache-dir --upgrade pip && \
    /app/.venv/bin/pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

COPY pyproject.toml poetry.lock* ./
RUN poetry install --no-interaction --no-ansi --no-root --only main

# Re-install CPU-only PyTorch and remove large CUDA/NVIDIA libraries to save ~2.5GB space
# [2026-06-21] Added by Antigravity to keep runner image minimal and avoid copy timeout
RUN /app/.venv/bin/pip uninstall -y torch torchvision nvidia-cuda-nvrtc-cu12 nvidia-cuda-runtime-cu12 nvidia-cuda-cupti-cu12 nvidia-cudnn-cu12 nvidia-cublas-cu12 nvidia-cufft-cu12 nvidia-curand-cu12 nvidia-cusolver-cu12 nvidia-cusparse-cu12 nvidia-nccl-cu12 nvidia-nvtx-cu12 nvidia-nvjitlink-cu12 triton && \
    /app/.venv/bin/pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

# Clean up build-time virtualenv caches to reduce copied size
RUN find /app/.venv -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
RUN find /app/.venv -name "*.pyc" -delete 2>/dev/null || true

# Stage 2: Final minimal runner image
FROM python:3.11-slim AS runner

WORKDIR /app

# Install only runtime system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Copy virtualenv and code from builder stage
COPY --from=builder /app/.venv /app/.venv
COPY . .

ENV PATH="/app/.venv/bin:$PATH"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Pre-download EasyOCR English & Bengali models during build for zero-maintenance offline operation
RUN python -c "import easyocr; easyocr.Reader(['bn', 'en'])" && \
    rm -f /root/.EasyOCR/model/*.zip && \
    find /app/.venv -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true && \
    find /app/.venv -name "*.pyc" -delete 2>/dev/null || true

CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}"]
