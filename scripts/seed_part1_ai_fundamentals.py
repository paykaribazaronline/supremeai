#!/usr/bin/env python3
"""
Part 1 — AI Fundamentals
Seeds SupremeAI Firebase with deep knowledge about:
  • Large Language Models (LLMs) — how they work, capabilities, limits
  • Prompt Engineering — few-shot, chain-of-thought, system prompts
  • Retrieval-Augmented Generation (RAG) — architecture, chunking, embeddings
  • Fine-tuning — when to use, LoRA, RLHF, instruction tuning
  • AI agents — tool use, ReAct, planning loops
  • AI safety & hallucination mitigation

Collections written:
  • system_learning        (SystemLearning model records)
  • ai_fundamentals        (rich topic documents)

Run:
  pip install firebase-admin
  python seed_part1_ai_fundamentals.py [--dry-run]
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from seed_lib import _learning, run_part

# ============================================================================
# SYSTEM_LEARNING records  (match SystemLearning.java model)
# ============================================================================

SYSTEM_LEARNINGS = {

    # ── LLM fundamentals ─────────────────────────────────────────────────────

    "llm_transformer_architecture": _learning(
        type_="PATTERN",
        category="AI_FUNDAMENTALS",
        content=(
            "Transformer architecture: attention mechanism lets the model weigh every "
            "token against every other token in context (O(n²) complexity). "
            "GPT-style (decoder-only): generates one token at a time, left-to-right. "
            "BERT-style (encoder-only): bidirectional, best for classification/retrieval. "
            "T5/Flan-style (encoder-decoder): best for seq2seq tasks like translation/summarisation. "
            "Context window = max tokens the model can 'see' at once (GPT-4: 128k, Claude 3: 200k)."
        ),
        solutions=[
            "Choose decoder-only (GPT, Claude, Llama) for chat/completion tasks",
            "Choose encoder-only (BERT, E5, BGE) for embeddings and classification",
            "For long documents: chunk and embed with encoder; generate answer with decoder (RAG)",
            "Monitor context usage — performance degrades near context limit ('lost in the middle' effect)",
        ],
        severity="HIGH",
        confidence=0.97,
        times_applied=120,
        context={
            "models": ["GPT-4o", "Claude 3.5", "Llama 3", "Gemini 1.5 Pro", "Mistral Large"],
            "learned_from": "Attention Is All You Need (Vaswani et al., 2017)",
        },
    ),

    "llm_temperature_sampling": _learning(
        type_="PATTERN",
        category="AI_FUNDAMENTALS",
        content=(
            "Temperature controls randomness: 0 = deterministic (greedy), 1 = default, >1 = creative/chaotic. "
            "Top-p (nucleus sampling): only sample from smallest token set whose cumulative probability ≥ p. "
            "Top-k: only sample from k most-probable tokens. "
            "For code generation: temperature=0 or 0.1 for reproducibility. "
            "For creative writing: temperature=0.8–1.2 with top-p=0.95. "
            "For factual Q&A: temperature=0, top-p=1 to avoid hallucination drift."
        ),
        solutions=[
            "Set temperature=0 when you need deterministic, reproducible code generation",
            "Use temperature=0.7, top-p=0.9 for balanced creative + factual responses",
            "Never set temperature>1.5 in production — responses become incoherent",
            "For structured output (JSON): temperature=0 + response_format={type:'json_object'}",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=88,
        context={"tip": "top-p and top-k are applied AFTER temperature scaling"},
    ),

    "prompt_engineering_system_prompt": _learning(
        type_="PATTERN",
        category="PROMPT_ENGINEERING",
        content=(
            "System prompt is the most powerful lever for LLM behaviour. "
            "Structure: (1) Role definition — 'You are a senior Java engineer…', "
            "(2) Task constraints — 'Only answer questions about Java Spring Boot', "
            "(3) Output format — 'Always respond with valid JSON matching this schema: …', "
            "(4) Tone/style — 'Be concise; use bullet points; no preamble'. "
            "The system prompt is processed before user input, so constraints set here "
            "override most in-context instructions from the user."
        ),
        solutions=[
            "Define persona first: 'You are an expert <role> with 10+ years experience in <domain>'",
            "Specify output format in system prompt to ensure structured, parseable responses",
            "Add 'Think step by step before answering' to system prompt for reasoning tasks",
            "Include negative constraints: 'Do not apologise; do not repeat the question back'",
        ],
        severity="HIGH",
        confidence=0.96,
        times_applied=201,
        context={"copilot_analogy": "Copilot's 'editor context' is effectively its system prompt"},
    ),

    "prompt_engineering_few_shot": _learning(
        type_="PATTERN",
        category="PROMPT_ENGINEERING",
        content=(
            "Few-shot prompting provides 2–8 input/output examples before the actual query. "
            "Shows the model the expected pattern: format, tone, depth, edge-case handling. "
            "Zero-shot: no examples — works for simple tasks with powerful models (GPT-4, Claude 3). "
            "One-shot: 1 example — 40% accuracy improvement on format-sensitive tasks. "
            "Few-shot (3-8 examples): 70–90% accuracy on classification, extraction, transformation. "
            "Keep examples diverse; include at least one edge case to prevent model shortcuts."
        ),
        solutions=[
            "Use 3-5 examples for classification tasks; more examples rarely help beyond 8",
            "Include a wrong-but-plausible example and show why it's wrong (contrast example)",
            "For code generation: show input→output pairs demonstrating your style and conventions",
            "Use chain-of-thought few-shot: show reasoning steps inside each example",
        ],
        severity="MEDIUM",
        confidence=0.93,
        times_applied=145,
        context={
            "research": "Few-Shot Learners (Brown et al., 2020 GPT-3 paper)",
            "warning": "Too many examples can crowd out the actual question in short context windows",
        },
    ),

    "prompt_engineering_chain_of_thought": _learning(
        type_="PATTERN",
        category="PROMPT_ENGINEERING",
        content=(
            "Chain-of-thought (CoT): add 'Let's think step by step' or explicit reasoning steps "
            "to dramatically improve performance on arithmetic, logic, and multi-step reasoning. "
            "Zero-shot CoT: just append 'Think step by step.' — free performance boost. "
            "Few-shot CoT: show reasoning traces in examples. "
            "Self-consistency: sample CoT 5 times, take the majority answer — reduces errors ~30%. "
            "Tree-of-thought: branch multiple reasoning paths and score them — best for planning tasks."
        ),
        solutions=[
            "Append 'Think step by step before giving your final answer' for complex reasoning",
            "For maths/logic: show intermediate steps in your few-shot examples",
            "Use self-consistency for critical decisions: generate N answers, take majority vote",
            "For code debugging: ask model to trace execution line-by-line before suggesting fix",
        ],
        severity="HIGH",
        confidence=0.94,
        times_applied=178,
        context={
            "research": "Chain-of-Thought Prompting (Wei et al., 2022)",
            "benchmark_gain": "CoT improves GSM8K accuracy from 17% to 78% on large models",
        },
    ),

    "rag_architecture": _learning(
        type_="PATTERN",
        category="RAG",
        content=(
            "Retrieval-Augmented Generation (RAG): instead of relying on parametric knowledge "
            "baked into model weights, fetch relevant documents at query time and inject them "
            "into the prompt context. Architecture: "
            "(1) Ingest: chunk docs → embed with encoder → store in vector DB. "
            "(2) Retrieve: embed query → cosine similarity search → top-K chunks. "
            "(3) Generate: build prompt = system + retrieved chunks + user query → LLM → answer. "
            "Eliminates hallucination on proprietary/recent knowledge. "
            "Best vector DBs: Pinecone, Weaviate, Qdrant, pgvector (Postgres extension), ChromaDB."
        ),
        solutions=[
            "Chunk size: 512 tokens for precise retrieval; 1024 tokens for context-rich passages",
            "Chunk overlap 10-20% so sentences spanning chunk boundaries are captured",
            "Use sentence-transformers/all-MiniLM-L6-v2 (fast) or text-embedding-3-large (accurate)",
            "Add metadata filters (date, source, category) to reduce retrieval noise",
            "Use hybrid retrieval: BM25 keyword search + semantic search, re-rank with cross-encoder",
        ],
        severity="CRITICAL",
        confidence=0.96,
        times_applied=87,
        context={
            "implementations": ["LangChain", "LlamaIndex", "Spring AI", "Haystack"],
            "tip": "Retrieved chunks should be < 50% of context window to leave room for the answer",
        },
    ),

    "rag_chunking_strategy": _learning(
        type_="PATTERN",
        category="RAG",
        content=(
            "Chunking strategy critically affects RAG quality. Strategies: "
            "(1) Fixed-size: split every N tokens — fast but breaks sentences. "
            "(2) Sentence: split on sentence boundaries — preserves meaning. "
            "(3) Recursive character: LangChain default — tries paragraphs, then sentences, then chars. "
            "(4) Semantic: embed sentences and split when cosine similarity drops — best quality. "
            "(5) Document-aware: use headers/sections as boundaries (Markdown, HTML) — "
            "preserves document structure. "
            "Always store chunk metadata: source URL, page number, section title, timestamp."
        ),
        solutions=[
            "For code: chunk by function/class boundary, not fixed tokens",
            "For legal/medical docs: chunk by section/clause to preserve context",
            "For FAQs: chunk question+answer together as a single unit",
            "Test retrieval quality with a golden set of 20 queries before deploying",
            "Add chunk title/heading as prefix to every chunk to improve embedding quality",
        ],
        severity="HIGH",
        confidence=0.92,
        times_applied=54,
        context={
            "tools": ["LangChain TextSplitter", "LlamaIndex NodeParser", "Unstructured.io"],
        },
    ),

    "llm_fine_tuning_when": _learning(
        type_="PATTERN",
        category="AI_FINE_TUNING",
        content=(
            "Fine-tuning vs prompting decision guide: "
            "Prompt engineering first — solves 80% of use cases without any training. "
            "Fine-tune when: (1) consistent output format that prompt alone can't enforce, "
            "(2) domain-specific style or vocabulary, (3) latency budget too tight for long prompts, "
            "(4) need to inject thousands of examples into the model. "
            "Do NOT fine-tune to inject new knowledge — use RAG for knowledge, fine-tune for style. "
            "Fine-tuning methods: full fine-tune (expensive), LoRA (cheap, ~1% parameters), "
            "QLoRA (LoRA + 4-bit quantisation — runs on a single A100 GPU)."
        ),
        solutions=[
            "Start with prompt engineering; only fine-tune when prompt quality plateaus",
            "Use LoRA/QLoRA for most fine-tuning — 10x cheaper than full fine-tune",
            "Prepare at least 500–1000 high-quality instruction pairs for effective fine-tuning",
            "Evaluate with held-out test set; watch for catastrophic forgetting on general tasks",
            "Use HuggingFace PEFT library for LoRA; Axolotl for opinionated QLoRA pipelines",
        ],
        severity="HIGH",
        confidence=0.93,
        times_applied=41,
        context={
            "frameworks": ["HuggingFace Transformers", "Axolotl", "LLaMA-Factory", "OpenAI fine-tuning API"],
            "cost_estimate": "LoRA fine-tune of 7B model: ~$10 on 1 A100 for 1000 examples",
        },
    ),

    "llm_hallucination_mitigation": _learning(
        type_="ERROR",
        category="AI_SAFETY",
        content=(
            "LLM hallucination: model confidently generates plausible-but-false information. "
            "Root cause: models are trained to predict tokens, not to be truthful — "
            "they will complete a pattern even when the answer is unknown. "
            "High-risk scenarios: specific dates, names, citations, code APIs, legal/medical facts."
        ),
        solutions=[
            "Ground answers with RAG — retrieved facts prevent fabrication of unknown info",
            "Add to system prompt: 'If you are unsure, say so rather than guessing'",
            "Use structured grounding: provide the exact facts in the prompt, ask model to use them",
            "Post-process with citation verification: ask model to cite the source of each claim",
            "Run self-consistency check: 3 separate calls; flag if answers disagree significantly",
            "For critical facts: require the model to output confidence (LOW/MEDIUM/HIGH) per claim",
        ],
        severity="CRITICAL",
        confidence=0.94,
        error_count=0,
        resolved=True,
        resolution="Combine RAG + self-consistency + citation requirement for production AI systems",
        context={
            "measurement": "Use TruthfulQA benchmark; target >80% truthful answers",
            "tool": "Guardrails AI or NeMo Guardrails for automated hallucination detection",
        },
    ),

    "ai_agents_react_pattern": _learning(
        type_="PATTERN",
        category="AI_AGENTS",
        content=(
            "ReAct (Reason + Act) agent pattern: LLM alternates between reasoning (Thought), "
            "taking an action (Act: call a tool), and observing the result (Observation). "
            "Loop continues until the LLM decides it has the final answer. "
            "Tools: web search, code execution, database queries, API calls, file read/write. "
            "Key benefit: grounds reasoning in real-world observations, dramatically reducing hallucination. "
            "Frameworks: LangChain AgentExecutor, LlamaIndex ReActAgent, AutoGen, CrewAI."
        ),
        solutions=[
            "Define tools with clear docstrings — the model reads them to decide which tool to use",
            "Limit tools to 5-10 per agent — too many tools confuses the selection",
            "Add max_iterations guard (default 10) to prevent infinite loops",
            "Log every Thought/Act/Observation for debugging; use LangSmith or similar tracing",
            "Use structured tool output (JSON) not free text to prevent parse errors",
        ],
        severity="HIGH",
        confidence=0.91,
        times_applied=39,
        context={
            "frameworks": ["LangChain", "LlamaIndex", "AutoGen", "CrewAI", "Spring AI"],
            "research": "ReAct: Synergizing Reasoning and Acting in Language Models (Yao et al., 2022)",
        },
    ),

    "llm_structured_output": _learning(
        type_="PATTERN",
        category="AI_FUNDAMENTALS",
        content=(
            "Getting reliable structured output (JSON/XML/YAML) from LLMs: "
            "(1) OpenAI/Claude: use response_format={type:'json_object'} or tool/function calling. "
            "(2) JSON mode forces the model to output valid JSON — still needs a schema in the prompt. "
            "(3) Function calling / tool use: the safest method — model fills in a typed schema. "
            "(4) Constrained decoding (Outlines, Guidance): grammar-constrained generation — "
            "100% schema compliance, works with any model locally. "
            "(5) Output parsers: LangChain PydanticOutputParser — parse + retry on failure."
        ),
        solutions=[
            "Use OpenAI function calling / Anthropic tool_use for structured extraction",
            "Include the target JSON schema verbatim in the system prompt as a reference",
            "Wrap LLM call with retry logic: if JSON parse fails, re-prompt with the error",
            "Validate with Pydantic model — field-level validation catches type mismatches",
            "For local models: use Outlines or llama.cpp grammar mode for guaranteed JSON",
        ],
        severity="HIGH",
        confidence=0.95,
        times_applied=112,
        context={"library": "Pydantic v2 + instructor library for clean LLM→schema binding"},
    ),

    "embedding_models_selection": _learning(
        type_="PATTERN",
        category="RAG",
        content=(
            "Choosing an embedding model for RAG: "
            "text-embedding-3-large (OpenAI, 3072-dim, best accuracy, $0.13/1M tokens). "
            "text-embedding-3-small (OpenAI, 1536-dim, 5x cheaper, 95% of large's accuracy). "
            "sentence-transformers/all-mpnet-base-v2 (open-source, 768-dim, runs locally). "
            "BAAI/bge-large-en-v1.5 (open-source, SOTA for English retrieval). "
            "multilingual-e5-large (multilingual support, important for global apps). "
            "Always normalise embeddings (L2 norm) before cosine similarity search."
        ),
        solutions=[
            "Start with text-embedding-3-small for cost efficiency; upgrade if recall < 80%",
            "For multilingual content: use multilingual-e5-large or paraphrase-multilingual-mpnet",
            "Run MTEB benchmark on your domain data to pick the best model for your use case",
            "Dimensions can be truncated (MRL): text-embedding-3-large works well at 256 dims",
            "Re-embed periodically when the model is updated to avoid stale index mismatch",
        ],
        severity="HIGH",
        confidence=0.92,
        times_applied=63,
        context={"benchmark": "MTEB leaderboard at huggingface.co/spaces/mteb/leaderboard"},
    ),

    "improvement_llm_cost_optimization": _learning(
        type_="IMPROVEMENT",
        category="AI_COST_MANAGEMENT",
        content=(
            "LLM API cost optimisation strategies: "
            "(1) Cache responses — store (prompt_hash → response) in Redis; "
            "identical prompts hit cache, saving 30–60% of API calls in chatbots. "
            "(2) Prompt compression — remove filler words, compress few-shot examples. "
            "(3) Model routing — route simple queries to cheap models (GPT-3.5, Claude Haiku), "
            "complex queries to powerful models (GPT-4, Claude Opus). "
            "(4) Streaming — reduces perceived latency, not actual cost. "
            "(5) Batching — OpenAI Batch API: 50% discount for async requests."
        ),
        solutions=[
            "Implement semantic caching: embed query, check vector similarity vs cached queries",
            "Use GPT-3.5-turbo or Claude Haiku for classification/triage, GPT-4o for generation",
            "Compress system prompt to < 200 tokens without losing essential context",
            "Enable OpenAI Batch API for non-real-time jobs: 50% cost reduction",
            "Log token usage per endpoint; set budget alerts per project in OpenAI dashboard",
        ],
        severity="MEDIUM",
        confidence=0.90,
        times_applied=44,
        context={"savings_estimate": "Caching + routing together reduce LLM costs 40-70%"},
    ),
}

# ============================================================================
# AI_FUNDAMENTALS  rich topic documents
# ============================================================================

AI_FUNDAMENTALS_DOCS = {

    "llm_overview": {
        "topic": "Large Language Models — Complete Overview",
        "category": "AI_FUNDAMENTALS",
        "description": (
            "LLMs are neural networks trained on trillions of tokens of text to predict the next token. "
            "Through this pretraining they develop emergent capabilities: reasoning, coding, translation, "
            "summarisation, and tool use. The Transformer architecture (attention mechanism) enables "
            "parallel processing and long-range dependency modelling."
        ),
        "key_concepts": {
            "pretraining": "Self-supervised next-token prediction on massive text corpora",
            "instruction_tuning": "Supervised fine-tuning on (instruction, response) pairs to follow instructions",
            "RLHF": "Reinforcement Learning from Human Feedback — aligns model to human preferences",
            "context_window": "Maximum tokens model processes at once; longer = more expensive but more capable",
            "temperature": "Randomness control: 0=deterministic, 1=default, >1=creative/unpredictable",
            "tokens": "Sub-word units; roughly 1 token ≈ 0.75 English words; GPT-4 costs per token",
        },
        "top_models_2024_2025": [
            {"name": "GPT-4o", "provider": "OpenAI", "strengths": "Best all-rounder, vision, fast"},
            {"name": "Claude 3.5 Sonnet", "provider": "Anthropic", "strengths": "Coding, long context, safety"},
            {"name": "Gemini 1.5 Pro", "provider": "Google", "strengths": "1M token context, multimodal"},
            {"name": "Llama 3 70B", "provider": "Meta (open)", "strengths": "Best open-weights, deployable locally"},
            {"name": "Mistral Large", "provider": "Mistral", "strengths": "European, fast, multilingual"},
            {"name": "DeepSeek V3", "provider": "DeepSeek", "strengths": "Coding, cost-efficient, open"},
        ],
        "best_practices": [
            "Always test with your actual data — benchmarks don't predict your use case",
            "Set timeout and retry logic for all LLM API calls",
            "Monitor latency, cost, and quality with observability tools (LangSmith, Helicone)",
            "Never put user PII in prompts without explicit consent and data-processing agreement",
            "Version your prompts like code — breaking changes happen with model updates",
        ],
        "common_mistakes": [
            "Sending entire long documents when only a section is relevant (waste tokens)",
            "Trusting model output without validation in safety-critical contexts",
            "Using high temperature for factual/code tasks causing inconsistent output",
            "Not setting a system prompt — model behaviour is unpredictable without guidance",
            "Ignoring rate limits — production apps need exponential backoff + queue",
        ],
        "confidence": 0.97,
    },

    "prompt_engineering_guide": {
        "topic": "Prompt Engineering — Complete Playbook",
        "category": "PROMPT_ENGINEERING",
        "description": (
            "Prompt engineering is the practice of crafting inputs to LLMs that reliably "
            "produce high-quality outputs. It is the cheapest and fastest way to improve AI "
            "system quality — often eliminating the need for fine-tuning entirely."
        ),
        "techniques": {
            "zero_shot": "No examples — just instruction. Works with GPT-4+, Claude 3+.",
            "few_shot": "2-8 (input, output) example pairs before the actual query.",
            "chain_of_thought": "Ask model to reason step-by-step before final answer.",
            "self_consistency": "Sample N completions, take majority answer.",
            "structured_output": "Specify exact JSON/YAML schema in prompt or use function calling.",
            "react": "Reason-Act-Observe loop for agent tasks with tool use.",
            "tree_of_thought": "Branch multiple reasoning paths, score, backtrack.",
            "meta_prompting": "Ask model to generate the optimal prompt for a task.",
            "role_prompting": "Assign expert persona: 'You are a principal security engineer...'",
        },
        "prompt_template": {
            "system": "You are a {role} with expertise in {domain}. {constraints}. Output: {format}.",
            "user": "{task_description}\n\nContext:\n{context}\n\nRequirements:\n{requirements}",
        },
        "do_not": [
            "Use vague instructions like 'write something about X'",
            "Mix multiple unrelated tasks in one prompt",
            "Forget to specify output format when you need structured data",
            "Rely on prompt alone for safety-critical filtering — add a dedicated classifier",
        ],
        "confidence": 0.96,
    },

    "rag_implementation_guide": {
        "topic": "RAG — Production Implementation Guide",
        "category": "RAG",
        "description": (
            "Retrieval-Augmented Generation augments LLMs with external knowledge retrieved at "
            "query time. It is the standard pattern for building knowledge-grounded AI applications "
            "without the cost and risk of fine-tuning."
        ),
        "architecture_components": {
            "document_loader": "Load from PDF, DOCX, HTML, Markdown, DB — use Unstructured.io",
            "text_splitter": "Break docs into chunks (512-1024 tokens with 10-20% overlap)",
            "embedding_model": "Encode chunks into dense vectors (sentence-transformers, OpenAI Ada)",
            "vector_store": "Index and search vectors (Pinecone, Weaviate, Qdrant, pgvector)",
            "retriever": "Top-K similarity search; optionally hybrid (BM25 + vector) + re-ranker",
            "prompt_builder": "Inject retrieved chunks into LLM prompt with source attribution",
            "llm": "Generate grounded answer using retrieved context",
            "output_parser": "Extract answer + citations from LLM response",
        },
        "advanced_patterns": {
            "HyDE": "Hypothetical Document Embedding — embed a hypothetical answer to find relevant docs",
            "multi_query": "Generate 3 paraphrases of the query; union of retrieved docs; reduces retrieval miss",
            "parent_child_chunking": "Store small chunks for retrieval, return parent chunk to LLM for context",
            "RAPTOR": "Recursively summarise chunk clusters; enables multi-level abstraction retrieval",
            "self_rag": "Model decides when to retrieve (via reflection tokens) instead of retrieving always",
        },
        "evaluation_metrics": {
            "faithfulness": "Answer is grounded in retrieved docs (Ragas faithfulness score)",
            "answer_relevancy": "Answer addresses the question (Ragas answer_relevancy)",
            "context_precision": "Retrieved docs are relevant (Ragas context_precision)",
            "context_recall": "Retrieved docs cover all needed info (Ragas context_recall)",
        },
        "spring_ai_example": (
            "// Spring AI RAG with pgvector\n"
            "@Bean VectorStore vectorStore(EmbeddingModel em, JdbcTemplate jdbc) {\n"
            "    return new PgVectorStore(jdbc, em);\n"
            "}\n"
            "// Query\n"
            "var docs = vectorStore.similaritySearch(SearchRequest.query(userQuery).withTopK(5));\n"
            "var context = docs.stream().map(Document::getContent).collect(joining(\"\\n\"));\n"
            "var prompt = new Prompt(systemPrompt + context + userQuery);\n"
            "return chatClient.call(prompt).getResult().getOutput().getContent();"
        ),
        "confidence": 0.95,
    },

    "fine_tuning_guide": {
        "topic": "LLM Fine-Tuning — When and How",
        "category": "AI_FINE_TUNING",
        "description": (
            "Fine-tuning adapts a pretrained LLM to a specific task or domain by training "
            "on additional (instruction, response) pairs. Should be last resort after "
            "prompt engineering and RAG have been exhausted."
        ),
        "methods": {
            "full_fine_tune": {
                "description": "Update all model weights",
                "cost": "Very high — requires multiple A100 GPUs",
                "use_when": "Domain shift is extreme (medical, legal jargon)",
            },
            "LoRA": {
                "description": "Low-Rank Adaptation — train small adapter matrices (~1% of params)",
                "cost": "Low — single A100 GPU; 1-4 hours for 7B model",
                "use_when": "Style/format adaptation; consistent output structure",
            },
            "QLoRA": {
                "description": "LoRA + 4-bit quantised base model",
                "cost": "Very low — runs on 24GB consumer GPU",
                "use_when": "Budget-constrained fine-tuning on consumer hardware",
            },
            "RLHF": {
                "description": "Reinforcement Learning from Human Feedback",
                "cost": "Very high — requires reward model + PPO training",
                "use_when": "Alignment, safety, human preference optimisation (OpenAI's method)",
            },
            "DPO": {
                "description": "Direct Preference Optimisation — simpler RLHF alternative",
                "cost": "Moderate — no reward model needed",
                "use_when": "Preference-based alignment without RL complexity",
            },
        },
        "dataset_requirements": {
            "minimum": "500 high-quality (instruction, response) pairs",
            "recommended": "2000-10000 pairs for meaningful improvement",
            "format": "JSONL with {instruction, input, output} fields",
            "quality_over_quantity": "100 perfect examples beat 1000 mediocre ones",
        },
        "tools": ["HuggingFace PEFT", "Axolotl", "LLaMA-Factory", "Unsloth (2x faster QLoRA)"],
        "confidence": 0.91,
    },

    "ai_agents_guide": {
        "topic": "AI Agents — Architecture and Implementation",
        "category": "AI_AGENTS",
        "description": (
            "AI agents are LLMs augmented with tools and a control loop. "
            "They can take actions in the real world: browse the web, write/run code, "
            "query databases, call APIs, and manipulate files — autonomously pursuing a goal."
        ),
        "agent_types": {
            "ReAct": "Alternates Reason→Act→Observe; best for tool-using tasks",
            "Plan_and_Execute": "Planner LLM creates plan; executor LLM runs steps — better for long tasks",
            "Reflexion": "After each attempt, reflects on failure and retries — self-improving",
            "multi_agent": "Multiple specialised agents collaborate (CrewAI, AutoGen, LangGraph)",
        },
        "tool_design_best_practices": [
            "Give each tool a clear, single responsibility",
            "Write descriptive docstrings — the LLM reads them to decide which tool to call",
            "Return structured JSON from tools, not free text",
            "Add input validation and graceful error messages in tool implementations",
            "Log every tool call with input, output, and latency for debugging",
        ],
        "production_concerns": {
            "safety": "Add confirmation step before destructive actions (delete, payment)",
            "timeouts": "Set per-tool timeouts; agent loop max_iterations guard",
            "cost": "Agent loops can call LLM 10-50 times per user request — set budget limits",
            "tracing": "Use LangSmith, Arize Phoenix, or Weights&Biases for agent observability",
        },
        "frameworks": {
            "LangChain": "Most mature; large ecosystem; good for standard tool-use agents",
            "LangGraph": "Graph-based agent orchestration; supports cycles and branching",
            "AutoGen": "Multi-agent conversations; good for code-writing agents",
            "CrewAI": "Role-based multi-agent; good for structured workflows",
            "Spring_AI": "Java/Spring Boot native; integrates with existing enterprise Java stack",
        },
        "confidence": 0.90,
    },

    "llm_evaluation_guide": {
        "topic": "LLM Evaluation — Measuring AI Quality",
        "category": "AI_EVALUATION",
        "description": (
            "Systematic evaluation is essential for production AI. "
            "Without measurement, you cannot improve or detect regressions between model versions."
        ),
        "evaluation_types": {
            "automated_benchmarks": "MMLU, HumanEval, GSM8K, TruthfulQA — compare models",
            "domain_specific_evals": "Build 100-500 golden Q&A pairs from your domain",
            "llm_as_judge": "Use GPT-4 to score model outputs on criteria (1-5 scale)",
            "human_eval": "Most accurate but slow/expensive; use for calibration",
            "ragas": "Automated RAG evaluation: faithfulness, relevancy, precision, recall",
        },
        "metrics_to_track": {
            "accuracy": "% correct on golden test set",
            "latency_p50_p95": "Median and tail response time",
            "cost_per_request": "Token usage × model price",
            "hallucination_rate": "% responses with fabricated facts (need fact-checker)",
            "refusal_rate": "% refused when should have answered",
            "format_compliance": "% responses matching expected output schema",
        },
        "regression_testing": (
            "Run eval suite on every: model version change, prompt change, RAG index update. "
            "Alert if accuracy drops >2% or hallucination rate increases."
        ),
        "tools": ["Ragas", "DeepEval", "LangSmith Evaluators", "OpenAI Evals", "Weights&Biases"],
        "confidence": 0.93,
    },
}

# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    run_part(
        part_name="Part 1 — AI Fundamentals",
        collections={
            "system_learning": SYSTEM_LEARNINGS,
            "ai_fundamentals": AI_FUNDAMENTALS_DOCS,
        },
    )
