#!/usr/bin/env python3
"""
Firebase Collections Setup Script
Initializes 8 collections for SupremeAI teaching system
Run: python firebase_collections_setup.py
"""

import json
import os
from datetime import datetime, timedelta
import uuid

# ============================================================================
# CONFIGURATION
# ============================================================================

FIREBASE_PROJECT_ID = "supremeai-a"
FIRESTORE_DATABASE_ID = "(default)"
CREDENTIALS_FILE = os.getenv("FIREBASE_CREDENTIALS_FILE")

# ============================================================================
# SAMPLE DATA - REAL DATA FROM PROJECT
# ============================================================================

APP_TEMPLATES = {
    "todo_app": {
        "name": "Todo Application",
        "description": "Multi-platform todo with storage",
        "complexity": "MEDIUM",
        "features": ["CRUD operations", "Search/filter", "Persistence", "Multi-theme"],
        "tech_stack": {
            "backend": "Spring Boot 3.2.3",
            "frontend_web": "React 18+",
            "frontend_mobile": "Flutter 3.4+",
            "database": "Firebase",
            "cloud": "Google Cloud Run",
            "auth": "JWT",
        },
        "estimated_time_hours": 2,
        "lines_of_code_estimate": 2500,
        "api_endpoints_count": 6,
        "test_count": 12,
        "tags": ["full-stack", "persistence", "mobile", "web"],
    },
    "chat_app": {
        "name": "Real-time Chat",
        "description": "WebSocket-based messaging platform",
        "complexity": "HIGH",
        "features": [
            "Real-time messaging",
            "User presence",
            "File sharing",
            "Notifications",
        ],
        "tech_stack": {
            "backend": "Spring Boot 3.2.3",
            "frontend_web": "React 18+",
            "frontend_mobile": "Flutter 3.4+",
            "database": "MongoDB",
            "cloud": "Kubernetes",
            "auth": "JWT",
        },
        "estimated_time_hours": 3,
        "lines_of_code_estimate": 2800,
        "api_endpoints_count": 8,
        "test_count": 15,
        "tags": ["real-time", "websocket", "messaging"],
    },
    "ecommerce_app": {
        "name": "E-commerce Store",
        "description": "Complete shopping platform with payment",
        "complexity": "HIGH",
        "features": [
            "Product catalog",
            "Shopping cart",
            "Checkout",
            "Inventory",
            "Payment integration",
        ],
        "tech_stack": {
            "backend": "Spring Boot 3.2.3",
            "frontend_web": "React 18+",
            "frontend_mobile": "Flutter 3.4+",
            "database": "PostgreSQL",
            "cloud": "Google Cloud Run",
            "auth": "OAuth2",
        },
        "estimated_time_hours": 4,
        "lines_of_code_estimate": 3200,
        "api_endpoints_count": 12,
        "test_count": 20,
        "tags": ["ecommerce", "payment", "inventory"],
    },
}

FIX_PROMPT_TEMPLATES = {
    "security": {
        "template": "Analyze the following code for security vulnerabilities, focusing on OWASP Top 10. Code: {{code}}",
        "category": "security",
        "description": "Template for security analysis"
    },
    "quality": {
        "template": "Review the following code for quality, readability, and adherence to best practices. Code: {{code}}",
        "category": "quality",
        "description": "Template for code quality review"
    },
    "dependencies": {
        "template": "Check the following build file for outdated or vulnerable dependencies. File: {{code}}",
        "category": "dependencies",
        "description": "Template for dependency check"
    },
    "architecture": {
        "template": "Analyze the architecture of the following component/module. Code: {{code}}",
        "category": "architecture",
        "description": "Template for architecture analysis"
    },
    "default": {
        "template": "Process the following request: {{code}}",
        "category": "general",
        "description": "Default processing template"
    }
}

SYSTEM_WORK_RULES = {
    "VOTING_CONSENSUS_THRESHOLD": {"value": 0.75, "type": "FLOAT", "description": "Required agreement percentage for Multi-AI voting"},
    "TRIAGE_MODEL_ID": {"value": "gemini-1.5-flash", "type": "STRING", "description": "Primary model used for routing tasks"},
    "FIRECRAWL_API_KEY": {"value": "sk-placeholder", "type": "STRING", "description": "Key for Firecrawl.dev browser scraping"},
    "GROQ_API_KEY": {"value": "gsk-placeholder", "type": "STRING", "description": "Key for high-speed LPU inference"},
    "CLAUDE_MEM_API_KEY": {"value": "sk-placeholder", "type": "STRING", "description": "Key for Claude-mem long-term memory"},
    "MEM0_API_KEY": {"value": "sk-placeholder", "type": "STRING", "description": "Key for personalized memory layer"},
    "FABRIC_PATTERNS_PATH": {"value": "./config/patterns", "type": "STRING", "description": "Local path for logic patterns"},
    "VOTING_ENABLED": {"value": True, "type": "BOOLEAN", "description": "Global switch for multi-agent consensus"},
}

GLOBAL_CONFIGS = {
    "timeouts": {
        "auto_learning_interval_min": 30,
        "browser_scrape_timeout_sec": 45,
        "ai_consensus_timeout_sec": 120
    },
    "security": {
        "jwt_ttl_hours": 24,
        "max_login_attempts": 5,
        "allow_anonymous_queries": False
    }
}

ADMIN_USERS = {
    "admin_initial": {"email": "paykaribazaronline@gmail.com", "role": "ADMIN", "status": "ACTIVE", "created_at": datetime.now().isoformat()}
}

API_PROVIDERS = {
    "claude": {
        "id": "claude",
        "name": "Anthropic Claude",
        "type": "external",
        "status": "active",
        "baseUrl": "https://api.anthropic.com",
        "models": ["claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307"],
        "capabilities": ["chat", "code_generation", "code_review", "debugging", "reasoning"],
        "priority": 1,
        "canCommunicate": True,
        "canExecuteTasks": True,
        "canParticipateInVoting": True,
    },
    "gpt4": {
        "id": "gpt4",
        "name": "OpenAI GPT-4",
        "type": "external",
        "status": "active",
        "baseUrl": "https://api.openai.com/v1",
        "models": ["gpt-4-turbo", "gpt-4-0125-preview"],
        "capabilities": ["chat", "code_generation", "code_review", "debugging", "reasoning"],
        "priority": 1,
        "canCommunicate": True,
        "canExecuteTasks": True,
        "canParticipateInVoting": True,
    },
    "google": {
        "id": "google",
        "name": "Google Gemini",
        "type": "external",
        "status": "active",
        "baseUrl": "https://generativelanguage.googleapis.com",
        "models": ["gemini-1.5-pro", "gemini-1.5-flash"],
        "capabilities": ["chat", "code_generation", "code_review", "debugging", "reasoning"],
        "priority": 1,
        "canCommunicate": True,
        "canExecuteTasks": True,
        "canParticipateInVoting": True,
    },
    "gemini-1.5-flash": {
        "id": "gemini-1.5-flash",
        "name": "Google Gemini 1.5 Flash (Triage)",
        "type": "external",
        "status": "active",
        "baseUrl": "https://generativelanguage.googleapis.com",
        "models": ["gemini-1.5-flash"],
        "capabilities": ["chat", "triage"],
        "priority": 1,
    },
    "openai": {
        "id": "openai",
        "name": "OpenAI general",
        "type": "external",
        "status": "active",
        "baseUrl": "https://api.openai.com/v1",
        "models": ["gpt-3.5-turbo-0125"],
        "capabilities": ["chat", "code_generation", "code_review"],
        "priority": 2,
        "canCommunicate": True,
        "canExecuteTasks": True,
        "canParticipateInVoting": True,
    },
    "mistral": {
        "id": "mistral",
        "name": "Mistral AI",
        "type": "external",
        "status": "active",
        "baseUrl": "https://api.mistral.ai",
        "models": ["mistral-large-latest", "mistral-medium-latest"],
        "capabilities": ["chat", "code_generation", "reasoning"],
        "priority": 3,
    },
    "chickenbrain": {
        "id": "chickenbrain",
        "name": "SupremeAI ChickenBrain",
        "type": "cloud_run",
        "status": "active",
        "baseUrl": "https://supreme-ai-chickenbrain-565236080752.us-central1.run.app",
        "models": ["chickenbrain-8b-instruct"],
        "capabilities": ["chat", "code_generation", "code_review", "debugging", "reasoning"],
        "priority": 1,
        "canCommunicate": True,
        "canExecuteTasks": True,
        "canParticipateInVoting": True,
    },
    "superfly": {
        "id": "superfly",
        "name": "SupremeAI SuperFly Edge",
        "type": "superfly",
        "status": "active",
        "baseUrl": "http://localhost:8082",
        "models": ["superfly-94m"],
        "capabilities": ["chat", "fast_response", "lightweight_reasoning"],
        "priority": 2,
        "canCommunicate": True,
        "canExecuteTasks": False,
        "canParticipateInVoting": False,
    },
    "nomic-embed": {
        "id": "nomic-embed",
        "name": "SupremeAI Nomic Embed",
        "type": "cloud_run",
        "status": "active",
        "baseUrl": "https://supreme-ai-nomic-embed-565236080752.us-central1.run.app",
        "models": ["nomic-embed-text-v1.5"],
        "capabilities": ["embeddings", "similarity_search", "rag"],
        "priority": 3,
        "canCommunicate": False,
        "canExecuteTasks": False,
        "canParticipateInVoting": False,
    },
    "n8n-core": {
        "id": "n8n-core",
        "name": "SupremeAI n8n Automation Core",
        "type": "automation",
        "status": "active",
        "baseUrl": "https://n8n-565236080752.us-central1.run.app",
        "models": ["n8n-workflows-v1"],
        "capabilities": ["workflow_automation", "visual_api", "task_orchestration"],
        "priority": 6,
        "canCommunicate": False,
        "canExecuteTasks": True,
        "canParticipateInVoting": False,
    },
    "voice-hub": {
        "id": "voice-hub",
        "name": "SupremeAI Multimodal Voice Hub",
        "type": "speech",
        "status": "active",
        "baseUrl": "Integrated",
        "models": ["voicebox-tts-v1", "whisper-stt-v1"],
        "capabilities": ["text_to_speech", "speech_to_text", "voice_dialogue"],
        "priority": 7,
        "canCommunicate": True,
        "canExecuteTasks": False,
        "canParticipateInVoting": False,
    }
}

ARCHITECTURES = {
    "full_stack_crud": {
        "scenario": "Todo/List CRUD app with Web + Mobile",
        "consensus": "REST API + Firebase",
        "votes_for": 8,
        "votes_against": 2,
        "confidence": 0.89,
        "deployment": "Google Cloud Run",
        "voting_breakdown": {
            "claude": {"choice": "REST API + Firebase", "confidence": 0.95},
            "gpt4": {"choice": "REST API + Firebase", "confidence": 0.92},
            "mistral": {"choice": "GraphQL + Firebase", "confidence": 0.71},
            "google": {"choice": "REST API + Firestore", "confidence": 0.88},
        },
    },
    "realtime_chat": {
        "scenario": "Real-time chat application",
        "consensus": "WebSocket + MongoDB",
        "votes_for": 7,
        "votes_against": 3,
        "confidence": 0.85,
        "deployment": "Kubernetes",
        "voting_breakdown": {
            "claude": {"choice": "WebSocket + MongoDB", "confidence": 0.92},
            "gpt4": {"choice": "WebSocket + PostgreSQL", "confidence": 0.85},
        },
    },
    "ecommerce_platform": {
        "scenario": "E-commerce with payment processing",
        "consensus": "REST API + PostgreSQL",
        "votes_for": 9,
        "votes_against": 1,
        "confidence": 0.91,
        "deployment": "Kubernetes + Cloud Run",
        "voting_breakdown": {
            "gpt4": {"choice": "REST API + PostgreSQL", "confidence": 0.94},
            "claude": {"choice": "REST API + PostgreSQL", "confidence": 0.91},
        },
    },
}

AI_PERFORMANCE = {
    "task_backend_generation": {
        "task": "Generate Spring Boot CRUD service",
        "best_ai": "claude",
        "ai_stats": {
            "claude": {
                "success": 15,
                "failed": 1,
                "success_rate": 0.94,
                "avg_quality": 0.91,
            },
            "gpt4": {
                "success": 12,
                "failed": 3,
                "success_rate": 0.80,
                "avg_quality": 0.85,
            },
            "mistral": {
                "success": 8,
                "failed": 3,
                "success_rate": 0.73,
                "avg_quality": 0.75,
            },
            "google": {
                "success": 11,
                "failed": 2,
                "success_rate": 0.85,
                "avg_quality": 0.82,
            },
        },
        "recommendation": "Use Claude for backend generation",
    },
    "task_frontend_generation": {
        "task": "Generate React component with hooks",
        "best_ai": "gpt4",
        "ai_stats": {
            "gpt4": {
                "success": 14,
                "failed": 2,
                "success_rate": 0.92,
                "avg_quality": 0.88,
            },
            "claude": {
                "success": 12,
                "failed": 2,
                "success_rate": 0.86,
                "avg_quality": 0.84,
            },
            "mistral": {
                "success": 8,
                "failed": 4,
                "success_rate": 0.67,
                "avg_quality": 0.72,
            },
        },
        "recommendation": "Use GPT-4 for frontend generation",
    },
    "task_mobile_generation": {
        "task": "Generate Flutter screen",
        "best_ai": "claude",
        "ai_stats": {
            "claude": {
                "success": 11,
                "failed": 1,
                "success_rate": 0.92,
                "avg_quality": 0.89,
            },
            "gpt4": {
                "success": 9,
                "failed": 3,
                "success_rate": 0.75,
                "avg_quality": 0.80,
            },
        },
        "recommendation": "Use Claude for Flutter",
    },
    "task_testing_generation": {
        "task": "Generate unit tests",
        "best_ai": "openai",
        "ai_stats": {
            "openai": {
                "success": 13,
                "failed": 2,
                "success_rate": 0.87,
                "avg_quality": 0.86,
            },
            "claude": {
                "success": 11,
                "failed": 2,
                "success_rate": 0.85,
                "avg_quality": 0.84,
            },
        },
        "recommendation": "Use OpenAI for comprehensive tests",
    },
}

PATTERNS = {
    "jwt_authentication": {
        "category": "Authentication",
        "framework": "Spring Boot",
        "description": "JWT token-based authentication",
        "when_to_use": "Any API that needs stateless auth",
        "confidence": 0.97,
        "times_used": 5,
        "pros": ["Stateless", "Scalable", "Mobile-friendly"],
        "cons": ["Token theft risk if no HTTPS"],
    },
    "pagination_rest_api": {
        "category": "API Design",
        "framework": "REST",
        "description": "Offset-based pagination for REST APIs",
        "when_to_use": "APIs returning large lists",
        "confidence": 0.91,
        "times_used": 12,
        "pros": ["Simple to implement", "Works with SQL directly"],
        "cons": ["Poor for real-time data"],
    },
    "error_handling_spring_boot": {
        "category": "Error Management",
        "framework": "Spring Boot",
        "description": "Global exception handler with specific error codes",
        "confidence": 0.95,
        "times_used": 8,
        "pros": ["Centralized error handling", "Consistent responses"],
        "cons": ["Complex setup"],
    },
    "component_composition_react": {
        "category": "React Architecture",
        "framework": "React",
        "description": "Compose small, reusable components",
        "confidence": 0.96,
        "times_used": 15,
        "pros": ["Reusable", "Maintainable", "Testable"],
        "cons": ["More files to manage"],
    },
}

ERROR_FIXES = {
    "entity_annotation_missing": {
        "error_message": "Cannot find symbol: @Entity annotation",
        "cause": "Hibernate JPA not in dependencies",
        "fix": "Add spring-boot-starter-data-jpa to pom.xml",
        "occurrences": 4,
        "confidence": 0.99,
        "ai_that_fixed": "claude",
    },
    "react_hooks_conditional": {
        "error_message": "React hooks called conditionally",
        "cause": "useEffect inside if statement",
        "fix": "Move useEffect outside conditional, use dependency array",
        "occurrences": 5,
        "confidence": 0.96,
        "ai_that_fixed": "gpt4",
    },
    "firebase_credentials": {
        "error_message": "Firebase credentials not found",
        "cause": "GOOGLE_APPLICATION_CREDENTIALS env var not set",
        "fix": "Set environment variable or use Application Default Credentials",
        "occurrences": 2,
        "confidence": 0.99,
        "ai_that_fixed": "google",
    },
    "port_already_in_use": {
        "error_message": "Port 8080 already in use",
        "cause": "Another process using the port",
        "fix": "Change server.port in application.properties",
        "occurrences": 7,
        "confidence": 0.95,
        "ai_that_fixed": "claude",
    },
    "jwt_token_expired": {
        "error_message": "JWT token expired",
        "cause": "Token TTL exceeded",
        "fix": "Implement refresh token endpoint",
        "occurrences": 3,
        "confidence": 0.98,
        "ai_that_fixed": "claude",
    },
    "flutter_dependency_conflict": {
        "error_message": "Flutter dependency conflict",
        "cause": "Incompatible package versions",
        "fix": "Align pubspec.yaml versions to compatible",
        "occurrences": 6,
        "confidence": 0.94,
        "ai_that_fixed": "gpt4",
    },
    "cors_policy_blocking": {
        "error_message": "CORS policy blocking requests",
        "cause": "Cross-origin requests not allowed",
        "fix": "Add @CrossOrigin annotation to controller",
        "occurrences": 8,
        "confidence": 0.97,
        "ai_that_fixed": "claude",
    },
    "node_heap_out_of_memory": {
        "error_message": "Node heap out of memory",
        "cause": "Insufficient memory for Node process",
        "fix": "Increase Node memory: NODE_OPTIONS=--max-old-space-size=4096",
        "occurrences": 2,
        "confidence": 0.92,
        "ai_that_fixed": "gpt4",
    },
}

GENERATED_APPS = {
    "app_20260402_todo_001": {
        "user_plan": "Create a Todo App with React + Flutter + Spring Boot",
        "status": "DEPLOYMENT_COMPLETE",
        "started_at": datetime.now() - timedelta(hours=2),
        "completed_at": datetime.now(),
        "deployment_url": "https://todo-app-xyz1.run.app",
        "lines_of_code": {
            "backend": 850,
            "frontend": 620,
            "mobile": 650,
            "tests": 380,
            "total": 2500,
        },
        "duration_seconds": 117,
        "test_coverage": 0.85,
        "quality_score": 0.92,
        "learnings_recorded": True,
    },
    "app_20260331_chat_001": {
        "user_plan": "Real-time chat application with WebSocket",
        "status": "DEPLOYMENT_COMPLETE",
        "deployment_url": "https://chat-app-xyz2.run.app",
        "lines_of_code": {
            "backend": 900,
            "frontend": 680,
            "mobile": 700,
            "tests": 420,
            "total": 2700,
        },
        "duration_seconds": 128,
        "test_coverage": 0.83,
        "quality_score": 0.90,
        "learnings_recorded": True,
    },
    "app_20260329_store_001": {
        "user_plan": "E-commerce store with payment integration",
        "status": "DEPLOYMENT_COMPLETE",
        "deployment_url": "https://store-app-xyz3.run.app",
        "lines_of_code": {
            "backend": 1200,
            "frontend": 850,
            "mobile": 800,
            "tests": 500,
            "total": 3350,
        },
        "duration_seconds": 145,
        "test_coverage": 0.86,
        "quality_score": 0.93,
        "learnings_recorded": True,
    },
}

DEPLOYMENT_CONFIGS = {
    "cloud_run_spring_boot": {
        "platform": "Google Cloud Run",
        "container_image": "gcr.io/PROJECT/app:latest",
        "port": 8080,
        "memory": "512Mi",
        "timeout": "300s",
        "auto_scaling_min": 0,
        "auto_scaling_max": 100,
        "deployment_time": "5 minutes",
        "monthly_cost": "$10-50",
    },
    "kubernetes_full_stack": {
        "platform": "Kubernetes",
        "components": [
            {"name": "backend", "replicas": 2},
            {"name": "frontend", "replicas": 3},
        ],
        "deployment_time": "15 minutes",
        "monthly_cost": "$50-200",
    },
    "firebase_hosting": {
        "platform": "Firebase Hosting",
        "deployment_time": "2 minutes",
        "monthly_cost": "$5-15",
    },
}

# ============================================================================
# SETUP INSTRUCTIONS
# ============================================================================

SETUP_INSTRUCTIONS = """
╔══════════════════════════════════════════════════════════════════════════════╗
║                    FIREBASE COLLECTIONS SETUP                               ║
║                                                                              ║
║  This script will create 8 collections with real sample data                ║
║  Collections:                                                               ║
║   1. app_templates (3 entries)                                             ║
║   2. architectures (3 entries)                                             ║
║   3. ai_performance_by_task (4 entries)                                    ║
║   4. patterns (4 entries)                                                  ║
║   5. generation_errors_and_fixes (8 entries)                               ║
║   6. generated_apps (3 entries)                                            ║
║   7. deployment_configs (3 entries)                                        ║
║   8. system_work_rules (8 rules)                                           ║
║   9. system_configs (global_settings)                                      ║
║   10. users (initial admin)                                               ║
║   11. fix_prompt_templates (5 entries)                                     ║
║   12. api_providers (11 entries)                                           ║
║   8. code_generators (placeholder for templates)                           ║
╚══════════════════════════════════════════════════════════════════════════════╝

Step 1: Install Firebase Admin SDK
  pip install firebase-admin

Step 2: Setup Credentials
    - Recommended: run gcloud auth application-default login
    - Optional: set FIREBASE_CREDENTIALS_FILE=./secrets/service-account.json
    - Or set GOOGLE_APPLICATION_CREDENTIALS env var

Step 3: Run This Script
  python firebase_collections_setup.py

Step 4: Verify
  - Check Firebase Console
  - View collections created
  - Verify data in Firestore

═══════════════════════════════════════════════════════════════════════════════
"""

# ============================================================================
# COLLECTION CREATION
# ============================================================================


def create_firebase_collections():
    """Create all 8 Firebase collections with real data"""
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
    except ImportError:
        print("❌ Firebase Admin SDK not installed!")
        print("Install with: pip install firebase-admin")
        return False

    print(SETUP_INSTRUCTIONS)

    try:
        if not firebase_admin.app._apps:
            if CREDENTIALS_FILE and os.path.exists(CREDENTIALS_FILE):
                cred = credentials.Certificate(CREDENTIALS_FILE)
                firebase_admin.initialize_app(cred)
                print(
                    f"✅ Initialized Firebase using credentials file: {CREDENTIALS_FILE}"
                )
            else:
                firebase_admin.initialize_app(credentials.ApplicationDefault())
                print(
                    "✅ Initialized Firebase using Application Default Credentials (ADC)"
                )

        db = firestore.client()
        print("✅ Connected to Firebase!")

        # 1. Create app_templates collection
        print("\n📁 Creating app_templates collection...")
        for template_id, template_data in APP_TEMPLATES.items():
            db.collection("app_templates").document(template_id).set(template_data)
        print(f"   ✅ Added {len(APP_TEMPLATES)} templates")

        # 2. Create architectures collection
        print("\n📁 Creating architectures collection...")
        for arch_id, arch_data in ARCHITECTURES.items():
            db.collection("architectures").document(arch_id).set(arch_data)
        print(f"   ✅ Added {len(ARCHITECTURES)} architectures")

        # 3. Create ai_performance_by_task collection
        print("\n📁 Creating ai_performance_by_task collection...")
        for perf_id, perf_data in AI_PERFORMANCE.items():
            db.collection("ai_performance_by_task").document(perf_id).set(perf_data)
        print(f"   ✅ Added {len(AI_PERFORMANCE)} performance records")

        # 4. Create patterns collection
        print("\n📁 Creating patterns collection...")
        for pattern_id, pattern_data in PATTERNS.items():
            db.collection("patterns").document(pattern_id).set(pattern_data)
        print(f"   ✅ Added {len(PATTERNS)} patterns")

        # 5. Create generation_errors_and_fixes collection
        print("\n📁 Creating generation_errors_and_fixes collection...")
        for error_id, error_data in ERROR_FIXES.items():
            db.collection("generation_errors_and_fixes").document(error_id).set(
                error_data
            )
        print(f"   ✅ Added {len(ERROR_FIXES)} error fixes")

        # 6. Create generated_apps collection
        print("\n📁 Creating generated_apps collection...")
        for app_id, app_data in GENERATED_APPS.items():
            # Convert datetime to ISO format for Firestore
            if "started_at" in app_data:
                app_data["started_at"] = app_data["started_at"].isoformat()
            if "completed_at" in app_data:
                app_data["completed_at"] = app_data["completed_at"].isoformat()
            db.collection("generated_apps").document(app_id).set(app_data)
        print(f"   ✅ Added {len(GENERATED_APPS)} generated apps")

        # 7. Create deployment_configs collection
        print("\n📁 Creating deployment_configs collection...")
        for config_id, config_data in DEPLOYMENT_CONFIGS.items():
            db.collection("deployment_configs").document(config_id).set(config_data)
        print(f"   ✅ Added {len(DEPLOYMENT_CONFIGS)} deployment configs")

        # 7.1 Create system_work_rules collection
        print("\n📁 Creating system_work_rules collection...")
        for rule_id, rule_data in SYSTEM_WORK_RULES.items():
            db.collection("system_work_rules").document(rule_id).set(rule_data)
        print(f"   ✅ Added {len(SYSTEM_WORK_RULES)} system work rules")

        # 7.2 Create system_configs collection
        print("\n📁 Creating system_configs collection...")
        db.collection("system_configs").document("global_settings").set(GLOBAL_CONFIGS)
        print(f"   ✅ Added global_settings document")

        # 7.3 Create users collection
        print("\n📁 Creating users collection...")
        for user_id, user_data in ADMIN_USERS.items():
            db.collection("users").document(user_id).set(user_data)
        print(f"   ✅ Added initial admin user")

        # 7.4 Create fix_prompt_templates collection
        print("\n📁 Creating fix_prompt_templates collection...")
        for template_id, template_data in FIX_PROMPT_TEMPLATES.items():
            db.collection("fix_prompt_templates").document(template_id).set(template_data)
        print(f"   ✅ Added {len(FIX_PROMPT_TEMPLATES)} fix prompt templates")

        # 7.5 Create api_providers collection
        print("\n📁 Creating api_providers collection...")
        for provider_id, provider_data in API_PROVIDERS.items():
            db.collection("api_providers").document(provider_id).set(provider_data)
        print(f"   ✅ Added {len(API_PROVIDERS)} API providers")

        # 8. Create code_generators collection placeholder
        print("\n📁 Creating code_generators collection...")
        placeholder = {
            "_info": "Code generator templates will be added here",
            "spring_boot_crud_model": "JPA Entity template",
            "react_functional_component": "Hooks-based component template",
            "flutter_stateful_screen": "State management screen template",
        }
        db.collection("code_generators").document("_templates").set(placeholder)
        print(f"   ✅ Added code_generators placeholder")

        print("\n" + "=" * 80)
        print("✅ ✅ ✅ ALL COLLECTIONS CREATED SUCCESSFULLY! ✅ ✅ ✅")
        print("=" * 80)
        print("\n📊 COLLECTIONS SUMMARY:")
        print(f"   • app_templates: {len(APP_TEMPLATES)} entries")
        print(f"   • architectures: {len(ARCHITECTURES)} entries")
        print(f"   • ai_performance_by_task: {len(AI_PERFORMANCE)} entries")
        print(f"   • patterns: {len(PATTERNS)} entries")
        print(f"   • generation_errors_and_fixes: {len(ERROR_FIXES)} entries")
        print(f"   • generated_apps: {len(GENERATED_APPS)} entries")
        print(f"   • deployment_configs: {len(DEPLOYMENT_CONFIGS)} entries")
        print(f"   • system_work_rules: {len(SYSTEM_WORK_RULES)} entries")
        print(f"   • system_configs: 1 entry (global_settings)")
        print(f"   • users: {len(ADMIN_USERS)} entries")
        print(f"   • fix_prompt_templates: {len(FIX_PROMPT_TEMPLATES)} entries")
        print(f"   • api_providers: {len(API_PROVIDERS)} entries")
        print(f"   • code_generators: 1 placeholder")
        print("\n🔗 View in Firebase Console:")
        print(f"   https://console.firebase.google.com/project/{FIREBASE_PROJECT_ID}")

        return True

    except FileNotFoundError:
        print(f"❌ Credentials file not found: {CREDENTIALS_FILE}")
        print(
            "Set FIREBASE_CREDENTIALS_FILE to a valid service-account JSON or use ADC"
        )
        return False
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return False


# ============================================================================
# MAIN
# ============================================================================

if __name__ == "__main__":
    print("\n🚀 Starting Firebase Collections Setup...\n")
    success = create_firebase_collections()
    exit(0 if success else 1)
