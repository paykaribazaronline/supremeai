import asyncio
import contextlib
import datetime
import json
import re
# বাংলা মন্তব্য: টাইপিং এরর এড়ানোর জন্য Any ইমপোর্ট করা হলো
from typing import Any

import anyio
from fastapi import APIRouter
from fastapi import BackgroundTasks
from fastapi import HTTPException
from fastapi.responses import JSONResponse
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

# --- Local Imports ---
# Moved imports to the top of the file to improve performance by avoiding repeated imports inside functions.
from adaptive_engine.experience_db import Experience
from core.intent_router import intent_router, PromptAction
from core.prompt_helpers import format_unified_chat_prompt


router = APIRouter()

try:
    from core.semantic_cache import VectorSemanticCache

    semantic_cache = VectorSemanticCache()
except ImportError:
    semantic_cache = None


# --- Agentic Security & Context: Task Request Schema ---
# Added messages and session_id parameters on 2026-06-21 to prevent context loss.
class TaskRequest(BaseModel):
    task: str
    task_type: str = "general"
    max_cost: float = 0.01
    admin_token: str | None = None
    schema_name: str | None = None
    messages: list[dict] | None = None
    session_id: str | None = None


class TaskResponse(BaseModel):
    success: bool
    result: str | None = None
    provider: str | None = None
    cost: float | None = None
    error: str | None = None
    action: dict[str, Any] | None = None
    intent: dict[str, Any] | None = None


class CompletionRequest(BaseModel):
    prefix: str
    suffix: str
    filePath: str
    language: str
    sessionId: str | None = None


class CompletionResponse(BaseModel):
    success: bool
    suggestions: list[str]


class ChatStreamRequest(BaseModel):
    message: str
    sessionId: str | None = None
    messages: list[dict] | None = None
    context: dict | None = None


class ActionStreamRequest(BaseModel):
    message: str
    messages: list[dict] | None = None


def _build_completion_prompt(prefix: str, suffix: str) -> str:
    return (
        f"You are a code completion assistant. Your task is to provide the code that fits between the prefix and suffix.\n"
        f"Do NOT wrap the response in code blocks, markdown, or explain it. Return ONLY the code to be inserted.\n\n"
        f"--- PREFIX ---\n{prefix}\n"
        f"--- SUFFIX ---\n{suffix}\n"
        f"--- COMPLETION ---"
    )


def _build_chat_prompt(req: ChatStreamRequest) -> str:
    context = req.context or {}
    parts = [f"User: {req.message}"]
    if context.get("codeSnippet"):
        parts.append(f"Code snippet:\n{context['codeSnippet']}")
    if context.get("filePath"):
        parts.append(f"File: {context['filePath']}")
    if context.get("language"):
        parts.append(f"Language: {context['language']}")
    parts.append("Assistant:")
    return "\n".join(parts)


@router.post("/api/chat/completion", response_model=CompletionResponse)
async def get_completion(req: CompletionRequest):
    import core.services as app_mod

    model_router = app_mod.model_router

    prompt = _build_completion_prompt(req.prefix, req.suffix)

    raw = await model_router.async_route_and_generate(
        prompt=prompt,
        task_type="completion",
        max_cost=0.005,
    )

    completion_text = raw.get("text", "")
    if completion_text.strip().startswith("```"):
        lines = completion_text.strip().splitlines()
        if len(lines) > 1:
            if lines[0].startswith("```"):
                lines = lines[1:]
            if lines and lines[-1].startswith("```"):
                lines = lines[:-1]
            completion_text = "\n".join(lines)

    suggestions = [completion_text] if completion_text else []
    return CompletionResponse(success=True, suggestions=suggestions)


@router.post("/api/chat/stream")
async def stream_chat(req: ChatStreamRequest):
    import core.services as app_mod

    model_router = app_mod.model_router
    prompt = _build_chat_prompt(req)

    async def event_generator():
        async for chunk in model_router.async_route_and_stream(
            prompt=prompt,
            task_type="general",
            max_cost=0.01,
        ):
            token = chunk.decode("utf-8") if isinstance(chunk, bytes) else str(chunk)
            yield f"data: {json.dumps({'token': token})}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


class ProblemDetailsResponse(JSONResponse):
    def __init__(
        self,
        title: str,
        status: int,
        detail: str,
        type_url: str = "about:blank",
        instance: str | None = None,
        **kwargs,
    ):
        content = {
            "type": type_url,
            "title": title,
            "status": status,
            "detail": detail,
            "instance": instance or "",
        }
        content.update(kwargs)
        super().__init__(
            status_code=status, content=content, media_type="application/problem+json"
        )


# --- Action Cards Helpers ---
# Formats output as structured action card JSON.
def format_chat_history(messages: list[dict]) -> str:
    lines = []
    for msg in messages:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        if isinstance(content, str) and content.strip().startswith("{"):
            try:
                data = json.loads(content)
                if isinstance(data, dict) and "content" in data:
                    content = data["content"]
            except Exception:
                pass
        role_label = "User" if role == "user" else "Assistant"
        lines.append(f"{role_label}: {content}")
    return "\n".join(lines)


def format_response(text: str, task_type: str) -> str:
    # Extracts code blocks to clean markup
    def extract_code(t: str) -> str:
        if "```" in t:
            parts = t.split("```")
            for part in parts[1:]:
                lines = part.splitlines()
                if len(lines) > 0:
                    return "\n".join(lines[1:])
        return t

    def detect_language(t: str) -> str:
        if "```" in t:
            lang = t.split("```")[1].splitlines()[0].strip()
            if lang:
                return lang
        return "javascript"

    if "```" in text or task_type in ["code", "completion"]:
        return json.dumps(
            {
                "type": "code",
                "content": extract_code(text),
                "metadata": {
                    "language": detect_language(text),
                    "filename": (
                        "index.html"
                        if "html" in detect_language(text)
                        else "component.tsx"
                    ),
                    "actions": [
                        {"id": "preview", "label": "👁️ Preview", "type": "preview"},
                        {"id": "save", "label": "💾 Save to Project", "type": "save"},
                        {"id": "run", "label": "▶️ Run Code", "type": "run"},
                        {"id": "deploy", "label": "🚀 Deploy", "type": "deploy"},
                    ],
                },
            },
            ensure_ascii=False,
        )

    if task_type == "image" or "generate image" in text.lower():
        urls = re.findall(
            r"http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+",
            text,
        )
        image_url = urls[0] if urls else text
        return json.dumps(
            {
                "type": "image",
                "content": image_url,
                "metadata": {
                    "actions": [
                        {"id": "download", "label": "⬇️ Download", "type": "save"},
                        {"id": "share", "label": "🔗 Share", "type": "share"},
                    ]
                },
            },
            ensure_ascii=False,
        )

    return json.dumps(
        {
            "type": "text",
            "content": text,
            "metadata": {
                "actions": [
                    {"id": "copy", "label": "📋 Copy", "type": "copy"},
                    {"id": "share", "label": "🔗 Share", "type": "share"},
                ]
            },
        },
        ensure_ascii=False,
    )


@router.post("/task/execute")
async def execute_task(req: TaskRequest, background_tasks: BackgroundTasks):
    import core.services as app_mod

    admin_god = app_mod.admin_god
    model_router = app_mod.model_router
    intent_clf = app_mod.intent_clf

    try:
        admin_god.enforce("execute")
    except PermissionError as exc:
        raise HTTPException(status_code=403, detail=str(exc)) from exc

    # --- Prompt-to-Action: Intent Routing ---
    # বাংলা মন্তব্য: ইনপুটকে ক্যাটাগরি রূপে চিহ্নিত করে ফ্রন্টএন্ডকে ডাইনামিক অ্যাকশন টিপ দিচ্ছে
    prompt_action: PromptAction = intent_router.route(req.task)

    # Offload heavy CPU-bound Intent classification to background thread pool
    app_spec = await anyio.to_thread.run_sync(
        app_mod.intent_parser.parse_intent, req.task, req.messages
    )
    intent = await anyio.to_thread.run_sync(intent_clf.classify, req.task)

    task_type = req.task_type
    if intent.task_type != "general" and req.task_type == "general":
        task_type = (
            intent.task_type.value
            if hasattr(intent.task_type, "value")
            else str(intent.task_type)
        )

    # Build prompt context if chat messages are provided
    prompt = format_unified_chat_prompt(req.task, req.messages)

    # --- True Vector Semantic Caching ---
    raw = None
    if semantic_cache:
        cached_text = await semantic_cache.get_cached_inference(
            prompt=prompt, model_name=task_type
        )
        if cached_text:
            raw = {
                "success": True,
                "text": cached_text,
                "provider": "semantic-vector-hit",
                "cost": 0.0,
            }

    if not raw:
        raw = await model_router.async_route_and_generate(
            prompt=prompt,
            task_type=task_type,
            max_cost=req.max_cost,
        )
        if raw.get("success") and semantic_cache:
            with contextlib.suppress(Exception):
                await semantic_cache.set_cache_inference(
                    prompt=prompt, model_name=task_type, response_text=raw.get("text")
                )

    # Log to ExperienceDatabase in the background to improve user-perceived latency.
    exp = Experience(
        timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        user_id=req.session_id or "default-user",
        request=req.task,
        context={
            "task_type": task_type,
            "session_id": req.session_id,
            "app_type": app_spec.app_type,
            "features": app_spec.features,
            "tech_stack": app_spec.tech_stack,
            "pages": app_spec.pages,
            "integrations": app_spec.integrations,
            "deployment_target": app_spec.deployment_target,
        },
        action_taken=f"Executed task on provider {raw.get('provider')}",
        result="success" if raw.get("success") else "failure",
        error_message=raw.get("error"),
        generated_code=raw.get("text") if ("```" in raw.get("text", "")) else None,
        what_worked=["Intent parsed successfully"] if raw.get("success") else [],
        what_failed=(
            [] if raw.get("success") else [str(raw.get("error", "Unknown error"))]
        ),
    )
    background_tasks.add_task(app_mod.experience_db.record_experience, exp)

    if not raw.get("success"):
        return ProblemDetailsResponse(
            title="Task Execution Failed",
            status=502,
            detail=raw.get("error") or "Unknown upstream error",
            type_url="https://supremeai.local/errors/bad-gateway",
            instance="/task/execute",
            provider=raw.get("provider"),
            cost=raw.get("cost"),
        )

    # Format output as Action-Oriented JSON string
    formatted_result = format_response(raw.get("text", ""), task_type)

    return TaskResponse(
            success=True,
            result=formatted_result,
            provider=raw.get("provider"),
            cost=raw.get("cost", 0.0),
            action={
                "type": prompt_action.action_type,
                "target": prompt_action.target_module,
                "label": prompt_action.label,
                "icon": prompt_action.icon,
                "confidence": prompt_action.confidence,
                "requires_confirmation": prompt_action.requires_confirmation,
                "payload": prompt_action.payload,
            },
            intent={
                "task_type": intent.task_type.value if hasattr(intent.task_type, "value") else str(intent.task_type),
                "confidence": intent.confidence,
            },
        )


@router.get("/api/task/stream")
async def task_stream():
    async def keepalive():
        yield f"data: {json.dumps({'status': 'alive', 'timestamp': datetime.datetime.now(datetime.timezone.utc).isoformat()})}\n\n"

    return StreamingResponse(
        keepalive(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.post("/api/chat/prompt-action")
async def prompt_action(req: ActionStreamRequest):
    from core.intent_router import intent_router
    from core.intent import IntentClassifier

    action = intent_router.route(req.message)
    intent_clf = IntentClassifier()
    intent = intent_clf.classify(req.message)

    return JSONResponse({
        "action": {
            "type": action.action_type,
            "target": action.target_module,
            "label": action.label,
            "icon": action.icon,
            "confidence": action.confidence,
            "requires_confirmation": action.requires_confirmation,
            "payload": action.payload,
        },
        "intent": {
            "task_type": intent.task_type.value,
            "confidence": intent.confidence,
        },
    })
