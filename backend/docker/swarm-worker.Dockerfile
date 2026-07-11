FROM python:3.11-slim

WORKDIR /app

# Upgrade pip and install minimal edge worker dependencies
RUN pip install --upgrade pip && \
    pip install --no-cache-dir nats-py pydantic litellm langchain

# We assume the user maps or copies the core messaging and worker scripts here
COPY core/nats_messaging.py ./core/
COPY engine/worker_node.py ./engine/

# Set Python path so imports work correctly
ENV PYTHONPATH="/app"

# NATS auth token can be passed as an env variable at runtime
ENV NATS_TOKEN="super_secret_token"
ENV NATS_URL="nats://host.docker.internal:4222"

CMD ["python", "engine/worker_node.py"]
