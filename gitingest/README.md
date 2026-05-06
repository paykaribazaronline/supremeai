# Gitingest

Convert Git repositories into LLM-friendly text digests.

## Overview

Gitingest is a tool that transforms any Git repository into a clean, structured text format that's perfect for feeding into Large Language Models (LLMs). Simply paste a GitHub URL and get back a text file with the repo's directory tree and all file contents concatenated.

## Features

- **Multiple Interfaces**: Use as a FastAPI web app, pip-installable CLI, or importable Python package
- **Smart Cloning**: Async clone with shallow depth and sparse checkout for subpaths
- **Intelligent Filtering**: Default ignore patterns for Python, JS, Java, and more, plus .gitignore and .gitingestignore support
- **Encoding Support**: Multi-encoding fallback (UTF-8 → UTF-16 → Latin-1)
- **Jupyter Support**: Extracts code cells from Jupyter notebooks
- **Caching**: S3/MinIO caching with local filesystem fallback
- **Rate Limiting**: 10 requests per minute per IP using slowapi
- **Prometheus Metrics**: Monitor usage on `/metrics` endpoint
- **Beautiful UI**: Warm, playful interface with Tailwind CSS

## Installation

```bash
pip install gitingest
```

Or install from source:

```bash
git clone https://github.com/supremeai/gitingest.git
cd gitingest
pip install -e ".[all]"
```

## Usage

### CLI

```bash
# Current directory to digest.txt
gitingest

# Remote repository
gitingest https://github.com/fastapi/fastapi

# Output to custom file
gitingest https://github.com/user/repo -o custom.txt

# Output to stdout
gitingest https://github.com/user/repo -o -

# With include patterns
gitingest https://github.com/user/repo -i "*.py" -i "*.md"

# With exclude patterns
gitingest https://github.com/user/repo -e "tests/*" -e "docs/*"

# With GitHub PAT for private repos
gitingest https://github.com/user/repo -t ghp_yourtoken

# Max file size (in KB)
gitingest https://github.com/user/repo --max-file-size 500
```

### Python API

```python
from gitingest import ingest, ingest_async
from gitingest.models import IngestRequest

# Synchronous
request = IngestRequest(
    url="https://github.com/fastapi/fastapi",
    pattern_type="exclude",
    patterns="*.md,docs/*",
    max_file_size_kb=100,
)
result = ingest(request)
print(result.summary)
print(result.tree)
print(result.content)

# Asynchronous
import asyncio
result = asyncio.run(ingest_async(request))
```

### Web API

```bash
# Start the server
python -m gitingest.api.main
# Or with uvicorn
uvicorn gitingest.api.main:app --reload
```

Then make requests:

```bash
# POST /api/ingest
curl -X POST http://localhost:8000/api/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://github.com/fastapi/fastapi",
    "pattern_type": "exclude",
    "patterns": "*.md,docs/*"
  }'

# GET /api/{user}/{repository}
curl http://localhost:8000/api/fastapi/fastapi?branch=main

# Web UI
open http://localhost:8000
```

## Configuration

Create a `.env` file:

```bash
# Server
HOST=0.0.0.0
PORT=8000
ALLOWED_HOSTS=["*"]

# GitHub
GITHUB_TOKEN=ghp_yourtoken

# S3/MinIO Cache (optional)
S3_ENABLED=false
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_BUCKET_NAME=gitingest-cache
S3_USE_SSL=false

# Logging
LOG_FORMAT=json  # json or text
LOG_LEVEL=INFO
SENTRY_DSN=https://...
POSTHOG_API_KEY=phc_...

# Limits
MAX_FILE_SIZE=10485760  # 10 MB
MAX_DIR_DEPTH=20
MAX_FILES=10000
MAX_TOTAL_SIZE=524288000  # 500 MB
CLONE_TIMEOUT=60  # seconds

# Rate Limiting
RATE_LIMIT_PER_MINUTE=10

# Prometheus
METRICS_PORT=9090
```

## Limits

- **Per file**: 10 MB
- **Directory depth**: 20 levels
- **Total files**: 10,000
- **Total size**: 500 MB
- **Clone timeout**: 60 seconds
- **Rate limit**: 10 requests/minute per IP

## API Response Format

```json
{
  "summary": "Repository: user/repo\nBranch: main\nFiles analyzed: 150\n...",
  "tree": "repo/\n  README.md\n  src/\n    main.py\n...",
  "content": "====...\nFile: README.md\n====...\n...",
  "file_count": 150,
  "estimated_tokens": 15000,
  "repo_name": "user/repo",
  "branch": "main",
  "cached": false
}
```

## Development

```bash
# Install dev dependencies
pip install -e ".[s3,logging,sentry,posthog]"

# Run with auto-reload
uvicorn gitingest.api.main:app --reload --log-level debug

# Run tests
pytest tests/
```

## License

MIT License
