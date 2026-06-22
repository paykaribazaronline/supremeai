import json

from fastapi import APIRouter
from pydantic import BaseModel
from fastapi.responses import StreamingResponse, JSONResponse


router = APIRouter()


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
    import core.app as app_mod
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
    import core.app as app_mod

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
            "X-Accel-Buffering": "no"
        }
    )


class ProblemDetailsResponse(JSONResponse):
    def __init__(self, title: str, status: int, detail: str, type_url: str = "about:blank", instance: typing.Optional[str] = None, **kwargs):
        content = {
            "type": type_url,
            "title": title,
            "status": status,
            "detail": detail,
            "instance": instance or "",
        }
        content.update(kwargs)
        super().__init__(status_code=status, content=content, media_type="application/problem+json")


# --- Action Cards Helpers ---
# Added by Agent Antigravity on 2026-06-21. Formats output as structured action card JSON.
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
        return json.dumps({
            "type": "code",
            "content": extract_code(text),
            "metadata": {
                "language": detect_language(text),
                "filename": "index.html" if "html" in detect_language(text) else "component.tsx",
                "actions": [
                    {"id": "preview", "label": "👁️ Preview", "type": "preview"},
                    {"id": "save", "label": "💾 Save to Project", "type": "save"},
                    {"id": "run", "label": "▶️ Run Code", "type": "run"},
                    {"id": "deploy", "label": "🚀 Deploy", "type": "deploy"}
                ]
            }
        }, ensure_ascii=False)
        
    if task_type == "image" or "generate image" in text.lower():
        import re
        urls = re.findall(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\(\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', text)
        image_url = urls[0] if urls else text
        return json.dumps({
            "type": "image",
            "content": image_url,
            "metadata": {
                "actions": [
                    {"id": "download", "label": "⬇️ Download", "type": "save"},
                    {"id": "share", "label": "🔗 Share", "type": "share"}
                ]
            }
        }, ensure_ascii=False)
        
    return json.dumps({
        "type": "text",
        "content": text,
        "metadata": {
            "actions": [
                {"id": "copy", "label": "📋 Copy", "type": "copy"},
                {"id": "share", "label": "🔗 Share", "type": "share"}
            ]
        }
    }, ensure_ascii=False)


@router.post("/task/execute")
def execute_task(req: TaskRequest):
    import core.app as app_mod
    admin_god = app_mod.admin_god
    model_router = app_mod.model_router
    intent_clf = app_mod.intent_clf

    try:
        admin_god.enforce("execute")
    except PermissionError as exc:
        return ProblemDetailsResponse(
            title="Forbidden Access",
            status=403,
            detail=str(exc),
            type_url="https://supremeai.local/errors/forbidden",
            instance="/task/execute"
        )

    # Use the adaptive IntentParser
    app_spec = app_mod.intent_parser.parse_intent(req.task, history=req.messages)

    intent = intent_clf.classify(req.task)
    task_type = req.task_type
    if intent.task_type != "general" and req.task_type == "general":
        task_type = intent.task_type.value if hasattr(intent.task_type, "value") else str(intent.task_type)

    # Build prompt context if chat messages are provided
    prompt = req.task
    if req.messages and len(req.messages) > 1:
        context_prompt = format_chat_history(req.messages[:-1])
        prompt = f"{context_prompt}\nUser: {req.task}\nAssistant:"

    raw = model_router.route_and_generate(
        prompt=prompt,
        task_type=task_type,
        max_cost=req.max_cost,
    )

    # Log to ExperienceDatabase
    from adaptive_engine.experience_db import Experience
    import datetime
    
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
            "deployment_target": app_spec.deployment_target
        },
        action_taken=f"Executed task on provider {raw.get('provider')}",
        result="success" if raw.get("success") else "failure",
        error_message=raw.get("error"),
        generated_code=raw.get("text") if ("```" in raw.get("text", "")) else None,
        what_worked=["Intent parsed successfully"] if raw.get("success") else [],
        what_failed=[] if raw.get("success") else [raw.get("error", "Unknown error")]
    )
    try:
        app_mod.experience_db.record_experience(exp)
    except Exception:
        pass

    if not raw.get("success"):
        return ProblemDetailsResponse(
            title="Task Execution Failed",
            status=502,
            detail=raw.get("error") or "Unknown upstream error",
            type_url="https://supremeai.local/errors/bad-gateway",
            instance="/task/execute",
            provider=raw.get("provider"),
            cost=raw.get("cost")
        )

    # Format output as Action-Oriented JSON string
    formatted_result = format_response(raw.get("text", ""), task_type)

    return TaskResponse(
        success=True,
        result=formatted_result,
        provider=raw.get("provider"),
        cost=raw.get("cost", 0.0),
    )


