# Stage 1: Builder
FROM python:3.11-slim AS builder
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    build-essential libpq-dev curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN pip install --no-cache-dir poetry
RUN poetry config virtualenvs.in-project true

# ক্যাশ লেয়ার: শুধু ডিপেন্ডেন্সি ইন্সটল
COPY backend/pyproject.toml backend/poetry.lock* ./
RUN poetry install --no-interaction --no-ansi --no-root --only main,tools

# Stage 2: Runner
FROM python:3.11-slim AS runner
WORKDIR /app
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    libpq5 && rm -rf /var/lib/apt/lists/*

# শুধুমাত্র ভার্চুয়াল এনভায়রনমেন্ট কপি করো (পুরো সোর্স কোড নয়)
COPY --from=builder /app/.venv /app/.venv
COPY backend/ .
# বাংলা মন্তব্য: রুট-লেভেল 'skills' ডিরেক্টরি কপি করা হচ্ছে যাতে
# core/evolution/auto_skill_creator.py সঠিকভাবে 'skills.installer' ইম্পোর্ট করতে পারে।
COPY skills/ ./skills/

ENV PATH="/app/.venv/bin:$PATH"
EXPOSE 8000

# CRITICAL FIX (Cloud Run Port Binding):
# Always use shell form for CMD (e.g., `CMD uvicorn ...`) instead of JSON array (`CMD ["uvicorn", ...]`).
# The shell form allows Cloud Run to dynamically inject the $PORT environment variable.
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8080}"]
