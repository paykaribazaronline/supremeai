import json
import os

new_entries = [
    # Category: AI Provider Management
    {
        "task": "add new ai provider custom api endpoint registry register add provider manual",
        "solution": "To add a new AI provider, navigate to Admin Panel > AI Providers > Add Provider. Fill in name, type (e.g., OPENAI_COMPAT, ANTHROPIC, GEMINI), base URL, and API key. The system will save this in Firestore 'api_providers' and automatically test connectivity.",
        "category": "AI_PROVIDER_MGMT",
        "confidence": 0.95,
        "tags": ["ai-provider", "add-provider", "configure", "manual"],
    },
    {
        "task": "rotate api keys credentials vault rotation revoke key safe rotation security settings",
        "solution": "Rotate credentials periodically by generating a new key, updating the active provider config in Admin Panel > Settings > API Keys, verifying health, and then deprecating the old key. Never expose raw keys in logs or files.",
        "category": "AI_PROVIDER_MGMT",
        "confidence": 0.95,
        "tags": ["api-key", "rotation", "security", "credential"],
    },
    {
        "task": "quarantine provider cooldown circuit breaker reset manual quarantine quarantine release active",
        "solution": "Providers showing consecutive failures are quarantined automatically. To manually quarantine, go to AI Providers > Select Provider > Quarantine. To release, select Release or POST /api/admin/providers/{id}/release. Cooldown lasts 10 minutes.",
        "category": "AI_PROVIDER_MGMT",
        "confidence": 0.95,
        "tags": ["quarantine", "cooldown", "circuit-breaker", "release"],
    },
    {
        "task": "provider cost limit throttle consumption daily quota alert budget threshold limit",
        "solution": "Configure daily cost limits for each provider in Firestore configurations. If a provider's cost exceeds the limit, it is automatically throttled (status=throttled), and request routing falls back to the next cheapest provider.",
        "category": "AI_PROVIDER_MGMT",
        "confidence": 0.95,
        "tags": ["cost-limit", "budget", "throttle", "quota"],
    },
    {
        "task": "provider discovery discovery service register automatically auto find scan cheap free models",
        "solution": "The AutoProviderDiscoveryService runs every 24 hours. It queries OpenRouter/HuggingFace API, filters models with benchmark scores > 0.7, and automatically registers them in the Firestore api_providers collection to keep endpoints fresh without manual code updates.",
        "category": "AI_PROVIDER_MGMT",
        "confidence": 0.95,
        "tags": ["provider-discovery", "auto-find", "huggingface", "openrouter"],
    },
    # Category: User/Permission Management
    {
        "task": "assign role user permission authorization level security admin developer viewer user role",
        "solution": "Update user permissions by going to Admin Panel > Users, select the user, and assign roles: ADMIN (full access), DEVELOPER (healing, metrics, code access), VIEWER (read-only), or TESTER (trigger workflows).",
        "category": "USER_PERM_MGMT",
        "confidence": 0.95,
        "tags": ["user-management", "roles", "permissions", "access-control"],
    },
    {
        "task": "create admin account seed bootstrap admin default login credentials admin username password",
        "solution": "During the initial cold-start, the system seeds a default admin account from the environment variables ADMIN_USERNAME and ADMIN_PASSWORD. If these are empty, default credentials are safe-seeded and printed to the startup console.",
        "category": "USER_PERM_MGMT",
        "confidence": 0.95,
        "tags": ["admin-claims", "bootstrap", "credentials", "seed"],
    },
    {
        "task": "disable suspended user account lock unauthorized access revoke session toggle active",
        "solution": "To suspend a user, toggle isActive=false in their user document via the Admin Panel or Firestore. Once deactivated, the user's active JWT is revoked, and all future authenticated requests are blocked.",
        "category": "USER_PERM_MGMT",
        "confidence": 0.95,
        "tags": ["revoke-access", "disable-account", "suspend-user"],
    },
    {
        "task": "jwt token secret signature mismatch verification failed token expired auth validation",
        "solution": "If JWT verification fails (signature mismatch or expiration), check that the JWT_SECRET is consistent across system instances and check that the client's system clock is synchronized. Expiration defaults to 24 hours.",
        "category": "USER_PERM_MGMT",
        "confidence": 0.95,
        "tags": ["security", "auth", "jwt", "expiration"],
    },
    {
        "task": "rbac role based access control webflux spring security rules enforcement scope role",
        "solution": "Access control is enforced at the WebFlux filter layer. The security config checks for scopes on JWT claims (e.g., .hasAuthority('SCOPE_ROLE_ADMIN') or .hasAuthority('SCOPE_ROLE_DEVELOPER')) before forwarding requests to sensitive controllers.",
        "category": "USER_PERM_MGMT",
        "confidence": 0.95,
        "tags": ["rbac", "webflux", "security-rules", "enforcement"],
    },
    # Category: Zero-AI Offline Operation
    {
        "task": "zero ai offline operations offline query core seed resilience offline mode total resilience",
        "solution": "When all external AI helper connections are down, the system enters TIER 3 (Emergency Offline Mode). In this tier, queries are routed exclusively to core_knowledge.json and local seed stores, bypassing network calls.",
        "category": "ZERO_AI_OFFLINE",
        "confidence": 0.95,
        "tags": ["zero-ai", "offline-mode", "resilience", "blackout"],
    },
    {
        "task": "offline fallback path complete blackout resilience sequence layer caffeine cache",
        "solution": "The system resilience sequence when offline: 1. In-memory Caffeine cache (TTL 5m) -> 2. Firestore Solution Memory (if cached) -> 3. Local core_knowledge.json (this seed) -> 4. Static emergency fallback prompt response.",
        "category": "ZERO_AI_OFFLINE",
        "confidence": 0.95,
        "tags": ["fallback-path", "resilience", "caffeine-cache"],
    },
    {
        "task": "local knowledge indexing search offline keyword match scoring jaccard similarity",
        "solution": "Offline queries are matched against core_knowledge.json tasks using tokenized Jaccard similarity and keyword matching. The solution with the highest similarity score above the gate (0.55) is chosen.",
        "category": "ZERO_AI_OFFLINE",
        "confidence": 0.95,
        "tags": ["indexing", "keyword-match", "jaccard-similarity"],
    },
    {
        "task": "cache hits local cache offline caffeine instant lookup sub millisecond",
        "solution": "Caffeine cache is enabled for offline operations. Duplicate tasks bypass all lookup routines, yielding instant (sub-millisecond) response times, and are logged under activity logs as isCacheHit=true.",
        "category": "ZERO_AI_OFFLINE",
        "confidence": 0.95,
        "tags": ["cache-hits", "instant-lookup", "caffeine"],
    },
    {
        "task": "unsupported query in offline mode zero ai limit grace fallback local knowledge",
        "solution": "If a query has no matching keywords or solutions in the local seed, the system gracefully responds with: '[OFFLINE] This feature is currently limited to local knowledge. Full AI features will resume when online connectivity is restored.'",
        "category": "ZERO_AI_OFFLINE",
        "confidence": 0.95,
        "tags": ["unsupported-query", "grace-fallback", "offline-limit"],
    },
    # Category: AirLLM Sidecar Setup Guide
    {
        "task": "airllm sidecar setup configure endpoint local server docker run launch start airllm",
        "solution": "To set up the AirLLM sidecar for solo offline intelligence: 1. Deploy the AirLLM Docker container using: docker run -d --name airllm -p 8085:8085 airllm/runtime:latest. 2. Configure AIRLLM_ENDPOINT=http://localhost:8085.",
        "category": "AIRLLM_SETUP",
        "confidence": 0.95,
        "tags": ["airllm-setup", "docker-run", "endpoint", "sidecar"],
    },
    {
        "task": "airllm hardware requirements memory gpu cuda vram requirements metal apple silicon",
        "solution": "AirLLM requires at least 16 GB of RAM for 7B models. When using a GPU, ensure CUDA 11.8+ is installed with at least 8 GB of VRAM for 4-bit quantized GGUF/AWQ models. On Apple Silicon, Metal acceleration is auto-detected.",
        "category": "AIRLLM_SETUP",
        "confidence": 0.95,
        "tags": ["hardware-requirements", "gpu-cuda", "vram", "metal"],
    },
    {
        "task": "airllm fallback offline code generation emergency fallback model solo mode manager",
        "solution": "If cloud AI goes dark, the SoloModeManagerService routes execution-tier prompts to the local AirLLM endpoint. If AirLLM is also down, the system gracefully degrades to DOM text-only mode and local knowledge templates.",
        "category": "AIRLLM_SETUP",
        "confidence": 0.95,
        "tags": ["solo-mode", "code-generation", "fallback-model"],
    },
    {
        "task": "airllm model loading memory compression quantization options gguf awq",
        "solution": "AirLLM loads large models dynamically. Ensure you run with --quantize 4bit to reduce RAM usage from 14 GB to 4.5 GB. Use Q4_K_M or Q5_K_M GGUF formats to balance inference speed and accuracy on consumer hardware.",
        "category": "AIRLLM_SETUP",
        "confidence": 0.95,
        "tags": ["model-loading", "quantization", "gguf", "compression"],
    },
    {
        "task": "airllm troubleshooting connections timeout container crash loop docker logs bound",
        "solution": "If AirLLM container crashes: 1. Check docker logs airllm. 2. Verify port 8085 is not bound by another service. 3. Ensure --gpus all flag is passed if using NVIDIA. 4. If connection timeouts occur, increase solo.fallback.timeout to 30s.",
        "category": "AIRLLM_SETUP",
        "confidence": 0.95,
        "tags": ["troubleshoot", "container-crash", "timeouts", "port-bound"],
    },
    # Category: P2P Sync Scenarios
    {
        "task": "p2p sync multi instance lan multicast peer discovery auto discovery mdns enable",
        "solution": "P2P Sync allows LAN instances of SupremeAI to discover each other automatically via mDNS/multicast when offline. Ensure supremeai.p2p.enabled=true and supremeai.p2p.multicast-group=239.255.42.99 are configured.",
        "category": "P2P_SYNC",
        "confidence": 0.95,
        "tags": ["p2p-sync", "peer-discovery", "lan-sync", "multicast"],
    },
    {
        "task": "p2p conflict resolution duplicate keys different solutions merge strategy tied score",
        "solution": "When P2P peers exchange divergent solutions for the same task, the conflict is resolved by: 1. Keeping the solution with the highest confidence score. 2. Merging provenances. 3. If tied, keeping the user-verified version.",
        "category": "P2P_SYNC",
        "confidence": 0.95,
        "tags": ["conflict-resolution", "merge-strategy", "tied-score"],
    },
    {
        "task": "p2p sync bandwidth throttle network type compression zstd cellular wifi wan lan",
        "solution": "P2P sync throttles bandwidth based on network interfaces: 110 Mb/s for LAN, 25 Mb/s for WAN, and 5 Mb/s for cellular. Payloads over 4 MB are compressed using zstd and divided into 4 MB chunks.",
        "category": "P2P_SYNC",
        "confidence": 0.95,
        "tags": ["bandwidth-throttle", "compression", "zstd", "chunks"],
    },
    {
        "task": "p2p sync complete peer disconnect chunked transfer queue resume chunk queue",
        "solution": "If a peer disconnects during knowledge sync, the transfer pauses. On reconnect, sync resumes from the last acknowledged chunk index using the retry-queue, preventing duplicate network overhead.",
        "category": "P2P_SYNC",
        "confidence": 0.95,
        "tags": ["peer-disconnect", "resume-sync", "chunk-queue"],
    },
    {
        "task": "p2p sync cloud reconnect upload firestore system_learning sync pull gkb",
        "solution": "Upon reconnecting to the cloud, any knowledge acquired offline via P2P is uploaded to the Firestore system_learning collection with the tag learnedFrom=P2P_SYNC. All peers then pull the normalized GKB.",
        "category": "P2P_SYNC",
        "confidence": 0.95,
        "tags": ["cloud-reconnect", "upload", "system_learning"],
    },
]


def expand_file(path):
    if not os.path.exists(path):
        print(f"File {path} does not exist!")
        return False

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Handle both format types: direct list or dict with key "entries"
    if isinstance(data, dict) and "entries" in data:
        target_list = data["entries"]
    elif isinstance(data, list):
        target_list = data
    else:
        print(f"Unknown format in {path}")
        return False

    existing_tasks = {item["task"] for item in target_list if "task" in item}
    added = 0
    for entry in new_entries:
        if entry["task"] not in existing_tasks:
            # For direct list format, filter out tags/confidence if not in the other format?
            # Wait, let's keep all fields since they are safe and compatible.
            target_list.append(entry)
            added += 1

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print(f"Successfully appended {added} new entries to {path}!")
    return True


expand_file("src/main/resources/core_knowledge.json")
expand_file("core_knowledge.json")
