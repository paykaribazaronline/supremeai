FROM python:3.12-slim

WORKDIR /app

# Install dependencies (cached layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy app code
COPY . .

# Create data directory for SQLite
RUN mkdir -p /app/data
VOLUME /app/data

EXPOSE 8000

# Render sets PORT env var; fallback to 8000
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000} --workers 1"]
