# Reverse Engineering Service - FastAPI
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies if needed (e.g., for Playwright/browser later)
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the entire repository (so we can import reverse_engineer package)
COPY .. ..

# Copy service code
COPY main.py .

ENV PORT=8080
ENV GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/cloud.json

EXPOSE 8080

CMD ["python", "main.py"]
