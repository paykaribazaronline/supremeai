# বাংলা মন্তব্য: স্টার্টআপ ও ইম্পোর্ট টাইম কমাতে LazyModule ডিফাইন করা হলো।
# এটি `sys.modules` এ সাবমডিউলগুলোর ডামি প্রক্সি হিসেবে কাজ করবে এবং প্রথম অ্যাট্রিবিউট অ্যাক্সেসে
# আসল সাবমডিউল লোড করবে। ফলে `tools` ইম্পোর্ট করলেও হেভি ডিপেন্ডেন্সিগুলো ব্যাকগ্রাউন্ডে অলস (lazy) থাকবে।
import importlib
import sys
import types


class LazyModule(types.ModuleType):
    def __init__(self, name: str, real_path: str):
        super().__init__(name)
        self._real_path = real_path
        self._module = None

    def _load(self):
        if self._module is None:
            self._module = importlib.import_module(self._real_path)
            self.__dict__.update(self._module.__dict__)
        return self._module

    def __getattr__(self, item):
        module = self._load()
        return getattr(module, item)

    def __dir__(self):
        module = self._load()
        return dir(module)


# isolated tests বা venv-এ sys.modules['tools'] KeyError এড়াতে সেলফ-ম্যাপিং
if "tools" not in sys.modules:
    sys.modules["tools"] = sys.modules[__name__]

_SUBMODULE_MAP = {
    "mcp_cloud_deploy": "tools.mcp.mcp_cloud_deploy",
    "mcp_github_cicd": "tools.mcp.mcp_github_cicd",
    "mcp_supabase": "tools.mcp.mcp_supabase",
    "mcp_workspace": "tools.mcp.mcp_workspace",
    "bangla_voice": "tools.localization.bangla_voice",
    "model_trainer": "tools.learning.model_trainer",
    "pr_reviewer": "tools.code.pr_reviewer",
    "skill_recommender": "tools.learning.skill_recommender",
    "browser_agent": "tools.ai_agents.browser_agent",
    "style_learner": "tools.learning.style_learner",
    "auto_coverage_improver": "tools.devops.auto_coverage_improver",
    "image_to_code": "tools.code.image_to_code",
    "multilingual_tts": "tools.media.multilingual_tts",
}

# sys.modules এ প্রক্সি রেজিস্টার করা
for name, real_path in _SUBMODULE_MAP.items():
    lazy_mod = LazyModule(f"tools.{name}", real_path)
    sys.modules[f"tools.{name}"] = lazy_mod
    setattr(sys.modules[__name__], name, lazy_mod)

__all__ = list(_SUBMODULE_MAP.keys())
