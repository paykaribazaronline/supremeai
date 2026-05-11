FROM ollama/ollama:latest

# Set Ollama host
ENV OLLAMA_HOST=0.0.0.0:8080

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Pull the model during build time
ARG MODEL_NAME
RUN (ollama serve &) && \
    until curl -s localhost:11434 > /dev/null; do sleep 1; done && \
    ollama pull $MODEL_NAME && \
    pkill ollama

# Expose port
EXPOSE 8080

# Start Ollama
ENTRYPOINT ["ollama", "serve"]
