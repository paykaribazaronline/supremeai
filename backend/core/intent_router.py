import json
import re
from dataclasses import dataclass, field
from typing import Any


@dataclass
class PromptAction:
    action_type: str
    target_module: str | None = None
    payload: dict[str, Any] = field(default_factory=dict)
    confidence: float = 0.0
    requires_confirmation: bool = False
    label: str | None = None
    icon: str | None = None


ACTION_PATTERNS = {
    "code_generate": {
        "keywords": [
            "write", "create", "generate", "build", "make", "implement",
            "function", "component", "script", "program", "code", "api",
            "class", "method", "algorithm", "cli", "tool", "bot",
            "python", "javascript", "typescript", "react", "node",
        ],
        "target": "ide",
        "icon": "💻",
        "label": "Generate Code",
        "requires_confirmation": False,
    },
    "ide_open": {
        "keywords": [
            "open ide", "switch to code", "show editor", "full editor",
            "open editor", "edit code", "start coding", "write code",
            "new file", "open project",
        ],
        "target": "ide",
        "icon": "🖥️",
        "label": "Open IDE",
        "requires_confirmation": False,
    },
    "video_edit": {
        "keywords": [
            "video", "edit", "trim", "cut", "merge", "timeline", "clip",
            "frame", "audio", "background music", "transition",
        ],
        "target": "video_editor",
        "icon": "🎬",
        "label": "Edit Video",
        "requires_confirmation": True,
    },
    "research": {
        "keywords": [
            "search", "research", "find", "look up", "google",
            "investigate", "explain", "what is", "who is",
            "summarize", "analyze data", "report",
        ],
        "target": "research",
        "icon": "🔍",
        "label": "Research",
        "requires_confirmation": False,
    },
    "deploy": {
        "keywords": [
            "deploy", "publish", "push to production",
            "go live", "release", "host", "ship it",
        ],
        "target": "deploy",
        "icon": "🚀",
        "label": "Deploy",
        "requires_confirmation": True,
    },
    "settings_change": {
        "keywords": [
            "settings", "preferences", "config", "theme", "model",
            "provider", "temperature", "max tokens",
        ],
        "target": "settings",
        "icon": "⚙️",
        "label": "Settings",
        "requires_confirmation": False,
    },
}


class IntentRouter:
    def route(self, prompt: str) -> PromptAction:
        text = prompt.lower()
        scores: dict[str, int] = {}
        for action_name, cfg in ACTION_PATTERNS.items():
            score = sum(1 for kw in cfg["keywords"] if re.search(r"(^|\W)" + re.escape(kw) + r"(\W|$)", text))
            if score > 0:
                scores[action_name] = score

        if not scores:
            return PromptAction(
                action_type="chat",
                target_module=None,
                confidence=0.5,
                label=None,
                icon=None,
            )

        best = max(scores, key=lambda k: scores[k])
        total = sum(scores.values())
        confidence = round(scores[best] / total, 3)
        cfg = ACTION_PATTERNS[best]

        payload: dict[str, Any] = {"original_prompt": prompt}

        if best == "code_generate":
            payload["language"] = self._detect_language(text)
            payload["filename"] = self._guess_filename(payload["language"])
        elif best == "video_edit":
            payload["operations"] = self._extract_operations(text)
        elif best == "research":
            payload["query"] = prompt.strip()
        elif best == "deploy":
            payload["target"] = "firebase" if "firebase" in text else "vercel" if "vercel" in text else "cloud_run"
        elif best == "settings_change":
            payload["changes"] = self._extract_setting_changes(text)

        return PromptAction(
            action_type=best,
            target_module=cfg["target"],
            payload=payload,
            confidence=confidence,
            requires_confirmation=cfg["requires_confirmation"],
            label=cfg["label"],
            icon=cfg["icon"],
        )

    def _detect_language(self, text: str) -> str:
        lang_map = {
            "python": "python", "javascript": "javascript", "typescript": "typescript",
            "react": "jsx", "node": "javascript", "java": "java", "c++": "cpp",
            "cpp": "cpp", "rust": "rust", "go": "go", "html": "html",
            "css": "css", "sql": "sql", "shell": "bash", "bash": "bash",
        }
        for lang, code in lang_map.items():
            if re.search(r"(^|\W)" + re.escape(lang) + r"(\W|$)", text):
                return code
        return "javascript"

    def _guess_filename(self, language: str) -> str:
        defaults = {
            "python": "main.py", "javascript": "index.js", "typescript": "index.ts",
            "jsx": "App.jsx", "tsx": "App.tsx", "html": "index.html",
            "java": "Main.java", "rust": "main.rs", "go": "main.go",
        }
        return defaults.get(language, "component.tsx")

    def _extract_operations(self, text: str) -> list[str]:
        ops = []
        for op in ["trim", "cut", "merge", "transition", "filter", "overlay", "caption"]:
            if op in text:
                ops.append(op)
        return ops or ["edit"]

    def _extract_setting_changes(self, text: str) -> list[str]:
        changes = []
        for kw in ["theme", "model", "provider", "temperature", "max tokens", "compact"]:
            if kw in text:
                changes.append(kw)
        return changes


intent_router = IntentRouter()
