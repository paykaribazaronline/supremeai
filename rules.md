# SupremeAI Architectural & Code Rules

These rules are mandatory for all future development, refactoring, and code generation within the SupremeAI project. They have been established to prevent "conflicting logic" and ensure the system remains truly dynamic, maintainable, and scalable.

## Rule 1: NO CONFLICTING LOGIC (Zero Exceptions)
If a system or component is designed to be "dynamic," it must be 100% dynamic. Middle-ground compromises (e.g., claiming a dynamic AI gateway but hardcoding 10-12 popular models as a "fallback") are strictly forbidden. Do not write code that contradicts the architectural goals of the project.

## Rule 2: ZERO HARDCODED EXTERNAL ENTITIES
Never hardcode external entity names, provider IDs, model names, endpoints, or pricing tiers in the core source code (Java, JavaScript, TypeScript, etc.).
- **Prohibited:** `if (modelName.contains("gpt-"))`, `switch(provider) { case "openai": ... }`
- **Prohibited Enums:** `enum Provider { OPENAI, GEMINI, CLAUDE }`
- **Reasoning:** Adding 10 hardcoded models when there are thousands available is an anti-pattern. If we cannot hardcode a million models, we should not hardcode ten. 

## Rule 3: 100% METADATA-DRIVEN ABSTRACTIONS
All integrations, routing logic, and cost calculations must be driven by external metadata (e.g., Firestore, SQL Database, or external configuration files like `.yml` / `.json`). 
- Features like UI suggestions, budget tiers, and endpoint URLs must be fetched dynamically at runtime.
- The core system must remain completely agnostic to the specific AI provider being used.

## Rule 4: GENERIC PATTERNS ONLY
Use strategy patterns, interfaces, and generic connectors (e.g., `OpenAICompatibleProvider`) that can accept any base URL, API key, and model name dynamically. The backend architecture should require **ZERO CODE CHANGES** and **ZERO RECOMPILATIONS** when a new AI provider or model is released on the market.

## Rule 5: NO HARDCODED FALLBACKS FOR NETWORK ERRORS
Do not use hardcoded arrays/lists (e.g., `["gemini", "llama", "groq"]`) as fallbacks for database/network failures. If a fallback is necessary, it must be loaded from a local configuration file (`fallback.json` or `application.yml`) that can be modified without changing the compiled code.

## Rule 6: THE RICH MAN & SERVANT ARCHITECTURE (True Solo Mode)
All changes and architectural designs must focus on enabling and maintaining an active "Solo Mode". 
- **The Analogy:** SupremeAI is a "Rich Man". External AI models, APIs, and local models are merely "Servants".
- **The Principle:** A rich man can hire many servants to make his work easier, faster, and more efficient. He can change, hire, or fire servants at any time. However, if he has no servants, *he can still do the basic work himself*. His life does not stop.
- **The Requirement:** SupremeAI must be capable of functioning entirely on its own. No process or system should ever crash, stop, or halt simply because an external AI or API is unavailable or disconnected. The external AIs are only there to *assist* our system, not to *be* the system. SupremeAI must be perfectly self-sufficient.
