# Simulator Runtime Service
# Lightweight FastAPI container to serve generated apps with device emulation
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies (ca-certificates for TLS)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install Python deps
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source
COPY main.py .

# Run as non-root
RUN useradd --create-home appuser
USER appuser

# Expose Cloud Run default port
EXPOSE 8080

# Run with uvicorn (dynamic port for Cloud Run)
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8080}"]
