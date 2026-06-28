from typing import Any


class LocalModelHandler:
    def __init__(self) -> None:
        self.healthy = False

    async def health_check(self) -> bool:
        return self.healthy

    async def list_models(self) -> list[str]:
        return []

    async def infer(self, model: str, prompt: str) -> dict[str, Any]:
        return {"text": "", "model": model}
