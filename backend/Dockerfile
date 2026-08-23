# Stage 1: Builder
FROM python:3.11-slim AS builder

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install poetry
RUN pip install --no-cache-dir poetry==1.8.4

# Copy dependencies
COPY pyproject.toml poetry.lock ./

# Install dependencies in project virtualenv
RUN poetry config virtualenvs.in-project true \
    && poetry install --no-interaction --no-ansi --only main --no-root

# Stage 2: Runtime
FROM python:3.11-slim AS runtime

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8080

# Copy only the virtualenv from builder
COPY --from=builder /app/.venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"

# Copy application code
COPY . .

# Install Playwright and its system dependencies (must be run as root)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && playwright install --with-deps chromium

# Create and use non-root user for security compliance
RUN addgroup --system supremeai && adduser --system supremeai --ingroup supremeai \
    && chown -R supremeai:supremeai /app

USER supremeai

# Expose port
EXPOSE 8080

# 🔬 Evolution v3.0: Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/health/live')" || exit 1

# Start command
CMD ["python", "main.py"]

