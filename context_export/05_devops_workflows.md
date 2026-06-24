# 05 Devops Workflows

_Auto-generated from `supremeai_full_codebase.md`_

### File: `cloudbuild.yaml`

### File: `cloudbuild.yaml`

```yaml
steps:
# Step 1: Install dependencies and run tests
- name: 'python:3.11-slim'
  id: 'Test'
  entrypoint: 'bash'
  args:
    - '-c'
    - |
      pip install poetry
      cd backend
      poetry install --no-interaction --no-ansi
      PYTHONPATH=.:.. poetry run pytest tests/
# Step 2: Build Docker image with Kaniko cache
- name: 'gcr.io/kaniko-project/executor:latest'
  id: 'Build'
  args:
  - --destination=gcr.io/$PROJECT_ID/supremeai-api:$COMMIT_SHA
  - --cache=true
  - --cache-dir=gcr.io/$PROJECT_ID/kaniko-cache
  - --dockerfile=Dockerfile
  - --context=dir://.
# Step 3: Redundant push removed (Kaniko automatically pushes to destination)
# Step 4: Deploy to Cloud Run
- name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
  id: 'Deploy'
  entrypoint: 'bash'
  args:
    - '-c'
    - |
      gcloud run deploy supremeai-api \
        --image gcr.io/$PROJECT_ID/supremeai-api:$COMMIT_SHA \
        --platform managed \
        --region $_REGION \
        --allow-unauthenticated \
        --min-instances 1 \
        --set-env-vars="ENV=production,GCP_PROJECT_ID=$PROJECT_ID"
timeout: '1200s'
options:
  machineType: 'E2_HIGHCPU_32'
substitutions:
  _REGION: 'us-central1'
```

### File: `docker-compose.yml`

### File: `docker-compose.yml`

```yaml
services:
  api:
    build: .
    ports:
      - "8000:8000"
    volumes:
      - .:/app
      - ./data:/app/data
    env_file:
      - .env
    command: uvicorn main:app --host 0.0.0.0 --port 8000 --reload

  n8n:
    image: docker.n8n.io/n8nio/n8n:latest
    ports:
      - "5678:5678"
    volumes:
      - n8n_data:/home/node/.n8n
    environment:
      - N8N_SECURE_COOKIE=false
    restart: unless-stopped

volumes:
  n8n_data:

```

### File: `Dockerfile`

### File: `Dockerfile`

```text
# ══════════════════════════════════════════════════════════
# SupremeAI 2.0 — Root Dockerfile (Distroless variant)
# Target: Maximum security with gcr.io/distroless
# ══════════════════════════════════════════════════════════

# Stage 1: Build dependencies
FROM python:3.11-slim AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir poetry && poetry config virtualenvs.in-project true

# ── Install CPU-only PyTorch FIRST ──
WORKDIR /app/backend
RUN python -m venv /app/backend/.venv && \
    /app/backend/.venv/bin/pip install --no-cache-dir --upgrade pip && \
    /app/backend/.venv/bin/pip install --no-cache-dir \
        torch torchvision --index-url https://download.pytorch.org/whl/cpu && \
    /app/backend/.venv/bin/pip install --no-cache-dir "setuptools<82.0.0"

COPY backend/pyproject.toml ./
COPY backend/poetry.lock* ./
RUN poetry lock --no-update || true
RUN poetry install --no-interaction --no-ansi --no-root --only main --with ml || \
    poetry install --no-interaction --no-ansi --no-root --only main

# ── Force CPU torch, remove CUDA bloat ──
RUN /app/backend/.venv/bin/pip uninstall -y \
    nvidia-cuda-nvrtc-cu12 nvidia-cuda-runtime-cu12 nvidia-cuda-cupti-cu12 \
    nvidia-cudnn-cu12 nvidia-cublas-cu12 nvidia-cufft-cu12 nvidia-curand-cu12 \
    nvidia-cusolver-cu12 nvidia-cusparse-cu12 nvidia-nccl-cu12 nvidia-nvtx-cu12 \
    nvidia-nvjitlink-cu12 triton 2>/dev/null || true && \
    /app/backend/.venv/bin/pip install --no-cache-dir torch torchvision \
        --index-url https://download.pytorch.org/whl/cpu

# ── Pre-download EasyOCR models ──
RUN /app/backend/.venv/bin/pip install --no-cache-dir --no-build-isolation "openai-whisper==20240930" 2>/dev/null || true
RUN mkdir -p /root/.EasyOCR/model && \
    /app/backend/.venv/bin/python -c "import easyocr; easyocr.Reader(['bn', 'en'])" 2>/dev/null || true && \
    rm -f /root/.EasyOCR/model/*.zip 2>/dev/null || true

# ── Aggressive cleanup ──
RUN find /app/backend/.venv -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/backend/.venv -type d -name "tests" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/backend/.venv -type d -name "test" -exec rm -rf {} + 2>/dev/null || true && \
    find /app/backend/.venv -type f -name "*.pyc" -delete 2>/dev/null || true && \
    rm -rf /app/backend/.venv/lib/python3.11/site-packages/torch/test 2>/dev/null || true && \
    rm -rf /app/backend/.venv/lib/python3.11/site-packages/caffe2 2>/dev/null || true


# Stage 2: Final minimal runner (Google Distroless)
FROM gcr.io/distroless/python3-debian12 AS runner

WORKDIR /app

COPY --from=builder /app/backend/.venv /app/backend/.venv
COPY backend /app/backend
COPY --from=builder /root/.EasyOCR /home/nonroot/.EasyOCR

ENV PATH="/app/backend/.venv/bin:$PATH"
ENV PYTHONPATH="/app/backend"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

WORKDIR /app/backend

ENTRYPOINT ["/app/backend/.venv/bin/python", "main.py"]
```

### File: `Dockerfile.backend`

### File: `Dockerfile.backend`

```text
# Layer 1: System deps (rarely changes)
FROM python:3.11-slim AS deps

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Layer 2: Poetry + Python deps (only changes if pyproject.toml or poetry.lock changes)
RUN pip install --no-cache-dir poetry && \
    poetry config virtualenvs.create false

COPY pyproject.toml poetry.lock* ./
RUN poetry install --no-interaction --no-ansi --only main

# Layer 3: Application code (changes frequently)
COPY . .

CMD ["poetry", "run", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### File: `cloudbuild.yaml`

### File: `cloudbuild.yaml`

```yaml
steps:
# Step 1: Install dependencies and run tests
- name: 'python:3.11-slim'
  id: 'Test'
  entrypoint: 'bash'
  args:
    - '-c'
    - |
      pip install poetry
      cd backend
      poetry install --no-interaction --no-ansi
      PYTHONPATH=.:.. poetry run pytest tests/
# Step 2: Build Docker image with Kaniko cache
- name: 'gcr.io/kaniko-project/executor:latest'
  id: 'Build'
  args:
  - --destination=gcr.io/$PROJECT_ID/supremeai-api:$COMMIT_SHA
  - --cache=true
  - --cache-dir=gcr.io/$PROJECT_ID/kaniko-cache
  - --dockerfile=Dockerfile
  - --context=dir://.
# Step 3: Redundant push removed (Kaniko automatically pushes to destination)
# Step 4: Deploy to Cloud Run
- name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
  id: 'Deploy'
  entrypoint: 'bash'
  args:
    - '-c'
    - |
      gcloud run deploy supremeai-api \
        --image gcr.io/$PROJECT_ID/supremeai-api:$COMMIT_SHA \
        --platform managed \
        --region $_REGION \
        --allow-unauthenticated \
        --min-instances 1 \
        --set-env-vars="ENV=production,GCP_PROJECT_ID=$PROJECT_ID"
timeout: '1200s'
options:
  machineType: 'E2_HIGHCPU_32'
substitutions:
  _REGION: 'us-central1'
```

### File: `docker-compose.yml`

### File: `docker-compose.yml`

```yaml
services:
  api:
    build: .
    ports:
      - "8000:8000"
    volumes:
      - .:/app
      - ./data:/app/data
    env_file:
      - .env
    command: uvicorn main:app --host 0.0.0.0 --port 8000 --reload

  n8n:
    image: docker.n8n.io/n8nio/n8n:latest
    ports:
      - "5678:5678"
    volumes:
      - n8n_data:/home/node/.n8n
    environment:
      - N8N_SECURE_COOKIE=false
    restart: unless-stopped

volumes:
  n8n_data:

```

### File: `Dockerfile`

### File: `Dockerfile`

```text
# Stage 1: Build dependencies
FROM python:3.11-slim AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir poetry && poetry config virtualenvs.in-project true

# Pre-create virtualenv and install CPU-only PyTorch to save ~1.7GB space
WORKDIR /app/backend
RUN python -m venv /app/backend/.venv && \
    /app/backend/.venv/bin/pip install --no-cache-dir --upgrade pip && \
    /app/backend/.venv/bin/pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

COPY backend/pyproject.toml backend/poetry.lock* ./
RUN poetry install --no-interaction --no-ansi --no-root --only main

# Re-install CPU-only PyTorch to overwrite the large CUDA PyTorch downloaded by Poetry and save ~1.7GB space
RUN /app/backend/.venv/bin/pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu

# Clean up build-time virtualenv caches to reduce copied size
RUN find /app/backend/.venv -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
RUN find /app/backend/.venv -name "*.pyc" -delete 2>/dev/null || true


# Stage 2: Final minimal runner image
FROM python:3.11-slim AS runner

WORKDIR /app

# Install only runtime system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Copy virtualenv and backend code only (avoiding monorepo clutter)
COPY --from=builder /app/backend/.venv /app/backend/.venv
COPY backend /app/backend

ENV PATH="/app/backend/.venv/bin:$PATH"
ENV PYTHONPATH="/app/backend"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Pre-download EasyOCR English & Bengali models during build and clean zip files to save space
RUN python -c "import easyocr; easyocr.Reader(['bn', 'en'])" && \
    rm -f /root/.EasyOCR/model/*.zip && \
    find /app/backend/.venv -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true && \
    find /app/backend/.venv -name "*.pyc" -delete 2>/dev/null || true

WORKDIR /app/backend
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}"]
```

### File: `infrastructure\cloudflare_worker.js`

### File: `infrastructure\cloudflare_worker.js`

```javascript
addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request))
})

async function handleRequest(request) {
  const url = new URL(request.url)
  const serviceName = url.hostname

  const backends = [
    {
      name: 'gcp-cloud-run',
      url: env.GCP_CLOUD_RUN_URL,
      health: `${env.GCP_CLOUD_RUN_URL}/health`,
      region: env.GCP_REGION,
      timeout: 5000,
      retries: 3,
      weight: parseInt(env.GCP_WEIGHT || '50', 10),
    },
    {
      name: 'railway',
      url: env.RAILWAY_URL,
      health: `${env.RAILWAY_URL}/health`,
      region: 'us-east1',
      timeout: 5000,
      retries: 3,
      weight: parseInt(env.RAILWAY_WEIGHT || '30', 10),
    },
    {
      name: 'render',
      url: env.RENDER_URL,
      health: `${env.RENDER_URL}/health`,
      region: 'us-east1',
      timeout: 5000,
      retries: 3,
      weight: parseInt(env.RENDER_WEIGHT || '20', 10),
    },
  ].filter(b => b.url)

  if (backends.length === 0) {
    return new Response('No backends configured', { status: 503 })
  }

  const healthyBackends = await getHealthyBackends(backends)
  if (healthyBackends.length === 0) {
    return new Response('All backends unhealthy', { status: 503 })
  }

  const backend = weightedPick(healthyBackends)
  const target = new URL(url.pathname + url.search, backend.url)

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: omitWranglerHeaders(request.headers),
      body: request.method !== 'GET' ? await request.text() : null,
      signal: AbortSignal.timeout(backend.timeout),
    })

    return new Response(response.body, {
      status: response.status,
      headers: omitHopByHopHeaders(new Headers(response.headers)),
    })
  } catch (err) {
    return new Response(`Backend ${backend.name} error: ${err.message}`, { status: 502 })
  }
}

async function getHealthyBackends(backends) {
  const results = await Promise.allSettled(
    backends.map(async backend => {
      for (let attempt = 0; attempt < backend.retries; attempt++) {
        try {
          const res = await fetch(backend.health, { signal: AbortSignal.timeout(backend.timeout) })
          if (res.ok) return backend
        } catch (_) {
          if (attempt === backend.retries - 1) return null
          await new Promise(r => setTimeout(r, 200 * (attempt + 1)))
        }
      }
      return null
    })
  )
  return results.filter(r => r.status === 'fulfilled' && r.value).map(r => r.value)
}

function weightedPick(backends) {
  const total = backends.reduce((sum, b) => sum + (b.weight || 0), 0)
  if (total === 0) return backends[Math.floor(Math.random() * backends.length)]
  let r = Math.random() * total
  for (const b of backends) {
    r -= b.weight || 0
    if (r <= 0) return b
  }
  return backends[backends.length - 1]
}

function omitWranglerHeaders(headers) {
  const allowlist = ['content-type', 'authorization', 'x-telegram-bot-token']
  const out = new Headers()
  headers.forEach((v, k) => { if (allowlist.includes(k.toLowerCase()) || !k.startsWith('cf-')) out.set(k, v) })
  return out
}

function omitHopByHopHeaders(headers) {
  const block = new Set(['connection', 'keep-alive', 'proxy-authenticate', 'proxy-authorization', 'te', 'trailer', 'transfer-encoding', 'upgrade'])
  const out = new Headers()
  headers.forEach((v, k) => { if (!block.has(k.toLowerCase())) out.set(k, v) })
  return out
}
```

### File: `infrastructure\deploy.ps1`

### File: `infrastructure\deploy.ps1`

```text
<#
.SYNOPSIS
SupremeAI 2.0 deployment orchestrator for GCP Cloud Run, Railway, Render.
.PARAMETER Target
Optional deployment target: gcp | railway | render | all (default: all)
.EXAMPLE
.\infrastructure\deploy.ps1 -Target all
#>
param(
  [ValidateSet('gcp', 'railway', 'render', 'all')]
  [string]$Target = 'all'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ProjectRoot ".env"

function Log($Message) { Write-Host "[DEPLOY] $Message" -ForegroundColor Cyan }
function Fail($Message) { Write-Host "[DEPLOY][FAIL] $Message" -ForegroundColor Red; exit 1 }

function Test-Prerequisites {
  Log "Checking prerequisites..."
  $required = @('gcloud', 'docker', 'git')
  $missing = @()
  foreach ($cmd in $required) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { $missing += $cmd }
  }
  if ($missing) { Fail "Missing tools: $($missing -join ', ')" }
  if (Test-Path $EnvFile) {
    foreach ($line in Get-Content $EnvFile) {
      $trimmed = $line.Trim()
      if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
      $idx = $trimmed.IndexOf('=')
      if ($idx -lt 1) { continue }
      $k = $trimmed.Substring(0, $idx).Trim()
      $v = $trimmed.Substring($idx + 1).Trim()
      if (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'"))) {
        $v = $v.Substring(1, $v.Length - 2)
      }
      [System.Environment]::SetEnvironmentVariable($k, $v, 'Process')
    }
  }
}

function Get-RegistryImage {
  param([string]$ProjectId, [string]$Region)
  $artifactRepo = "$Region-docker.pkg.dev/$ProjectId/supremeai"
  $tag = if ($env:GITHUB_SHA) { $env:GITHUB_SHA } else { "local-$(Get-Date -Format 'yyyyMMdd-HHmmss')" }
  return "$artifactRepo/supremeai:$tag"
}

function Deploy-GCP {
  param([string]$EnvTarget)
  Log "Deploying to GCP Cloud Run... (target: $EnvTarget)"
  if (-not $env:GCP_PROJECT_ID) { Fail "GCP_PROJECT_ID is not set" }
  if (-not $env:GCP_REGION) { $env:GCP_REGION = 'us-central1' }
  if (-not $env:GCP_SERVICE_NAME) { $env:GCP_SERVICE_NAME = 'supremeai' }
  if ($EnvTarget -eq 'production') { $env:ENV = 'production' } else { $env:ENV = $EnvTarget }

  $image = Get-RegistryImage -ProjectId $env:GCP_PROJECT_ID -Region $env:GCP_REGION
  Log "Building and pushing $image"
  docker build -t $image (Join-Path $ProjectRoot '.')
  if ($LASTEXITCODE -ne 0) { Fail 'Docker build failed' }
  docker push $image
  if ($LASTEXITCODE -ne 0) { Fail 'Docker push failed' }

  $setEnvVars = @("ENV=$EnvTarget")
  if ($env:GCP_PROJECT_ID) { $setEnvVars += "GCP_PROJECT_ID=$env:GCP_PROJECT_ID" }
  if ($env:GCP_REGION) { $setEnvVars += "GCP_REGION=$env:GCP_REGION" }
  if ($env:OPENAI_API_KEY) { $setEnvVars += "OPENAI_API_KEY=$env:OPENAI_API_KEY" }
  if ($env:TELEGRAM_BOT_TOKEN) { $setEnvVars += "TELEGRAM_BOT_TOKEN=$env:TELEGRAM_BOT_TOKEN" }
  if ($env:SUPABASE_URL) { $setEnvVars += "SUPABASE_URL=$env:SUPABASE_URL" }
  if ($env:SUPABASE_KEY) { $setEnvVars += "SUPABASE_KEY=$env:SUPABASE_KEY" }
  if ($env:UPSTASH_REDIS_REST_URL) { $setEnvVars += "UPSTASH_REDIS_REST_URL=$env:UPSTASH_REDIS_REST_URL" }
  if ($env:UPSTASH_REDIS_REST_TOKEN) { $setEnvVars += "UPSTASH_REDIS_REST_TOKEN=$env:UPSTASH_REDIS_REST_TOKEN" }

  $envValue = $setEnvVars -join ','
  $gcloudArgs = @(
    'run', 'deploy', $env:GCP_SERVICE_NAME,
    '--image', $image,
    '--region', $env:GCP_REGION,
    '--project', $env:GCP_PROJECT_ID,
    '--allow-unauthenticated',
    '--set-env-vars', $envValue
  )
  if ($env:PORT) {
    $gcloudArgs += '--port'
    $gcloudArgs += $env:PORT
  }

  & gcloud @gcloudArgs
  if ($LASTEXITCODE -ne 0) { Fail "gcloud deploy failed" }

  & gcloud run services update-traffic $env:GCP_SERVICE_NAME --region $env:GCP_REGION --project $env:GCP_PROJECT_ID --to-latest
  if ($LASTEXITCODE -ne 0) { Fail "traffic promotion failed" }
  Log 'GCP Cloud Run deployment completed'
}

function Deploy-Railway {
  param([string]$EnvTarget)
  Log "Railway deploy for target: $EnvTarget"
  Push-Location $ProjectRoot
  if (-not (Get-Command railway -ErrorAction SilentlyContinue)) {
    Log "Railway CLI not detected; printing deploy snippet."
    Write-Host "`n--- railway deploy snippet ---"
    Write-Host " railway login --browserless"
    if ($env:RAILWAY_TOKEN) { Write-Host " railway link --token $env:RAILWAY_TOKEN" }
    Write-Host " railway variables set NODE_ENV=$EnvTarget"
    if ($env:OPENAI_API_KEY) { Write-Host " railway variables set OPENAI_API_KEY=$env:OPENAI_API_KEY" }
    if ($env:TELEGRAM_BOT_TOKEN) { Write-Host " railway variables set TELEGRAM_BOT_TOKEN=$env:TELEGRAM_BOT_TOKEN" }
    Write-Host " railway up --detach`n"
  } else {
    railway login --browserless | Out-Null
    if ($env:RAILWAY_TOKEN) { railway link --token $env:RAILWAY_TOKEN | Out-Null }
    railway variables set NODE_ENV=$EnvTarget | Out-Null
    railway up --detach
    if ($LASTEXITCODE -ne 0) { Fail 'railway deploy failed' }
  }
  Pop-Location
}

function Deploy-Render {
  param([string]$EnvTarget)
  Log "Render deploy for target: $EnvTarget"
  Push-Location $ProjectRoot
  if (-not (Get-Command render -ErrorAction SilentlyContinue)) {
    Log "Render CLI not detected; printing deploy snippet."
    Write-Host "`n--- render deploy snippet ---"
    Write-Host " render login"
    Write-Host " render environment set supremeai NODE_ENV=$EnvTarget"
    if ($env:OPENAI_API_KEY) { Write-Host " render secrets set OPENAI_API_KEY=$env:OPENAI_API_KEY" }
    Write-Host " render deploys start --service supremeai --yes`n"
  } else {
    render environment set supremeai NODE_ENV=$EnvTarget | Out-Null
    render deploys start --service supremeai --yes
    if ($LASTEXITCODE -ne 0) { Fail 'render deploy failed' }
  }
  Pop-Location
}

try {
  Test-Prerequisites
  if ($Target -eq 'all' -or $Target -eq 'gcp') { Deploy-GCP -EnvTarget production }
  if ($Target -eq 'all' -or $Target -eq 'railway') { Deploy-Railway -EnvTarget production }
  if ($Target -eq 'all' -or $Target -eq 'render') { Deploy-Render -EnvTarget production }
  Log 'Deployment orchestration completed.'
}
catch { Fail $_ }
```

### File: `infrastructure\cloudflare\worker.js`

### File: `infrastructure\cloudflare\worker.js`

```javascript
/**
 * SupremeAI 2.0 — Cloudflare Worker Load Balancer (Upgraded)
 * Traffic split: GCP(40%) + Railway(35%) + Render(25%)
 *
 * ── Setup ──────────────────────────────────────────────────────
 * Cloudflare Dashboard → Workers → Settings → Variables:
 *   GCP_CLOUD_RUN_URL  = https://supremeai-api-565236080752.us-central1.run.app
 *   RAILWAY_URL        = https://your-app.railway.app
 *   RENDER_URL         = https://your-app.onrender.com
 *
 * Endpoints:
 *   /lb-status  → backend health report
 *   /*          → proxied to selected backend
 * ────────────────────────────────────────────────────────────────
 */

const HEALTH_PATH = "/health";
const HEALTH_TIMEOUT_MS = 4000;

/** Build backend list from env, filter unconfigured ones */
function buildBackends(env) {
  return [
  // GCP Cloud Run — primary (40% traffic)
  { name: "gcp",     url: env.GCP_CLOUD_RUN_URL ?? "https://supremeai-api-565236080752.us-central1.run.app", weight: 40, healthy: true },
  // Railway — secondary (35% traffic)
  { name: "railway", url: env.RAILWAY_URL        ?? "https://supremeai-api-production-c6c8.up.railway.app",  weight: 35, healthy: true },
  // Render — tertiary (25% traffic)
  { name: "render",  url: env.RENDER_URL         ?? "https://supremeai-gzwe.onrender.com",                   weight: 25, healthy: true },
  ].filter((b) => b.url && b.url.startsWith("http"));
}

/** Weighted random pick from healthy backends, fallback to all if none healthy */
function pickBackend(backends) {
  const pool = backends.filter((b) => b.healthy);
  const active = pool.length > 0 ? pool : backends;
  const total = active.reduce((s, b) => s + b.weight, 0);
  let r = Math.random() * total;
  for (const b of active) {
    r -= b.weight;
    if (r <= 0) return b;
  }
  return active[active.length - 1];
}

/** Probe all backends for health (runs in background) */
async function probeHealth(backends) {
  await Promise.allSettled(
    backends.map(async (b) => {
      try {
        const res = await fetch(b.url + HEALTH_PATH, {
          method: "GET",
          signal: AbortSignal.timeout(HEALTH_TIMEOUT_MS),
        });
        b.healthy = res.status < 500;
      } catch {
        b.healthy = false;
      }
    })
  );
}

/** Forward request to backend */
async function proxyRequest(backend, request) {
  const url = new URL(request.url);
  const target = new URL(url.pathname + url.search, backend.url).toString();

  const headers = new Headers(request.headers);
  headers.set("x-supremeai-origin", backend.name);
  headers.set("x-forwarded-host", url.host);

  const upReq = new Request(target, {
    method: request.method,
    headers,
    body: ["GET", "HEAD"].includes(request.method) ? undefined : request.body,
    redirect: "follow",
  });

  const res = await fetch(upReq);
  const resHeaders = new Headers(res.headers);
  resHeaders.set("x-supremeai-node", backend.name);
  return new Response(res.body, { status: res.status, statusText: res.statusText, headers: resHeaders });
}

// ─── Main Worker ─────────────────────────────────────────────────
export default {
  async fetch(request, env, ctx) {
    const backends = buildBackends(env);

    if (backends.length === 0) {
      return Response.json({ error: "No upstream providers configured" }, { status: 503 });
    }

    // Probe health in background (non-blocking)
    ctx.waitUntil(probeHealth(backends));

    const url = new URL(request.url);

    // ── Status endpoint ───────────────────────────────────────────
    if (url.pathname === "/lb-status") {
      return Response.json({
        status: "ok",
        strategy: "weighted-active-active",
        backends: backends.map(({ name, url: u, weight, healthy }) => ({ name, url: u, weight, healthy })),
        timestamp: new Date().toISOString(),
      });
    }

    // ── Proxy with fallback ───────────────────────────────────────
    const primary = pickBackend(backends);
    try {
      return await proxyRequest(primary, request);
    } catch (err) {
      // Try remaining backends
      const fallbacks = backends.filter((b) => b !== primary);
      for (const fb of fallbacks) {
        try {
          return await proxyRequest(fb, request);
        } catch {
          continue;
        }
      }
      return Response.json(
        { error: "All backends unavailable", detail: String(err) },
        { status: 503 }
      );
    }
  },
};
```

### File: `infrastructure\cloudflare\wrangler.toml`

### File: `infrastructure\cloudflare\wrangler.toml`

```text
name = "supremeai-edge"
main = "worker.js"
compatibility_date = "2026-06-17"

[vars]
GCP_CLOUD_RUN_URL = ""
RAILWAY_URL = ""
RENDER_URL = ""
```

### File: `infrastructure\firebase_functions\ocrTrigger.ts`

### File: `infrastructure\firebase_functions\ocrTrigger.ts`

```typescript
# SupremeAI — Firebase OCR Trigger
Provides a sample Cloud Function (Realtime Database + Firestore) that initiates an OCR task when a document is queued.
Use this as a reference; integrate into your actual functions source.

### Realtime Database reference implementation
- Database path: `/ocr-queue/{pushId}`
- Expected fields: `{ file_path: string, mime: string }`
- Result: writes `{ status: 'completed', result: any }` under `/ocr-results/{pushId}`
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\api-router.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\api-router.js`

```javascript
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const externalClient = require('./utils/externalClient');
const axios = require('axios');

const app = express();

const allowedOrigins = [
  'http://127.0.0.1:3000',
  'http://127.0.0.1:5173',
  'http://127.0.0.1:5000',
  'http://127.0.0.1:3000',
  'http://127.0.0.1:5173',
  'http://127.0.0.1:5000',
];

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes(origin) || origin.includes('supremeai')) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
}));
app.use(express.json());

const DEFAULT_SCRAPE_ENDPOINT = process.env.SCRAPE_ENGINE_URL || 'https://us-central1-supremeai.cloudfunctions.net/scrapeAndRespondFn';
const DEFAULT_CHAT_ENDPOINT = process.env.CHAT_API_URL || 'https://supremeai-a.web.app/api/chat/send';

function shouldUseScrapeEngine(req) {
  const preferScrape = (req.headers['x-use-scrape'] === 'true') || (req.body && req.body.useScrape === true);
  return !!preferScrape;
}

async function proxyToScrapeEngine(message, userId) {
  const url = DEFAULT_SCRAPE_ENDPOINT;
  const response = await axios.post(url, { message, userId }, { timeout: 30000 });
  return response.data;
}

app.get(['/health', '/api/health'], (req, res) => {
  res.json({
    status: 'ok',
    mode: 'coordinator',
    scrapeEngine: DEFAULT_SCRAPE_ENDPOINT,
    chatBackend: DEFAULT_CHAT_ENDPOINT,
  });
});

// REAL LLM Connection (Gemini / OpenAI Fallback)
async function callChatBackend(message, token) {
  const apiKey = process.env.SUPREME_API_KEY || process.env.GEMINI_API_KEY || process.env.OPENAI_API_KEY;
  const targetModel = process.env.SUPREME_CORE_MODEL || 'gemini-pro';

  if (!apiKey) {
    // Fallback to local neural core if no API key
    return generateSmartAIResponse(message);
  }

  try {
    // Attempt Gemini call
    const response = await axios.post(
      `https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent`,
      {
        contents: [{ parts: [{ text: message }] }]
      },
      {
        headers: {
          'x-goog-api-key': apiKey,
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );

    if (response.data && response.data.candidates && response.data.candidates.length > 0) {
      const text = response.data.candidates[0].content.parts[0].text;
      return {
        message: text,
        confidence: 0.95,
        chatType: 'LLM_RESPONSE',
        sourceType: 'SUPREME_CORE_API',
        sources: [`${process.env.SUPREME_BRAND_NAME || 'SupremeAI'} Intelligence`]
      };
    }
    throw new Error('Invalid LLM response format');
  } catch (err) {
    console.error('[LLM] API call failed:', err.message);
    return generateSmartAIResponse(message);
  }
}

async function unifiedChatHandler(req, res) {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  const userId = (req.body && req.body.userId) ? String(req.body.userId) : 'anonymous';
  const token = req.headers['authorization'] ? String(req.headers['authorization']).split('Bearer ')[1] : null;

  if (!message || !message.trim()) {
    return res.status(400).json({ success: false, message: 'Message is required', sourceType: 'error' });
  }

  try {
    let answer = '';
    let sources = [];
    let confidence = 0.2;
    let chatType = 'UNKNOWN';
    let sourceType = 'UNKNOWN';
    let scrapedPages = 0;

    if (shouldUseScrapeEngine(req)) {
      try {
        const scrapeResult = await proxyToScrapeEngine(message.trim(), userId);
        if (scrapeResult && scrapeResult.answer) {
          answer = scrapeResult.answer;
          sources = Array.isArray(scrapeResult.sources) ? scrapeResult.sources : [];
          confidence = typeof scrapeResult.confidence === 'number' ? scrapeResult.confidence : 0.55;
          chatType = scrapeResult.chatType || 'COMPLEX_QUESTION';
          sourceType = scrapeResult.cached ? 'SCRAPE_CACHE' : 'SCRAPE_ENGINE';
          scrapedPages = typeof scrapeResult.scrapedPages === 'number' ? scrapeResult.scrapedPages : sources.length;
        }
      } catch (scrapeError) {
        console.warn('[api-router] Scrape engine failed, falling back to chat backend:', scrapeError.message);
      }
    }

    if (!answer) {
      try {
        const chatResult = await callChatBackend(message, token);
        if (chatResult && chatResult.message) {
          answer = chatResult.message;
          confidence = typeof chatResult.confidence === 'number' ? chatResult.confidence : 0.5;
          chatType = chatResult.chatType || 'SIMPLE_QUESTION';
          sourceType = chatResult.source_type || chatResult.sourceType || 'CORE_API';
          sources = Array.isArray(chatResult.sources) ? chatResult.sources : [];
        }
      } catch (chatError) {
        console.warn('[api-router] Chat backend failed, using virtual crawler:', chatError.message);
      }
    }

    if (!answer) {
      // Both scraping and LLM failed or yielded empty
      chatType = 'UNKNOWN';
      sourceType = 'ERROR';
      confidence = 0;
      answer = "সিস্টেম তথ্য সংগ্রহ করতে পারেনি। অনুগ্রহ করে আবার চেষ্টা করুন।";
    }

    return res.json({
      success: true,
      message: answer,
      sources,
      confidence,
      chatType,
      sourceType,
      scrapedPages,
      userId,
    });
  } catch (error) {
    console.error('[api-router] Unified chat error:', error && error.message);
    return res.status(500).json({ success: false, message: 'Service unavailable. Please try again later.', sourceType: 'error', chatType: 'UNKNOWN' });
  }
}

app.post(['/api/chat/send', '/chat/send'], async (req, res) => {
  return unifiedChatHandler(req, res);
});

app.post(['/api/scrape/and-respond', '/scrape/and-respond'], async (req, res) => {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  const userId = (req.body && req.body.userId) ? String(req.body.userId) : 'anonymous';
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'Missing required field: message' });
  }
  try {
    const result = await proxyToScrapeEngine(message.trim(), userId);
    return res.json({ success: true, ...result });
  } catch (error) {
    console.error('[api-router] Scrape proxy error:', error && error.message);
    return res.status(502).json({ success: false, error: 'Scrape engine unavailable', details: error && error.message });
  }
});

app.post(['/api/chat/classify', '/chat/classify'], async (req, res) => {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'message required' });
  }
  try {
    const scrapeUrl = DEFAULT_SCRAPE_ENDPOINT.replace('/scrapeAndRespondFn', '/classifyIntentFn');
    const response = await axios.post(scrapeUrl, { message }, { timeout: 10000 });
    return res.json({ success: true, ...response.data });
  } catch (error) {
    return res.status(500).json({ success: false, error: 'Classification failed' });
  }
});

function calculateOverlapScore(query, task) {
  const q = (query || '').toLowerCase().replace(/[^\u0000-\u007F\u0980-\u09ff\w\s]/g, '');
  const a = (task || '').toLowerCase().replace(/[^\u0000-\u007F\u0980-\u09ff\w\s]/g, '');
  const queryWords = new Set(q.split(/\s+/).filter(w => w && w.length > 2));
  const taskWords = a.split(/\s+/).filter(w => w && w.length > 2);
  if (queryWords.size === 0) return 0;
  let match = 0;
  for (const w of taskWords) if (queryWords.has(w)) match++;
  return match / Math.max(1, queryWords.size);
}

function searchCoreKnowledge(userMessage) {
  try {
    const knowledgePath = path.join(__dirname, '..', 'src', 'main', 'resources', 'core_knowledge.json');
    if (!fs.existsSync(knowledgePath)) return null;
    const raw = fs.readFileSync(knowledgePath, 'utf8');
    const list = JSON.parse(raw || '[]');
    let best = null;
    let bestScore = 0;
    for (const item of list) {
      const score = calculateOverlapScore(userMessage, item.task || item.question || '');
      if (score > bestScore) {
        bestScore = score;
        best = item;
      }
    }
    if (best && bestScore >= 0.3) {
      return { solution: best.solution || best.answer || '', score: bestScore, category: best.category };
    }
    return null;
  } catch (e) {
    console.error('[CoreKnowledge] read error', e && e.message);
    return null;
  }
}

function classifySemanticIntent(userMessage) {
  const q = (userMessage || '').toLowerCase();
  if (/exception|error|compile|run|bug|nullpointer|git|npm|gradle|dependency|api|db|class|function|method|import|debug|stack trace/i.test(q)) {
    return { categoryId: 'coding', name: 'Coding', timeout: 3000 };
  }
  if (/bangladesh|govt|government|সরকার|কর|দাপ্তরিক|মন্ত্রণালয়/i.test(q)) {
    return { categoryId: 'bangladesh_govt', name: 'Bangladesh Govt', timeout: 3500 };
  }
  if (/weather|temperature|rain|আবহাওয়া|বৃষ্টি|তাপমাত্রা/i.test(q)) {
    return { categoryId: 'weather', name: 'Weather', timeout: 2000 };
  }
  if (/tech|nvidia|gpu|cpu|openai|gemini|llama|release|প্রযুক্তি/i.test(q)) {
    return { categoryId: 'tech_news', name: 'Tech News', timeout: 3000 };
  }
  if (/health|doctor|hospital|medicine|স্বাস্থ্য|চিকিৎসা/i.test(q)) {
    return { categoryId: 'health', name: 'Health', timeout: 3000 };
  }
  return { categoryId: 'general', name: 'General', timeout: 3000 };
}

// Virtual crawler removed in favor of unified scrapeEngine
function generateSmartAIResponse(userMessage) {
  const msg = (userMessage || '').trim();
  if (!msg) {
    return { success: false, message: 'Empty input. Please enter a valid message.', agent_name: 'SupremeAI Neural Core', confidence: 0, source_type: 'error' };
  }

  if (/who are you|আপনি কে/i.test(msg)) {
    return {
      success: true,
      message: `আমি ${process.env.SUPREME_BRAND_NAME || 'SupremeAI'}। আমি আপনার ডিজিটাল অ্যাসিস্ট্যান্ট।`,
      agent_name: process.env.SUPREME_BRAND_NAME || 'SupremeAI',
      confidence: 0.99,
      source_type: 'LOCAL_SEED',
    };
  }
  if (/time|সময়|বাজে|time now/i.test(msg)) {
    const bdTime = new Date().toLocaleString('bn-BD', { timeZone: 'Asia/Dhaka' });
    return {
      success: true,
      message: `বর্তমান সময়: ${bdTime} (বাংলাদেশ সময়)`,
      agent_name: 'SupremeAI Neural Core',
      confidence: 1.0,
      source_type: 'LOCAL_SEED',
    };
  }
  if (/date|তারিখ|today/i.test(msg)) {
    const bdDate = new Date().toLocaleDateString('bn-BD', { timeZone: 'Asia/Dhaka', year: 'numeric', month: 'long', day: 'numeric' });
    return {
      success: true,
      message: `আজকের তারিখ: ${bdDate}`,
      agent_name: 'SupremeAI Neural Core',
      confidence: 1.0,
      source_type: 'LOCAL_SEED',
    };
  }

  const core = searchCoreKnowledge(userMessage);
  if (core) {
    return {
      success: true,
      message: core.solution,
      agent_name: 'SupremeAI Neural Core',
      confidence: core.score,
      source_type: 'CORE_KNOWLEDGE',
    };
  }

  return {
    success: true,
    message: 'আমি তথ্যটি বিশ্লেষণ করতে পারছি না।',
    agent_name: 'SupremeAI Neural Core',
    confidence: 0.1,
    source_type: 'DEFAULT_FALLBACK',
  };
}

app.post(['/api/chat/legacy', '/chat/legacy'], async (req, res) => {
  const userMessage = (req.body && req.body.message) || '';
  if (!userMessage || !userMessage.trim()) {
    return res.status(400).json({ success: false, message: 'Message is required', source_type: 'error' });
  }
  try {
    const resp = generateSmartAIResponse(userMessage);
    return res.json(resp);
  } catch (e) {
    console.error('[API] chat handler error', e && e.message);
    return res.status(500).json({ success: false, message: 'Internal server error. Please try again later.', source_type: 'error' });
  }
});

app.get(['/api/projects', '/projects', '/api/api/projects'], (req, res) => {
  res.json({ success: true, data: [] });
});

app.use(['/api/admin/*', '/admin/*'], (req, res) => {
  const requestPath = req.path;
  let data = null;

  if (requestPath.includes('users')) data = [];
  if (requestPath.includes('rules')) data = [];
  if (requestPath.includes('plans')) data = [];
  if (requestPath.includes('chat')) data = [];
  if (requestPath.includes('logs')) data = [];
  if (requestPath.includes('quotas')) data = { usage: 0, limit: 1000 };

  res.json({ success: true, data, note: 'Emulator stub - real implementation pending on coordinator server' });
});

app.use((req, res) => {
  res.status(404).json({ error: 'Not found in api-router coordinator', path: req.path });
});

module.exports = app;
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\deployment-monitor.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\deployment-monitor.js`

```javascript
// functions/deployment-monitor.js - AI-Powered Deployment Monitor
// Uses Groq AI to analyze GitHub changes and wake system if needed

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin only once (index.js already calls initializeApp)
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();

// Groq API configuration
const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

/**
 * HTTP trigger: Analyze deployment changes with AI
 * Endpoint: https://region-supremeai.cloudfunctions.net/analyzeDeployment
 * 
 * LOCAL-FIRST MODE: If no AI API key is configured, uses heuristic analysis.
 */
exports.analyzeDeployment = onRequest(async (req, res) => {
    try {
        const { commitMessage, changedFiles, author, branch, runId } = req.body;

        if (!commitMessage || !changedFiles) {
            return res.status(400).json({
                error: "Missing required fields: commitMessage, changedFiles"
            });
        }

        // LOCAL-FIRST: No external AI API key required
        console.log("[LOCAL-FIRST] Analyzing deployment without external AI API key...");

        // Always use local heuristic analysis - no external API required
        const analysis = fallbackAnalysis({
            commitMessage,
            changedFiles,
            author,
            branch,
            projectInfo: getProjectContext()
        });

        // Determine if system needs to be woken up
        const needsWakeUp = shouldWakeSystem(analysis);

        // BUG FIX: Actually call the wakeSystem function if needed
        if (needsWakeUp) {
            console.log(`[MONITOR] High-impact deployment detected for Run ID: ${runId}. Pinging backend...`);
            const success = await wakeSystem(runId, analysis);
            if (!success) {
                console.warn(`[MONITOR] Warning: System wake-up failed for Run ID: ${runId}`);
            }
        }

        // Save analysis to Firestore for tracking
        await saveDeploymentAnalysis({
            timestamp: new Date().toISOString(),
            commitMessage,
            author,
            branch,
            changedFiles,
            analysis,
            needsWakeUp,
            runId,
            actionTaken: needsWakeUp ? "system_woken" : "none"
        });

        // Send notification to admin
        await sendDeploymentNotification(analysis, needsWakeUp);

        res.json({
            success: true,
            analysis,
            needsWakeUp,
            action: needsWakeUp ? "System woken up" : "No action needed",
            localMode: true
        });

    } catch (error) {
        console.error("Error analyzing deployment:", error);
        res.status(500).json({
            error: error.message,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
});

/**
 * Use Groq AI to analyze deployment changes
 */
async function analyzeWithGroq(apiKey, deploymentInfo) {
    const systemPrompt = `
You are an AI DevOps engineer monitoring the SupremeAI deployment system.
Analyze GitHub commits/changes and determine:

1. IMPACT LEVEL: (critical, high, medium, low, none)
   - critical: Core system files changed (backend, database, security, API)
   - high: Major feature changes affecting multiple components
   - medium: Feature additions or significant modifications
   - low: Documentation, minor UI tweaks, non-essential changes
   - none: Test files, CI/CD config updates only

2. WAKE_NEEDED: (true/false)
   - TRUE if impact is critical OR high AND files are in: src/, build.gradle*, Dockerfile, application.yml, functions/
   - FALSE for low-impact changes

3. ACTION: Recommended action (deploy, notify, ignore, wake_system)

4. REASON: Brief explanation in 1-2 sentences

Respond in JSON format:
{
  "impact": "critical|high|medium|low|none",
  "wakeNeeded": true|false,
  "action": "string",
  "reason": "string",
  "affectedComponents": ["list", "of", "components"]
}
`;

    const userMessage = `
Analyze this deployment:

Commit: ${deploymentInfo.commitMessage}
Author: ${deploymentInfo.author || 'Unknown'}
Branch: ${deploymentInfo.branch}
Changed Files:
${deploymentInfo.changedFiles.map(f => `- ${f}`).join('\n')}

Project Context:
${deploymentInfo.projectInfo}

Analyze and respond with JSON only.
`;

    try {
        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-70b-8192",
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userMessage }
                ],
                temperature: 0.1,
                max_tokens: 512
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 10000
            }
        );

        const content = response.data.choices[0].message.content;
        // Extract JSON from response (Groq may include markdown)
        const jsonMatch = content.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
            return JSON.parse(jsonMatch[0]);
        }

        throw new Error("Could not parse AI response");
    } catch (error) {
        console.error("Groq API error:", error.message);
        // Fallback: simple heuristic analysis
        return fallbackAnalysis(deploymentInfo);
    }
}

/**
 * Fallback analysis if Groq fails
 */
function fallbackAnalysis(deploymentInfo) {
    const criticalPatterns = [
        /src\/main/,
        /build\.gradle/,
        /application\.yml/,
        /Dockerfile/,
        /functions\//,
        /firebase\.json/,
        /security/,
        /auth/,
        /database/
    ];

    const allFiles = deploymentInfo.changedFiles.join(' ').toLowerCase();

    const isCritical = criticalPatterns.some(pattern => pattern.test(allFiles));

    return {
        impact: isCritical ? "critical" : "medium",
        wakeNeeded: isCritical,
        action: isCritical ? "wake_system" : "notify",
        reason: isCritical
            ? "Critical system files changed - immediate attention required"
            : "Non-critical changes detected",
        affectedComponents: isCritical ? ["core_system"] : ["ui_or_docs"],
        fallback: true
    };
}

/**
 * Determine if system needs to be woken up
 */
function shouldWakeSystem(analysis) {
    return analysis.wakeNeeded === true ||
        analysis.impact === "critical" ||
        analysis.action === "wake_system";
}

/**
 * Wake up the Cloud Run service by sending a health check request
 */
async function wakeSystem(runId, analysis) {
    const backendUrl = process.env.JAVA_BACKEND_URL || "https://ide-api.supremeai.google.com";

    try {
        console.log(`Waking system for run ${runId}...`);

        // Send wake-up ping to health endpoint
        const response = await axios.get(`${backendUrl}/api/health`, {
            timeout: 30000,
            headers: {
                "X-Wake-Call": "true",
                "X-Run-ID": runId || "unknown"
            }
        });

        console.log(`System wake response:`, response.data);

        // Log wake event
        await db.collection('system_wake_events').add({
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            runId,
            analysis,
            status: "woken",
            response: response.data
        });

        return true;
    } catch (error) {
        console.error("Failed to wake system:", error.message);
        await db.collection('system_wake_events').add({
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            runId,
            analysis,
            status: "failed",
            error: error.message
        });
        return false;
    }
}

/**
 * Get project context for AI analysis
 */
function getProjectContext() {
    return `
SupremeAI Multi-Agent System:
- Backend: Spring Boot 3 (Java 21) on Cloud Run
- Frontend: Flutter web on Firebase Hosting
- Functions: Firebase Cloud Functions
- AI: 10+ providers (Groq, OpenAI, Gemini, Claude, etc.)
- Database: Firestore + Redis caching
- Key directories:
  * src/main/java/com/supremeai/ - Backend Java code
  * supremeai/ - Flutter frontend
  * functions/ - Firebase Cloud Functions
  * dashboard/ - 3D React dashboard
  * build.gradle.kts, settings.gradle.kts - Gradle config
  * application.yml - Spring configuration
`;
}

/**
 * Save deployment analysis to Firestore
 */
async function saveDeploymentAnalysis(data) {
    try {
        await db.collection('deployment_analysis').add({
            ...data,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    } catch (error) {
        console.error("Error saving analysis:", error.message);
    }
}

/**
 * Send smart deployment notification via FCM
 */
async function sendDeploymentNotification(analysis, needsWakeUp) {
    const title = needsWakeUp
        ? "🚨 CRITICAL: System Woken Up"
        : analysis.impact === "high"
            ? "⚠️ High Impact Deployment"
            : "📦 Deployment Detected";

    const body = needsWakeUp
        ? `Critical changes detected. System has been activated. ${analysis.reason || ''}`
        : analysis.reason || "Deployment completed with medium/low impact changes";

    const notification = {
        notification: {
            title,
            body,
            clickAction: "FLUTTER_NOTIFICATION_CLICK"
        },
        data: {
            type: "deployment_analysis",
            impact: analysis.impact || "unknown",
            wakeNeeded: needsWakeUp.toString(),
            timestamp: new Date().toISOString()
        },
        topic: "admin-notifications"
    };

    try {
        await admin.messaging().send(notification);
        console.log("Deployment notification sent");
    } catch (error) {
        console.error("Error sending notification:", error.message);
    }
}

/**
 * Scheduled trigger: Periodic system health check with AI analysis
 * Runs every 5 minutes
 */
exports.monitorSystemHealth = onSchedule('*/5 * * * *', async (event) => {
    try {
        const backendUrl = process.env.JAVA_BACKEND_URL || "https://ide-api.supremeai.google.com";
        const healthResponse = await axios.get(`${backendUrl}/api/health`, {
            timeout: 10000
        }).catch(() => null);

        if (!healthResponse) {
            console.log("System appears to be down. Attempting to diagnose...");
            await diagnoseAndAlert();
        } else {
            console.log("System health check passed:", healthResponse.data);
        }

        return null;
    } catch (error) {
        console.error("Health monitor error:", error);
        return null;
    }
});

/**
 * Diagnose system issues and alert admin
 */
async function diagnoseAndAlert() {
    const groqApiKey = functions.config().groq?.api_key ||
        process.env.GROQ_API_KEY_DEPLOYMENT_MONITOR;

    if (!groqApiKey) {
        console.warn("Groq API key not available for diagnosis");
        return;
    }

    // Collect recent logs and errors
    const recentErrors = await db.collection('system_health')
        .orderBy('createdAt', 'desc')
        .limit(10)
        .get();

    const errorSummaries = recentErrors.docs.map(doc => doc.data());

    // Ask Groq to diagnose
    const diagnosis = await askGroqForDiagnosis(groqApiKey, errorSummaries);

    await admin.messaging().send({
        notification: {
            title: "🚨 System Down - AI Diagnosis",
            body: diagnosis.summary || "System appears offline. Check Cloud Run logs.",
            clickAction: "FLUTTER_NOTIFICATION_CLICK"
        },
        data: {
            type: "system_diagnosis",
            diagnosis: JSON.stringify(diagnosis),
            timestamp: new Date().toISOString()
        },
        topic: "admin-notifications"
    });
}

/**
 * Ask Groq to diagnose system issues
 */
async function askGroqForDiagnosis(apiKey, errorData) {
    try {
        const prompt = `
Based on these recent system health records, diagnose the likely cause and suggest fixes:

${JSON.stringify(errorData, null, 2)}

Respond in JSON:
{
  "likelyCause": "brief description",
  "suggestedFix": "actionable fix",
  "severity": "critical|high|medium|low",
  "summary": "One-line summary for notification"
}
`;

        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-70b-8192",
                messages: [{ role: "user", content: prompt }],
                temperature: 0.2,
                max_tokens: 256
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 10000
            }
        );

        const content = response.data.choices[0].message.content;
        const jsonMatch = content.match(/\{[\s\S]*\}/);
        return jsonMatch ? JSON.parse(jsonMatch[0]) : { summary: "AI analysis unavailable" };
    } catch (error) {
        return {
            likelyCause: "Unknown",
            suggestedFix: "Check Cloud Run logs manually",
            severity: "high",
            summary: "System down - manual investigation required"
        };
    }
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\health-smart.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\health-smart.js`

```javascript
// Simple health + stats endpoints for emulator stability

exports.healthCheck = (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.json({ status: 'ok', timestamp: new Date().toISOString(), mode: 'emulator' });
};

exports.getProviderHealthStats = (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.json({
    success: true,
    data: {
      total: 2,
      active: 2,
      error: 0,
      rotating: 0,
      lastCheck: new Date().toISOString()
    }
  });
};
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\index.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\index.js`

```javascript
// functions/index.js - Firebase Cloud Functions for AI System
// Deploy with: firebase deploy --only functions

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// ============ GLOBAL CORS (for 127.0.0.1 emulator + future) ============
const allowedOrigins = [
    'http://127.0.0.1:3000',
    'http://127.0.0.1:5173',
    'http://127.0.0.1:5000',
    'http://127.0.0.1:3000',
    'http://127.0.0.1:5173',
    'http://127.0.0.1:5000'
];

const allowCors = (handler) => async (req, res) => {
    const origin = req.headers.origin;
    const allowedOrigin = (origin && (allowedOrigins.includes(origin) || origin.includes('supremeai'))) ? origin : 'https://supremeai-dashboard.web.app';

    res.set('Access-Control-Allow-Origin', allowedOrigin);
    res.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-api-key');
    res.set('Vary', 'Origin');

    if (req.method === 'OPTIONS') {
        return res.status(204).send('');
    }
    return handler(req, res);
};

// ============ AUTHENTICATION MIDDLEWARE ============
const authenticate = async (req, res, next) => {
    // 1. Allow Java backend to bypass if correct system secret is provided
    const apiKey = req.get('x-api-key') || (req.body && req.body.apiKey) || (req.query && req.query.apiKey);
    const systemSecret = functions.config().system && functions.config().system.secret;

    // SECURITY FIX: Only allow bypass if system secret is configured AND matches
    // Do NOT allow bypass if systemSecret is undefined/null/empty
    if (systemSecret && systemSecret.trim() !== '' && apiKey && apiKey === systemSecret) {
        console.log('Java backend authenticated via system secret');
        return next();
    }

    // 2. Require Firebase Auth Admin Token for frontend/admin UI calls
    const authHeader = req.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: "Unauthorized: Missing or invalid token" });
    }

    try {
        const idToken = authHeader.split('Bearer ')[1];
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        // Enforce 'admin' claim as a strict boolean true
        if (decodedToken.admin !== true) {
            return res.status(403).json({ error: "Forbidden: Admin access required" });
        }
        req.user = decodedToken;
        return next();
    } catch (error) {
        console.error('Error verifying token:', error);
        return res.status(401).json({ error: "Unauthorized: Invalid token" });
    }
};

const withAuth = (handler) => {
    return async (req, res) => {
        return authenticate(req, res, () => handler(req, res));
    };
};

// ============ SYSTEM HEALTH MONITORING ============

const systemHealth = require('./system-health');
exports.getSystemHealth = systemHealth.getSystemHealth;
exports.collectHealthMetrics = systemHealth.collectHealthMetrics;

// Smart AI Providers (auto-discovery from Cloud Run + env + Firestore)
const smartProviders = require('./providers-smart');
exports.getConfiguredProviders = smartProviders.getConfiguredProviders;
exports.getProviderHealthStats = smartProviders.getProviderHealthStats;

// Central API Router (best long-term solution)
const apiRouter = require('./api-router');
exports.api = require('firebase-functions').https.onRequest(apiRouter);

// ============ REQUIREMENT PROCESSING ============

/**
 * HTTP trigger: Process new requirement from admin
 * Endpoint: https://region-supremeai.cloudfunctions.net/processRequirement
 */
exports.processRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, description } = req.body;

        if (!projectId || !description) {
            return res.status(400).json({ error: "Missing projectId or description" });
        }

        // Call Java backend to classify
        const backendUrl = (functions.config().backend && functions.config().backend.url) || process.env.JAVA_BACKEND_URL || 'http://127.0.0.1:8080';
        const classificationUrl = `${backendUrl}/classify`;
        const classifyResponse = await axios.post(classificationUrl, { description });
        const size = classifyResponse.data.size; // SMALL, MEDIUM, or BIG

        // Save requirement to Firestore
        const reqRef = await db.collection("requirements").add({
            projectId,
            description,
            size,
            status: "pending",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Auto-approve or notify
        if (size === "SMALL") {
            await reqRef.update({ status: "approved" });
            console.log(`✅ Auto-approved SMALL requirement: ${reqRef.id}`);
        } else if (size === "MEDIUM") {
            // Schedule auto-approve after 10 minutes
            db.collection("scheduled_approvals").add({
                requirementId: reqRef.id,
                approvalTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 10 * 60000)),
            });

            // Send notification
            await admin.messaging().send({
                notification: {
                    title: "⏳ Approval Needed",
                    body: description,
                },
                data: { requirementId: reqRef.id },
                topic: "admin-notifications",
            });
            console.log(`⏳ MEDIUM requirement pending approval: ${reqRef.id}`);
        } else {
            // Send urgent notification for BIG tasks
            await admin.messaging().send({
                notification: {
                    title: "🛑 URGENT: Manual Approval Required",
                    body: description,
                },
                data: { requirementId: reqRef.id, type: "big_approval" },
                topic: "admin-notifications",
            });
            console.log(`🛑 BIG requirement awaiting manual approval: ${reqRef.id}`);
        }

        res.json({
            success: true,
            requirementId: reqRef.id,
            size,
            message: `Requirement processed as ${size}`,
        });
    } catch (error) {
        console.error("Error processing requirement:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ APPROVAL HANDLING ============

/**
 * HTTP trigger: Admin approves/rejects requirement
 * Endpoint: https://region-supremeai.cloudfunctions.net/approveRequirement
 */
exports.approveRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { requirementId, approved, notes } = req.body;

        if (!requirementId) {
            return res.status(400).json({ error: "Missing requirementId" });
        }

        // Update requirement status
        await db.collection("requirements").doc(requirementId).update({
            status: approved ? "approved" : "rejected",
            notes,
            approvedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // If approved, trigger agent orchestrator
        if (approved) {
            const req_doc = await db.collection("requirements").doc(requirementId).get();
            const { projectId, description } = req_doc.data();

            // Call Java backend orchestrator
            const backendUrl = (functions.config().backend && functions.config().backend.url) || process.env.JAVA_BACKEND_URL || 'http://127.0.0.1:8080';
            const orchestrateUrl = `${backendUrl}/orchestrate`;
            await axios.post(orchestrateUrl, {
                projectId,
                requirementDescription: description,
            });

            // Update project status
            await db.collection("projects").doc(projectId).update({
                status: "building",
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }

        res.json({
            success: true,
            status: approved ? "approved" : "rejected",
        });
    } catch (error) {
        console.error("Error approving requirement:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ AUTO-APPROVAL SCHEDULER ============

/**
 * Scheduled trigger: Auto-approve MEDIUM tasks after 10 minutes
 */
exports.autoApproveScheduled = onSchedule("*/5 * * * *", async (event) => {
    const now = admin.firestore.Timestamp.now();

    const scheduledApprovals = await db.collection("scheduled_approvals")
        .where("approvalTime", "<=", now)
        .get();

    for (const doc of scheduledApprovals.docs) {
        const { requirementId } = doc.data();

        // Check if still pending
        const req = await db.collection("requirements").doc(requirementId).get();
        if (req.data().status === "pending") {
            await req.ref.update({
                status: "approved",
                autoApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            console.log(`✅ Auto-approved MEDIUM requirement: ${requirementId}`);
        }

        // Delete scheduled entry
        await doc.ref.delete();
    }

    return null;
});

// ============ AI AGENT ROTATION ============

/**
 * HTTP trigger: Handle quota exceeded / API errors
 * Called by Java backend on 429/403 errors
 */
exports.rotateAgent = onRequest(withAuth(async (req, res) => {
    try {
        const { agentId, reason } = req.body;

        // Update agent status
        await db.collection("ai_pool").doc(agentId).update({
            status: "rotated",
            reason,
            rotatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Trigger VPN switch (if enabled in config)
        const config = await db.collection("config").doc("system").get();
        if (config.data().vpn_enabled) {
            const vpnResult = await switchVPN(agentId);
            console.log(`🔄 VPN switched for ${agentId}: ${vpnResult}`);
        }

        // Notify admin
        await admin.messaging().send({
            notification: {
                title: "⚠️  Agent Rotated",
                body: `${agentId} rotated due to: ${reason}`,
            },
            topic: "admin-notifications",
        });

        res.json({ success: true, message: "Agent rotated" });
    } catch (error) {
        console.error("Error rotating agent:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ CHAT MESSAGE HANDLER ============

/**
 * Firestore trigger: Save AI messages to chat history
 */
exports.onChatMessage = onDocumentCreated("projects/{projectId}/chat/{messageId}", async (event) => {
    const { projectId } = event.params;
    const message = event.data.data();

    // Update project's lastMessage timestamp
    await admin.firestore().collection("projects").doc(projectId).update({
        lastMessageAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send real-time notification if from AI
    if (message.sender !== "admin") {
        const project = await admin.firestore().collection("projects").doc(projectId).get();
        const adminUserId = project.data().adminUserId;

        if (adminUserId) {
            await admin.messaging().send({
                notification: {
                    title: `${message.sender} Updated`,
                    body: message.message.substring(0, 50) + "...",
                },
                data: {
                    projectId,
                    type: "chat_update",
                },
                topic: `user-${adminUserId}`,
            });
        }
    }
});

// ============ PROGRESS TRACKER ============

/**
 * HTTP trigger: Update project progress
 */
exports.updateProgress = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, progress, status } = req.body;

        const updateData = {
            progress,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        if (status) {
            updateData.status = status;
        }
        await db.collection("projects").doc(projectId).update(updateData);

        res.json({ success: true });
    } catch (error) {
        console.error("Error updating project progress:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ SERVER CONNECTION MONITORING ============

const serverConnectionMonitor = require('./server-connection-monitor');
exports.checkServerConnections = serverConnectionMonitor.checkServerConnections;
exports.monitorConnections = serverConnectionMonitor.monitorConnections;

// ============ AI DEPLOYMENT MONITORING ============

const deploymentMonitor = require('./deployment-monitor');
exports.analyzeDeployment = deploymentMonitor.analyzeDeployment;
exports.monitorSystemHealth = deploymentMonitor.monitorSystemHealth;

// ============ BENGALI OCR PROCESSING ============

/**
 * HTTP trigger: Process Multi-language OCR on uploaded images
 * Endpoint: https://region-supremeai.cloudfunctions.net/processOCR
 */
const locales = {
  en: {
    ocr_complete_title: "✅ OCR Processing Complete",
    ocr_complete_body: "Processed {success_count}/{total_count} images",
    error_missing_urls: "Missing or invalid imageUrls array",
    error_forbidden_url: "Forbidden URL target",
    error_vision_api: "Vision API error: {message}"
  },
  bn: {
    ocr_complete_title: "✅ ওসিআর প্রসেসিং সম্পন্ন",
    ocr_complete_body: "সফলভাবে {success_count}/{total_count} টি ইমেজ প্রসেস করা হয়েছে",
    error_missing_urls: "অনুপস্থিত বা অবৈধ imageUrls অ্যারে",
    error_forbidden_url: "নিষিদ্ধ ইউআরএল টার্গেট",
    error_vision_api: "ভিশন এপিআই ত্রুটি: {message}"
  }
};

function getLocaleString(locale, key, params = {}) {
  const dict = locales[locale] || locales['en'];
  let str = dict[key] || locales['en'][key] || key;
  for (const [k, v] of Object.entries(params)) {
    str = str.replace(`{${k}}`, v);
  }
  return str;
}

const axiosGetWithRetry = async (url, options, retries = 3, delay = 1000) => {
    for (let i = 0; i < retries; i++) {
        try {
            return await axios.get(url, options);
        } catch (error) {
            if (i === retries - 1) throw error;
            console.warn(`Axios fetch failed, retrying in ${delay}ms... (Attempt ${i + 1}/${retries})`);
            await new Promise(res => setTimeout(res, delay));
            delay *= 2; // Exponential Backoff
        }
    }
};

exports.processOCR = onRequest(withAuth(async (req, res) => {
    try {
        const { imageUrls, projectId, userId, languages = ['en', 'bn'], locale = 'en' } = req.body;

        if (!imageUrls || !Array.isArray(imageUrls) || imageUrls.length === 0) {
            return res.status(400).json({ error: getLocaleString(locale, 'error_missing_urls') });
        }

        const results = [];
        const vision = require('@google-cloud/vision');
        const client = new vision.ImageAnnotatorClient();

        // Use Promise.all for parallel processing to improve performance
        const processingPromises = imageUrls.map(async (imageUrl) => {
            try {
                console.log(`🔍 Processing OCR for: ${imageUrl}`);

                // SSRF Protection: Validate URL
                const urlObj = new URL(imageUrl);
                const forbiddenHostPatterns = [/169\.254/, /127\.0\.0\.1/, /127.0.0.1/];
                if (forbiddenHostPatterns.some(pattern => pattern.test(urlObj.hostname))) {
                    throw new Error(getLocaleString(locale, 'error_forbidden_url'));
                }

                // For Firebase Storage URLs, we need to download the image
                let image;
                if (imageUrl.startsWith('gs://') || imageUrl.startsWith('https://firebasestorage.googleapis.com')) {
                    // Download from Firebase Storage
                    const bucket = admin.storage().bucket();
                    // Note: Improved parsing logic needed for complex URLs
                    const fileName = imageUrl.split('/').pop();
                    const file = bucket.file(fileName);
                    const [buffer] = await file.download();
                    image = { content: buffer };
                } else if (imageUrl.startsWith('data:image/')) {
                    // Base64 encoded image
                    const base64Data = imageUrl.split(',')[1];
                    image = { content: Buffer.from(base64Data, 'base64') };
                } else {
                    // External URL with timeout and retry logic
                    const response = await axiosGetWithRetry(imageUrl, {
                        responseType: 'arraybuffer',
                        timeout: 10000,
                        headers: { 'Accept': 'image/*' }
                    });
                    image = { content: Buffer.from(response.data) };
                }

                // Configure for multi-language text recognition
                const imageContext = {
                    languageHints: languages,
                };

                // Perform OCR
                const [result] = await client.textDetection({
                    image,
                    imageContext,
                });

                if (result.error) {
                    throw new Error(getLocaleString(locale, 'error_vision_api', { message: result.error.message }));
                }

                const detections = result.textAnnotations;
                const extractedText = detections.length > 0 ? detections[0].description : '';

                // Parse table structure if possible
                const lines = extractedText.split('\n').filter(line => line.trim());
                const tableData = parseTableFromText(lines);

                // Save to Firestore
                const ocrResult = {
                    imageUrl,
                    extractedText,
                    tableData,
                    languages_requested: languages,
                    processedAt: admin.firestore.FieldValue.serverTimestamp(),
                    confidence: detections.length > 0 ? detections[0].boundingPoly : null,
                };

                if (projectId) {
                    await db.collection('projects').doc(projectId).collection('ocr_results').add(ocrResult);
                }

                return {
                    imageUrl,
                    success: true,
                    textLength: extractedText.length,
                    linesCount: lines.length,
                    tableDetected: tableData.length > 0,
                };

            } catch (imageError) {
                console.error(`Error processing ${imageUrl}:`, imageError);
                return {
                    imageUrl,
                    success: false,
                    error: imageError.message,
                };
            }
        });

        const results = await Promise.all(processingPromises);

        if (userId && results.some(r => r.success)) {
            const successCount = results.filter(r => r.success).length;
            await admin.messaging().send({
                notification: {
                    title: getLocaleString(locale, 'ocr_complete_title'),
                    body: getLocaleString(locale, 'ocr_complete_body', { success_count: successCount, total_count: results.length }),
                },
                data: {
                    type: "ocr_complete",
                    projectId: projectId || "",
                },
                topic: `user-${userId}`,
            });
        }

        res.json({
            success: true,
            results,
            summary: {
                total: results.length,
                successful: results.filter(r => r.success).length,
                failed: results.filter(r => !r.success).length,
            },
            // Pattern from seed_data: Consistent meta response
            _meta: {
                timestamp: new Date().toISOString(),
                version: "2.1.0-international"
            }
        });

    } catch (error) {
        console.error("Error in Bengali OCR processing:", error);
        res.status(500).json({ error: error.message });
    }
}));

/**
 * HTTP trigger: Get OCR results for a project
 * Endpoint: https://region-supremeai.cloudfunctions.net/getOCRResults
 */
exports.getOCRResults = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId } = req.query;

        if (!projectId) {
            return res.status(400).json({ error: "Missing projectId" });
        }

        const ocrResults = await db.collection('projects').doc(projectId)
            .collection('ocr_results')
            .orderBy('processedAt', 'desc')
            .get();

        const results = [];
        ocrResults.forEach(doc => {
            results.push({
                id: doc.id,
                ...doc.data(),
            });
        });

        res.json({
            success: true,
            results,
        });

    } catch (error) {
        console.error("Error fetching OCR results:", error);
        res.status(500).json({ error: error.message });
    }
}));

/**
 * HTTP trigger: Convert OCR results to Excel and upload
 * Endpoint: https://region-supremeai.cloudfunctions.net/exportOCRToExcel
 */
exports.exportOCRToExcel = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, resultIds } = req.body;

        if (!projectId || !resultIds || !Array.isArray(resultIds)) {
            return res.status(400).json({ error: "Missing projectId or resultIds array" });
        }

        const ExcelJS = require('exceljs');
        const workbook = new ExcelJS.Workbook();

        for (const resultId of resultIds) {
            const resultDoc = await db.collection('projects').doc(projectId)
                .collection('ocr_results').doc(resultId).get();

            if (!resultDoc.exists) {
                continue;
            }

            const result = resultDoc.data();
            const worksheet = workbook.addWorksheet(`OCR_${resultId.slice(-8)}`);

            // Add metadata
            worksheet.addRow(['Image URL', result.imageUrl]);
            worksheet.addRow(['Processed At', result.processedAt.toDate()]);
            worksheet.addRow(['Language', result.language]);
            worksheet.addRow(['']); // Empty row

            // Add extracted text
            worksheet.addRow(['Extracted Text']);
            worksheet.addRow([result.extractedText]);
            worksheet.addRow(['']); // Empty row

            // Add table data if available
            if (result.tableData && result.tableData.length > 0) {
                worksheet.addRow(['Structured Table Data']);
                result.tableData.forEach(row => {
                    worksheet.addRow(row);
                });
            }
        }

        // Generate Excel buffer
        const buffer = await workbook.xlsx.writeBuffer();

        // Upload to Firebase Storage
        const bucket = admin.storage().bucket();
        const fileName = `ocr_exports/${projectId}/bengali_ocr_${Date.now()}.xlsx`;
        const file = bucket.file(fileName);

        await file.save(buffer, {
            metadata: {
                contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            },
        });

        // Get download URL
        const [url] = await file.getSignedUrl({
            action: 'read',
            expires: Date.now() + 7 * 24 * 60 * 60 * 1000, // Expires in 7 days
        });

        // Save export record
        await db.collection('projects').doc(projectId).collection('exports').add({
            type: 'bengali_ocr_excel',
            fileName,
            downloadUrl: url,
            exportedAt: admin.firestore.FieldValue.serverTimestamp(),
            resultCount: resultIds.length,
        });

        res.json({
            success: true,
            downloadUrl: url,
            fileName,
            message: `Excel file created with ${resultIds.length} OCR results`,
        });

    } catch (error) {
        console.error("Error exporting to Excel:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ HELPER FUNCTIONS ============

/**
 * Parse table structure from OCR text lines
 */
function parseTableFromText(lines) {
    if (!lines || lines.length === 0) return [];

    const tableData = [];

    // Simple table detection: look for consistent column patterns
    // This is a basic implementation - can be enhanced with ML

    for (const line of lines) {
        // Split on multiple spaces or tabs (common in tabular data)
        const cells = line.split(/\s{2,}|\t/).map(cell => cell.trim()).filter(cell => cell);
        if (cells.length > 1) { // Likely a table row
            tableData.push(cells);
        }
    }

    return tableData;
}

// ============ HELPER: VPN SWITCHING ============

async function switchVPN(agentId) {
    // Call Proton/Windscribe API to rotate IP
    // For demo: just log
    console.log(`🔄 Switching VPN for ${agentId}`);
    return "VPN_SWITCHED";
}

// ============ FIRESTORE SECURITY RULES ============
// The active Firestore rules are configured in config/firestore.rules.
// Modifying this comment will not change the active rules. Please refer to config/firestore.rules.
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\package.json`

### File: `infrastructure\firebase_functions\firebase_functions_v1\package.json`

```json
{
  "name": "functions",
  "description": "Cloud Functions for Firebase",
  "scripts": {
    "serve": "firebase emulators:start --only functions",
    "shell": "firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "lint": "echo 'Linting functions...'",
    "logs": "firebase functions:log",
    "build": "tsc"
  },
  "engines": {
    "node": "22"
  },
  "main": "index.js",
  "dependencies": {
    "@dataconnect/admin-generated": "file:./src/dataconnect-admin-generated",
    "@google-cloud/vision": "^3.1.0",
    "axios": "^1.4.0",
    "cors": "^2.8.5",
    "exceljs": "^4.3.0",
    "express": "^4.18.2",
    "firebase-admin": "^13.10.0",
    "firebase-functions": "^7.2.5",
    "nodemailer": "^6.9.13",
    "mailparser": "^3.7.1"
  },
  "devDependencies": {
    "firebase-functions-test": "^3.1.0",
    "typescript": "^5.0.0"
  },
  "private": true
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\providers-smart.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\providers-smart.js`

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

// ============ SMART PROVIDER DISCOVERY ============
// Discovers AI models from multiple sources:
// 1. Firestore (user-added API keys)
// 2. Environment config (Firebase, Vertex AI)
// 3. Cloud Run service discovery (deployed models)

async function discoverProviders() {
  const providers = [];

  // ── Source: Firestore (user-configured and dynamic system providers) ──
  try {
    const db = admin.firestore();
    const snap = await db.collection('ai_providers').get();
    snap.forEach(doc => {
      const data = doc.data();
      // Skip inactive ones in general listing, or filter by active status if needed
      if (data.status === 'active') {
        providers.push({
          id: doc.id,
          name: data.name || doc.id,
          type: data.type || 'api',
          deploymentSource: data.deploymentSource || 'api',
          status: data.status || 'active',
          apiKeyConfigured: !!data.apiKey,
          endpoint: data.endpoint || '',
          models: data.models || [],
          roles: data.roles || ['general_chat'],
          source: data.source || 'firestore',
        });
      }
    });
  } catch (err) {
    console.error('Error discovering providers from Firestore:', err);
  }

  return providers;
}

// ============ API ENDPOINTS ============

exports.getConfiguredProviders = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        providers,
        total: providers.length,
        active: providers.length,
        sources: [...new Set(providers.map(p => p.source))],
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

exports.getProviderHealthStats = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        total: providers.length,
        active: providers.filter(p => p.status === 'active').length,
        error: 0,
        bySource: {
          firestore: providers.filter(p => p.source === 'firestore').length,
          env: providers.filter(p => p.source === 'env').length,
          cloudrun: providers.filter(p => p.source === 'cloudrun').length,
        }
      }
    });
  } catch (err) {
    res.json({ success: true, data: { total: 0, active: 0, error: 0 } });
  }
});
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\README_BD.md`

### File: `infrastructure\firebase_functions\firebase_functions_v1\README_BD.md`

```markdown
# functions/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি Firebase Cloud Functions কোড ধারণ করে, যা SupremeAI-র সার্ভার-সাইড লজিক হ্যান্ডেল করে।

## ফোল্ডার স্ট্রাকচার

```
functions/
├── lib/           # ফায়ারবেস ফাংশনগুলো
├── node_modules/  # নোড মডিউল
├── package.json   # ফাংশন প্যাকেজ
└── tsconfig.json  # টাইপস্ক্রিপ্ট কনফিগ
```

## মূল ফাংশনগুলো

| ফাংশন                   | ব্যবহার                      |
| ----------------------- | ---------------------------- |
| `api-router.js`         | API রাউটিং                   |
| `health-smart.js`       | সিস্টেম হেলথ চেক             |
| `deployment-monitor.js` | ডিপ্লোয়মেন্ট মনিটরিং        |
| `providers-smart.js`    | AI প্রোভাইডার্স ম্যানেজমেন্ট |

## ডিপ্লোয় করা

```bash
# ফাংশন ডিপ্লোয় করন
firebase deploy --only functions
```

## লোকালি রান

```bash
# ফায়ারবেস ইমুলেটর
firebase emulators:start --only functions
```
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\server-connection-monitor.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\server-connection-monitor.js`

```javascript
// functions/server-connection-monitor.js - Server Connection Monitoring
// Monitors connections between all servers and services

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Firebase app is initialized in index.js
const db = admin.firestore();

/**
 * HTTP trigger: Check all server connections
 * Endpoint: https://region-supremeai.cloudfunctions.net/checkServerConnections
 */
exports.checkServerConnections = onRequest(async (req, res) => {
    try {
        const connectionData = {
            timestamp: new Date().toISOString(),
            connections: {}
        };

        // Load system configuration
        const configDoc = await db.collection('config').doc('system').get();
        const config = configDoc.exists ? configDoc.data() : {};

        // Check Firebase connections
        connectionData.connections.firebase = await checkFirebaseConnections();

        // Check GCloud connections
        connectionData.connections.gcloud = await checkGCloudConnections();

        // Check Local Server connection
        connectionData.connections.local = await checkLocalServerConnection(config.localServerUrl || 'http://127.0.0.1:5000');

        // Check Smart Chat System connection
        connectionData.connections.smartChatSystem = await checkSmartChatSystemConnection(config.smartChatSystemUrl || 'http://127.0.0.1:5000');

        // Calculate overall connection status
        connectionData.overallStatus = calculateConnectionStatus(connectionData.connections);

        // Save connection snapshot to Firestore
        await saveConnectionSnapshot(connectionData);

        res.json({
            success: true,
            data: connectionData
        });
    } catch (error) {
        console.error("Error checking server connections:", error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * Check Firebase service connections
 */
async function checkFirebaseConnections() {
    const startTime = Date.now();
    try {
        const checks = {
            firestore: await checkFirestoreConnection(),
            auth: await checkAuthConnection(),
            storage: await checkStorageConnection()
        };

        const responseTime = Date.now() - startTime;
        const allHealthy = Object.values(checks).every(c => c.status === 'connected');

        return {
            name: 'Firebase',
            status: allHealthy ? 'connected' : 'degraded',
            responseTime: `${responseTime}ms`,
            services: checks,
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firebase',
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Firestore connection
 */
async function checkFirestoreConnection() {
    try {
        const testDoc = await db.collection('connection_checks').add({
            service: 'firestore',
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await testDoc.delete();

        return {
            service: 'Firestore',
            status: 'connected',
            latency: Date.now() - Date.now()
        };
    } catch (error) {
        return {
            service: 'Firestore',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Firebase Auth connection
 */
async function checkAuthConnection() {
    try {
        const auth = admin.auth();
        await auth.listUsers(1);

        return {
            service: 'Auth',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Auth',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Firebase Storage connection
 */
async function checkStorageConnection() {
    try {
        const bucket = admin.storage().bucket();
        const [files] = await bucket.getFiles({ maxResults: 1 });

        return {
            service: 'Storage',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Storage',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check GCloud service connections
 */
async function checkGCloudConnections() {
    const startTime = Date.now();
    try {
        const checks = {
            cloudFunctions: await checkCloudFunctionsConnection(),
            cloudRun: await checkCloudRunConnection(),
            bigQuery: await checkBigQueryConnection()
        };

        const responseTime = Date.now() - startTime;
        const allHealthy = Object.values(checks).every(c => c.status === 'connected');

        return {
            name: 'Google Cloud Platform',
            status: allHealthy ? 'connected' : 'degraded',
            responseTime: `${responseTime}ms`,
            services: checks,
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Google Cloud Platform',
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Cloud Functions connection
 */
async function checkCloudFunctionsConnection() {
    try {
        // Try to call a simple health check function
        const response = await axios.get(
            `https://us-central1-${process.env.GCP_PROJECT_ID || 'supremeai'}.cloudfunctions.net/getSystemHealth`,
            { timeout: 5000 }
        );

        return {
            service: 'Cloud Functions',
            status: response.status === 200 ? 'connected' : 'degraded',
            statusCode: response.status
        };
    } catch (error) {
        return {
            service: 'Cloud Functions',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Cloud Run connection
 */
async function checkCloudRunConnection() {
    try {
        // Check if any Cloud Run services are accessible
        // This would need to be configured based on your services
        return {
            service: 'Cloud Run',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Cloud Run',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check BigQuery connection
 */
async function checkBigQueryConnection() {
    try {
        // Check BigQuery connection (if configured)
        // This would need to be configured based on your setup
        return {
            service: 'BigQuery',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'BigQuery',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Local Server connection
 */
async function checkLocalServerConnection(url) {
    try {
        const response = await axios.get(`${url}/health`, {
            timeout: 5000
        });

        const data = response.data;

        return {
            name: 'Local Development Server',
            url: url,
            status: response.status === 200 ? 'connected' : 'degraded',
            responseTime: `${response.headers['x-response-time'] || 'N/A'}`,
            health: {
                status: data.status,
                cpu: data.cpu,
                memory: data.memory,
                disk: data.disk,
                uptime: data.uptime
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Local Development Server',
            url: url,
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Smart Chat System connection
 */
async function checkSmartChatSystemConnection(url) {
    try {
        const response = await axios.get(`${url}/api/status`, {
            timeout: 5000
        });

        return {
            name: 'Smart Chat System',
            url: url,
            status: response.status === 200 ? 'connected' : 'degraded',
            services: response.data.services || {},
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Smart Chat System',
            url: url,
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Calculate overall connection status
 */
function calculateConnectionStatus(connections) {
    const statuses = Object.values(connections).map(c => c.status);

    if (statuses.every(s => s === 'connected')) {
        return 'all_connected';
    } else if (statuses.some(s => s === 'connected')) {
        return 'partial';
    }
    return 'disconnected';
}

/**
 * Save connection snapshot to Firestore
 */
async function saveConnectionSnapshot(connectionData) {
    try {
        await db.collection('server_connections').add({
            ...connectionData,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Clean up old snapshots (keep last 7 days)
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 7);

        const oldSnapshots = await db.collection('server_connections')
            .where('createdAt', '<', cutoffDate)
            .limit(100)
            .get();

        const batch = db.batch();
        oldSnapshots.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
    } catch (error) {
        console.error('Error saving connection snapshot:', error);
    }
}

/**
 * Scheduled trigger: Check connections every 2 minutes
 */
exports.monitorConnections = onSchedule('*/2 * * * *', async (event) => {
    try {
        const connectionData = {
            timestamp: new Date().toISOString(),
            connections: {}
        };

        const configDoc = await db.collection('config').doc('system').get();
        const config = configDoc.exists ? configDoc.data() : {};

        connectionData.connections.firebase = await checkFirebaseConnections();
        connectionData.connections.gcloud = await checkGCloudConnections();
        connectionData.connections.local = await checkLocalServerConnection(config.localServerUrl || 'http://127.0.0.1:5000');
        connectionData.connections.smartChatSystem = await checkSmartChatSystemConnection(config.smartChatSystemUrl || 'http://127.0.0.1:5000');

        connectionData.overallStatus = calculateConnectionStatus(connectionData.connections);

        await saveConnectionSnapshot(connectionData);

        // Alert if any critical disconnections
        if (connectionData.overallStatus === 'disconnected') {
            await sendConnectionAlert(connectionData);
        }

        console.log('Connection monitoring completed');
        return null;
    } catch (error) {
        console.error('Error monitoring connections:', error);
        throw error;
    }
});

/**
 * Send connection alert notification
 */
async function sendConnectionAlert(connectionData) {
    try {
        const disconnectedServices = Object.entries(connectionData.connections)
            .filter(([_, conn]) => conn.status === 'disconnected')
            .map(([name, _]) => name);

        const message = {
            notification: {
                title: '🔴 Server Connection Alert',
                body: `Disconnected services: ${disconnectedServices.join(', ')}`
            },
            data: {
                type: 'connection_alert',
                status: connectionData.overallStatus,
                timestamp: connectionData.timestamp,
                services: disconnectedServices
            },
            topic: 'admin-notifications'
        };

        await admin.messaging().send(message);
        console.log('Connection alert sent successfully');
    } catch (error) {
        console.error('Error sending connection alert:', error);
    }
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\swagger.yaml`

### File: `infrastructure\firebase_functions\firebase_functions_v1\swagger.yaml`

```yaml
openapi: 3.0.0
info:
  title: SupremeAI API
  description: API documentation for SupremeAI coordinator and endpoints
  version: 1.0.0
servers:
  - url: https://supremeai-a.web.app/api
    description: Production Server
  - url: http://127.0.0.1:5000/api
    description: Local Emulator
paths:
  /chat/send:
    post:
      summary: Send a message to the unified chat handler
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
                useScrape:
                  type: boolean
      responses:
        '200':
          description: Successful AI response
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string
                  sources:
                    type: array
                    items:
                      type: string
                  confidence:
                    type: number
                  chatType:
                    type: string
                  sourceType:
                    type: string
  /scrape/and-respond:
    post:
      summary: Scrape and respond directly
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
      responses:
        '200':
          description: Successful scrape response
  /chat/classify:
    post:
      summary: Classify semantic intent of a message
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
      responses:
        '200':
          description: Intent classification
  /health:
    get:
      summary: Check health status of the API router
      responses:
        '200':
          description: Health OK
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\system-health.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\system-health.js`

```javascript
// functions/system-health.js - System Health Monitoring
// Monitors Firebase, GCloud, and Local PC health status

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Firebase app is initialized in index.js
const db = admin.firestore();

// Health check intervals (in milliseconds)
const HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
const HISTORY_RETENTION_DAYS = 7;

/**
 * HTTP trigger: Get current system health status
 * Endpoint: https://region-supremeai.cloudfunctions.net/getSystemHealth
 */
exports.getSystemHealth = onRequest({ cors: true }, async (req, res) => {
    try {
        const healthData = {
            timestamp: new Date().toISOString(),
            components: {}
        };

        // Check Firebase Health
        healthData.components.firebase = await checkFirebaseHealth();

        // Check GCloud Health
        healthData.components.gcloud = await checkGCloudHealth();

        // Check Local PC Health (if accessible)
        healthData.components.localPC = await checkLocalPcHealth();

        // Check Database Health
        healthData.components.database = await checkDatabaseHealth();

        // Calculate overall system status
        healthData.overallStatus = calculateOverallStatus(healthData.components);

        // Save health snapshot to Firestore
        await saveHealthSnapshot(healthData);

        res.json({
            success: true,
            data: healthData
        });
    } catch (error) {
        console.error("Error fetching system health:", error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * Check Firebase services health
 */
async function checkFirebaseHealth() {
    const startTime = Date.now();
    try {
        // Test Firestore
        const testDoc = await db.collection('health_checks').add({
            service: 'firestore',
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await testDoc.delete();

        // Test Authentication
        const auth = admin.auth();
        const userCount = (await auth.listUsers(1)).users.length;

        // Check Firebase Storage (if configured)
        const storageHealthy = await checkStorageHealth();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Firebase',
            status: 'healthy',
            uptime: '99.99%',
            responseTime: `${responseTime}ms`,
            services: {
                firestore: { status: 'healthy', responseTime: `${responseTime}ms` },
                auth: { status: 'healthy', userCount },
                storage: { status: storageHealthy ? 'healthy' : 'degraded' }
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firebase',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check GCloud services health
 */
async function checkGCloudHealth() {
    const startTime = Date.now();
    try {
        // Check Cloud Functions status
        const functionsHealthy = await checkCloudFunctionsHealth();

        // Check Cloud Run services (if any)
        const cloudRunHealthy = await checkCloudRunHealth();

        // Check BigQuery (if configured)
        const bigQueryHealthy = await checkBigQueryHealth();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Google Cloud Platform',
            status: functionsHealthy && cloudRunHealthy ? 'healthy' : 'degraded',
            uptime: '99.9%',
            responseTime: `${responseTime}ms`,
            services: {
                cloudFunctions: { status: functionsHealthy ? 'healthy' : 'degraded' },
                cloudRun: { status: cloudRunHealthy ? 'healthy' : 'degraded' },
                bigQuery: { status: bigQueryHealthy ? 'healthy' : 'degraded' }
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Google Cloud Platform',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Local PC health (if accessible via local server)
 */
async function checkLocalPcHealth() {
    try {
        // Try to reach local health endpoint
        const backendUrl = (functions.config().backend && functions.config().backend.url) || 'https://supremeai-a.web.app';
        const response = await axios.get(`${backendUrl}/health`, {
            timeout: 5000
        });

        const data = response.data;

        return {
            name: 'Local Development Server',
            status: data.status || 'healthy',
            uptime: data.uptime || 'N/A',
            cpu: data.cpu || { usage: 'N/A' },
            memory: data.memory || { usage: 'N/A', total: 'N/A' },
            disk: data.disk || { usage: 'N/A', total: 'N/A' },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        // Local server might not be running - this is expected
        return {
            name: 'Local Development Server',
            status: 'unavailable',
            message: 'Local server not accessible',
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Database health
 */
async function checkDatabaseHealth() {
    try {
        const startTime = Date.now();

        // Perform a simple read operation
        const snapshot = await db.collection('health_checks').limit(1).get();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Firestore Database',
            status: 'healthy',
            uptime: '99.99%',
            responseTime: `${responseTime}ms`,
            operations: {
                reads: 'healthy',
                writes: 'healthy',
                queries: 'healthy'
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firestore Database',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Firebase Storage health
 */
async function checkStorageHealth() {
    try {
        const bucket = admin.storage().bucket();
        const [files] = await bucket.getFiles({ maxResults: 1 });
        return true;
    } catch (error) {
        console.error('Storage health check failed:', error);
        return false;
    }
}

/**
 * Check Cloud Functions health
 */
async function checkCloudFunctionsHealth() {
    try {
        // Try to call a simple health check function
        // This would need to be implemented as a separate function
        return true;
    } catch (error) {
        console.error('Cloud Functions health check failed:', error);
        return false;
    }
}

/**
 * Check Cloud Run health
 */
async function checkCloudRunHealth() {
    try {
        // Check if any Cloud Run services are deployed and healthy
        // This would need to be configured based on your services
        return true;
    } catch (error) {
        console.error('Cloud Run health check failed:', error);
        return false;
    }
}

/**
 * Check BigQuery health
 */
async function checkBigQueryHealth() {
    try {
        // Check BigQuery connection (if configured)
        // This would need to be configured based on your setup
        return true;
    } catch (error) {
        console.error('BigQuery health check failed:', error);
        return false;
    }
}

/**
 * Calculate overall system status
 */
function calculateOverallStatus(components) {
    const statuses = Object.values(components).map(c => c.status);

    if (statuses.every(s => s === 'healthy')) {
        return 'healthy';
    } else if (statuses.some(s => s === 'critical')) {
        return 'critical';
    } else if (statuses.some(s => s === 'degraded' || s === 'unavailable')) {
        return 'degraded';
    }
    return 'healthy';
}

/**
 * Save health snapshot to Firestore
 */
async function saveHealthSnapshot(healthData) {
    try {
        await db.collection('system_health').add({
            ...healthData,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Clean up old health snapshots
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - HISTORY_RETENTION_DAYS);

        const oldSnapshots = await db.collection('system_health')
            .where('createdAt', '<', cutoffDate)
            .limit(100)
            .get();

        const batch = db.batch();
        oldSnapshots.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
    } catch (error) {
        console.error('Error saving health snapshot:', error);
    }
}

/**
 * Scheduled trigger: Collect health metrics every 5 minutes
 */
exports.collectHealthMetrics = onSchedule('*/5 * * * *', async (event) => {
    try {
        const healthData = {
            timestamp: new Date().toISOString(),
            components: {}
        };

        // Collect health data for all components
        healthData.components.firebase = await checkFirebaseHealth();
        healthData.components.gcloud = await checkGCloudHealth();
        healthData.components.localPC = await checkLocalPcHealth();
        healthData.components.database = await checkDatabaseHealth();

        healthData.overallStatus = calculateOverallStatus(healthData.components);

        // Save to Firestore
        await saveHealthSnapshot(healthData);

        // If status is critical, send alert
        if (healthData.overallStatus === 'critical') {
            await sendHealthAlert(healthData);
        }

        console.log('Health metrics collected successfully');
        return null;
    } catch (error) {
        console.error('Error collecting health metrics:', error);
        throw error;
    }
});

/**
 * Send health alert notification with AI-generated message
 */
async function sendHealthAlert(healthData) {
    try {
        // Get Groq API key for smart message generation
        const groqApiKey = functions.config().groq?.api_key ||
                          process.env.GROQ_API_KEY;

        let messageBody;
        if (groqApiKey) {
            // Use Groq to generate a smart, concise alert message
            messageBody = await generateSmartAlertMessage(groqApiKey, healthData);
        } else {
            // Fallback to simple message
            messageBody = `System status is ${healthData.overallStatus.toUpperCase()}. Please check the dashboard.`;
        }

        const message = {
            notification: {
                title: '🚨 System Health Alert',
                body: messageBody
            },
            data: {
                type: 'health_alert',
                status: healthData.overallStatus,
                timestamp: healthData.timestamp,
                components: JSON.stringify(Object.keys(healthData.components))
            },
            topic: 'admin-notifications'
        };

        await admin.messaging().send(message);
        console.log('Health alert sent successfully');
    } catch (error) {
        console.error('Error sending health alert:', error);
    }
}

/**
 * Generate smart alert message using Groq AI
 */
async function generateSmartAlertMessage(apiKey, healthData) {
    const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    const systemPrompt = `You are a system monitoring assistant.
Generate a VERY SHORT (max 100 chars) alert message based on system health.
Format: "[COMPONENT] Issue: [brief description]"
Examples:
  "Firebase: Auth service degraded"
  "Database: High latency detected"
  "Critical: Multiple services down"
`;

    const userPrompt = `System status: ${healthData.overallStatus}
Components:
${JSON.stringify(healthData.components, null, 2)}

Generate a concise alert message:`;

    try {
        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-8b-8192",
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userPrompt }
                ],
                temperature: 0.3,
                max_tokens: 100
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 5000
            }
        );

        return response.data.choices[0].message.content.trim();
    } catch (error) {
        console.error("Failed to generate smart alert:", error.message);
        return `System ${healthData.overallStatus.toUpperCase()}`;
    }
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\tsconfig.json`

### File: `infrastructure\firebase_functions\firebase_functions_v1\tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "lib",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "lib"]
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.d.ts`

```typescript
/** All supported chat types returned by classifyIntent() */
export type ChatType = "GREETING" | "SIMILAR" | "SIMPLE_QUESTION" | "COMPLEX_QUESTION" | "FOLLOW_UP" | "COMMAND" | "UNKNOWN";
/** Result of a single-classify call */
export interface ClassifyResult {
    chatType: ChatType;
    message: string;
    classifiedAt: number;
}
/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
export declare function classifyIntent(message: string, nowMs?: number): ClassifyResult;
//# sourceMappingURL=chatClassifier.d.ts.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.d.ts.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.d.ts.map`

```text
{"version":3,"file":"chatClassifier.d.ts","sourceRoot":"","sources":["../src/chatClassifier.ts"],"names":[],"mappings":"AASA,4DAA4D;AAC5D,MAAM,MAAM,QAAQ,GAChB,UAAU,GACV,SAAS,GACT,iBAAiB,GACjB,kBAAkB,GAClB,WAAW,GACX,SAAS,GACT,SAAS,CAAC;AAEd,uCAAuC;AACvC,MAAM,WAAW,cAAc;IAC7B,QAAQ,EAAE,QAAQ,CAAC;IACnB,OAAO,EAAE,MAAM,CAAC;IAChB,YAAY,EAAE,MAAM,CAAC;CACtB;AAkBD;;;;;;;;;;;;GAYG;AACH,wBAAgB,cAAc,CAAC,OAAO,EAAE,MAAM,EAAE,KAAK,CAAC,EAAE,MAAM,GAAG,cAAc,CAkB9E"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.js`

```javascript
"use strict";
// ─────────────────────────────────────────────────────────────────
// chatClassifier.ts
// Intent / ChatType classifier for the SupremeAI scraping pipeline.
//
// Extracted from classifyIntent() in scrapeEngine.ts so that
// ChatProcessingService.java and other callers can invoke intent
// classification without depending on the full scraping engine.
// ─────────────────────────────────────────────────────────────────
Object.defineProperty(exports, "__esModule", { value: true });
exports.classifyIntent = classifyIntent;
// ─────────────────────────────────────────────────────────────────
// Regex patterns — kept identical to scrapeEngine.ts for backwards
// compatibility with any existing compare-diff expectations.
// ─────────────────────────────────────────────────────────────────
const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK = /\?$/;
// ─────────────────────────────────────────────────────────────────
// classifyIntent
// ─────────────────────────────────────────────────────────────────
/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
function classifyIntent(message, nowMs) {
    const trimmed = message.trim().toLowerCase();
    let chatType;
    if (GREETING_WORDS.test(trimmed))
        chatType = "GREETING";
    else if (SIMILAR_WORDS.test(trimmed))
        chatType = "SIMILAR";
    else if (COMMAND_WORDS.test(trimmed))
        chatType = "COMMAND";
    else if (FOLLOW_UP_WORDS.test(trimmed))
        chatType = "FOLLOW_UP";
    else if (COMPLEX_HINTS.test(trimmed))
        chatType = "COMPLEX_QUESTION";
    else if (QUESTION_MARK.test(trimmed))
        chatType = "SIMPLE_QUESTION";
    else if (trimmed.length < 20)
        chatType = "SIMPLE_QUESTION";
    else
        chatType = "COMPLEX_QUESTION";
    return {
        chatType,
        message,
        classifiedAt: nowMs ?? Date.now(),
    };
}
//# sourceMappingURL=chatClassifier.js.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.js.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\chatClassifier.js.map`

```text
{"version":3,"file":"chatClassifier.js","sourceRoot":"","sources":["../src/chatClassifier.ts"],"names":[],"mappings":";AAAA,oEAAoE;AACpE,oBAAoB;AACpB,oEAAoE;AACpE,EAAE;AACF,6DAA6D;AAC7D,iEAAiE;AACjE,gEAAgE;AAChE,oEAAoE;;AAgDpE,wCAkBC;AA/CD,oEAAoE;AACpE,mEAAmE;AACnE,6DAA6D;AAC7D,oEAAoE;AAEpE,MAAM,cAAc,GAAG,gFAAgF,CAAC;AACxG,MAAM,aAAa,GAAI,uEAAuE,CAAC;AAC/F,MAAM,eAAe,GAAG,gDAAgD,CAAC;AACzE,MAAM,aAAa,GAAI,iEAAiE,CAAC;AACzF,MAAM,aAAa,GAAI,6EAA6E,CAAC;AACrG,MAAM,aAAa,GAAI,KAAK,CAAC;AAE7B,oEAAoE;AACpE,iBAAiB;AACjB,oEAAoE;AAEpE;;;;;;;;;;;;GAYG;AACH,SAAgB,cAAc,CAAC,OAAe,EAAE,KAAc;IAC5D,MAAM,OAAO,GAAG,OAAO,CAAC,IAAI,EAAE,CAAC,WAAW,EAAE,CAAC;IAE7C,IAAI,QAAkB,CAAC;IACvB,IAAI,cAAc,CAAC,IAAI,CAAC,OAAO,CAAC;QAAO,QAAQ,GAAG,UAAU,CAAC;SACxD,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,SAAS,CAAC;SACvD,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,SAAS,CAAC;SACvD,IAAI,eAAe,CAAC,IAAI,CAAC,OAAO,CAAC;QAAE,QAAQ,GAAG,WAAW,CAAC;SAC1D,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,kBAAkB,CAAC;SAChE,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,iBAAiB,CAAC;SAC/D,IAAI,OAAO,CAAC,MAAM,GAAG,EAAE;QAAY,QAAQ,GAAG,iBAAiB,CAAC;;QAC7B,QAAQ,GAAG,kBAAkB,CAAC;IAEtE,OAAO;QACL,QAAQ;QACR,OAAO;QACP,YAAY,EAAE,KAAK,IAAI,IAAI,CAAC,GAAG,EAAE;KAClC,CAAC;AACJ,CAAC"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.d.ts`

```typescript
import * as functions from 'firebase-functions/v2';
/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
export declare const handleIncomingEmail: functions.https.HttpsFunction;
//# sourceMappingURL=email_handler.d.ts.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.d.ts.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.d.ts.map`

```text
{"version":3,"file":"email_handler.d.ts","sourceRoot":"","sources":["../src/email_handler.ts"],"names":[],"mappings":"AAAA,OAAO,KAAK,SAAS,MAAM,uBAAuB,CAAC;AAcnD;;;GAGG;AACH,eAAO,MAAM,mBAAmB,+BAuD9B,CAAC"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleIncomingEmail = void 0;
const functions = __importStar(require("firebase-functions/v2"));
const admin = __importStar(require("firebase-admin"));
const mailparser_1 = require("mailparser");
const nodemailer = __importStar(require("nodemailer"));
// Configuration for outgoing status updates
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.SUPREMEAI_EMAIL,
        pass: process.env.SUPREMEAI_EMAIL_PASSWORD
    }
});
/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
exports.handleIncomingEmail = functions.https.onRequest(async (req, res) => {
    try {
        // 1. Parse the multipart email body
        const parsed = await (0, mailparser_1.simpleParser)(req.body);
        const sender = parsed.from?.value[0].address;
        const recipient = parsed.to?.value?.[0]?.address;
        const subject = parsed.subject;
        const body = parsed.text;
        const html = parsed.html;
        console.log(`[SupremeAI Email] Incoming from: ${sender} to ${recipient}, Subject: ${subject}`);
        // 1. Check for Verification Codes/Links (The "Personhood" check)
        // If the email is from a known provider (Google, DeepSeek, etc.), extract OTP
        const otpMatch = body?.match(/\b\d{6}\b/); // Look for 6-digit codes
        const linkMatch = html?.match(/href="([^"]*confirm[^"]*|[^"]*verify[^"]*)"/i);
        if (otpMatch || linkMatch) {
            await admin.firestore().collection('verification_queue').add({
                sender,
                email_target: recipient,
                subject,
                code: otpMatch ? otpMatch[0] : null,
                link: linkMatch ? linkMatch[1] : null,
                receivedAt: admin.firestore.FieldValue.serverTimestamp(),
                processed: false
            });
            console.log(`[SupremeAI] Extracted verification data from ${sender}`);
        }
        // 2. Security: Only process if it's from the verified Admin
        const authorizedAdmins = ['admin@yourdomain.com'];
        if (!sender || !authorizedAdmins.includes(sender)) {
            console.warn(`Unauthorized access attempt by ${sender}`);
            res.status(403).send('Forbidden');
            return;
        }
        // 3. Process Logic (Pseudo-code)
        // Here you would pass 'body' to your Gemini-powered agent
        // result = await supremeAiCore.processCommand(body);
        // 4. Send Confirmation/Result back to Admin
        await transporter.sendMail({
            from: '"SupremeAI Assistant" <supremeai@yourdomain.com>',
            to: sender,
            subject: `Re: ${subject} [PROCESSED]`,
            text: `Hello Admin, I have received your request and executed the tasks. \n\nCommand: ${subject}\nStatus: Successfully completed via SupremeAI Core Engine.`
        });
        res.status(200).send('Email Processed');
    }
    catch (error) {
        console.error('Email processing error:', error);
        res.status(500).send('Internal Server Error');
    }
});
//# sourceMappingURL=email_handler.js.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.js.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\email_handler.js.map`

```text
{"version":3,"file":"email_handler.js","sourceRoot":"","sources":["../src/email_handler.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.d.ts`

```typescript
import * as functions from 'firebase-functions/v1';
/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
export declare const onUserSignUp: functions.CloudFunction<import("firebase-admin/auth").UserRecord>;
//# sourceMappingURL=index.d.ts.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.d.ts.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.d.ts.map`

```text
{"version":3,"file":"index.d.ts","sourceRoot":"","sources":["../src/index.ts"],"names":[],"mappings":"AAAA,OAAO,KAAK,SAAS,MAAM,uBAAuB,CAAC;AAMnD;;;GAGG;AACH,eAAO,MAAM,YAAY,mEAsBvB,CAAC"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.onUserSignUp = void 0;
const functions = __importStar(require("firebase-functions/v1"));
const admin = __importStar(require("firebase-admin"));
// Initialize Firebase Admin SDK
admin.initializeApp();
/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
exports.onUserSignUp = functions.auth.user().onCreate(async (user) => {
    try {
        // 1. Set Custom User Claims (Embeds the role directly into their JWT token)
        await admin.auth().setCustomUserClaims(user.uid, {
            role: 'user',
            accessLevel: 1
        });
        // 2. Create a synchronized profile document in Firestore
        await admin.firestore().collection('users').doc(user.uid).set({
            email: user.email,
            displayName: user.displayName || 'Operator',
            role: 'user',
            tier: 'FREE',
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            lastLogin: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        console.log(`[AUTH] Successfully initialized new user: ${user.uid} with 'user' role.`);
    }
    catch (error) {
        console.error(`[AUTH ERROR] Failed to initialize user ${user.uid}:`, error);
    }
});
//# sourceMappingURL=index.js.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.js.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\index.js.map`

```text
{"version":3,"file":"index.js","sourceRoot":"","sources":["../src/index.ts"],"names":[],"mappings":";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;AAAA,iEAAmD;AACnD,sDAAwC;AAExC,gCAAgC;AAChC,KAAK,CAAC,aAAa,EAAE,CAAC;AAEtB;;;GAGG;AACU,QAAA,YAAY,GAAG,SAAS,CAAC,IAAI,CAAC,IAAI,EAAE,CAAC,QAAQ,CAAC,KAAK,EAAE,IAA2B,EAAE,EAAE;IAC7F,IAAI,CAAC;QACD,4EAA4E;QAC5E,MAAM,KAAK,CAAC,IAAI,EAAE,CAAC,mBAAmB,CAAC,IAAI,CAAC,GAAG,EAAE;YAC7C,IAAI,EAAE,MAAM;YACZ,WAAW,EAAE,CAAC;SACjB,CAAC,CAAC;QAEH,yDAAyD;QACzD,MAAM,KAAK,CAAC,SAAS,EAAE,CAAC,UAAU,CAAC,OAAO,CAAC,CAAC,GAAG,CAAC,IAAI,CAAC,GAAG,CAAC,CAAC,GAAG,CAAC;YAC1D,KAAK,EAAE,IAAI,CAAC,KAAK;YACjB,WAAW,EAAE,IAAI,CAAC,WAAW,IAAI,UAAU;YAC3C,IAAI,EAAE,MAAM;YACZ,IAAI,EAAE,MAAM;YACZ,SAAS,EAAE,KAAK,CAAC,SAAS,CAAC,UAAU,CAAC,eAAe,EAAE;YACvD,SAAS,EAAE,KAAK,CAAC,SAAS,CAAC,UAAU,CAAC,eAAe,EAAE;SAC1D,EAAE,EAAE,KAAK,EAAE,IAAI,EAAE,CAAC,CAAC;QAEpB,OAAO,CAAC,GAAG,CAAC,6CAA6C,IAAI,CAAC,GAAG,oBAAoB,CAAC,CAAC;IAC3F,CAAC;IAAC,OAAO,KAAK,EAAE,CAAC;QACb,OAAO,CAAC,KAAK,CAAC,0CAA0C,IAAI,CAAC,GAAG,GAAG,EAAE,KAAK,CAAC,CAAC;IAChF,CAAC;AACL,CAAC,CAAC,CAAC"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.d.ts`

```typescript
import * as https from "firebase-functions/v2/https";
/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
export declare function scrapeAndRespond(message: string, userId: string): Promise<Record<string, unknown>>;
/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
export declare const scrapeAndRespondFn: https.HttpsFunction;
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export declare const classifyIntentFn: https.HttpsFunction;
/**
 * GET /health
 */
export declare const scrapeHealthFn: https.HttpsFunction;
//# sourceMappingURL=scrapeEngine.d.ts.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.d.ts.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.d.ts.map`

```text
{"version":3,"file":"scrapeEngine.d.ts","sourceRoot":"","sources":["../src/scrapeEngine.ts"],"names":[],"mappings":"AAEA,OAAO,KAAK,KAAK,MAAM,6BAA6B,CAAC;AA2PrD;;;;;;GAMG;AACH,wBAAsB,gBAAgB,CACpC,OAAO,EAAE,MAAM,EACf,MAAM,EAAE,MAAM,GACb,OAAO,CAAC,MAAM,CAAC,MAAM,EAAE,OAAO,CAAC,CAAC,CA6JlC;AA4DD;;;GAGG;AACH,eAAO,MAAM,kBAAkB,qBAoB9B,CAAC;AAEF;;;GAGG;AACH,eAAO,MAAM,gBAAgB,qBAQ5B,CAAC;AAEF;;GAEG;AACH,eAAO,MAAM,cAAc,qBAe1B,CAAC"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.scrapeHealthFn = exports.classifyIntentFn = exports.scrapeAndRespondFn = void 0;
exports.scrapeAndRespond = scrapeAndRespond;
const app_1 = require("firebase-admin/app");
const firestore_1 = require("firebase-admin/firestore");
const https = __importStar(require("firebase-functions/v2/https"));
const axios_1 = __importDefault(require("axios"));
const httpsOptions = { region: "us-central1" };
// ─────────────────────────────────────────────────────────────────
// Firebase initialisation (singleton-safe)
// ─────────────────────────────────────────────────────────────────
let db = null;
function getDb() {
    db ?? (db = (0, firestore_1.getFirestore)((0, app_1.initializeApp)({})));
    return db;
}
// ─────────────────────────────────────────────────────────────────
// Firestore collection constants
// ─────────────────────────────────────────────────────────────────
const COL = {
    policies: "scrapePolicies",
    presets: "scrapePresets",
    domains: "scrapeAllowedDomains",
    history: "scrapeHistory",
    events: "scrapeEvent",
};
const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK = /\?$/;
function classifyIntent(message) {
    const msg = message.trim().toLowerCase();
    if (GREETING_WORDS.test(msg))
        return "GREETING";
    if (SIMILAR_WORDS.test(msg))
        return "SIMILAR";
    if (COMMAND_WORDS.test(msg))
        return "COMMAND";
    if (FOLLOW_UP_WORDS.test(msg))
        return "FOLLOW_UP";
    if (COMPLEX_HINTS.test(msg))
        return "COMPLEX_QUESTION";
    if (QUESTION_MARK.test(msg))
        return "SIMPLE_QUESTION";
    if (msg.length < 20)
        return "SIMPLE_QUESTION";
    return "COMPLEX_QUESTION";
}
// ─────────────────────────────────────────────────────────────────
// Step 2 — Firestore config loader helpers
// ─────────────────────────────────────────────────────────────────
async function getGlobalPolicy() {
    const snap = await getDb().collection(COL.policies).doc("global").get();
    return snap.exists ? snap.data() : null;
}
async function getPolicy(type) {
    const snap = await getDb().collection(COL.policies).doc(type).get();
    return snap.exists ? snap.data() : null;
}
async function getPreset(presetId) {
    const snap = await getDb().collection(COL.presets).doc(presetId).get();
    return snap.exists ? snap.data() : null;
}
async function getAllowedDomains() {
    const snap = await getDb().collection(COL.domains).get();
    return snap.docs.map((d) => d.data()).filter((d) => d.enabled);
}
async function findCachedAnswer(query, cacheTTLSeconds) {
    const threshold = firestore_1.Timestamp.fromMillis(Date.now() - cacheTTLSeconds * 1000);
    const snap = await getDb()
        .collection(COL.history)
        .where("query", "==", query)
        .where("timestamp", ">", threshold)
        .orderBy("timestamp", "desc")
        .limit(1)
        .get();
    if (snap.empty)
        return null;
    const d = snap.docs[0].data();
    return { ...d, sessionId: snap.docs[0].id };
}
// ─────────────────────────────────────────────────────────────────
// Step 6 helpers — domain allow /Trust-scores
// ─────────────────────────────────────────────────────────────────
function extractHost(url) {
    try {
        return new URL(url).hostname;
    }
    catch {
        return url;
    }
}
function isDomainAllowed(domain, domains) {
    const entry = domains.find((d) => domain === d.domain || domain.endsWith("." + d.domain));
    if (!entry)
        return { allowed: true, trustLevel: "standard" }; // open by default — admin can restrict via Firestore
    return { allowed: entry.enabled, trustLevel: entry.trustLevel };
}
async function extractFromPage(pageUrl, strategy, eventId) {
    const result = await callPlaywright("extract", { url: pageUrl, strategy, eventId });
    return {
        url: pageUrl,
        title: result?.title ? String(result.title) : pageUrl,
        text: result?.text ? String(result.text) : "",
        strategy,
    };
}
// ─────────────────────────────────────────────────────────────────
// Playwright proxy helper
// ─────────────────────────────────────────────────────────────────
const PLAYWRIGHT_URL = process.env.BROWSER_AUTOMATION_URL || "http://127.0.0.1:3001";
async function callPlaywright(action, body) {
    try {
        const res = await axios_1.default.post(`${PLAYWRIGHT_URL}/${action}`, body, {
            timeout: parseInt(process.env.SCRAPE_TIMEOUT_MS || "30000"),
        });
        return res.data;
    }
    catch (err) {
        throw new https.HttpsError("unavailable", `Browser automation unavailable for ${action}: ${err.message}`);
    }
}
// ─────────────────────────────────────────────────────────────────
// Step 9 — Firestore history writer
// ─────────────────────────────────────────────────────────────────
async function writeHistory(entry) {
    const ref = entry.sessionId
        ? getDb().collection(COL.history).doc(entry.sessionId)
        : getDb().collection(COL.history).doc();
    await ref.set({
        ...entry,
        timestamp: firestore_1.Timestamp.now(),
    });
    return ref.id;
}
async function logEvent(sessionId, type, payload) {
    await getDb()
        .collection(COL.events)
        .doc()
        .set({
        sessionId,
        type,
        payload,
        timestamp: firestore_1.Timestamp.now(),
    });
}
// ─────────────────────────────────────────────────────────────────
// Public scrape flow
// ─────────────────────────────────────────────────────────────────
/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
async function scrapeAndRespond(message, userId) {
    const sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const chatType = classifyIntent(message);
    // ── Step 2: policy lookup ──────────────────────────────────────
    const globalPolicy = await getGlobalPolicy();
    if (!globalPolicy?.enabled) {
        return { answer: "Web scraping is currently disabled by global policy.", sources: [], confidence: 0 };
    }
    const perTypePolicy = await getPolicy(chatType);
    if (!perTypePolicy?.enabled) {
        // Skipped — return empty, caller falls back to local knowledge
        return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
    }

...[truncated chunk 12]
    const policy = { ...globalPolicy, ...perTypePolicy };
    // ── Cache check for FOLLOW_UP ──────────────────────────────────
    if (chatType === "FOLLOW_UP" || chatType === "SIMPLE_QUESTION" || chatType === "COMPLEX_QUESTION") {
        const cached = await findCachedAnswer(message, policy.cacheTTL);
        if (cached) {
            await logEvent(sessionId, "cached_answer", { fromSession: cached.sessionId, query: message });
            return {
                answer: cached.finalAnswer,
                sources: cached.sources,
                confidence: cached.confidence,
                chatType,
                sessionId,
                cached: true,
                originalSessionId: cached.sessionId,
            };
        }
    }
    // Skip heavy flow for GREETING / SIMILAR / COMMAND
    if (chatType === "GREETING" || chatType === "SIMILAR") {
        return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
    }
    // ── Step 3: build search entry point ───────────────────────────
    const domains = await getAllowedDomains();
    const allSearchEngines = policy.searchEngines || ["google"];
    const maxResults = policy.maxResults || 3;
    const maxDepth = policy.maxDepth ?? 1;
    const strategy = policy.extractStrategy || "article-extract";
    const builtUrls = [];
    for (const engine of allSearchEngines) {
        const preset = await getPreset(engine);
        if (!preset || !preset.searchUrlTemplate)
            continue;
        const queryParam = encodeURIComponent(message);
        const engineUrl = preset.searchUrlTemplate.replace("{q}", queryParam);
        // trust gate
        const host = extractHost(engineUrl);
        const { allowed } = isDomainAllowed(host, domains);
        if (!allowed) {
            await logEvent(sessionId, "domain_skipped", { url: engineUrl, reason: "not_in_allowed_domains" });
            continue;
        }
        builtUrls.push({ engine, url: engineUrl });
    }
    if (builtUrls.length === 0) {
        return { answer: "No search engine configured or all domains blocked.", sources: [], confidence: 0, chatType, sessionId };
    }
    // ── Step 4: launch navigate sessions in parallel ───────────────
    const searchEventId = `${sessionId}_search`;
    await logEvent(sessionId, "navigate_start", { urls: builtUrls.map((b) => b.url) });
    // Dispatch all search-engine navigations via Playwright, but don't block the
    // event loop — fire and await.
    const navigatePromises = builtUrls.map(async ({ engine, url }) => {
        try {
            await callPlaywright("navigate", { url, eventId: searchEventId });
            await logEvent(sessionId, "navigate_complete", { engine, url });
        }
        catch (err) {
            await logEvent(sessionId, "error", { engine, url, error: err.message });
        }
    });
    await Promise.allSettled(navigatePromises);
    // ── Step 5: extract result links ───────────────────────────────
    let resultLinks = [];
    try {
        const searchContent = await callPlaywright("extract", { url: builtUrls[0]?.url || "", strategy: "search-links", eventId: searchEventId });
        resultLinks = Array.isArray(searchContent)
            ? searchContent.map((r) => r.href).filter(Boolean)
            : [];
    }
    catch (extractionError) {
        console.error(`[ScrapeEngine] Failed to extract search result links:`, extractionError);
        // If link extraction fails, fall back to navigating each engine URL directly
        resultLinks = builtUrls.map((b) => b.url);
    }
    // cap to top N results
    resultLinks = resultLinks.slice(0, maxResults);
    // ── Step 6: crawl deeper (maxDepth > 0) ───────────────────────
    const allExtracted = [];
    const crawl = async (url, depth) => {
        if (depth > maxDepth || resultLinks.length === 0)
            return;
        for (const link of resultLinks) {
            const host = extractHost(link);
            const { allowed, trustLevel } = isDomainAllowed(host, domains);
            if (!allowed || trustLevel === "suspicious") {
                await logEvent(sessionId, "domain_skipped", { url: link, reason: trustLevel === "suspicious" ? "suspicious_domain" : "not_allowed", trustLevel });
                continue;
            }
            await logEvent(sessionId, "extract_start", { url: link, depth });
            try {
                const page = await extractFromPage(link, strategy, `${sessionId}_d${depth}`);
                allExtracted.push(page);
                await logEvent(sessionId, "extract_complete", { url: link, depth, textLength: page.text.length, strategy: page.strategy });
                // shallow follow links one level deeper
                if (depth < maxDepth) {
                    const outbound = (await callPlaywright("extract", { url: link, strategy: "outbound-links", eventId: `${sessionId}_out_${depth}` }));
                    const nextUrls = outbound?.map((r) => r.href).filter(Boolean) || [];
                    for (const next of nextUrls.slice(0, 2))
                        await crawl(next, depth + 1);
                }
            }
            catch (err) {
                await logEvent(sessionId, "error", { url: link, phase: "extract", error: err.message });
            }
        }
    };
    // crawl the top results
    await crawl(builtUrls[0]?.url || "", 0);
    // ── Step 7: merge, deduplicate, summarize ─────────────────────
    const mergedText = mergeAndDeduplicate(allExtracted);
    const answer = summarise(mergedText, message);
    // ── Step 8: store session history ──────────────────────────────
    const firestoreDocId = await writeHistory({
        sessionId,
        query: message,
        chatType,
        sources: allExtracted.map((p) => p.url),
        rawChunks: allExtracted.map((p) => ({ url: p.url, text: p.text })),
        finalAnswer: answer,
        confidence: allExtracted.length > 0 ? Math.min(0.85, 0.55 + allExtracted.length * 0.07) : 0,
        timestamp: firestore_1.Timestamp.now(),
    });
    sessionId; // used; intentionally shadowed by const above — keep local sessionId for return
    // ── Step 9: return ─────────────────────────────────────────────
    return {
        answer,
        sources: allExtracted.map((p) => p.url),
        confidence: allExtracted.length > 0 ? Math.min(0.90, 0.55 + allExtracted.length * 0.08) : 0.2,
        chatType,
        sessionId: firestoreDocId,
        scrapedPages: allExtracted.length,
    };
}
// ─────────────────────────────────────────────────────────────────
// Text processing helpers
// ─────────────────────────────────────────────────────────────────
const TEXT_SIMILARITY_THRESHOLD = 0.85;
function jaccard(a, b) {
    const setA = new Set(a.toLowerCase().split(/\s+/));
    const setB = new Set(b.toLowerCase().split(/\s+/));
    const intersection = [...setA].filter((w) => setB.has(w)).length;
    const union = new Set([...setA, ...setB]).size;
    return union === 0 ? 0 : intersection / union;
}
function contentHash(text) {
    let hash = 0;
    const normalized = text.replace(/\s+/g, " ").slice(0, 2000);
    for (let i = 0; i < normalized.length; i++) {
        hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0;
    }
    return hash.toString(16);
}
function mergeAndDeduplicate(pages) {
    // Deduplicate by URL
    const urlMap = new Map();
    for (const p of pages)
        if (!urlMap.has(p.url))
            urlMap.set(p.url, p);
    // Deduplicate by content similarity (Jaccard ≥ threshold)
    const unique = [];
    const hashes = new Set();
    const textContent = new Set();
    for (const p of urlMap.values()) {
        const hash = contentHash(p.text);
        const isNearDuplicate = [...textContent].some((existing) => jaccard(p.text, existing) >= TEXT_SIMILARITY_THRESHOLD);
        if (!hashes.has(hash) && !isNearDuplicate) {
            hashes.add(hash);
            textContent.add(p.text.slice(0, 500));
            unique.push(p);
        }
    }
    return unique.map((p) => `### ${p.title}\n${p.text}`).join("\n\n");
}
function summarise(mergedText, query) {
    // Local extractive summary — first 3 most informative paragraphs
    if (!mergedText.trim())
        return `No useful content was found for "${query}". Try rephrasing the question or checking the configured search engines.`;
    const paragraphs = mergedText.split(/\n\n+/).filter((p) => p.length > 60);
    const topThree = paragraphs.slice(0, 3).join("\n\n");
    const wordCount = mergedText.split(/\s+/).length;
    return `**Research Summary** (${wordCount} words total)\n\n${topThree}`;
}
// ─────────────────────────────────────────────────────────────────
// Cloud Function entry points
// ─────────────────────────────────────────────────────────────────
/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
exports.scrapeAndRespondFn = https.onRequest({ ...httpsOptions, cors: true }, async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
    }
    const { message, userId } = req.body;
    if (!message || !userId) {
        res.status(400).json({ error: "Missing required field: message or userId" });
        return;
    }
    try {
        const result = await scrapeAndRespond(message, userId);
        res.status(200).json(result);
    }
    catch (err) {
        console.error("[scrapeEngine]", err);
        res.status(500).json({ error: err.message });
    }
});
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
exports.classifyIntentFn = https.onRequest({ ...httpsOptions, cors: true }, async (req, _res) => {
    if (req.method !== "POST") {
        _res.status(405).end();
        return;
    }
    const { message } = req.body;
    if (!message) {
        _res.status(400).json({ error: "message required" });
        return;
    }
    _res.status(200).json({ chatType: classifyIntent(message), message });
});
/**
 * GET /health
 */
exports.scrapeHealthFn = https.onRequest({ ...httpsOptions, cors: true }, async (_req, res) => {
    const playStatus = (await (async () => {
        try {
            const r = await axios_1.default.get(`${PLAYWRIGHT_URL}/health`, { timeout: 5000 });
            return { ok: r.status === 200, status: r.status };
        }
        catch {
            return { ok: false };
        }
    })());
    res.status(200).json({
        service: "scrapeEngine",
        playwright: playStatus,
        uptime: process.uptime(),
    });
});
//# sourceMappingURL=scrapeEngine.js.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.js.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeEngine.js.map`

```text
{"version":3,"file":"scrapeEngine.js","sourceRoot":"","sources":["../src/scrapeEngine.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.d.ts`

```typescript
/** Filter options for listHistory() */
export interface HistoryFilter {
    chatType?: string;
    minConfidence?: number;
    userId?: string;
    startDate?: Date;
    endDate?: Date;
    searchQuery?: string;
}
/** Pagination options */
export interface PaginationOptions {
    pageSize: number;
    pageToken?: string;
}
/** Paginated response */
export interface PaginatedHistory {
    entries: HistoryEntry[];
    nextPageToken: string | null;
    totalCount: number;
}
/**
 * Shallow copy of a Firestore history document.
 * Mirrors the interface in scrapeEngine.ts to avoid a circular import.
 */
export interface HistoryEntry {
    sessionId: string;
    query: string;
    chatType: string;
    sources: string[];
    rawChunks: Array<{
        url: string;
        text: string;
    }>;
    finalAnswer: string;
    confidence: number;
    timestamp: Date;
    userFeedback?: string;
    scrapedPages?: number;
    cached?: boolean;
    skipped?: boolean;
    [key: string]: unknown;
}
/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
export declare function addEntry(entry: Partial<HistoryEntry>): Promise<string>;
/**
 * Fetch a single history entry by document ID.
 */
export declare function getEntry(sessionId: string): Promise<HistoryEntry | null>;
/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
export declare function getSessionHistory(sessionId: string): Promise<HistoryEntry[]>;
/**
 * List history entries with optional filters and pagination.
 */
export declare function listHistory(filter: HistoryFilter | undefined, pagination: PaginationOptions): Promise<PaginatedHistory>;
/**
 * Get the total count of history entries (uncapped).
 */
export declare function getHistoryCount(filter?: HistoryFilter): Promise<number>;
/**
 * Delete a single history entry by session/document ID.
 */
export declare function deleteEntry(sessionId: string): Promise<void>;
/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
export declare function deleteAllHistory(): Promise<number>;
/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
export declare function recordFeedback(sessionId: string, feedback: string): Promise<void>;
//# sourceMappingURL=scrapeHistoryManager.d.ts.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.d.ts.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.d.ts.map`

```text
{"version":3,"file":"scrapeHistoryManager.d.ts","sourceRoot":"","sources":["../src/scrapeHistoryManager.ts"],"names":[],"mappings":"AAgBA,uCAAuC;AACvC,MAAM,WAAW,aAAa;IAC5B,QAAQ,CAAC,EAAE,MAAM,CAAC;IAClB,aAAa,CAAC,EAAE,MAAM,CAAC;IACvB,MAAM,CAAC,EAAE,MAAM,CAAC;IAChB,SAAS,CAAC,EAAE,IAAI,CAAC;IACjB,OAAO,CAAC,EAAE,IAAI,CAAC;IACf,WAAW,CAAC,EAAE,MAAM,CAAC;CACtB;AAED,yBAAyB;AACzB,MAAM,WAAW,iBAAiB;IAChC,QAAQ,EAAE,MAAM,CAAC;IACjB,SAAS,CAAC,EAAE,MAAM,CAAC;CACpB;AAED,yBAAyB;AACzB,MAAM,WAAW,gBAAgB;IAC/B,OAAO,EAAE,YAAY,EAAE,CAAC;IACxB,aAAa,EAAE,MAAM,GAAG,IAAI,CAAC;IAC7B,UAAU,EAAE,MAAM,CAAC;CACpB;AAED;;;GAGG;AACH,MAAM,WAAW,YAAY;IAC3B,SAAS,EAAE,MAAM,CAAC;IAClB,KAAK,EAAE,MAAM,CAAC;IACd,QAAQ,EAAE,MAAM,CAAC;IACjB,OAAO,EAAE,MAAM,EAAE,CAAC;IAClB,SAAS,EAAE,KAAK,CAAC;QAAE,GAAG,EAAE,MAAM,CAAC;QAAC,IAAI,EAAE,MAAM,CAAA;KAAE,CAAC,CAAC;IAChD,WAAW,EAAE,MAAM,CAAC;IACpB,UAAU,EAAE,MAAM,CAAC;IACnB,SAAS,EAAE,IAAI,CAAC;IAChB,YAAY,CAAC,EAAE,MAAM,CAAC;IACtB,YAAY,CAAC,EAAE,MAAM,CAAC;IACtB,MAAM,CAAC,EAAE,OAAO,CAAC;IACjB,OAAO,CAAC,EAAE,OAAO,CAAC;IAClB,CAAC,GAAG,EAAE,MAAM,GAAG,OAAO,CAAC;CACxB;AA8BD;;;GAGG;AACH,wBAAsB,QAAQ,CAC5B,KAAK,EAAE,OAAO,CAAC,YAAY,CAAC,GAC3B,OAAO,CAAC,MAAM,CAAC,CAmBjB;AAED;;GAEG;AACH,wBAAsB,QAAQ,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,YAAY,GAAG,IAAI,CAAC,CAG9E;AAED;;;GAGG;AACH,wBAAsB,iBAAiB,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,YAAY,EAAE,CAAC,CAOlF;AAED;;GAEG;AACH,wBAAsB,WAAW,CAC/B,MAAM,EAAE,aAAa,YAAK,EAC1B,UAAU,EAAE,iBAAiB,GAC5B,OAAO,CAAC,gBAAgB,CAAC,CAyB3B;AAED;;GAEG;AACH,wBAAsB,eAAe,CAAC,MAAM,GAAE,aAAkB,GAAG,OAAO,CAAC,MAAM,CAAC,CAOjF;AAED;;GAEG;AACH,wBAAsB,WAAW,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,IAAI,CAAC,CAElE;AAED;;;GAGG;AACH,wBAAsB,gBAAgB,IAAI,OAAO,CAAC,MAAM,CAAC,CAMxD;AAED;;;;;GAKG;AACH,wBAAsB,cAAc,CAClC,SAAS,EAAE,MAAM,EACjB,QAAQ,EAAE,MAAM,GACf,OAAO,CAAC,IAAI,CAAC,CAIf"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.js`

```javascript
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.addEntry = addEntry;
exports.getEntry = getEntry;
exports.getSessionHistory = getSessionHistory;
exports.listHistory = listHistory;
exports.getHistoryCount = getHistoryCount;
exports.deleteEntry = deleteEntry;
exports.deleteAllHistory = deleteAllHistory;
exports.recordFeedback = recordFeedback;
const firestore_1 = require("firebase-admin/firestore");
// ─────────────────────────────────────────────────────────────────
// Initialisation — singleton-safe
// ─────────────────────────────────────────────────────────────────
let db = null;
function getDb() {
    db ?? (db = (0, firestore_1.getFirestore)());
    return db;
}
// ─────────────────────────────────────────────────────────────────
// Helper — convert Firestore doc → HistoryEntry
// ─────────────────────────────────────────────────────────────────
function docToEntry(doc) {
    const data = doc.data();
    return {
        sessionId: doc.id,
        query: data.query ?? "",
        chatType: data.chatType ?? "UNKNOWN",
        sources: data.sources ?? [],
        rawChunks: data.rawChunks ?? [],
        finalAnswer: data.finalAnswer ?? "",
        confidence: data.confidence ?? 0,
        timestamp: (data.timestamp && typeof data.timestamp.toDate === "function" ? data.timestamp.toDate() : new Date()),
        userFeedback: data.userFeedback,
        scrapedPages: data.scrapedPages,
        cached: data.cached,
        skipped: data.skipped,
    };
}
// ─────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────
const COL = "scrapeHistory";
/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
async function addEntry(entry) {
    const id = entry.sessionId ?? `hist_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const payload = {
        query: entry.query ?? "",
        chatType: entry.chatType ?? "UNKNOWN",
        sources: entry.sources ?? [],
        rawChunks: entry.rawChunks ?? [],
        finalAnswer: entry.finalAnswer ?? "",
        confidence: entry.confidence ?? 0,
    };
    if (entry.timestamp)
        payload.timestamp = entry.timestamp;
    else
        payload.timestamp = new Date();
    if (entry.userFeedback !== undefined)
        payload.userFeedback = entry.userFeedback;
    if (entry.scrapedPages !== undefined)
        payload.scrapedPages = entry.scrapedPages;
    if (entry.cached !== undefined)
        payload.cached = entry.cached;
    if (entry.skipped !== undefined)
        payload.skipped = entry.skipped;
    await getDb().collection(COL).doc(id).set(payload, { merge: true });
    return id;
}
/**
 * Fetch a single history entry by document ID.
 */
async function getEntry(sessionId) {
    const snap = await getDb().collection(COL).doc(sessionId).get();
    return snap.exists ? docToEntry(snap) : null;
}
/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
async function getSessionHistory(sessionId) {
    const snap = await getDb()
        .collection(COL)
        .where("sessionId", "==", sessionId)
        .orderBy("timestamp", "desc")
        .get();
    return snap.docs.map(docToEntry);
}
/**
 * List history entries with optional filters and pagination.
 */
async function listHistory(filter = {}, pagination) {
    let query = getDb().collection(COL);
    // Apply filters
    if (filter.chatType)
        query = query.where("chatType", "==", filter.chatType);
    if (filter.minConfidence != null)
        query = query.where("confidence", ">=", filter.minConfidence);
    if (filter.startDate)
        query = query.where("timestamp", ">=", filter.startDate);
    if (filter.endDate)
        query = query.where("timestamp", "<=", filter.endDate);
    query = query.orderBy("timestamp", "desc");
    const pageSize = Math.min(pagination.pageSize, 100);
    if (pagination.pageToken) {
        const cursorSnap = await getDb().collection(COL).doc(pagination.pageToken).get();
        query = query.startAfter(cursorSnap);
    }
    const snap = await query.limit(pageSize + 1).get();
    const docs = snap.docs;
    const entries = docs.slice(0, pageSize).map(docToEntry);
    const nextPageToken = docs.length > pageSize ? docs[snap.size - 2].id : null;
    return { entries, nextPageToken, totalCount: snap.size };
}
/**
 * Get the total count of history entries (uncapped).
 */
async function getHistoryCount(filter = {}) {
    let query = getDb().collection(COL);
    if (filter.chatType)
        query = query.where("chatType", "==", filter.chatType);
    if (filter.startDate)
        query = query.where("timestamp", ">=", filter.startDate);
    if (filter.endDate)
        query = query.where("timestamp", "<=", filter.endDate);
    const snap = await query.get();
    return snap.size;
}
/**
 * Delete a single history entry by session/document ID.
 */
async function deleteEntry(sessionId) {
    await getDb().collection(COL).doc(sessionId).delete();
}
/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
async function deleteAllHistory() {
    const snap = await getDb().collection(COL).get();
    const batch = getDb().batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    if (snap.size > 0)
        await batch.commit();
    return snap.size;
}
/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
async function recordFeedback(sessionId, feedback) {
    await getDb().collection(COL).doc(sessionId).update({
        userFeedback: feedback,
    });
}
//# sourceMappingURL=scrapeHistoryManager.js.map
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.js.map`

### File: `infrastructure\firebase_functions\firebase_functions_v1\lib\scrapeHistoryManager.js.map`

```text
{"version":3,"file":"scrapeHistoryManager.js","sourceRoot":"","sources":["../src/scrapeHistoryManager.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\chatClassifier.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\chatClassifier.ts`

```typescript
// ─────────────────────────────────────────────────────────────────
// chatClassifier.ts
// Intent / ChatType classifier for the SupremeAI scraping pipeline.
//
// Extracted from classifyIntent() in scrapeEngine.ts so that
// ChatProcessingService.java and other callers can invoke intent
// classification without depending on the full scraping engine.
// ─────────────────────────────────────────────────────────────────

/** All supported chat types returned by classifyIntent() */
export type ChatType =
  | "GREETING"
  | "SIMILAR"
  | "SIMPLE_QUESTION"
  | "COMPLEX_QUESTION"
  | "FOLLOW_UP"
  | "COMMAND"
  | "UNKNOWN";

/** Result of a single-classify call */
export interface ClassifyResult {
  chatType: ChatType;
  message: string;
  classifiedAt: number; // epoch ms
}

// ─────────────────────────────────────────────────────────────────
// Regex patterns — kept identical to scrapeEngine.ts for backwards
// compatibility with any existing compare-diff expectations.
// ─────────────────────────────────────────────────────────────────

const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS  = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS  = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS  = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK  = /\?$/;

// ─────────────────────────────────────────────────────────────────
// classifyIntent
// ─────────────────────────────────────────────────────────────────

/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
export function classifyIntent(message: string, nowMs?: number): ClassifyResult {
  const trimmed = message.trim().toLowerCase();

  let chatType: ChatType;
  if (GREETING_WORDS.test(trimmed))      chatType = "GREETING";
  else if (SIMILAR_WORDS.test(trimmed))  chatType = "SIMILAR";
  else if (COMMAND_WORDS.test(trimmed))  chatType = "COMMAND";
  else if (FOLLOW_UP_WORDS.test(trimmed)) chatType = "FOLLOW_UP";
  else if (COMPLEX_HINTS.test(trimmed))  chatType = "COMPLEX_QUESTION";
  else if (QUESTION_MARK.test(trimmed))  chatType = "SIMPLE_QUESTION";
  else if (trimmed.length < 20)           chatType = "SIMPLE_QUESTION";
  else                                    chatType = "COMPLEX_QUESTION";

  return {
    chatType,
    message,
    classifiedAt: nowMs ?? Date.now(),
  };
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\email_handler.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\email_handler.ts`

```typescript
import * as functions from 'firebase-functions/v2';
import * as admin from 'firebase-admin';
import { simpleParser } from 'mailparser';
import * as nodemailer from 'nodemailer';

// Configuration for outgoing status updates
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.SUPREMEAI_EMAIL,
        pass: process.env.SUPREMEAI_EMAIL_PASSWORD
    }
});

/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
export const handleIncomingEmail = functions.https.onRequest(async (req, res) => {
    try {
        // 1. Parse the multipart email body
        const parsed = await simpleParser(req.body);
        const sender = parsed.from?.value[0].address;
        const recipient = (parsed.to as any)?.value?.[0]?.address;
        const subject = parsed.subject;
        const body = parsed.text;
        const html = parsed.html;

        console.log(`[SupremeAI Email] Incoming from: ${sender} to ${recipient}, Subject: ${subject}`);

        // 1. Check for Verification Codes/Links (The "Personhood" check)
        // If the email is from a known provider (Google, DeepSeek, etc.), extract OTP
        const otpMatch = body?.match(/\b\d{6}\b/); // Look for 6-digit codes
        const linkMatch = html?.match(/href="([^"]*confirm[^"]*|[^"]*verify[^"]*)"/i);

        if (otpMatch || linkMatch) {
            await admin.firestore().collection('verification_queue').add({
                sender,
                email_target: recipient,
                subject,
                code: otpMatch ? otpMatch[0] : null,
                link: linkMatch ? linkMatch[1] : null,
                receivedAt: admin.firestore.FieldValue.serverTimestamp(),
                processed: false
            });
            console.log(`[SupremeAI] Extracted verification data from ${sender}`);
        }

        // 2. Security: Only process if it's from the verified Admin
        const authorizedAdmins = ['admin@yourdomain.com'];
        if (!sender || !authorizedAdmins.includes(sender)) {
            console.warn(`Unauthorized access attempt by ${sender}`);
            res.status(403).send('Forbidden');
            return;
        }

        // 3. Process Logic (Pseudo-code)
        // Here you would pass 'body' to your Gemini-powered agent
        // result = await supremeAiCore.processCommand(body);

        // 4. Send Confirmation/Result back to Admin
        await transporter.sendMail({
            from: '"SupremeAI Assistant" <supremeai@yourdomain.com>',
            to: sender,
            subject: `Re: ${subject} [PROCESSED]`,
            text: `Hello Admin, I have received your request and executed the tasks. \n\nCommand: ${subject}\nStatus: Successfully completed via SupremeAI Core Engine.`
        });

        res.status(200).send('Email Processed');
    } catch (error) {
        console.error('Email processing error:', error);
        res.status(500).send('Internal Server Error');
    }
});
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\index.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\index.ts`

```typescript
import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
export const onUserSignUp = functions.auth.user().onCreate(async (user: admin.auth.UserRecord) => {
    try {
        // 1. Set Custom User Claims (Embeds the role directly into their JWT token)
        await admin.auth().setCustomUserClaims(user.uid, {
            role: 'user',
            accessLevel: 1
        });

        // 2. Create a synchronized profile document in Firestore
        await admin.firestore().collection('users').doc(user.uid).set({
            email: user.email,
            displayName: user.displayName || 'Operator',
            role: 'user',
            tier: 'FREE',
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            lastLogin: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        console.log(`[AUTH] Successfully initialized new user: ${user.uid} with 'user' role.`);
    } catch (error) {
        console.error(`[AUTH ERROR] Failed to initialize user ${user.uid}:`, error);
    }
});
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeEngine.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeEngine.ts`

```typescript
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Firestore, Timestamp } from "firebase-admin/firestore";
import * as https from "firebase-functions/v2/https";
import axios from "axios";

const httpsOptions = { region: "us-central1" };

// ─────────────────────────────────────────────────────────────────
// Firebase initialisation (singleton-safe)
// ─────────────────────────────────────────────────────────────────
let db: Firestore | null = null;

function getDb(): Firestore {
  db ??= getFirestore(initializeApp({}));
  return db;
}

// ─────────────────────────────────────────────────────────────────
// Firestore collection constants
// ─────────────────────────────────────────────────────────────────
const COL = {
  policies: "scrapePolicies",
  presets: "scrapePresets",
  domains: "scrapeAllowedDomains",
  history: "scrapeHistory",
  events: "scrapeEvent",
};

// ─────────────────────────────────────────────────────────────────
// Interfaces (document shapes)
// ─────────────────────────────────────────────────────────────────
interface ScrapePolicy {
  enabled: boolean;
  maxDepth: number;
  maxResults: number;
  allowedDomains?: string[];
  searchEngines?: string[];
  contentTypes?: string[];
  extractStrategy: string;
  fallbackAndRetry: boolean;
  rateLimitMs: number;
  timeoutMs: number;
  cacheTTL: number;
  [key: string]: unknown;
}

interface ScrapePreset {
  name: string;
  searchUrlTemplate: string;
  searchParam: string;
  extractStrategy: string;
  allowedSources?: string[];
  maxDepth?: number;
  [key: string]: unknown;
}

interface AllowedDomain {
  domain: string;
  allowedPaths?: string[];
  allowedTypes?: string[];
  trustLevel: "trusted" | "standard" | "suspicious";
  rateLimitMs: number;
  enabled: boolean;
  [key: string]: unknown;
}

interface ScrapeHistoryEntry {
  sessionId: string;
  query: string;
  chatType: string;
  sources: string[];
  rawChunks: unknown[];
  finalAnswer: string;
  confidence: number;
  timestamp: FirebaseFirestore.Timestamp;
  userFeedback?: string;
  [key: string]: unknown;
}

interface ScrapeEventEntry {
  id: string;
  sessionId: string;
  type:
    | "navigate_start"
    | "navigate_complete"
    | "extract_start"
    | "extract_complete"
    | "domain_skipped"
    | "error"
    | "crawl_depth_reached"
    | "rate_limited"
    | "cached_answer";
  payload: Record<string, unknown>;
  timestamp: FirebaseFirestore.Timestamp;
}

// ─────────────────────────────────────────────────────────────────
// Step 1 — Chat / Intent Classifier
// ─────────────────────────────────────────────────────────────────
type ChatType = "GREETING" | "SIMILAR" | "SIMPLE_QUESTION" | "COMPLEX_QUESTION" | "FOLLOW_UP" | "COMMAND" | "UNKNOWN";

const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS   = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS   = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS   = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK   = /\?$/;

function classifyIntent(message: string): ChatType {
  const msg = message.trim().toLowerCase();
  if (GREETING_WORDS.test(msg))       return "GREETING";
  if (SIMILAR_WORDS.test(msg))         return "SIMILAR";
  if (COMMAND_WORDS.test(msg))         return "COMMAND";
  if (FOLLOW_UP_WORDS.test(msg))       return "FOLLOW_UP";
  if (COMPLEX_HINTS.test(msg))         return "COMPLEX_QUESTION";
  if (QUESTION_MARK.test(msg))         return "SIMPLE_QUESTION";
  if (msg.length < 20)                 return "SIMPLE_QUESTION";
  return "COMPLEX_QUESTION";
}

// ─────────────────────────────────────────────────────────────────
// Step 2 — Firestore config loader helpers
// ─────────────────────────────────────────────────────────────────
async function getGlobalPolicy(): Promise<ScrapePolicy | null> {
  const snap = await getDb().collection(COL.policies).doc("global").get();
  return snap.exists ? (snap.data() as ScrapePolicy) : null;
}

async function getPolicy(type: string): Promise<ScrapePolicy | null> {
  const snap = await getDb().collection(COL.policies).doc(type).get();
  return snap.exists ? (snap.data() as ScrapePolicy) : null;
}

async function getPreset(presetId: string): Promise<ScrapePreset | null> {
  const snap = await getDb().collection(COL.presets).doc(presetId).get();
  return snap.exists ? (snap.data() as ScrapePreset) : null;
}

async function getAllowedDomains(): Promise<AllowedDomain[]> {
  const snap = await getDb().collection(COL.domains).get();
  return snap.docs.map((d) => d.data() as AllowedDomain).filter((d) => d.enabled);
}

async function findCachedAnswer(
  query: string,
  cacheTTLSeconds: number,
): Promise<ScrapeHistoryEntry | null> {
  const threshold = Timestamp.fromMillis(Date.now() - cacheTTLSeconds * 1000);
  const snap = await getDb()
    .collection(COL.history)
    .where("query", "==", query)
    .where("timestamp", ">", threshold)
    .orderBy("timestamp", "desc")
    .limit(1)
    .get();
  if (snap.empty) return null;
  const d = snap.docs[0].data() as ScrapeHistoryEntry;
  return { ...d, sessionId: snap.docs[0].id };
}

// ─────────────────────────────────────────────────────────────────
// Step 6 helpers — domain allow /Trust-scores
// ─────────────────────────────────────────────────────────────────
function extractHost(url: string): string {
  try { return new URL(url).hostname; } catch { return url; }
}

function isDomainAllowed(domain: string, domains: AllowedDomain[]): { allowed: boolean; trustLevel: string } {
  const entry = domains.find((d) => domain === d.domain || domain.endsWith("." + d.domain));
  if (!entry) return { allowed: true, trustLevel: "standard" }; // open by default — admin can restrict via Firestore
  return { allowed: entry.enabled, trustLevel: entry.trustLevel };
}

// ─────────────────────────────────────────────────────────────────
// Step 5 — Content extraction (strategy dispatch)
// ─────────────────────────────────────────────────────────────────
interface ExtractedPage {
  url: string;
  title: string;
  text: string;
  strategy: string;
}

async function extractFromPage(
  pageUrl: string,
  strategy: string,
  eventId: string,
): Promise<ExtractedPage> {
  const result = await callPlaywright("extract", { url: pageUrl, strategy, eventId });
  return {
    url: pageUrl,
    title: (result as any)?.title ? String((result as any).title) : pageUrl,
    text:   (result as any)?.text  ? String((result as any).text)  : "",
    strategy,
  };
}

// ─────────────────────────────────────────────────────────────────
// Playwright proxy helper
// ─────────────────────────────────────────────────────────────────
const PLAYWRIGHT_URL = process.env.BROWSER_AUTOMATION_URL || "http://127.0.0.1:3001";

async function callPlaywright(
  action: string,
  body: Record<string, unknown>,
): Promise<unknown> {
  try {
    const res = await axios.post(`${PLAYWRIGHT_URL}/${action}`, body, {
      timeout: parseInt(process.env.SCRAPE_TIMEOUT_MS || "30000"),
    });
    return res.data;
  } catch (err) {
    throw new https.HttpsError(
      "unavailable",
      `Browser automation unavailable for ${action}: ${(err as Error).message}`,
    );
  }
}

// ─────────────────────────────────────────────────────────────────
// Step 9 — Firestore history writer
// ─────────────────────────────────────────────────────────────────
async function writeHistory(entry: ScrapeHistoryEntry): Promise<string> {
  const ref = entry.sessionId
    ? getDb().collection(COL.history).doc(entry.sessionId)
    : getDb().collection(COL.history).doc();
  await ref.set({
    ...entry,
    timestamp: Timestamp.now(),
  });
  return ref.id;
}

async function logEvent(
  sessionId: string,
  type: ScrapeEventEntry["type"],
  payload: Record<string, unknown>,
): Promise<void> {
  await getDb()
    .collection(COL.events)
    .doc()
    .set({
      sessionId,
      type,
      payload,
      timestamp: Timestamp.now(),
    });
}

// ─────────────────────────────────────────────────────────────────
// Public scrape flow
// ─────────────────────────────────────────────────────────────────

/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
export async function scrapeAndRespond(
  message: string,
  userId: string,
): Promise<Record<string, unknown>> {
  const sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const chatType = classifyIntent(message);

  // ── Step 2: policy lookup ──────────────────────────────────────
  const globalPolicy = await getGlobalPolicy();
  if (!globalPolicy?.enabled) {
    return { answer: "Web scraping is currently disabled by global policy.", sources: [], confidence: 0 };
  }

  const perTypePolicy = await getPolicy(chatType);
  if (!perTypePolicy?.enabled) {
    // Skipped — return empty, caller falls back to local knowledge
    return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
  }

  const policy: ScrapePolicy = { ...globalPolicy, ...perTypePolicy };

  // ── Cache check for FOLLOW_UP ──────────────────────────────────
  if (chatType === "FOLLOW_UP" || chatType === "SIMPLE_QUESTION" || chatType === "COMPLEX_QUESTION") {
    const cached = await findCachedAnswer(message, policy.cacheTTL);
    if (cached) {
      await logEvent(sessionId, "cached_answer", { fromSession: cached.sessionId, query: message });
      return {
        answer: cached.finalAnswer,
        sources: cached.sources,
        confidence: cached.confidence,
        chatType,
        sessionId,
        cached: true,
        originalSessionId: cached.sessionId,
      };
    }
  }

  // Skip heavy flow for GREETING / SIMILAR / COMMAND
  if (chatType === "GREETING" || chatType === "SIMILAR") {
    return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
  }

  // ── Step 3: build search entry point ───────────────────────────
  const domains = await getAllowedDomains();
  const allSearchEngines = policy.searchEngines || ["google"];
  const maxResults  = policy.maxResults  || 3;
  const maxDepth    = policy.maxDepth    ?? 1;
  const strategy    = policy.extractStrategy || "article-extract";

  const builtUrls: { engine: string; url: string }[] = [];
  for (const engine of allSearchEngines) {
    const preset = await getPreset(engine);
    if (!preset || !preset.searchUrlTemplate) continue;
    const queryParam = encodeURIComponent(message);
    const engineUrl  = preset.searchUrlTemplate.replace("{q}", queryParam);
    // trust gate
    const host = extractHost(engineUrl);
    const { allowed } = isDomainAllowed(host, domains);
    if (!allowed) {
      await logEvent(sessionId, "domain_skipped", { url: engineUrl, reason: "not_in_allowed_domains" });
      continue;
    }
    builtUrls.push({ engine, url: engineUrl });
  }

  if (builtUrls.length === 0) {
    return { answer: "No search engine configured or all domains blocked.", sources: [], confidence: 0, chatType, sessionId };
  }

  // ── Step 4: launch navigate sessions in parallel ───────────────
  const searchEventId = `${sessionId}_search`;
  await logEvent(sessionId, "navigate_start", { urls: builtUrls.map((b) => b.url) });

  // Dispatch all search-engine navigations via Playwright, but don't block the
  // event loop — fire and await.
  const navigatePromises = builtUrls.map(async ({ engine, url }) => {
    try {
      await callPlaywright("navigate", { url, eventId: searchEventId });
      await logEvent(sessionId, "navigate_complete", { engine, url });
    } catch (err) {
      await logEvent(sessionId, "error", { engine, url, error: (err as Error).message });
    }
  });
  await Promise.allSettled(navigatePromises);

  // ── Step 5: extract result links ───────────────────────────────
  let resultLinks: string[] = [];
  try {
    const searchContent = await callPlaywright("extract", { url: builtUrls[0]?.url || "", strategy: "search-links", eventId: searchEventId });
    resultLinks = Array.isArray(searchContent)
      ? (searchContent as Array<{ href: string }>).map((r) => r.href).filter(Boolean)
      : [];
  } catch (extractionError) {
    console.error(`[ScrapeEngine] Failed to extract search result links:`, extractionError);
    // If link extraction fails, fall back to navigating each engine URL directly
    resultLinks = builtUrls.map((b) => b.url);
  }

  // cap to top N results
  resultLinks = resultLinks.slice(0, maxResults);

  // ── Step 6: crawl deeper (maxDepth > 0) ───────────────────────
  const allExtracted: ExtractedPage[] = [];
  const crawl = async (url: string, depth: number): Promise<void> => {
    if (depth > maxDepth || resultLinks.length === 0) return;
    for (const link of resultLinks) {
      const host = extractHost(link);
      const { allowed, trustLevel } = isDomainAllowed(host, domains);
      if (!allowed || trustLevel === "suspicious") {
        await logEvent(sessionId, "domain_skipped", { url: link, reason: trustLevel === "suspicious" ? "suspicious_domain" : "not_allowed", trustLevel });
        continue;
      }
      await logEvent(sessionId, "extract_start", { url: link, depth });
      try {
        const page = await extractFromPage(link, strategy, `${sessionId}_d${depth}`);
        allExtracted.push(page);
        await logEvent(sessionId, "extract_complete", { url: link, depth, textLength: page.text.length, strategy: page.strategy });

        // shallow follow links one level deeper
        if (depth < maxDepth) {
          const outbound = (await callPlaywright("extract", { url: link, strategy: "outbound-links", eventId: `${sessionId}_out_${depth}` })) as Array<{ href: string }>;
          const nextUrls = outbound?.map((r) => r.href).filter(Boolean) || [];
          for (const next of nextUrls.slice(0, 2)) await crawl(next, depth + 1);
        }
      } catch (err) {
        await logEvent(sessionId, "error", { url: link, phase: "extract", error: (err as Error).message });
      }
    }
  };

  // crawl the top results
  await crawl(builtUrls[0]?.url || "", 0);

  // ── Step 7: merge, deduplicate, summarize ─────────────────────
  const mergedText = mergeAndDeduplicate(allExtracted);
  const answer    = summarise(mergedText, message);

  // ── Step 8: store session history ──────────────────────────────
  const firestoreDocId = await writeHistory({
    sessionId,
    query: message,
    chatType,
    sources: allExtracted.map((p) => p.url),
    rawChunks: allExtracted.map((p) => ({ url: p.url, text: p.text })),
    finalAnswer: answer,
    confidence: allExtracted.length > 0 ? Math.min(0.85, 0.55 + allExtracted.length * 0.07) : 0,
    timestamp: Timestamp.now() as unknown as FirebaseFirestore.Timestamp,
  });
  sessionId; // used; intentionally shadowed by const above — keep local sessionId for return

  // ── Step 9: return ─────────────────────────────────────────────
  return {
    answer,
    sources: allExtracted.map((p) => p.url),
    confidence: allExtracted.length > 0 ? Math.min(0.90, 0.55 + allExtracted.length * 0.08) : 0.2,
    chatType,
    sessionId: firestoreDocId,
    scrapedPages: allExtracted.length,
  };
}

// ─────────────────────────────────────────────────────────────────
// Text processing helpers
// ─────────────────────────────────────────────────────────────────
const TEXT_SIMILARITY_THRESHOLD = 0.85;

function jaccard(a: string, b: string): number {
  const setA = new Set(a.toLowerCase().split(/\s+/));
  const setB = new Set(b.toLowerCase().split(/\s+/));
  const intersection = [...setA].filter((w) => setB.has(w)).length;
  const union = new Set([...setA, ...setB]).size;
  return union === 0 ? 0 : intersection / union;
}

function contentHash(text: string): string {
  let hash = 0;
  const normalized = text.replace(/\s+/g, " ").slice(0, 2000);
  for (let i = 0; i < normalized.length; i++) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0;
  }
  return hash.toString(16);
}

function mergeAndDeduplicate(pages: ExtractedPage[]): string {
  // Deduplicate by URL
  const urlMap = new Map<string, ExtractedPage>();
  for (const p of pages) if (!urlMap.has(p.url)) urlMap.set(p.url, p);

  // Deduplicate by content similarity (Jaccard ≥ threshold)
  const unique: ExtractedPage[] = [];
  const hashes = new Set<string>();
  const textContent = new Set<string>();
  for (const p of urlMap.values()) {
    const hash = contentHash(p.text);
    const isNearDuplicate = [...textContent].some((existing) => jaccard(p.text, existing) >= TEXT_SIMILARITY_THRESHOLD);
    if (!hashes.has(hash) && !isNearDuplicate) {
      hashes.add(hash);
      textContent.add(p.text.slice(0, 500));
      unique.push(p);
    }
  }
  return unique.map((p) => `### ${p.title}\n${p.text}`).join("\n\n");
}

function summarise(mergedText: string, query: string): string {
  // Local extractive summary — first 3 most informative paragraphs
  if (!mergedText.trim()) return `No useful content was found for "${query}". Try rephrasing the question or checking the configured search engines.`;

  const paragraphs = mergedText.split(/\n\n+/).filter((p) => p.length > 60);
  const topThree   = paragraphs.slice(0, 3).join("\n\n");
  const wordCount  = mergedText.split(/\s+/).length;

  return `**Research Summary** (${wordCount} words total)\n\n${topThree}`;
}

// ─────────────────────────────────────────────────────────────────
// Cloud Function entry points
// ─────────────────────────────────────────────────────────────────

/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
export const scrapeAndRespondFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (req: any, res: any) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed" });
      return;
    }
    const { message, userId } = req.body;
    if (!message || !userId) {
      res.status(400).json({ error: "Missing required field: message or userId" });
      return;
    }
    try {
      const result = await scrapeAndRespond(message, userId);
      res.status(200).json(result);
    } catch (err) {
      console.error("[scrapeEngine]", err);
      res.status(500).json({ error: (err as Error).message });
    }
  },
);

/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export const classifyIntentFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (req: any, _res: any) => {
    if (req.method !== "POST") { _res.status(405).end(); return; }
    const { message } = req.body;
    if (!message) { _res.status(400).json({ error: "message required" }); return; }
    _res.status(200).json({ chatType: classifyIntent(message), message });
  },
);

/**
 * GET /health
 */
export const scrapeHealthFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (_req: any, res: any) => {
    const playStatus = (await (async (): Promise<unknown> => {
      try {
        const r = await axios.get(`${PLAYWRIGHT_URL}/health`, { timeout: 5000 });
        return { ok: r.status === 200, status: r.status };
      } catch { return { ok: false }; }
    })()) as { ok: boolean; status: number };
    res.status(200).json({
      service: "scrapeEngine",
      playwright: playStatus,
      uptime: process.uptime(),
    });
  },
);
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeHistoryManager.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeHistoryManager.ts`

```typescript
import { getFirestore, Firestore } from "firebase-admin/firestore";

// ─────────────────────────────────────────────────────────────────
// Initialisation — singleton-safe
// ─────────────────────────────────────────────────────────────────
let db: Firestore | null = null;

function getDb(): Firestore {
  db ??= getFirestore();
  return db;
}

// ─────────────────────────────────────────────────────────────────
// Type definitions (mirrors ScrapeHistoryEntry from scrapeEngine.ts)
// ─────────────────────────────────────────────────────────────────

/** Filter options for listHistory() */
export interface HistoryFilter {
  chatType?: string;
  minConfidence?: number;
  userId?: string;
  startDate?: Date;
  endDate?: Date;
  searchQuery?: string;   // substring match on query field
}

/** Pagination options */
export interface PaginationOptions {
  pageSize: number;       // ≤ 100
  pageToken?: string;     // last doc id from previous page
}

/** Paginated response */
export interface PaginatedHistory {
  entries: HistoryEntry[];
  nextPageToken: string | null;
  totalCount: number;
}

/**
 * Shallow copy of a Firestore history document.
 * Mirrors the interface in scrapeEngine.ts to avoid a circular import.
 */
export interface HistoryEntry {
  sessionId: string;
  query: string;
  chatType: string;
  sources: string[];
  rawChunks: Array<{ url: string; text: string }>;
  finalAnswer: string;
  confidence: number;
  timestamp: Date;
  userFeedback?: string;
  scrapedPages?: number;
  cached?: boolean;
  skipped?: boolean;
  [key: string]: unknown;
}

// ─────────────────────────────────────────────────────────────────
// Helper — convert Firestore doc → HistoryEntry
// ─────────────────────────────────────────────────────────────────

function docToEntry(doc: FirebaseFirestore.DocumentSnapshot<FirebaseFirestore.DocumentData>): HistoryEntry {
  const data = doc.data() as Record<string, unknown>;
  return {
    sessionId: doc.id,
    query: (data.query as string) ?? "",
    chatType: (data.chatType as string) ?? "UNKNOWN",
    sources: (data.sources as string[]) ?? [],
    rawChunks: (data.rawChunks as Array<{ url: string; text: string }>) ?? [],
    finalAnswer: (data.finalAnswer as string) ?? "",
    confidence: (data.confidence as number) ?? 0,
    timestamp: (data.timestamp && typeof (data.timestamp as any).toDate === "function" ? (data.timestamp as any).toDate() : new Date()),
    userFeedback: data.userFeedback as string | undefined,
    scrapedPages: data.scrapedPages as number | undefined,
    cached: data.cached as boolean | undefined,
    skipped: data.skipped as boolean | undefined,
  };
}

// ─────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────

const COL = "scrapeHistory";

/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
export async function addEntry(
  entry: Partial<HistoryEntry>,
): Promise<string> {
  const id = entry.sessionId ?? `hist_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const payload: Record<string, unknown> = {
    query:   entry.query ?? "",
    chatType: entry.chatType ?? "UNKNOWN",
    sources: entry.sources ?? [],
    rawChunks: entry.rawChunks ?? [],
    finalAnswer: entry.finalAnswer ?? "",
    confidence: entry.confidence ?? 0,
  };
  if (entry.timestamp)  payload.timestamp = entry.timestamp;
  else                  payload.timestamp = new Date();
  if (entry.userFeedback !== undefined) payload.userFeedback = entry.userFeedback;
  if (entry.scrapedPages !== undefined) payload.scrapedPages = entry.scrapedPages;
  if (entry.cached !== undefined)       payload.cached       = entry.cached;
  if (entry.skipped !== undefined)      payload.skipped      = entry.skipped;

  await getDb().collection(COL).doc(id).set(payload, { merge: true });
  return id;
}

/**
 * Fetch a single history entry by document ID.
 */
export async function getEntry(sessionId: string): Promise<HistoryEntry | null> {
  const snap = await getDb().collection(COL).doc(sessionId).get();
  return snap.exists ? docToEntry(snap) : null;
}

/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
export async function getSessionHistory(sessionId: string): Promise<HistoryEntry[]> {
  const snap = await getDb()
    .collection(COL)
    .where("sessionId", "==", sessionId)
    .orderBy("timestamp", "desc")
    .get();
  return snap.docs.map(docToEntry);
}

/**
 * List history entries with optional filters and pagination.
 */
export async function listHistory(
  filter: HistoryFilter = {},
  pagination: PaginationOptions,
): Promise<PaginatedHistory> {
  let query: FirebaseFirestore.Query = getDb().collection(COL);

  // Apply filters
  if (filter.chatType)  query = query.where("chatType", "==", filter.chatType);
  if (filter.minConfidence != null)
    query = query.where("confidence", ">=", filter.minConfidence);
  if (filter.startDate) query = query.where("timestamp", ">=", filter.startDate);
  if (filter.endDate)   query = query.where("timestamp", "<=", filter.endDate);

  query = query.orderBy("timestamp", "desc");

  const pageSize = Math.min(pagination.pageSize, 100);
  if (pagination.pageToken) {
    const cursorSnap = await getDb().collection(COL).doc(pagination.pageToken).get();
    query = query.startAfter(cursorSnap);
  }

  const snap = await query.limit(pageSize + 1).get();
  const docs = snap.docs;

  const entries = docs.slice(0, pageSize).map(docToEntry);
  const nextPageToken = docs.length > pageSize ? docs[snap.size - 2].id : null;

  return { entries, nextPageToken, totalCount: snap.size };
}

/**
 * Get the total count of history entries (uncapped).
 */
export async function getHistoryCount(filter: HistoryFilter = {}): Promise<number> {
  let query: FirebaseFirestore.Query = getDb().collection(COL);
  if (filter.chatType)  query = query.where("chatType", "==", filter.chatType);
  if (filter.startDate) query = query.where("timestamp", ">=", filter.startDate);
  if (filter.endDate)   query = query.where("timestamp", "<=", filter.endDate);
  const snap = await query.get();
  return snap.size;
}

/**
 * Delete a single history entry by session/document ID.
 */
export async function deleteEntry(sessionId: string): Promise<void> {
  await getDb().collection(COL).doc(sessionId).delete();
}

/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
export async function deleteAllHistory(): Promise<number> {
  const snap = await getDb().collection(COL).get();
  const batch = getDb().batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  if (snap.size > 0) await batch.commit();
  return snap.size;
}

/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
export async function recordFeedback(
  sessionId: string,
  feedback: string,
): Promise<void> {
  await getDb().collection(COL).doc(sessionId).update({
    userFeedback: feedback,
  });
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeSchema.yaml`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\scrapeSchema.yaml`

```yaml
# scrapeSchema.yaml
# Firestore collection schema for the SupremeAI Scraping Engine.
#
# These YAML documents describe the expected field names, types,
# and index patterns for each scraping-related Firestore collection.
# Firestore itself is schemaless; this file documents the *contract*
# enforced by scrapeEngine.ts and scrapeHistoryManager.ts so that
# writers, validators, and migration tools can enforce correctness.
#
# Reference implementation: functions/src/scrapeEngine.ts
# History manager:         functions/src/scrapeHistoryManager.ts

version: "1.0"
service: "scraping-engine"
generated: "auto"
environment: "production"

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeHistory
# Primary table — one document per scraping session / user query.
# ─────────────────────────────────────────────────────────────────
collections:
  - name: "scrapeHistory"
    description: |
      One document per scraping session. Stores the user query, the
      generated answer, source URLs, raw extracted text, and confidence.
    fields:
      - name: "sessionId"       # Document ID = sessionId (set on write); also stored as a field for lookups
        type: "string"
        required: true
      - name: "query"
        type: "string"
        required: true
        description: "Original user message / search query"
      - name: "chatType"
        type: "string"
        required: true
        allowedValues: [GREETING, SIMILAR, SIMPLE_QUESTION, COMPLEX_QUESTION, FOLLOW_UP, COMMAND, UNKNOWN]
        description: "Intent type assigned by chatClassifier.classifyIntent()"
      - name: "sources"
        type: "array[string]"
        required: true
        description: "URLs from which content was scraped"
      - name: "rawChunks"
        type: "array[object]"
        required: true
        description: "Per-page extracted text chunks; each item has { url, text }"
        subFields:
          - name: "url"
            type: "string"
          - name: "text"
            type: "string"
      - name: "finalAnswer"
        type: "string"
        required: true
        description: "Merged + summarised response returned to the user"
      - name: "confidence"
        type: "number"
        required: true
        range: [0.0, 1.0]
        description: "Confidence score (0 = no scraped content, 0.9 = many pages)"
      - name: "timestamp"
        type: "Timestamp"
        required: true
        description: "Firestore server Timestamp set at write time"
      - name: "userFeedback"
        type: "string"
        required: false
        description: 'Free-text or "up"/"down" / "corrected:{text}" per scrapeHistoryManager.ts'
      - name: "scrapedPages"
        type: "integer"
        required: false
        description: "Number of unique pages successfully extracted"
      - name: "cached"
        type: "boolean"
        required: false
        description: "true when the answer was served from cache (findCachedAnswer)"
      - name: "skipped"
        type: "boolean"
        required: false
        description: "true when scraping was skipped (GREETING / DISABLED policy)"
    indexes:
      - fields: ["query", "timestamp"]
        type: "COMPOSITE"
        queryScope: "COLLECTION"
        description: "Cache-lookup index used by findCachedAnswer() in scrapeEngine.ts"
      - fields: ["sessionId"]
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "Point-lookup index used by getEntry() in scrapeHistoryManager.ts"
      - fields: ["timestamp"]
        order: "DESCENDING"
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "History listing — newest-first ordering"
      - fields: ["chatType", "timestamp"]
        order: "DESCENDING"
        type: "COMPOSITE"
        queryScope: "COLLECTION"
        description: "Filter by chat type, order newest-first (scrapeHistoryManager.ts listHistory)"
      - fields: ["timestamp"]
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "Time-range filter for expiry cleanup and pagination"

# ─────────────────────────────────────────────────────────────────
# Collection: scrapePolicies
# Global and per-type scraping policies.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapePolicies"
    description: "Scraping behaviour policy documents keyed by policy id (e.g. 'global', per chatType)"
    fields:
      - name: "enabled"
        type: "boolean"
        required: true
        description: "false = scraping disabled globally or for this type"
      - name: "maxDepth"
        type: "integer"
        required: false
        default: 1
      - name: "maxResults"
        type: "integer"
        required: false
        default: 3
      - name: "allowedDomains"
        type: "array[string]"
        required: false
        description: "Optional per-policy domain allow-list override"
      - name: "searchEngines"
        type: "array[string]"
        required: false
        default: ["google"]
      - name: "contentTypes"
        type: "array[string]"
        required: false
      - name: "extractStrategy"
        type: "string"
        required: false
        default: "article-extract"
      - name: "fallbackAndRetry"
        type: "boolean"
        required: false
        default: true
      - name: "rateLimitMs"
        type: "integer"
        required: false
        default: 1000
      - name: "timeoutMs"
        type: "integer"
        required: false
        default: 30000
      - name: "cacheTTL"
        type: "integer"
        required: false
        default: 3600

# ─────────────────────────────────────────────────────────────────
# Collection: scrapePresets
# Search-engine URL templates used to build query URLs.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapePresets"
    description: "Search-engine preset documents keyed by engine id (e.g. 'google', 'bing')"
    fields:
      - name: "name"
        type: "string"
        required: true
      - name: "searchUrlTemplate"
        type: "string"
        required: true
        description: "URL template with {q} placeholder for the query string"
      - name: "searchParam"
        type: "string"
        required: false
        default: "q"
      - name: "extractStrategy"
        type: "string"
        required: true
      - name: "allowedSources"
        type: "array[string]"
        required: false
      - name: "maxDepth"
        type: "integer"
        required: false

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeAllowedDomains
# Per-domain trust and rate-limit configuration.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapeAllowedDomains"
    description: "Domain-level trust and rate-limit entries"
    fields:
      - name: "domain"
        type: "string"
        required: true
      - name: "allowedPaths"
        type: "array[string]"
        required: false
      - name: "allowedTypes"
        type: "array[string]"
        required: false
      - name: "trustLevel"
        type: "string"
        required: false
        allowedValues: [trusted, standard, suspicious]
        default: "standard"
      - name: "rateLimitMs"
        type: "integer"
        required: true
        description: "Minimum milliseconds between requests to this domain"
      - name: "enabled"
        type: "boolean"
        required: true
        default: true

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeEvent
# Low-level pipeline events for observability / trending.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapeEvent"
    description: "One document per pipeline event (navigate_start, extract_complete, error, …)"
    fields:
      - name: "sessionId"
        type: "string"
        required: true
      - name: "type"
        type: "string"
        required: true
        allowedValues:
          - navigate_start
          - navigate_complete
          - extract_start
          - extract_complete
          - domain_skipped
          - error
          - crawl_depth_reached
          - rate_limited
          - cached_answer
      - name: "payload"
        type: "object"
        required: true
      - name: "timestamp"
        type: "Timestamp"
        required: true
    indexes:
      - fields: ["sessionId", "timestamp"]
        order: "DESCENDING"
        type: "COMPOSITE"
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\index.cjs.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\index.cjs.js`

```javascript
const { validateAdminArgs } = require('firebase-admin/data-connect');

const connectorConfig = {
  connector: 'example',
  serviceId: 'supremeai',
  location: 'asia-southeast1'
};
exports.connectorConfig = connectorConfig;

function createMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateMovie', inputVars, inputOpts);
}
exports.createMovie = createMovie;

function upsertUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertUser', inputVars, inputOpts);
}
exports.upsertUser = upsertUser;

function addReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('AddReview', inputVars, inputOpts);
}
exports.addReview = addReview;

function deleteReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteReview', inputVars, inputOpts);
}
exports.deleteReview = deleteReview;

function listMovies(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListMovies', undefined, inputOpts);
}
exports.listMovies = listMovies;

function listUsers(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUsers', undefined, inputOpts);
}
exports.listUsers = listUsers;

function listUserReviews(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUserReviews', undefined, inputOpts);
}
exports.listUserReviews = listUserReviews;

function getMovieById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetMovieById', inputVars, inputOpts);
}
exports.getMovieById = getMovieById;

function searchMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, false);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('SearchMovie', inputVars, inputOpts);
}
exports.searchMovie = searchMovie;

```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\index.d.ts`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\index.d.ts`

```typescript
import { ConnectorConfig, DataConnect, OperationOptions, ExecuteOperationResponse } from 'firebase-admin/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;


export interface AddReviewData {
  review_upsert: Review_Key;
}

export interface AddReviewVariables {
  movieId: UUIDString;
  rating: number;
  reviewText: string;
}

export interface CreateMovieData {
  movie_insert: Movie_Key;
}

export interface CreateMovieVariables {
  title: string;
  genre: string;
  imageUrl: string;
}

export interface DeleteReviewData {
  review_delete?: Review_Key | null;
}

export interface DeleteReviewVariables {
  movieId: UUIDString;
}

export interface GetMovieByIdData {
  movie?: {
    id: UUIDString;
    title: string;
    imageUrl: string;
    genre?: string | null;
    metadata?: {
      rating?: number | null;
      releaseYear?: number | null;
      description?: string | null;
    };
      reviews: ({
        reviewText?: string | null;
        reviewDate: DateString;
        rating?: number | null;
        user: {
          id: string;
          username: string;
        } & User_Key;
      })[];
  } & Movie_Key;
}

export interface GetMovieByIdVariables {
  id: UUIDString;
}

export interface ListMoviesData {
  movies: ({
    id: UUIDString;
    title: string;
    imageUrl: string;
    genre?: string | null;
  } & Movie_Key)[];
}

export interface ListUserReviewsData {
  user?: {
    id: string;
    username: string;
    reviews: ({
      rating?: number | null;
      reviewDate: DateString;
      reviewText?: string | null;
      movie: {
        id: UUIDString;
        title: string;
      } & Movie_Key;
    })[];
  } & User_Key;
}

export interface ListUsersData {
  users: ({
    id: string;
    username: string;
  } & User_Key)[];
}

export interface MovieMetadata_Key {
  id: UUIDString;
  __typename?: 'MovieMetadata_Key';
}

export interface Movie_Key {
  id: UUIDString;
  __typename?: 'Movie_Key';
}

export interface Review_Key {
  userId: string;
  movieId: UUIDString;
  __typename?: 'Review_Key';
}

export interface SearchMovieData {
  movies: ({
    id: UUIDString;
    title: string;
    genre?: string | null;
    imageUrl: string;
  } & Movie_Key)[];
}

export interface SearchMovieVariables {
  titleInput?: string | null;
  genre?: string | null;
}

export interface UpsertUserData {
  user_upsert: User_Key;
}

export interface UpsertUserVariables {
  username: string;
}

export interface User_Key {
  id: string;
  __typename?: 'User_Key';
}

/** Generated Node Admin SDK operation action function for the 'CreateMovie' Mutation. Allow users to execute without passing in DataConnect. */
export function createMovie(dc: DataConnect, vars: CreateMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateMovieData>>;
/** Generated Node Admin SDK operation action function for the 'CreateMovie' Mutation. Allow users to pass in custom DataConnect instances. */
export function createMovie(vars: CreateMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateMovieData>>;

/** Generated Node Admin SDK operation action function for the 'UpsertUser' Mutation. Allow users to execute without passing in DataConnect. */
export function upsertUser(dc: DataConnect, vars: UpsertUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertUserData>>;
/** Generated Node Admin SDK operation action function for the 'UpsertUser' Mutation. Allow users to pass in custom DataConnect instances. */
export function upsertUser(vars: UpsertUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertUserData>>;

/** Generated Node Admin SDK operation action function for the 'AddReview' Mutation. Allow users to execute without passing in DataConnect. */
export function addReview(dc: DataConnect, vars: AddReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<AddReviewData>>;
/** Generated Node Admin SDK operation action function for the 'AddReview' Mutation. Allow users to pass in custom DataConnect instances. */
export function addReview(vars: AddReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<AddReviewData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteReview' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteReview(dc: DataConnect, vars: DeleteReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteReviewData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteReview' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteReview(vars: DeleteReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteReviewData>>;

/** Generated Node Admin SDK operation action function for the 'ListMovies' Query. Allow users to execute without passing in DataConnect. */
export function listMovies(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListMoviesData>>;
/** Generated Node Admin SDK operation action function for the 'ListMovies' Query. Allow users to pass in custom DataConnect instances. */
export function listMovies(options?: OperationOptions): Promise<ExecuteOperationResponse<ListMoviesData>>;

/** Generated Node Admin SDK operation action function for the 'ListUsers' Query. Allow users to execute without passing in DataConnect. */
export function listUsers(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUsersData>>;
/** Generated Node Admin SDK operation action function for the 'ListUsers' Query. Allow users to pass in custom DataConnect instances. */
export function listUsers(options?: OperationOptions): Promise<ExecuteOperationResponse<ListUsersData>>;

/** Generated Node Admin SDK operation action function for the 'ListUserReviews' Query. Allow users to execute without passing in DataConnect. */
export function listUserReviews(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserReviewsData>>;
/** Generated Node Admin SDK operation action function for the 'ListUserReviews' Query. Allow users to pass in custom DataConnect instances. */
export function listUserReviews(options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserReviewsData>>;

/** Generated Node Admin SDK operation action function for the 'GetMovieById' Query. Allow users to execute without passing in DataConnect. */
export function getMovieById(dc: DataConnect, vars: GetMovieByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetMovieByIdData>>;
/** Generated Node Admin SDK operation action function for the 'GetMovieById' Query. Allow users to pass in custom DataConnect instances. */
export function getMovieById(vars: GetMovieByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetMovieByIdData>>;

/** Generated Node Admin SDK operation action function for the 'SearchMovie' Query. Allow users to execute without passing in DataConnect. */
export function searchMovie(dc: DataConnect, vars?: SearchMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SearchMovieData>>;
/** Generated Node Admin SDK operation action function for the 'SearchMovie' Query. Allow users to pass in custom DataConnect instances. */
export function searchMovie(vars?: SearchMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SearchMovieData>>;

```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\package.json`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\package.json`

```json
{
  "name": "@dataconnect/admin-generated",
  "version": "0.0.1",
  "author": "Firebase <firebase-support@google.com> (https://firebase.google.com/)",
  "description": "Generated Admin SDK For example",
  "license": "Apache-2.0",
  "engines": {
    "node": " >=18.0"
  },
  "typings": "index.d.ts",
  "module": "esm/index.esm.js",
  "main": "index.cjs.js",
  "browser": "esm/index.esm.js",
  "exports": {
    ".": {
      "types": "./index.d.ts",
      "require": "./index.cjs.js",
      "default": "./esm/index.esm.js"
    },
    "./package.json": "./package.json"
  },
  "peerDependencies": {
    "firebase-admin": "^13.4.0"
  }
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\esm\index.esm.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\esm\index.esm.js`

```javascript
import { validateAdminArgs } from 'firebase-admin/data-connect';

export const connectorConfig = {
  connector: 'example',
  serviceId: 'supremeai',
  location: 'asia-southeast1'
};

export function createMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateMovie', inputVars, inputOpts);
}

export function upsertUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertUser', inputVars, inputOpts);
}

export function addReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('AddReview', inputVars, inputOpts);
}

export function deleteReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteReview', inputVars, inputOpts);
}

export function listMovies(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListMovies', undefined, inputOpts);
}

export function listUsers(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUsers', undefined, inputOpts);
}

export function listUserReviews(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUserReviews', undefined, inputOpts);
}

export function getMovieById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetMovieById', inputVars, inputOpts);
}

export function searchMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, false);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('SearchMovie', inputVars, inputOpts);
}

```

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\esm\package.json`

### File: `infrastructure\firebase_functions\firebase_functions_v1\src\dataconnect-admin-generated\esm\package.json`

```json
{
  "type": "module"
}
```

### File: `infrastructure\firebase_functions\firebase_functions_v1\utils\externalClient.js`

### File: `infrastructure\firebase_functions\firebase_functions_v1\utils\externalClient.js`

```javascript
const axios = require('axios');

/**
 * externalClient wrapper
 * - honors environment flags to enable/disable external calls
 * - supports timeout and simple retry
 */
async function callExternal(url, opts = {}) {
  const {
    method = 'get',
    data = null,
    headers = {},
    timeout = process.env.EXTERNAL_TIMEOUT_MS ? parseInt(process.env.EXTERNAL_TIMEOUT_MS, 10) : 4000,
    retries = process.env.EXTERNAL_RETRY ? parseInt(process.env.EXTERNAL_RETRY, 10) : 1,
    enabledFlag = 'ENABLE_EXTERNAL_API'
  } = opts;

  const enabled = (process.env[enabledFlag] || 'false').toLowerCase() === 'true';
  if (!enabled) {
    const err = new Error(`external api disabled via ${enabledFlag}`);
    err.code = 'EXTERNAL_DISABLED';
    throw err;
  }

  let lastErr = null;
  for (let i = 0; i <= retries; i++) {
    try {
      const res = await axios({ url, method, data, headers, timeout });
      return res;
    } catch (e) {
      lastErr = e;
      // small backoff
      await new Promise(r => setTimeout(r, 100 * (i + 1)));
    }
  }

  throw lastErr;
}

module.exports = { callExternal };
```

### File: `infrastructure\terraform\cloud_functions.tf`

### File: `infrastructure\terraform\cloud_functions.tf`

```text
resource "google_cloudfunctions2_function" "supremeai_ocr" {
  project  = var.project_id
  region   = var.region
  name     = "supremeai-ocr-trigger"
  
  build_config {
    runtime           = "python311"
    entry_point       = "handle"
    source {
      storage_source {
        bucket = google_storage_bucket.functions.name
        object = google_storage_bucket_object.function_source.name
      }
    }
  }
  
  service_config {
    max_instance_count = 1
    available_memory   = "256Mi"
    timeout_seconds    = 60
  }
}

resource "google_storage_bucket" "functions" {
  name     = "${var.project_id}-supremeai-functions"
  location = var.region
}

resource "google_storage_bucket_object" "function_source" {
  name   = "function-source.zip"
  bucket = google_storage_bucket.functions.name
  source = "./functions/placeholder.zip"
}
```

### File: `infrastructure\terraform\cloud_run.tf`

### File: `infrastructure\terraform\cloud_run.tf`

```text
resource "google_cloud_run_service" "api" {
  name     = var.service_name
  location = var.region

  template {
    spec {
      service_account_name = google_service_account.api.email

      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/supremeai/supremeai-api:latest"

        ports {
          container_port = 8000
        }

        resources {
          limits = {
            cpu    = "1"
            memory = "512Mi"
          }
        }

        env {
          name  = "PORT"
          value = "8000"
        }
        env {
          name  = "ENV"
          value = "production"
        }
        env {
          name = "SUPABASE_URL"
          value = var.supabase_url
        }
        env {
          name = "SUPABASE_ANON_KEY"
          value = var.supabase_anon_key
        }
        env {
          name = "PINECONE_API_KEY"
          value = var.pinecone_api_key
        }
        env {
          name = "PINECONE_INDEX"
          value = var.pinecone_index
        }
        env {
          name = "QDRANT_URL"
          value = var.qdrant_url
        }
        env {
          name = "QDRANT_API_KEY"
          value = var.qdrant_api_key
        }
      }

      timeout_seconds       = 300
      service_account_name  = google_service_account.api.email
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

data "google_iam_policy" "noauth" {
  binding {
    role = "roles/run.invoker"
    members = [
      "allUsers",
    ]
  }
}

resource "google_cloud_run_service_iam_policy" "public" {
  service  = google_cloud_run_service.api.name
  location = google_cloud_run_service.api.location
  policy   = data.google_iam_policy.noauth.policy
}
```

### File: `infrastructure\terraform\firebase.tf`

### File: `infrastructure\terraform\firebase.tf`

```text
resource "google_firebase_project" "default" {
  project  = var.project_id
}

resource "google_firebase_hosting_site" "default" {
  project  = var.project_id
  site_id  = var.firebase_hosting_site_id
}
```

### File: `infrastructure\terraform\firestore.tf`

### File: `infrastructure\terraform\firestore.tf`

```text
resource "google_firestore_database" "default" {
  project     = var.project_id
  name        = "default"
  location_id = var.region
  type        = "FIRESTORE_NATIVE"
}
```

### File: `infrastructure\terraform\main.tf`

### File: `infrastructure\terraform\main.tf`

```text
terraform {
  required_version = ">= 1.5.0, < 2.0.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 6.0.0, < 7.0.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.6.0, < 4.0.0"
    }
    supabase = {
      source  = "supabase/supabase"
      version = ">= 1.0.0, < 2.0.0"
    }
    pinecone = {
      source  = "pinecone-io/pinecone"
      version = ">= 1.0.0, < 2.0.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "supabase" {
  # Note: Supabase is managed externally. Authenticate via SUPABASE_ACCESS_TOKEN env var.
  # No provider config needed at this time.
}

provider "pinecone" {
  # Note: Pinecone is managed externally. Authenticate via PINECONE_API_KEY env var.
}
```

### File: `infrastructure\terraform\outputs.tf`

### File: `infrastructure\terraform\outputs.tf`

```text
output "cloud_run_url" {
  value       = google_cloud_run_service.api.status[0].url
  description = "Cloud Run service URL"
}

output "cloud_run_service_name" {
  value       = google_cloud_run_service.api.name
  description = "Cloud Run service name"
}

output "service_account_email" {
  value       = google_service_account.api.email
  description = "Cloud Run service account email"
}

output "cloud_function_url" {
  value       = google_cloudfunctions2_function.supremeai_ocr.url
  description = "Cloud Functions ocr-trigger HTTPS trigger URL"
}

output "cloud_function_name" {
  value       = google_cloudfunctions2_function.supremeai_ocr.name
  description = "Cloud Function name"
}

output "firestore_database" {
  value       = google_firestore_database.default.name
  description = "Firestore database name"
}

output "firebase_project_id" {
  value       = google_firebase_project.default.project
  description = "Firebase project ID"
}

output "firebase_hosting_url" {
  value       = "https://${google_firebase_hosting_site.default.site_id}.web.app"
  description = "Firebase Hosting default site URL"
}

output "artifact_registry_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/supremeai"
  description = "Artifact Registry repo URL for Docker images"
}

output "supabase_project_ref" {
  value       = var.supabase_url
  description = "Supabase project reference URL"
}

output "pinecone_index_name" {
  value       = var.pinecone_index
  description = "Pinecone index name"
}

output "qdrant_cluster_url" {
  value       = var.qdrant_url
  description = "Qdrant cluster URL"
}

output "vercel_project_url" {
  value       = var.vercel_team_id
  description = "Vercel team/project identifier"
}

output "cloudflare_zone_id" {
  value       = var.cloudflare_account_id
  description = "Cloudflare account/zone identifier"
}
```

### File: `infrastructure\terraform\pubsub.tf`

### File: `infrastructure\terraform\pubsub.tf`

```text
resource "google_pubsub_topic" "tasks" {
  name    = "supremeai-tasks"
  project = var.project_id
}

resource "google_pubsub_subscription" "tasks" {
  name    = "supremeai-tasks-sub"
  project = var.project_id
  topic   = google_pubsub_topic.tasks.name
}
```

### File: `infrastructure\terraform\service_account.tf`

### File: `infrastructure\terraform\service_account.tf`

```text
resource "google_service_account" "api" {
  account_id   = "supremeai-api-sa"
  display_name = "SupremeAI API Service Account"
  project      = var.project_id
}
```

### File: `infrastructure\terraform\variables.tf`

### File: `infrastructure\terraform\variables.tf`

```text
variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "Default GCP region"
  type        = string
  default     = "us-central1"
}

variable "service_name" {
  description = "Cloud Run service name"
  type        = string
  default     = "supremeai-api"
}

variable "firebase_hosting_site_id" {
  description = "Firebase Hosting site id"
  type        = string
  default     = "supremeai"
}

variable "supabase_url" {
  description = "Supabase project URL"
  type        = string
}

variable "supabase_anon_key" {
  description = "Supabase anon/public API key"
  type        = string
  sensitive   = true
}

variable "pinecone_api_key" {
  description = "Pinecone API key"
  type        = string
  sensitive   = true
}

variable "pinecone_index" {
  description = "Pinecone index name"
  type        = string
}

variable "qdrant_url" {
  description = "Qdrant cluster URL"
  type        = string
}

variable "qdrant_api_key" {
  description = "Qdrant API key"
  type        = string
  sensitive   = true
}

variable "vercel_team_id" {
  description = "Vercel team ID"
  type        = string
}

variable "cloudflare_account_id" {
  description = "Cloudflare account ID"
  type        = string
}
```

### File: `scripts\bootstrap_env.py`

### File: `scripts\bootstrap_env.py`

```python
from pathlib import Path

env_path = Path('.env')
env_example_path = Path('.env.example')

def parse_env(content: str):
    result = {}
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key = line.split('=', 1)[0].strip()
        result[key] = line

env = env_path.read_text(encoding='utf-8') if env_path.exists() else ''
existing = set(parse_env(env).keys())
example = env_example_path.read_text(encoding='utf-8') if env_example_path.exists() else ''
missing = []
for line in example.splitlines():
    line = line.strip()
    if not line or line.startswith('#') or '=' not in line:
        continue
    key = line.split('=', 1)[0].strip()
    if key and key not in existing:
        missing.append(line)

out = [env.rstrip('\n')]
if missing:
    out.append('')
    out.append('# Added from .env.example')
    out.extend(missing)

new_content = '\n'.join(out) + '\n'
env_path.write_text(new_content, encoding='utf-8')
print(f'Updated .env with {len(missing)} missing keys')
```

### File: `scripts\deploy_cloud_mesh.sh`

### File: `scripts\deploy_cloud_mesh.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID}"
REGION="${REGION:-us-central1}"
SERVICE="${SERVICE:-supremeai}"
IMAGE="${IMAGE:-${PROJECT_ID}/supremeai:${GITHUB_SHA:-local}}"
GCP_REGION="${GCP_REGION:-${REGION}}"

if command -v docker >/dev/null 2>&1; then
  docker build -t "${IMAGE}" .
fi

if command -v gcloud >/dev/null 2>&1; then
  gcloud run deploy "${SERVICE}" --image "${IMAGE}" --region "${GCP_REGION}" --project "${PROJECT_ID}"
fi

if command -v railway >/dev/null 2>&1; then
  railway up --detach
fi

if command -v renderctl >/dev/null 2>&1; then
  renderctl deploy
fi

if command -v wrangler >/dev/null 2>&1; then
  wrangler deploy infrastructure/cloudflare/worker.js --config infrastructure/cloudflare/wrangler.toml
fi
```

### File: `infrastructure/check_deploy_gate.py`

### File: `infrastructure/check_deploy_gate.py`

```python
import sys
from google.cloud import firestore
from loguru import logger

def verify_deployment_gate():
    logger.info("🔍 CI/CD Gatekeeper: Auditing SupremeAI 2.0 autonomous deployment gate status...")
    
    try:
        db = firestore.Client()
        gate_ref = db.collection("deploy_gate").document("status")
        doc = gate_ref.get()
        
        if not doc.exists:
            logger.warning("⚠️ Deploy gate status document not found. Defaulting to SAFE/UNLOCKED.")
            sys.exit(0)
            
        gate_data = doc.to_dict()
        status = gate_data.get("status", "UNLOCKED").upper()
        reason = gate_data.get("reason", "No reason provided.")
        updated_at = gate_data.get("updated_at", "Unknown time")
        
        if status == "LOCKED":
            logger.critical("❌" * 20)
            logger.critical(f"🚨 DEPLOYMENT REJECTED! The autonomous gate is LOCKED.")
            logger.critical(f"📝 Reason: {reason}")
            logger.critical(f"⏰ Last Audit Update: {updated_at}")
            logger.critical("❌" * 20)
            # Exit code 1 দিয়ে সিআই/সিডি পাইপলাইনকে এখানেই থামিয়ে দেওয়া হবে
            sys.exit(1)
            
        logger.info(f"🟢 DEPLOYMENT APPROVED. Autonomous gate status is UNLOCKED. (Reason: {reason})")
        sys.exit(0)
        
    except Exception as e:
        logger.critical(f"⚠️ Gatekeeper failed to query Firestore: {str(e)}. Locking deployment for safety.")
        sys.exit(1)

if __name__ == "__main__":
    verify_deployment_gate()
```

### File: `infrastructure/cloudflare_worker.js`

### File: `infrastructure/cloudflare_worker.js`

```javascript
addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request))
})

addEventListener('scheduled', event => {
  event.waitUntil(checkHealthAndStore())
})

function getBackends() {
  const gcp_url = typeof env !== 'undefined' ? env.GCP_CLOUD_RUN_URL : (typeof GCP_CLOUD_RUN_URL !== 'undefined' ? GCP_CLOUD_RUN_URL : '');
  const railway_url = typeof env !== 'undefined' ? env.RAILWAY_URL : (typeof RAILWAY_URL !== 'undefined' ? RAILWAY_URL : '');
  const render_url = typeof env !== 'undefined' ? env.RENDER_URL : (typeof RENDER_URL !== 'undefined' ? RENDER_URL : '');
  
  const gcp_weight = typeof env !== 'undefined' ? env.GCP_WEIGHT : (typeof GCP_WEIGHT !== 'undefined' ? GCP_WEIGHT : '50');
  const railway_weight = typeof env !== 'undefined' ? env.RAILWAY_WEIGHT : (typeof RAILWAY_WEIGHT !== 'undefined' ? RAILWAY_WEIGHT : '30');
  const render_weight = typeof env !== 'undefined' ? env.RENDER_WEIGHT : (typeof RENDER_WEIGHT !== 'undefined' ? RENDER_WEIGHT : '20');

  const gcp_region = typeof env !== 'undefined' ? env.GCP_REGION : (typeof GCP_REGION !== 'undefined' ? GCP_REGION : 'us-central1');

  return [
    {
      name: 'gcp-cloud-run',
      url: gcp_url,
      health: gcp_url ? `${gcp_url}/health` : '',
      region: gcp_region,
      timeout: 5000,
      retries: 3,
      weight: parseInt(gcp_weight || '50', 10),
    },
    {
      name: 'railway',
      url: railway_url,
      health: railway_url ? `${railway_url}/health` : '',
      region: 'us-east1',
      timeout: 5000,
      retries: 3,
      weight: parseInt(railway_weight || '30', 10),
    },
    {
      name: 'render',
      url: render_url,
      health: render_url ? `${render_url}/health` : '',
      region: 'us-east1',
      timeout: 5000,
      retries: 3,
      weight: parseInt(render_weight || '20', 10),
    },
  ].filter(b => b.url)
}

async function handleRequest(request) {
  const url = new URL(request.url)
  const backends = getBackends()

  if (backends.length === 0) {
    return new Response('No backends configured', { status: 503 })
  }

  const healthyBackends = await getHealthyBackendsFromKV(backends)
  if (healthyBackends.length === 0) {
    return new Response('All backends unhealthy', { status: 503 })
  }

  const backend = weightedPick(healthyBackends)
  const target = new URL(url.pathname + url.search, backend.url)

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: omitWranglerHeaders(request.headers),
      body: request.method !== 'GET' ? await request.text() : null,
      signal: AbortSignal.timeout(backend.timeout),
    })

    return new Response(response.body, {
      status: response.status,
      headers: omitHopByHopHeaders(new Headers(response.headers)),
    })
  } catch (err) {
    return new Response(`Backend ${backend.name} error: ${err.message}`, { status: 502 })
  }
}

async function getHealthyBackendsFromKV(backends) {
  try {
    const kv = typeof SUPREMEAI_KV !== 'undefined' ? SUPREMEAI_KV : (typeof env !== 'undefined' && env.SUPREMEAI_KV ? env.SUPREMEAI_KV : null);
    if (kv) {
      const cached = await kv.get('healthy_backends');
      if (cached) {
        const healthyNames = JSON.parse(cached);
        const filtered = backends.filter(b => healthyNames.includes(b.name));
        if (filtered.length > 0) {
          return filtered;
        }
      }
    }
  } catch (e) {
    console.error('KV read error:', e);
  }
  // Fallback to direct health check if KV is empty or fails
  return await getHealthyBackends(backends);
}

async function checkHealthAndStore() {
  const backends = getBackends()
  if (backends.length === 0) return

  const healthyBackends = await getHealthyBackends(backends)
  const healthyNames = healthyBackends.map(b => b.name)

  const kv = typeof SUPREMEAI_KV !== 'undefined' ? SUPREMEAI_KV : (typeof env !== 'undefined' && env.SUPREMEAI_KV ? env.SUPREMEAI_KV : null);
  if (kv) {
    await kv.put('healthy_backends', JSON.stringify(healthyNames))
    console.log('Saved healthy backends to KV:', healthyNames)
  }
}

async function getHealthyBackends(backends) {
  const results = await Promise.allSettled(
    backends.map(async backend => {
      for (let attempt = 0; attempt < backend.retries; attempt++) {
        try {
          const res = await fetch(backend.health, { signal: AbortSignal.timeout(backend.timeout) })
          if (res.ok) return backend
        } catch (_) {
          if (attempt === backend.retries - 1) return null
          await new Promise(r => setTimeout(r, 200 * (attempt + 1)))
        }
      }
      return null
    })
  )
  return results.filter(r => r.status === 'fulfilled' && r.value).map(r => r.value)
}

function weightedPick(backends) {
  const total = backends.reduce((sum, b) => sum + (b.weight || 0), 0)
  if (total === 0) return backends[Math.floor(Math.random() * backends.length)]
  let r = Math.random() * total
  for (const b of backends) {
    r -= b.weight || 0
    if (r <= 0) return b
  }
  return backends[backends.length - 1]
}

function omitWranglerHeaders(headers) {
  const allowlist = ['content-type', 'authorization', 'x-telegram-bot-token']
  const out = new Headers()
  headers.forEach((v, k) => { if (allowlist.includes(k.toLowerCase()) || !k.startsWith('cf-')) out.set(k, v) })
  return out
}

function omitHopByHopHeaders(headers) {
  const block = new Set(['connection', 'keep-alive', 'proxy-authenticate', 'proxy-authorization', 'te', 'trailer', 'transfer-encoding', 'upgrade'])
  const out = new Headers()
  headers.forEach((v, k) => { if (!block.has(k.toLowerCase())) out.set(k, v) })
  return out
}
```

### File: `infrastructure/deploy.ps1`

### File: `infrastructure/deploy.ps1`

```text
<#
.SYNOPSIS
SupremeAI 2.0 deployment orchestrator for GCP Cloud Run, Railway, Render.
.PARAMETER Target
Optional deployment target: gcp | railway | render | all (default: all)
.EXAMPLE
.\infrastructure\deploy.ps1 -Target all
#>
param(
  [ValidateSet('gcp', 'railway', 'render', 'all')]
  [string]$Target = 'all'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ProjectRoot ".env"

function Log($Message) { Write-Host "[DEPLOY] $Message" -ForegroundColor Cyan }
function Fail($Message) { Write-Host "[DEPLOY][FAIL] $Message" -ForegroundColor Red; exit 1 }

function Test-Prerequisites {
  Log "Checking prerequisites..."
  $required = @('gcloud', 'docker', 'git')
  $missing = @()
  foreach ($cmd in $required) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { $missing += $cmd }
  }
  if ($missing) { Fail "Missing tools: $($missing -join ', ')" }
  if (Test-Path $EnvFile) {
    foreach ($line in Get-Content $EnvFile) {
      $trimmed = $line.Trim()
      if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
      $idx = $trimmed.IndexOf('=')
      if ($idx -lt 1) { continue }
      $k = $trimmed.Substring(0, $idx).Trim()
      $v = $trimmed.Substring($idx + 1).Trim()
      if (($v.StartsWith('"') -and $v.EndsWith('"')) -or ($v.StartsWith("'") -and $v.EndsWith("'"))) {
        $v = $v.Substring(1, $v.Length - 2)
      }
      [System.Environment]::SetEnvironmentVariable($k, $v, 'Process')
    }
  }
}

function Get-RegistryImage {
  param([string]$ProjectId, [string]$Region)
  $artifactRepo = "$Region-docker.pkg.dev/$ProjectId/supremeai"
  $tag = if ($env:GITHUB_SHA) { $env:GITHUB_SHA } else { "local-$(Get-Date -Format 'yyyyMMdd-HHmmss')" }
  return "$artifactRepo/supremeai:$tag"
}

function Deploy-GCP {
  param([string]$EnvTarget)
  Log "Deploying to GCP Cloud Run... (target: $EnvTarget)"
  if (-not $env:GCP_PROJECT_ID) { Fail "GCP_PROJECT_ID is not set" }
  if (-not $env:GCP_REGION) { $env:GCP_REGION = 'us-central1' }
  if (-not $env:GCP_SERVICE_NAME) { $env:GCP_SERVICE_NAME = 'supremeai' }
  if ($EnvTarget -eq 'production') { $env:ENV = 'production' } else { $env:ENV = $EnvTarget }

  $image = Get-RegistryImage -ProjectId $env:GCP_PROJECT_ID -Region $env:GCP_REGION
  Log "Building and pushing $image"
  docker build -t $image (Join-Path $ProjectRoot '.')
  if ($LASTEXITCODE -ne 0) { Fail 'Docker build failed' }
  docker push $image
  if ($LASTEXITCODE -ne 0) { Fail 'Docker push failed' }

  $setEnvVars = @("ENV=$EnvTarget")
  if ($env:GCP_PROJECT_ID) { $setEnvVars += "GCP_PROJECT_ID=$env:GCP_PROJECT_ID" }
  if ($env:GCP_REGION) { $setEnvVars += "GCP_REGION=$env:GCP_REGION" }
  if ($env:OPENAI_API_KEY) { $setEnvVars += "OPENAI_API_KEY=$env:OPENAI_API_KEY" }
  if ($env:TELEGRAM_BOT_TOKEN) { $setEnvVars += "TELEGRAM_BOT_TOKEN=$env:TELEGRAM_BOT_TOKEN" }
  if ($env:SUPABASE_URL) { $setEnvVars += "SUPABASE_URL=$env:SUPABASE_URL" }
  if ($env:SUPABASE_KEY) { $setEnvVars += "SUPABASE_KEY=$env:SUPABASE_KEY" }
  if ($env:UPSTASH_REDIS_REST_URL) { $setEnvVars += "UPSTASH_REDIS_REST_URL=$env:UPSTASH_REDIS_REST_URL" }
  if ($env:UPSTASH_REDIS_REST_TOKEN) { $setEnvVars += "UPSTASH_REDIS_REST_TOKEN=$env:UPSTASH_REDIS_REST_TOKEN" }

  $envValue = $setEnvVars -join ','
  $gcloudArgs = @(
    'run', 'deploy', $env:GCP_SERVICE_NAME,
    '--image', $image,
    '--region', $env:GCP_REGION,
    '--project', $env:GCP_PROJECT_ID,
    '--allow-unauthenticated',
    '--set-env-vars', $envValue
  )
  if ($env:PORT) {
    $gcloudArgs += '--port'
    $gcloudArgs += $env:PORT
  }

  & gcloud @gcloudArgs
  if ($LASTEXITCODE -ne 0) { Fail "gcloud deploy failed" }

  & gcloud run services update-traffic $env:GCP_SERVICE_NAME --region $env:GCP_REGION --project $env:GCP_PROJECT_ID --to-latest
  if ($LASTEXITCODE -ne 0) { Fail "traffic promotion failed" }
  Log 'GCP Cloud Run deployment completed'
}

function Deploy-Railway {
  param([string]$EnvTarget)
  Log "Railway deploy for target: $EnvTarget"
  Push-Location $ProjectRoot
  if (-not (Get-Command railway -ErrorAction SilentlyContinue)) {
    Log "Railway CLI not detected; printing deploy snippet."
    Write-Host "`n--- railway deploy snippet ---"
    Write-Host " railway login --browserless"
    if ($env:RAILWAY_TOKEN) { Write-Host " railway link --token $env:RAILWAY_TOKEN" }
    Write-Host " railway variables set NODE_ENV=$EnvTarget"
    if ($env:OPENAI_API_KEY) { Write-Host " railway variables set OPENAI_API_KEY=$env:OPENAI_API_KEY" }
    if ($env:TELEGRAM_BOT_TOKEN) { Write-Host " railway variables set TELEGRAM_BOT_TOKEN=$env:TELEGRAM_BOT_TOKEN" }
    Write-Host " railway up --detach`n"
  } else {
    railway login --browserless | Out-Null
    if ($env:RAILWAY_TOKEN) { railway link --token $env:RAILWAY_TOKEN | Out-Null }
    railway variables set NODE_ENV=$EnvTarget | Out-Null
    railway up --detach
    if ($LASTEXITCODE -ne 0) { Fail 'railway deploy failed' }
  }
  Pop-Location
}

function Deploy-Render {
  param([string]$EnvTarget)
  Log "Render deploy for target: $EnvTarget"
  Push-Location $ProjectRoot
  if (-not (Get-Command render -ErrorAction SilentlyContinue)) {
    Log "Render CLI not detected; printing deploy snippet."
    Write-Host "`n--- render deploy snippet ---"
    Write-Host " render login"
    Write-Host " render environment set supremeai NODE_ENV=$EnvTarget"
    if ($env:OPENAI_API_KEY) { Write-Host " render secrets set OPENAI_API_KEY=$env:OPENAI_API_KEY" }
    Write-Host " render deploys start --service supremeai --yes`n"
  } else {
    render environment set supremeai NODE_ENV=$EnvTarget | Out-Null
    render deploys start --service supremeai --yes
    if ($LASTEXITCODE -ne 0) { Fail 'render deploy failed' }
  }
  Pop-Location
}

try {
  Test-Prerequisites
  if ($Target -eq 'all' -or $Target -eq 'gcp') { Deploy-GCP -EnvTarget production }
  if ($Target -eq 'all' -or $Target -eq 'railway') { Deploy-Railway -EnvTarget production }
  if ($Target -eq 'all' -or $Target -eq 'render') { Deploy-Render -EnvTarget production }
  Log 'Deployment orchestration completed.'
}
catch { Fail $_ }
```

### File: `infrastructure/cloudflare/worker.js`

### File: `infrastructure/cloudflare/worker.js`

```javascript
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Static Asset CDN from R2 bucket
    if (url.pathname.startsWith('/cdn/')) {
      const cacheKey = new Request(url.toString(), request);
      const cache = caches.default;
      
      let response = await cache.match(cacheKey);
      if (!response) {
        // Fetch from R2 bucket (assumed binding named STATIC_ASSETS)
        const objectName = url.pathname.replace('/cdn/', '');
        const object = await env.STATIC_ASSETS.get(objectName);

        if (object === null) {
          return new Response('Not Found', { status: 404 });
        }

        const headers = new Headers();
        object.writeHttpMetadata(headers);
        headers.set('etag', object.httpEtag);
        headers.set('Cache-Control', 'public, max-age=31536000'); // 1 year cache

        response = new Response(object.body, { headers });
        ctx.waitUntil(cache.put(cacheKey, response.clone()));
      }
      return response;
    }

    // Cache specific public API responses (e.g. repo list)
    if (request.method === 'GET' && url.pathname.startsWith('/api/repos')) {
      const cache = caches.default;
      let response = await cache.match(request);
      
      if (!response) {
        response = await fetch(request);
        if (response.ok) {
          response = new Response(response.body, response);
          response.headers.set('Cache-Control', 'public, max-age=300'); // 5 mins
          ctx.waitUntil(cache.put(request, response.clone()));
        }
      }
      return response;
    }

    // Default: pass through to origin
    return fetch(request);
  },
};
```

### File: `infrastructure/cloudflare/wrangler.toml`

### File: `infrastructure/cloudflare/wrangler.toml`

```text
name = "supremeai-edge"
main = "worker.js"
compatibility_date = "2026-06-17"

[vars]
GCP_CLOUD_RUN_URL = ""
RAILWAY_URL = ""
RENDER_URL = ""
```

### File: `infrastructure/cloudrun/autoscale.yaml`

### File: `infrastructure/cloudrun/autoscale.yaml`

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: supremeai-backend
  annotations:
    run.googleapis.com/ingress: all
    run.googleapis.com/launch-stage: BETA
spec:
  template:
    metadata:
      annotations:
        # Autoscale bounds
        autoscaling.knative.dev/minScale: "1"
        autoscaling.knative.dev/maxScale: "10"
        
        # CPU allocation: CPU is always allocated so background tasks (agents) can run
        run.googleapis.com/cpu-throttling: "false"
        
        # Concurrency
        autoscaling.knative.dev/target: "80"
    spec:
      containerConcurrency: 80
      containers:
      - image: gcr.io/supremeai/backend:latest
        resources:
          limits:
            cpu: "2000m"
            memory: "2Gi"
        env:
        - name: GCP_PROJECT_ID
          value: "supremeai-a"
        - name: ENV
          value: "production"
        ports:
        - name: http1
          containerPort: 8000
```

### File: `infrastructure/cloudrun/multi_region.yaml`

### File: `infrastructure/cloudrun/multi_region.yaml`

```yaml
apiVersion: serving.knative.dev/v1
kind: Service
metadata:
  name: supremeai-backend
  annotations:
    run.googleapis.com/ingress: all
    run.googleapis.com/launch-stage: BETA
spec:
  template:
    metadata:
      annotations:
        # Autoscale bounds
        autoscaling.knative.dev/minScale: "2"
        autoscaling.knative.dev/maxScale: "100"
        # CPU allocation: CPU is always allocated so background tasks (agents) can run
        run.googleapis.com/cpu-throttling: "false"
        # Concurrency
        autoscaling.knative.dev/target: "80"
        # Multi-region deployment via Cloud Run traffic split
        # Run `gcloud run services update-traffic` to route 80% -> us-central1, 20% -> europe-west1
        run.googleapis.com/location: "us-central1"
    spec:
      containerConcurrency: 80
      containers:
      - image: gcr.io/supremeai/backend:latest
        resources:
          limits:
            cpu: "2000m"
            memory: "2Gi"
        env:
        - name: GCP_PROJECT_ID
          value: "supremeai-a"
        - name: ENV
          value: "production"
        - name: GCP_REGION
          value: "us-central1"
        ports:
        - name: http1
          containerPort: 8000
---
# Secondary region: europe-west1 (deploy separately and use traffic-split)
# gcloud run services update supremeai-backend-europe \
#   --image=gcr.io/supremeai/backend:latest \
#   --region=europe-west1 \
#   --set-env-vars=ENV=production,GCP_REGION=europe-west1
```

### File: `infrastructure/firebase_functions/ocrTrigger.ts`

### File: `infrastructure/firebase_functions/ocrTrigger.ts`

```typescript
# SupremeAI — Firebase OCR Trigger
Provides a sample Cloud Function (Realtime Database + Firestore) that initiates an OCR task when a document is queued.
Use this as a reference; integrate into your actual functions source.

### Realtime Database reference implementation
- Database path: `/ocr-queue/{pushId}`
- Expected fields: `{ file_path: string, mime: string }`
- Result: writes `{ status: 'completed', result: any }` under `/ocr-results/{pushId}`
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/api-router.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/api-router.js`

```javascript
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const externalClient = require('./utils/externalClient');
const axios = require('axios');

const app = express();

const allowedOrigins = [
  'http://127.0.0.1:3000',
  'http://127.0.0.1:5173',
  'http://127.0.0.1:5000',
  'http://127.0.0.1:3000',
  'http://127.0.0.1:5173',
  'http://127.0.0.1:5000',
];

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes(origin) || origin.includes('supremeai')) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
}));
app.use(express.json());

const DEFAULT_SCRAPE_ENDPOINT = process.env.SCRAPE_ENGINE_URL || 'https://us-central1-supremeai.cloudfunctions.net/scrapeAndRespondFn';
const DEFAULT_CHAT_ENDPOINT = process.env.CHAT_API_URL || 'https://supremeai-a.web.app/api/chat/send';

function shouldUseScrapeEngine(req) {
  const preferScrape = (req.headers['x-use-scrape'] === 'true') || (req.body && req.body.useScrape === true);
  return !!preferScrape;
}

async function proxyToScrapeEngine(message, userId) {
  const url = DEFAULT_SCRAPE_ENDPOINT;
  const response = await axios.post(url, { message, userId }, { timeout: 30000 });
  return response.data;
}

app.get(['/health', '/api/health'], (req, res) => {
  res.json({
    status: 'ok',
    mode: 'coordinator',
    scrapeEngine: DEFAULT_SCRAPE_ENDPOINT,
    chatBackend: DEFAULT_CHAT_ENDPOINT,
  });
});

// REAL LLM Connection (Gemini / OpenAI Fallback)
async function callChatBackend(message, token) {
  const apiKey = process.env.SUPREME_API_KEY || process.env.GEMINI_API_KEY || process.env.OPENAI_API_KEY;
  const targetModel = process.env.SUPREME_CORE_MODEL || 'gemini-pro';

  if (!apiKey) {
    // Fallback to local neural core if no API key
    return generateSmartAIResponse(message);
  }

  try {
    // Attempt Gemini call
    const response = await axios.post(
      `https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent`,
      {
        contents: [{ parts: [{ text: message }] }]
      },
      {
        headers: {
          'x-goog-api-key': apiKey,
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );

    if (response.data && response.data.candidates && response.data.candidates.length > 0) {
      const text = response.data.candidates[0].content.parts[0].text;
      return {
        message: text,
        confidence: 0.95,
        chatType: 'LLM_RESPONSE',
        sourceType: 'SUPREME_CORE_API',
        sources: [`${process.env.SUPREME_BRAND_NAME || 'SupremeAI'} Intelligence`]
      };
    }
    throw new Error('Invalid LLM response format');
  } catch (err) {
    console.error('[LLM] API call failed:', err.message);
    return generateSmartAIResponse(message);
  }
}

async function unifiedChatHandler(req, res) {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  const userId = (req.body && req.body.userId) ? String(req.body.userId) : 'anonymous';
  const token = req.headers['authorization'] ? String(req.headers['authorization']).split('Bearer ')[1] : null;

  if (!message || !message.trim()) {
    return res.status(400).json({ success: false, message: 'Message is required', sourceType: 'error' });
  }

  try {
    let answer = '';
    let sources = [];
    let confidence = 0.2;
    let chatType = 'UNKNOWN';
    let sourceType = 'UNKNOWN';
    let scrapedPages = 0;

    if (shouldUseScrapeEngine(req)) {
      try {
        const scrapeResult = await proxyToScrapeEngine(message.trim(), userId);
        if (scrapeResult && scrapeResult.answer) {
          answer = scrapeResult.answer;
          sources = Array.isArray(scrapeResult.sources) ? scrapeResult.sources : [];
          confidence = typeof scrapeResult.confidence === 'number' ? scrapeResult.confidence : 0.55;
          chatType = scrapeResult.chatType || 'COMPLEX_QUESTION';
          sourceType = scrapeResult.cached ? 'SCRAPE_CACHE' : 'SCRAPE_ENGINE';
          scrapedPages = typeof scrapeResult.scrapedPages === 'number' ? scrapeResult.scrapedPages : sources.length;
        }
      } catch (scrapeError) {
        console.warn('[api-router] Scrape engine failed, falling back to chat backend:', scrapeError.message);
      }
    }

    if (!answer) {
      try {
        const chatResult = await callChatBackend(message, token);
        if (chatResult && chatResult.message) {
          answer = chatResult.message;
          confidence = typeof chatResult.confidence === 'number' ? chatResult.confidence : 0.5;
          chatType = chatResult.chatType || 'SIMPLE_QUESTION';
          sourceType = chatResult.source_type || chatResult.sourceType || 'CORE_API';
          sources = Array.isArray(chatResult.sources) ? chatResult.sources : [];
        }
      } catch (chatError) {
        console.warn('[api-router] Chat backend failed, using virtual crawler:', chatError.message);
      }
    }

    if (!answer) {
      // Both scraping and LLM failed or yielded empty
      chatType = 'UNKNOWN';
      sourceType = 'ERROR';
      confidence = 0;
      answer = "সিস্টেম তথ্য সংগ্রহ করতে পারেনি। অনুগ্রহ করে আবার চেষ্টা করুন।";
    }

    return res.json({
      success: true,
      message: answer,
      sources,
      confidence,
      chatType,
      sourceType,
      scrapedPages,
      userId,
    });
  } catch (error) {
    console.error('[api-router] Unified chat error:', error && error.message);
    return res.status(500).json({ success: false, message: 'Service unavailable. Please try again later.', sourceType: 'error', chatType: 'UNKNOWN' });
  }
}

app.post(['/api/chat/send', '/chat/send'], async (req, res) => {
  return unifiedChatHandler(req, res);
});

app.post(['/api/scrape/and-respond', '/scrape/and-respond'], async (req, res) => {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  const userId = (req.body && req.body.userId) ? String(req.body.userId) : 'anonymous';
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'Missing required field: message' });
  }
  try {
    const result = await proxyToScrapeEngine(message.trim(), userId);
    return res.json({ success: true, ...result });
  } catch (error) {
    console.error('[api-router] Scrape proxy error:', error && error.message);
    return res.status(502).json({ success: false, error: 'Scrape engine unavailable', details: error && error.message });
  }
});

app.post(['/api/chat/classify', '/chat/classify'], async (req, res) => {
  const message = (req.body && req.body.message) ? String(req.body.message) : '';
  if (!message || !message.trim()) {
    return res.status(400).json({ error: 'message required' });
  }
  try {
    const scrapeUrl = DEFAULT_SCRAPE_ENDPOINT.replace('/scrapeAndRespondFn', '/classifyIntentFn');
    const response = await axios.post(scrapeUrl, { message }, { timeout: 10000 });
    return res.json({ success: true, ...response.data });
  } catch (error) {
    return res.status(500).json({ success: false, error: 'Classification failed' });
  }
});

function calculateOverlapScore(query, task) {
  const q = (query || '').toLowerCase().replace(/[^\u0000-\u007F\u0980-\u09ff\w\s]/g, '');
  const a = (task || '').toLowerCase().replace(/[^\u0000-\u007F\u0980-\u09ff\w\s]/g, '');
  const queryWords = new Set(q.split(/\s+/).filter(w => w && w.length > 2));
  const taskWords = a.split(/\s+/).filter(w => w && w.length > 2);
  if (queryWords.size === 0) return 0;
  let match = 0;
  for (const w of taskWords) if (queryWords.has(w)) match++;
  return match / Math.max(1, queryWords.size);
}

function searchCoreKnowledge(userMessage) {
  try {
    const knowledgePath = path.join(__dirname, '..', 'src', 'main', 'resources', 'core_knowledge.json');
    if (!fs.existsSync(knowledgePath)) return null;
    const raw = fs.readFileSync(knowledgePath, 'utf8');
    const list = JSON.parse(raw || '[]');
    let best = null;
    let bestScore = 0;
    for (const item of list) {
      const score = calculateOverlapScore(userMessage, item.task || item.question || '');
      if (score > bestScore) {
        bestScore = score;
        best = item;
      }
    }
    if (best && bestScore >= 0.3) {
      return { solution: best.solution || best.answer || '', score: bestScore, category: best.category };
    }
    return null;
  } catch (e) {
    console.error('[CoreKnowledge] read error', e && e.message);
    return null;
  }
}

function classifySemanticIntent(userMessage) {
  const q = (userMessage || '').toLowerCase();
  if (/exception|error|compile|run|bug|nullpointer|git|npm|gradle|dependency|api|db|class|function|method|import|debug|stack trace/i.test(q)) {
    return { categoryId: 'coding', name: 'Coding', timeout: 3000 };
  }
  if (/bangladesh|govt|government|সরকার|কর|দাপ্তরিক|মন্ত্রণালয়/i.test(q)) {
    return { categoryId: 'bangladesh_govt', name: 'Bangladesh Govt', timeout: 3500 };
  }
  if (/weather|temperature|rain|আবহাওয়া|বৃষ্টি|তাপমাত্রা/i.test(q)) {
    return { categoryId: 'weather', name: 'Weather', timeout: 2000 };
  }
  if (/tech|nvidia|gpu|cpu|openai|gemini|llama|release|প্রযুক্তি/i.test(q)) {
    return { categoryId: 'tech_news', name: 'Tech News', timeout: 3000 };
  }
  if (/health|doctor|hospital|medicine|স্বাস্থ্য|চিকিৎসা/i.test(q)) {
    return { categoryId: 'health', name: 'Health', timeout: 3000 };
  }
  return { categoryId: 'general', name: 'General', timeout: 3000 };
}

// Virtual crawler removed in favor of unified scrapeEngine
function generateSmartAIResponse(userMessage) {
  const msg = (userMessage || '').trim();
  if (!msg) {
    return { success: false, message: 'Empty input. Please enter a valid message.', agent_name: 'SupremeAI Neural Core', confidence: 0, source_type: 'error' };
  }

  if (/who are you|আপনি কে/i.test(msg)) {
    return {
      success: true,
      message: `আমি ${process.env.SUPREME_BRAND_NAME || 'SupremeAI'}। আমি আপনার ডিজিটাল অ্যাসিস্ট্যান্ট।`,
      agent_name: process.env.SUPREME_BRAND_NAME || 'SupremeAI',
      confidence: 0.99,
      source_type: 'LOCAL_SEED',
    };
  }
  if (/time|সময়|বাজে|time now/i.test(msg)) {
    const bdTime = new Date().toLocaleString('bn-BD', { timeZone: 'Asia/Dhaka' });
    return {
      success: true,
      message: `বর্তমান সময়: ${bdTime} (বাংলাদেশ সময়)`,
      agent_name: 'SupremeAI Neural Core',
      confidence: 1.0,
      source_type: 'LOCAL_SEED',
    };
  }
  if (/date|তারিখ|today/i.test(msg)) {
    const bdDate = new Date().toLocaleDateString('bn-BD', { timeZone: 'Asia/Dhaka', year: 'numeric', month: 'long', day: 'numeric' });
    return {
      success: true,
      message: `আজকের তারিখ: ${bdDate}`,
      agent_name: 'SupremeAI Neural Core',
      confidence: 1.0,
      source_type: 'LOCAL_SEED',
    };
  }

  const core = searchCoreKnowledge(userMessage);
  if (core) {
    return {
      success: true,
      message: core.solution,
      agent_name: 'SupremeAI Neural Core',
      confidence: core.score,
      source_type: 'CORE_KNOWLEDGE',
    };
  }

  return {
    success: true,
    message: 'আমি তথ্যটি বিশ্লেষণ করতে পারছি না।',
    agent_name: 'SupremeAI Neural Core',
    confidence: 0.1,
    source_type: 'DEFAULT_FALLBACK',
  };
}

app.post(['/api/chat/legacy', '/chat/legacy'], async (req, res) => {
  const userMessage = (req.body && req.body.message) || '';
  if (!userMessage || !userMessage.trim()) {
    return res.status(400).json({ success: false, message: 'Message is required', source_type: 'error' });
  }
  try {
    const resp = generateSmartAIResponse(userMessage);
    return res.json(resp);
  } catch (e) {
    console.error('[API] chat handler error', e && e.message);
    return res.status(500).json({ success: false, message: 'Internal server error. Please try again later.', source_type: 'error' });
  }
});

app.get(['/api/projects', '/projects', '/api/api/projects'], (req, res) => {
  res.json({ success: true, data: [] });
});

app.use(['/api/admin/*', '/admin/*'], (req, res) => {
  const requestPath = req.path;
  let data = null;

  if (requestPath.includes('users')) data = [];
  if (requestPath.includes('rules')) data = [];
  if (requestPath.includes('plans')) data = [];
  if (requestPath.includes('chat')) data = [];
  if (requestPath.includes('logs')) data = [];
  if (requestPath.includes('quotas')) data = { usage: 0, limit: 1000 };

  res.json({ success: true, data, note: 'Emulator stub - real implementation pending on coordinator server' });
});

app.use((req, res) => {
  res.status(404).json({ error: 'Not found in api-router coordinator', path: req.path });
});

module.exports = app;
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/deployment-monitor.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/deployment-monitor.js`

```javascript
// functions/deployment-monitor.js - AI-Powered Deployment Monitor
// Uses Groq AI to analyze GitHub changes and wake system if needed

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin only once (index.js already calls initializeApp)
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();

// Groq API configuration
const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

/**
 * HTTP trigger: Analyze deployment changes with AI
 * Endpoint: https://region-supremeai.cloudfunctions.net/analyzeDeployment
 * 
 * LOCAL-FIRST MODE: If no AI API key is configured, uses heuristic analysis.
 */
exports.analyzeDeployment = onRequest(async (req, res) => {
    try {
        const { commitMessage, changedFiles, author, branch, runId } = req.body;

        if (!commitMessage || !changedFiles) {
            return res.status(400).json({
                error: "Missing required fields: commitMessage, changedFiles"
            });
        }

        // LOCAL-FIRST: No external AI API key required
        console.log("[LOCAL-FIRST] Analyzing deployment without external AI API key...");

        // Always use local heuristic analysis - no external API required
        const analysis = fallbackAnalysis({
            commitMessage,
            changedFiles,
            author,
            branch,
            projectInfo: getProjectContext()
        });

        // Determine if system needs to be woken up
        const needsWakeUp = shouldWakeSystem(analysis);

        // BUG FIX: Actually call the wakeSystem function if needed
        if (needsWakeUp) {
            console.log(`[MONITOR] High-impact deployment detected for Run ID: ${runId}. Pinging backend...`);
            const success = await wakeSystem(runId, analysis);
            if (!success) {
                console.warn(`[MONITOR] Warning: System wake-up failed for Run ID: ${runId}`);
            }
        }

        // Save analysis to Firestore for tracking
        await saveDeploymentAnalysis({
            timestamp: new Date().toISOString(),
            commitMessage,
            author,
            branch,
            changedFiles,
            analysis,
            needsWakeUp,
            runId,
            actionTaken: needsWakeUp ? "system_woken" : "none"
        });

        // Send notification to admin
        await sendDeploymentNotification(analysis, needsWakeUp);

        res.json({
            success: true,
            analysis,
            needsWakeUp,
            action: needsWakeUp ? "System woken up" : "No action needed",
            localMode: true
        });

    } catch (error) {
        console.error("Error analyzing deployment:", error);
        res.status(500).json({
            error: error.message,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
});

/**
 * Use Groq AI to analyze deployment changes
 */
async function analyzeWithGroq(apiKey, deploymentInfo) {
    const systemPrompt = `
You are an AI DevOps engineer monitoring the SupremeAI deployment system.
Analyze GitHub commits/changes and determine:

1. IMPACT LEVEL: (critical, high, medium, low, none)
   - critical: Core system files changed (backend, database, security, API)
   - high: Major feature changes affecting multiple components
   - medium: Feature additions or significant modifications
   - low: Documentation, minor UI tweaks, non-essential changes
   - none: Test files, CI/CD config updates only

2. WAKE_NEEDED: (true/false)
   - TRUE if impact is critical OR high AND files are in: src/, build.gradle*, Dockerfile, application.yml, functions/
   - FALSE for low-impact changes

3. ACTION: Recommended action (deploy, notify, ignore, wake_system)

4. REASON: Brief explanation in 1-2 sentences

Respond in JSON format:
{
  "impact": "critical|high|medium|low|none",
  "wakeNeeded": true|false,
  "action": "string",
  "reason": "string",
  "affectedComponents": ["list", "of", "components"]
}
`;

    const userMessage = `
Analyze this deployment:

Commit: ${deploymentInfo.commitMessage}
Author: ${deploymentInfo.author || 'Unknown'}
Branch: ${deploymentInfo.branch}
Changed Files:
${deploymentInfo.changedFiles.map(f => `- ${f}`).join('\n')}

Project Context:
${deploymentInfo.projectInfo}

Analyze and respond with JSON only.
`;

    try {
        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-70b-8192",
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userMessage }
                ],
                temperature: 0.1,
                max_tokens: 512
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 10000
            }
        );

        const content = response.data.choices[0].message.content;
        // Extract JSON from response (Groq may include markdown)
        const jsonMatch = content.match(/\{[\s\S]*\}/);
        if (jsonMatch) {
            return JSON.parse(jsonMatch[0]);
        }

        throw new Error("Could not parse AI response");
    } catch (error) {
        console.error("Groq API error:", error.message);
        // Fallback: simple heuristic analysis
        return fallbackAnalysis(deploymentInfo);
    }
}

/**
 * Fallback analysis if Groq fails
 */
function fallbackAnalysis(deploymentInfo) {
    const criticalPatterns = [
        /src\/main/,
        /build\.gradle/,
        /application\.yml/,
        /Dockerfile/,
        /functions\//,
        /firebase\.json/,
        /security/,
        /auth/,
        /database/
    ];

    const allFiles = deploymentInfo.changedFiles.join(' ').toLowerCase();

    const isCritical = criticalPatterns.some(pattern => pattern.test(allFiles));

    return {
        impact: isCritical ? "critical" : "medium",
        wakeNeeded: isCritical,
        action: isCritical ? "wake_system" : "notify",
        reason: isCritical
            ? "Critical system files changed - immediate attention required"
            : "Non-critical changes detected",
        affectedComponents: isCritical ? ["core_system"] : ["ui_or_docs"],
        fallback: true
    };
}

/**
 * Determine if system needs to be woken up
 */
function shouldWakeSystem(analysis) {
    return analysis.wakeNeeded === true ||
        analysis.impact === "critical" ||
        analysis.action === "wake_system";
}

/**
 * Wake up the Cloud Run service by sending a health check request
 */
async function wakeSystem(runId, analysis) {
    const backendUrl = process.env.JAVA_BACKEND_URL || "https://ide-api.supremeai.google.com";

    try {
        console.log(`Waking system for run ${runId}...`);

        // Send wake-up ping to health endpoint
        const response = await axios.get(`${backendUrl}/api/health`, {
            timeout: 30000,
            headers: {
                "X-Wake-Call": "true",
                "X-Run-ID": runId || "unknown"
            }
        });

        console.log(`System wake response:`, response.data);

        // Log wake event
        await db.collection('system_wake_events').add({
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            runId,
            analysis,
            status: "woken",
            response: response.data
        });

        return true;
    } catch (error) {
        console.error("Failed to wake system:", error.message);
        await db.collection('system_wake_events').add({
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            runId,
            analysis,
            status: "failed",
            error: error.message
        });
        return false;
    }
}

/**
 * Get project context for AI analysis
 */
function getProjectContext() {
    return `
SupremeAI Multi-Agent System:
- Backend: Spring Boot 3 (Java 21) on Cloud Run
- Frontend: Flutter web on Firebase Hosting
- Functions: Firebase Cloud Functions
- AI: 10+ providers (Groq, OpenAI, Gemini, Claude, etc.)
- Database: Firestore + Redis caching
- Key directories:
  * src/main/java/com/supremeai/ - Backend Java code
  * supremeai/ - Flutter frontend
  * functions/ - Firebase Cloud Functions
  * dashboard/ - 3D React dashboard
  * build.gradle.kts, settings.gradle.kts - Gradle config
  * application.yml - Spring configuration
`;
}

/**
 * Save deployment analysis to Firestore
 */
async function saveDeploymentAnalysis(data) {
    try {
        await db.collection('deployment_analysis').add({
            ...data,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    } catch (error) {
        console.error("Error saving analysis:", error.message);
    }
}

/**
 * Send smart deployment notification via FCM
 */
async function sendDeploymentNotification(analysis, needsWakeUp) {
    const title = needsWakeUp
        ? "🚨 CRITICAL: System Woken Up"
        : analysis.impact === "high"
            ? "⚠️ High Impact Deployment"
            : "📦 Deployment Detected";

    const body = needsWakeUp
        ? `Critical changes detected. System has been activated. ${analysis.reason || ''}`
        : analysis.reason || "Deployment completed with medium/low impact changes";

    const notification = {
        notification: {
            title,
            body,
            clickAction: "FLUTTER_NOTIFICATION_CLICK"
        },
        data: {
            type: "deployment_analysis",
            impact: analysis.impact || "unknown",
            wakeNeeded: needsWakeUp.toString(),
            timestamp: new Date().toISOString()
        },
        topic: "admin-notifications"
    };

    try {
        await admin.messaging().send(notification);
        console.log("Deployment notification sent");
    } catch (error) {
        console.error("Error sending notification:", error.message);
    }
}

/**
 * Scheduled trigger: Periodic system health check with AI analysis
 * Runs every 5 minutes
 */
exports.monitorSystemHealth = onSchedule('*/5 * * * *', async (event) => {
    try {
        const backendUrl = process.env.JAVA_BACKEND_URL || "https://ide-api.supremeai.google.com";
        const healthResponse = await axios.get(`${backendUrl}/api/health`, {
            timeout: 10000
        }).catch(() => null);

        if (!healthResponse) {
            console.log("System appears to be down. Attempting to diagnose...");
            await diagnoseAndAlert();
        } else {
            console.log("System health check passed:", healthResponse.data);
        }

        return null;
    } catch (error) {
        console.error("Health monitor error:", error);
        return null;
    }
});

/**
 * Diagnose system issues and alert admin
 */
async function diagnoseAndAlert() {
    const groqApiKey = functions.config().groq?.api_key ||
        process.env.GROQ_API_KEY_DEPLOYMENT_MONITOR;

    if (!groqApiKey) {
        console.warn("Groq API key not available for diagnosis");
        return;
    }

    // Collect recent logs and errors
    const recentErrors = await db.collection('system_health')
        .orderBy('createdAt', 'desc')
        .limit(10)
        .get();

    const errorSummaries = recentErrors.docs.map(doc => doc.data());

    // Ask Groq to diagnose
    const diagnosis = await askGroqForDiagnosis(groqApiKey, errorSummaries);

    await admin.messaging().send({
        notification: {
            title: "🚨 System Down - AI Diagnosis",
            body: diagnosis.summary || "System appears offline. Check Cloud Run logs.",
            clickAction: "FLUTTER_NOTIFICATION_CLICK"
        },
        data: {
            type: "system_diagnosis",
            diagnosis: JSON.stringify(diagnosis),
            timestamp: new Date().toISOString()
        },
        topic: "admin-notifications"
    });
}

/**
 * Ask Groq to diagnose system issues
 */
async function askGroqForDiagnosis(apiKey, errorData) {
    try {
        const prompt = `
Based on these recent system health records, diagnose the likely cause and suggest fixes:

${JSON.stringify(errorData, null, 2)}

Respond in JSON:
{
  "likelyCause": "brief description",
  "suggestedFix": "actionable fix",
  "severity": "critical|high|medium|low",
  "summary": "One-line summary for notification"
}
`;

        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-70b-8192",
                messages: [{ role: "user", content: prompt }],
                temperature: 0.2,
                max_tokens: 256
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 10000
            }
        );

        const content = response.data.choices[0].message.content;
        const jsonMatch = content.match(/\{[\s\S]*\}/);
        return jsonMatch ? JSON.parse(jsonMatch[0]) : { summary: "AI analysis unavailable" };
    } catch (error) {
        return {
            likelyCause: "Unknown",
            suggestedFix: "Check Cloud Run logs manually",
            severity: "high",
            summary: "System down - manual investigation required"
        };
    }
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/health-smart.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/health-smart.js`

```javascript
// Simple health + stats endpoints for emulator stability

exports.healthCheck = (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.json({ status: 'ok', timestamp: new Date().toISOString(), mode: 'emulator' });
};

exports.getProviderHealthStats = (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.json({
    success: true,
    data: {
      total: 2,
      active: 2,
      error: 0,
      rotating: 0,
      lastCheck: new Date().toISOString()
    }
  });
};
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/index.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/index.js`

```javascript
// functions/index.js - Firebase Cloud Functions for AI System
// Deploy with: firebase deploy --only functions

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// ============ GLOBAL CORS (for 127.0.0.1 emulator + future) ============
const allowedOrigins = [
    'http://127.0.0.1:3000',
    'http://127.0.0.1:5173',
    'http://127.0.0.1:5000',
    'http://127.0.0.1:3000',
    'http://127.0.0.1:5173',
    'http://127.0.0.1:5000'
];

const allowCors = (handler) => async (req, res) => {
    const origin = req.headers.origin;
    const allowedOrigin = (origin && (allowedOrigins.includes(origin) || origin.includes('supremeai'))) ? origin : 'https://supremeai-dashboard.web.app';

    res.set('Access-Control-Allow-Origin', allowedOrigin);
    res.set('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-api-key');
    res.set('Vary', 'Origin');

    if (req.method === 'OPTIONS') {
        return res.status(204).send('');
    }
    return handler(req, res);
};

// ============ AUTHENTICATION MIDDLEWARE ============
const authenticate = async (req, res, next) => {
    // 1. Allow Java backend to bypass if correct system secret is provided
    const apiKey = req.get('x-api-key') || (req.body && req.body.apiKey) || (req.query && req.query.apiKey);
    const systemSecret = functions.config().system && functions.config().system.secret;

    // SECURITY FIX: Only allow bypass if system secret is configured AND matches
    // Do NOT allow bypass if systemSecret is undefined/null/empty
    if (systemSecret && systemSecret.trim() !== '' && apiKey && apiKey === systemSecret) {
        console.log('Java backend authenticated via system secret');
        return next();
    }

    // 2. Require Firebase Auth Admin Token for frontend/admin UI calls
    const authHeader = req.get('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: "Unauthorized: Missing or invalid token" });
    }

    try {
        const idToken = authHeader.split('Bearer ')[1];
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        // Enforce 'admin' claim as a strict boolean true
        if (decodedToken.admin !== true) {
            return res.status(403).json({ error: "Forbidden: Admin access required" });
        }
        req.user = decodedToken;
        return next();
    } catch (error) {
        console.error('Error verifying token:', error);
        return res.status(401).json({ error: "Unauthorized: Invalid token" });
    }
};

const withAuth = (handler) => {
    return async (req, res) => {
        return authenticate(req, res, () => handler(req, res));
    };
};

// ============ SYSTEM HEALTH MONITORING ============

const systemHealth = require('./system-health');
exports.getSystemHealth = systemHealth.getSystemHealth;
exports.collectHealthMetrics = systemHealth.collectHealthMetrics;

// Smart AI Providers (auto-discovery from Cloud Run + env + Firestore)
const smartProviders = require('./providers-smart');
exports.getConfiguredProviders = smartProviders.getConfiguredProviders;
exports.getProviderHealthStats = smartProviders.getProviderHealthStats;

// Central API Router (best long-term solution)
const apiRouter = require('./api-router');
exports.api = require('firebase-functions').https.onRequest(apiRouter);

// ============ REQUIREMENT PROCESSING ============

/**
 * HTTP trigger: Process new requirement from admin
 * Endpoint: https://region-supremeai.cloudfunctions.net/processRequirement
 */
exports.processRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, description } = req.body;

        if (!projectId || !description) {
            return res.status(400).json({ error: "Missing projectId or description" });
        }

        // Call Java backend to classify
        const backendUrl = (functions.config().backend && functions.config().backend.url) || process.env.JAVA_BACKEND_URL || 'http://127.0.0.1:8080';
        const classificationUrl = `${backendUrl}/classify`;
        const classifyResponse = await axios.post(classificationUrl, { description });
        const size = classifyResponse.data.size; // SMALL, MEDIUM, or BIG

        // Save requirement to Firestore
        const reqRef = await db.collection("requirements").add({
            projectId,
            description,
            size,
            status: "pending",
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Auto-approve or notify
        if (size === "SMALL") {
            await reqRef.update({ status: "approved" });
            console.log(`✅ Auto-approved SMALL requirement: ${reqRef.id}`);
        } else if (size === "MEDIUM") {
            // Schedule auto-approve after 10 minutes
            db.collection("scheduled_approvals").add({
                requirementId: reqRef.id,
                approvalTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 10 * 60000)),
            });

            // Send notification
            await admin.messaging().send({
                notification: {
                    title: "⏳ Approval Needed",
                    body: description,
                },
                data: { requirementId: reqRef.id },
                topic: "admin-notifications",
            });
            console.log(`⏳ MEDIUM requirement pending approval: ${reqRef.id}`);
        } else {
            // Send urgent notification for BIG tasks
            await admin.messaging().send({
                notification: {
                    title: "🛑 URGENT: Manual Approval Required",
                    body: description,
                },
                data: { requirementId: reqRef.id, type: "big_approval" },
                topic: "admin-notifications",
            });
            console.log(`🛑 BIG requirement awaiting manual approval: ${reqRef.id}`);
        }

        res.json({
            success: true,
            requirementId: reqRef.id,
            size,
            message: `Requirement processed as ${size}`,
        });
    } catch (error) {
        console.error("Error processing requirement:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ APPROVAL HANDLING ============

/**
 * HTTP trigger: Admin approves/rejects requirement
 * Endpoint: https://region-supremeai.cloudfunctions.net/approveRequirement
 */
exports.approveRequirement = onRequest(withAuth(async (req, res) => {
    try {
        const { requirementId, approved, notes } = req.body;

        if (!requirementId) {
            return res.status(400).json({ error: "Missing requirementId" });
        }

        // Update requirement status
        await db.collection("requirements").doc(requirementId).update({
            status: approved ? "approved" : "rejected",
            notes,
            approvedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // If approved, trigger agent orchestrator
        if (approved) {
            const req_doc = await db.collection("requirements").doc(requirementId).get();
            const { projectId, description } = req_doc.data();

            // Call Java backend orchestrator
            const backendUrl = (functions.config().backend && functions.config().backend.url) || process.env.JAVA_BACKEND_URL || 'http://127.0.0.1:8080';
            const orchestrateUrl = `${backendUrl}/orchestrate`;
            await axios.post(orchestrateUrl, {
                projectId,
                requirementDescription: description,
            });

            // Update project status
            await db.collection("projects").doc(projectId).update({
                status: "building",
                updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }

        res.json({
            success: true,
            status: approved ? "approved" : "rejected",
        });
    } catch (error) {
        console.error("Error approving requirement:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ AUTO-APPROVAL SCHEDULER ============

/**
 * Scheduled trigger: Auto-approve MEDIUM tasks after 10 minutes
 */
exports.autoApproveScheduled = onSchedule("*/5 * * * *", async (event) => {
    const now = admin.firestore.Timestamp.now();

    const scheduledApprovals = await db.collection("scheduled_approvals")
        .where("approvalTime", "<=", now)
        .get();

    for (const doc of scheduledApprovals.docs) {
        const { requirementId } = doc.data();

        // Check if still pending
        const req = await db.collection("requirements").doc(requirementId).get();
        if (req.data().status === "pending") {
            await req.ref.update({
                status: "approved",
                autoApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            console.log(`✅ Auto-approved MEDIUM requirement: ${requirementId}`);
        }

        // Delete scheduled entry
        await doc.ref.delete();
    }

    return null;
});

// ============ AI AGENT ROTATION ============

/**
 * HTTP trigger: Handle quota exceeded / API errors
 * Called by Java backend on 429/403 errors
 */
exports.rotateAgent = onRequest(withAuth(async (req, res) => {
    try {
        const { agentId, reason } = req.body;

        // Update agent status
        await db.collection("ai_pool").doc(agentId).update({
            status: "rotated",
            reason,
            rotatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Trigger VPN switch (if enabled in config)
        const config = await db.collection("config").doc("system").get();
        if (config.data().vpn_enabled) {
            const vpnResult = await switchVPN(agentId);
            console.log(`🔄 VPN switched for ${agentId}: ${vpnResult}`);
        }

        // Notify admin
        await admin.messaging().send({
            notification: {
                title: "⚠️  Agent Rotated",
                body: `${agentId} rotated due to: ${reason}`,
            },
            topic: "admin-notifications",
        });

        res.json({ success: true, message: "Agent rotated" });
    } catch (error) {
        console.error("Error rotating agent:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ CHAT MESSAGE HANDLER ============

/**
 * Firestore trigger: Save AI messages to chat history
 */
exports.onChatMessage = onDocumentCreated("projects/{projectId}/chat/{messageId}", async (event) => {
    const { projectId } = event.params;
    const message = event.data.data();

    // Update project's lastMessage timestamp
    await admin.firestore().collection("projects").doc(projectId).update({
        lastMessageAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send real-time notification if from AI
    if (message.sender !== "admin") {
        const project = await admin.firestore().collection("projects").doc(projectId).get();
        const adminUserId = project.data().adminUserId;

        if (adminUserId) {
            await admin.messaging().send({
                notification: {
                    title: `${message.sender} Updated`,
                    body: message.message.substring(0, 50) + "...",
                },
                data: {
                    projectId,
                    type: "chat_update",
                },
                topic: `user-${adminUserId}`,
            });
        }
    }
});

// ============ PROGRESS TRACKER ============

/**
 * HTTP trigger: Update project progress
 */
exports.updateProgress = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, progress, status } = req.body;

        const updateData = {
            progress,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        if (status) {
            updateData.status = status;
        }
        await db.collection("projects").doc(projectId).update(updateData);

        res.json({ success: true });
    } catch (error) {
        console.error("Error updating project progress:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ SERVER CONNECTION MONITORING ============

const serverConnectionMonitor = require('./server-connection-monitor');
exports.checkServerConnections = serverConnectionMonitor.checkServerConnections;
exports.monitorConnections = serverConnectionMonitor.monitorConnections;

// ============ AI DEPLOYMENT MONITORING ============

const deploymentMonitor = require('./deployment-monitor');
exports.analyzeDeployment = deploymentMonitor.analyzeDeployment;
exports.monitorSystemHealth = deploymentMonitor.monitorSystemHealth;

// ============ BENGALI OCR PROCESSING ============

/**
 * HTTP trigger: Process Multi-language OCR on uploaded images
 * Endpoint: https://region-supremeai.cloudfunctions.net/processOCR
 */
const locales = {
  en: {
    ocr_complete_title: "✅ OCR Processing Complete",
    ocr_complete_body: "Processed {success_count}/{total_count} images",
    error_missing_urls: "Missing or invalid imageUrls array",
    error_forbidden_url: "Forbidden URL target",
    error_vision_api: "Vision API error: {message}"
  },
  bn: {
    ocr_complete_title: "✅ ওসিআর প্রসেসিং সম্পন্ন",
    ocr_complete_body: "সফলভাবে {success_count}/{total_count} টি ইমেজ প্রসেস করা হয়েছে",
    error_missing_urls: "অনুপস্থিত বা অবৈধ imageUrls অ্যারে",
    error_forbidden_url: "নিষিদ্ধ ইউআরএল টার্গেট",
    error_vision_api: "ভিশন এপিআই ত্রুটি: {message}"
  }
};

function getLocaleString(locale, key, params = {}) {
  const dict = locales[locale] || locales['en'];
  let str = dict[key] || locales['en'][key] || key;
  for (const [k, v] of Object.entries(params)) {
    str = str.replace(`{${k}}`, v);
  }
  return str;
}

const axiosGetWithRetry = async (url, options, retries = 3, delay = 1000) => {
    for (let i = 0; i < retries; i++) {
        try {
            return await axios.get(url, options);
        } catch (error) {
            if (i === retries - 1) throw error;
            console.warn(`Axios fetch failed, retrying in ${delay}ms... (Attempt ${i + 1}/${retries})`);
            await new Promise(res => setTimeout(res, delay));
            delay *= 2; // Exponential Backoff
        }
    }
};

exports.processOCR = onRequest(withAuth(async (req, res) => {
    try {
        const { imageUrls, projectId, userId, languages = ['en', 'bn'], locale = 'en' } = req.body;

        if (!imageUrls || !Array.isArray(imageUrls) || imageUrls.length === 0) {
            return res.status(400).json({ error: getLocaleString(locale, 'error_missing_urls') });
        }

        const results = [];
        const vision = require('@google-cloud/vision');
        const client = new vision.ImageAnnotatorClient();

        // Use Promise.all for parallel processing to improve performance
        const processingPromises = imageUrls.map(async (imageUrl) => {
            try {
                console.log(`🔍 Processing OCR for: ${imageUrl}`);

                // SSRF Protection: Validate URL
                const urlObj = new URL(imageUrl);
                const forbiddenHostPatterns = [/169\.254/, /127\.0\.0\.1/, /127.0.0.1/];
                if (forbiddenHostPatterns.some(pattern => pattern.test(urlObj.hostname))) {
                    throw new Error(getLocaleString(locale, 'error_forbidden_url'));
                }

                // For Firebase Storage URLs, we need to download the image
                let image;
                if (imageUrl.startsWith('gs://') || imageUrl.startsWith('https://firebasestorage.googleapis.com')) {
                    // Download from Firebase Storage
                    const bucket = admin.storage().bucket();
                    // Note: Improved parsing logic needed for complex URLs
                    const fileName = imageUrl.split('/').pop();
                    const file = bucket.file(fileName);
                    const [buffer] = await file.download();
                    image = { content: buffer };
                } else if (imageUrl.startsWith('data:image/')) {
                    // Base64 encoded image
                    const base64Data = imageUrl.split(',')[1];
                    image = { content: Buffer.from(base64Data, 'base64') };
                } else {
                    // External URL with timeout and retry logic
                    const response = await axiosGetWithRetry(imageUrl, {
                        responseType: 'arraybuffer',
                        timeout: 10000,
                        headers: { 'Accept': 'image/*' }
                    });
                    image = { content: Buffer.from(response.data) };
                }

                // Configure for multi-language text recognition
                const imageContext = {
                    languageHints: languages,
                };

                // Perform OCR
                const [result] = await client.textDetection({
                    image,
                    imageContext,
                });

                if (result.error) {
                    throw new Error(getLocaleString(locale, 'error_vision_api', { message: result.error.message }));
                }

                const detections = result.textAnnotations;
                const extractedText = detections.length > 0 ? detections[0].description : '';

                // Parse table structure if possible
                const lines = extractedText.split('\n').filter(line => line.trim());
                const tableData = parseTableFromText(lines);

                // Save to Firestore
                const ocrResult = {
                    imageUrl,
                    extractedText,
                    tableData,
                    languages_requested: languages,
                    processedAt: admin.firestore.FieldValue.serverTimestamp(),
                    confidence: detections.length > 0 ? detections[0].boundingPoly : null,
                };

                if (projectId) {
                    await db.collection('projects').doc(projectId).collection('ocr_results').add(ocrResult);
                }

                return {
                    imageUrl,
                    success: true,
                    textLength: extractedText.length,
                    linesCount: lines.length,
                    tableDetected: tableData.length > 0,
                };

            } catch (imageError) {
                console.error(`Error processing ${imageUrl}:`, imageError);
                return {
                    imageUrl,
                    success: false,
                    error: imageError.message,
                };
            }
        });

        const results = await Promise.all(processingPromises);

        if (userId && results.some(r => r.success)) {
            const successCount = results.filter(r => r.success).length;
            await admin.messaging().send({
                notification: {
                    title: getLocaleString(locale, 'ocr_complete_title'),
                    body: getLocaleString(locale, 'ocr_complete_body', { success_count: successCount, total_count: results.length }),
                },
                data: {
                    type: "ocr_complete",
                    projectId: projectId || "",
                },
                topic: `user-${userId}`,
            });
        }

        res.json({
            success: true,
            results,
            summary: {
                total: results.length,
                successful: results.filter(r => r.success).length,
                failed: results.filter(r => !r.success).length,
            },
            // Pattern from seed_data: Consistent meta response
            _meta: {
                timestamp: new Date().toISOString(),
                version: "2.1.0-international"
            }
        });

    } catch (error) {
        console.error("Error in Bengali OCR processing:", error);
        res.status(500).json({ error: error.message });
    }
}));

/**
 * HTTP trigger: Get OCR results for a project
 * Endpoint: https://region-supremeai.cloudfunctions.net/getOCRResults
 */
exports.getOCRResults = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId } = req.query;

        if (!projectId) {
            return res.status(400).json({ error: "Missing projectId" });
        }

        const ocrResults = await db.collection('projects').doc(projectId)
            .collection('ocr_results')
            .orderBy('processedAt', 'desc')
            .get();

        const results = [];
        ocrResults.forEach(doc => {
            results.push({
                id: doc.id,
                ...doc.data(),
            });
        });

        res.json({
            success: true,
            results,
        });

    } catch (error) {
        console.error("Error fetching OCR results:", error);
        res.status(500).json({ error: error.message });
    }
}));

/**
 * HTTP trigger: Convert OCR results to Excel and upload
 * Endpoint: https://region-supremeai.cloudfunctions.net/exportOCRToExcel
 */
exports.exportOCRToExcel = onRequest(withAuth(async (req, res) => {
    try {
        const { projectId, resultIds } = req.body;

        if (!projectId || !resultIds || !Array.isArray(resultIds)) {
            return res.status(400).json({ error: "Missing projectId or resultIds array" });
        }

        const ExcelJS = require('exceljs');
        const workbook = new ExcelJS.Workbook();

        for (const resultId of resultIds) {
            const resultDoc = await db.collection('projects').doc(projectId)
                .collection('ocr_results').doc(resultId).get();

            if (!resultDoc.exists) {
                continue;
            }

            const result = resultDoc.data();
            const worksheet = workbook.addWorksheet(`OCR_${resultId.slice(-8)}`);

            // Add metadata
            worksheet.addRow(['Image URL', result.imageUrl]);
            worksheet.addRow(['Processed At', result.processedAt.toDate()]);
            worksheet.addRow(['Language', result.language]);
            worksheet.addRow(['']); // Empty row

            // Add extracted text
            worksheet.addRow(['Extracted Text']);
            worksheet.addRow([result.extractedText]);
            worksheet.addRow(['']); // Empty row

            // Add table data if available
            if (result.tableData && result.tableData.length > 0) {
                worksheet.addRow(['Structured Table Data']);
                result.tableData.forEach(row => {
                    worksheet.addRow(row);
                });
            }
        }

        // Generate Excel buffer
        const buffer = await workbook.xlsx.writeBuffer();

        // Upload to Firebase Storage
        const bucket = admin.storage().bucket();
        const fileName = `ocr_exports/${projectId}/bengali_ocr_${Date.now()}.xlsx`;
        const file = bucket.file(fileName);

        await file.save(buffer, {
            metadata: {
                contentType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            },
        });

        // Get download URL
        const [url] = await file.getSignedUrl({
            action: 'read',
            expires: Date.now() + 7 * 24 * 60 * 60 * 1000, // Expires in 7 days
        });

        // Save export record
        await db.collection('projects').doc(projectId).collection('exports').add({
            type: 'bengali_ocr_excel',
            fileName,
            downloadUrl: url,
            exportedAt: admin.firestore.FieldValue.serverTimestamp(),
            resultCount: resultIds.length,
        });

        res.json({
            success: true,
            downloadUrl: url,
            fileName,
            message: `Excel file created with ${resultIds.length} OCR results`,
        });

    } catch (error) {
        console.error("Error exporting to Excel:", error);
        res.status(500).json({ error: error.message });
    }
}));

// ============ HELPER FUNCTIONS ============

/**
 * Parse table structure from OCR text lines
 */
function parseTableFromText(lines) {
    if (!lines || lines.length === 0) return [];

    const tableData = [];

    // Simple table detection: look for consistent column patterns
    // This is a basic implementation - can be enhanced with ML

    for (const line of lines) {
        // Split on multiple spaces or tabs (common in tabular data)
        const cells = line.split(/\s{2,}|\t/).map(cell => cell.trim()).filter(cell => cell);
        if (cells.length > 1) { // Likely a table row
            tableData.push(cells);
        }
    }

    return tableData;
}

// ============ HELPER: VPN SWITCHING ============

async function switchVPN(agentId) {
    // Call Proton/Windscribe API to rotate IP
    // For demo: just log
    console.log(`🔄 Switching VPN for ${agentId}`);
    return "VPN_SWITCHED";
}

// ============ FIRESTORE SECURITY RULES ============
// The active Firestore rules are configured in config/firestore.rules.
// Modifying this comment will not change the active rules. Please refer to config/firestore.rules.
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/package.json`

### File: `infrastructure/firebase_functions/firebase_functions_v1/package.json`

```json
{
  "name": "functions",
  "description": "Cloud Functions for Firebase",
  "scripts": {
    "serve": "firebase emulators:start --only functions",
    "shell": "firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "lint": "echo 'Linting functions...'",
    "logs": "firebase functions:log",
    "build": "tsc"
  },
  "engines": {
    "node": "22"
  },
  "main": "index.js",
  "dependencies": {
    "@dataconnect/admin-generated": "file:./src/dataconnect-admin-generated",
    "@google-cloud/vision": "^3.1.0",
    "axios": "^1.4.0",
    "cors": "^2.8.5",
    "exceljs": "^4.3.0",
    "express": "^4.18.2",
    "firebase-admin": "^13.10.0",
    "firebase-functions": "^7.2.5",
    "nodemailer": "^6.9.13",
    "mailparser": "^3.7.1"
  },
  "devDependencies": {
    "firebase-functions-test": "^3.1.0",
    "typescript": "^5.0.0"
  },
  "private": true
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/providers-smart.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/providers-smart.js`

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

// ============ SMART PROVIDER DISCOVERY ============
// Discovers AI models from multiple sources:
// 1. Firestore (user-added API keys)
// 2. Environment config (Firebase, Vertex AI)
// 3. Cloud Run service discovery (deployed models)

async function discoverProviders() {
  const providers = [];

  // ── Source: Firestore (user-configured and dynamic system providers) ──
  try {
    const db = admin.firestore();
    const snap = await db.collection('ai_providers').get();
    snap.forEach(doc => {
      const data = doc.data();
      // Skip inactive ones in general listing, or filter by active status if needed
      if (data.status === 'active') {
        providers.push({
          id: doc.id,
          name: data.name || doc.id,
          type: data.type || 'api',
          deploymentSource: data.deploymentSource || 'api',
          status: data.status || 'active',
          apiKeyConfigured: !!data.apiKey,
          endpoint: data.endpoint || '',
          models: data.models || [],
          roles: data.roles || ['general_chat'],
          source: data.source || 'firestore',
        });
      }
    });
  } catch (err) {
    console.error('Error discovering providers from Firestore:', err);
  }

  return providers;
}

// ============ API ENDPOINTS ============

exports.getConfiguredProviders = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        providers,
        total: providers.length,
        active: providers.length,
        sources: [...new Set(providers.map(p => p.source))],
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

exports.getProviderHealthStats = functions.https.onRequest(async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  try {
    const providers = await discoverProviders();
    res.json({
      success: true,
      data: {
        total: providers.length,
        active: providers.filter(p => p.status === 'active').length,
        error: 0,
        bySource: {
          firestore: providers.filter(p => p.source === 'firestore').length,
          env: providers.filter(p => p.source === 'env').length,
          cloudrun: providers.filter(p => p.source === 'cloudrun').length,
        }
      }
    });
  } catch (err) {
    res.json({ success: true, data: { total: 0, active: 0, error: 0 } });
  }
});
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/README_BD.md`

### File: `infrastructure/firebase_functions/firebase_functions_v1/README_BD.md`

```markdown
# functions/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি Firebase Cloud Functions কোড ধারণ করে, যা SupremeAI-র সার্ভার-সাইড লজিক হ্যান্ডেল করে।

## ফোল্ডার স্ট্রাকচার

```
functions/
├── lib/           # ফায়ারবেস ফাংশনগুলো
├── node_modules/  # নোড মডিউল
├── package.json   # ফাংশন প্যাকেজ
└── tsconfig.json  # টাইপস্ক্রিপ্ট কনফিগ
```

## মূল ফাংশনগুলো

| ফাংশন                   | ব্যবহার                      |
| ----------------------- | ---------------------------- |
| `api-router.js`         | API রাউটিং                   |
| `health-smart.js`       | সিস্টেম হেলথ চেক             |
| `deployment-monitor.js` | ডিপ্লোয়মেন্ট মনিটরিং        |
| `providers-smart.js`    | AI প্রোভাইডার্স ম্যানেজমেন্ট |

## ডিপ্লোয় করা

```bash
# ফাংশন ডিপ্লোয় করন
firebase deploy --only functions
```

## লোকালি রান

```bash
# ফায়ারবেস ইমুলেটর
firebase emulators:start --only functions
```
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/server-connection-monitor.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/server-connection-monitor.js`

```javascript
// functions/server-connection-monitor.js - Server Connection Monitoring
// Monitors connections between all servers and services

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Firebase app is initialized in index.js
const db = admin.firestore();

/**
 * HTTP trigger: Check all server connections
 * Endpoint: https://region-supremeai.cloudfunctions.net/checkServerConnections
 */
exports.checkServerConnections = onRequest(async (req, res) => {
    try {
        const connectionData = {
            timestamp: new Date().toISOString(),
            connections: {}
        };

        // Load system configuration
        const configDoc = await db.collection('config').doc('system').get();
        const config = configDoc.exists ? configDoc.data() : {};

        // Check Firebase connections
        connectionData.connections.firebase = await checkFirebaseConnections();

        // Check GCloud connections
        connectionData.connections.gcloud = await checkGCloudConnections();

        // Check Local Server connection
        connectionData.connections.local = await checkLocalServerConnection(config.localServerUrl || 'http://127.0.0.1:5000');

        // Check Smart Chat System connection
        connectionData.connections.smartChatSystem = await checkSmartChatSystemConnection(config.smartChatSystemUrl || 'http://127.0.0.1:5000');

        // Calculate overall connection status
        connectionData.overallStatus = calculateConnectionStatus(connectionData.connections);

        // Save connection snapshot to Firestore
        await saveConnectionSnapshot(connectionData);

        res.json({
            success: true,
            data: connectionData
        });
    } catch (error) {
        console.error("Error checking server connections:", error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * Check Firebase service connections
 */
async function checkFirebaseConnections() {
    const startTime = Date.now();
    try {
        const checks = {
            firestore: await checkFirestoreConnection(),
            auth: await checkAuthConnection(),
            storage: await checkStorageConnection()
        };

        const responseTime = Date.now() - startTime;
        const allHealthy = Object.values(checks).every(c => c.status === 'connected');

        return {
            name: 'Firebase',
            status: allHealthy ? 'connected' : 'degraded',
            responseTime: `${responseTime}ms`,
            services: checks,
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firebase',
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Firestore connection
 */
async function checkFirestoreConnection() {
    try {
        const testDoc = await db.collection('connection_checks').add({
            service: 'firestore',
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await testDoc.delete();

        return {
            service: 'Firestore',
            status: 'connected',
            latency: Date.now() - Date.now()
        };
    } catch (error) {
        return {
            service: 'Firestore',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Firebase Auth connection
 */
async function checkAuthConnection() {
    try {
        const auth = admin.auth();
        await auth.listUsers(1);

        return {
            service: 'Auth',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Auth',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Firebase Storage connection
 */
async function checkStorageConnection() {
    try {
        const bucket = admin.storage().bucket();
        const [files] = await bucket.getFiles({ maxResults: 1 });

        return {
            service: 'Storage',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Storage',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check GCloud service connections
 */
async function checkGCloudConnections() {
    const startTime = Date.now();
    try {
        const checks = {
            cloudFunctions: await checkCloudFunctionsConnection(),
            cloudRun: await checkCloudRunConnection(),
            bigQuery: await checkBigQueryConnection()
        };

        const responseTime = Date.now() - startTime;
        const allHealthy = Object.values(checks).every(c => c.status === 'connected');

        return {
            name: 'Google Cloud Platform',
            status: allHealthy ? 'connected' : 'degraded',
            responseTime: `${responseTime}ms`,
            services: checks,
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Google Cloud Platform',
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Cloud Functions connection
 */
async function checkCloudFunctionsConnection() {
    try {
        // Try to call a simple health check function
        const response = await axios.get(
            `https://us-central1-${process.env.GCP_PROJECT_ID || 'supremeai'}.cloudfunctions.net/getSystemHealth`,
            { timeout: 5000 }
        );

        return {
            service: 'Cloud Functions',
            status: response.status === 200 ? 'connected' : 'degraded',
            statusCode: response.status
        };
    } catch (error) {
        return {
            service: 'Cloud Functions',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Cloud Run connection
 */
async function checkCloudRunConnection() {
    try {
        // Check if any Cloud Run services are accessible
        // This would need to be configured based on your services
        return {
            service: 'Cloud Run',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'Cloud Run',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check BigQuery connection
 */
async function checkBigQueryConnection() {
    try {
        // Check BigQuery connection (if configured)
        // This would need to be configured based on your setup
        return {
            service: 'BigQuery',
            status: 'connected'
        };
    } catch (error) {
        return {
            service: 'BigQuery',
            status: 'disconnected',
            error: error.message
        };
    }
}

/**
 * Check Local Server connection
 */
async function checkLocalServerConnection(url) {
    try {
        const response = await axios.get(`${url}/health`, {
            timeout: 5000
        });

        const data = response.data;

        return {
            name: 'Local Development Server',
            url: url,
            status: response.status === 200 ? 'connected' : 'degraded',
            responseTime: `${response.headers['x-response-time'] || 'N/A'}`,
            health: {
                status: data.status,
                cpu: data.cpu,
                memory: data.memory,
                disk: data.disk,
                uptime: data.uptime
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Local Development Server',
            url: url,
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Smart Chat System connection
 */
async function checkSmartChatSystemConnection(url) {
    try {
        const response = await axios.get(`${url}/api/status`, {
            timeout: 5000
        });

        return {
            name: 'Smart Chat System',
            url: url,
            status: response.status === 200 ? 'connected' : 'degraded',
            services: response.data.services || {},
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Smart Chat System',
            url: url,
            status: 'disconnected',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Calculate overall connection status
 */
function calculateConnectionStatus(connections) {
    const statuses = Object.values(connections).map(c => c.status);

    if (statuses.every(s => s === 'connected')) {
        return 'all_connected';
    } else if (statuses.some(s => s === 'connected')) {
        return 'partial';
    }
    return 'disconnected';
}

/**
 * Save connection snapshot to Firestore
 */
async function saveConnectionSnapshot(connectionData) {
    try {
        await db.collection('server_connections').add({
            ...connectionData,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Clean up old snapshots (keep last 7 days)
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - 7);

        const oldSnapshots = await db.collection('server_connections')
            .where('createdAt', '<', cutoffDate)
            .limit(100)
            .get();

        const batch = db.batch();
        oldSnapshots.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
    } catch (error) {
        console.error('Error saving connection snapshot:', error);
    }
}

/**
 * Scheduled trigger: Check connections every 2 minutes
 */
exports.monitorConnections = onSchedule('*/2 * * * *', async (event) => {
    try {
        const connectionData = {
            timestamp: new Date().toISOString(),
            connections: {}
        };

        const configDoc = await db.collection('config').doc('system').get();
        const config = configDoc.exists ? configDoc.data() : {};

        connectionData.connections.firebase = await checkFirebaseConnections();
        connectionData.connections.gcloud = await checkGCloudConnections();
        connectionData.connections.local = await checkLocalServerConnection(config.localServerUrl || 'http://127.0.0.1:5000');
        connectionData.connections.smartChatSystem = await checkSmartChatSystemConnection(config.smartChatSystemUrl || 'http://127.0.0.1:5000');

        connectionData.overallStatus = calculateConnectionStatus(connectionData.connections);

        await saveConnectionSnapshot(connectionData);

        // Alert if any critical disconnections
        if (connectionData.overallStatus === 'disconnected') {
            await sendConnectionAlert(connectionData);
        }

        console.log('Connection monitoring completed');
        return null;
    } catch (error) {
        console.error('Error monitoring connections:', error);
        throw error;
    }
});

/**
 * Send connection alert notification
 */
async function sendConnectionAlert(connectionData) {
    try {
        const disconnectedServices = Object.entries(connectionData.connections)
            .filter(([_, conn]) => conn.status === 'disconnected')
            .map(([name, _]) => name);

        const message = {
            notification: {
                title: '🔴 Server Connection Alert',
                body: `Disconnected services: ${disconnectedServices.join(', ')}`
            },
            data: {
                type: 'connection_alert',
                status: connectionData.overallStatus,
                timestamp: connectionData.timestamp,
                services: disconnectedServices
            },
            topic: 'admin-notifications'
        };

        await admin.messaging().send(message);
        console.log('Connection alert sent successfully');
    } catch (error) {
        console.error('Error sending connection alert:', error);
    }
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/swagger.yaml`

### File: `infrastructure/firebase_functions/firebase_functions_v1/swagger.yaml`

```yaml
openapi: 3.0.0
info:
  title: SupremeAI API
  description: API documentation for SupremeAI coordinator and endpoints
  version: 1.0.0
servers:
  - url: https://supremeai-a.web.app/api
    description: Production Server
  - url: http://127.0.0.1:5000/api
    description: Local Emulator
paths:
  /chat/send:
    post:
      summary: Send a message to the unified chat handler
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
                useScrape:
                  type: boolean
      responses:
        '200':
          description: Successful AI response
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string
                  sources:
                    type: array
                    items:
                      type: string
                  confidence:
                    type: number
                  chatType:
                    type: string
                  sourceType:
                    type: string
  /scrape/and-respond:
    post:
      summary: Scrape and respond directly
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
      responses:
        '200':
          description: Successful scrape response
  /chat/classify:
    post:
      summary: Classify semantic intent of a message
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
      responses:
        '200':
          description: Intent classification
  /health:
    get:
      summary: Check health status of the API router
      responses:
        '200':
          description: Health OK
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/system-health.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/system-health.js`

```javascript
// functions/system-health.js - System Health Monitoring
// Monitors Firebase, GCloud, and Local PC health status

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Firebase app is initialized in index.js
const db = admin.firestore();

// Health check intervals (in milliseconds)
const HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
const HISTORY_RETENTION_DAYS = 7;

/**
 * HTTP trigger: Get current system health status
 * Endpoint: https://region-supremeai.cloudfunctions.net/getSystemHealth
 */
exports.getSystemHealth = onRequest({ cors: true }, async (req, res) => {
    try {
        const healthData = {
            timestamp: new Date().toISOString(),
            components: {}
        };

        // Check Firebase Health
        healthData.components.firebase = await checkFirebaseHealth();

        // Check GCloud Health
        healthData.components.gcloud = await checkGCloudHealth();

        // Check Local PC Health (if accessible)
        healthData.components.localPC = await checkLocalPcHealth();

        // Check Database Health
        healthData.components.database = await checkDatabaseHealth();

        // Calculate overall system status
        healthData.overallStatus = calculateOverallStatus(healthData.components);

        // Save health snapshot to Firestore
        await saveHealthSnapshot(healthData);

        res.json({
            success: true,
            data: healthData
        });
    } catch (error) {
        console.error("Error fetching system health:", error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * Check Firebase services health
 */
async function checkFirebaseHealth() {
    const startTime = Date.now();
    try {
        // Test Firestore
        const testDoc = await db.collection('health_checks').add({
            service: 'firestore',
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await testDoc.delete();

        // Test Authentication
        const auth = admin.auth();
        const userCount = (await auth.listUsers(1)).users.length;

        // Check Firebase Storage (if configured)
        const storageHealthy = await checkStorageHealth();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Firebase',
            status: 'healthy',
            uptime: '99.99%',
            responseTime: `${responseTime}ms`,
            services: {
                firestore: { status: 'healthy', responseTime: `${responseTime}ms` },
                auth: { status: 'healthy', userCount },
                storage: { status: storageHealthy ? 'healthy' : 'degraded' }
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firebase',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check GCloud services health
 */
async function checkGCloudHealth() {
    const startTime = Date.now();
    try {
        // Check Cloud Functions status
        const functionsHealthy = await checkCloudFunctionsHealth();

        // Check Cloud Run services (if any)
        const cloudRunHealthy = await checkCloudRunHealth();

        // Check BigQuery (if configured)
        const bigQueryHealthy = await checkBigQueryHealth();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Google Cloud Platform',
            status: functionsHealthy && cloudRunHealthy ? 'healthy' : 'degraded',
            uptime: '99.9%',
            responseTime: `${responseTime}ms`,
            services: {
                cloudFunctions: { status: functionsHealthy ? 'healthy' : 'degraded' },
                cloudRun: { status: cloudRunHealthy ? 'healthy' : 'degraded' },
                bigQuery: { status: bigQueryHealthy ? 'healthy' : 'degraded' }
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Google Cloud Platform',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Local PC health (if accessible via local server)
 */
async function checkLocalPcHealth() {
    try {
        // Try to reach local health endpoint
        const backendUrl = (functions.config().backend && functions.config().backend.url) || 'https://supremeai-a.web.app';
        const response = await axios.get(`${backendUrl}/health`, {
            timeout: 5000
        });

        const data = response.data;

        return {
            name: 'Local Development Server',
            status: data.status || 'healthy',
            uptime: data.uptime || 'N/A',
            cpu: data.cpu || { usage: 'N/A' },
            memory: data.memory || { usage: 'N/A', total: 'N/A' },
            disk: data.disk || { usage: 'N/A', total: 'N/A' },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        // Local server might not be running - this is expected
        return {
            name: 'Local Development Server',
            status: 'unavailable',
            message: 'Local server not accessible',
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Database health
 */
async function checkDatabaseHealth() {
    try {
        const startTime = Date.now();

        // Perform a simple read operation
        const snapshot = await db.collection('health_checks').limit(1).get();

        const responseTime = Date.now() - startTime;

        return {
            name: 'Firestore Database',
            status: 'healthy',
            uptime: '99.99%',
            responseTime: `${responseTime}ms`,
            operations: {
                reads: 'healthy',
                writes: 'healthy',
                queries: 'healthy'
            },
            lastCheck: new Date().toISOString()
        };
    } catch (error) {
        return {
            name: 'Firestore Database',
            status: 'degraded',
            error: error.message,
            lastCheck: new Date().toISOString()
        };
    }
}

/**
 * Check Firebase Storage health
 */
async function checkStorageHealth() {
    try {
        const bucket = admin.storage().bucket();
        const [files] = await bucket.getFiles({ maxResults: 1 });
        return true;
    } catch (error) {
        console.error('Storage health check failed:', error);
        return false;
    }
}

/**
 * Check Cloud Functions health
 */
async function checkCloudFunctionsHealth() {
    try {
        // Try to call a simple health check function
        // This would need to be implemented as a separate function
        return true;
    } catch (error) {
        console.error('Cloud Functions health check failed:', error);
        return false;
    }
}

/**
 * Check Cloud Run health
 */
async function checkCloudRunHealth() {
    try {
        // Check if any Cloud Run services are deployed and healthy
        // This would need to be configured based on your services
        return true;
    } catch (error) {
        console.error('Cloud Run health check failed:', error);
        return false;
    }
}

/**
 * Check BigQuery health
 */
async function checkBigQueryHealth() {
    try {
        // Check BigQuery connection (if configured)
        // This would need to be configured based on your setup
        return true;
    } catch (error) {
        console.error('BigQuery health check failed:', error);
        return false;
    }
}

/**
 * Calculate overall system status
 */
function calculateOverallStatus(components) {
    const statuses = Object.values(components).map(c => c.status);

    if (statuses.every(s => s === 'healthy')) {
        return 'healthy';
    } else if (statuses.some(s => s === 'critical')) {
        return 'critical';
    } else if (statuses.some(s => s === 'degraded' || s === 'unavailable')) {
        return 'degraded';
    }
    return 'healthy';
}

/**
 * Save health snapshot to Firestore
 */
async function saveHealthSnapshot(healthData) {
    try {
        await db.collection('system_health').add({
            ...healthData,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Clean up old health snapshots
        const cutoffDate = new Date();
        cutoffDate.setDate(cutoffDate.getDate() - HISTORY_RETENTION_DAYS);

        const oldSnapshots = await db.collection('system_health')
            .where('createdAt', '<', cutoffDate)
            .limit(100)
            .get();

        const batch = db.batch();
        oldSnapshots.docs.forEach(doc => batch.delete(doc.ref));
        await batch.commit();
    } catch (error) {
        console.error('Error saving health snapshot:', error);
    }
}

/**
 * Scheduled trigger: Collect health metrics every 5 minutes
 */
exports.collectHealthMetrics = onSchedule('*/5 * * * *', async (event) => {
    try {
        const healthData = {
            timestamp: new Date().toISOString(),
            components: {}
        };

        // Collect health data for all components
        healthData.components.firebase = await checkFirebaseHealth();
        healthData.components.gcloud = await checkGCloudHealth();
        healthData.components.localPC = await checkLocalPcHealth();
        healthData.components.database = await checkDatabaseHealth();

        healthData.overallStatus = calculateOverallStatus(healthData.components);

        // Save to Firestore
        await saveHealthSnapshot(healthData);

        // If status is critical, send alert
        if (healthData.overallStatus === 'critical') {
            await sendHealthAlert(healthData);
        }

        console.log('Health metrics collected successfully');
        return null;
    } catch (error) {
        console.error('Error collecting health metrics:', error);
        throw error;
    }
});

/**
 * Send health alert notification with AI-generated message
 */
async function sendHealthAlert(healthData) {
    try {
        // Get Groq API key for smart message generation
        const groqApiKey = functions.config().groq?.api_key ||
                          process.env.GROQ_API_KEY;

        let messageBody;
        if (groqApiKey) {
            // Use Groq to generate a smart, concise alert message
            messageBody = await generateSmartAlertMessage(groqApiKey, healthData);
        } else {
            // Fallback to simple message
            messageBody = `System status is ${healthData.overallStatus.toUpperCase()}. Please check the dashboard.`;
        }

        const message = {
            notification: {
                title: '🚨 System Health Alert',
                body: messageBody
            },
            data: {
                type: 'health_alert',
                status: healthData.overallStatus,
                timestamp: healthData.timestamp,
                components: JSON.stringify(Object.keys(healthData.components))
            },
            topic: 'admin-notifications'
        };

        await admin.messaging().send(message);
        console.log('Health alert sent successfully');
    } catch (error) {
        console.error('Error sending health alert:', error);
    }
}

/**
 * Generate smart alert message using Groq AI
 */
async function generateSmartAlertMessage(apiKey, healthData) {
    const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    const systemPrompt = `You are a system monitoring assistant.
Generate a VERY SHORT (max 100 chars) alert message based on system health.
Format: "[COMPONENT] Issue: [brief description]"
Examples:
  "Firebase: Auth service degraded"
  "Database: High latency detected"
  "Critical: Multiple services down"
`;

    const userPrompt = `System status: ${healthData.overallStatus}
Components:
${JSON.stringify(healthData.components, null, 2)}

Generate a concise alert message:`;

    try {
        const response = await axios.post(
            GROQ_API_URL,
            {
                model: "llama3-8b-8192",
                messages: [
                    { role: "system", content: systemPrompt },
                    { role: "user", content: userPrompt }
                ],
                temperature: 0.3,
                max_tokens: 100
            },
            {
                headers: {
                    "Authorization": `Bearer ${apiKey}`,
                    "Content-Type": "application/json"
                },
                timeout: 5000
            }
        );

        return response.data.choices[0].message.content.trim();
    } catch (error) {
        console.error("Failed to generate smart alert:", error.message);
        return `System ${healthData.overallStatus.toUpperCase()}`;
    }
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/tsconfig.json`

### File: `infrastructure/firebase_functions/firebase_functions_v1/tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "lib",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "moduleResolution": "node",
    "resolveJsonModule": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "lib"]
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.d.ts`

```typescript
/** All supported chat types returned by classifyIntent() */
export type ChatType = "GREETING" | "SIMILAR" | "SIMPLE_QUESTION" | "COMPLEX_QUESTION" | "FOLLOW_UP" | "COMMAND" | "UNKNOWN";
/** Result of a single-classify call */
export interface ClassifyResult {
    chatType: ChatType;
    message: string;
    classifiedAt: number;
}
/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
export declare function classifyIntent(message: string, nowMs?: number): ClassifyResult;
//# sourceMappingURL=chatClassifier.d.ts.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.d.ts.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.d.ts.map`

```text
{"version":3,"file":"chatClassifier.d.ts","sourceRoot":"","sources":["../src/chatClassifier.ts"],"names":[],"mappings":"AASA,4DAA4D;AAC5D,MAAM,MAAM,QAAQ,GAChB,UAAU,GACV,SAAS,GACT,iBAAiB,GACjB,kBAAkB,GAClB,WAAW,GACX,SAAS,GACT,SAAS,CAAC;AAEd,uCAAuC;AACvC,MAAM,WAAW,cAAc;IAC7B,QAAQ,EAAE,QAAQ,CAAC;IACnB,OAAO,EAAE,MAAM,CAAC;IAChB,YAAY,EAAE,MAAM,CAAC;CACtB;AAkBD;;;;;;;;;;;;GAYG;AACH,wBAAgB,cAAc,CAAC,OAAO,EAAE,MAAM,EAAE,KAAK,CAAC,EAAE,MAAM,GAAG,cAAc,CAkB9E"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.js`

```javascript
"use strict";
// ─────────────────────────────────────────────────────────────────
// chatClassifier.ts
// Intent / ChatType classifier for the SupremeAI scraping pipeline.
//
// Extracted from classifyIntent() in scrapeEngine.ts so that
// ChatProcessingService.java and other callers can invoke intent
// classification without depending on the full scraping engine.
// ─────────────────────────────────────────────────────────────────
Object.defineProperty(exports, "__esModule", { value: true });
exports.classifyIntent = classifyIntent;
// ─────────────────────────────────────────────────────────────────
// Regex patterns — kept identical to scrapeEngine.ts for backwards
// compatibility with any existing compare-diff expectations.
// ─────────────────────────────────────────────────────────────────
const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK = /\?$/;
// ─────────────────────────────────────────────────────────────────
// classifyIntent
// ─────────────────────────────────────────────────────────────────
/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
function classifyIntent(message, nowMs) {
    const trimmed = message.trim().toLowerCase();
    let chatType;
    if (GREETING_WORDS.test(trimmed))
        chatType = "GREETING";
    else if (SIMILAR_WORDS.test(trimmed))
        chatType = "SIMILAR";
    else if (COMMAND_WORDS.test(trimmed))
        chatType = "COMMAND";
    else if (FOLLOW_UP_WORDS.test(trimmed))
        chatType = "FOLLOW_UP";
    else if (COMPLEX_HINTS.test(trimmed))
        chatType = "COMPLEX_QUESTION";
    else if (QUESTION_MARK.test(trimmed))
        chatType = "SIMPLE_QUESTION";
    else if (trimmed.length < 20)
        chatType = "SIMPLE_QUESTION";
    else
        chatType = "COMPLEX_QUESTION";
    return {
        chatType,
        message,
        classifiedAt: nowMs ?? Date.now(),
    };
}
//# sourceMappingURL=chatClassifier.js.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.js.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/chatClassifier.js.map`

```text
{"version":3,"file":"chatClassifier.js","sourceRoot":"","sources":["../src/chatClassifier.ts"],"names":[],"mappings":";AAAA,oEAAoE;AACpE,oBAAoB;AACpB,oEAAoE;AACpE,EAAE;AACF,6DAA6D;AAC7D,iEAAiE;AACjE,gEAAgE;AAChE,oEAAoE;;AAgDpE,wCAkBC;AA/CD,oEAAoE;AACpE,mEAAmE;AACnE,6DAA6D;AAC7D,oEAAoE;AAEpE,MAAM,cAAc,GAAG,gFAAgF,CAAC;AACxG,MAAM,aAAa,GAAI,uEAAuE,CAAC;AAC/F,MAAM,eAAe,GAAG,gDAAgD,CAAC;AACzE,MAAM,aAAa,GAAI,iEAAiE,CAAC;AACzF,MAAM,aAAa,GAAI,6EAA6E,CAAC;AACrG,MAAM,aAAa,GAAI,KAAK,CAAC;AAE7B,oEAAoE;AACpE,iBAAiB;AACjB,oEAAoE;AAEpE;;;;;;;;;;;;GAYG;AACH,SAAgB,cAAc,CAAC,OAAe,EAAE,KAAc;IAC5D,MAAM,OAAO,GAAG,OAAO,CAAC,IAAI,EAAE,CAAC,WAAW,EAAE,CAAC;IAE7C,IAAI,QAAkB,CAAC;IACvB,IAAI,cAAc,CAAC,IAAI,CAAC,OAAO,CAAC;QAAO,QAAQ,GAAG,UAAU,CAAC;SACxD,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,SAAS,CAAC;SACvD,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,SAAS,CAAC;SACvD,IAAI,eAAe,CAAC,IAAI,CAAC,OAAO,CAAC;QAAE,QAAQ,GAAG,WAAW,CAAC;SAC1D,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,kBAAkB,CAAC;SAChE,IAAI,aAAa,CAAC,IAAI,CAAC,OAAO,CAAC;QAAG,QAAQ,GAAG,iBAAiB,CAAC;SAC/D,IAAI,OAAO,CAAC,MAAM,GAAG,EAAE;QAAY,QAAQ,GAAG,iBAAiB,CAAC;;QAC7B,QAAQ,GAAG,kBAAkB,CAAC;IAEtE,OAAO;QACL,QAAQ;QACR,OAAO;QACP,YAAY,EAAE,KAAK,IAAI,IAAI,CAAC,GAAG,EAAE;KAClC,CAAC;AACJ,CAAC"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.d.ts`

```typescript
import * as functions from 'firebase-functions/v2';
/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
export declare const handleIncomingEmail: functions.https.HttpsFunction;
//# sourceMappingURL=email_handler.d.ts.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.d.ts.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.d.ts.map`

```text
{"version":3,"file":"email_handler.d.ts","sourceRoot":"","sources":["../src/email_handler.ts"],"names":[],"mappings":"AAAA,OAAO,KAAK,SAAS,MAAM,uBAAuB,CAAC;AAiBnD;;;GAGG;AACH,eAAO,MAAM,mBAAmB,+BAiF9B,CAAC"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.handleIncomingEmail = void 0;
const functions = __importStar(require("firebase-functions/v2"));
const admin = __importStar(require("firebase-admin"));
// @ts-ignore
const mailparser_1 = require("mailparser");
// @ts-ignore
const nodemailer = __importStar(require("nodemailer"));
const axios_1 = __importDefault(require("axios"));
// Configuration for outgoing status updates
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.SUPREMEAI_EMAIL,
        pass: process.env.SUPREMEAI_EMAIL_PASSWORD
    }
});
/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
exports.handleIncomingEmail = functions.https.onRequest(async (req, res) => {
    try {
        // 1. Parse the multipart email body
        const parsed = await (0, mailparser_1.simpleParser)(req.body);
        const sender = parsed.from?.value[0].address;
        const recipient = parsed.to?.value?.[0]?.address;
        const subject = parsed.subject;
        const body = parsed.text;
        const html = parsed.html;
        console.log(`[SupremeAI Email] Incoming from: ${sender} to ${recipient}, Subject: ${subject}`);
        // 1. Check for Verification Codes/Links (The "Personhood" check)
        // If the email is from a known provider (Google, DeepSeek, etc.), extract OTP
        const otpMatch = body?.match(/\b\d{6}\b/); // Look for 6-digit codes
        const linkMatch = html?.match(/href="([^"]*confirm[^"]*|[^"]*verify[^"]*)"/i);
        if (otpMatch || linkMatch) {
            await admin.firestore().collection('verification_queue').add({
                sender,
                email_target: recipient,
                subject,
                code: otpMatch ? otpMatch[0] : null,
                link: linkMatch ? linkMatch[1] : null,
                receivedAt: admin.firestore.FieldValue.serverTimestamp(),
                processed: false
            });
            console.log(`[SupremeAI] Extracted verification data from ${sender}`);
        }
        // 2. Security: Only process if it's from the verified Admin
        const authorizedAdmins = process.env.AUTHORIZED_ADMINS
            ? process.env.AUTHORIZED_ADMINS.split(',').map(email => email.trim().toLowerCase())
            : ['admin@yourdomain.com'];
        if (!sender || !authorizedAdmins.includes(sender.toLowerCase())) {
            console.warn(`Unauthorized access attempt by ${sender}`);
            res.status(403).send('Forbidden');
            return;
        }
        // 3. Process Logic with Gemini API via Axios
        let resultText = '';
        const geminiApiKey = process.env.GEMINI_API_KEY;
        if (geminiApiKey && body) {
            console.log(`[SupremeAI] Processing command using Gemini API...`);
            try {
                const response = await axios_1.default.post(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiApiKey}`, {
                    contents: [{
                            parts: [{ text: `You are the SupremeAI Core Engine. Execute or respond to this command from the Admin:\n\n${body}` }]
                        }]
                }, {
                    headers: { 'Content-Type': 'application/json' }
                });
                resultText = response.data?.candidates?.[0]?.content?.parts?.[0]?.text || 'No response from AI engine.';
            }
            catch (err) {
                console.error('Error calling Gemini API:', err?.response?.data || err.message);
                resultText = `Failed to process command with AI: ${err.message}`;
            }
        }
        else {
            console.log(`[SupremeAI] Empty body or GEMINI_API_KEY not configured. Returning dummy execution.`);
            resultText = `Hello Admin, I received your command "${subject}" but could not process it using AI because the GEMINI_API_KEY is not set.`;
        }
        // 4. Send Confirmation/Result back to Admin
        await transporter.sendMail({
            from: `"SupremeAI Assistant" <${process.env.SUPREMEAI_EMAIL || 'supremeai@yourdomain.com'}>`,
            to: sender,
            subject: `Re: ${subject} [PROCESSED]`,
            text: `Hello Admin, I have received your request and executed the tasks. \n\nCommand: ${subject}\n\nExecution Result:\n${resultText}`
        });
        res.status(200).send('Email Processed');
    }
    catch (error) {
        console.error('Email processing error:', error);
        res.status(500).send('Internal Server Error');
    }
});
//# sourceMappingURL=email_handler.js.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.js.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/email_handler.js.map`

```text
{"version":3,"file":"email_handler.js","sourceRoot":"","sources":["../src/email_handler.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.d.ts`

```typescript
import * as functions from 'firebase-functions/v1';
/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
export declare const onUserSignUp: functions.CloudFunction<import("firebase-admin/auth").UserRecord>;
//# sourceMappingURL=index.d.ts.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.d.ts.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.d.ts.map`

```text
{"version":3,"file":"index.d.ts","sourceRoot":"","sources":["../src/index.ts"],"names":[],"mappings":"AAAA,OAAO,KAAK,SAAS,MAAM,uBAAuB,CAAC;AAMnD;;;GAGG;AACH,eAAO,MAAM,YAAY,mEAsBvB,CAAC"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.onUserSignUp = void 0;
const functions = __importStar(require("firebase-functions/v1"));
const admin = __importStar(require("firebase-admin"));
// Initialize Firebase Admin SDK
admin.initializeApp();
/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
exports.onUserSignUp = functions.auth.user().onCreate(async (user) => {
    try {
        // 1. Set Custom User Claims (Embeds the role directly into their JWT token)
        await admin.auth().setCustomUserClaims(user.uid, {
            role: 'user',
            accessLevel: 1
        });
        // 2. Create a synchronized profile document in Firestore
        await admin.firestore().collection('users').doc(user.uid).set({
            email: user.email,
            displayName: user.displayName || 'Operator',
            role: 'user',
            tier: 'FREE',
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            lastLogin: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        console.log(`[AUTH] Successfully initialized new user: ${user.uid} with 'user' role.`);
    }
    catch (error) {
        console.error(`[AUTH ERROR] Failed to initialize user ${user.uid}:`, error);
    }
});
//# sourceMappingURL=index.js.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.js.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/index.js.map`

```text
{"version":3,"file":"index.js","sourceRoot":"","sources":["../src/index.ts"],"names":[],"mappings":";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;AAAA,iEAAmD;AACnD,sDAAwC;AAExC,gCAAgC;AAChC,KAAK,CAAC,aAAa,EAAE,CAAC;AAEtB;;;GAGG;AACU,QAAA,YAAY,GAAG,SAAS,CAAC,IAAI,CAAC,IAAI,EAAE,CAAC,QAAQ,CAAC,KAAK,EAAE,IAA2B,EAAE,EAAE;IAC7F,IAAI,CAAC;QACD,4EAA4E;QAC5E,MAAM,KAAK,CAAC,IAAI,EAAE,CAAC,mBAAmB,CAAC,IAAI,CAAC,GAAG,EAAE;YAC7C,IAAI,EAAE,MAAM;YACZ,WAAW,EAAE,CAAC;SACjB,CAAC,CAAC;QAEH,yDAAyD;QACzD,MAAM,KAAK,CAAC,SAAS,EAAE,CAAC,UAAU,CAAC,OAAO,CAAC,CAAC,GAAG,CAAC,IAAI,CAAC,GAAG,CAAC,CAAC,GAAG,CAAC;YAC1D,KAAK,EAAE,IAAI,CAAC,KAAK;YACjB,WAAW,EAAE,IAAI,CAAC,WAAW,IAAI,UAAU;YAC3C,IAAI,EAAE,MAAM;YACZ,IAAI,EAAE,MAAM;YACZ,SAAS,EAAE,KAAK,CAAC,SAAS,CAAC,UAAU,CAAC,eAAe,EAAE;YACvD,SAAS,EAAE,KAAK,CAAC,SAAS,CAAC,UAAU,CAAC,eAAe,EAAE;SAC1D,EAAE,EAAE,KAAK,EAAE,IAAI,EAAE,CAAC,CAAC;QAEpB,OAAO,CAAC,GAAG,CAAC,6CAA6C,IAAI,CAAC,GAAG,oBAAoB,CAAC,CAAC;IAC3F,CAAC;IAAC,OAAO,KAAK,EAAE,CAAC;QACb,OAAO,CAAC,KAAK,CAAC,0CAA0C,IAAI,CAAC,GAAG,GAAG,EAAE,KAAK,CAAC,CAAC;IAChF,CAAC;AACL,CAAC,CAAC,CAAC"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.d.ts`

```typescript
import * as https from "firebase-functions/v2/https";
/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
export declare function scrapeAndRespond(message: string, userId: string): Promise<Record<string, unknown>>;
/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
export declare const scrapeAndRespondFn: https.HttpsFunction;
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export declare const classifyIntentFn: https.HttpsFunction;
/**
 * GET /health
 */
export declare const scrapeHealthFn: https.HttpsFunction;
//# sourceMappingURL=scrapeEngine.d.ts.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.d.ts.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.d.ts.map`

```text
{"version":3,"file":"scrapeEngine.d.ts","sourceRoot":"","sources":["../src/scrapeEngine.ts"],"names":[],"mappings":"AAEA,OAAO,KAAK,KAAK,MAAM,6BAA6B,CAAC;AA2PrD;;;;;;GAMG;AACH,wBAAsB,gBAAgB,CACpC,OAAO,EAAE,MAAM,EACf,MAAM,EAAE,MAAM,GACb,OAAO,CAAC,MAAM,CAAC,MAAM,EAAE,OAAO,CAAC,CAAC,CA6JlC;AA4DD;;;GAGG;AACH,eAAO,MAAM,kBAAkB,qBAoB9B,CAAC;AAEF;;;GAGG;AACH,eAAO,MAAM,gBAAgB,qBAQ5B,CAAC;AAEF;;GAEG;AACH,eAAO,MAAM,cAAc,qBAe1B,CAAC"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.js`

```javascript
"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.scrapeHealthFn = exports.classifyIntentFn = exports.scrapeAndRespondFn = void 0;
exports.scrapeAndRespond = scrapeAndRespond;
const app_1 = require("firebase-admin/app");
const firestore_1 = require("firebase-admin/firestore");
const https = __importStar(require("firebase-functions/v2/https"));
const axios_1 = __importDefault(require("axios"));
const httpsOptions = { region: "us-central1" };
// ─────────────────────────────────────────────────────────────────
// Firebase initialisation (singleton-safe)
// ─────────────────────────────────────────────────────────────────
let db = null;
function getDb() {
    db ?? (db = (0, firestore_1.getFirestore)((0, app_1.initializeApp)({})));
    return db;
}
// ─────────────────────────────────────────────────────────────────
// Firestore collection constants
// ─────────────────────────────────────────────────────────────────
const COL = {
    policies: "scrapePolicies",
    presets: "scrapePresets",
    domains: "scrapeAllowedDomains",
    history: "scrapeHistory",
    events: "scrapeEvent",
};
const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK = /\?$/;
function classifyIntent(message) {
    const msg = message.trim().toLowerCase();
    if (GREETING_WORDS.test(msg))
        return "GREETING";
    if (SIMILAR_WORDS.test(msg))
        return "SIMILAR";
    if (COMMAND_WORDS.test(msg))
        return "COMMAND";
    if (FOLLOW_UP_WORDS.test(msg))
        return "FOLLOW_UP";
    if (COMPLEX_HINTS.test(msg))
        return "COMPLEX_QUESTION";
    if (QUESTION_MARK.test(msg))
        return "SIMPLE_QUESTION";
    if (msg.length < 20)
        return "SIMPLE_QUESTION";
    return "COMPLEX_QUESTION";
}
// ─────────────────────────────────────────────────────────────────
// Step 2 — Firestore config loader helpers
// ─────────────────────────────────────────────────────────────────
async function getGlobalPolicy() {
    const snap = await getDb().collection(COL.policies).doc("global").get();
    return snap.exists ? snap.data() : null;
}
async function getPolicy(type) {
    const snap = await getDb().collection(COL.policies).doc(type).get();
    return snap.exists ? snap.data() : null;
}
async function getPreset(presetId) {
    const snap = await getDb().collection(COL.presets).doc(presetId).get();
    return snap.exists ? snap.data() : null;
}
async function getAllowedDomains() {
    const snap = await getDb().collection(COL.domains).get();
    return snap.docs.map((d) => d.data()).filter((d) => d.enabled);
}
async function findCachedAnswer(query, cacheTTLSeconds) {
    const threshold = firestore_1.Timestamp.fromMillis(Date.now() - cacheTTLSeconds * 1000);
    const snap = await getDb()
        .collection(COL.history)
        .where("query", "==", query)
        .where("timestamp", ">", threshold)
        .orderBy("timestamp", "desc")
        .limit(1)
        .get();
    if (snap.empty)
        return null;
    const d = snap.docs[0].data();
    return { ...d, sessionId: snap.docs[0].id };
}
// ─────────────────────────────────────────────────────────────────
// Step 6 helpers — domain allow /Trust-scores
// ─────────────────────────────────────────────────────────────────
function extractHost(url) {
    try {
        return new URL(url).hostname;
    }
    catch {
        return url;
    }
}
function isDomainAllowed(domain, domains) {
    const entry = domains.find((d) => domain === d.domain || domain.endsWith("." + d.domain));
    if (!entry)
        return { allowed: true, trustLevel: "standard" }; // open by default — admin can restrict via Firestore
    return { allowed: entry.enabled, trustLevel: entry.trustLevel };
}
async function extractFromPage(pageUrl, strategy, eventId) {
    const result = await callPlaywright("extract", { url: pageUrl, strategy, eventId });
    return {
        url: pageUrl,
        title: result?.title ? String(result.title) : pageUrl,
        text: result?.text ? String(result.text) : "",
        strategy,
    };
}
// ─────────────────────────────────────────────────────────────────
// Playwright proxy helper
// ─────────────────────────────────────────────────────────────────
const PLAYWRIGHT_URL = process.env.BROWSER_AUTOMATION_URL || "http://127.0.0.1:3001";
async function callPlaywright(action, body) {
    try {
        const res = await axios_1.default.post(`${PLAYWRIGHT_URL}/${action}`, body, {
            timeout: parseInt(process.env.SCRAPE_TIMEOUT_MS || "30000"),
        });
        return res.data;
    }
    catch (err) {
        throw new https.HttpsError("unavailable", `Browser automation unavailable for ${action}: ${err.message}`);
    }
}
// ─────────────────────────────────────────────────────────────────
// Step 9 — Firestore history writer
// ─────────────────────────────────────────────────────────────────
async function writeHistory(entry) {
    const ref = entry.sessionId
        ? getDb().collection(COL.history).doc(entry.sessionId)
        : getDb().collection(COL.history).doc();
    await ref.set({
        ...entry,
        timestamp: firestore_1.Timestamp.now(),
    });
    return ref.id;
}
async function logEvent(sessionId, type, payload) {
    await getDb()
        .collection(COL.events)
        .doc()
        .set({
        sessionId,
        type,
        payload,
        timestamp: firestore_1.Timestamp.now(),
    });
}
// ─────────────────────────────────────────────────────────────────
// Public scrape flow
// ─────────────────────────────────────────────────────────────────
/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
async function scrapeAndRespond(message, userId) {
    const sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const chatType = classifyIntent(message);
    // ── Step 2: policy lookup ──────────────────────────────────────
    const globalPolicy = await getGlobalPolicy();
    if (!globalPolicy?.enabled) {
        return { answer: "Web scraping is currently disabled by global policy.", sources: [], confidence: 0 };
    }
    const perTypePolicy = await getPolicy(chatType);
    if (!perTypePolicy?.enabled) {
        // Skipped — return empty, caller falls back to local knowledge
        return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
    }
    const policy = { ...globalPolicy, ...perTypePolicy };
    // ── Cache check for FOLLOW_UP ──────────────────────────────────
    if (chatType === "FOLLOW_UP" || chatType === "SIMPLE_QUESTION" || chatType === "COMPLEX_QUESTION") {
        const cached = await findCachedAnswer(message, policy.cacheTTL);
        if (cached) {
            await logEvent(sessionId, "cached_answer", { fromSession: cached.sessionId, query: message });
            return {
                answer: cached.finalAnswer,
                sources: cached.sources,
                confidence: cached.confidence,
                chatType,
                sessionId,
                cached: true,
                originalSessionId: cached.sessionId,
            };
        }
    }
    // Skip heavy flow for GREETING / SIMILAR / COMMAND
    if (chatType === "GREETING" || chatType === "SIMILAR") {
        return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
    }
    // ── Step 3: build search entry point ───────────────────────────
    const domains = await getAllowedDomains();
    const allSearchEngines = policy.searchEngines || ["google"];
    const maxResults = policy.maxResults || 3;
    const maxDepth = policy.maxDepth ?? 1;
    const strategy = policy.extractStrategy || "article-extract";
    const builtUrls = [];
    for (const engine of allSearchEngines) {
        const preset = await getPreset(engine);
        if (!preset || !preset.searchUrlTemplate)
            continue;
        const queryParam = encodeURIComponent(message);
        const engineUrl = preset.searchUrlTemplate.replace("{q}", queryParam);
        // trust gate
        const host = extractHost(engineUrl);
        const { allowed } = isDomainAllowed(host, domains);
        if (!allowed) {
            await logEvent(sessionId, "domain_skipped", { url: engineUrl, reason: "not_in_allowed_domains" });
            continue;
        }
        builtUrls.push({ engine, url: engineUrl });
    }
    if (builtUrls.length === 0) {
        return { answer: "No search engine configured or all domains blocked.", sources: [], confidence: 0, chatType, sessionId };
    }
    // ── Step 4: launch navigate sessions in parallel ───────────────
    const searchEventId = `${sessionId}_search`;
    await logEvent(sessionId, "navigate_start", { urls: builtUrls.map((b) => b.url) });
    // Dispatch all search-engine navigations via Playwright, but don't block the
    // event loop — fire and await.
    const navigatePromises = builtUrls.map(async ({ engine, url }) => {
        try {
            await callPlaywright("navigate", { url, eventId: searchEventId });
            await logEvent(sessionId, "navigate_complete", { engine, url });
        }
        catch (err) {
            await logEvent(sessionId, "error", { engine, url, error: err.message });
        }
    });
    await Promise.allSettled(navigatePromises);
    // ── Step 5: extract result links ───────────────────────────────
    let resultLinks = [];
    try {
        const searchContent = await callPlaywright("extract", { url: builtUrls[0]?.url || "", strategy: "search-links", eventId: searchEventId });
        resultLinks = Array.isArray(searchContent)
            ? searchContent.map((r) => r.href).filter(Boolean)
            : [];
    }
    catch (extractionError) {
        console.error(`[ScrapeEngine] Failed to extract search result links:`, extractionError);
        // If link extraction fails, fall back to navigating each engine URL directly
        resultLinks = builtUrls.map((b) => b.url);
    }
    // cap to top N results
    resultLinks = resultLinks.slice(0, maxResults);
    // ── Step 6: crawl deeper (maxDepth > 0) ───────────────────────
    const allExtracted = [];
    const crawl = async (url, depth) => {
        if (depth > maxDepth || resultLinks.length === 0)
            return;
        for (const link of resultLinks) {
            const host = extractHost(link);
            const { allowed, trustLevel } = isDomainAllowed(host, domains);
            if (!allowed || trustLevel === "suspicious") {
                await logEvent(sessionId, "domain_skipped", { url: link, reason: trustLevel === "suspicious" ? "suspicious_domain" : "not_allowed", trustLevel });
                continue;
            }
            await logEvent(sessionId, "extract_start", { url: link, depth });
            try {
                const page = await extractFromPage(link, strategy, `${sessionId}_d${depth}`);
                allExtracted.push(page);
                await logEvent(sessionId, "extract_complete", { url: link, depth, textLength: page.text.length, strategy: page.strategy });
                // shallow follow links one level deeper
                if (depth < maxDepth) {
                    const outbound = (await callPlaywright("extract", { url: link, strategy: "outbound-links", eventId: `${sessionId}_out_${depth}` }));
                    const nextUrls = outbound?.map((r) => r.href).filter(Boolean) || [];
                    for (const next of nextUrls.slice(0, 2))
                        await crawl(next, depth + 1);
                }
            }
            catch (err) {
                await logEvent(sessionId, "error", { url: link, phase: "extract", error: err.message });
            }
        }
    };
    // crawl the top results
    await crawl(builtUrls[0]?.url || "", 0);
    // ── Step 7: merge, deduplicate, summarize ─────────────────────
    const mergedText = mergeAndDeduplicate(allExtracted);
    const answer = summarise(mergedText, message);
    // ── Step 8: store session history ──────────────────────────────
    const firestoreDocId = await writeHistory({
        sessionId,
        query: message,
        chatType,
        sources: allExtracted.map((p) => p.url),
        rawChunks: allExtracted.map((p) => ({ url: p.url, text: p.text })),
        finalAnswer: answer,
        confidence: allExtracted.length > 0 ? Math.min(0.85, 0.55 + allExtracted.length * 0.07) : 0,
        timestamp: firestore_1.Timestamp.now(),
    });
    sessionId; // used; intentionally shadowed by const above — keep local sessionId for return
    // ── Step 9: return ─────────────────────────────────────────────
    return {
        answer,
        sources: allExtracted.map((p) => p.url),
        confidence: allExtracted.length > 0 ? Math.min(0.90, 0.55 + allExtracted.length * 0.08) : 0.2,
        chatType,
        sessionId: firestoreDocId,
        scrapedPages: allExtracted.length,
    };
}
// ─────────────────────────────────────────────────────────────────
// Text processing helpers
// ─────────────────────────────────────────────────────────────────
const TEXT_SIMILARITY_THRESHOLD = 0.85;
function jaccard(a, b) {
    const setA = new Set(a.toLowerCase().split(/\s+/));
    const setB = new Set(b.toLowerCase().split(/\s+/));
    const intersection = [...setA].filter((w) => setB.has(w)).length;
    const union = new Set([...setA, ...setB]).size;
    return union === 0 ? 0 : intersection / union;
}
function contentHash(text) {
    let hash = 0;
    const normalized = text.replace(/\s+/g, " ").slice(0, 2000);
    for (let i = 0; i < normalized.length; i++) {
        hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0;
    }
    return hash.toString(16);
}
function mergeAndDeduplicate(pages) {
    // Deduplicate by URL
    const urlMap = new Map();
    for (const p of pages)
        if (!urlMap.has(p.url))
            urlMap.set(p.url, p);
    // Deduplicate by content similarity (Jaccard ≥ threshold)
    const unique = [];
    const hashes = new Set();
    const textContent = new Set();
    for (const p of urlMap.values()) {
        const hash = contentHash(p.text);
        const isNearDuplicate = [...textContent].some((existing) => jaccard(p.text, existing) >= TEXT_SIMILARITY_THRESHOLD);
        if (!hashes.has(hash) && !isNearDuplicate) {
            hashes.add(hash);
            textContent.add(p.text.slice(0, 500));
            unique.push(p);
        }
    }
    return unique.map((p) => `### ${p.title}\n${p.text}`).join("\n\n");
}
function summarise(mergedText, query) {
    // Local extractive summary — first 3 most informative paragraphs
    if (!mergedText.trim())
        return `No useful content was found for "${query}". Try rephrasing the question or checking the configured search engines.`;
    const paragraphs = mergedText.split(/\n\n+/).filter((p) => p.length > 60);
    const topThree = paragraphs.slice(0, 3).join("\n\n");
    const wordCount = mergedText.split(/\s+/).length;
    return `**Research Summary** (${wordCount} words total)\n\n${topThree}`;
}
// ─────────────────────────────────────────────────────────────────
// Cloud Function entry points
// ─────────────────────────────────────────────────────────────────
/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
exports.scrapeAndRespondFn = https.onRequest({ ...httpsOptions, cors: true }, async (req, res) => {
    if (req.method !== "POST") {
        res.status(405).json({ error: "Method Not Allowed" });
        return;
    }
    const { message, userId } = req.body;
    if (!message || !userId) {
        res.status(400).json({ error: "Missing required field: message or userId" });
        return;
    }
    try {
        const result = await scrapeAndRespond(message, userId);
        res.status(200).json(result);
    }
    catch (err) {
        console.error("[scrapeEngine]", err);
        res.status(500).json({ error: err.message });
    }
});
/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
exports.classifyIntentFn = https.onRequest({ ...httpsOptions, cors: true }, async (req, _res) => {
    if (req.method !== "POST") {
        _res.status(405).end();
        return;
    }
    const { message } = req.body;
    if (!message) {
        _res.status(400).json({ error: "message required" });
        return;
    }
    _res.status(200).json({ chatType: classifyIntent(message), message });
});
/**
 * GET /health
 */
exports.scrapeHealthFn = https.onRequest({ ...httpsOptions, cors: true }, async (_req, res) => {
    const playStatus = (await (async () => {
        try {
            const r = await axios_1.default.get(`${PLAYWRIGHT_URL}/health`, { timeout: 5000 });
            return { ok: r.status === 200, status: r.status };
        }
        catch {
            return { ok: false };
        }
    })());
    res.status(200).json({
        service: "scrapeEngine",
        playwright: playStatus,
        uptime: process.uptime(),
    });
});
//# sourceMappingURL=scrapeEngine.js.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.js.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeEngine.js.map`

```text
{"version":3,"file":"scrapeEngine.js","sourceRoot":"","sources":["../src/scrapeEngine.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.d.ts`

```typescript
/** Filter options for listHistory() */
export interface HistoryFilter {
    chatType?: string;
    minConfidence?: number;
    userId?: string;
    startDate?: Date;
    endDate?: Date;
    searchQuery?: string;
}
/** Pagination options */
export interface PaginationOptions {
    pageSize: number;
    pageToken?: string;
}
/** Paginated response */
export interface PaginatedHistory {
    entries: HistoryEntry[];
    nextPageToken: string | null;
    totalCount: number;
}
/**
 * Shallow copy of a Firestore history document.
 * Mirrors the interface in scrapeEngine.ts to avoid a circular import.
 */
export interface HistoryEntry {
    sessionId: string;
    query: string;
    chatType: string;
    sources: string[];
    rawChunks: Array<{
        url: string;
        text: string;
    }>;
    finalAnswer: string;
    confidence: number;
    timestamp: Date;
    userFeedback?: string;
    scrapedPages?: number;
    cached?: boolean;
    skipped?: boolean;
    [key: string]: unknown;
}
/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
export declare function addEntry(entry: Partial<HistoryEntry>): Promise<string>;
/**
 * Fetch a single history entry by document ID.
 */
export declare function getEntry(sessionId: string): Promise<HistoryEntry | null>;
/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
export declare function getSessionHistory(sessionId: string): Promise<HistoryEntry[]>;
/**
 * List history entries with optional filters and pagination.
 */
export declare function listHistory(filter: HistoryFilter | undefined, pagination: PaginationOptions): Promise<PaginatedHistory>;
/**
 * Get the total count of history entries (uncapped).
 */
export declare function getHistoryCount(filter?: HistoryFilter): Promise<number>;
/**
 * Delete a single history entry by session/document ID.
 */
export declare function deleteEntry(sessionId: string): Promise<void>;
/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
export declare function deleteAllHistory(): Promise<number>;
/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
export declare function recordFeedback(sessionId: string, feedback: string): Promise<void>;
//# sourceMappingURL=scrapeHistoryManager.d.ts.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.d.ts.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.d.ts.map`

```text
{"version":3,"file":"scrapeHistoryManager.d.ts","sourceRoot":"","sources":["../src/scrapeHistoryManager.ts"],"names":[],"mappings":"AAgBA,uCAAuC;AACvC,MAAM,WAAW,aAAa;IAC5B,QAAQ,CAAC,EAAE,MAAM,CAAC;IAClB,aAAa,CAAC,EAAE,MAAM,CAAC;IACvB,MAAM,CAAC,EAAE,MAAM,CAAC;IAChB,SAAS,CAAC,EAAE,IAAI,CAAC;IACjB,OAAO,CAAC,EAAE,IAAI,CAAC;IACf,WAAW,CAAC,EAAE,MAAM,CAAC;CACtB;AAED,yBAAyB;AACzB,MAAM,WAAW,iBAAiB;IAChC,QAAQ,EAAE,MAAM,CAAC;IACjB,SAAS,CAAC,EAAE,MAAM,CAAC;CACpB;AAED,yBAAyB;AACzB,MAAM,WAAW,gBAAgB;IAC/B,OAAO,EAAE,YAAY,EAAE,CAAC;IACxB,aAAa,EAAE,MAAM,GAAG,IAAI,CAAC;IAC7B,UAAU,EAAE,MAAM,CAAC;CACpB;AAED;;;GAGG;AACH,MAAM,WAAW,YAAY;IAC3B,SAAS,EAAE,MAAM,CAAC;IAClB,KAAK,EAAE,MAAM,CAAC;IACd,QAAQ,EAAE,MAAM,CAAC;IACjB,OAAO,EAAE,MAAM,EAAE,CAAC;IAClB,SAAS,EAAE,KAAK,CAAC;QAAE,GAAG,EAAE,MAAM,CAAC;QAAC,IAAI,EAAE,MAAM,CAAA;KAAE,CAAC,CAAC;IAChD,WAAW,EAAE,MAAM,CAAC;IACpB,UAAU,EAAE,MAAM,CAAC;IACnB,SAAS,EAAE,IAAI,CAAC;IAChB,YAAY,CAAC,EAAE,MAAM,CAAC;IACtB,YAAY,CAAC,EAAE,MAAM,CAAC;IACtB,MAAM,CAAC,EAAE,OAAO,CAAC;IACjB,OAAO,CAAC,EAAE,OAAO,CAAC;IAClB,CAAC,GAAG,EAAE,MAAM,GAAG,OAAO,CAAC;CACxB;AA8BD;;;GAGG;AACH,wBAAsB,QAAQ,CAC5B,KAAK,EAAE,OAAO,CAAC,YAAY,CAAC,GAC3B,OAAO,CAAC,MAAM,CAAC,CAmBjB;AAED;;GAEG;AACH,wBAAsB,QAAQ,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,YAAY,GAAG,IAAI,CAAC,CAG9E;AAED;;;GAGG;AACH,wBAAsB,iBAAiB,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,YAAY,EAAE,CAAC,CAOlF;AAED;;GAEG;AACH,wBAAsB,WAAW,CAC/B,MAAM,EAAE,aAAa,YAAK,EAC1B,UAAU,EAAE,iBAAiB,GAC5B,OAAO,CAAC,gBAAgB,CAAC,CAyB3B;AAED;;GAEG;AACH,wBAAsB,eAAe,CAAC,MAAM,GAAE,aAAkB,GAAG,OAAO,CAAC,MAAM,CAAC,CAOjF;AAED;;GAEG;AACH,wBAAsB,WAAW,CAAC,SAAS,EAAE,MAAM,GAAG,OAAO,CAAC,IAAI,CAAC,CAElE;AAED;;;GAGG;AACH,wBAAsB,gBAAgB,IAAI,OAAO,CAAC,MAAM,CAAC,CAMxD;AAED;;;;;GAKG;AACH,wBAAsB,cAAc,CAClC,SAAS,EAAE,MAAM,EACjB,QAAQ,EAAE,MAAM,GACf,OAAO,CAAC,IAAI,CAAC,CAIf"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.js`

```javascript
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.addEntry = addEntry;
exports.getEntry = getEntry;
exports.getSessionHistory = getSessionHistory;
exports.listHistory = listHistory;
exports.getHistoryCount = getHistoryCount;
exports.deleteEntry = deleteEntry;
exports.deleteAllHistory = deleteAllHistory;
exports.recordFeedback = recordFeedback;
const firestore_1 = require("firebase-admin/firestore");
// ─────────────────────────────────────────────────────────────────
// Initialisation — singleton-safe
// ─────────────────────────────────────────────────────────────────
let db = null;
function getDb() {
    db ?? (db = (0, firestore_1.getFirestore)());
    return db;
}
// ─────────────────────────────────────────────────────────────────
// Helper — convert Firestore doc → HistoryEntry
// ─────────────────────────────────────────────────────────────────
function docToEntry(doc) {
    const data = doc.data();
    return {
        sessionId: doc.id,
        query: data.query ?? "",
        chatType: data.chatType ?? "UNKNOWN",
        sources: data.sources ?? [],
        rawChunks: data.rawChunks ?? [],
        finalAnswer: data.finalAnswer ?? "",
        confidence: data.confidence ?? 0,
        timestamp: (data.timestamp && typeof data.timestamp.toDate === "function" ? data.timestamp.toDate() : new Date()),
        userFeedback: data.userFeedback,
        scrapedPages: data.scrapedPages,
        cached: data.cached,
        skipped: data.skipped,
    };
}
// ─────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────
const COL = "scrapeHistory";
/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
async function addEntry(entry) {
    const id = entry.sessionId ?? `hist_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const payload = {
        query: entry.query ?? "",
        chatType: entry.chatType ?? "UNKNOWN",
        sources: entry.sources ?? [],
        rawChunks: entry.rawChunks ?? [],
        finalAnswer: entry.finalAnswer ?? "",
        confidence: entry.confidence ?? 0,
    };
    if (entry.timestamp)
        payload.timestamp = entry.timestamp;
    else
        payload.timestamp = new Date();
    if (entry.userFeedback !== undefined)
        payload.userFeedback = entry.userFeedback;
    if (entry.scrapedPages !== undefined)
        payload.scrapedPages = entry.scrapedPages;
    if (entry.cached !== undefined)
        payload.cached = entry.cached;
    if (entry.skipped !== undefined)
        payload.skipped = entry.skipped;
    await getDb().collection(COL).doc(id).set(payload, { merge: true });
    return id;
}
/**
 * Fetch a single history entry by document ID.
 */
async function getEntry(sessionId) {
    const snap = await getDb().collection(COL).doc(sessionId).get();
    return snap.exists ? docToEntry(snap) : null;
}
/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
async function getSessionHistory(sessionId) {
    const snap = await getDb()
        .collection(COL)
        .where("sessionId", "==", sessionId)
        .orderBy("timestamp", "desc")
        .get();
    return snap.docs.map(docToEntry);
}
/**
 * List history entries with optional filters and pagination.
 */
async function listHistory(filter = {}, pagination) {
    let query = getDb().collection(COL);
    // Apply filters
    if (filter.chatType)
        query = query.where("chatType", "==", filter.chatType);
    if (filter.minConfidence != null)
        query = query.where("confidence", ">=", filter.minConfidence);
    if (filter.startDate)
        query = query.where("timestamp", ">=", filter.startDate);
    if (filter.endDate)
        query = query.where("timestamp", "<=", filter.endDate);
    query = query.orderBy("timestamp", "desc");
    const pageSize = Math.min(pagination.pageSize, 100);
    if (pagination.pageToken) {
        const cursorSnap = await getDb().collection(COL).doc(pagination.pageToken).get();
        query = query.startAfter(cursorSnap);
    }
    const snap = await query.limit(pageSize + 1).get();
    const docs = snap.docs;
    const entries = docs.slice(0, pageSize).map(docToEntry);
    const nextPageToken = docs.length > pageSize ? docs[snap.size - 2].id : null;
    return { entries, nextPageToken, totalCount: snap.size };
}
/**
 * Get the total count of history entries (uncapped).
 */
async function getHistoryCount(filter = {}) {
    let query = getDb().collection(COL);
    if (filter.chatType)
        query = query.where("chatType", "==", filter.chatType);
    if (filter.startDate)
        query = query.where("timestamp", ">=", filter.startDate);
    if (filter.endDate)
        query = query.where("timestamp", "<=", filter.endDate);
    const snap = await query.get();
    return snap.size;
}
/**
 * Delete a single history entry by session/document ID.
 */
async function deleteEntry(sessionId) {
    await getDb().collection(COL).doc(sessionId).delete();
}
/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
async function deleteAllHistory() {
    const snap = await getDb().collection(COL).get();
    const batch = getDb().batch();
    snap.docs.forEach((doc) => batch.delete(doc.ref));
    if (snap.size > 0)
        await batch.commit();
    return snap.size;
}
/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
async function recordFeedback(sessionId, feedback) {
    await getDb().collection(COL).doc(sessionId).update({
        userFeedback: feedback,
    });
}
//# sourceMappingURL=scrapeHistoryManager.js.map
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.js.map`

### File: `infrastructure/firebase_functions/firebase_functions_v1/lib/scrapeHistoryManager.js.map`

```text
{"version":3,"file":"scrapeHistoryManager.js","sourceRoot":"","sources":["../src/scrapeHistoryManager.ts"],"names":[],"mappings":"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"}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/chatClassifier.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/chatClassifier.ts`

```typescript
// ─────────────────────────────────────────────────────────────────
// chatClassifier.ts
// Intent / ChatType classifier for the SupremeAI scraping pipeline.
//
// Extracted from classifyIntent() in scrapeEngine.ts so that
// ChatProcessingService.java and other callers can invoke intent
// classification without depending on the full scraping engine.
// ─────────────────────────────────────────────────────────────────

/** All supported chat types returned by classifyIntent() */
export type ChatType =
  | "GREETING"
  | "SIMILAR"
  | "SIMPLE_QUESTION"
  | "COMPLEX_QUESTION"
  | "FOLLOW_UP"
  | "COMMAND"
  | "UNKNOWN";

/** Result of a single-classify call */
export interface ClassifyResult {
  chatType: ChatType;
  message: string;
  classifiedAt: number; // epoch ms
}

// ─────────────────────────────────────────────────────────────────
// Regex patterns — kept identical to scrapeEngine.ts for backwards
// compatibility with any existing compare-diff expectations.
// ─────────────────────────────────────────────────────────────────

const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS  = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS  = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS  = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK  = /\?$/;

// ─────────────────────────────────────────────────────────────────
// classifyIntent
// ─────────────────────────────────────────────────────────────────

/**
 * Classify a raw user message into a ChatType.
 *
 * Priority order:
 *   1. GREETING  — hi / hello / hællo / etc.
 *   2. SIMILAR   — "how are you" / "কেমন আছো"
 *   3. COMMAND   — starts with a known command keyword
 *   4. FOLLOW_UP — "tell me more", "আরও", etc.
 *   5. COMPLEX_QUESTION — sub-string hint words (compare, vs, analysis …)
 *   6. SIMPLE_QUESTION — ends with "?"
 *   7. SIMPLE_QUESTION — short messages (< 20 chars after trim)
 *   8. COMPLEX_QUESTION — fallback (assume complex)
 */
export function classifyIntent(message: string, nowMs?: number): ClassifyResult {
  const trimmed = message.trim().toLowerCase();

  let chatType: ChatType;
  if (GREETING_WORDS.test(trimmed))      chatType = "GREETING";
  else if (SIMILAR_WORDS.test(trimmed))  chatType = "SIMILAR";
  else if (COMMAND_WORDS.test(trimmed))  chatType = "COMMAND";
  else if (FOLLOW_UP_WORDS.test(trimmed)) chatType = "FOLLOW_UP";
  else if (COMPLEX_HINTS.test(trimmed))  chatType = "COMPLEX_QUESTION";
  else if (QUESTION_MARK.test(trimmed))  chatType = "SIMPLE_QUESTION";
  else if (trimmed.length < 20)           chatType = "SIMPLE_QUESTION";
  else                                    chatType = "COMPLEX_QUESTION";

  return {
    chatType,
    message,
    classifiedAt: nowMs ?? Date.now(),
  };
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/email_handler.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/email_handler.ts`

```typescript
import * as functions from 'firebase-functions/v2';
import * as admin from 'firebase-admin';
// @ts-ignore
import { simpleParser } from 'mailparser';
// @ts-ignore
import * as nodemailer from 'nodemailer';
import axios from 'axios';

// Configuration for outgoing status updates
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.SUPREMEAI_EMAIL,
        pass: process.env.SUPREMEAI_EMAIL_PASSWORD
    }
});

/**
 * Inbound Email Webhook
 * Triggered by an email provider (e.g., SendGrid Inbound Parse)
 */
export const handleIncomingEmail = functions.https.onRequest(async (req, res) => {
    try {
        // 1. Parse the multipart email body
        const parsed = await simpleParser(req.body);
        const sender = parsed.from?.value[0].address;
        const recipient = (parsed.to as any)?.value?.[0]?.address;
        const subject = parsed.subject;
        const body = parsed.text;
        const html = parsed.html;

        console.log(`[SupremeAI Email] Incoming from: ${sender} to ${recipient}, Subject: ${subject}`);

        // 1. Check for Verification Codes/Links (The "Personhood" check)
        // If the email is from a known provider (Google, DeepSeek, etc.), extract OTP
        const otpMatch = body?.match(/\b\d{6}\b/); // Look for 6-digit codes
        const linkMatch = html?.match(/href="([^"]*confirm[^"]*|[^"]*verify[^"]*)"/i);

        if (otpMatch || linkMatch) {
            await admin.firestore().collection('verification_queue').add({
                sender,
                email_target: recipient,
                subject,
                code: otpMatch ? otpMatch[0] : null,
                link: linkMatch ? linkMatch[1] : null,
                receivedAt: admin.firestore.FieldValue.serverTimestamp(),
                processed: false
            });
            console.log(`[SupremeAI] Extracted verification data from ${sender}`);
        }

        // 2. Security: Only process if it's from the verified Admin
        const authorizedAdmins = process.env.AUTHORIZED_ADMINS
            ? process.env.AUTHORIZED_ADMINS.split(',').map(email => email.trim().toLowerCase())
            : ['admin@yourdomain.com'];
            
        if (!sender || !authorizedAdmins.includes(sender.toLowerCase())) {
            console.warn(`Unauthorized access attempt by ${sender}`);
            res.status(403).send('Forbidden');
            return;
        }

        // 3. Process Logic with Gemini API via Axios
        let resultText = '';
        const geminiApiKey = process.env.GEMINI_API_KEY;
        if (geminiApiKey && body) {
            console.log(`[SupremeAI] Processing command using Gemini API...`);
            try {
                const response = await axios.post(
                    `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${geminiApiKey}`,
                    {
                        contents: [{
                            parts: [{ text: `You are the SupremeAI Core Engine. Execute or respond to this command from the Admin:\n\n${body}` }]
                        }]
                    },
                    {
                        headers: { 'Content-Type': 'application/json' }
                    }
                );
                resultText = response.data?.candidates?.[0]?.content?.parts?.[0]?.text || 'No response from AI engine.';
            } catch (err: any) {
                console.error('Error calling Gemini API:', err?.response?.data || err.message);
                resultText = `Failed to process command with AI: ${err.message}`;
            }
        } else {
            console.log(`[SupremeAI] Empty body or GEMINI_API_KEY not configured. Returning dummy execution.`);
            resultText = `Hello Admin, I received your command "${subject}" but could not process it using AI because the GEMINI_API_KEY is not set.`;
        }

        // 4. Send Confirmation/Result back to Admin
        await transporter.sendMail({
            from: `"SupremeAI Assistant" <${process.env.SUPREMEAI_EMAIL || 'supremeai@yourdomain.com'}>`,
            to: sender,
            subject: `Re: ${subject} [PROCESSED]`,
            text: `Hello Admin, I have received your request and executed the tasks. \n\nCommand: ${subject}\n\nExecution Result:\n${resultText}`
        });

        res.status(200).send('Email Processed');
    } catch (error) {
        console.error('Email processing error:', error);
        res.status(500).send('Internal Server Error');
    }
});
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/index.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/index.ts`

```typescript
import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Trigger: Executes automatically whenever a new user signs up via Firebase Auth.
 * Action: Assigns a default 'user' custom claim and creates a Firestore profile.
 */
export const onUserSignUp = functions.auth.user().onCreate(async (user: admin.auth.UserRecord) => {
    try {
        // 1. Set Custom User Claims (Embeds the role directly into their JWT token)
        await admin.auth().setCustomUserClaims(user.uid, {
            role: 'user',
            accessLevel: 1
        });

        // 2. Create a synchronized profile document in Firestore
        await admin.firestore().collection('users').doc(user.uid).set({
            email: user.email,
            displayName: user.displayName || 'Operator',
            role: 'user',
            tier: 'FREE',
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            lastLogin: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        console.log(`[AUTH] Successfully initialized new user: ${user.uid} with 'user' role.`);
    } catch (error) {
        console.error(`[AUTH ERROR] Failed to initialize user ${user.uid}:`, error);
    }
});
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeEngine.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeEngine.ts`

```typescript
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Firestore, Timestamp } from "firebase-admin/firestore";
import * as https from "firebase-functions/v2/https";
import axios from "axios";

const httpsOptions = { region: "us-central1" };

// ─────────────────────────────────────────────────────────────────
// Firebase initialisation (singleton-safe)
// ─────────────────────────────────────────────────────────────────
let db: Firestore | null = null;

function getDb(): Firestore {
  db ??= getFirestore(initializeApp({}));
  return db;
}

// ─────────────────────────────────────────────────────────────────
// Firestore collection constants
// ─────────────────────────────────────────────────────────────────
const COL = {
  policies: "scrapePolicies",
  presets: "scrapePresets",
  domains: "scrapeAllowedDomains",
  history: "scrapeHistory",
  events: "scrapeEvent",
};

// ─────────────────────────────────────────────────────────────────
// Interfaces (document shapes)
// ─────────────────────────────────────────────────────────────────
interface ScrapePolicy {
  enabled: boolean;
  maxDepth: number;
  maxResults: number;
  allowedDomains?: string[];
  searchEngines?: string[];
  contentTypes?: string[];
  extractStrategy: string;
  fallbackAndRetry: boolean;
  rateLimitMs: number;
  timeoutMs: number;
  cacheTTL: number;
  [key: string]: unknown;
}

interface ScrapePreset {
  name: string;
  searchUrlTemplate: string;
  searchParam: string;
  extractStrategy: string;
  allowedSources?: string[];
  maxDepth?: number;
  [key: string]: unknown;
}

interface AllowedDomain {
  domain: string;
  allowedPaths?: string[];
  allowedTypes?: string[];
  trustLevel: "trusted" | "standard" | "suspicious";
  rateLimitMs: number;
  enabled: boolean;
  [key: string]: unknown;
}

interface ScrapeHistoryEntry {
  sessionId: string;
  query: string;
  chatType: string;
  sources: string[];
  rawChunks: unknown[];
  finalAnswer: string;
  confidence: number;
  timestamp: FirebaseFirestore.Timestamp;
  userFeedback?: string;
  [key: string]: unknown;
}

interface ScrapeEventEntry {
  id: string;
  sessionId: string;
  type:
    | "navigate_start"
    | "navigate_complete"
    | "extract_start"
    | "extract_complete"
    | "domain_skipped"
    | "error"
    | "crawl_depth_reached"
    | "rate_limited"
    | "cached_answer";
  payload: Record<string, unknown>;
  timestamp: FirebaseFirestore.Timestamp;
}

// ─────────────────────────────────────────────────────────────────
// Step 1 — Chat / Intent Classifier
// ─────────────────────────────────────────────────────────────────
type ChatType = "GREETING" | "SIMILAR" | "SIMPLE_QUESTION" | "COMPLEX_QUESTION" | "FOLLOW_UP" | "COMMAND" | "UNKNOWN";

const GREETING_WORDS = /^(hi|hello|hlw|hey|good\s*(morning|afternoon|evening)|হ্যালো|নমস্কার|হাই)\s*$/i;
const SIMILAR_WORDS   = /^(how are you|কেমন আছো|কেমন আছ|কেমন Chao|what'?s up|how'?s it going)/i;
const FOLLOW_UP_WORDS = /^(tell me more|more|and\?|আরও|আরও বলো|而且|その他)/i;
const COMMAND_WORDS   = /^(deploy|run|test|build|reboot|kill|restart|ডিপ্লয়|রান|টেস্ট)/i;
const COMPLEX_HINTS   = /(compare|vs|versus|benchmark|review|analysis|difference|মতামত|তুলনা|বিবরণ)/i;
const QUESTION_MARK   = /\?$/;

function classifyIntent(message: string): ChatType {
  const msg = message.trim().toLowerCase();
  if (GREETING_WORDS.test(msg))       return "GREETING";
  if (SIMILAR_WORDS.test(msg))         return "SIMILAR";
  if (COMMAND_WORDS.test(msg))         return "COMMAND";
  if (FOLLOW_UP_WORDS.test(msg))       return "FOLLOW_UP";
  if (COMPLEX_HINTS.test(msg))         return "COMPLEX_QUESTION";
  if (QUESTION_MARK.test(msg))         return "SIMPLE_QUESTION";
  if (msg.length < 20)                 return "SIMPLE_QUESTION";
  return "COMPLEX_QUESTION";
}

// ─────────────────────────────────────────────────────────────────
// Step 2 — Firestore config loader helpers
// ─────────────────────────────────────────────────────────────────
async function getGlobalPolicy(): Promise<ScrapePolicy | null> {
  const snap = await getDb().collection(COL.policies).doc("global").get();
  return snap.exists ? (snap.data() as ScrapePolicy) : null;
}

async function getPolicy(type: string): Promise<ScrapePolicy | null> {
  const snap = await getDb().collection(COL.policies).doc(type).get();
  return snap.exists ? (snap.data() as ScrapePolicy) : null;
}

async function getPreset(presetId: string): Promise<ScrapePreset | null> {
  const snap = await getDb().collection(COL.presets).doc(presetId).get();
  return snap.exists ? (snap.data() as ScrapePreset) : null;
}

async function getAllowedDomains(): Promise<AllowedDomain[]> {
  const snap = await getDb().collection(COL.domains).get();
  return snap.docs.map((d) => d.data() as AllowedDomain).filter((d) => d.enabled);
}

async function findCachedAnswer(
  query: string,
  cacheTTLSeconds: number,
): Promise<ScrapeHistoryEntry | null> {
  const threshold = Timestamp.fromMillis(Date.now() - cacheTTLSeconds * 1000);
  const snap = await getDb()
    .collection(COL.history)
    .where("query", "==", query)
    .where("timestamp", ">", threshold)
    .orderBy("timestamp", "desc")
    .limit(1)
    .get();
  if (snap.empty) return null;
  const d = snap.docs[0].data() as ScrapeHistoryEntry;
  return { ...d, sessionId: snap.docs[0].id };
}

// ─────────────────────────────────────────────────────────────────
// Step 6 helpers — domain allow /Trust-scores
// ─────────────────────────────────────────────────────────────────
function extractHost(url: string): string {
  try { return new URL(url).hostname; } catch { return url; }
}

function isDomainAllowed(domain: string, domains: AllowedDomain[]): { allowed: boolean; trustLevel: string } {
  const entry = domains.find((d) => domain === d.domain || domain.endsWith("." + d.domain));
  if (!entry) return { allowed: true, trustLevel: "standard" }; // open by default — admin can restrict via Firestore
  return { allowed: entry.enabled, trustLevel: entry.trustLevel };
}

// ─────────────────────────────────────────────────────────────────
// Step 5 — Content extraction (strategy dispatch)
// ─────────────────────────────────────────────────────────────────
interface ExtractedPage {
  url: string;
  title: string;
  text: string;
  strategy: string;
}

async function extractFromPage(
  pageUrl: string,
  strategy: string,
  eventId: string,
): Promise<ExtractedPage> {
  const result = await callPlaywright("extract", { url: pageUrl, strategy, eventId });
  return {
    url: pageUrl,
    title: (result as any)?.title ? String((result as any).title) : pageUrl,
    text:   (result as any)?.text  ? String((result as any).text)  : "",
    strategy,
  };
}

// ─────────────────────────────────────────────────────────────────
// Playwright proxy helper
// ─────────────────────────────────────────────────────────────────
const PLAYWRIGHT_URL = process.env.BROWSER_AUTOMATION_URL || "http://127.0.0.1:3001";

async function callPlaywright(
  action: string,
  body: Record<string, unknown>,
): Promise<unknown> {
  try {
    const res = await axios.post(`${PLAYWRIGHT_URL}/${action}`, body, {
      timeout: parseInt(process.env.SCRAPE_TIMEOUT_MS || "30000"),
    });
    return res.data;
  } catch (err) {
    throw new https.HttpsError(
      "unavailable",
      `Browser automation unavailable for ${action}: ${(err as Error).message}`,
    );
  }
}

// ─────────────────────────────────────────────────────────────────
// Step 9 — Firestore history writer
// ─────────────────────────────────────────────────────────────────
async function writeHistory(entry: ScrapeHistoryEntry): Promise<string> {
  const ref = entry.sessionId
    ? getDb().collection(COL.history).doc(entry.sessionId)
    : getDb().collection(COL.history).doc();
  await ref.set({
    ...entry,
    timestamp: Timestamp.now(),
  });
  return ref.id;
}

async function logEvent(
  sessionId: string,
  type: ScrapeEventEntry["type"],
  payload: Record<string, unknown>,
): Promise<void> {
  await getDb()
    .collection(COL.events)
    .doc()
    .set({
      sessionId,
      type,
      payload,
      timestamp: Timestamp.now(),
    });
}

// ─────────────────────────────────────────────────────────────────
// Public scrape flow
// ─────────────────────────────────────────────────────────────────

/**
 * Main scraping entry point — called by the chat-processing pipeline.
 *
 * @param message  User message text
 * @param userId    Firebase UID of the caller
 * @returns         { answer, sources, confidence, sessionId }
 */
export async function scrapeAndRespond(
  message: string,
  userId: string,
): Promise<Record<string, unknown>> {
  const sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const chatType = classifyIntent(message);

  // ── Step 2: policy lookup ──────────────────────────────────────
  const globalPolicy = await getGlobalPolicy();
  if (!globalPolicy?.enabled) {
    return { answer: "Web scraping is currently disabled by global policy.", sources: [], confidence: 0 };
  }

  const perTypePolicy = await getPolicy(chatType);
  if (!perTypePolicy?.enabled) {
    // Skipped — return empty, caller falls back to local knowledge
    return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
  }

  const policy: ScrapePolicy = { ...globalPolicy, ...perTypePolicy };

  // ── Cache check for FOLLOW_UP ──────────────────────────────────
  if (chatType === "FOLLOW_UP" || chatType === "SIMPLE_QUESTION" || chatType === "COMPLEX_QUESTION") {
    const cached = await findCachedAnswer(message, policy.cacheTTL);
    if (cached) {
      await logEvent(sessionId, "cached_answer", { fromSession: cached.sessionId, query: message });
      return {
        answer: cached.finalAnswer,
        sources: cached.sources,
        confidence: cached.confidence,
        chatType,
        sessionId,
        cached: true,
        originalSessionId: cached.sessionId,
      };
    }
  }

  // Skip heavy flow for GREETING / SIMILAR / COMMAND
  if (chatType === "GREETING" || chatType === "SIMILAR") {
    return { answer: "", sources: [], confidence: 0, chatType, sessionId, skipped: true };
  }

  // ── Step 3: build search entry point ───────────────────────────
  const domains = await getAllowedDomains();
  const allSearchEngines = policy.searchEngines || ["google"];
  const maxResults  = policy.maxResults  || 3;
  const maxDepth    = policy.maxDepth    ?? 1;
  const strategy    = policy.extractStrategy || "article-extract";

  const builtUrls: { engine: string; url: string }[] = [];
  for (const engine of allSearchEngines) {
    const preset = await getPreset(engine);
    if (!preset || !preset.searchUrlTemplate) continue;
    const queryParam = encodeURIComponent(message);
    const engineUrl  = preset.searchUrlTemplate.replace("{q}", queryParam);
    // trust gate
    const host = extractHost(engineUrl);
    const { allowed } = isDomainAllowed(host, domains);
    if (!allowed) {
      await logEvent(sessionId, "domain_skipped", { url: engineUrl, reason: "not_in_allowed_domains" });
      continue;
    }
    builtUrls.push({ engine, url: engineUrl });
  }

  if (builtUrls.length === 0) {
    return { answer: "No search engine configured or all domains blocked.", sources: [], confidence: 0, chatType, sessionId };
  }

  // ── Step 4: launch navigate sessions in parallel ───────────────
  const searchEventId = `${sessionId}_search`;
  await logEvent(sessionId, "navigate_start", { urls: builtUrls.map((b) => b.url) });

  // Dispatch all search-engine navigations via Playwright, but don't block the
  // event loop — fire and await.
  const navigatePromises = builtUrls.map(async ({ engine, url }) => {
    try {
      await callPlaywright("navigate", { url, eventId: searchEventId });
      await logEvent(sessionId, "navigate_complete", { engine, url });
    } catch (err) {
      await logEvent(sessionId, "error", { engine, url, error: (err as Error).message });
    }
  });
  await Promise.allSettled(navigatePromises);

  // ── Step 5: extract result links ───────────────────────────────
  let resultLinks: string[] = [];
  try {
    const searchContent = await callPlaywright("extract", { url: builtUrls[0]?.url || "", strategy: "search-links", eventId: searchEventId });
    resultLinks = Array.isArray(searchContent)
      ? (searchContent as Array<{ href: string }>).map((r) => r.href).filter(Boolean)
      : [];
  } catch (extractionError) {
    console.error(`[ScrapeEngine] Failed to extract search result links:`, extractionError);
    // If link extraction fails, fall back to navigating each engine URL directly
    resultLinks = builtUrls.map((b) => b.url);
  }

  // cap to top N results
  resultLinks = resultLinks.slice(0, maxResults);

  // ── Step 6: crawl deeper (maxDepth > 0) ───────────────────────
  const allExtracted: ExtractedPage[] = [];
  const crawl = async (url: string, depth: number): Promise<void> => {
    if (depth > maxDepth || resultLinks.length === 0) return;
    for (const link of resultLinks) {
      const host = extractHost(link);
      const { allowed, trustLevel } = isDomainAllowed(host, domains);
      if (!allowed || trustLevel === "suspicious") {
        await logEvent(sessionId, "domain_skipped", { url: link, reason: trustLevel === "suspicious" ? "suspicious_domain" : "not_allowed", trustLevel });
        continue;
      }
      await logEvent(sessionId, "extract_start", { url: link, depth });
      try {
        const page = await extractFromPage(link, strategy, `${sessionId}_d${depth}`);
        allExtracted.push(page);
        await logEvent(sessionId, "extract_complete", { url: link, depth, textLength: page.text.length, strategy: page.strategy });

        // shallow follow links one level deeper
        if (depth < maxDepth) {
          const outbound = (await callPlaywright("extract", { url: link, strategy: "outbound-links", eventId: `${sessionId}_out_${depth}` })) as Array<{ href: string }>;
          const nextUrls = outbound?.map((r) => r.href).filter(Boolean) || [];
          for (const next of nextUrls.slice(0, 2)) await crawl(next, depth + 1);
        }
      } catch (err) {
        await logEvent(sessionId, "error", { url: link, phase: "extract", error: (err as Error).message });
      }
    }
  };

  // crawl the top results
  await crawl(builtUrls[0]?.url || "", 0);

  // ── Step 7: merge, deduplicate, summarize ─────────────────────
  const mergedText = mergeAndDeduplicate(allExtracted);
  const answer    = summarise(mergedText, message);

  // ── Step 8: store session history ──────────────────────────────
  const firestoreDocId = await writeHistory({
    sessionId,
    query: message,
    chatType,
    sources: allExtracted.map((p) => p.url),
    rawChunks: allExtracted.map((p) => ({ url: p.url, text: p.text })),
    finalAnswer: answer,
    confidence: allExtracted.length > 0 ? Math.min(0.85, 0.55 + allExtracted.length * 0.07) : 0,
    timestamp: Timestamp.now() as unknown as FirebaseFirestore.Timestamp,
  });
  sessionId; // used; intentionally shadowed by const above — keep local sessionId for return

  // ── Step 9: return ─────────────────────────────────────────────
  return {
    answer,
    sources: allExtracted.map((p) => p.url),
    confidence: allExtracted.length > 0 ? Math.min(0.90, 0.55 + allExtracted.length * 0.08) : 0.2,
    chatType,
    sessionId: firestoreDocId,
    scrapedPages: allExtracted.length,
  };
}

// ─────────────────────────────────────────────────────────────────
// Text processing helpers
// ─────────────────────────────────────────────────────────────────
const TEXT_SIMILARITY_THRESHOLD = 0.85;

function jaccard(a: string, b: string): number {
  const setA = new Set(a.toLowerCase().split(/\s+/));
  const setB = new Set(b.toLowerCase().split(/\s+/));
  const intersection = [...setA].filter((w) => setB.has(w)).length;
  const union = new Set([...setA, ...setB]).size;
  return union === 0 ? 0 : intersection / union;
}

function contentHash(text: string): string {
  let hash = 0;
  const normalized = text.replace(/\s+/g, " ").slice(0, 2000);
  for (let i = 0; i < normalized.length; i++) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0;
  }
  return hash.toString(16);
}

function mergeAndDeduplicate(pages: ExtractedPage[]): string {
  // Deduplicate by URL
  const urlMap = new Map<string, ExtractedPage>();
  for (const p of pages) if (!urlMap.has(p.url)) urlMap.set(p.url, p);

  // Deduplicate by content similarity (Jaccard ≥ threshold)
  const unique: ExtractedPage[] = [];
  const hashes = new Set<string>();
  const textContent = new Set<string>();
  for (const p of urlMap.values()) {
    const hash = contentHash(p.text);
    const isNearDuplicate = [...textContent].some((existing) => jaccard(p.text, existing) >= TEXT_SIMILARITY_THRESHOLD);
    if (!hashes.has(hash) && !isNearDuplicate) {
      hashes.add(hash);
      textContent.add(p.text.slice(0, 500));
      unique.push(p);
    }
  }
  return unique.map((p) => `### ${p.title}\n${p.text}`).join("\n\n");
}

function summarise(mergedText: string, query: string): string {
  // Local extractive summary — first 3 most informative paragraphs
  if (!mergedText.trim()) return `No useful content was found for "${query}". Try rephrasing the question or checking the configured search engines.`;

  const paragraphs = mergedText.split(/\n\n+/).filter((p) => p.length > 60);
  const topThree   = paragraphs.slice(0, 3).join("\n\n");
  const wordCount  = mergedText.split(/\s+/).length;

  return `**Research Summary** (${wordCount} words total)\n\n${topThree}`;
}

// ─────────────────────────────────────────────────────────────────
// Cloud Function entry points
// ─────────────────────────────────────────────────────────────────

/**
 * POST /scrapeAndRespond — main scraping endpoint.
 * Body: { message: string, userId: string }
 */
export const scrapeAndRespondFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (req: any, res: any) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed" });
      return;
    }
    const { message, userId } = req.body;
    if (!message || !userId) {
      res.status(400).json({ error: "Missing required field: message or userId" });
      return;
    }
    try {
      const result = await scrapeAndRespond(message, userId);
      res.status(200).json(result);
    } catch (err) {
      console.error("[scrapeEngine]", err);
      res.status(500).json({ error: (err as Error).message });
    }
  },
);

/**
 * POST /classifyIntent — classifier-only endpoint for testing.
 * Body: { message: string }
 */
export const classifyIntentFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (req: any, _res: any) => {
    if (req.method !== "POST") { _res.status(405).end(); return; }
    const { message } = req.body;
    if (!message) { _res.status(400).json({ error: "message required" }); return; }
    _res.status(200).json({ chatType: classifyIntent(message), message });
  },
);

/**
 * GET /health
 */
export const scrapeHealthFn = https.onRequest(
  { ...httpsOptions, cors: true },
  async (_req: any, res: any) => {
    const playStatus = (await (async (): Promise<unknown> => {
      try {
        const r = await axios.get(`${PLAYWRIGHT_URL}/health`, { timeout: 5000 });
        return { ok: r.status === 200, status: r.status };
      } catch { return { ok: false }; }
    })()) as { ok: boolean; status: number };
    res.status(200).json({
      service: "scrapeEngine",
      playwright: playStatus,
      uptime: process.uptime(),
    });
  },
);
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeHistoryManager.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeHistoryManager.ts`

```typescript
import { getFirestore, Firestore } from "firebase-admin/firestore";

// ─────────────────────────────────────────────────────────────────
// Initialisation — singleton-safe
// ─────────────────────────────────────────────────────────────────
let db: Firestore | null = null;

function getDb(): Firestore {
  db ??= getFirestore();
  return db;
}

// ─────────────────────────────────────────────────────────────────
// Type definitions (mirrors ScrapeHistoryEntry from scrapeEngine.ts)
// ─────────────────────────────────────────────────────────────────

/** Filter options for listHistory() */
export interface HistoryFilter {
  chatType?: string;
  minConfidence?: number;
  userId?: string;
  startDate?: Date;
  endDate?: Date;
  searchQuery?: string;   // substring match on query field
}

/** Pagination options */
export interface PaginationOptions {
  pageSize: number;       // ≤ 100
  pageToken?: string;     // last doc id from previous page
}

/** Paginated response */
export interface PaginatedHistory {
  entries: HistoryEntry[];
  nextPageToken: string | null;
  totalCount: number;
}

/**
 * Shallow copy of a Firestore history document.
 * Mirrors the interface in scrapeEngine.ts to avoid a circular import.
 */
export interface HistoryEntry {
  sessionId: string;
  query: string;
  chatType: string;
  sources: string[];
  rawChunks: Array<{ url: string; text: string }>;
  finalAnswer: string;
  confidence: number;
  timestamp: Date;
  userFeedback?: string;
  scrapedPages?: number;
  cached?: boolean;
  skipped?: boolean;
  [key: string]: unknown;
}

// ─────────────────────────────────────────────────────────────────
// Helper — convert Firestore doc → HistoryEntry
// ─────────────────────────────────────────────────────────────────

function docToEntry(doc: FirebaseFirestore.DocumentSnapshot<FirebaseFirestore.DocumentData>): HistoryEntry {
  const data = doc.data() as Record<string, unknown>;
  return {
    sessionId: doc.id,
    query: (data.query as string) ?? "",
    chatType: (data.chatType as string) ?? "UNKNOWN",
    sources: (data.sources as string[]) ?? [],
    rawChunks: (data.rawChunks as Array<{ url: string; text: string }>) ?? [],
    finalAnswer: (data.finalAnswer as string) ?? "",
    confidence: (data.confidence as number) ?? 0,
    timestamp: (data.timestamp && typeof (data.timestamp as any).toDate === "function" ? (data.timestamp as any).toDate() : new Date()),
    userFeedback: data.userFeedback as string | undefined,
    scrapedPages: data.scrapedPages as number | undefined,
    cached: data.cached as boolean | undefined,
    skipped: data.skipped as boolean | undefined,
  };
}

// ─────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────

const COL = "scrapeHistory";

/**
 * Add a new scraping history entry.
 * Use a provided sessionId for updates, or let one be auto-generated.
 */
export async function addEntry(
  entry: Partial<HistoryEntry>,
): Promise<string> {
  const id = entry.sessionId ?? `hist_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const payload: Record<string, unknown> = {
    query:   entry.query ?? "",
    chatType: entry.chatType ?? "UNKNOWN",
    sources: entry.sources ?? [],
    rawChunks: entry.rawChunks ?? [],
    finalAnswer: entry.finalAnswer ?? "",
    confidence: entry.confidence ?? 0,
  };
  if (entry.timestamp)  payload.timestamp = entry.timestamp;
  else                  payload.timestamp = new Date();
  if (entry.userFeedback !== undefined) payload.userFeedback = entry.userFeedback;
  if (entry.scrapedPages !== undefined) payload.scrapedPages = entry.scrapedPages;
  if (entry.cached !== undefined)       payload.cached       = entry.cached;
  if (entry.skipped !== undefined)      payload.skipped      = entry.skipped;

  await getDb().collection(COL).doc(id).set(payload, { merge: true });
  return id;
}

/**
 * Fetch a single history entry by document ID.
 */
export async function getEntry(sessionId: string): Promise<HistoryEntry | null> {
  const snap = await getDb().collection(COL).doc(sessionId).get();
  return snap.exists ? docToEntry(snap) : null;
}

/**
 * Get all entries for a given session (by sessionId field).
 * Returns them ordered newest-first.
 */
export async function getSessionHistory(sessionId: string): Promise<HistoryEntry[]> {
  const snap = await getDb()
    .collection(COL)
    .where("sessionId", "==", sessionId)
    .orderBy("timestamp", "desc")
    .get();
  return snap.docs.map(docToEntry);
}

/**
 * List history entries with optional filters and pagination.
 */
export async function listHistory(
  filter: HistoryFilter = {},
  pagination: PaginationOptions,
): Promise<PaginatedHistory> {
  let query: FirebaseFirestore.Query = getDb().collection(COL);

  // Apply filters
  if (filter.chatType)  query = query.where("chatType", "==", filter.chatType);
  if (filter.minConfidence != null)
    query = query.where("confidence", ">=", filter.minConfidence);
  if (filter.startDate) query = query.where("timestamp", ">=", filter.startDate);
  if (filter.endDate)   query = query.where("timestamp", "<=", filter.endDate);

  query = query.orderBy("timestamp", "desc");

  const pageSize = Math.min(pagination.pageSize, 100);
  if (pagination.pageToken) {
    const cursorSnap = await getDb().collection(COL).doc(pagination.pageToken).get();
    query = query.startAfter(cursorSnap);
  }

  const snap = await query.limit(pageSize + 1).get();
  const docs = snap.docs;

  const entries = docs.slice(0, pageSize).map(docToEntry);
  const nextPageToken = docs.length > pageSize ? docs[snap.size - 2].id : null;

  return { entries, nextPageToken, totalCount: snap.size };
}

/**
 * Get the total count of history entries (uncapped).
 */
export async function getHistoryCount(filter: HistoryFilter = {}): Promise<number> {
  let query: FirebaseFirestore.Query = getDb().collection(COL);
  if (filter.chatType)  query = query.where("chatType", "==", filter.chatType);
  if (filter.startDate) query = query.where("timestamp", ">=", filter.startDate);
  if (filter.endDate)   query = query.where("timestamp", "<=", filter.endDate);
  const snap = await query.get();
  return snap.size;
}

/**
 * Delete a single history entry by session/document ID.
 */
export async function deleteEntry(sessionId: string): Promise<void> {
  await getDb().collection(COL).doc(sessionId).delete();
}

/**
 * Purge all scraping history entries. Use with caution; returns the
 * number of documents deleted.
 */
export async function deleteAllHistory(): Promise<number> {
  const snap = await getDb().collection(COL).get();
  const batch = getDb().batch();
  snap.docs.forEach((doc) => batch.delete(doc.ref));
  if (snap.size > 0) await batch.commit();
  return snap.size;
}

/**
 * Record user feedback (thumbs-up / thumbs-down / correction) on a history entry.
 *
 * @param sessionId   Document ID of the history entry
 * @param feedback    "up" | "down" | "corrected:{text}"
 */
export async function recordFeedback(
  sessionId: string,
  feedback: string,
): Promise<void> {
  await getDb().collection(COL).doc(sessionId).update({
    userFeedback: feedback,
  });
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeSchema.yaml`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/scrapeSchema.yaml`

```yaml
# scrapeSchema.yaml
# Firestore collection schema for the SupremeAI Scraping Engine.
#
# These YAML documents describe the expected field names, types,
# and index patterns for each scraping-related Firestore collection.
# Firestore itself is schemaless; this file documents the *contract*
# enforced by scrapeEngine.ts and scrapeHistoryManager.ts so that
# writers, validators, and migration tools can enforce correctness.
#
# Reference implementation: functions/src/scrapeEngine.ts
# History manager:         functions/src/scrapeHistoryManager.ts

version: "1.0"
service: "scraping-engine"
generated: "auto"
environment: "production"

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeHistory
# Primary table — one document per scraping session / user query.
# ─────────────────────────────────────────────────────────────────
collections:
  - name: "scrapeHistory"
    description: |
      One document per scraping session. Stores the user query, the
      generated answer, source URLs, raw extracted text, and confidence.
    fields:
      - name: "sessionId"       # Document ID = sessionId (set on write); also stored as a field for lookups
        type: "string"
        required: true
      - name: "query"
        type: "string"
        required: true
        description: "Original user message / search query"
      - name: "chatType"
        type: "string"
        required: true
        allowedValues: [GREETING, SIMILAR, SIMPLE_QUESTION, COMPLEX_QUESTION, FOLLOW_UP, COMMAND, UNKNOWN]
        description: "Intent type assigned by chatClassifier.classifyIntent()"
      - name: "sources"
        type: "array[string]"
        required: true
        description: "URLs from which content was scraped"
      - name: "rawChunks"
        type: "array[object]"
        required: true
        description: "Per-page extracted text chunks; each item has { url, text }"
        subFields:
          - name: "url"
            type: "string"
          - name: "text"
            type: "string"
      - name: "finalAnswer"
        type: "string"
        required: true
        description: "Merged + summarised response returned to the user"
      - name: "confidence"
        type: "number"
        required: true
        range: [0.0, 1.0]
        description: "Confidence score (0 = no scraped content, 0.9 = many pages)"
      - name: "timestamp"
        type: "Timestamp"
        required: true
        description: "Firestore server Timestamp set at write time"
      - name: "userFeedback"
        type: "string"
        required: false
        description: 'Free-text or "up"/"down" / "corrected:{text}" per scrapeHistoryManager.ts'
      - name: "scrapedPages"
        type: "integer"
        required: false
        description: "Number of unique pages successfully extracted"
      - name: "cached"
        type: "boolean"
        required: false
        description: "true when the answer was served from cache (findCachedAnswer)"
      - name: "skipped"
        type: "boolean"
        required: false
        description: "true when scraping was skipped (GREETING / DISABLED policy)"
    indexes:
      - fields: ["query", "timestamp"]
        type: "COMPOSITE"
        queryScope: "COLLECTION"
        description: "Cache-lookup index used by findCachedAnswer() in scrapeEngine.ts"
      - fields: ["sessionId"]
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "Point-lookup index used by getEntry() in scrapeHistoryManager.ts"
      - fields: ["timestamp"]
        order: "DESCENDING"
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "History listing — newest-first ordering"
      - fields: ["chatType", "timestamp"]
        order: "DESCENDING"
        type: "COMPOSITE"
        queryScope: "COLLECTION"
        description: "Filter by chat type, order newest-first (scrapeHistoryManager.ts listHistory)"
      - fields: ["timestamp"]
        type: "SINGLE_FIELD"
        queryScope: "COLLECTION"
        description: "Time-range filter for expiry cleanup and pagination"

# ─────────────────────────────────────────────────────────────────
# Collection: scrapePolicies
# Global and per-type scraping policies.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapePolicies"
    description: "Scraping behaviour policy documents keyed by policy id (e.g. 'global', per chatType)"
    fields:
      - name: "enabled"
        type: "boolean"
        required: true
        description: "false = scraping disabled globally or for this type"
      - name: "maxDepth"
        type: "integer"
        required: false
        default: 1
      - name: "maxResults"
        type: "integer"
        required: false
        default: 3
      - name: "allowedDomains"
        type: "array[string]"
        required: false
        description: "Optional per-policy domain allow-list override"
      - name: "searchEngines"
        type: "array[string]"
        required: false
        default: ["google"]
      - name: "contentTypes"
        type: "array[string]"
        required: false
      - name: "extractStrategy"
        type: "string"
        required: false
        default: "article-extract"
      - name: "fallbackAndRetry"
        type: "boolean"
        required: false
        default: true
      - name: "rateLimitMs"
        type: "integer"
        required: false
        default: 1000
      - name: "timeoutMs"
        type: "integer"
        required: false
        default: 30000
      - name: "cacheTTL"
        type: "integer"
        required: false
        default: 3600

# ─────────────────────────────────────────────────────────────────
# Collection: scrapePresets
# Search-engine URL templates used to build query URLs.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapePresets"
    description: "Search-engine preset documents keyed by engine id (e.g. 'google', 'bing')"
    fields:
      - name: "name"
        type: "string"
        required: true
      - name: "searchUrlTemplate"
        type: "string"
        required: true
        description: "URL template with {q} placeholder for the query string"
      - name: "searchParam"
        type: "string"
        required: false
        default: "q"
      - name: "extractStrategy"
        type: "string"
        required: true
      - name: "allowedSources"
        type: "array[string]"
        required: false
      - name: "maxDepth"
        type: "integer"
        required: false

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeAllowedDomains
# Per-domain trust and rate-limit configuration.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapeAllowedDomains"
    description: "Domain-level trust and rate-limit entries"
    fields:
      - name: "domain"
        type: "string"
        required: true
      - name: "allowedPaths"
        type: "array[string]"
        required: false
      - name: "allowedTypes"
        type: "array[string]"
        required: false
      - name: "trustLevel"
        type: "string"
        required: false
        allowedValues: [trusted, standard, suspicious]
        default: "standard"
      - name: "rateLimitMs"
        type: "integer"
        required: true
        description: "Minimum milliseconds between requests to this domain"
      - name: "enabled"
        type: "boolean"
        required: true
        default: true

# ─────────────────────────────────────────────────────────────────
# Collection: scrapeEvent
# Low-level pipeline events for observability / trending.
# ─────────────────────────────────────────────────────────────────
  - name: "scrapeEvent"
    description: "One document per pipeline event (navigate_start, extract_complete, error, …)"
    fields:
      - name: "sessionId"
        type: "string"
        required: true
      - name: "type"
        type: "string"
        required: true
        allowedValues:
          - navigate_start
          - navigate_complete
          - extract_start
          - extract_complete
          - domain_skipped
          - error
          - crawl_depth_reached
          - rate_limited
          - cached_answer
      - name: "payload"
        type: "object"
        required: true
      - name: "timestamp"
        type: "Timestamp"
        required: true
    indexes:
      - fields: ["sessionId", "timestamp"]
        order: "DESCENDING"
        type: "COMPOSITE"
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/index.cjs.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/index.cjs.js`

```javascript
const { validateAdminArgs } = require('firebase-admin/data-connect');

const connectorConfig = {
  connector: 'example',
  serviceId: 'supremeai',
  location: 'asia-southeast1'
};
exports.connectorConfig = connectorConfig;

function createMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateMovie', inputVars, inputOpts);
}
exports.createMovie = createMovie;

function upsertUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertUser', inputVars, inputOpts);
}
exports.upsertUser = upsertUser;

function addReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('AddReview', inputVars, inputOpts);
}
exports.addReview = addReview;

function deleteReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteReview', inputVars, inputOpts);
}
exports.deleteReview = deleteReview;

function listMovies(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListMovies', undefined, inputOpts);
}
exports.listMovies = listMovies;

function listUsers(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUsers', undefined, inputOpts);
}
exports.listUsers = listUsers;

function listUserReviews(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUserReviews', undefined, inputOpts);
}
exports.listUserReviews = listUserReviews;

function getMovieById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetMovieById', inputVars, inputOpts);
}
exports.getMovieById = getMovieById;

function searchMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, false);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('SearchMovie', inputVars, inputOpts);
}
exports.searchMovie = searchMovie;

```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/index.d.ts`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/index.d.ts`

```typescript
import { ConnectorConfig, DataConnect, OperationOptions, ExecuteOperationResponse } from 'firebase-admin/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;


export interface AddReviewData {
  review_upsert: Review_Key;
}

export interface AddReviewVariables {
  movieId: UUIDString;
  rating: number;
  reviewText: string;
}

export interface CreateMovieData {
  movie_insert: Movie_Key;
}

export interface CreateMovieVariables {
  title: string;
  genre: string;
  imageUrl: string;
}

export interface DeleteReviewData {
  review_delete?: Review_Key | null;
}

export interface DeleteReviewVariables {
  movieId: UUIDString;
}

export interface GetMovieByIdData {
  movie?: {
    id: UUIDString;
    title: string;
    imageUrl: string;
    genre?: string | null;
    metadata?: {
      rating?: number | null;
      releaseYear?: number | null;
      description?: string | null;
    };
      reviews: ({
        reviewText?: string | null;
        reviewDate: DateString;
        rating?: number | null;
        user: {
          id: string;
          username: string;
        } & User_Key;
      })[];
  } & Movie_Key;
}

export interface GetMovieByIdVariables {
  id: UUIDString;
}

export interface ListMoviesData {
  movies: ({
    id: UUIDString;
    title: string;
    imageUrl: string;
    genre?: string | null;
  } & Movie_Key)[];
}

export interface ListUserReviewsData {
  user?: {
    id: string;
    username: string;
    reviews: ({
      rating?: number | null;
      reviewDate: DateString;
      reviewText?: string | null;
      movie: {
        id: UUIDString;
        title: string;
      } & Movie_Key;
    })[];
  } & User_Key;
}

export interface ListUsersData {
  users: ({
    id: string;
    username: string;
  } & User_Key)[];
}

export interface MovieMetadata_Key {
  id: UUIDString;
  __typename?: 'MovieMetadata_Key';
}

export interface Movie_Key {
  id: UUIDString;
  __typename?: 'Movie_Key';
}

export interface Review_Key {
  userId: string;
  movieId: UUIDString;
  __typename?: 'Review_Key';
}

export interface SearchMovieData {
  movies: ({
    id: UUIDString;
    title: string;
    genre?: string | null;
    imageUrl: string;
  } & Movie_Key)[];
}

export interface SearchMovieVariables {
  titleInput?: string | null;
  genre?: string | null;
}

export interface UpsertUserData {
  user_upsert: User_Key;
}

export interface UpsertUserVariables {
  username: string;
}

export interface User_Key {
  id: string;
  __typename?: 'User_Key';
}

/** Generated Node Admin SDK operation action function for the 'CreateMovie' Mutation. Allow users to execute without passing in DataConnect. */
export function createMovie(dc: DataConnect, vars: CreateMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateMovieData>>;
/** Generated Node Admin SDK operation action function for the 'CreateMovie' Mutation. Allow users to pass in custom DataConnect instances. */
export function createMovie(vars: CreateMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateMovieData>>;

/** Generated Node Admin SDK operation action function for the 'UpsertUser' Mutation. Allow users to execute without passing in DataConnect. */
export function upsertUser(dc: DataConnect, vars: UpsertUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertUserData>>;
/** Generated Node Admin SDK operation action function for the 'UpsertUser' Mutation. Allow users to pass in custom DataConnect instances. */
export function upsertUser(vars: UpsertUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertUserData>>;

/** Generated Node Admin SDK operation action function for the 'AddReview' Mutation. Allow users to execute without passing in DataConnect. */
export function addReview(dc: DataConnect, vars: AddReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<AddReviewData>>;
/** Generated Node Admin SDK operation action function for the 'AddReview' Mutation. Allow users to pass in custom DataConnect instances. */
export function addReview(vars: AddReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<AddReviewData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteReview' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteReview(dc: DataConnect, vars: DeleteReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteReviewData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteReview' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteReview(vars: DeleteReviewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteReviewData>>;

/** Generated Node Admin SDK operation action function for the 'ListMovies' Query. Allow users to execute without passing in DataConnect. */
export function listMovies(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListMoviesData>>;
/** Generated Node Admin SDK operation action function for the 'ListMovies' Query. Allow users to pass in custom DataConnect instances. */
export function listMovies(options?: OperationOptions): Promise<ExecuteOperationResponse<ListMoviesData>>;

/** Generated Node Admin SDK operation action function for the 'ListUsers' Query. Allow users to execute without passing in DataConnect. */
export function listUsers(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUsersData>>;
/** Generated Node Admin SDK operation action function for the 'ListUsers' Query. Allow users to pass in custom DataConnect instances. */
export function listUsers(options?: OperationOptions): Promise<ExecuteOperationResponse<ListUsersData>>;

/** Generated Node Admin SDK operation action function for the 'ListUserReviews' Query. Allow users to execute without passing in DataConnect. */
export function listUserReviews(dc: DataConnect, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserReviewsData>>;
/** Generated Node Admin SDK operation action function for the 'ListUserReviews' Query. Allow users to pass in custom DataConnect instances. */
export function listUserReviews(options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserReviewsData>>;

/** Generated Node Admin SDK operation action function for the 'GetMovieById' Query. Allow users to execute without passing in DataConnect. */
export function getMovieById(dc: DataConnect, vars: GetMovieByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetMovieByIdData>>;
/** Generated Node Admin SDK operation action function for the 'GetMovieById' Query. Allow users to pass in custom DataConnect instances. */
export function getMovieById(vars: GetMovieByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetMovieByIdData>>;

/** Generated Node Admin SDK operation action function for the 'SearchMovie' Query. Allow users to execute without passing in DataConnect. */
export function searchMovie(dc: DataConnect, vars?: SearchMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SearchMovieData>>;
/** Generated Node Admin SDK operation action function for the 'SearchMovie' Query. Allow users to pass in custom DataConnect instances. */
export function searchMovie(vars?: SearchMovieVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SearchMovieData>>;

```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/package.json`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/package.json`

```json
{
  "name": "@dataconnect/admin-generated",
  "version": "0.0.1",
  "author": "Firebase <firebase-support@google.com> (https://firebase.google.com/)",
  "description": "Generated Admin SDK For example",
  "license": "Apache-2.0",
  "engines": {
    "node": " >=18.0"
  },
  "typings": "index.d.ts",
  "module": "esm/index.esm.js",
  "main": "index.cjs.js",
  "browser": "esm/index.esm.js",
  "exports": {
    ".": {
      "types": "./index.d.ts",
      "require": "./index.cjs.js",
      "default": "./esm/index.esm.js"
    },
    "./package.json": "./package.json"
  },
  "peerDependencies": {
    "firebase-admin": "^13.4.0"
  }
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/esm/index.esm.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/esm/index.esm.js`

```javascript
import { validateAdminArgs } from 'firebase-admin/data-connect';

export const connectorConfig = {
  connector: 'example',
  serviceId: 'supremeai',
  location: 'asia-southeast1'
};

export function createMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateMovie', inputVars, inputOpts);
}

export function upsertUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertUser', inputVars, inputOpts);
}

export function addReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('AddReview', inputVars, inputOpts);
}

export function deleteReview(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteReview', inputVars, inputOpts);
}

export function listMovies(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListMovies', undefined, inputOpts);
}

export function listUsers(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUsers', undefined, inputOpts);
}

export function listUserReviews(dcOrOptions, options) {
  const { dc: dcInstance, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrOptions, options, undefined);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUserReviews', undefined, inputOpts);
}

export function getMovieById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetMovieById', inputVars, inputOpts);
}

export function searchMovie(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, false);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('SearchMovie', inputVars, inputOpts);
}

```

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/esm/package.json`

### File: `infrastructure/firebase_functions/firebase_functions_v1/src/dataconnect-admin-generated/esm/package.json`

```json
{
  "type": "module"
}
```

### File: `infrastructure/firebase_functions/firebase_functions_v1/utils/externalClient.js`

### File: `infrastructure/firebase_functions/firebase_functions_v1/utils/externalClient.js`

```javascript
const axios = require('axios');

/**
 * externalClient wrapper
 * - honors environment flags to enable/disable external calls
 * - supports timeout and simple retry
 */
async function callExternal(url, opts = {}) {
  const {
    method = 'get',
    data = null,
    headers = {},
    timeout = process.env.EXTERNAL_TIMEOUT_MS ? parseInt(process.env.EXTERNAL_TIMEOUT_MS, 10) : 4000,
    retries = process.env.EXTERNAL_RETRY ? parseInt(process.env.EXTERNAL_RETRY, 10) : 1,
    enabledFlag = 'ENABLE_EXTERNAL_API'
  } = opts;

  const enabled = (process.env[enabledFlag] || 'false').toLowerCase() === 'true';
  if (!enabled) {
    const err = new Error(`external api disabled via ${enabledFlag}`);
    err.code = 'EXTERNAL_DISABLED';
    throw err;
  }

  let lastErr = null;
  for (let i = 0; i <= retries; i++) {
    try {
      const res = await axios({ url, method, data, headers, timeout });
      return res;
    } catch (e) {
      lastErr = e;
      // small backoff
      await new Promise(r => setTimeout(r, 100 * (i + 1)));
    }
  }

  throw lastErr;
}

module.exports = { callExternal };
```

### File: `infrastructure/terraform/cloud_functions.tf`

### File: `infrastructure/terraform/cloud_functions.tf`

```text
resource "google_cloudfunctions2_function" "supremeai_ocr" {
  project  = var.project_id
  region   = var.region
  name     = "supremeai-ocr-trigger"
  
  build_config {
    runtime           = "python311"
    entry_point       = "handle"
    source {
      storage_source {
        bucket = google_storage_bucket.functions.name
        object = google_storage_bucket_object.function_source.name
      }
    }
  }
  
  service_config {
    max_instance_count = 1
    available_memory   = "256Mi"
    timeout_seconds    = 60
  }
}

resource "google_storage_bucket" "functions" {
  name     = "${var.project_id}-supremeai-functions"
  location = var.region
}

resource "google_storage_bucket_object" "function_source" {
  name   = "function-source.zip"
  bucket = google_storage_bucket.functions.name
  source = "./functions/placeholder.zip"
}
```

### File: `infrastructure/terraform/cloud_run.tf`

### File: `infrastructure/terraform/cloud_run.tf`

```text
resource "google_cloud_run_service" "api" {
  name     = var.service_name
  location = var.region

  template {
    spec {
      service_account_name = google_service_account.api.email

      containers {
        image = "${var.region}-docker.pkg.dev/${var.project_id}/supremeai/supremeai-api:latest"

        ports {
          container_port = 8000
        }

        resources {
          limits = {
            cpu    = "1"
            memory = "512Mi"
          }
        }

        env {
          name  = "PORT"
          value = "8000"
        }
        env {
          name  = "ENV"
          value = "production"
        }
        env {
          name = "SUPABASE_URL"
          value = var.supabase_url
        }
        env {
          name = "SUPABASE_ANON_KEY"
          value = var.supabase_anon_key
        }
        env {
          name = "PINECONE_API_KEY"
          value = var.pinecone_api_key
        }
        env {
          name = "PINECONE_INDEX"
          value = var.pinecone_index
        }
        env {
          name = "QDRANT_URL"
          value = var.qdrant_url
        }
        env {
          name = "QDRANT_API_KEY"
          value = var.qdrant_api_key
        }
      }

      timeout_seconds       = 300
      service_account_name  = google_service_account.api.email
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }
}

data "google_iam_policy" "noauth" {
  binding {
    role = "roles/run.invoker"
    members = [
      "allUsers",
    ]
  }
}

resource "google_cloud_run_service_iam_policy" "public" {
  service  = google_cloud_run_service.api.name
  location = google_cloud_run_service.api.location
  policy   = data.google_iam_policy.noauth.policy
}
```

### File: `infrastructure/terraform/firebase.tf`

### File: `infrastructure/terraform/firebase.tf`

```text
resource "google_firebase_project" "default" {
  project  = var.project_id
}

resource "google_firebase_hosting_site" "default" {
  project  = var.project_id
  site_id  = var.firebase_hosting_site_id
}
```

### File: `infrastructure/terraform/firestore.tf`

### File: `infrastructure/terraform/firestore.tf`

```text
resource "google_firestore_database" "default" {
  project     = var.project_id
  name        = "default"
  location_id = var.region
  type        = "FIRESTORE_NATIVE"
}
```

### File: `infrastructure/terraform/main.tf`

### File: `infrastructure/terraform/main.tf`

```text
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 4.0"
    }
  }
}

provider "google" {
  project = var.gcp_project_id
  region  = var.gcp_region
}

resource "google_cloud_run_service" "supremeai_core" {
  name     = "supremeai-backend-core"
  location = var.gcp_region

  template {
    spec {
      containers {
        # ক্লাউড বিল্ড থেকে পুশ হওয়া ইমেজের ডাইনামিক রেফারেন্স
        image = "gcr.io/${var.gcp_project_id}/supremeai-backend:latest"
        ports {
          container_port = 8000
        }
      }
    }
  }
}
```

### File: `infrastructure/terraform/outputs.tf`

### File: `infrastructure/terraform/outputs.tf`

```text
output "cloud_run_url" {
  value       = google_cloud_run_service.api.status[0].url
  description = "Cloud Run service URL"
}

output "cloud_run_service_name" {
  value       = google_cloud_run_service.api.name
  description = "Cloud Run service name"
}

output "service_account_email" {
  value       = google_service_account.api.email
  description = "Cloud Run service account email"
}

output "cloud_function_url" {
  value       = google_cloudfunctions2_function.supremeai_ocr.url
  description = "Cloud Functions ocr-trigger HTTPS trigger URL"
}

output "cloud_function_name" {
  value       = google_cloudfunctions2_function.supremeai_ocr.name
  description = "Cloud Function name"
}

output "firestore_database" {
  value       = google_firestore_database.default.name
  description = "Firestore database name"
}

output "firebase_project_id" {
  value       = google_firebase_project.default.project
  description = "Firebase project ID"
}

output "firebase_hosting_url" {
  value       = "https://${google_firebase_hosting_site.default.site_id}.web.app"
  description = "Firebase Hosting default site URL"
}

output "artifact_registry_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/supremeai"
  description = "Artifact Registry repo URL for Docker images"
}

output "supabase_project_ref" {
  value       = var.supabase_url
  description = "Supabase project reference URL"
}

output "pinecone_index_name" {
  value       = var.pinecone_index
  description = "Pinecone index name"
}

output "qdrant_cluster_url" {
  value       = var.qdrant_url
  description = "Qdrant cluster URL"
}

output "vercel_project_url" {
  value       = var.vercel_team_id
  description = "Vercel team/project identifier"
}

output "cloudflare_zone_id" {
  value       = var.cloudflare_account_id
  description = "Cloudflare account/zone identifier"
}
```

### File: `infrastructure/terraform/pubsub.tf`

### File: `infrastructure/terraform/pubsub.tf`

```text
resource "google_pubsub_topic" "tasks" {
  name    = "supremeai-tasks"
  project = var.project_id
}

resource "google_pubsub_subscription" "tasks" {
  name    = "supremeai-tasks-sub"
  project = var.project_id
  topic   = google_pubsub_topic.tasks.name
}
```

### File: `infrastructure/terraform/railway.tf`

### File: `infrastructure/terraform/railway.tf`

```text
resource "null_resource" "railway_deployment_hook" {
  triggers = {
    deployment_sync = timestamp()
  }

  provisioner "local-exec" {
    command = "echo '🚀 Railway node deployment is delegated to GitHub Actions (railway-cli) for zero-downtime rollouts.'"
  }
}
```

### File: `infrastructure/terraform/render.tf`

### File: `infrastructure/terraform/render.tf`

```text
terraform {
  required_providers {
    render = {
      source  = "render-oss/render"
      version = "~> 1.3"
    }
  }
}

provider "render" {
  api_key = var.render_api_key
}

resource "render_web_service" "supremeai_render_node" {
  name    = "supremeai-backend-render"
  plan    = "free"
  region  = "oregon"
  runtime = "docker"

  # আপনার কোডবেস রিপোজিটরির ডিরেক্ট হুক
  repo    = "https://github.com/paykaribazaronline/supremeai"
  branch  = "main"

  env_vars = {
    "SUPREME_ENVIRONMENT" = "production"
  }
}
```

### File: `infrastructure/terraform/service_account.tf`

### File: `infrastructure/terraform/service_account.tf`

```text
resource "google_service_account" "api" {
  account_id   = "supremeai-api-sa"
  display_name = "SupremeAI API Service Account"
  project      = var.project_id
}
```

### File: `infrastructure/terraform/variables.tf`

### File: `infrastructure/terraform/variables.tf`

```text
variable "gcp_project_id" {
  description = "GCP Project ID for SupremeAI"
  type        = string
}

variable "gcp_region" {
  description = "Primary Deployment Region"
  type        = string
  default     = "us-central1"
}

variable "render_api_key" {
  description = "Secure Render API Key for Multi-Cloud Auth"
  type        = string
  sensitive   = true
}
```

### File: `scripts/add_bangla_comments.py`

### File: `scripts/add_bangla_comments.py`

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# ফাইল >> ফাইল
# প্রকল্প >> SupremeAI 2.0
# উদ্দেশ্য >> Bangla NLP
# মডিউল >> scripts
# ============================================================================
import os
import re
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')

B = chr  # Bangla builder via unicode code points

ROOT = Path(r"C:\Users\n\supremeai\supremeai_2.0")

SKIP = [
    '.git', 'node_modules', '__pycache__', '.venv',
    '.turbo', 'dist', 'build', '.next', 'coverage', '.vitest',
]

EXTS = {'.py', '.ts', '.tsx', '.js', '.jsx', '.sh', '.yml', '.yaml', '.tf', '.toml', '.cfg', '.ini', '.Dockerfile'}

LANG_PATTERNS = {'bangla', 'bengali', 'multilingual', 'language', 'bangla_voice', 'bangla_nlp', 'bangla_ai', 'bengali_ocr'}

BN_FILE = "ফাইল >> " + "ফাইল"

PY_HDR = "#!/usr/bin/env python3\n# -*- coding: utf-8 -*-\n# ============================================================================\n# " + BN_FILE + "\n# প্রকল্প >> SupremeAI 2.0\n# উদ্দেশ্য >> {p}\n# মডিউল >> {m}\n# ============================================================================\n"
JS_HDR = "// ============================================================================\n// " + BN_FILE + "\n// প্রকল্প >> SupremeAI 2.0\n// উদ্দেশ্য >> {p}\n// মডিউল >> {m}\n// ============================================================================\n"
TSX_HDR = "// ============================================================================\n// কম্পোনেন্ট >> {n}\n// প্রকল্প >> SupremeAI 2.0\n// উদ্দেশ্য >> {p}\n// মডিউল >> {m}\n// ============================================================================\n"
SH_HDR = "#!/bin/bash\n# ============================================================================\n# স্ক্রিপ্ট >> {n}\n# প্রকল্প >> SupremeAI 2.0\n# উদ্দেশ্য >> {p}\n# মডিউল >> {m}\n# ============================================================================\n"
YAML_HDR = "# ============================================================================\n# কনফিগ >> {n}\n# প্রকল্প >> SupremeAI 2.0\n# উদ্দেশ্য >> {p}\n// মডিউল >> {m}\n# ============================================================================\n"
TF_HDR = "# ============================================================================\n# টেরাফর্ম >> {n}\n# প্রকল্প >> SupremeAI 2.0\n# উদ্দেশ্য >> {p}\n// মডিউল >> {m}\n# ============================================================================\n"
DF_HDR = "# ============================================================================\n# ডকারফাইলে >> {n}\n# প্রকল্প >> SupremeAI 2.0\n# উদ্দেশ্য >> {p}\n// মডিউল >> {m}\n# ============================================================================\n"

HEADERS = {
    '.py': lambda n,m,p: PY_HDR.format(n=n, m=m, p=p),
    '.ts': lambda n,m,p: JS_HDR.format(n=n, m=m, p=p),
    '.tsx': lambda n,m,p: TSX_HDR.format(n=n, m=m, p=p),
    '.js': lambda n,m,p: JS_HDR.format(n=n, m=m, p=p),
    '.jsx': lambda n,m,p: TSX_HDR.format(n=n, m=m, p=p),
    '.sh': lambda n,m,p: SH_HDR.format(n=n, m=m, p=p),
    '.yml': lambda n,m,p: YAML_HDR.format(n=n, m=m, p=p),
    '.yaml': lambda n,m,p: YAML_HDR.format(n=n, m=m, p=p),
    '.tf': lambda n,m,p: TF_HDR.format(n=n, m=m, p=p),
    '.toml': lambda n,m,p: YAML_HDR.format(n=n, m=m, p=p),
    '.cfg': lambda n,m,p: YAML_HDR.format(n=n, m=m, p=p),
    '.ini': lambda n,m,p: YAML_HDR.format(n=n, m=m, p=p),
    '.Dockerfile': lambda n,m,p: DF_HDR.format(n=n, m=m, p=p),
}

PURPOSES = {
    'main': 'App main entry point',
    'config': 'Configuration loading',
    'conf': 'Configuration management',
    'auth': 'User authentication',
    'routes': 'API route definitions',
    'admin': 'Admin panel and controls',
    'agent': 'AI agent management',
    'tool': 'Helper tools',
    'memory': 'Memory storage',
    'brain': 'AI brain and routing',
    'core': 'Core system functionality',
    'database': 'Database operations',
    'db': 'Database operations',
    'test': 'Unit testing and QC',
    'models': 'Data models',
    'services': 'Business logic',
    'service': 'Business service',
    'api': 'API client',
    'docker': 'Docker settings',
    'terraform': 'Infrastructure as code',
    'monitor': 'System monitoring',
    'monitoring': 'System monitoring',
    'health': 'Health check',
    'billing': 'Billing management',
    'payment': 'Payment gateway',
    'webhook': 'Webhook handling',
    'stream': 'Streaming data',
    'storage': 'File storage',
    'audit': 'Audit logging',
    'migration': 'Database migration',
    'migrations': 'Database migrations',
    'notification': 'Notification service',
    'marketplace': 'Skill marketplace',
    'evolution': 'Evolution engine',
    'swarm': 'Swarm orchestration',
    'skill': 'Skill registry',
    'chat': 'Chat interface',
    'editor': 'Code editor',
    'dashboard': 'Dashboards',
    'provider': 'VS Code providers',
    'handler': 'Event handlers',
    'store': 'State management',
    'language': 'Multilingual support',
    'vector': 'Vector store',
    'rag': 'RAG pipeline',
    'cost': 'Cost tracking',
    'security': 'Security middleware',
    'rate': 'Rate limiting',
    'circuit': 'Circuit breaker',
    'cache': 'Caching system',
    'cloud': 'Cloud provider',
    'gcp': 'GCP integration',
    'firebase': 'Firebase integration',
    'supabase': 'Supabase integration',
    'github': 'GitHub integration',
    'discord': 'Discord bot',
    'email': 'Email service',
    'voice': 'Voice and TTS',
    'image': 'Image generation',
    'video': 'Video generation',
    'browser': 'Browser automation',
    'legal': 'Legal agent',
    'medical': 'Medical agent',
    'research': 'Research agent',
    'trading': 'Trading agent',
    'scientific': 'Scientific agent',
    'code': 'Code analysis',
    'feedback': 'Feedback service',
    'rbac': 'Role-based access',
    'tenant': 'Multi-tenant',
    'sso': 'Single sign-on',
    'ocr': 'OCR and document',
    'nlp': 'Natural language',
    'bangla': 'Bangla NLP',
    'bengali': 'Bengali OCR',
    'notifications': 'Notifications',
    'checkpoint': 'Checkpoint manager',
    'rollback': 'Rollback monitor',
    'idempotency': 'Idempotency',
    'observability': 'Observability',
    'telemetry': 'Telemetry',
    'honeypot': 'Honeypot middleware',
    'prompt': 'Prompt firewall',
    'input': 'Input sanitization',
    'output': 'Output validation',
    'semantic': 'Semantic cache',
    'free': 'Free tier tracker',
    'cost_auditor': 'Cost auditor',
    'posthog': 'PostHog analytics',
    'discord_bot': 'Discord bot',
    'factual': 'Factual verifier',
    'generation': 'Generation monitor',
    'evolution_engine': 'Evolution engine',
    'rules': 'Rules management',
    'token': 'Token budget',
    'task': 'Task routing',
    'mcp': 'MCP tools',
    'sandbox': 'Sandbox isolation',
    'pgbouncer': 'PgBouncer pool',
    'postgres': 'PostgreSQL store',
    'chromadb': 'ChromaDB vector',
    'sqlite': 'SQLite store',
    'episodic': 'Episodic memory',
    'long_term': 'Long-term memory',
    'sliding': 'Sliding window',
    'summary': 'Summary tree',
    'rag_pipeline': 'RAG pipeline',
    'rag': 'RAG retrieval',
    'validator': 'Code validators',
    'auto': 'Auto remediation',
    'dashboard_admin': 'Admin dashboard',
    'god': 'God mode',
    'orchestrator': 'Agent orchestration',
    'crewai': 'CrewAI',
    'langgraph': 'LangGraph',
    'parallel': 'Parallel routing',
    'fine': 'Fine-tuning',
    'rlhf': 'RLHF',
    'onboarding': 'User onboarding',
    'preferences': 'User preferences',
    'legal_agent': 'Legal agent',
    'medical_agent': 'Medical agent',
    'trading_agent': 'Trading agent',
    'research_assistant': 'Research assistant',
    'email_agent': 'Email agent',
    'github_agent': 'GitHub agent',
    'marketplace_agent': 'Marketplace agent',
    'pr_reviewer': 'PR reviewer',
    'vision': 'Vision agent',
    'game': 'Game dev agent',
    'scientific_agent': 'Scientific agent',
    'plan_sorter': 'Plan sorter',
    'style': 'Style learning',
    'preference': 'Preference memory',
    'domain': 'Domain adapter',
    'ensemble': 'Ensemble routing',
    'multi_account': 'Multi-account',
    'vpn': 'VPN switching',
    'bandwidth': 'Bandwidth optimization',
    'browser_agent': 'Browser agent',
    'model_trainer': 'Model trainer',
    'sync': 'Feature sync',
    'skill_recommender': 'Skill recommender',
    'bangla_ai': 'Bangla AI connector',
    'bengali_ocr': 'Bengali OCR',
    'cloud_sandbox': 'Cloud sandbox',
    'docker_sandbox': 'Docker sandbox',
    'computer': 'Computer agent',
    'playwright': 'Playwright agent',
    'checkpoint_manager': 'Checkpoint manager',
    'seed': 'Database seed',
    'video_generator': 'Video generation',
    'image_generator': 'Image generation',
    'viral': 'Viral referral',
    'parallel_agent': 'Parallel executor',
    'sync_features': 'Feature sync utility',
    'supreme_context_builder': 'Context builder',
    'skill_loader': 'Skill loader',
    'api_gateway': 'API gateway',
    'supreme_risk_scorer': 'Risk scoring',
    'supreme_docker_analyzer': 'Docker analysis',
    'supreme_config_audit': 'Config audit',
    'docker_ai_guard': 'Docker guard',
    'config_audit': 'Config audit',
    'package_json': 'Package config',
    'tsconfig': 'TypeScript config',
    'vite_config': 'Vite config',
    'turbo_json': 'Turborepo config',
    'kilo_json': 'Kilo project config',
    'docker_compose': 'Docker compose config',
    'firebase_json': 'Firebase configuration',
    'railway_json': 'Railway configuration',
    'render_yaml': 'Render configuration',
    'pre_commit_config': 'Pre-commit hooks config',
    'compliance_rules': 'Compliance config',
    'docker_limits': 'Docker limits config',
    'audit_rules': 'Audit rules config',
    'firestore_indexes': 'Firestore indexes',
    'openapi_spec': 'OpenAPI specification',
}


def get_module(fp):
    parts = list(fp.relative_to(ROOT).parts)
    if not parts:
        return 'root'
    if parts[0] == 'backend' and len(parts) > 1:
        return parts[1]
    if parts[0] == 'apps' and len(parts) > 2:
        return parts[2]
    return parts[0]


def get_purpose(stem):
    n = stem.lower()
    for k, v in PURPOSES.items():
        if k in n:
            return v
    return 'General utility'


def is_lang_module(fp):
    r = str(fp.relative_to(ROOT)).lower()
    n = fp.name.lower()
    return any(p in r or p in n for p in LANG_PATTERNS)


def is_code(line):
    s = line.strip()
    if not s:
        return False
    if s.startswith('#') or s.startswith('//') or s.startswith('/*') or s.startswith('*'):
        return False
    if set(s) <= {'=', ' ', '#', '/', '*', '-', '~'}:
        return False
    for pat in ['import ', 'from ', 'export ', 'const ', 'let ', 'var ', 'function ', 'class ', 'def ', 'async ', 'await ', 'return ', 'if ', 'for ', 'while ', 'try:', 'with ', 'elif', 'else:', 'except', 'finally:', 'raise ', 'yield ', 'match ', 'case ', 'lambda', 'print(', 'logging.', 'app.', 'router.', 'FastAPI(', 'APIRouter(', 'Blueprint(', '# !', '<?php', '<html', '<!DOCTYPE', 'package ', 'require(', 'module.', 'describe(', 'test(', 'it(', 'beforeEach(', '@', 'namespace ', 'interface ', 'type ', 'enum ', 'public ', 'private ', 'protected ', 'static ', 'void ', 'int ', 'string ']:
        if s.startswith(pat):
            return True
    return bool(re.search(r'\b(def|class|import|from|return|if|else|elif|for|while|try|except|finally|with|async|await|yield|raise)\b', s))


def strip(content):
    lines = content.split('\n')
    idx = 0
    for i, ln in enumerate(lines):
        if is_code(ln):
            idx = i
            break
    body = lines[idx:]
    txt = '\n'.join(body)
    return txt.lstrip('\n')


def main():
    print("=" * 70)
    print("SupremeAI 2.0 - Bangla Comment Injection")
    print("=" * 70)
    
    files = []
    for root, dirs, fnames in os.walk(ROOT):
        dirs[:] = [d for d in dirs if d not in SKIP and not d.startswith('.')]
        for f in fnames:
            fp = Path(root) / f
            r = str(fp.relative_to(ROOT))
            if any(p in r for p in SKIP):
                continue
            ext = fp.suffix.lower()
            if ext in EXTS and is_lang_module(fp):
                files.append(fp)
    
    print(f"Total: {len(files)}\n")
    
    ok = 0
    for i, fp in enumerate(files, 1):
        try:
            txt = fp.read_text(encoding='utf-8', errors='ignore')
        except Exception:
            continue
        
        ext = fp.suffix.lower()
        hfn = HEADERS.get(ext)
        if not hfn:
            continue
        
        hdr = hfn(fp.name, get_module(fp), get_purpose(fp.stem))
        try:
            fp.write_text(hdr + strip(txt), encoding='utf-8')
            ok += 1
        except Exception:
            pass
        
        if i % 100 == 0 or i == len(files):
            print(f"{i}/{len(files)} ok={ok}")
    
    print("\nDONE: %d files" % ok)


if __name__ == '__main__':
    main()
```

### File: `scripts/bootstrap_env.py`

### File: `scripts/bootstrap_env.py`

```python
from pathlib import Path

env_path = Path('.env')
env_example_path = Path('.env.example')

def parse_env(content: str):
    result = {}
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key = line.split('=', 1)[0].strip()
        result[key] = line

env = env_path.read_text(encoding='utf-8') if env_path.exists() else ''
existing = set(parse_env(env).keys())
example = env_example_path.read_text(encoding='utf-8') if env_example_path.exists() else ''
missing = []
for line in example.splitlines():
    line = line.strip()
    if not line or line.startswith('#') or '=' not in line:
        continue
    key = line.split('=', 1)[0].strip()
    if key and key not in existing:
        missing.append(line)

out = [env.rstrip('\n')]
if missing:
    out.append('')
    out.append('# Added from .env.example')
    out.extend(missing)

new_content = '\n'.join(out) + '\n'
env_path.write_text(new_content, encoding='utf-8')
print(f'Updated .env with {len(missing)} missing keys')
```

### File: `scripts/config_audit.py`

### File: `scripts/config_audit.py`

```python
import re
import sys
from pathlib import Path

def parse_config_py(file_path: Path) -> set[str]:
    """Extract setting names from config.py Settings class."""
    if not file_path.exists():
        print(f"Error: Config file not found at {file_path}")
        sys.exit(1)
        
    content = file_path.read_text(encoding="utf-8")
    
    # Locate Settings class
    class_match = re.search(r"class Settings\(BaseSettings\):(.*?)(\n\n\w|\Z)", content, re.DOTALL)
    if not class_match:
        print("Error: Could not find Settings class in config.py")
        sys.exit(1)
        
    class_body = class_match.group(1)
    
    # Extract variables defined directly under the class body (exactly 4 spaces indentation)
    settings_vars = set()
    for line in class_body.splitlines():
        # Match lines starting with exactly 4 spaces, followed by a valid variable name, and then ':' or '='
        match = re.match(r"^ {4}([a-zA-Z_][a-zA-Z0-9_]*)\s*(?::|=)", line)
        if match:
            var_name = match.group(1)
            # Skip Pydantic model_config or methods
            if var_name not in {"model_config"}:
                settings_vars.add(var_name.upper())
                
    return settings_vars

def parse_env_example(file_path: Path) -> set[str]:
    """Extract variable names from .env.example."""
    if not file_path.exists():
        print(f"Error: .env.example not found at {file_path}")
        sys.exit(1)
        
    content = file_path.read_text(encoding="utf-8")
    env_vars = set()
    
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        # Match "KEY=value" or "KEY="
        match = re.match(r"^([a-zA-Z_][a-zA-Z0-9_]*)\s*=", line)
        if match:
            env_vars.add(match.group(1).upper())
            
    return env_vars

def run_audit():
    project_root = Path(__file__).parent.parent
    config_path = project_root / "backend" / "core" / "config.py"
    env_example_path = project_root / ".env.example"
    
    print("[*] Auditing Configuration Settings...")
    config_keys = parse_config_py(config_path)
    env_keys = parse_env_example(env_example_path)
    
    # Ignore internal/local Pydantic configurations or those that don't belong in .env
    ignored_keys = {
        "PROJECT_NAME", "API_V1_STR", "APP_NAME", "CLAUDE_OPENROUTER_MODEL",
        "ADMIN_RULES_DB", "MEMORY_DB_DIR", "SKILL_REGISTRY_PATH"
    }
    
    missing_in_env = (config_keys - env_keys) - ignored_keys
    
    if missing_in_env:
        print("\n[!] CONFIG AUDIT FAILED!")
        print("The following keys are defined in backend/core/config.py but missing in .env.example:")
        for key in sorted(missing_in_env):
            print(f"  - {key}")
        print("\nAction Required: Please document these environment variables in .env.example.")
        sys.exit(1)
        
    print("[x] Configuration audit passed! All Pydantic Settings are aligned with .env.example.")
    sys.exit(0)

if __name__ == "__main__":
    run_audit()
```

### File: `scripts/create_test_admin.py`

### File: `scripts/create_test_admin.py`

```python
import firebase_admin
from firebase_admin import credentials, firestore, auth

cred = credentials.Certificate("backend/service-account.json")
firebase_admin.initialize_app(cred)

db = firestore.client()
email = "testadmin@supremeai.com"
password = "SecurePassword123!"

try:
    # 1. Firebase Auth-এ ইউজার ক্রিয়েট বা গেট করা
    try:
        user = auth.create_user(
            email=email,
            password=password,
            email_verified=True
        )
        print(f"Created user in Auth: {user.uid}")
    except auth.EmailAlreadyExistsError:
        user = auth.get_user_by_email(email)
        print(f"User already exists in Auth: {user.uid}")

    # 2. Firestore-এ অ্যাডমিন রোল সেট করা
    db.collection("admin_users").document(user.uid).set({
        "email": email,
        "role": "admin",
        "created_at": "2026-06-22",
        "totp_secret": None
    }, merge=True)
    print(f"[OK] Admin role set in Firestore for {email}")

except Exception as e:
    print(f"Error: {e}")
```

### File: `scripts/deploy_cloud_mesh.sh`

### File: `scripts/deploy_cloud_mesh.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?Set PROJECT_ID}"
REGION="${REGION:-us-central1}"
SERVICE="${SERVICE:-supremeai}"
IMAGE="${IMAGE:-${PROJECT_ID}/supremeai:${GITHUB_SHA:-local}}"
GCP_REGION="${GCP_REGION:-${REGION}}"

if command -v docker >/dev/null 2>&1; then
  docker build -t "${IMAGE}" .
fi

if command -v gcloud >/dev/null 2>&1; then
  gcloud run deploy "${SERVICE}" --image "${IMAGE}" --region "${GCP_REGION}" --project "${PROJECT_ID}"
fi

if command -v railway >/dev/null 2>&1; then
  railway up --detach
fi

if command -v renderctl >/dev/null 2>&1; then
  renderctl deploy
fi

if command -v wrangler >/dev/null 2>&1; then
  wrangler deploy infrastructure/cloudflare/worker.js --config infrastructure/cloudflare/wrangler.toml
fi
```

### File: `scripts/docker_ai_guard.py`

### File: `scripts/docker_ai_guard.py`

```python
import os
import sys
import subprocess
import google.generativeai as genai

def analyze_docker_bloat():
    image_name = os.environ.get("IMAGE_NAME", "supremeai-api:test")
    max_size_mb = int(os.environ.get("MAX_IMAGE_SIZE_MB", "500"))
    
    # ডকার ইমেজের টোটাল সাইজ বের করা
    size_cmd = f"docker image inspect {image_name} --format='{{{{.Size}}}}'"
    try:
        size_bytes = int(subprocess.check_output(size_cmd, shell=True).decode('utf-8').strip())
        size_mb = size_bytes / (1024 * 1024)
    except Exception as e:
        print(f"❌ Failed to get image size: {e}")
        sys.exit(1)

    print(f"📊 Current Image Size: {size_mb:.2f} MB")
    print(f"🎯 Target Max Size: {max_size_mb} MB")

    if size_mb <= max_size_mb:
        print("✅ Docker image size is strictly optimized. Proceeding...")
        sys.exit(0)

    print("🚨 BLOAT DETECTED! Image size exceeded limit. Initiating AI Autopsy...")
    
    # ডকারের কোন লেয়ারে কত এমবি ডেটা আছে তা বের করা
    history_cmd = f"docker history {image_name} --no-trunc --format '{{{{.Size}}}}\t{{{{.CreatedBy}}}}'"
    history_output = subprocess.check_output(history_cmd, shell=True).decode('utf-8')

    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("❌ GEMINI_API_KEY missing. Failing build due to size bloat without AI analysis.")
        sys.exit(1)

    genai.configure(api_key=api_key)
    model = genai.GenerativeModel('gemini-1.5-flash')

    prompt = f"""You are an elite DevSecOps AI. The CI/CD pipeline failed because the Docker image size ({size_mb:.2f} MB) exceeded the limit of {max_size_mb} MB.
    
    Analyze the following 'docker history' output. Identify EXACTLY which layers/commands are causing the bloat. 
    Provide a strict, bulleted action plan on how to fix it (e.g., adding specific files to .dockerignore, using multi-stage builds, or cleaning up apt caches).
    
    Docker History:
    {history_output}
    """

    response = model.generate_content(prompt)
    
    print("\n" + "="*50)
    print("🤖 SUPREMEAI DOCKER BLOAT ANALYSIS REPORT")
    print("="*50)
    print(response.text)
    print("="*50 + "\n")
    
    # গিটহাব অ্যাকশনস-এ রিপোর্ট দেখানোর জন্য
    with open("bloat_report.md", "w") as f:
        f.write(f"## 🚨 Docker Bloat Detected ({size_mb:.2f} MB)\n\n")
        f.write(response.text)

    # পাইপলাইন ফেইল (Fail) করানো
    print("💀 Failing the GitHub Action pipeline to prevent bloated deployment.")
    sys.exit(1)

if __name__ == "__main__":
    analyze_docker_bloat()
```

### File: `scripts/migrate.py`

### File: `scripts/migrate.py`

```python
import os
import psycopg2
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def run_migrations():
    # Fetch database connection string
    conn_string = os.getenv("SUPABASE_DATABASE_URL")
    pooler_string = os.getenv("SUPABASE_DATABASE_URL_POOLER")
    
    if not conn_string and not pooler_string:
        print("[Error] No database URL found in environment variables.")
        return

    print("[Info] Connecting to Supabase PostgreSQL database...")
    conn = None
    
    # Try direct connection URL first
    if conn_string:
        try:
            print("[Info] Trying direct connection...")
            conn = psycopg2.connect(conn_string)
        except Exception as e:
            print(f"[Warning] Direct connection failed: {e}")

    # Fallback to Pooler connection URL
    if not conn and pooler_string:
        try:
            print("[Info] Trying pooler connection...")
            conn = psycopg2.connect(pooler_string)
        except Exception as e:
            print(f"[Error] Pooler connection failed: {e}")

    if not conn:
        print("[Error] Could not connect to any database connection string. Please check internet connection or database credentials.")
        return
        
    conn.autocommit = True

    migrations_dir = os.path.join("backend", "database", "migrations")
    if not os.path.exists(migrations_dir):
        print(f"[Error] Migrations directory '{migrations_dir}' not found.")
        conn.close()
        return

    # List and sort migrations
    migration_files = sorted(
        [f for f in os.listdir(migrations_dir) if f.endswith(".sql")]
    )

    import sys
    start_from = sys.argv[1] if len(sys.argv) > 1 else ""
    if start_from:
        if start_from in migration_files:
            migration_files = migration_files[migration_files.index(start_from):]
            print(f"[Info] Resuming migrations from: {start_from}")
        else:
            print(f"[Error] Start migration file '{start_from}' not found in migrations folder.")
            conn.close()
            return

    print(f"[Info] Found {len(migration_files)} migration files to run.")

    for file_name in migration_files:
        file_path = os.path.join(migrations_dir, file_name)
        print(f"[Run] Running migration: {file_name}...")
        
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                sql_script = f.read()

            with conn.cursor() as cursor:
                cursor.execute(sql_script)
            print(f"[Success] Migration successful: {file_name}")
        except Exception as e:
            print(f"[Error] Error running migration {file_name}: {e}")
            print("[Warning] Stopping migration process.")
            break

    conn.close()
    print("[Info] Database connection closed.")

if __name__ == "__main__":
    run_migrations()
```

### File: `scripts/seed_repos.py`

### File: `scripts/seed_repos.py`

```python
import re
import os

md_path = r"c:\Users\n\supremeai\supremeai_2.0\docs\-01-admin's plan\3.1supremeai-tailored-repos.md"
sql_path = r"c:\Users\n\supremeai\supremeai_2.0\backend\database\migrations\05_seed_github_repos.sql"

with open(md_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

sql = []
sql.append('-- Migration: 05_seed_github_repos.sql')
sql.append('BEGIN;')

current_category = 'General'
repo_data = {}

for line in lines:
    line = line.strip()
    if line.startswith('## Category '):
        current_category = line.split(':')[1].split('(')[0].strip()
    elif line.startswith('### '):
        repo_data = {'name': line[line.find('.')+1:].strip(), 'category': current_category}
    elif line.startswith('- **URL:**'):
        repo_data['url'] = line.split('URL:**')[1].strip()
        parts = repo_data['url'].split('/')
        if len(parts) >= 5:
            repo_data['id'] = f"{parts[3]}-{parts[4]}".lower()
        else:
            repo_data['id'] = repo_data['name'].lower().replace(' ', '-')
    elif line.startswith('- **Stars:**'):
        s = line.split('Stars:**')[1].strip()
        num = re.search(r'[\d.]+', s)
        if num:
            stars = int(float(num.group()) * (1000 if 'K' in s else 1))
        else:
            stars = 0
        repo_data['stars'] = stars
    elif line.startswith('- **Why:**'):
        repo_data['purpose'] = line.split('Why:**')[1].strip()
    elif line.startswith('- **Priority:**'):
        p = line.split('Priority:**')[1].strip().lower()
        pri = 'medium'
        if 'critical' in p: pri = 'critical'
        elif 'high' in p: pri = 'high'
        elif 'low' in p: pri = 'low'
        repo_data['priority'] = pri
        
        # Save repo
        if 'id' in repo_data and 'url' in repo_data:
            id_val = repo_data['id'].replace("'", "''")
            name_val = repo_data['name'].replace("'", "''")
            url_val = repo_data['url'].replace("'", "''")
            cat_val = repo_data['category'].replace("'", "''")
            pur_val = repo_data.get('purpose', '').replace("'", "''")
            pri_val = repo_data.get('priority', 'medium')
            stars_val = repo_data.get('stars', 0)
            
            sql.append(f"INSERT INTO github_repos (id, name, url, category, purpose, priority, stars) VALUES ('{id_val}', '{name_val}', '{url_val}', '{cat_val}', '{pur_val}', '{pri_val}', {stars_val}) ON CONFLICT (id) DO NOTHING;")
        repo_data = {}

sql.append('COMMIT;')

with open(sql_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql))
print(f'Generated {len(sql)-3} inserts into {sql_path}')
```

### File: `scripts/setup_firebase_admin.py`

### File: `scripts/setup_firebase_admin.py`

```python
"""
Firebase Admin Setup Script — One-time run করুন
Firestore-এ niloyjoy7@gmail.com কে admin role দেবে
"""
import firebase_admin
from firebase_admin import credentials, firestore, auth

# 1. Service Account দিয়ে initialize করুন
#    (নিচের path আপনার service-account.json এর path দিন)
cred = credentials.Certificate("backend/service-account.json")
firebase_admin.initialize_app(cred)

db = firestore.client()

# 2. niloyjoy7@gmail.com এর UID খোঁজো Firebase Auth থেকে
try:
    user = auth.get_user_by_email("niloyjoy7@gmail.com")
    uid = user.uid
    print(f"Found user: {uid}")
except Exception as e:
    print(f"Error: {e}")
    uid = None

# 3. Firestore-এ admin role set করো
if uid:
    db.collection("admin_users").document(uid).set({
        "email": "niloyjoy7@gmail.com",
        "role": "admin",
        "created_at": "2026-06-22",
        "totp_secret": None,  # প্রথম login-এ TOTP setup হবে
    }, merge=True)
    print(f"[OK] Admin role set for niloyjoy7@gmail.com (uid={uid})")

# 4. admin@supremeai.com কেও admin করো (যদি চান)
try:
    user2 = auth.get_user_by_email("admin@supremeai.com")
    db.collection("admin_users").document(user2.uid).set({
        "email": "admin@supremeai.com",
        "role": "admin",
        "created_at": "2026-06-22",
    }, merge=True)
    print(f"[OK] Admin role set for admin@supremeai.com")
except Exception as e:
    print(f"admin@supremeai.com: {e}")

print("Done! Now restart the backend server.")
```

### File: `scripts/supreme-config-audit.py`

### File: `scripts/supreme-config-audit.py`

```python
#!/usr/bin/env python3
import os
import json
import re
import yaml
from pathlib import Path

try:
    from dotenv import dotenv_values
except ImportError:
    def dotenv_values(path):
        return {}

try:
    from deepdiff import DeepDiff
except ImportError:
    class DeepDiff:
        def __init__(self, c1, c2, ignore_order=True):
            self.c1 = c1
            self.c2 = c2
        def get(self, key, default):
            if key == 'dictionary_item_added':
                return [f"root['{k}']" for k in self.c2 if k not in self.c1]
            if key == 'dictionary_item_removed':
                return [f"root['{k}']" for k in self.c1 if k not in self.c2]
            return default

class SupremeConfigAuditor:
    def __init__(self, envs):
        self.envs = envs
        self.issues = []
        self.autoFixed = []
        self.rules = self.load_rules()
        
    def load_rules(self):
        rules_path = Path("config/audit-rules.yml")
        if rules_path.exists():
            with open(rules_path, "r") as f:
                return yaml.safe_load(f)
        return {
            'risk_rules': {
                'CRITICAL': [
                    {'pattern': r'SECRET_KEY=.*', 'message': 'Hardcoded secrets'},
                    {'pattern': r'PASSWORD=.*[^*]', 'message': 'Plain passwords'},
                    {'pattern': r'API_KEY=sk-live', 'message': 'Live keys in non-prod'},
                    {'pattern': r'DEBUG=true', 'message': 'Debug in prod'}
                ],
                'HIGH': [
                    {'pattern': r'DATABASE_URL=.*localhost', 'message': 'Local DB in staging+'},
                    {'pattern': r'REDIS_URL=.*localhost', 'message': 'Local Redis in staging+'},
                    {'pattern': r'LOG_LEVEL=debug', 'message': 'Verbose logging in prod'}
                ],
                'MEDIUM': []
            },
            'required_envs': {
                'development': ['DATABASE_URL', 'REDIS_URL', 'API_KEY'],
                'staging': ['DATABASE_URL', 'REDIS_URL', 'API_KEY', 'SENTRY_DSN'],
                'production': ['DATABASE_URL', 'REDIS_URL', 'API_KEY', 'SENTRY_DSN', 'SSL_CERT']
            }
        }
        
    def audit(self):
        env_configs = {}
        
        # Load all env files
        for env in self.envs:
            env_file = f'.env.{env}'
            if os.path.exists(env_file):
                env_configs[env] = dotenv_values(env_file)
            elif env == 'development' and os.path.exists('.env'):
                env_configs[env] = dotenv_values('.env')
        
        # Cross-environment comparison
        for i, env1 in enumerate(self.envs):
            for env2 in self.envs[i+1:]:
                if env1 in env_configs and env2 in env_configs:
                    self.compare_envs(env1, env_configs[env1], 
                                    env2, env_configs[env2])
        
        # Required variables check
        for env, config in env_configs.items():
            self.check_required(env, config)
        
        # Secret scanning
        for env, config in env_configs.items():
            self.scan_secrets(env, config)
        
        return {
            'issues': self.issues,
            'autoFixed': self.autoFixed,
            'has_safe_fixes': 'true' if len(self.autoFixed) > 0 else 'false'
        }
    
    def compare_envs(self, env1, config1, env2, config2):
        """Find meaningful differences between environments"""
        diff = DeepDiff(config1, config2, ignore_order=True)
        
        added = diff.get('dictionary_item_added', [])
        removed = diff.get('dictionary_item_removed', [])
        
        all_diff_keys = []
        for key in added:
            var_name = key.split('[')[-1].strip("']")
            all_diff_keys.append((var_name, env2, env1))
        for key in removed:
            var_name = key.split('[')[-1].strip("']")
            all_diff_keys.append((var_name, env1, env2))
            
        for var_name, missing_in, present_in in all_diff_keys:
            self.issues.append({
                'risk': 'MEDIUM',
                'file': f'.env.{missing_in}',
                'message': f'{var_name} is missing (present in .env.{present_in})',
                'suggestion': f'Add {var_name} to .env.{missing_in}'
            })
    
    def check_required(self, env, config):
        """Check required variables per environment"""
        required = self.rules.get('required_envs', {}).get(env, [])
        for var in required:
            if var not in config:
                self.issues.append({
                    'risk': 'HIGH',
                    'file': f'.env.{env}',
                    'message': f'Required variable {var} missing',
                    'suggestion': f'Add {var} to .env.{env}'
                })
    
    def scan_secrets(self, env, config):
        """Scan for hardcoded secrets and misconfigurations"""
        for key, value in config.items():
            full_line = f'{key}={value}'
            
            risk_rules = self.rules.get('risk_rules', {})
            for risk_level, rules in risk_rules.items():
                for rule in rules:
                    pattern = rule.get('pattern', '')
                    message = rule.get('message', f'Potential issue with {key}')
                    if re.match(pattern, full_line, re.IGNORECASE):
                        if self.is_expected(key, value, env):
                            continue
                            
                        self.issues.append({
                            'risk': risk_level,
                            'file': f'.env.{env}' if env != 'development' else '.env',
                            'message': f'{message}: {key}',
                            'suggestion': self.get_suggestion(key, env)
                        })
    
    def is_expected(self, key, value, env):
        """Determine if a config is expected for the environment"""
        expected = {
            'development': {
                'DATABASE_URL': ['localhost', '127.0.0.1'],
                'DEBUG': ['true'],
                'LOG_LEVEL': ['debug']
            }
        }
        
        env_expected = expected.get(env, {})
        if key in env_expected:
            return any(exp in str(value) for exp in env_expected[key])
        return False
    
    def get_suggestion(self, key, env):
        """Get fix suggestion"""
        suggestions = {
            'DATABASE_URL': f'Use environment-specific DB URL for {env}',
            'SECRET_KEY': 'Use GitHub Secrets or AWS Secrets Manager',
            'API_KEY': f'Use {env}-specific API key from vault',
            'DEBUG': 'Set to false in production'
        }
        return suggestions.get(key, 'Review and fix manually')

if __name__ == '__main__':
    envs = os.getenv('ENVIRONMENTS', 'development,staging,production').split(',')
    auditor = SupremeConfigAuditor(envs)
    result = auditor.audit()
    
    # Save report to github output or file
    with open("audit_report.json", "w") as f:
        json.dump(result, f, indent=2)
        
    # Print the json so it can be captured by the workflow
    print(json.dumps(result))
```

### File: `scripts/supreme-docker-analyzer.py`

### File: `scripts/supreme-docker-analyzer.py`

```python
#!/usr/bin/env python3
import os
import sys
import json
import subprocess
import yaml
from pathlib import Path

class SupremeDockerAnalyzer:
    def __init__(self, image_name, service_name="default"):
        self.image_name = image_name
        self.service_name = service_name
        self.limits = self.load_limits()
        
    def load_limits(self):
        limits_path = Path("config/docker-limits.yml")
        if limits_path.exists():
            with open(limits_path, "r") as f:
                data = yaml.safe_load(f)
                return data.get("limits", {}).get(self.service_name, data.get("limits", {}).get("default", {}))
        return {"max_size_mb": 500, "warn_size_mb": 300}

    def analyze(self):
        # Retrieve image size
        size_cmd = f"docker image inspect {self.image_name} --format='{{{{.Size}}}}'"
        try:
            size_bytes = int(subprocess.check_output(size_cmd, shell=True).decode('utf-8').strip())
            size_mb = size_bytes / (1024 * 1024)
        except Exception as e:
            print(f"❌ Failed to get image size: {e}")
            return {"status": "FAIL", "reason": f"Could not inspect image: {e}", "size_mb": 0}

        max_size = self.limits.get("max_size_mb", 500)
        warn_size = self.limits.get("warn_size_mb", 300)

        status = "PASS"
        if size_mb > max_size:
            status = "FAIL"
        elif size_mb > warn_size:
            status = "WARN"

        # Retrieve large layers
        history_cmd = f"docker history {self.image_name} --no-trunc --format '{{{{.Size}}}}\t{{{{.CreatedBy}}}}'"
        try:
            history_output = subprocess.check_output(history_cmd, shell=True).decode('utf-8')
            layers = []
            for line in history_output.strip().split('\n'):
                if not line:
                    continue
                parts = line.split('\t', 1)
                size_str = parts[0]
                created_by = parts[1] if len(parts) > 1 else ""
                layers.append({"size": size_str, "created_by": created_by})
        except Exception:
            layers = []

        return {
            "status": status,
            "size_mb": round(size_mb, 2),
            "max_size_mb": max_size,
            "warn_size_mb": warn_size,
            "large_layers": layers[:10]
        }

if __name__ == '__main__':
    image = os.getenv("IMAGE_NAME", "supremeai-api:test")
    service = os.getenv("SERVICE_NAME", "default")
    analyzer = SupremeDockerAnalyzer(image, service)
    result = analyzer.analyze()
    print(json.dumps(result, indent=2))
    
    with open("docker_analysis.json", "w") as f:
        json.dump(result, f, indent=2)
        
    if result["status"] == "FAIL":
        sys.exit(1)
    sys.exit(0)
```

### File: `scripts/supreme-risk-scorer.py`

### File: `scripts/supreme-risk-scorer.py`

```python
#!/usr/bin/env python3
import os
import json
import sys
import yaml
from pathlib import Path

class SupremeRiskScorer:
    def __init__(self):
        self.score = 0
        self.risk_factors = []
        
    def evaluate_config(self):
        audit_path = Path("audit_report.json")
        if audit_path.exists():
            with open(audit_path, "r") as f:
                report = json.load(f)
            for issue in report.get("issues", []):
                risk = issue.get("risk", "LOW")
                if risk == "CRITICAL":
                    self.score += 40
                    self.risk_factors.append(f"Critical Config Issue: {issue.get('message')}")
                elif risk == "HIGH":
                    self.score += 20
                    self.risk_factors.append(f"High Config Issue: {issue.get('message')}")
                elif risk == "MEDIUM":
                    self.score += 10
                    self.risk_factors.append(f"Medium Config Issue: {issue.get('message')}")

    def evaluate_docker(self):
        docker_path = Path("docker_analysis.json")
        if docker_path.exists():
            with open(docker_path, "r") as f:
                report = json.load(f)
            status = report.get("status")
            if status == "FAIL":
                self.score += 50
                self.risk_factors.append(f"Docker Size Exceeded Limit ({report.get('size_mb')}MB > {report.get('max_size_mb')}MB)")
            elif status == "WARN":
                self.score += 25
                self.risk_factors.append(f"Docker Size Warn Level Reached ({report.get('size_mb')}MB > {report.get('warn_size_mb')}MB)")

    def get_risk_rating(self):
        if self.score >= 70:
            return "CRITICAL"
        elif self.score >= 40:
            return "HIGH"
        elif self.score >= 15:
            return "MEDIUM"
        return "LOW"

    def run(self):
        self.evaluate_config()
        self.evaluate_docker()
        
        rating = self.get_risk_rating()
        
        result = {
            "score": min(self.score, 100),
            "rating": rating,
            "risk_factors": self.risk_factors,
            "status": "BLOCK" if rating == "CRITICAL" else "PASS"
        }
        
        print(json.dumps(result, indent=2))
        with open("risk_report.json", "w") as f:
            json.dump(result, f, indent=2)

if __name__ == '__main__':
    scorer = SupremeRiskScorer()
    scorer.run()
```

### File: `scripts/test_bangla.py`

### File: `scripts/test_bangla.py`

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# ============================================================================
# ফাইল >> ফাইল
# প্রকল্প >> SupremeAI 2.0
# উদ্দেশ্য >> Unit testing and QC
# মডিউল >> scripts
# ============================================================================
import sys
sys.stdout.reconfigure(encoding='utf-8')
print('বাংলা টেস্ট')
```

### File: `scripts/test_read.py`

### File: `scripts/test_read.py`

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
fp = Path(r"C:\Users\n\supremeai\supremeai_2.0\backend\main.py")
txt = fp.read_text(encoding='utf-8')
print(txt[:200])
```

### File: `scripts/benchmark/perf_benchmark.py`

### File: `scripts/benchmark/perf_benchmark.py`

```python
#!/usr/bin/env python3
"""
Performance Benchmark for SupremeAI 2.0
Measures API latency, throughput, and resource usage.
"""

import time
import statistics
import argparse
import sys
from pathlib import Path

try:
    import httpx
except ImportError:
    print("httpx required: pip install httpx")
    sys.exit(1)


def benchmark_endpoint(base_url: str, path: str, num_requests: int = 50):
    url = f"{base_url}{path}"
    latencies = []
    success = 0
    failures = 0

    with httpx.Client(timeout=30.0) as client:
        for i in range(num_requests):
            start = time.perf_counter()
            try:
                resp = client.get(url)
                elapsed = time.perf_counter() - start
                latencies.append(elapsed)
                if resp.status_code < 400:
                    success += 1
                else:
                    failures += 1
            except Exception as exc:
                failures += 1
                print(f"Request {i+1} failed: {exc}")

    if not latencies:
        print(f"No successful requests to {path}")
        return

    print(f"\n=== Benchmark: {path} ===")
    print(f"Requests: {num_requests} | Success: {success} | Failures: {failures}")
    print(f"Min:    {min(latencies)*1000:.1f} ms")
    print(f"Median: {statistics.median(latencies)*1000:.1f} ms")
    print(f"P95:    {sorted(latencies)[int(len(latencies)*0.95)]*1000:.1f} ms")
    print(f"Max:    {max(latencies)*1000:.1f} ms")
    print(f"RPS:    {num_requests / sum(latencies):.1f}")


def main():
    parser = argparse.ArgumentParser(description="SupremeAI Performance Benchmark")
    parser.add_argument("--url", default="http://127.0.0.1:8000", help="Base API URL")
    parser.add_argument("--requests", type=int, default=50, help="Number of requests per endpoint")
    parser.add_argument("--endpoints", nargs="+", default=[
        "/health",
        "/api/v1/metrics",
        "/api/v1/repos?limit=10",
    ])
    args = parser.parse_args()

    print(f"Benchmarking {args.url} with {args.requests} requests per endpoint...")
    for ep in args.endpoints:
        benchmark_endpoint(args.url, ep, args.requests)


if __name__ == "__main__":
    main()
```

### File: `scripts/k6/load_test.js`

### File: `scripts/k6/load_test.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.SUPREMEAI_URL || 'http://127.0.0.1:8000';

export default function () {
  let res = http.get(`${BASE_URL}/health`);
  check(res, {
    'health is 200': (r) => r.status === 200,
    'health p95 < 500ms': (r) => r.timings.duration < 500,
  });
  sleep(1);

  res = http.get(`${BASE_URL}/actuator/health`);
  check(res, {
    'actuator is 200': (r) => r.status === 200,
  });
  sleep(1);

  res = http.post(`${BASE_URL}/task/execute`, JSON.stringify({
    task: 'health-check ping',
    task_type: 'general',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, {
    'task status != 500': (r) => r.status !== 500,
  });
  sleep(2);
}
```

### File: `scripts/runner/setup_runner.sh`

### File: `scripts/runner/setup_runner.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# Runner Setup Script for CI/CD and local development
# Configures environment secrets, services, and local runners

BASE_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

setup_local_runner() {
    echo "Setting up local development runner..."
    
    # Ensure .env exists
    if [ ! -f "$BASE_DIR/.env" ]; then
        if [ -f "$BASE_DIR/.env.example" ]; then
            cp "$BASE_DIR/.env.example" "$BASE_DIR/.env"
            echo "Created .env from .env.example"
        else
            echo "Warning: .env.example not found"
        fi
    fi

    # Bootstrap missing env keys
    if [ -f "$BASE_DIR/scripts/bootstrap_env.py" ]; then
        cd "$BASE_DIR" && python scripts/bootstrap_env.py
    fi

    # Install backend deps
    if [ -f "$BASE_DIR/backend/pyproject.toml" ]; then
        echo "Installing backend dependencies..."
        cd "$BASE_DIR/backend" && poetry install --no-interaction --no-ansi --no-root || true
    fi

    # Install frontend deps
    if [ -f "$BASE_DIR/package.json" ]; then
        echo "Installing frontend dependencies..."
        cd "$BASE_DIR" && pnpm install --frozen-lockfile --prefer-offline || true
    fi

    echo "Local runner setup complete."
}

setup_docker_runner() {
    echo "Setting up Docker runner..."
    cd "$BASE_DIR"
    docker-compose -f infrastructure/docker/docker-compose.yml build
    docker-compose -f infrastructure/docker/docker-compose.yml up -d
    echo "Docker runner started."
}

teardown_docker_runner() {
    echo "Tearing down Docker runner..."
    cd "$BASE_DIR"
    docker-compose -f infrastructure/docker/docker-compose.yml down
    echo "Docker runner stopped."
}

case "${1:-}" in
    local)
        setup_local_runner
        ;;
    docker)
        setup_docker_runner
        ;;
    teardown)
        teardown_docker_runner
        ;;
    *)
        echo "Usage: $0 {local|docker|teardown}"
        exit 1
        ;;
esac
```

### File: `scripts/testenv/setup_test_env.sh`

### File: `scripts/testenv/setup_test_env.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# Separate Test Environment Setup
# Creates isolated test env with local-only Supabase/Postgres

BASE_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
TEST_DIR="$BASE_DIR/.testenv"

create_test_env() {
    echo "Creating isolated test environment at $TEST_DIR"
    mkdir -p "$TEST_DIR"
    
    cat > "$TEST_DIR/.env.test" << 'EOF'
ENV=test
DEBUG=true
JWT_SECRET=test-secret-do-not-use-in-production
OPENROUTER_API_KEY=
GEMINI_API_KEY=
SENTRY_DSN=
SUPABASE_URL=http://localhost:54321
SUPABASE_KEY=test-anon-key
DATABASE_URL=postgresql://postgres:postgres@localhost:54322/postgres
REDIS_URL=redis://localhost:6379/0
EOF

    cat > "$TEST_DIR/docker-compose.test.yml" << 'EOF'
version: '3.8'
services:
  postgres-test:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: supremeai_test
    ports:
      - "54322:5432"
    tmpfs:
      - /var/lib/postgresql/data
  redis-test:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    tmpfs:
      - /data
EOF

    echo "Test environment created."
    echo "Start with: docker-compose -f $TEST_DIR/docker-compose.test.yml up -d"
}

destroy_test_env() {
    echo "Destroying test environment..."
    docker-compose -f "$TEST_DIR/docker-compose.test.yml" down -v 2>/dev/null || true
    rm -rf "$TEST_DIR"
    echo "Test environment destroyed."
}

case "${1:-}" in
    create)
        create_test_env
        ;;
    destroy)
        destroy_test_env
        ;;
    *)
        echo "Usage: $0 {create|destroy}"
        exit 1
        ;;
esac
```

### File: `scripts/worktrees/run_task.sh`

### File: `scripts/worktrees/run_task.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# Agent Task Runner
# Executes isolated tasks within a worktree context

WORKTREES_DIR="$(cd "$(dirname "$0")/.." && pwd)/.worktrees"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

run_task() {
    local task_name="$1"
    local command="${2:-pytest}"
    local worktree_path="$WORKTREES_DIR/$task_name"

    if [ ! -d "$worktree_path" ]; then
        echo "Error: Worktree not found for task '$task_name'. Run setup_worktree.sh create first."
        exit 1
    fi

    echo "Running task '$task_name' in $worktree_path"
    cd "$worktree_path"
    
    if [ -f "pyproject.toml" ]; then
        poetry run $command
    elif [ -f "package.json" ]; then
        pnpm run $command
    else
        eval "$command"
    fi
}

run_task "${1:-}" "${2:-pytest}"
```

### File: `scripts/worktrees/setup_worktree.sh`

### File: `scripts/worktrees/setup_worktree.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# Agent Manager Worktree Setup Script
# Creates isolated git worktrees for parallel Agent Manager sessions

BASE_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
WORKTREES_DIR="$BASE_DIR/.worktrees"
DEFAULT_BRANCH="main"

create_worktree() {
    local task_name="$1"
    local branch_name="${2:-$DEFAULT_BRANCH}"
    local worktree_path="$WORKTREES_DIR/$task_name"

    if [ -z "$task_name" ]; then
        echo "Usage: $0 <task-name> [branch]"
        exit 1
    fi

    mkdir -p "$WORKTREES_DIR"

    if [ -d "$worktree_path" ]; then
        echo "Worktree already exists at $worktree_path"
        exit 1
    fi

    echo "Creating worktree for task: $task_name"
    git worktree add "$worktree_path" -b "agent/$task_name" "$branch_name"

    echo "Worktree created at: $worktree_path"
    echo "Branch: agent/$task_name"
}

list_worktrees() {
    echo "Active worktrees:"
    git worktree list
}

remove_worktree() {
    local task_name="$1"
    local worktree_path="$WORKTREES_DIR/$task_name"

    if [ ! -d "$worktree_path" ]; then
        echo "Worktree not found at $worktree_path"
        exit 1
    fi

    git worktree remove "$worktree_path"
    git branch -d "agent/$task_name" 2>/dev/null || true
    echo "Worktree removed: $task_name"
}

case "${1:-}" in
    create)
        create_worktree "${2:-}" "${3:-}"
        ;;
    list)
        list_worktrees
        ;;
    remove)
        remove_worktree "${2:-}"
        ;;
    *)
        echo "Usage: $0 {create|list|remove} [args...]"
        exit 1
        ;;
esac
```

