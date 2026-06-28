from typing import Any


class SkillRegistry:
    SKILLS: dict[str, dict[str, Any]] = {
        "video_editing": {
            "dependencies": ["ffmpeg", "imagemagick"],
            "terraform_module": "gcp/skill_gpu",
            "category": "media",
        },
        "stable_diffusion": {
            "dependencies": ["torch", "diffusers"],
            "terraform_module": "gcp/skill_gpu_heavy",
            "category": "ai_media",
        },
    }

    @classmethod
    def get(cls, skill_id: str) -> dict[str, Any] | None:
        return cls.SKILLS.get(skill_id)

    @classmethod
    def list_all(cls) -> dict[str, dict[str, Any]]:
        return cls.SKILLS
