"""
Dockerfile — BERT Sentiment ML Inference Service
Optimised for GCP Cloud Run (Gen 2)
"""
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies (CPU-only PyTorch)
RUN apt-get update -qq && apt-get install -y --no-install-recommends \
    gcc g++ libgomp1 curl && \
    rm -rf /var/lib/apt/lists/*

# CPU-only PyTorch wheels (smaller, no CUDA)
RUN pip install --no-cache-dir --find-links https://download.pytorch.org/whl/cpu \
    torch==2.3.1 torchvision==0.18.1 torchaudio==2.3.1

COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py /app/app.py

EXPOSE 8080

# Cloud Run expects container to listen on $PORT (mapped to 8080)
ENV PORT=8080
ENV PYTHONUNBUFFERED=1

CMD ["python", "app.py"]
